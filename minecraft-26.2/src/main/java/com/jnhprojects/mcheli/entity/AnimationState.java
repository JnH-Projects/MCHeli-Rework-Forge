package com.jnhprojects.mcheli.entity;

import com.jnhprojects.mcheli.content.aircraft.AircraftDefinition;
import com.jnhprojects.mcheli.content.aircraft.RackDefinition;
import com.jnhprojects.mcheli.content.aircraft.part.RotatingPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

/**
 * Purely visual state that every side advances once per tick: wheels, tracks, spinning parts and
 * where each crew member is looking relative to the hull.
 *
 * <p>Ported from {@code updatePartWheel}, {@code updatePartCrawlerTrack} and
 * {@code onUpdate_PartRotation}. Nothing in here affects physics, so it is never synced; each
 * client derives it from the synced throttle, input and crew rotations.
 */
public final class AnimationState {
    /** Road wheel roll in degrees. */
    public float wheelRoll;
    public float prevWheelRoll;
    /** Steering from -1 (full right) to 1 (full left). */
    public float steer;
    public float prevSteer;
    /** Track roller angle per side, index 1 is positive X. */
    public final float[] roller = new float[2];
    public final float[] prevRoller = new float[2];
    /** Track run per side, in link spacings from 0 to 1. */
    public final float[] track = new float[2];
    public final float[] prevTrack = new float[2];
    private final float[] trackSpeed = new float[2];
    public float[] rotatingParts = new float[0];
    public float[] prevRotatingParts = new float[0];
    public float throttle;
    public float prevThrottle;
    /** Per seat and rack: view yaw and pitch of the occupant relative to the hull. */
    public float[] seatYaw = new float[0];
    public float[] prevSeatYaw = new float[0];
    public float[] seatPitch = new float[0];
    public float[] prevSeatPitch = new float[0];

    void resize(AircraftDefinition def) {
        rotatingParts = new float[def.rotatingParts.size()];
        prevRotatingParts = new float[def.rotatingParts.size()];
        int seats = def.seatAndRackCount();
        seatYaw = new float[seats];
        prevSeatYaw = new float[seats];
        seatPitch = new float[seats];
        prevSeatPitch = new float[seats];
        for (int i = def.seatCount(); i < seats; i++) {
            RackDefinition rack = def.rack(i);
            seatYaw[i] = prevSeatYaw[i] = rack.yaw();
            seatPitch[i] = prevSeatPitch[i] = rack.pitch();
        }
    }

    void tick(AircraftEntity vehicle, AircraftDefinition def) {
        AircraftEntity.ControlInput input = vehicle.input();
        float visibleThrottle = vehicle.visibleThrottle();
        float reverse = vehicle.visibleReverse();
        prevThrottle = throttle;
        throttle = visibleThrottle;

        tickWheels(def, input, visibleThrottle, reverse);
        tickTracks(def, input, visibleThrottle, reverse);
        tickRotatingParts(def, vehicle.getControllingPassenger() != null);
        tickCrewAim(vehicle, def);
    }

    private void tickWheels(AircraftDefinition def, AircraftEntity.ControlInput input, double throttle, float reverse) {
        prevWheelRoll = wheelRoll;
        prevSteer = steer;
        if (def.enableBack && reverse > 0.01F && throttle <= 0.0) {
            throttle = -reverse * 15.0F;
        }
        if (input.left && !input.right) {
            steer = Math.min(steer + 0.1F, 1.0F);
        } else if (input.right && !input.left) {
            steer = Math.max(steer - 0.1F, -1.0F);
        } else {
            steer *= 0.9F;
        }
        wheelRoll += (float) (throttle * def.partWheelRot);
        if (wheelRoll >= 360.0F) {
            wheelRoll -= 360.0F;
            prevWheelRoll -= 360.0F;
        } else if (wheelRoll < 0.0F) {
            wheelRoll += 360.0F;
            prevWheelRoll += 360.0F;
        }
    }

    private void tickTracks(AircraftDefinition def, AircraftEntity.ControlInput input, double throttle, float reverse) {
        System.arraycopy(roller, 0, prevRoller, 0, 2);
        System.arraycopy(track, 0, prevTrack, 0, 2);
        double pivot = def.pivotTurnThrottle <= 0.0F ? 1.0 : def.pivotTurnThrottle * 0.1F;
        boolean left = input.left;
        boolean right = input.right;
        int dir = 1;
        if (def.enableBack && reverse > 0.0F && throttle <= 0.0) {
            throttle = -reverse * 5.0F;
            // Reversing swaps which track leads a turn.
            if (left != right) {
                boolean t = left;
                left = right;
                right = t;
                dir = -1;
            }
        }
        if (left && !right) {
            double t = 0.2 * dir;
            trackSpeed[0] += (float) t;
            trackSpeed[1] -= (float) (pivot * t);
        } else if (right && !left) {
            double t = 0.2 * dir;
            trackSpeed[0] -= (float) (pivot * t);
            trackSpeed[1] += (float) t;
        } else {
            float t = (float) Mth.clamp(throttle, -0.2, 0.2);
            trackSpeed[0] += t;
            trackSpeed[1] += t;
        }

        for (int i = 0; i < 2; i++) {
            trackSpeed[i] = Mth.clamp(trackSpeed[i], -0.72F, 0.72F);
            roller[i] += trackSpeed[i] * def.trackRollerRot;
            if (roller[i] >= 360.0F) {
                roller[i] -= 360.0F;
                prevRoller[i] -= 360.0F;
            } else if (roller[i] < 0.0F) {
                roller[i] += 360.0F;
                prevRoller[i] += 360.0F;
            }
            track[i] -= trackSpeed[i];
            while (track[i] >= 1.0F) {
                track[i]--;
                prevTrack[i]--;
            }
            while (track[i] < 0.0F) {
                track[i]++;
            }
            while (prevTrack[i] < 0.0F) {
                prevTrack[i]++;
            }
            trackSpeed[i] *= 0.75F;
        }
    }

    private void tickRotatingParts(AircraftDefinition def, boolean piloted) {
        for (int i = 0; i < rotatingParts.length && i < def.rotatingParts.size(); i++) {
            RotatingPart part = def.rotatingParts.get(i);
            prevRotatingParts[i] = rotatingParts[i];
            if (part.always() || piloted) {
                float r = rotatingParts[i] + part.speed();
                if (r < 0.0F) {
                    r += 360.0F;
                    prevRotatingParts[i] += 360.0F;
                } else if (r >= 360.0F) {
                    r -= 360.0F;
                    prevRotatingParts[i] -= 360.0F;
                }
                rotatingParts[i] = r;
            }
        }
    }

    private void tickCrewAim(AircraftEntity vehicle, AircraftDefinition def) {
        float hullYaw = vehicle.getYRot();
        float hullPitch = vehicle.getXRot();
        float hullRoll = vehicle.roll();
        for (int i = 0; i < seatYaw.length && i < def.seatCount(); i++) {
            prevSeatYaw[i] = seatYaw[i];
            prevSeatPitch[i] = seatPitch[i];
            Entity crew = vehicle.occupant(i);
            if (crew == null) {
                continue;
            }
            float yaw = Mth.wrapDegrees(crew.getYRot() - hullYaw);
            double rad = Math.toRadians(yaw);
            // The hull tilt along the crew member's view direction, as in the original tank code.
            float tilt = (float) (hullPitch * Math.cos(rad) + hullRoll * Math.sin(rad));
            float pitch = crew.getXRot() - tilt;
            // Keep the previous value on the same side of the seam so interpolation never spins.
            prevSeatYaw[i] = yaw + Mth.wrapDegrees(prevSeatYaw[i] - yaw);
            seatYaw[i] = yaw;
            seatPitch[i] = pitch;
        }
    }

    public float seatYaw(int seat, float partialTicks) {
        if (seat < 0 || seat >= seatYaw.length) {
            return 0.0F;
        }
        return prevSeatYaw[seat] + (seatYaw[seat] - prevSeatYaw[seat]) * partialTicks;
    }

    public float seatPitch(int seat, float partialTicks) {
        if (seat < 0 || seat >= seatPitch.length) {
            return 0.0F;
        }
        return prevSeatPitch[seat] + (seatPitch[seat] - prevSeatPitch[seat]) * partialTicks;
    }
}
