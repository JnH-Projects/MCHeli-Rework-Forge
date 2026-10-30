package com.jnhprojects.mcheli.weapon;

import com.jnhprojects.mcheli.ModSounds;
import com.jnhprojects.mcheli.content.weapon.WeaponDefinition;
import com.jnhprojects.mcheli.content.weapon.WeaponType;
import com.jnhprojects.mcheli.entity.AircraftEntity;
import com.jnhprojects.mcheli.entity.GroundVehicleEntity;
import com.jnhprojects.mcheli.entity.HelicopterEntity;
import com.jnhprojects.mcheli.entity.PlaneEntity;
import com.jnhprojects.mcheli.entity.TankEntity;
import com.jnhprojects.mcheli.entity.projectile.A10Entity;
import com.jnhprojects.mcheli.entity.projectile.AAMissileEntity;
import com.jnhprojects.mcheli.entity.projectile.ASMissileEntity;
import com.jnhprojects.mcheli.entity.projectile.ATMissileEntity;
import com.jnhprojects.mcheli.entity.projectile.BombEntity;
import com.jnhprojects.mcheli.entity.projectile.BulletEntity;
import com.jnhprojects.mcheli.entity.projectile.DispensedItemEntity;
import com.jnhprojects.mcheli.entity.projectile.MarkerRocketEntity;
import com.jnhprojects.mcheli.entity.projectile.ProjectileEntity;
import com.jnhprojects.mcheli.entity.projectile.RocketEntity;
import com.jnhprojects.mcheli.entity.projectile.TorpedoEntity;
import com.jnhprojects.mcheli.entity.projectile.TvMissileEntity;
import com.jnhprojects.mcheli.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * What each weapon type does when the server fires it: the {@code shot} methods of the
 * {@code MCH_Weapon*} classes. Every method returns whether a round was used.
 */
public final class ServerFiring implements WeaponSet.Trigger {
    public static final ServerFiring INSTANCE = new ServerFiring();
    private static final RandomSource RANDOM = RandomSource.create();

    private ServerFiring() {
    }

    @Override
    public boolean fire(WeaponSet set, int mount, Shot shot) {
        if (!(shot.shooter.level() instanceof ServerLevel level)) {
            return false;
        }
        WeaponDefinition w = set.definition();
        return switch (w.type) {
            case MACHINE_GUN_1, MACHINE_GUN_2 -> gun(level, set, shot);
            case ROCKET -> rocket(level, set, shot);
            case BOMB -> bomb(level, set, shot);
            case AA_MISSILE -> aaMissile(level, set, mount, shot);
            case AS_MISSILE -> asMissile(level, set, shot);
            case AT_MISSILE -> atMissile(level, set, mount, shot);
            case TV_MISSILE -> tvMissile(level, set, mount, shot);
            case TORPEDO -> torpedo(level, set, shot);
            case CAS -> cas(level, set, shot);
            case MARKER_ROCKET -> markerRocket(level, set, shot);
            case DISPENSER -> dispenser(level, set, shot);
            case TARGETING_POD -> targetingPod(level, set, shot);
            case DUMMY, SMOKE -> false;
            case null -> false;
        };
    }

    private static <T extends ProjectileEntity> @Nullable T spawn(ServerLevel level, EntityType<T> type, WeaponSet set, Shot shot,
                                                                 Vec3 dir, float yaw, float pitch, double acceleration) {
        T e = type.create(level);
        if (e == null) {
            return null;
        }
        e.launch(shot.x, shot.y, shot.z, dir.x, dir.y, dir.z, yaw, pitch, acceleration);
        e.setWeapon(set.name());
        e.setParameters(set.definition(), shot.shooter, shot.user);
        return e;
    }

    private static Vec3 forward(float yaw, float pitch) {
        return Vec3.directionFromRotation(pitch, yaw);
    }

    private static void sound(WeaponSet set, Entity at) {
        sound(set, at, set.definition().sound);
    }

    private static void sound(WeaponSet set, Entity at, String name) {
        if (set.soundAllowed) {
            WeaponDefinition w = set.definition();
            float rnd = w.soundPitchRandom;
            ModSounds.playAt(at, name, w.soundVolume, w.soundPitch * (1.0F - rnd) + RANDOM.nextFloat() * rnd);
        }
    }

    // Guns, rockets, bombs

    private static boolean gun(ServerLevel level, WeaponSet set, Shot shot) {
        WeaponDefinition w = set.definition();
        BulletEntity e = spawn(level, ModEntities.BULLET.get(), set, shot, forward(shot.yaw, shot.pitch), shot.yaw, shot.pitch, w.acceleration);
        if (e == null) {
            return false;
        }
        if (w.type == WeaponType.MACHINE_GUN_2 && w.modeNum >= 2) {
            // Mode 0 fires ball ammunition, which only bangs; mode 1 is high explosive.
            e.explosionPower = shot.option1 == 0 ? -w.explosion : w.explosion;
        }
        Vec3 m = e.getDeltaMovement();
        e.setPos(e.getX() + m.x * 0.5, e.getY() + m.y * 0.5, e.getZ() + m.z * 0.5);
        level.addFreshEntity(e);
        sound(set, shot.shooter);
        return true;
    }

    private static boolean rocket(ServerLevel level, WeaponSet set, Shot shot) {
        sound(set, shot.shooter);
        RocketEntity e = spawn(level, ModEntities.ROCKET.get(), set, shot, forward(shot.yaw, shot.pitch), shot.yaw, shot.pitch,
                set.definition().acceleration);
        if (e == null) {
            return false;
        }
        if (shot.option1 == 0 && set.definition().modeNum > 1) {
            e.piercing = 0;
        }
        level.addFreshEntity(e);
        return true;
    }

    private static boolean bomb(ServerLevel level, WeaponSet set, Shot shot) {
        WeaponDefinition w = set.definition();
        if (w.destruct) {
            // A drone's self destruct charge.
            if (shot.shooter instanceof HelicopterEntity drone && drone.definition() != null && drone.definition().uav) {
                Explosions.explode(level, drone.getX(), drone.getY(), drone.getZ(),
                        new Explosions.Params(null, shot.user, w.explosion, w.explosionBlock, w.flaming, true, 0, false, null));
                sound(set, shot.shooter);
                drone.ejectPassengers();
                drone.discard();
            }
            return true;
        }
        sound(set, shot.shooter);
        Vec3 motion = shot.shooter.getDeltaMovement();
        BombEntity e = spawn(level, ModEntities.BOMB.get(), set, shot, motion, shot.shooter.getYRot(), 0.0F, w.acceleration);
        if (e == null) {
            return false;
        }
        e.setDeltaMovement(motion);
        level.addFreshEntity(e);
        return true;
    }

    // Missiles

    private static float fixedYaw(WeaponSet set, int mount) {
        return set.mounts().get(mount).yaw();
    }

    private static float fixedPitch(WeaponSet set, int mount) {
        return set.mounts().get(mount).pitch();
    }

    private static boolean aaMissile(ServerLevel level, WeaponSet set, int mount, Shot shot) {
        Entity target = level.getEntity(shot.option1);
        if (target == null || target.isRemoved()) {
            return false;
        }
        sound(set, shot.shooter);
        float yaw = shot.shooter.getYRot() + fixedYaw(set, mount);
        float pitch = shot.shooter.getXRot() + fixedPitch(set, mount);
        AAMissileEntity e = spawn(level, ModEntities.AA_MISSILE.get(), set, shot, forward(yaw, pitch), yaw, pitch, set.definition().acceleration);
        if (e == null) {
            return false;
        }
        e.setTarget(target);
        level.addFreshEntity(e);
        return true;
    }

    /** Ray from the shooter along the aimer's view, as the original's AS missile and CAS sights. */
    private static @Nullable BlockHitResult sight(ServerLevel level, Shot shot, double eyeHeight, double range) {
        Entity aimer = shot.aimer();
        Vec3 from = shot.shooter.position().add(0.0, eyeHeight, 0.0);
        Vec3 to = from.add(forward(aimer.getYRot(), aimer.getXRot()).scale(range));
        BlockHitResult hit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, shot.shooter));
        return hit.getType() == HitResult.Type.BLOCK ? hit : null;
    }

    private static boolean nextToWater(ServerLevel level, BlockPos pos) {
        if (level.getFluidState(pos).is(FluidTags.WATER)) {
            return true;
        }
        for (Direction d : Direction.values()) {
            if (level.getFluidState(pos.relative(d)).is(FluidTags.WATER)) {
                return true;
            }
        }
        return false;
    }

    private static boolean asMissile(ServerLevel level, WeaponSet set, Shot shot) {
        BlockHitResult hit = sight(level, shot, 1.62, 250.0);
        if (hit == null || nextToWater(level, hit.getBlockPos())) {
            return false;
        }
        Entity aimer = shot.aimer();
        ASMissileEntity e = spawn(level, ModEntities.AS_MISSILE.get(), set, shot, forward(aimer.getYRot(), aimer.getXRot()),
                aimer.getYRot(), aimer.getXRot(), set.definition().acceleration);
        if (e == null) {
            return false;
        }
        e.targetX = hit.getLocation().x;
        e.targetY = hit.getLocation().y;
        e.targetZ = hit.getLocation().z;
        level.addFreshEntity(e);
        sound(set, shot.shooter);
        return true;
    }

    private static boolean atMissile(ServerLevel level, WeaponSet set, int mount, Shot shot) {
        Entity target = level.getEntity(shot.option1);
        if (target == null || target.isRemoved()) {
            return false;
        }
        float yaw = shot.aimer().getYRot() + fixedYaw(set, mount);
        float pitch = shot.shooter.getXRot() + fixedPitch(set, mount);
        ATMissileEntity e = spawn(level, ModEntities.AT_MISSILE.get(), set, shot, forward(yaw, pitch), yaw, pitch, set.definition().acceleration);
        if (e == null) {
            return false;
        }
        e.setTarget(target);
        e.guidanceType = shot.option2;
        level.addFreshEntity(e);
        sound(set, shot.shooter);
        return true;
    }

    private static boolean tvMissile(ServerLevel level, WeaponSet set, int mount, Shot shot) {
        float yaw = shot.aimer().getYRot() + fixedYaw(set, mount);
        float pitch = shot.aimer().getXRot() + fixedPitch(set, mount);
        boolean tvGuided = shot.option1 == 0;
        float acceleration = set.definition().acceleration * (tvGuided ? 1.0F : 1.5F);
        TvMissileEntity e = spawn(level, ModEntities.TV_MISSILE.get(), set, shot, forward(yaw, pitch), yaw, pitch, acceleration);
        if (e == null) {
            return false;
        }
        level.addFreshEntity(e);
        if (shot.shooter instanceof AircraftEntity vehicle) {
            vehicle.setTvMissile(e);
        }
        sound(set, shot.shooter);
        return true;
    }

    private static boolean torpedo(ServerLevel level, WeaponSet set, Shot shot) {
        WeaponDefinition w = set.definition();
        Vec3 vehicleMotion = shot.shooter.getDeltaMovement();
        float yaw = shot.yaw;
        float pitch = shot.pitch;
        Vec3 target = null;
        if (w.guidedTorpedo) {
            Entity aimer = shot.aimer();
            yaw = aimer.getYRot();
            pitch = aimer.getXRot();
            Vec3 from = aimer.position();
            Vec3 to = from.add(forward(yaw, pitch).scale(150.0));
            BlockHitResult hit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, shot.shooter));
            if (hit.getType() != HitResult.Type.BLOCK || !nextToWater(level, hit.getBlockPos())) {
                return false;
            }
            target = hit.getLocation();
        }
        Vec3 m = forward(yaw, pitch).scale(w.acceleration).add(vehicleMotion);
        double speed = m.length();
        TorpedoEntity e = spawn(level, ModEntities.TORPEDO.get(), set, shot, m, yaw, 0.0F, speed);
        if (e == null) {
            return false;
        }
        e.setDeltaMovement(m);
        e.accelerationInWater = w.accelerationInWater;
        if (target != null) {
            e.targetX = target.x;
            e.targetY = target.y;
            e.targetZ = target.z;
        }
        level.addFreshEntity(e);
        sound(set, shot.shooter);
        return true;
    }

    // Support weapons

    private static boolean markerRocket(ServerLevel level, WeaponSet set, Shot shot) {
        sound(set, shot.shooter);
        MarkerRocketEntity e = spawn(level, ModEntities.MARKER_ROCKET.get(), set, shot, forward(shot.yaw, shot.pitch), shot.yaw, shot.pitch,
                set.definition().acceleration);
        if (e == null) {
            return false;
        }
        e.setStatus(1);
        level.addFreshEntity(e);
        return true;
    }

    private static boolean dispenser(ServerLevel level, WeaponSet set, Shot shot) {
        sound(set, shot.shooter);
        DispensedItemEntity e = spawn(level, ModEntities.DISPENSED_ITEM.get(), set, shot, forward(shot.yaw, shot.pitch), shot.yaw, shot.pitch,
                set.definition().acceleration);
        if (e == null) {
            return false;
        }
        Vec3 m = shot.shooter.getDeltaMovement().add(e.getDeltaMovement().scale(0.5));
        e.setDeltaMovement(m);
        e.setPos(e.getX() + m.x * 0.5, e.getY() + m.y * 0.5, e.getZ() + m.z * 0.5);
        level.addFreshEntity(e);
        return true;
    }

    /**
     * Spots everything of the weapon's {@code Target} kinds inside its cone, which then glows for
     * {@code MarkTime}. Marking a block for the team needs the team manager of a later phase.
     */
    private static boolean targetingPod(ServerLevel level, WeaponSet set, Shot shot) {
        WeaponDefinition w = set.definition();
        Entity aimer = shot.aimer();
        if ((w.target & WeaponDefinition.TARGET_BLOCK) != 0) {
            sound(set, aimer, "ng");
            return true;
        }
        boolean spotted = false;
        double range = Math.max(w.length, 1.0F);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, aimer.getBoundingBox().inflate(range))) {
            if (entity == aimer || shot.shooter.hasPassenger(entity) || aimer.distanceToSqr(entity) > range * range
                    || !matchesTarget(entity, w.target) || !Guidance.inCone(aimer, aimer.getYRot(), aimer.getXRot(), entity, w.angle)) {
                continue;
            }
            entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, Math.max(w.markTime, 20)));
            spotted = true;
        }
        sound(set, shot.shooter, spotted ? w.sound : "ng");
        return true;
    }

    private static boolean matchesTarget(LivingEntity entity, int bits) {
        Entity vehicle = entity.getVehicle();
        if (vehicle instanceof PlaneEntity) {
            return (bits & WeaponDefinition.TARGET_PLANES) != 0;
        }
        if (vehicle instanceof HelicopterEntity) {
            return (bits & WeaponDefinition.TARGET_HELICOPTERS) != 0;
        }
        if (vehicle instanceof TankEntity || vehicle instanceof GroundVehicleEntity) {
            return (bits & WeaponDefinition.TARGET_GROUND_VEHICLES) != 0;
        }
        if (entity instanceof Player) {
            return (bits & WeaponDefinition.TARGET_PLAYERS) != 0;
        }
        if (entity instanceof Enemy) {
            return (bits & WeaponDefinition.TARGET_MONSTERS) != 0;
        }
        return (bits & WeaponDefinition.TARGET_OTHERS) != 0;
    }

    // Close air support

    private static boolean cas(ServerLevel level, WeaponSet set, Shot shot) {
        BlockHitResult hit = sight(level, shot, 2.0, 150.0);
        if (hit == null) {
            return false;
        }
        Vec3 at = hit.getLocation();
        set.casX = at.x;
        set.casY = at.y;
        set.casZ = at.z;
        int direction = (int) Mth.positiveModulo(shot.aimer().getYRot() + 45.0F, 360.0F) / 90;
        direction += RANDOM.nextBoolean() ? -1 : 1;
        set.casDirection = Math.floorMod(direction, 4);
        set.casUser = shot.user;
        set.casShooter = shot.shooter;
        set.casStartTick = 50;
        set.casCount = 0;
        if (shot.user instanceof ServerPlayer player) {
            ModSounds.playTo(player, set.definition().sound, 1.0F, 1.0F);
        }
        return true;
    }

    /** {@code MCH_WeaponCAS.update}: three passes, 45 ticks apart, the later ones offset to the sides. */
    static void tick(AircraftEntity vehicle, WeaponSet set) {
        if (set.casCount >= 3 || set.countWait == 0 || set.ticksSinceShot(0) != set.casStartTick
                || !(vehicle.level() instanceof ServerLevel level)) {
            return;
        }
        double x = 0.0;
        double z = 0.0;
        if (set.casCount >= 1) {
            double sign = set.casCount == 1 ? 1.0 : -1.0;
            if (set.casDirection % 2 == 0) {
                x = RANDOM.nextDouble() * 10.0 * sign;
                z = (RANDOM.nextDouble() - 0.5) * 10.0;
            } else {
                z = RANDOM.nextDouble() * 10.0 * sign;
                x = (RANDOM.nextDouble() - 0.5) * 10.0;
            }
        }
        spawnA10(level, set, set.casX + x, set.casY + 20.0, set.casZ + z);
        set.casStartTick = set.ticksSinceShot(0) + 45;
        set.casCount++;
    }

    private static void spawnA10(ServerLevel level, WeaponSet set, double x, double y, double z) {
        double mx = 0.0;
        double mz = 0.0;
        switch (set.casDirection) {
            case 0 -> {
                mz = 3.0;
                z -= 90.0;
            }
            case 1 -> {
                mx = -3.0;
                x += 90.0;
            }
            case 2 -> {
                mz = -3.0;
                z += 90.0;
            }
            default -> {
                mx = 3.0;
                x -= 90.0;
            }
        }
        A10Entity a10 = ModEntities.A10.get().create(level);
        if (a10 == null) {
            return;
        }
        a10.setPos(x, y, z);
        WeaponDefinition w = set.definition();
        a10.setup(set.name(), set.casDirection, mx, mz, set.casShooter, set.casUser);
        a10.power = w.power;
        a10.explosionPower = w.explosion;
        a10.acceleration = w.acceleration;
        level.addFreshEntity(a10);
        ModSounds.playAt(level, x, y, z, "a-10_snd", 150.0F, 1.0F);
    }
}
