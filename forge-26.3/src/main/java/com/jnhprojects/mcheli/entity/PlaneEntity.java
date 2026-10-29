package com.jnhprojects.mcheli.entity;

import com.jnhprojects.mcheli.MCHeli;
import com.jnhprojects.mcheli.content.aircraft.AircraftDefinition;
import com.jnhprojects.mcheli.content.aircraft.PlaneDefinition;
import com.jnhprojects.mcheli.physics.AircraftRotation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A fixed wing aircraft, ported from {@code MCP_EntityPlane}.
 *
 * <p>Throttle pushes the plane along its nose, tilted ten degrees up, and the vertical part of
 * that push is what keeps it in the air: below full throttle it sinks. VTOL capable planes tilt
 * their nozzles (and tilt rotors) towards straight down, which turns the push into lift.
 *
 * <p>The original applied its attitude rules once per rendered frame with the partial tick
 * filtered to about 0.5. Here they run three times per tick with that value, which is what a
 * 60 FPS client did, so the handling is the same without depending on the frame rate.
 */
public class PlaneEntity extends AircraftEntity implements EngineSoundSource {
    private static final float LOOK_SCALE = 0.03F;
    private static final float MOUSE_COUNTS_PER_DEGREE = 1.0F / 0.15F;
    private static final int FRAMES_PER_TICK = 3;
    private static final float FRAME_PARTIAL_TICK = 0.5F;
    private static final double MIN_SPEED = 0.07;

    private @Nullable PartState nozzle;
    private @Nullable PartState wing;

    private double currentSpeed = MIN_SPEED;
    private float keyRoll;
    private float rotorAngle;
    public float rotorAngleO;
    private float soundLevel;

    public PlaneEntity(EntityType<? extends PlaneEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected @Nullable PlaneDefinition lookup(String name) {
        return MCHeli.content().plane(name);
    }

    @Override
    public @Nullable PlaneDefinition definition() {
        return (PlaneDefinition) super.definition();
    }

    @Override
    protected void onDefinitionChanged(AircraftDefinition definition) {
        super.onDefinitionChanged(definition);
        PlaneDefinition def = (PlaneDefinition) definition;
        nozzle = def.hasNozzleState() ? addPart(PART_NOZZLE, 1.5F, PartState.Sounds.NOZZLE) : null;
        wing = def.wings.isEmpty() ? null : addPart(PART_WING, 2.5F, PartState.Sounds.WING);
        if (nozzle != null && def.defaultVtol && !nozzle.status()) {
            nozzle.force(true);
        }
    }

    @Override
    public boolean steersWithMouse() {
        return true;
    }

    public @Nullable PartState nozzle() {
        return nozzle;
    }

    public @Nullable PartState wing() {
        return wing;
    }

    public float nozzleAngle() {
        return nozzle != null ? nozzle.angle() : 0.0F;
    }

    @Override
    public int vtolMode() {
        if (nozzle == null) {
            return 0;
        }
        if (!nozzle.status()) {
            return nozzle.angle() <= 0.005F ? 0 : 1;
        }
        return nozzle.angle() >= 89.995F ? 2 : 1;
    }

    public float rotorAngle(float partialTicks) {
        return rotorAngleO + (rotorAngle - rotorAngleO) * partialTicks;
    }

    // Pilot actions, server side

    public boolean canSwitchVtol() {
        PlaneDefinition def = definition();
        if (def == null || !def.vtol || nozzle == null || modeSwitchCooldown > 0 || vtolMode() == 1) {
            return false;
        }
        return Math.abs(roll) <= 30.0F && !(onGround() && def.defaultVtol);
    }

    public void toggleVtol() {
        if (canSwitchVtol()) {
            modeSwitchCooldown = 20;
            setVtol(!nozzle.status());
        }
    }

    private void setVtol(boolean on) {
        PlaneDefinition def = definition();
        if (nozzle == null || def == null || (def.defaultVtol && onGround() && !on)) {
            return;
        }
        nozzle.set(on, true);
    }

    public boolean canFoldWing() {
        PlaneDefinition def = definition();
        if (wing == null || def == null || modeSwitchCooldown > 0) {
            return false;
        }
        if (def.variableSweepWing) {
            // Sweep back only when fast and well clear of the ground, spread again only when slow.
            if (!onGround() && !hasBlockBelow(3, 20, true)) {
                if (throttle < 0.7F) {
                    return false;
                }
            } else if (throttle > 0.1F) {
                return false;
            }
        } else {
            if (!onGround() && !hasBlockBelow(3, 3, true)) {
                return false;
            }
            if (throttle > 0.01F) {
                return false;
            }
        }
        return wing.isOff();
    }

    public boolean canUnfoldWing() {
        return wing != null && modeSwitchCooldown <= 0 && wing.isOn();
    }

    @Override
    public void toggleFold() {
        PlaneDefinition def = definition();
        if (def == null) {
            return;
        }
        if (!def.hatches.isEmpty()) {
            super.toggleFold();
        } else if (canFoldWing()) {
            wing.set(true, true);
            modeSwitchCooldown = 20;
        } else if (canUnfoldWing()) {
            wing.set(false, true);
            modeSwitchCooldown = 20;
        }
    }

    /** Folded wings ground the plane; swept wings only below 20% throttle. */
    private boolean canUseWing(PlaneDefinition def) {
        if (wing == null) {
            return true;
        }
        if (def.variableSweepWing) {
            return throttle >= 0.2 || wing.isOff();
        }
        return wing.isOff();
    }

    @Override
    protected float autoLowerGearThrottle() {
        return 0.7F;
    }

    @Override
    protected void updatePartsServer(AircraftDefinition definition) {
        super.updatePartsServer(definition);
        PlaneDefinition def = (PlaneDefinition) definition;
        // Swept wings spread out again when the plane slows down or gets close to the ground.
        if (wing != null && def.variableSweepWing && wing.isOn() && throttle >= 0.2F
                && (throttle < 0.5 || hasBlockBelow(1, 10, true))) {
            wing.set(false, true);
        }
        if (nozzle != null && def.defaultVtol && onGround() && vtolMode() == 0) {
            nozzle.set(true, true);
        }
    }

    // Mouse

    @Override
    public void applyPilotLook(float dYaw, float dPitch) {
        PlaneDefinition def = definition();
        if (def == null || ticksAlive() < 30 || isPassenger() || hasBlockBelow(3, 2, true)) {
            return;
        }
        float x = dYaw * MOUSE_COUNTS_PER_DEGREE;
        float y = dPitch * MOUSE_COUNTS_PER_DEGREE;
        boolean vtol = vtolMode() > 0;
        float yawFactor = (vtol ? def.vtolYaw : 1.0F) * 0.8F;
        float pitchFactor = (vtol ? def.vtolPitch : 1.0F) * 0.8F;
        float rollInput = vtolMode() == 0 ? x * 0.5F : x;

        float yaw = clamp(x, 40.0F * def.mobilityYaw) * yawFactor * LOOK_SCALE;
        float pitch = clamp(y, 40.0F * def.mobilityPitch) * pitchFactor * LOOK_SCALE;
        float rollDelta = clamp(rollInput, 40.0F * def.mobilityRoll) * yawFactor * LOOK_SCALE;
        if (yaw == 0.0F && pitch == 0.0F && rollDelta == 0.0F) {
            return;
        }
        float[] r = AircraftRotation.applyLocal(getYRot(), getXRot(), roll, yaw, pitch, rollDelta);
        float newPitch = r[1];
        float newRoll = r[2];
        if (def.limitRotation) {
            newPitch = Mth.clamp(newPitch, def.minRotationPitch, def.maxRotationPitch);
            newRoll = Mth.clamp(newRoll, def.minRotationRoll, def.maxRotationRoll);
        }
        setYRot(getYRot() + Mth.wrapDegrees(r[0] - getYRot()));
        setXRot(newPitch);
        roll = newRoll;
    }

    private static float clamp(float v, float limit) {
        return Mth.clamp(v, -limit, limit);
    }

    // Simulation

    @Override
    protected void simulate() {
        PlaneDefinition def = definition();
        boolean piloted = getControllingPassenger() != null;
        if (def.targetDrone && !piloted) {
            input.throttleUp = true;
        }
        updateThrottle(def, piloted);
        if (piloted) {
            for (int i = 0; i < FRAMES_PER_TICK; i++) {
                updateAttitude(def, FRAME_PARTIAL_TICK);
            }
        }
        updateMotion(def);
    }

    /**
     * Gunner mode needs a plane in level flight with throttle up and nothing solid within five
     * blocks below, so the autopilot has room to circle.
     */
    @Override
    public boolean canSwitchGunnerMode() {
        if (!super.canSwitchGunnerMode()) {
            return false;
        }
        float r = Math.abs(Mth.wrapDegrees(roll()));
        float p = Math.abs(Mth.wrapDegrees(getXRot()));
        return r <= 40.0F && p <= 40.0F && visibleThrottle() > 0.6F && !hasBlockBelow(3, 5, false);
    }

    /** {@code onUpdate_Control} and {@code onUpdate_ControlNotHovering}. */
    private void updateThrottle(PlaneDefinition def, boolean piloted) {
        reverse *= 0.8F;
        boolean canFly = piloted && isCanopyClosed() && canUseWing(def);
        if (canFly && isGunnerMode()) {
            // The autopilot holds the throttle the pilot left it at.
            return;
        }
        if (canFly || def.targetDrone) {
            float upDown = def.throttleUpDown;
            boolean turning = input.left != input.right;
            boolean up = input.throttleUp;
            if (turning && throttle < def.pivotTurnThrottle && !up && !input.throttleDown) {
                up = true;
                upDown *= 2.0F;
            }
            if (up) {
                if (def.enableBack && reverse > 0.0F) {
                    reverse -= 0.01F * upDown;
                } else {
                    reverse = 0.0F;
                    throttle = Math.min(throttle + 0.01 * upDown, 1.0);
                }
            } else if (input.throttleDown) {
                if (throttle > 0.0) {
                    throttle -= 0.01 * upDown;
                } else {
                    throttle = 0.0;
                    if (def.enableBack) {
                        reverse = Math.min(reverse + 0.0025F * upDown, 0.6F);
                    }
                }
            }
        } else if (throttle > 0.0) {
            throttle -= 0.0025 * def.throttleUpDown;
        }
        throttle = Math.max(throttle, 0.0);
    }

    /** One frame of {@code onUpdateAngles}. In gunner mode the autopilot levels out and circles by {@code AutoPilotRot}. */
    private void updateAttitude(PlaneDefinition def, float pt) {
        boolean gunner = isGunnerMode();
        if (gunner) {
            setXRot(getXRot() * 0.95F);
            setYRot(getYRot() + def.autoPilotRot * 0.2F);
            if (Math.abs(roll) > 20.0F) {
                roll *= 0.95F;
            }
        }
        boolean flying = !hasBlockBelow(3, 3, true);
        if (!flying || input.freeLook || gunner || (def.floats && waterDepth() > 0.0)) {
            float yawRate = 1.0F;
            if (!flying) {
                yawRate = def.mobilityYawOnGround;
                if (!def.canRotateOnGround && isOnSolidGround()) {
                    yawRate = 0.0F;
                }
            }
            if (input.left && !input.right) {
                setYRot(getYRot() - 0.6F * yawRate * pt);
            } else if (input.right && !input.left) {
                setYRot(getYRot() + 0.6F * yawRate * pt);
            }
        } else {
            // A/D bank the plane; in VTOL mode the keys do nothing in the air.
            float rate = vtolMode() != 0 ? 0.0F : 0.2F;
            if (input.left && !input.right) {
                keyRoll -= rate * pt;
            } else if (input.right && !input.left) {
                keyRoll += rate * pt;
            }
            roll += keyRoll * 0.5F * def.mobilityRoll;
        }
        keyRoll *= 1.0F - 0.1F * pt;
        if (!flying && Math.abs(getXRot()) < 40.0F) {
            applyOnGroundPitch(0.97F);
        }
        if (nozzleAngle() > 0.001F) {
            setXRot(getXRot() * (1.0F - 0.03F * pt));
            roll *= 1.0F - 0.1F * pt;
        }
        roll = AircraftRotation.wrap(roll);
        if (def.limitRotation) {
            setXRot(Mth.clamp(getXRot(), def.minRotationPitch, def.maxRotationPitch));
            roll = Mth.clamp(roll, def.minRotationRoll, def.maxRotationRoll);
        }
    }

    private float maxSpeed(PlaneDefinition def) {
        float extra = 0.0F;
        if (def.variableSweepWing) {
            if (wing != null) {
                extra = (def.sweepWingSpeed - def.speed) * wing.factor();
            } else if (hatch != null) {
                extra = (def.sweepWingSpeed - def.speed) * hatch.factor();
            }
        }
        return def.speed + extra;
    }

    /** {@code onUpdate_Server}. */
    private void updateMotion(PlaneDefinition def) {
        Vec3 m = getDeltaMovement();
        double mx = m.x;
        double my = m.y;
        double mz = m.z;
        double prevMotion = Math.sqrt(mx * mx + mz * mz);
        double depth = def.floats ? waterDepth() : 0.0;
        // The gunner mode autopilot holds its altitude.
        boolean levelOff = isGunnerMode();

        if (depth == 0.0) {
            if (def.targetDrone) {
                levelOff = targetDroneAutopilot(def) || levelOff;
            }
            if (!levelOff) {
                my += 0.04 + (isInWater() ? def.gravityInWater : def.gravity);
                my += -0.047 * (1.0 - throttle);
            } else {
                my *= 0.8;
            }
        } else {
            setXRot(getXRot() * 0.8F);
            if (Math.abs(roll) < 40.0F) {
                roll *= 0.9F;
            }
            if (depth < 1.0) {
                my -= 1.0E-4;
                my += 0.007 * throttle;
            } else {
                if (my < 0.0) {
                    my /= 2.0;
                }
                my += 0.007;
            }
        }

        double push = throttle / 10.0;
        float nozzleAngle = nozzleAngle();
        Vec3 v;
        if (nozzleAngle > 0.001F) {
            setXRot(getXRot() * 0.95F);
            v = direction(getYRot(), getXRot() - nozzleAngle);
            if (nozzleAngle >= 90.0F) {
                v = new Vec3(v.x * 0.8, v.y, v.z * 0.8);
            }
        } else {
            v = direction(getYRot(), getXRot() - 10.0F);
        }
        if (!levelOff) {
            my += v.y * push / (nozzleAngle <= 0.01F ? 2.0 : 8.0);
        }

        boolean canMove = def.canMoveOnGround || !isOnSolidGround();
        if (canMove) {
            if (def.enableBack && reverse > 0.0F) {
                mx -= v.x * reverse;
                mz -= v.z * reverse;
            } else {
                mx += v.x * push;
                mz += v.z * push;
            }
        }

        double motion = Math.sqrt(mx * mx + mz * mz);
        float limit = maxSpeed(def);
        if (motion > limit) {
            mx *= limit / motion;
            mz *= limit / motion;
            motion = limit;
        }
        if (motion > prevMotion && currentSpeed < limit) {
            currentSpeed = Math.min(limit, currentSpeed + (limit - currentSpeed) / 35.0);
        } else {
            currentSpeed = Math.max(MIN_SPEED, currentSpeed - (currentSpeed - MIN_SPEED) / 35.0);
        }

        if (onGround() || hasBlockBelow(1, 2, true)) {
            mx *= def.motionFactor;
            mz *= def.motionFactor;
            if (Math.abs(getXRot()) < 40.0F) {
                applyOnGroundPitch(0.8F);
            }
        }

        move(MoverType.SELF, new Vec3(mx, my, mz));
        setDeltaMovement(mx * def.motionFactor, my * 0.95, mz * def.motionFactor);
    }

    /** Target drones circle at a safe height on their own. Returns whether to hold altitude. */
    private boolean targetDroneAutopilot(PlaneDefinition def) {
        if (hasBlockBelow(3, 40, true)) {
            if (!hasBlockBelow(3, 5, true)) {
                setYRot(getYRot() + def.autoPilotRot * 2.0F);
                if (getXRot() > -20.0F) {
                    setXRot(getXRot() - 0.5F);
                }
            }
            return false;
        }
        setYRot(getYRot() + def.autoPilotRot);
        setXRot(getXRot() * 0.95F);
        if (!level().isClientSide() && canRaiseLandingGear() && modeSwitchCooldown <= 0) {
            toggleLandingGear();
        }
        return true;
    }

    /** {@code MCH_Lib.Rot2Vec3}: unit vector along a yaw and pitch, Minecraft conventions. */
    private static Vec3 direction(float yaw, float pitch) {
        double y = Math.toRadians(yaw);
        double p = Math.toRadians(pitch);
        return new Vec3(-Math.sin(y) * Math.cos(p), -Math.sin(p), Math.cos(y) * Math.cos(p));
    }

    // Animation and sound

    @Override
    protected void tickAnimation() {
        PlaneDefinition def = definition();
        float throttle = visibleThrottle();
        rotorAngleO = rotorAngle;
        rotorAngle += throttle * def.rotorSpeed;
        if (rotorAngle > 360.0F) {
            rotorAngle -= 360.0F;
            rotorAngleO -= 360.0F;
        } else if (rotorAngle < 0.0F) {
            rotorAngle += 360.0F;
            rotorAngleO += 360.0F;
        }

        float target = throttle;
        if (getControllingPassenger() != null && (canopy == null || canopy.angle() < 1.0F)) {
            target += 0.1F;
        }
        soundLevel = soundLevel < target ? Math.min(soundLevel + 0.02F, target) : Math.max(soundLevel - 0.02F, target);
    }

    @Override
    public float engineVolume() {
        PlaneDefinition def = definition();
        return def != null && def.throttleUpDown <= 0.0F ? 0.0F : soundLevel * 0.7F;
    }

    @Override
    public float enginePitch() {
        return 0.6F + visibleThrottle() * 0.4F;
    }
}
