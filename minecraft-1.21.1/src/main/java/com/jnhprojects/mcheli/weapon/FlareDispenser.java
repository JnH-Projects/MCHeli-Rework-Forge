package com.jnhprojects.mcheli.weapon;

import com.jnhprojects.mcheli.content.aircraft.AircraftDefinition;
import com.jnhprojects.mcheli.entity.AircraftEntity;
import com.jnhprojects.mcheli.entity.projectile.FlareEntity;
import com.jnhprojects.mcheli.entity.projectile.ProjectileEntity;
import com.jnhprojects.mcheli.registry.ModEntities;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.phys.Vec3;

/**
 * Flares and smoke dischargers, ported from {@code MCH_Flare} and the flare half of
 * {@code MCH_MissileDetector}. While a salvo burns, every missile homing on the vehicle within 60
 * blocks is decoyed.
 *
 * <p>The ten {@code FlareType} values pick a pattern: 1 random scatter, 2 and 3 to the sides and
 * rear, 4 and 5 trailing, 10 a ring of smoke grenades. Unknown types fall back to type 1.
 */
public final class FlareDispenser {
    private static final int DECOY_RANGE = 60;

    /** {@code num, interval, tickWait, tickEnable, numFlareMax} per type. */
    private static final int[][] TYPES = new int[11][];

    static {
        TYPES[1] = new int[]{1, 3, 200, 100, 16};
        TYPES[2] = new int[]{3, 5, 300, 200, 16};
        TYPES[3] = new int[]{2, 3, 200, 100, 16};
        TYPES[4] = new int[]{1, 3, 200, 100, 16};
        TYPES[5] = new int[]{2, 3, 200, 100, 16};
        TYPES[10] = new int[]{8, 1, 250, 60, 1};
        for (int i : new int[]{0, 6, 7, 8, 9}) {
            TYPES[i] = TYPES[1];
        }
    }

    private final AircraftEntity vehicle;
    private final AircraftDefinition def;
    private final RandomSource random = RandomSource.create();
    private int tick;
    private int fired;
    private int type;
    private int typeIndex;

    FlareDispenser(AircraftEntity vehicle, AircraftDefinition def) {
        this.vehicle = vehicle;
        this.def = def;
    }

    public boolean isFitted() {
        return !def.flareTypes.isEmpty();
    }

    /** Waiting out the reload of the last salvo. */
    public boolean isPreparing() {
        return tick != 0;
    }

    /** The salvo is still burning, so incoming missiles are being decoyed. */
    public boolean isActive() {
        int[] p = params(type);
        return tick != 0 && tick > p[2] - p[3];
    }

    public int ticksLeft() {
        return tick;
    }

    public int cooldown() {
        return params(type)[2];
    }

    public int currentType() {
        return def.flareTypes.isEmpty() ? 0 : def.flareTypes.get(typeIndex % def.flareTypes.size());
    }

    public void nextType() {
        if (!def.flareTypes.isEmpty()) {
            typeIndex = (typeIndex + 1) % def.flareTypes.size();
        }
    }

    /** {@code useFlare}: starts a salvo of the current type if the last one has finished. */
    public boolean use() {
        if (!isFitted() || tick != 0 || vehicle.isDestroyed()) {
            return false;
        }
        type = currentType();
        fired = 0;
        // The client waited 50 ticks longer before showing the dispenser as ready again.
        tick = params(type)[2] + (vehicle.level().isClientSide() ? 50 : 0);
        return true;
    }

    private static int[] params(int type) {
        return type >= 0 && type < TYPES.length && TYPES[type] != null ? TYPES[type] : TYPES[1];
    }

    void tick() {
        if (type <= 0 || vehicle.isRemoved()) {
            return;
        }
        if (tick > 0) {
            tick--;
        }
        int[] p = params(type);
        if (!vehicle.level().isClientSide()) {
            if (tick > 0 && tick % p[1] == 0 && fired < p[4]) {
                Vec3 at = vehicle.toWorld(def.flarePosition);
                spawnSalvo(new Vec3(at.x + vehicle.xo - vehicle.getX(), at.y + vehicle.yo - vehicle.getY(), at.z + vehicle.zo - vehicle.getZ()));
            }
            if (isActive()) {
                decoyMissiles();
            }
        }
    }

    private void spawnSalvo(Vec3 at) {
        fired++;
        int[] p = params(type);
        Vec3 m = vehicle.getDeltaMovement();
        double x = at.x - m.x * 2.0;
        double y = at.y - m.y * 2.0 - 1.0;
        double z = at.z - m.z * 2.0;
        vehicle.level().levelEvent(LevelEvent.SOUND_DISPENSER_PROJECTILE_LAUNCH, net.minecraft.core.BlockPos.containing(x, y, z), 0);
        for (int i = 0; i < p[0]; i++) {
            double tx = 0.0;
            double ty = m.y;
            double tz = 0.0;
            int fuse = 0;
            double r = vehicle.getYRot();
            double fy = y;
            switch (type) {
                case 1 -> {
                    tx = Mth.sin(random.nextFloat() * 360.0F);
                    tz = Mth.cos(random.nextFloat() * 360.0F);
                }
                case 2, 3 -> {
                    if (i == 0) {
                        r += 90.0;
                    } else if (i == 1) {
                        r -= 90.0;
                    } else if (i == 2) {
                        r += 180.0;
                    }
                    r = Math.toRadians(r);
                    tx = -Math.sin(r) + (random.nextFloat() - 0.5) * 0.6;
                    tz = Math.cos(r) + (random.nextFloat() - 0.5) * 0.6;
                }
                case 4 -> {
                    r = Math.toRadians(r);
                    tx = -Math.sin(r) + (random.nextFloat() - 0.5) * 1.3;
                    tz = Math.cos(r) + (random.nextFloat() - 0.5) * 1.3;
                }
                case 5 -> {
                    r = Math.toRadians(r);
                    tx = (-Math.sin(r) + (random.nextFloat() - 0.5) * 0.9) * 0.3;
                    tz = (Math.cos(r) + (random.nextFloat() - 0.5) * 0.9) * 0.3;
                }
                default -> {
                }
            }
            tx += m.x;
            ty += m.y / 2.0;
            tz += m.z;
            if (type == 10) {
                r += 360.0 / p[0] / 2.0 + i * (360.0 / p[0]);
                r = Math.toRadians(r);
                tx = -Math.sin(r) * 2.0;
                tz = Math.cos(r) * 2.0;
                ty = 0.7;
                fy += 2.0;
                fuse = 10;
            }
            FlareEntity flare = ModEntities.FLARE.get().create(vehicle.level());
            if (flare != null) {
                flare.launch(x, fy, z, tx * 0.5, ty * 0.5, tz * 0.5, 6.0F, fuse, type == 4);
                vehicle.level().addFreshEntity(flare);
            }
        }
    }

    /** {@code destroyMissile}: missiles homing on this vehicle or its crew lose their target. */
    private void decoyMissiles() {
        for (ProjectileEntity missile : vehicle.level().getEntitiesOfClass(ProjectileEntity.class,
                vehicle.getBoundingBox().inflate(DECOY_RANGE), p -> p.target() != null)) {
            if (missile.target() == vehicle || vehicle.hasPassenger(missile.target())) {
                missile.decoy();
            }
        }
    }
}
