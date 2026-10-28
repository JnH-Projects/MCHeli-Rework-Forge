package com.jnhprojects.mcheli.weapon;

import com.jnhprojects.mcheli.content.aircraft.WeaponMount;
import com.jnhprojects.mcheli.content.weapon.WeaponDefinition;
import com.jnhprojects.mcheli.content.weapon.WeaponType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * One selectable weapon of a vehicle: every mount that fires the same weapon, with the magazine,
 * reload, heat and recoil they share. A port of {@code MCH_WeaponSet} together with the parts of
 * {@code MCH_WeaponBase} and {@code MCH_WeaponCreator} that are not specific to a weapon type.
 *
 * <p>Both sides keep one. The server's is authoritative for ammunition and decides what is fired;
 * the client's runs with the slightly longer intervals the original used there, so the client never
 * asks to fire faster than the server accepts.
 */
public final class WeaponSet {
    private static final int SWITCH_WAIT = 15;

    /** Fires one mount. Returns whether the shot happened, which consumes a round. */
    @FunctionalInterface
    public interface Trigger {
        boolean fire(WeaponSet set, int mount, Shot shot);
    }

    private final RandomSource random = RandomSource.create();
    private final WeaponDefinition def;
    private final List<WeaponMount> mounts;
    private final boolean clientSide;

    public final int interval;
    public final int heatCount;
    private final int numMode;

    private int currentMount;
    private int mode;
    private int ammo;
    private int restAmmo;
    public int currentHeat;
    private int cooldownSpeed = 1;
    public int countWait;
    public int countReloadWait;
    private final int[] lastUsedCount;
    /** Ticks since each mount last fired, {@code MCH_WeaponBase.tick}. */
    final int[] ticks;
    public int soundWait;
    /** Whether the shot being fired may play its sound, see {@code SoundDelay}. */
    public boolean soundAllowed = true;

    /** Aim relative to the hull, following the crew member who operates the weapon. */
    public float rotationYaw;
    public float rotationPitch;
    public float prevRotationYaw;
    public float prevRotationPitch;
    public final float defaultRotationYaw;
    /** How far behind the pilot's view a turret weapon lags, for drawing. */
    public float rotationTurretYaw;

    public float barrelSpeed;
    public float barrel;
    public float prevBarrel;
    private final Recoil[] recoil;

    // Close air support run, server only. See ServerFiring.
    @Nullable Entity casUser;
    @Nullable Entity casShooter;
    double casX;
    double casY;
    double casZ;
    int casDirection;
    int casStartTick;
    int casCount = 3;

    public WeaponSet(WeaponDefinition def, List<WeaponMount> mounts, boolean clientSide) {
        this.def = def;
        this.mounts = List.copyOf(mounts);
        this.clientSide = clientSide;
        this.lastUsedCount = new int[mounts.size()];
        this.ticks = new int[mounts.size()];
        this.recoil = new Recoil[mounts.size()];
        for (int i = 0; i < recoil.length; i++) {
            recoil[i] = new Recoil(def.recoilBufCount, def.recoilBufCountSpeed);
        }
        this.defaultRotationYaw = mounts.isEmpty() ? 0.0F : mounts.getFirst().defaultYaw();
        this.rotationYaw = defaultRotationYaw;
        this.prevRotationYaw = defaultRotationYaw;

        int wait = def.delay;
        if (def.type == WeaponType.CAS && wait > -250) {
            wait = -250;
        }
        int heat = def.heatCount;
        if (def.maxHeatCount > 0 && heat < 2) {
            heat = 2;
        }
        if (clientSide) {
            wait = clientInterval(wait);
            heat++;
        }
        this.interval = wait;
        this.heatCount = heat;
        this.numMode = def.type == WeaponType.MACHINE_GUN_2 && def.explosion == 0 ? 0 : def.modeNum;
    }

    /** The original gave the client longer intervals so its requests never outran the server. */
    private static int clientInterval(int wait) {
        if (wait < 4) {
            return wait + 1;
        } else if (wait < 7) {
            return wait + 2;
        } else if (wait < 10) {
            return wait + 3;
        } else if (wait < 20) {
            return wait + 6;
        }
        wait += 10;
        return wait >= 40 ? -wait : wait;
    }

    public WeaponDefinition definition() {
        return def;
    }

    public String name() {
        return def.name();
    }

    public List<WeaponMount> mounts() {
        return mounts;
    }

    public WeaponMount firstMount() {
        return mounts.getFirst();
    }

    public int currentMount() {
        return currentMount;
    }

    // Ammunition

    public int ammo() {
        return ammo;
    }

    public int magazineSize() {
        return def.round;
    }

    public int restAmmo() {
        return restAmmo;
    }

    public int maxAmmo() {
        return def.maxAmmo;
    }

    public void setAmmo(int n) {
        ammo = n;
    }

    /** Everything not in the magazine, capped so magazine plus reserve never exceed {@code MaxAmmo}. */
    public void setRestAmmo(int n) {
        restAmmo = Math.min(n, def.maxAmmo - ammo);
    }

    /** {@code supplyRestAllAmmo}: one resupply adds {@code SuppliedNum} rounds. */
    public boolean supply() {
        if (restAmmo + ammo < def.maxAmmo) {
            setRestAmmo(restAmmo + ammo + def.suppliedNum);
            return true;
        }
        return false;
    }

    public boolean isInPreparation() {
        return countWait < 0 || countReloadWait > 0;
    }

    public boolean isReloading() {
        return countReloadWait > 0;
    }

    public boolean canUse() {
        return countWait == 0;
    }

    public boolean isLongDelay() {
        return def.delay > 4;
    }

    public boolean isOverheated() {
        return def.maxHeatCount > 0 && currentHeat >= def.maxHeatCount;
    }

    public void reload() {
        if (magazineSize() > 0 && ammo < magazineSize() && def.reloadTime > 0) {
            countReloadWait = def.reloadTime;
            if (clientSide) {
                ammo = 0;
            } else {
                countReloadWait = Math.max(countReloadWait - 20, 1);
            }
        }
    }

    public void reloadMagazine() {
        int needed = magazineSize() - ammo;
        if (needed > 0) {
            needed = Math.min(needed, restAmmo);
            ammo += needed;
            setRestAmmo(restAmmo - needed);
        }
    }

    // Modes

    public int mode() {
        return def.fixMode > 0 ? def.fixMode : mode;
    }

    public boolean switchMode() {
        if (def.fixMode > 0) {
            return false;
        }
        int before = mode();
        mode = numMode > 0 ? (mode() + 1) % numMode : 0;
        if (before == mode()) {
            return false;
        }
        waitAfterSwitch(SWITCH_WAIT);
        return true;
    }

    /** {@code getName}: the display name with the mode suffix the weapon class added. */
    public String displayName() {
        String suffix = switch (def.type) {
            case MACHINE_GUN_2 -> mode() == 0 ? "" : " [HE]";
            case ROCKET -> mode() == 0 ? "" : " [HEIAP]";
            case AT_MISSILE -> mode() == 1 ? " [TA]" : "";
            case TV_MISSILE -> switch (mode()) {
                case 0 -> " [TV]";
                case 2 -> " [TA]";
                default -> "";
            };
            case null, default -> "";
        };
        return def.displayName + suffix;
    }

    public void onSwitchWeapon(boolean infiniteAmmo) {
        waitAfterSwitch(clientSide ? SWITCH_WAIT + 10 : SWITCH_WAIT);
        currentMount = 0;
        if (infiniteAmmo) {
            ammo = magazineSize();
        }
    }

    private void waitAfterSwitch(int wait) {
        if (countWait >= -wait) {
            countWait = countWait > wait ? -countWait : -wait;
        }
    }

    /** Another weapon of the same {@code Group} fired; this one waits and reloads with it. */
    public void waitForGroup(boolean reload) {
        countWait = interval;
        countReloadWait = 0;
        if (reload && magazineSize() > 0 && def.reloadTime > 0) {
            countReloadWait = clientSide ? def.reloadTime : Math.max(def.reloadTime - 20, 1);
        }
    }

    // Firing

    /**
     * Fires the next mount if the set is ready. The aim is the shooter's rotation plus this set's
     * turret aim and the mount's fixed offset, scattered by {@code Accuracy}.
     */
    public boolean use(Shot shot, Trigger trigger) {
        if (magazineSize() > 0 && ammo <= 0 || isOverheated()) {
            return false;
        }
        WeaponMount mount = mounts.get(currentMount);
        soundAllowed = soundWait == 0;
        float yaw = shot.shooter.getYRot() + rotationYaw + mount.yaw();
        float pitch = shot.shooter.getXRot() + rotationPitch + mount.pitch();
        if (def.accuracy > 0.0F) {
            yaw += (random.nextFloat() - 0.5F) * def.accuracy;
            pitch += (random.nextFloat() - 0.5F) * def.accuracy;
        }
        shot.yaw = Mth.wrapDegrees(yaw);
        shot.pitch = Mth.wrapDegrees(pitch);
        if (!trigger.fire(this, currentMount, shot)) {
            return false;
        }
        ticks[currentMount] = 0;

        if (def.maxHeatCount > 0) {
            cooldownSpeed = 1;
            currentHeat += heatCount;
            if (currentHeat >= def.maxHeatCount) {
                currentHeat += 30;
            }
        }
        if (def.soundDelay > 0 && soundWait == 0) {
            soundWait = def.soundDelay;
        }
        lastUsedCount[currentMount] = Math.abs(interval);
        if (isCooldownReloadTime() && def.reloadTime - 10 > lastUsedCount[currentMount]) {
            lastUsedCount[currentMount] = def.reloadTime - 10;
        }
        currentMount = (currentMount + 1) % mounts.size();
        countWait = interval;
        countReloadWait = 0;
        if (ammo > 0) {
            ammo--;
        }
        if (ammo <= 0) {
            if (shot.infiniteAmmo && restAmmo < magazineSize()) {
                setRestAmmo(magazineSize());
            }
            reload();
            shot.reload = true;
        }
        return true;
    }

    /** Missiles keep their mount empty for the whole reload, which hides the missile part meanwhile. */
    private boolean isCooldownReloadTime() {
        return def.type == WeaponType.AA_MISSILE || def.type == WeaponType.AS_MISSILE || def.type == WeaponType.AT_MISSILE;
    }

    /** Whether {@code mount} fired recently enough to show muzzle effects or keep a missile hidden. */
    public boolean isUsed(int mount) {
        if (mount >= lastUsedCount.length) {
            return false;
        }
        int count = lastUsedCount[mount];
        return interval >= 4 && count > interval / 2 || count >= 4;
    }

    public int ticksSinceShot(int mount) {
        return ticks[mount];
    }

    // Ticking

    /** {@code MCH_WeaponSet.update}, once per tick on both sides. */
    public void tick(boolean used) {
        if (countReloadWait > 0) {
            countReloadWait--;
            if (countReloadWait == 0) {
                reloadMagazine();
            }
        }
        for (int i = 0; i < lastUsedCount.length; i++) {
            if (lastUsedCount[i] > 0) {
                if (lastUsedCount[i] != 4 || currentMount == 0 && canUse() && (ammo > 0 || def.maxAmmo <= 0)) {
                    lastUsedCount[i]--;
                }
            }
        }
        if (currentHeat > 0) {
            if (currentHeat < def.maxHeatCount) {
                cooldownSpeed++;
            }
            currentHeat = Math.max(currentHeat - (cooldownSpeed / 20 + 1), 0);
        }
        if (countWait > 0) {
            countWait--;
        } else if (countWait < 0) {
            countWait++;
        }
        prevRotationYaw = rotationYaw;
        prevRotationPitch = rotationPitch;
        if (countWait != 0) {
            for (int i = 0; i < ticks.length; i++) {
                ticks[i]++;
            }
        }
        if (soundWait > 0) {
            soundWait--;
        }

        if (used && barrelSpeed < 75.0F) {
            barrelSpeed = Math.min(barrelSpeed + 25 + random.nextInt(3), 74.0F);
        }
        prevBarrel = barrel;
        barrel += barrelSpeed;
        if (barrel >= 360.0F) {
            barrel -= 360.0F;
            prevBarrel -= 360.0F;
        }
        if (barrelSpeed > 0.0F) {
            barrelSpeed = Math.max(barrelSpeed - 1.48F, 0.0F);
        }
    }

    /** {@code updateWeapon}: the recoil kick of one mount. */
    public void tickRecoil(int mount, boolean used) {
        Recoil r = recoil[mount];
        r.prev = r.value;
        if (used && r.count <= 0) {
            r.count = r.max;
        }
        if (r.count > 0) {
            if (r.max <= 1) {
                r.value = 1.0F;
            } else if (r.max == 2) {
                r.value = r.count == 2 ? 1.0F : 0.6F;
            } else {
                if (r.count > r.max / 2) {
                    r.count -= r.speed;
                }
                r.value = Mth.sin((float) r.count / r.max * Mth.PI);
            }
            r.count--;
        } else {
            r.value = 0.0F;
        }
    }

    public float recoil(int mount, float partialTicks) {
        Recoil r = recoil[Math.min(mount, recoil.length - 1)];
        return Mth.lerp(partialTicks, r.prev, r.value);
    }

    private static final class Recoil {
        final int max;
        final int speed;
        int count;
        float value;
        float prev;

        Recoil(int max, int speed) {
            this.max = max;
            this.speed = speed;
        }
    }
}
