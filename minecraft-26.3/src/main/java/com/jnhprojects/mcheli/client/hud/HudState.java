package com.jnhprojects.mcheli.client.hud;

import com.jnhprojects.mcheli.content.aircraft.AircraftDefinition;
import com.jnhprojects.mcheli.content.weapon.WeaponDefinition;
import com.jnhprojects.mcheli.entity.AircraftEntity;
import com.jnhprojects.mcheli.entity.HelicopterEntity;
import com.jnhprojects.mcheli.entity.PlaneEntity;
import com.jnhprojects.mcheli.entity.TankEntity;
import com.jnhprojects.mcheli.entity.projectile.TvMissileEntity;
import com.jnhprojects.mcheli.hud.HudVariable;
import com.jnhprojects.mcheli.hud.TextArg;
import com.jnhprojects.mcheli.platform.Platform;
import com.jnhprojects.mcheli.weapon.Guidance;
import com.jnhprojects.mcheli.weapon.VehicleWeapons;
import com.jnhprojects.mcheli.weapon.WeaponSet;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * What the HUD scripts read, the static state of {@code MCH_HudItem}. Ticked with the client, so
 * the radar sweep, altitude and stick behave as in the original; {@link #update} then fills in the
 * per frame values before a script runs.
 *
 * <p>Systems that arrive later in phase 4 read as their idle state for now: fuel is full, the
 * camera is in normal view at zoom 1, the pilot gunner mode is off, the vehicle inventory is empty
 * and the mortar distance is unknown.
 */
final class HudState {
    private static final int RADAR_RANGE = 64;
    private static final int ALTITUDE_INTERVAL = 10;
    private static final double STICK_MAX = 40.0;
    /** Roughly the mouse units of the original per degree of pilot look. */
    private static final double STICK_PER_DEGREE = 6.0;
    private static final int STICK_SAMPLES = 4;

    final HudVariable.Values vars = new HudVariable.Values();
    final List<double[]> entityBlips = new ArrayList<>();
    final List<double[]> enemyBlips = new ArrayList<>();

    private @Nullable AircraftEntity vehicle;
    private int radarRotate;
    private int radarRotateO;
    private int altitude;
    private int altitudeCountdown;
    private double stickX;
    private double stickY;
    private final double[] stickXSamples = new double[STICK_SAMPLES];
    private final double[] stickYSamples = new double[STICK_SAMPLES];
    private int stickSample;

    /** Colour set by the last {@code Color} line. Like the original it carries over between frames. */
    int color = 0xFF000000;

    // Per frame, for the text arguments.
    private @Nullable WeaponSet weapon;
    private String weaponName = "";
    private String weaponAmmo = "";
    private String weaponRestAmmo = "";
    private float reloadPercent;
    private float reloadSeconds;

    /** Mouse look of the pilot this frame, in degrees; drives the control stick indicator. */
    void onPilotLook(float dYaw, float dPitch) {
        stickX += dYaw * STICK_PER_DEGREE;
        stickY += dPitch * STICK_PER_DEGREE;
        double length = Math.sqrt(stickX * stickX + stickY * stickY);
        if (length > STICK_MAX) {
            stickX *= STICK_MAX / length;
            stickY *= STICK_MAX / length;
        }
    }

    void tick(@Nullable AircraftEntity riding) {
        if (riding != vehicle) {
            vehicle = riding;
            radarRotate = 0;
            radarRotateO = 0;
            altitudeCountdown = 0;
            entityBlips.clear();
            enemyBlips.clear();
        }
        stickX *= 0.85;
        stickY *= 0.85;
        if (riding == null || riding.definition() == null) {
            return;
        }
        if (riding.definition().entityRadar) {
            radarRotateO = radarRotate;
            radarRotate += radarSpeed(riding);
            if (radarRotate >= 360) {
                radarRotate = 0;
                radarRotateO = 0;
                sweepRadar(riding);
            }
        }
        if (--altitudeCountdown <= 0) {
            altitude = measureAltitude(riding);
            altitudeCountdown = ALTITUDE_INTERVAL;
        }
    }

    /** Degrees per tick, from each kind's client tick handler. */
    private static int radarSpeed(AircraftEntity vehicle) {
        return vehicle instanceof PlaneEntity || vehicle instanceof TankEntity ? 10 : 5;
    }

    /** {@code MCH_Radar.updateXZ}: mobs within 64 blocks that are not buried under five or more blocks. */
    private void sweepRadar(AircraftEntity center) {
        entityBlips.clear();
        enemyBlips.clear();
        Level level = center.level();
        for (Mob mob : level.getEntitiesOfClass(Mob.class, center.getBoundingBox().inflate(RADAR_RANGE))) {
            double x = mob.getX() - center.getX();
            double z = mob.getZ() - center.getZ();
            if (x * x + z * z >= RADAR_RANGE * RADAR_RANGE || isBuried(level, mob)) {
                continue;
            }
            (mob instanceof Enemy ? enemyBlips : entityBlips).add(new double[]{x, z});
        }
    }

    private static boolean isBuried(Level level, Mob mob) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(mob.getBlockX(), Math.max(mob.getBlockY() + 1, level.getMinY()), mob.getBlockZ());
        int blocks = 0;
        for (; pos.getY() < level.getMaxY(); pos.move(0, 1, 0)) {
            if (!level.getBlockState(pos).isAir() && ++blocks >= 5) {
                return true;
            }
        }
        return false;
    }

    /** {@code updateAltitude}: blocks of air below the vehicle, looking at most 256 down. */
    private static int measureAltitude(AircraftEntity vehicle) {
        Level level = vehicle.level();
        BlockPos.MutableBlockPos pos = vehicle.blockPosition().mutable();
        int top = pos.getY();
        for (int i = 0; i < 256 && top - i > level.getMinY(); i++) {
            pos.setY(top - i);
            if (!level.getBlockState(pos).isAir()) {
                return i;
            }
        }
        return 0;
    }

    float radarRotation(float partialTicks) {
        float rot = radarRotate;
        float prev = radarRotateO;
        if (rot < prev) {
            rot += 360.0F;
        }
        return prev + (rot - prev) * partialTicks;
    }

    void update(AircraftEntity vehicle, AircraftDefinition def, LocalPlayer player, int seat, double width, double height,
                float partialTicks) {
        HudVariable.Values v = vars;
        v.set(HudVariable.COLOR, color & 0xFFFFFFFFL);
        v.set(HudVariable.CENTER_X, width / 2.0);
        v.set(HudVariable.CENTER_Y, height / 2.0);
        v.set(HudVariable.WIDTH, width);
        v.set(HudVariable.HEIGHT, height);
        v.set(HudVariable.TIME, Math.floorMod(vehicle.level().getOverworldClockTime(), 24000L));
        v.set(HudVariable.TEST_MODE, 0.0);
        v.set(HudVariable.PLYR_YAW, Mth.wrapDegrees(player.getViewYRot(partialTicks)));
        v.set(HudVariable.PLYR_PITCH, player.getViewXRot(partialTicks));
        v.set(HudVariable.YAW, Mth.wrapDegrees(vehicle.getYRot(partialTicks)));
        v.set(HudVariable.PITCH, vehicle.getXRot(partialTicks));
        v.set(HudVariable.ROLL, Mth.wrapDegrees(vehicle.roll(partialTicks)));
        v.set(HudVariable.ALTITUDE, altitude);
        v.set(HudVariable.SEA_ALT, Math.max(vehicle.getY() - vehicle.level().getSeaLevel(), 0.0));
        v.set(HudVariable.HAVE_RADAR, def.entityRadar);
        v.set(HudVariable.RADAR_ROT, radarRotation(partialTicks));
        v.set(HudVariable.HP, vehicle.hp());
        v.set(HudVariable.MAX_HP, def.maxHp);
        v.set(HudVariable.HP_RTO, def.maxHp > 0 ? (double) vehicle.hp() / def.maxHp : 0.0);
        v.set(HudVariable.THROTTLE, vehicle.visibleThrottle());
        v.set(HudVariable.POS_X, vehicle.getX());
        v.set(HudVariable.POS_Y, vehicle.getY());
        v.set(HudVariable.POS_Z, vehicle.getZ());
        // Only the pilot's client simulates the hull, so read motion from the position instead.
        double mx = vehicle.getX() - vehicle.xo;
        double my = vehicle.getY() - vehicle.yo;
        double mz = vehicle.getZ() - vehicle.zo;
        v.set(HudVariable.MOTION_X, mx);
        v.set(HudVariable.MOTION_Y, my);
        v.set(HudVariable.MOTION_Z, mz);
        v.set(HudVariable.SPEED, Math.sqrt(mx * mx + my * my + mz * mz));
        v.set(HudVariable.FUEL, 1.0);
        v.set(HudVariable.LOW_FUEL, 0.0);
        updateStick(seat == 0 && vehicle.steersWithMouse());
        updateWeapon(vehicle, seat);
        v.set(HudVariable.VTOL_STAT, vehicle.vtolMode());
        v.set(HudVariable.FREE_LOOK, seat == 0 && vehicle.input().freeLook);
        v.set(HudVariable.GUNNER_MODE, vehicle.isGunnerMode(player));
        v.set(HudVariable.CAM_MODE, 0.0);
        v.set(HudVariable.CAM_ZOOM, 1.0);
        v.set(HudVariable.AUTO_PILOT, vehicle instanceof PlaneEntity && vehicle.isGunnerMode(player));
        VehicleWeapons weapons = vehicle.weapons();
        boolean flares = weapons != null && weapons.flares.isFitted();
        v.set(HudVariable.HAVE_FLARE, flares);
        v.set(HudVariable.CAN_FLARE, flares && !weapons.flares.isPreparing() && !weapons.flares.isActive());
        v.set(HudVariable.INVENTORY, 0.0);
        v.set(HudVariable.HOVERING, vehicle instanceof HelicopterEntity && vehicle.isHoveringMode());
        v.set(HudVariable.IS_UAV, def.uav);
        v.set(HudVariable.UAV_FS, 0.0);
    }

    /** The original averaged the last four stick positions, scaled to -1..1, with up positive. */
    private void updateStick(boolean pilot) {
        stickXSamples[stickSample] = pilot ? stickX / STICK_MAX : 0.0;
        stickYSamples[stickSample] = pilot ? -stickY / STICK_MAX : 0.0;
        stickSample = (stickSample + 1) % STICK_SAMPLES;
        double x = 0.0;
        double y = 0.0;
        for (int i = 0; i < STICK_SAMPLES; i++) {
            x += stickXSamples[i];
            y += stickYSamples[i];
        }
        vars.set(HudVariable.STICK_X, x / STICK_SAMPLES);
        vars.set(HudVariable.STICK_Y, y / STICK_SAMPLES);
    }

    /** {@code updateWeapon} and {@code updateVarMap_Weapon}. */
    private void updateWeapon(AircraftEntity vehicle, int seat) {
        VehicleWeapons weapons = vehicle.weapons();
        WeaponSet set = weapons != null ? weapons.current(seat) : null;
        weapon = set;
        double reloadTime = 0.0;
        double heat = 0.0;
        double lock = 0.0;
        int sightType = 0;
        if (set != null) {
            WeaponDefinition def = set.definition();
            weaponName = set.displayName();
            weaponAmmo = set.magazineSize() > 0 ? "%4d".formatted(set.ammo()) : "";
            weaponRestAmmo = set.magazineSize() > 0 ? "%4d".formatted(set.restAmmo()) : "";
            float wait;
            if (def.delay > def.reloadTime) {
                reloadSeconds = Math.abs(set.countWait);
                wait = Math.min(Math.abs((float) set.countWait / Math.max(def.delay, 1)), 1.0F);
            } else {
                reloadSeconds = set.countReloadWait;
                wait = (float) set.countReloadWait / Math.max(def.reloadTime, 1);
            }
            reloadTime = wait;
            reloadSeconds /= 20.0F;
            reloadPercent = (1.0F - wait) * 100.0F;
            if (def.maxHeatCount > 0) {
                heat = Math.min((double) set.currentHeat / def.maxHeatCount, 1.0);
            }
            Guidance guidance = weapons.guidance(weapons.selected(seat));
            if (def.sight == WeaponDefinition.Sight.LOCK && guidance != null && guidance.lockCountMax() > 0) {
                lock = (double) guidance.lockCount() / guidance.lockCountMax();
                sightType = 2;
            } else if (def.sight == WeaponDefinition.Sight.ROCKET) {
                sightType = 1;
            }
            vars.set(HudVariable.IS_HEAT_WPN, def.maxHeatCount > 0);
            vars.set(HudVariable.RELOADING, set.isInPreparation());
            vars.set(HudVariable.DSP_MT_DIST, def.displayMortarDistance);
        } else {
            vars.set(HudVariable.IS_HEAT_WPN, 0.0);
            vars.set(HudVariable.RELOADING, 0.0);
            vars.set(HudVariable.DSP_MT_DIST, 0.0);
        }
        vars.set(HudVariable.RELOAD_TIME, reloadTime);
        vars.set(HudVariable.WPN_HEAT, heat);
        vars.set(HudVariable.SIGHT_TYPE, sightType);
        vars.set(HudVariable.LOCK, lock);
        vars.set(HudVariable.MT_DIST, -1.0);
    }

    boolean hasWeapon() {
        return weapon != null;
    }

    boolean hasMagazine() {
        return weapon != null && weapon.magazineSize() > 0;
    }

    /** A {@code DrawString} argument, typed the way the original passed it to {@code String.format}. */
    @Nullable Object text(TextArg arg, AircraftEntity vehicle, AircraftDefinition def, LocalPlayer player) {
        int worldTime = (int) Math.floorMod(vehicle.level().getOverworldClockTime() + 6000L, 24000L);
        return switch (arg) {
            case NONE -> null;
            // The vehicle inventory screen and its key come later in phase 4.
            case KEY_GUI -> "";
            case NAME -> def.displayName;
            case ALTITUDE -> altitude;
            case DATE -> new Date();
            case MC_THOR -> worldTime / 1000;
            case MC_TMIN -> worldTime % 1000 * 36 / 10 / 60;
            case MC_TSEC -> worldTime % 1000 * 36 / 10 % 60;
            case MAX_HP -> def.maxHp;
            case HP -> vehicle.hp();
            case HP_PER -> def.maxHp > 0 ? 100.0 * vehicle.hp() / def.maxHp : 0.0;
            case POS_X -> vehicle.getX();
            case POS_Y -> vehicle.getY();
            case POS_Z -> vehicle.getZ();
            case MOTION_X -> vars.get(HudVariable.MOTION_X);
            case MOTION_Y -> vars.get(HudVariable.MOTION_Y);
            case MOTION_Z -> vars.get(HudVariable.MOTION_Z);
            case INVENTORY -> 0;
            case WPN_NAME -> weaponName;
            case WPN_AMMO -> weaponAmmo;
            case WPN_RM_AMMO -> weaponRestAmmo;
            case RELOAD_PER -> reloadPercent;
            case RELOAD_SEC -> reloadSeconds;
            case MORTAR_DIST -> -1.0F;
            case MC_VER -> "26.3";
            case MOD_VER -> Platform.get().modVersion();
            case MOD_NAME -> "MCHeli Rework";
            case YAW -> rotate360(vehicle.getYRot() + 180.0);
            case PITCH -> -vehicle.getXRot();
            case ROLL -> Mth.wrapDegrees(vehicle.roll());
            case PLYR_YAW -> rotate360(player.getYRot() + 180.0);
            case PLYR_PITCH -> -player.getXRot();
            case TVM_POS_X, TVM_POS_Y, TVM_POS_Z, TVM_DIFF -> tvMissile(arg, vehicle);
            case CAM_ZOOM -> 1.0F;
            case UAV_DIST -> 0.0;
            case THROTTLE -> vehicle.visibleThrottle() * 100.0;
        };
    }

    private static double tvMissile(TextArg arg, AircraftEntity vehicle) {
        TvMissileEntity missile = vehicle.tvMissile();
        if (missile == null) {
            return 0.0;
        }
        return switch (arg) {
            case TVM_POS_X -> missile.getX();
            case TVM_POS_Y -> missile.getY();
            case TVM_POS_Z -> missile.getZ();
            default -> missile.position().distanceTo(vehicle.position());
        };
    }

    static double rotate360(double degrees) {
        double r = degrees % 360.0;
        return r >= 0.0 ? r : r + 360.0;
    }
}
