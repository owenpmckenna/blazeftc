package dev.anygeneric.blazeftc;

import com.qualcomm.robotcore.hardware.usb.ftdi.RobotUsbDeviceFtdi;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.robotcore.internal.usb.exception.RobotUsbException;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

public class BlazeFTC {
    public static int VERSION_LOADED = -1;
    public static void load() {
        if (VERSION_LOADED != -1) {
            return;
        }
        try {
            System.loadLibrary("blaze_ftc");
            System.out.println("loaded blaze_ftc!");
            VERSION_LOADED = 0;
        } catch (UnsatisfiedLinkError ule) {
            try {
                System.loadLibrary("blaze_ftc_quickstart");
                System.out.println("loaded blaze_ftc_quickstart!");
                VERSION_LOADED = 1;
            } catch (UnsatisfiedLinkError ule2) {
                System.loadLibrary("blaze_ftc_neutrino");
                System.out.println("loaded blaze_ftc_neutrino!");
                VERSION_LOADED = 2;
            }
        }
    }

    /**
     * order: inform, initialize, write/read
     */
    public static native void initialize(BlazeTelemetry blazeTelemetry);
    public static native void write(byte[] bytes, int connectionNumber);
    public static native void gamepad(byte[] gp1, byte[] gp2);
    public static native void sendProperty(String key, String value);
    public static native int available();
    public static native void run(int toRun);
    public static native void setMotorPower(int hub, int port, double power);
    public static native void setMotorPowers(int hub, double power0, double power1, double power2, double power3);
    public static native int read(byte[] b, int off, int len, int connectionNumber);//added conn number
    public static native void close();
    public static native void informOfModule(int module, boolean parent, FileDescriptor fd);
    public static native void informOfServoHub(int module, int parent);
    public static RobotUsbDeviceFtdi usb;
    public static void writeToUsb(byte[] b) throws RobotUsbException, InterruptedException {
        usb.write(b);
    }
    public static void readFromUsbExact(byte[] bytes, int off, int len) throws RobotUsbException, InterruptedException {
        int total = 0;
        while (total < len) {
            //read(byte[], offset, len, timeout, ignore)
            int remaining = len - total;
            int read = usb.read(bytes, total + off, remaining, 30_000, null);
            if (read < 0)
                throw new RuntimeException("reading failed");
            total += read;
        }
    }
    public interface ByteHandler {
        byte[] handle(byte[] b);
    }
    private static Map<String, ByteHandler> handlerMap = new HashMap<>();
    public static void setByteHandler(String name, ByteHandler closure) {
        handlerMap.put(name, closure);
    }
    public static void clearByteHandlers() {handlerMap.clear();}
    public static byte[] sendBytes(String name, byte[] bytes) {
        ByteHandler bh = handlerMap.get(name);
        if (bh != null) {
            return bh.handle(bytes);
        }
        return new byte[] {};//empty array idk. probably not the right call
    }
    public static OutputStream os = null;
    public static InputStream is = null;
    public static void closeStreams() {
        System.out.println("closing streams...");
        if (os != null) {
            try {
                os.close();
                is.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
    public static BlazeTelemetry bt = null;
    public static class BlazeTelemetry {
        public Telemetry telemetry;
        public CachedTelemetry ct = null;
        public BlazeTelemetry(Telemetry tele) {
            telemetry = tele;
        }
        public void update() {
            if (ct != null) {
                ct.updateToTelemetry(telemetry);
            }
            telemetry.update();
        }
        public void addData(String name, Object data) {
            telemetry.addData(name, data);
        }
        public void addData(String name, long data) {
            telemetry.addData(name, data);
        }
        public void addData(String name, double data) {
            telemetry.addData(name, data);
        }
    }
}
