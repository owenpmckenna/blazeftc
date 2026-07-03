package dev.anygeneric.blazeftc

import com.qualcomm.hardware.lynx.LynxModule
import com.qualcomm.hardware.lynx.LynxUsbDevice
import com.qualcomm.robotcore.hardware.usb.RobotUsbDevice
import com.qualcomm.robotcore.hardware.usb.serial.RobotUsbDeviceTty
import com.qualcomm.robotcore.hardware.usb.serial.SerialPort
import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream
import com.qualcomm.robotcore.hardware.usb.ftdi.RobotUsbDeviceFtdi
import kotlin.io.path.Path
import kotlin.math.PI

class InterfaceAccessor(val module: LynxModule) {
    //com.qualcomm.robotcore.hardware.usb.ftdi.RobotUsbDeviceFtdi
    val usbDevField = LynxModule::class.java.getDeclaredField("lynxUsbDevice").also { it.isAccessible = true }

    //for tty device
    val serialPortField = RobotUsbDeviceTty::class.java.getDeclaredField("serialPort").also { it.isAccessible = true }
    val fileDescriptorField = SerialPort::class.java.getDeclaredField("fileDescriptor").also { it.isAccessible = true }
    val inputStreamField = SerialPort::class.java.getDeclaredField("fileInputStream").also { it.isAccessible = true }
    val outputStreamField = SerialPort::class.java.getDeclaredField("fileOutputStream").also { it.isAccessible = true }
    val fileField = SerialPort::class.java.getDeclaredField("file").also { it.isAccessible = true }

    //for ftdi device


    val device = usbDevField.get(module) as LynxUsbDevice
    val port = if (device.robotUsbDevice is RobotUsbDeviceTty) {
            serialPortField.get(device.robotUsbDevice) as SerialPort
        } else {null}
    fun extractUnderlyingFD(): FileDescriptor {
        return fileDescriptorField.get(port) as FileDescriptor
    }
    fun extractStreams(): Pair<FileInputStream, FileOutputStream> {
        return Pair(
            inputStreamField.get(port) as FileInputStream,
            outputStreamField.get(port) as FileOutputStream
        )
    }
    fun replaceStreams(streams: Pair<FileInputStream, FileOutputStream>, action: () -> Unit) {
        action()
        inputStreamField.set(port, streams.first)//set input stream, will be used next time stream is grabbed to block on
        action()
        outputStreamField.set(port, streams.second)//now set our stream as default out
        action()
    }
    fun createFakeStreams(input: (ByteArray, Int, Int) -> Int, output: (ByteArray, Int, Int) -> Unit): Pair<FileInputStream, FileOutputStream> {
        val nullOutputStream = FileOutputStream("/dev/null")
        val nullFd = nullOutputStream.fd
        val fakeFileOutputStream = object : FileOutputStream(nullFd) {
            override fun write(b: ByteArray?, off: Int, len: Int) {
                output(b!!, off, len)
            }
        }
        val fakeFileInputStream = object : FileInputStream(nullFd) {
            override fun read(b: ByteArray?, off: Int, len: Int): Int {
                return input(b!!, off, len)
            }
        }
        return Pair(fakeFileInputStream, fakeFileOutputStream)
    }
    //class FakeRobotUSBDevice : RobotUsbDevice {

    //}
}