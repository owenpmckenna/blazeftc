package dev.anygeneric.blazeftc

import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver
import com.qualcomm.hardware.lynx.LynxController
import com.qualcomm.hardware.lynx.LynxI2cDeviceSynch
import com.qualcomm.hardware.lynx.LynxModule
import com.qualcomm.hardware.lynx.commands.core.LynxGetBulkInputDataResponse
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.HardwareMap
import org.firstinspires.ftc.robotcore.external.Telemetry
import org.firstinspires.ftc.robotcore.external.navigation.VoltageUnit
import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.absoluteValue
import kotlin.random.Random

object BlazeDummyPlug {
    /**
     * This used for logging the first output packet. ignore it.
     */
    var outUsed = mutableListOf(false, false)
    /**
     * This used for logging the first input packet. ignore it.
     */
    var inUsed = mutableListOf(false, false)
    var timesRespondedLargeNumber = mutableListOf(0, 0)
    /**
     * This controls if we give handles to Blaze again. It's atomic because it's used in multiple threads
     */
    var opened = listOf(AtomicBoolean(false), AtomicBoolean(false))
    @JvmStatic
    fun engageMotorAccel(hardwareMap: HardwareMap) {
        val motors = hardwareMap.getAllNames(DcMotorEx::class.java)
        for (m in motors) {
            var motor = hardwareMap.get(DcMotorEx::class.java, m)
            if (motor is AcceleratedMotor) {
                continue
            }
            //hardwareMap.remove(m, motor)
            motor = AcceleratedMotor(motor)
            hardwareMap.dcMotor.remove(m)
            //hardwareMap.put(m, motor)

            hardwareMap.dcMotor.put(m, motor)
        }
    }
    @JvmStatic
    fun tryInform(it: LynxModule) : Boolean {
        println("informing of module: " + it.moduleAddress + ": " + it.isParent)
        //note: this doesn't touch hardware. it's preemptive
        val extractor = InterfaceAccessor(it)
        var handFD: FileDescriptor? = extractor.extractUnderlyingFD()
        if (extractor.module_status() == InterfaceAccessor.ModuleStatus.ServoHub)
            return false
        if (extractor.module_status() == InterfaceAccessor.ModuleStatus.USB) {
            handFD = null
            BlazeFTC.ftd = extractor.ftd
            BlazeFTC.usb = extractor.ftdi
        }
        BlazeFTC.informOfModule(it.moduleAddress, it.isParent, handFD)
        return true
    }
    /*        let num = robot.get_property(&format!("attachBulkRead{}", ctrl))?
            .parse().unwrap_or(1);
        let callback_name = robot.get_property(&format!("bulkReadCallbackName{}", ctrl))?;
*/
    fun engageBulkReadAcceleration(hardwareMap: HardwareMap, ctrlHub: Boolean, numberPackets: Int, acceptor: (ByteArray) -> Unit) {
        val hubType = if (ctrlHub) InterfaceAccessor.ModuleStatus.Internal else InterfaceAccessor.ModuleStatus.RS485
        val hub = hardwareMap.getAll(LynxModule::class.java)
            .find { it.module_status() == hubType }!!

        val cons = LynxModule.BulkData::class.java.getDeclaredConstructor(
            LynxGetBulkInputDataResponse::class.java,
            Boolean::class.java
        );
        cons.isAccessible = true
        val bulkData = LynxModule::class.java.getDeclaredField("lastBulkData")
        bulkData.isAccessible = true

        val tempId = Random.nextInt().absoluteValue.toString()
        val name = if (ctrlHub) {"chub"} else {"exhub"}
        BlazeFTC.sendProperty("attachBulkRead$name", numberPackets.toString())
        BlazeFTC.sendProperty("bulkReadCallbackName$name", tempId)
        BlazeFTC.setByteHandler(tempId) { data ->
            try {
                val resp = LynxGetBulkInputDataResponse(hub)
                resp.fromPayloadByteArray(data)
                val bulk = cons.newInstance(resp, false)
                bulkData.set(hub, bulk)
                acceptor(data)
            } catch (t: Throwable) {
                t.printStackTrace();
                println("somehow got error inside of br byte handler $t")
            }
            byteArrayOf(1)
        }
    }
    /**
     * You *must* initialize a java pinpoint driver before calling this to set the settings.
     */
    @JvmStatic
    fun engagePinpointAcceleration(ppd: GoBildaPinpointDriver, acceptor: (PositionData) -> Unit, frequency: Int = -1) {
        val dcr = ppd.deviceClient
        val deviceClient = dcr as LynxI2cDeviceSynch;
        val busRead = LynxI2cDeviceSynch::class.java.getDeclaredField("bus").also { it.isAccessible = true }
        val bus = busRead.getInt(deviceClient)
        val moduleRead = LynxController::class.java.getDeclaredField("module").also { it.isAccessible = true }
        val module = moduleRead.get(deviceClient) as LynxModule

        val tempId = Random.nextInt().absoluteValue.toString()
        BlazeFTC.sendProperty("internalPinpointHub", if (module.isParent) "hub0" else "hub1")
        BlazeFTC.sendProperty("internalPinpointBus", bus.toString())
        BlazeFTC.sendProperty("internalPinpointCallbackName", tempId)
        if (frequency != -1) {
            BlazeFTC.sendProperty("internalPinpointUpdateFreq", frequency.toString())
        }
        BlazeFTC.setByteHandler(tempId) {
            try {
                val tmp = PositionData()
                tmp.handlePinpointData(it)
                acceptor(tmp)
            } catch (t: Throwable) {
                t.printStackTrace();
                println("somehow got error inside of br byte handler $t")
            }
            byteArrayOf(1)
        }
    }
    @JvmStatic
    fun getClosures(accessor: InterfaceAccessor, hwNum: Int, isOnUsbB: Boolean): Pair<FileInputStream, FileOutputStream> {
        val isOnUsb = if (isOnUsbB) 1 else 0
        //hwNum is ignored except for when there's an RS485 Ex Hub. otherwise it *does not* matter
        return accessor.createFakeStreams(
            {bytes, off, len ->
                if (!opened[isOnUsb].getAndSet(true))
                    open(isOnUsbB)
                if (!inUsed[isOnUsb]) {
                    Throwable("Note: not an error, input stream called to read $len bytes usb:$isOnUsbB").printStackTrace()
                    if (len != 1 && len < 250) {
                        inUsed[isOnUsb] = true
                    } else if (len >= 250) {
                        println("responded with all blanks usb:$isOnUsbB")
                        (off..<len+off).forEach { bytes[it] = 0 }
                        return@createFakeStreams len
                    } else if (timesRespondedLargeNumber[isOnUsb] < 6) {
                        timesRespondedLargeNumber[isOnUsb] += 1
                        println("responded with large number usb:$isOnUsbB")
                        bytes[off] = Byte.MAX_VALUE
                        return@createFakeStreams 1
                        //Return an unnecessarily large byte so we can give them empty data
                    }
                }
                BlazeFTC.read(bytes, off, len, hwNum)
            },
            { bytes, off, len ->
                if (!opened[isOnUsb].getAndSet(true))
                    open(isOnUsbB)
                if (!outUsed[isOnUsb]) {
                    outUsed[isOnUsb] = true
                    println("output stream used first time usb:$isOnUsbB! Printing... ${bytes.joinToString(",") { it.toInt().toString() }}")
                }
                BlazeFTC.write(bytes.slice(off..<off + len).toByteArray(), hwNum)
            }
        )
    }
    private fun open(isUsb: Boolean) {
        BlazeFTC.initialize(BlazeFTC.bt, isUsb)//this function tells BlazeFTC to take over hardware
        //it will have no effect if it has already been called.
    }
    @JvmStatic
    fun closeBlazeFTC() {
        BlazeFTC.close()
        BlazeFTC.clearByteHandlers()
    }
    @JvmStatic
    fun initializeBlazeFTC(hardwareMap: HardwareMap) {
        initializeBlazeFTC(NOPTelemetry(), hardwareMap)
    }
    @JvmStatic
    fun initializeBlazeFTC(userTelemetry: Telemetry, hardwareMap: HardwareMap) : Telemetry {
        BlazeFTC.load()
        val bt = if (BlazeFTC.bt == null) {
            val bt = BlazeFTC.BlazeTelemetry(userTelemetry)
            BlazeFTC.bt = bt
            bt.ct = CachedTelemetry(bt)
            bt
        } else {
            BlazeFTC.bt.ct.clearAll()
            BlazeFTC.bt.telemetry.clearAll()
            BlazeFTC.bt
        }

        val module = hardwareMap.getAll(LynxModule::class.java)

        module.forEach { it.bulkCachingMode = LynxModule.BulkCachingMode.MANUAL; it.clearBulkCache() }
        module.forEach { println("MODULE ADDRESS: " + it.moduleAddress + ": " + it.isParent) }

        //the first that isParent (not over rs485) and isn't over USB
        val ctrlHub = module.firstOrNull { it.module_status() == InterfaceAccessor.ModuleStatus.Internal && tryInform(it) }
        if (ctrlHub == null)
            throw IllegalArgumentException("No non-usb parent control hubs!")
        val ctrlHubAccessor = InterfaceAccessor(ctrlHub)
        val fileDescriptor = ctrlHubAccessor.extractUnderlyingFD()
        val ctrlStreams = getClosures(ctrlHubAccessor, ctrlHub.moduleAddress, false)

        val exHub = module.firstOrNull { it.module_status() != InterfaceAccessor.ModuleStatus.ServoHub && it.module_status() != InterfaceAccessor.ModuleStatus.Internal }
        if (exHub != null) {
            //if the exHub is over USB, just drop it and pretend it doesn't exist
            if (!tryInform(exHub)) {
                println("discovered ex hub over usb! Using it...")
                //exHub = null
            } else {
                println("discovered ex hub over rs485!")
            }
        }

        for (i in module.filter { it.module_status() == InterfaceAccessor.ModuleStatus.ServoHub }) {
            //we have to tell Blaze that the exhub is the parent if it exists, so it doesn't try to send too many RS485 packets
            val through = if (exHub != null) {
                exHub.moduleAddress
            } else {
                ctrlHub.moduleAddress
            }
            BlazeFTC.informOfServoHub(i.moduleAddress, through)
        }

        var exHubAccessor: InterfaceAccessor? = null
        var exHubStreams: Pair<FileInputStream, FileOutputStream>? = null
        if (exHub != null) {
            exHubAccessor = InterfaceAccessor(exHub)
            val exDescriptor = exHubAccessor.extractUnderlyingFD()
            if (fileDescriptor == exDescriptor) {
                println("exhub over uart")
                //RS485!
            } else {
                println("exhub over usb")
                //USB!
                exHubStreams = getClosures(exHubAccessor, exHub.moduleAddress, true)
            }
        }

        var voltsChecked = 0
        ctrlHubAccessor.replaceStreams(ctrlStreams) {
            //this is the action that triggers a packet to be sent it does not matter what it is.
            val volts = ctrlHub.getInputVoltage(VoltageUnit.VOLTS)
            println("Checking volts: $volts, check num: $voltsChecked")
            voltsChecked++
        }
        voltsChecked = 0
        if (exHubStreams != null) {
            exHubAccessor!!.replaceUsbStreams(exHubStreams) {
                val volts = exHub!!.getInputVoltage(VoltageUnit.VOLTS)
                println("Checking usb volts: $volts, check num: $voltsChecked")
                voltsChecked++
            }
        }

        return bt.ct
    }
}
fun LynxModule.module_status() = InterfaceAccessor(this).module_status()