package dev.anygeneric.blazeftc

import java.nio.ByteBuffer
import java.nio.ByteOrder

class PositionData {
    var xPosition = 0f
    var yPosition = 0f
    /***
     * radians, obviously
     */
    var direction = 0f
    var xVelocity = 0f
    var yVelocity = 0f
    var angVelocity = 0f

    //these are optional, for instance they have no meaningful value on a OTOS localizer
    var xEncoder = 0
    var yEncoder = 0
    private fun arrToFloat(arr: ByteArray): Float {
        return ByteBuffer.wrap(arr).order(ByteOrder.LITTLE_ENDIAN).getFloat()
    }
    private fun arrToInt(arr: ByteArray): Int {
        return ByteBuffer.wrap(arr).order(ByteOrder.LITTLE_ENDIAN).getInt()
    }

    fun handlePinpointData(bArr: ByteArray) {
        xEncoder = arrToInt(bArr.copyOfRange(8, 12))
        yEncoder = arrToInt(bArr.copyOfRange(12, 16))
        xPosition = arrToFloat(bArr.copyOfRange(16, 20))
        yPosition = arrToFloat(bArr.copyOfRange(20, 24))
        direction = arrToFloat(bArr.copyOfRange(24, 28))
        xVelocity = arrToFloat(bArr.copyOfRange(28, 32))
        yVelocity = arrToFloat(bArr.copyOfRange(32, 36))
        angVelocity = arrToFloat(bArr.copyOfRange(36, 40))
    }

}