package dev.anygeneric.blazeftc_pedro;

import com.pedropathing.follower.Follower;
import com.pedropathing.localization.Localizer;
import com.pedropathing.localization.MotionState;
import com.pedropathing.math.Pose;
import com.pedropathing.math.Velocity;
import com.pedropathing.revhub.localizers.Pinpoint;
import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver;

import java.lang.reflect.Field;

import dev.anygeneric.blazeftc.BlazeDummyPlug;

public class Pedro3SingleDataLocalizer implements Localizer {
	public MotionState motionState;
	public Pedro3SingleDataLocalizer(MotionState motionState) {
		this.motionState = motionState;
	}
	public void update(MotionState motionState) {
		this.motionState = motionState;
	}

	@Override
	public void setPose(Pose setPose) {
		throw new RuntimeException("not implemented");
	}

	@Override
	public MotionState motionState() {
		return motionState;
	}

	@Override
	public void update() { /*no-op*/ }

	@Override
	public void reset() { /*no-op*/ }

	public static void setup(Follower follower, Runnable onNewData) {
		setupAtFrequency(follower, -1, onNewData);
	}
	public static void setupAtFrequency(Follower follower, int frequency, Runnable onNewData) {
		Localizer localizer = follower.localizer;
		if (!(localizer instanceof Pinpoint)) {
			throw new IllegalArgumentException("This only works if you're using a pinpoint.");
		}
		//    private final GoBildaPinpointDriver odometry;
		GoBildaPinpointDriver ppd = null;
		try {
			Field f = Pinpoint.class.getField("odometry");
			f.setAccessible(true);
			ppd = (GoBildaPinpointDriver) f.get(localizer);
			Pedro3SingleDataLocalizer sdl = new Pedro3SingleDataLocalizer(MotionState.zero());
			Field f1 = Follower.class.getField("localizer");
			f1.setAccessible(true);
			f1.set(follower, sdl);
			BlazeDummyPlug.engagePinpointAccelerationAtFrequency(ppd, frequency, (pos) -> {
				Pose pose = new Pose(pos.getXPosition(), pos.getYPosition(), pos.getDirection());
				Velocity vel = new Velocity(pos.getXVelocity(), pos.getYVelocity(), pos.getAngVelocity());
				sdl.motionState = MotionState.ofVelocity(pose, vel);
				if (onNewData != null) {
					onNewData.run();
				}
				return null;
			});
		} catch (Throwable t) {
			throw new RuntimeException(t);
		}
	}

}