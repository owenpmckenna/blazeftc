package dev.anygeneric.blazeftc_pedro

import com.pedropathing.ftc.drivetrains.Mecanum
import com.qualcomm.robotcore.hardware.HardwareMap
import dev.anygeneric.blazeftc.BlazeFTC
import kotlin.math.abs
import kotlin.math.max

open class BulkWriteMecanum
constructor(
    hardwareMap: HardwareMap,
    open val mdt: Mecanum,
) : Mecanum(hardwareMap, mdt.constants) {
    val hubId = run {
        val match1 = "(?<=module )[0-9]*".toRegex()
        val hub1 = this.mdt.motors[0].controller.connectionInfo
        match1.find(hub1)!!.value.toInt()
    }

    override fun runDrive(drivePowers: DoubleArray?) {
        super.runDrive(drivePowers)
        val list = mutableListOf(0.0, 0.0, 0.0, 0.0)
        for (i in motors.indices) {
            list[motors[i].portNumber] = motors[i].power
        }
        BlazeFTC.setMotorPowers(hubId, list[0], list[1], list[2], list[3])
    }
}