package dev.anygeneric.blazeftc

import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.DcMotorSimple
import org.firstinspires.ftc.robotcore.external.navigation.Rotation

class AcceleratedMotor
@JvmOverloads
constructor(
    open val dcMotorEx: DcMotorEx,
) : DcMotorEx by dcMotorEx {
    override fun setPower(power: Double) {
        val port = dcMotorEx.portNumber
        val power = adjustPower0(power)
        BlazeFTC.setMotorPower(hubId, port, power)
    }

    val hubId = run {
        val match1 = "(?<=module )[0-9]*".toRegex()
        val hub1 = this@AcceleratedMotor.dcMotorEx.controller.connectionInfo
        match1.find(hub1)!!.value.toInt()
    }

    fun adjustPower0(power: Double): Double {
        var power = power
        if (getOperationalDirection0() == DcMotorSimple.Direction.REVERSE) power = -power
        return power
    }

    fun getOperationalDirection0(): DcMotorSimple.Direction {
        return if (motorType.orientation == Rotation.CCW) direction.inverted() else direction
    }
}