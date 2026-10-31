package com.jnhprojects.mcheli.entity;

import com.jnhprojects.mcheli.MCHeli;
import com.jnhprojects.mcheli.content.aircraft.HelicopterDefinition;
import com.jnhprojects.mcheli.physics.AircraftRotation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A helicopter, ported from {@code MCH_EntityHeli}.
 *
 * <p>The flight model is the original one: throttle produces lift that weakens as the aircraft
 * tilts, and pitching the nose down converts throttle into forward speed through a cubic curve.
 * Hover mode holds full throttle, levels the aircraft and lets W/A/S/D slide it around instead.
 *
 * <p>Two deliberate differences from 1.7.10:
 * <ul>
 *   <li>The original ran roll damping once per rendered frame, so it depended on frame rate. It
 *       now runs once per tick with constants tuned to match the feel at 60 FPS.</li>
 *   <li>Blade folding, weapons, fuel and damage effects are not ported yet.</li>
 * </ul>
 */
public class HelicopterEntity extends AircraftEntity implements EngineSoundSource {
    /**
     * Rotation per mouse count. The original used {@code 0.06 * partialTicks} with the partial tick
     * low-pass filtered to roughly 0.5.
     */
    private static final float LOOK_SCALE = 0.03F;
    /** Vanilla turns the view 0.15 degrees per mouse count, so this recovers the raw count. */
    private static final float MOUSE_COUNTS_PER_DEGREE = 1.0F / 0.15F;
    private static final float ROLL_DAMPING = 0.06F;
    private static final float ROLL_DAMPING_HOVER = 0.1F;
    private static final float ROLL_RATE = 1.8F;
    private static final double MIN_SPEED = 0.07;

    private double currentSpeed = MIN_SPEED;
    private float rotorAngle;
    public float rotorAngleO;

    public HelicopterEntity(EntityType<? extends HelicopterEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected @Nullable HelicopterDefinition lookup(String name) {
        return MCHeli.content().helicopter(name);
    }

    @Override
    public @Nullable HelicopterDefinition definition() {
        return (HelicopterDefinition) super.definition();
    }

    @Override
    public boolean steersWithMouse() {
        return true;
    }

    @Override
    public float engineVolume() {
        HelicopterDefinition def = definition();
        return def != null && def.throttleUpDown <= 0.0F ? 0.0F : visibleThrottle() * 2.0F;
    }

    @Override
    public float enginePitch() {
        return 0.2F + visibleThrottle() * 0.2F;
    }

    /** Hovering or slow helicopters drop their gear sooner than ones passing over at speed. */
    @Override
    protected float autoLowerGearThrottle() {
        HelicopterDefinition def = definition();
        double dx = getX() - xo;
        double dy = getY() - yo;
        double dz = getZ() - zo;
        float slow = def != null ? def.speed / 3.5F : 0.0F;
        return dx * dx + dy * dy + dz * dz <= slow ? 0.8F : 0.3F;
    }

    public float rotorAngle(float partialTicks) {
        float prev = rotorAngleO;
        if (rotorAngle - prev < -180.0F) {
            prev -= 360.0F;
        } else if (prev - rotorAngle < -180.0F) {
            prev += 360.0F;
        }
        return prev + (rotorAngle - prev) * partialTicks;
    }

    /** Pilot toggles hover mode. Only allowed while fairly level and not in gunner mode, as in the original. */
    public void toggleHovering() {
        if (isHoveringMode()) {
            setHovering(false);
        } else if (!isGunnerMode() && isLevelEnough()) {
            setHovering(true);
        }
    }

    private boolean isLevelEnough() {
        return Math.abs(Mth.wrapDegrees(roll())) < 40.0F && Math.abs(Mth.wrapDegrees(getXRot())) < 40.0F;
    }

    /** The original also needed the blades unfolded; folding is not ported yet. */
    @Override
    public boolean canSwitchGunnerMode() {
        return super.canSwitchGunnerMode() && isLevelEnough();
    }

    @Override
    public void applyPilotLook(float dYaw, float dPitch) {
        HelicopterDefinition def = definition();
        if (def == null) {
            return;
        }
        float x = dYaw * MOUSE_COUNTS_PER_DEGREE;
        float y = dPitch * MOUSE_COUNTS_PER_DEGREE;

        float yaw = 0.0F;
        float pitch = 0.0F;
        float rollDelta = 0.0F;
        if (!isNearGround(2)) {
            yaw = clamp(x, 40.0F * def.mobilityYaw) * LOOK_SCALE;
        }
        // Standing on its skids or wheels a helicopter stays level; the mouse only tilts it in the air.
        if (!isHovering() && !onGround()) {
            pitch = clamp(y, 40.0F * def.mobilityPitch) * LOOK_SCALE;
            rollDelta = clamp(x, 40.0F * def.mobilityRoll) * LOOK_SCALE;
        }
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
        // Keep yaw continuous so the pilot's own yaw never jumps across the +-180 seam.
        setYRot(getYRot() + Mth.wrapDegrees(r[0] - getYRot()));
        setXRot(newPitch);
        roll = newRoll;
    }

    private static float clamp(float v, float limit) {
        return Mth.clamp(v, -limit, limit);
    }

    @Override
    protected void simulate() {
        HelicopterDefinition def = definition();
        boolean piloted = getControllingPassenger() != null;

        updateThrottle(def, piloted);
        updateAttitude(def);
        updateMotion(def);
    }

    /** {@code onUpdate_Control} and the hover/non-hover branches. */
    private void updateThrottle(HelicopterDefinition def, boolean piloted) {
        if (piloted) {
            Vec3 motion = getDeltaMovement();
            if (!isHovering()) {
                if (input.throttleUp) {
                    throttle = Math.min(1.0, throttle + 0.02 * def.throttleUpDown);
                } else if (input.throttleDown) {
                    throttle = Math.max(0.0, throttle - (1.0 / 70.0) * def.throttleUpDown);
                }
                motion = strafe(motion, false, input.left, input.right, 0.02 * def.speed);
            } else {
                throttle = Math.min(1.0, throttle + 1.0 / 30.0);
                motion = strafe(motion, input.throttleUp, input.left, input.right, 0.01 * def.speed);
                if (input.throttleDown) {
                    motion = push(motion, getYRot() - 180.0F, 0.01 * def.speed);
                }
            }
            setDeltaMovement(motion);
        } else {
            throttle = Math.max(0.0, throttle - 0.00125);
            if (isHoveringMode() && !level().isClientSide()) {
                setHovering(false);
            }
        }
    }

    private Vec3 strafe(Vec3 motion, boolean forward, boolean left, boolean right, double amount) {
        if (forward) {
            motion = push(motion, getYRot(), amount);
        }
        if (left && !right) {
            motion = push(motion, getYRot() - 90.0F, amount);
        } else if (right && !left) {
            motion = push(motion, getYRot() + 90.0F, amount);
        }
        return motion;
    }

    /** Adds {@code amount} of horizontal motion towards {@code yaw}, Minecraft style (0 = south). */
    private static Vec3 push(Vec3 motion, float yaw, double amount) {
        double rad = Math.toRadians(yaw);
        return motion.add(-Math.sin(rad) * amount, 0.0, Math.cos(rad) * amount);
    }

    /** Per-tick version of {@code onUpdateAngles}: roll levels out, A/D bank in the air. */
    private void updateAttitude(HelicopterDefinition def) {
        float damping = 1.0F - (isHovering() ? ROLL_DAMPING_HOVER : ROLL_DAMPING);
        if (Math.abs(roll) > 0.1F && Math.abs(roll) < 65.0F) {
            roll *= damping;
        }
        if (getControllingPassenger() != null && !isNearGround(3)) {
            if (input.left && !input.right) {
                roll -= ROLL_RATE;
            } else if (input.right && !input.left) {
                roll += ROLL_RATE;
            }
        }
        if (isHovering() && Math.abs(getXRot()) < 70.0F) {
            setXRot(getXRot() * 0.95F);
        }
        if (def.limitRotation) {
            roll = Mth.clamp(roll, def.minRotationRoll, def.maxRotationRoll);
        }
    }

    /** {@code onUpdate_Server}: gravity, lift, forward thrust, speed limit and ground friction. */
    private void updateMotion(HelicopterDefinition def) {
        Vec3 m = getDeltaMovement();
        double mx = m.x;
        double my = m.y;
        double mz = m.z;
        double prevMotion = Math.sqrt(mx * mx + mz * mz);
        float groundPitch = def.onGroundPitch;

        if (!isHovering()) {
            double depth = def.floats ? waterDepth() : 0.0;
            if (depth == 0.0) {
                my += isInWater() ? def.gravityInWater : def.gravity;
                double yawRad = Math.toRadians(getYRot());
                float pitch = getXRot();
                if (isNearGround(3)) {
                    pitch -= groundPitch;
                }
                double thrust = 0.1 * currentSpeed * (pitch * pitch * pitch / 30000.0F) * throttle;
                mx += Math.sin(yawRad) * -thrust;
                mz += Math.cos(yawRad) * thrust;

                double tilt = (Math.abs(getXRot()) + Math.abs(roll)) * 0.6;
                double lift = tilt <= 50.0 ? 1.0 - tilt / 50.0 : 0.0;
                my += (lift * 0.025 + 0.03) * throttle;
            } else {
                levelOut(groundPitch);
                if (depth < 1.0) {
                    my -= 1.0E-4;
                    my += 0.007 * throttle;
                } else {
                    if (my < 0.0) {
                        my *= 0.7;
                    }
                    my += 0.007;
                }
            }
        } else {
            if (random.nextInt(50) == 0) {
                mx += (random.nextDouble() - 0.5) / 30.0;
            }
            if (random.nextInt(50) == 0) {
                my += (random.nextDouble() - 0.5) / 50.0;
            }
            if (random.nextInt(50) == 0) {
                mz += (random.nextDouble() - 0.5) / 30.0;
            }
        }

        double motion = Math.sqrt(mx * mx + mz * mz);
        double limit = def.speed;
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

        if (onGround()) {
            mx *= 0.5;
            mz *= 0.5;
            levelOut(groundPitch);
        }

        move(MoverType.SELF, new Vec3(mx, my, mz));
        setDeltaMovement(mx * 0.99, my * 0.95, mz * 0.99);
    }

    private void levelOut(float groundPitch) {
        if (Math.abs(getXRot()) < 40.0F) {
            setXRot((getXRot() - groundPitch) * 0.9F + groundPitch);
        }
        if (Math.abs(roll) < 40.0F) {
            roll *= 0.9F;
        }
    }

    @Override
    protected void tickAnimation() {
        HelicopterDefinition def = definition();
        rotorAngleO = rotorAngle;
        float spool = 1.0F - visibleThrottle();
        rotorAngle = (rotorAngle + (1.0F - spool * spool * spool) * def.rotorSpeed) % 360.0F;
    }
}
