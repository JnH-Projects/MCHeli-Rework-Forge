package com.jnhprojects.mcheli.weapon;

import com.jnhprojects.mcheli.ModSounds;
import com.jnhprojects.mcheli.content.weapon.WeaponDefinition;
import com.jnhprojects.mcheli.entity.projectile.A10Entity;
import com.jnhprojects.mcheli.registry.ModEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A close air support run, {@code MCH_WeaponCAS}: three A-10 passes over a spot, 45 ticks apart,
 * the second and third offset to either side. Vehicles with a CAS weapon drive theirs from their
 * {@link WeaponSet}; the GLTD keeps one of these.
 */
public final class CasStrike {
    /** Ticks from the call to the first pass: the time it takes the A-10 to arrive. */
    public static final int FIRST_PASS = 50;
    private static final int BETWEEN_PASSES = 45;
    private static final int PASSES = 3;
    private static final RandomSource RANDOM = RandomSource.create();

    private final String weapon;
    private double x;
    private double y;
    private double z;
    private int direction;
    private @Nullable Entity shooter;
    private @Nullable Entity user;
    private int passes = PASSES;
    private int ticksToNext;

    /** @param weapon the CAS weapon definition the A-10s fire, such as {@code a10gau8} */
    public CasStrike(String weapon) {
        this.weapon = weapon;
    }

    /** Whether passes are still to come. */
    public boolean isRunning() {
        return passes < PASSES;
    }

    /**
     * Calls the strike on {@code at}. The A-10 comes in from one side of the caller's view,
     * picked at random, never straight from behind or towards them.
     */
    public void start(Vec3 at, float callerYaw, @Nullable Entity shooter, @Nullable Entity user) {
        x = at.x;
        y = at.y;
        z = at.z;
        direction = Math.floorMod(approach(callerYaw), 4);
        this.shooter = shooter;
        this.user = user;
        passes = 0;
        ticksToNext = FIRST_PASS;
    }

    /** The original's approach: the quarter the caller faces, turned one way or the other. */
    static int approach(float callerYaw) {
        int facing = (int) Mth.positiveModulo(callerYaw + 45.0F, 360.0F) / 90;
        return facing + (RANDOM.nextBoolean() ? -1 : 1);
    }

    public void tick(ServerLevel level, WeaponDefinition def) {
        if (passes >= PASSES || --ticksToNext > 0) {
            return;
        }
        Vec3 offset = passOffset(passes, direction);
        spawnA10(level, weapon, def, direction, x + offset.x, y + 20.0, z + offset.z, shooter, user);
        passes++;
        ticksToNext = BETWEEN_PASSES;
    }

    /** The first pass is dead on; the second strays to one side of the track, the third to the other. */
    static Vec3 passOffset(int pass, int direction) {
        if (pass == 0) {
            return Vec3.ZERO;
        }
        double sign = pass == 1 ? 1.0 : -1.0;
        if (direction % 2 == 0) {
            return new Vec3(RANDOM.nextDouble() * 10.0 * sign, 0.0, (RANDOM.nextDouble() - 0.5) * 10.0);
        }
        return new Vec3((RANDOM.nextDouble() - 0.5) * 10.0, 0.0, RANDOM.nextDouble() * 10.0 * sign);
    }

    /** One A-10, starting 90 blocks short of the spot and flying over it at three blocks a tick. */
    static void spawnA10(ServerLevel level, String weapon, WeaponDefinition def, int direction, double x, double y, double z,
            @Nullable Entity shooter, @Nullable Entity user) {
        double mx = 0.0;
        double mz = 0.0;
        switch (direction) {
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
        a10.setup(weapon, direction, mx, mz, shooter, user);
        a10.power = def.power;
        a10.explosionPower = def.explosion;
        a10.acceleration = def.acceleration;
        level.addFreshEntity(a10);
        ModSounds.playAt(level, x, y, z, "a-10_snd", 150.0F, 1.0F);
    }
}
