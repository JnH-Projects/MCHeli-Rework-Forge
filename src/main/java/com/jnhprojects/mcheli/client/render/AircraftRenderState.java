package com.jnhprojects.mcheli.client.render;

import com.jnhprojects.mcheli.content.aircraft.AircraftDefinition;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.jspecify.annotations.Nullable;

/**
 * Snapshot of a vehicle for one frame, filled in by {@link AircraftRenderer#extractRenderState}.
 * Every angle is already interpolated for the frame's partial tick.
 */
public class AircraftRenderState extends EntityRenderState {
    /** Definitions are immutable after loading, so the snapshot can share them. */
    public @Nullable AircraftDefinition definition;
    public String texture = "";
    public float yaw;
    public float pitch;
    public float roll;
    public float hurtTime;
    /** Helicopter main rotor or plane propeller angle. */
    public float rotorAngle;
    public float throttle;

    // Part states, 0 to 90 degrees
    public float canopy;
    public float landingGear;
    public float hatch;
    public float nozzle;
    public float wing;

    public float wheelRoll;
    public float steer;
    public final float[] roller = new float[2];
    /** Track run per side from 0 to 1, in link spacings. */
    public final float[] track = new float[2];
    public float[] rotatingParts = new float[0];

    /** Per seat: occupant view relative to the hull, and whether anyone sits there. */
    public float[] seatYaw = new float[0];
    public float[] seatPitch = new float[0];
    public boolean[] seatOccupied = new boolean[0];
    /** Seat of the player at this client, or -1, and whether they look through their own eyes. */
    public int localSeat = -1;
    public boolean localFirstPerson;
    /** Shade for the unlit glass, from the light at the vehicle. */
    public int glassColor = 0xFFFFFFFF;

    /** Per {@code AddPartWeapon} line, the state of the weapon it belongs to; see {@link WeaponPartState}. */
    public WeaponPartState[] weaponParts = new WeaponPartState[0];
    /** Per weapon bay, 0 to 90 degrees. */
    public float[] bays = new float[0];

    /** How one weapon part is drawn this frame. */
    public static final class WeaponPartState {
        /** Whether the part's weapon exists; without one the part follows its operator's view. */
        public boolean hasWeapon;
        /** Aim relative to the weapon's default direction. */
        public float yaw;
        public float pitch;
        public float defaultYaw;
        /** How far a turret weapon lags behind the pilot's view. */
        public float turretLag;
        public float recoil;
        public float barrel;
        /** A missile part whose round is on its way and not reloaded yet. */
        public boolean hidden;
    }
}
