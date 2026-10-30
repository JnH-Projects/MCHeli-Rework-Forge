package com.jnhprojects.mcheli.weapon;

import com.jnhprojects.mcheli.entity.AircraftEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.function.Predicate;

/**
 * Lock-on for seeking missiles, ported from {@code MCH_WeaponGuidanceSystem}. It runs on the
 * client of the crew member aiming; the locked entity travels to the server with the fire request.
 *
 * <p>A target is acquired inside a cone around the aim, then the lock count climbs while it stays
 * in the cone. Stealth shrinks both the range and the cone and stretches the time to lock.
 */
public final class Guidance {
    public final boolean canLockInAir;
    public final boolean canLockOnGround;
    public final boolean canLockInWater = false;
    public final boolean ridableOnly;
    public final double lockRange = 200.0;
    public final int lockAngle = 5;
    private final int lockCountMax;
    private final @Nullable Predicate<Entity> filter;

    private @Nullable Entity target;
    private @Nullable Entity lastLocked;
    private int lockCount;
    private int prevLockCount;
    private int continueLockCount;
    private int soundCount;

    public Guidance(boolean air, boolean ground, boolean ridableOnly, int lockTime, @Nullable Predicate<Entity> filter) {
        this.canLockInAir = air;
        this.canLockOnGround = ground;
        this.ridableOnly = ridableOnly;
        this.lockCountMax = Math.max(lockTime, 1);
        this.filter = filter;
    }

    public int lockCount() {
        return lockCount;
    }

    public int lockCountMax() {
        float stealth = stealth(target);
        return (int) (lockCountMax + lockCountMax * stealth);
    }

    public @Nullable Entity target() {
        return lockCount > 0 && target != null && !target.isRemoved() ? target : null;
    }

    public @Nullable Entity lastLocked() {
        return lastLocked;
    }

    public boolean isLocked() {
        return lockCount == lockCountMax() && lastLocked != null;
    }

    /** Called every client tick: a lock decays unless {@link #lock} kept it alive this tick. */
    public void tick() {
        if (lockCount != prevLockCount) {
            prevLockCount = lockCount;
        } else {
            lockCount = 0;
            prevLockCount = 0;
        }
    }

    /**
     * Advances the lock for one tick of aiming from {@code user}'s view.
     *
     * @param eye where the view is seen from. The original used the player's position, which on a
     *            1.7.10 client was eye height; gunner sights and launcher cameras sit higher still,
     *            and a 5 degree cone measured from the wrong point misses targets close by.
     * @return true once the lock is complete; {@link #lastLocked()} is the target to send
     */
    public boolean lock(Entity user, Vec3 eye) {
        if (lockCount == 0) {
            acquire(user, eye);
            return false;
        }
        if (target == null || target.isRemoved() || !inLockableState(target)) {
            clear();
            return false;
        }
        double range = lockRange * (1.0F - stealth(target));
        if (user.distanceToSqr(target) >= range * range) {
            clear();
            return false;
        }
        soundCount = (soundCount + 1) % 15;
        if (inCone(eye, user.getYRot(), user.getXRot(), target, lockAngle)) {
            if (lockCount < lockCountMax()) {
                lockCount++;
            }
        } else if (continueLockCount > 0) {
            continueLockCount--;
            if (continueLockCount <= 0 && lockCount > 0) {
                lockCount--;
            }
        } else {
            continueLockCount = 0;
            lockCount--;
        }
        if (lockCount >= lockCountMax()) {
            if (continueLockCount <= 0) {
                continueLockCount = Math.min(lockCountMax() / 3, 20);
            }
            lastLocked = target;
            prevLockCount = lockCount - 1;
            return true;
        }
        return false;
    }

    /** True on the tick the target should hear that it is being tracked. */
    public boolean shouldNotifyTarget() {
        return lockCount > 0 && soundCount == 1;
    }

    private void acquire(Entity user, Vec3 eye) {
        Entity best = null;
        double bestDistance = lockRange * lockRange * 2.0;
        for (Entity entity : user.level().getEntities(user, user.getBoundingBox().inflate(lockRange), this::canLock)) {
            double d = entity.distanceToSqr(user);
            float stealth = 1.0F - stealth(entity);
            double range = lockRange * stealth;
            float angle = lockAngle * (stealth / 2.0F + 0.5F);
            if (d < range * range && d < bestDistance && inCone(eye, user.getYRot(), user.getXRot(), entity, angle)) {
                Vec3 to = new Vec3(entity.getX(), entity.getY() + entity.getBbHeight() / 2.0F, entity.getZ());
                HitResult hit = user.level().clip(new ClipContext(eye, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, user));
                if (hit.getType() == HitResult.Type.MISS) {
                    best = entity;
                    bestDistance = d;
                }
            }
        }
        target = best;
        if (best != null) {
            lockCount++;
        }
    }

    public void clear() {
        target = null;
        lockCount = 0;
        continueLockCount = 0;
        soundCount = 0;
    }

    private boolean canLock(Entity entity) {
        if (ridableOnly && entity instanceof Player && entity.getVehicle() == null) {
            return false;
        }
        if (!(entity instanceof LivingEntity) && !(entity instanceof AircraftEntity)) {
            return false;
        }
        if (filter != null && !filter.test(entity)) {
            return false;
        }
        return inLockableState(entity);
    }

    private boolean inLockableState(Entity entity) {
        if (!canLockInWater && entity.isInWater()) {
            return false;
        }
        boolean onGround = isOnGround(entity);
        return onGround ? canLockOnGround : canLockInAir;
    }

    /** On the ground, or less than twelve blocks above something solid. */
    public static boolean isOnGround(Entity entity) {
        if (entity.onGround()) {
            return true;
        }
        BlockPos.MutableBlockPos pos = BlockPos.containing(entity.getX() + 0.5, entity.getY() + 0.5, entity.getZ() + 0.5).mutable();
        for (int i = 0; i < 12; i++) {
            if (!entity.level().getBlockState(pos).isAir() && !entity.level().getFluidState(pos).is(FluidTags.WATER)) {
                return true;
            }
            pos.move(0, -1, 0);
        }
        return false;
    }

    public static float stealth(@Nullable Entity entity) {
        if (entity instanceof AircraftEntity vehicle) {
            return vehicle.stealth();
        }
        return entity != null && entity.getVehicle() instanceof AircraftEntity vehicle ? vehicle.stealth() : 0.0F;
    }

    /** {@code inLockRange}: whether {@code target} is within {@code angle} degrees of the aim. */
    public static boolean inCone(Entity from, float yaw, float pitch, Entity target, float angle) {
        return inCone(from.position(), yaw, pitch, target, angle);
    }

    public static boolean inCone(Vec3 from, float yaw, float pitch, Entity target, float angle) {
        double dx = target.getX() - from.x;
        double dy = target.getY() + target.getBbHeight() / 2.0F - from.y;
        double dz = target.getZ() - from.z;
        float targetYaw = (float) Math.toDegrees(Math.atan2(dz, dx));
        float diffYaw = Mth.positiveModulo(targetYaw - yaw - 90.0F, 360.0F);
        float targetPitch = -(float) Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        float diffPitch = targetPitch - pitch;
        return (diffYaw < angle || diffYaw > 360.0F - angle) && Math.abs(diffPitch) < angle;
    }
}
