package dev.anygeneric.blazeftc

import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode
import com.qualcomm.robotcore.hardware.HardwareMap
import org.firstinspires.ftc.robotcore.external.Telemetry

abstract class DummyPlugOpMode : LinearOpMode() {
    fun sendPropertyToRust(key: String, value: String) {
        BlazeFTC.sendProperty(key, value);
    }
    fun engageMotorAcceleration() {
        BlazeDummyPlug.engageMotorAccel(hardwareMap)
    }
    fun engagePinpointAcceleration(ppd: GoBildaPinpointDriver, acceptor: (PositionData) -> Unit) {
        BlazeDummyPlug.engagePinpointAcceleration(ppd, acceptor)
    }
    fun engageBulkReadAcceleration(ctrlHub: Boolean, numberPackets: Int, acceptor: () -> Unit) {
        BlazeDummyPlug.engageBulkReadAcceleration(hardwareMap, ctrlHub, numberPackets, acceptor)
    }
    final fun initializeBlazeFTC(userTelemetry: Telemetry) : Telemetry =
        BlazeDummyPlug.initializeBlazeFTC(telemetry, hardwareMap)
    fun runBlazeFTC(toRun: Int) {
        BlazeFTC.run(toRun)
    }
    fun updateGamepads() {
        BlazeFTC.gamepad(gamepad1.toByteArray(), gamepad2!!.toByteArray())
    }
    abstract fun runOpModeInBlaze();
    override fun runOpMode() {
        try {
            runOpModeInBlaze()
        } catch (e: Throwable) {
            println("BlazeFTC's Dummy Plug OpMode caught $e")
            e.printStackTrace()
            throw e
        } finally {
            println("Closing BlazeFTC")
            BlazeDummyPlug.closeBlazeFTC()
        }
    }
}