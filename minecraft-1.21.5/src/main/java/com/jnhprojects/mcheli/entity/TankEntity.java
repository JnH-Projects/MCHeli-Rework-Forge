package com.jnhprojects.mcheli.entity;

import com.jnhprojects.mcheli.MCHeli;
import com.jnhprojects.mcheli.content.Vector3;
import com.jnhprojects.mcheli.content.aircraft.AircraftDefinition;
import com.jnhprojects.mcheli.content.aircraft.TankDefinition;
import com.jnhprojects.mcheli.physics.GroundImpact;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Tanks and cars, ported from {@code MCH_EntityTank}.
 *
 * <p>The drive model is the plane's minus lift: throttle pushes along the hull, A/D turn it,
 * S brakes and then reverses when {@code EnableBack} is set, and Space is a hand brake. The hull
 * does not follow the mouse. Instead a {@link TerrainFollower} reads the terrain under the
 * wheels and the hull eases towards that pitch and roll, while the crew look around freely and
 * the turret follows their view.
 */
public class TankEntity extends AircraftEntity implements EngineSoundSource {
    private static final int FRAMES_PER_TICK = 3;
    private static final float FRAME_PARTIAL_TICK = 0.5F;
    private static final double MIN_SPEED = 0.07;

    private final TerrainFollower terrain = new TerrainFollower(this);
    private double currentSpeed = MIN_SPEED;
    /**
     * Squared horizontal distance covered by last tick's move. The original read
     * {@code posX - prevPosX} here, but vanilla resets {@code xo} before {@link #simulate()} runs,
     * which left vehicles with {@code PivotTurnThrottle} unable to steer.
     */
    private double lastStepSq;
    private float soundLevel;
    private float turnSound;

    public TankEntity(EntityType<? extends TankEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected @Nullable TankDefinition lookup(String name) {
        return MCHeli.content().tank(name);
    }

    @Override
    public @Nullable TankDefinition definition() {
        return (TankDefinition) super.definition();
    }

    @Override
    protected void onDefinitionChanged(AircraftDefinition definition) {
        super.onDefinitionChanged(definition);
        TankDefinition def = (TankDefinition) definition;
        terrain.create(def.groundWheels, new Vector3(0.0, -0.35, def.weightedCenterZ));
    }

    /** Walls and long drops hurt tanks and cars, scaled by speed and height. */
    @Override
    protected boolean takesGroundImpacts() {
        return true;
    }

    /** Tanks climb and tilt on slopes all the time; only rolling over damages them. */
    @Override
    protected float crashTilt() {
        return 91.0F;
    }

    @Override
    protected void simulate() {
        TankDefinition def = definition();
        boolean piloted = getControllingPassenger() != null;
        updateThrottle(def, piloted);
        if (piloted) {
            for (int i = 0; i < FRAMES_PER_TICK; i++) {
                updateAttitude(def, FRAME_PARTIAL_TICK);
            }
            if (def.limitRotation) {
                setXRot(Mth.clamp(getXRot(), -90.0F, 90.0F));
                roll = Mth.clamp(roll, -90.0F, 90.0F);
            }
        }
        updateMotion(def, piloted);
    }

    /** {@code onUpdate_Control} and {@code onUpdate_ControlSub}. */
    private void updateThrottle(TankDefinition def, boolean piloted) {
        reverse *= 0.8F;
        float upDown = def.throttleUpDown;
        if (input.brake) {
            reverse *= 0.5F;
            throttle = Math.max(throttle - 0.02 * upDown, 0.0);
        }
        if ((piloted && isCanopyClosed()) || def.targetDrone) {
            if (input.throttleUp || (def.targetDrone && !piloted)) {
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
            throttle -= 0.0025 * upDown;
        }
        throttle = Math.max(throttle, 0.0);
    }

    /** One frame of {@code onUpdateAngles}: ease onto the terrain, steer with A/D. */
    private void updateAttitude(TankDefinition def, float pt) {
        setXRot(getXRot() + (terrain.targetPitch - getXRot()) * pt);
        roll += (terrain.targetRoll - roll) * pt;

        boolean flying = !hasBlockBelow(3, 3, true);
        if (flying && !(def.floats && waterDepth() > 0.0)) {
            return;
        }
        float yawRate = 1.0F;
        if (!flying) {
            yawRate = def.mobilityYawOnGround;
            if (!def.canRotateOnGround && isOnSolidGround()) {
                yawRate = 0.0F;
            }
        }
        float pivot = def.pivotTurnThrottle;
        double dist = lastStepSq;
        // With PivotTurnThrottle set the vehicle has to be rolling before it can turn, like a car.
        if (pivot <= 0.0F || throttle >= pivot || reverse >= pivot / 10.0F || dist > reverse * 0.01) {
            float speedScale = pivot <= 0.0F ? 1.0F : (float) Math.sqrt(Math.min(dist, 1.0));
            float direction = !input.throttleUp && input.throttleDown && throttle < pivot + 0.05 ? -1.0F : 1.0F;
            float step = 0.6F * yawRate * pt * direction * speedScale;
            if (input.left && !input.right) {
                setYRot(getYRot() - step);
            } else if (input.right && !input.left) {
                setYRot(getYRot() + step);
            }
        }
    }

    /** {@code onUpdate_Server}. */
    private void updateMotion(TankDefinition def, boolean piloted) {
        Vec3 m = getDeltaMovement();
        double mx = m.x;
        double my = m.y;
        double mz = m.z;
        double prevMotion = Math.sqrt(mx * mx + mz * mz);
        double depth = def.floats ? waterDepth() : 0.0;

        if (depth == 0.0) {
            my += 0.04 + (isInWater() ? def.gravityInWater : def.gravity);
            my += -0.047 * (1.0 - throttle);
        } else if (depth < 1.0) {
            my -= 1.0E-4;
            my += 0.007 * throttle;
        } else {
            if (my < 0.0) {
                my /= 2.0;
            }
            my += 0.007;
        }

        double push = throttle / 10.0;
        double yawRad = Math.toRadians(getYRot());
        double pitchRad = Math.toRadians(getXRot() - 10.0F);
        double vx = -Math.sin(yawRad) * Math.cos(pitchRad);
        double vy = -Math.sin(pitchRad);
        double vz = Math.cos(yawRad) * Math.cos(pitchRad);
        my += vy * push / 8.0;

        if (def.canMoveOnGround || !isOnSolidGround()) {
            if (def.enableBack && reverse > 0.0F) {
                mx -= vx * reverse;
                mz -= vz * reverse;
            } else {
                mx += vx * push;
                mz += vz * push;
            }
        }

        double motion = Math.sqrt(mx * mx + mz * mz);
        float limit = def.speed;
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
        }

        if (!terrain.isEmpty()) {
            Vec3 slide = terrain.move(mx, mz);
            mx += slide.x;
            mz += slide.z;
            if (!piloted) {
                // Nobody is easing the hull in per frame, so take the attitude straight away.
                setXRot(terrain.targetPitch);
                roll = terrain.targetRoll;
            }
        }

        double startX = getX();
        double startZ = getZ();
        move(MoverType.SELF, new Vec3(mx, my, mz));
        double stepX = getX() - startX;
        double stepZ = getZ() - startZ;
        lastStepSq = stepX * stepX + stepZ * stepZ;
        setDeltaMovement(mx * def.motionFactor, my * 0.95, mz * def.motionFactor);
    }

    /** The original's {@code applyOnGroundPitch} is a no-op for tanks; the terrain decides. */
    @Override
    protected void applyOnGroundPitch(float factor) {
    }

    @Override
    protected float autoLowerGearThrottle() {
        return 0.7F;
    }

    @Override
    protected void tickAnimation() {
        float target = visibleThrottle();
        if (getControllingPassenger() != null && (canopy == null || canopy.angle() < 1.0F)) {
            target += 0.1F;
        }
        // Turning on the spot still revs the engine.
        if (!input.left && !input.right && !input.throttleDown) {
            turnSound *= 0.8F;
        } else {
            turnSound = Math.min(turnSound + 0.1F, 0.75F);
        }
        target = Math.max(target, turnSound);
        soundLevel = soundLevel < target ? Math.min(soundLevel + 0.02F, target) : Math.max(soundLevel - 0.02F, target);
    }

    @Override
    public float engineVolume() {
        TankDefinition def = definition();
        return def != null && def.throttleUpDown <= 0.0F ? 0.0F : soundLevel * 0.7F;
    }

    @Override
    public float enginePitch() {
        return Math.max(0.5F + visibleThrottle() * 0.5F, 0.5F + turnSound * 0.5F);
    }
}
