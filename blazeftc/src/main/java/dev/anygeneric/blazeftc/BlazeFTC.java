package dev.anygeneric.blazeftc;

import org.firstinspires.ftc.robotcore.external.Telemetry;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class BlazeFTC {
    public static int VERSION_LOADED;
    static {
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
    public static native void openFile(FileDescriptor fd, BlazeTelemetry blazeTelemetry);
    public static native void write(byte[] bytes);
    public static native void gamepad(byte[] gp1, byte[] gp2);
    public static native int available();
    public static native void run(int toRun);
    public static native int read(byte[] b, int off, int len);
    public static native void close();
    public static native void informOfModule(int module, boolean parent);
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
