package dev.anygeneric.blazeftc_pedro;

import static org.firstinspires.ftc.robotcore.external.navigation.AngleUnit.normalizeRadians;

import com.pedropathing.Drivetrain;
import com.pedropathing.follower.Follower;
import com.pedropathing.ftc.localization.localizers.PinpointLocalizer;
import com.pedropathing.geometry.Pose;
import com.pedropathing.localization.Localizer;
import com.pedropathing.localization.PoseTracker;
import com.pedropathing.math.Vector;
import com.pedropathing.ftc.drivetrains.Mecanum;
import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver;
import com.qualcomm.robotcore.hardware.HardwareMap;

import java.util.concurrent.atomic.AtomicReference;

import dev.anygeneric.blazeftc.BlazeDummyPlug;
import dev.anygeneric.blazeftc.InterfaceAccessor;
import dev.anygeneric.blazeftc.InterfaceTest;
import dev.anygeneric.blazeftc.PositionData;

public class PedroSingleDataLocalizer implements Localizer {
	public static void setup_with_BulkWriteMecanum(HardwareMap hm, Follower follower, Runnable onNewData) {
		Drivetrain dt = follower.drivetrain;
		if (dt instanceof Mecanum) {
			Mecanum mc = (Mecanum) dt;
			//AtomicReference<String> ss = new AtomicReference<>("");
			//mc.getMotors().stream().map((it) -> InterfaceTest.motor_status(hm, it)).forEach((it) -> ss.set(ss + ", " + it));
			//System.out.println("");
			if (mc.getMotors().stream().allMatch((it) -> InterfaceTest.motor_status(hm, it) == InterfaceAccessor.ModuleStatus.Internal)) {
				follower.drivetrain = new BulkWriteMecanum(hm, mc);
			} else {
				System.out.println("Motors not all on ctrl hub, bulk not used!");
			}
		}
		setup(follower, onNewData);
	}
	public static void setup(Follower follower, Runnable onNewData) {
		Localizer localizer = follower.poseTracker.getLocalizer();
		if (!(localizer instanceof PinpointLocalizer)) {
			throw new IllegalArgumentException("This only works if you're using a pinpoint.");
		}
		GoBildaPinpointDriver ppd = ((PinpointLocalizer)localizer).getPinpoint();
		PedroSingleDataLocalizer sdl = new PedroSingleDataLocalizer();
		follower.poseTracker = new PoseTracker(sdl);
		BlazeDummyPlug.engagePinpointAcceleration(ppd, (pos) -> {
			sdl.lastData = pos;
			if (onNewData != null) {
				onNewData.run();
			}
			return null;
		});
	}
	public PositionData lastData = new PositionData();
	public Pose startPose = new Pose();

	@Override
	public Pose getPose() {
		double heading = normalizeRadians(lastData.getDirection() - startPose.getHeading());
		return new Pose(lastData.getXPosition() - startPose.getX(), lastData.getYPosition() - startPose.getY(), heading);
	}

	@Override
	public Pose getVelocity() {
		return new Pose(lastData.getXVelocity(), lastData.getYVelocity(), lastData.getAngVelocity());
	}

	@Override
	public Vector getVelocityVector() {
		return getVelocity().getAsVector();
	}

	/**
	 * This is not allowed because it would send an i2c packet, which breaks blaze
	 * (because blaze is running an i2c packet 100% of the time and there can't be two).
	 * Use setPose if you have to.
	 */
	@Override
	public void setStartPose(Pose setStart) {
		throw new RuntimeException("not implemented");
	}

	@Override
	public void setPose(Pose setPose) {
		//note: idk if this is right. lmk if i got it backwards
		//text: at 10, -5. Set to 1,1. startPose = 9, -6. Next will be 10-9=1, -5--6=1
		startPose = new Pose(lastData.getXPosition(), lastData.getYPosition(), lastData.getDirection()).minus(setPose);
	}

	@Override
	public void update() {
		//this doesn't do anything. we receive new data asynchronously
	}

	@Override
	public double getTotalHeading() {throw new RuntimeException("not implemented");}
	@Override
	public double getForwardMultiplier() {throw new RuntimeException("not implemented");}
	@Override
	public double getLateralMultiplier() {throw new RuntimeException("not implemented");}
	@Override
	public double getTurningMultiplier() {throw new RuntimeException("not implemented");}
	@Override
	public void resetIMU() {}
	@Override
	public double getIMUHeading() {throw new RuntimeException("not implemented");}
	@Override
	public boolean isNAN() {return false;}
}