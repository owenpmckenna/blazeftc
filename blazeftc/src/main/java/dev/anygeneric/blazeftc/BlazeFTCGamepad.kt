package dev.anygeneric.blazeftc

import com.qualcomm.robotcore.hardware.Gamepad

private class BlazeFTCGamepad(val othergp: Gamepad) : Gamepad() {
    //does not seem to work, unused as of now
    override fun copy(gamepad: Gamepad?) {
        BlazeFTC.gamepad(othergp.toByteArray(), gamepad!!.toByteArray())
        println("COPIED!!!")
        super.copy(gamepad)
    }
}