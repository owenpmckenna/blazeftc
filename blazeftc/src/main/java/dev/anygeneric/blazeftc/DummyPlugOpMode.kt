package dev.anygeneric.blazeftc

import com.qualcomm.hardware.lynx.LynxModule
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode
import com.qualcomm.robotcore.hardware.DcMotorEx
import org.firstinspires.ftc.robotcore.external.Telemetry
import org.firstinspires.ftc.robotcore.external.navigation.VoltageUnit
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicBoolean

abstract class DummyPlugOpMode : LinearOpMode() {
    private var opened = AtomicBoolean(false)
    private var userTelemetry: Telemetry? = null
    companion object {
        private var outUsed = false
        private var inUsed = false
        private var timesRespondedLargeNumber = 0
    }
    private fun getClosures(accessor: InterfaceAccessor, hwNum: Int): Pair<FileInputStream, FileOutputStream> {
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
    private fun inform(it: LynxModule) {
        println("informing of module: " + it.moduleAddress + ": " + it.isParent)
        //note: this doesn't touch hardware. it's preemptive
        val extractor = InterfaceAccessor(it)
        BlazeFTC.informOfModule(it.moduleAddress, it.isParent, extractor.extractUnderlyingFD())
    }
    fun engageMotorAcceleration() {
        val hardwareMap = hardwareMap
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
    final fun initializeBlazeFTC(userTelemetry: Telemetry) : Telemetry {
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

        val ctrlHub = module.first { it.isParent }
        inform(ctrlHub)
        val ctrlHubAccessor = InterfaceAccessor(ctrlHub)
        val fileDescriptor = ctrlHubAccessor.extractUnderlyingFD()
        val ctrlStreams = getClosures(ctrlHubAccessor, ctrlHub.moduleAddress)

        val exHub = module.firstOrNull { !it.isParent }
        if (exHub != null) {
            inform(exHub)
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
    fun runBlazeFTC(toRun: Int) {
        BlazeFTC.run(toRun)
    }
    fun closeBlazeFTC() {
        BlazeFTC.close()
    }
    fun updateGamepads() {
        BlazeFTC.gamepad(gamepad1.toByteArray(), gamepad2!!.toByteArray())
    }
    private fun open() {
        BlazeFTC.initialize(BlazeFTC.bt)//this function tells BlazeFTC to take over hardware
        //it will h ave no effect if it has already been called.
    }
    abstract fun runOpModeInBlaze();
    override fun runOpMode() {
        try {
            runOpModeInBlaze()
        } catch (e: Throwable) {
            println("caught $e in BlazeFTC's DPOM")
            throw e
        } finally {
            println("Closing BlazeFTC")
            closeBlazeFTC()
        }
    }
}