package dev.anygeneric.blazeftc_pedro;

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

import dev.anygeneric.blazeftc.BlazeDummyPlug;
import dev.anygeneric.blazeftc.InterfaceAccessor;
import dev.anygeneric.blazeftc.InterfaceTest;
import dev.anygeneric.blazeftc.PositionData;

public class PedroSingleDataLocalizer implements Localizer {
	public static void setup_with_BulkWriteMecanum(HardwareMap hm, Follower follower, Runnable onNewData) {
		Drivetrain dt = follower.drivetrain;
		if (dt instanceof Mecanum) {
			Mecanum mc = (Mecanum) dt;
			if (mc.getMotors().stream().allMatch((it) -> InterfaceTest.motor_status(hm, it) == InterfaceAccessor.ModuleStatus.Internal)) {
				follower.drivetrain = new BulkWriteMecanum(hm, mc);
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

	@Override
	public Pose getPose() {
		return new Pose(lastData.getXPosition(), lastData.getYPosition(), lastData.getDirection());
	}

	@Override
	public Pose getVelocity() {
		return new Pose(lastData.getXVelocity(), lastData.getYVelocity(), lastData.getAngVelocity());
	}

	@Override
	public Vector getVelocityVector() {
		return getVelocity().getAsVector();
	}

	@Override
	public void setStartPose(Pose setStart) {
		throw new RuntimeException("not implemented");
	}

	@Override
	public void setPose(Pose setPose) {
		throw new RuntimeException("not implemented");
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