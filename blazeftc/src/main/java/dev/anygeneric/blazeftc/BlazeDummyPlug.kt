package dev.anygeneric.blazeftc

import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver
import com.qualcomm.hardware.lynx.LynxController
import com.qualcomm.hardware.lynx.LynxI2cDeviceSynch
import com.qualcomm.hardware.lynx.LynxModule
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.HardwareMap
import org.firstinspires.ftc.robotcore.external.Telemetry
import org.firstinspires.ftc.robotcore.external.navigation.VoltageUnit
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.absoluteValue
import kotlin.random.Random

object BlazeDummyPlug {
    /**
     * This used for logging the first output packet. ignore it.
     */
    var outUsed = false
    /**
     * This used for logging the first input packet. ignore it.
     */
    var inUsed = false
    var timesRespondedLargeNumber = 0
    /**
     * This controls if we give handles to Blaze again. It's atomic because it's used in multiple threads
     */
    var opened = AtomicBoolean(false)
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
        if (extractor.module_status() == InterfaceAccessor.ModuleStatus.ServoHub)
            return false
        if (extractor.module_status() == InterfaceAccessor.ModuleStatus.USB)
            return false
        BlazeFTC.informOfModule(it.moduleAddress, it.isParent, extractor.extractUnderlyingFD())
        return true
    }
    /**
     * You *must* initialize a java pinpoint driver before calling this to set the settings.
     */
    @JvmStatic
    fun engagePinpointAcceleration(ppd: GoBildaPinpointDriver, acceptor: (PositionData) -> Unit) {
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
        BlazeFTC.setByteHandler(tempId) {
            val tmp = PositionData()
            tmp.handlePinpointData(it)
            acceptor(tmp)
            byteArrayOf(1)
        }
    }
    @JvmStatic
    fun getClosures(accessor: InterfaceAccessor, hwNum: Int, ): Pair<FileInputStream, FileOutputStream> {
        //hwNum is ignored except for when there's an RS485 Ex Hub. otherwise it *does not* matter
        return accessor.createFakeStreams(
            {bytes, off, len ->
                if (!opened.getAndSet(true))
                    open()
                if (!inUsed) {
                    Throwable("Note: not an error, input stream called to read $len bytes").printStackTrace()
                    if (len != 1 && len < 250) {
                        inUsed = true
                    } else if (len >= 250) {
                        println("responded with all blanks")
                        (off..<len+off).forEach { bytes[it] = 0 }
                        return@createFakeStreams len
                    } else if (timesRespondedLargeNumber < 6) {
                        timesRespondedLargeNumber += 1
                        println("responded with large number")
                        bytes[off] = Byte.MAX_VALUE
                        return@createFakeStreams 1
                        //Return an unnecessarily large byte so we can give them empty data
                    }
                }
                BlazeFTC.read(bytes, off, len, hwNum)
            },
            { bytes, off, len ->
                if (!opened.getAndSet(true))
                    open()
                if (!outUsed) {
                    outUsed = true
                    println("output stream used first time! Printing... ${bytes.joinToString(",") { it.toInt().toString() }}")
                }
                BlazeFTC.write(bytes.slice(off..<off + len).toByteArray(), hwNum)
            }
        )
    }
    private fun open() {
        BlazeFTC.initialize(BlazeFTC.bt)//this function tells BlazeFTC to take over hardware
        //it will have no effect if it has already been called.
    }
    @JvmStatic
    fun closeBlazeFTC() {
        BlazeFTC.close()
        BlazeFTC.clearByteHandlers()
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
        val ctrlHub = module.firstOrNull { it.isParent && tryInform(it) }
        if (ctrlHub == null)
            throw IllegalArgumentException("No non-usb parent control hubs!")
        val ctrlHubAccessor = InterfaceAccessor(ctrlHub)
        val fileDescriptor = ctrlHubAccessor.extractUnderlyingFD()
        val ctrlStreams = getClosures(ctrlHubAccessor, ctrlHub.moduleAddress)

        var exHub = module.firstOrNull { !it.isParent && it.module_status() != InterfaceAccessor.ModuleStatus.ServoHub }
        if (exHub != null) {
            //if the exHub is over USB, just drop it and pretend it doesn't exist
            if (!tryInform(exHub)) {
                println("discovered ex hub over usb! Ignoring it...")
                exHub = null
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
                //RS485!
            } else {
                exHubStreams = getClosures(exHubAccessor, exHub.moduleAddress)
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
            exHubAccessor!!.replaceStreams(exHubStreams) {
                val volts = exHub!!.getInputVoltage(VoltageUnit.VOLTS)
                println("Checking volts: $volts, check num: $voltsChecked")
                voltsChecked++
            }
        }

        return bt.ct
    }
}
fun LynxModule.module_status() = InterfaceAccessor(this).module_status()