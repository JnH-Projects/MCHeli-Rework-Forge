package com.jnhprojects.mcheli.weapon;

import com.jnhprojects.mcheli.MCHeli;
import com.jnhprojects.mcheli.content.Vector3;
import com.jnhprojects.mcheli.content.aircraft.AircraftDefinition;
import com.jnhprojects.mcheli.content.aircraft.WeaponGroup;
import com.jnhprojects.mcheli.content.aircraft.WeaponMount;
import com.jnhprojects.mcheli.content.aircraft.part.WeaponBayPart;
import com.jnhprojects.mcheli.content.weapon.MuzzleFlash;
import com.jnhprojects.mcheli.content.weapon.WeaponDefinition;
import com.jnhprojects.mcheli.content.weapon.WeaponType;
import com.jnhprojects.mcheli.entity.AircraftEntity;
import com.jnhprojects.mcheli.network.ModNetwork;
import com.jnhprojects.mcheli.network.WeaponStatePacket;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The armament of one vehicle: its weapon sets, which one each seat has selected, the flares, and
 * the effects every client derives from the synced firing state. Ported from the weapon handling
 * in {@code MCH_EntityAircraft} ({@code createWeapon}, {@code switchWeapon},
 * {@code useCurrentWeapon}, {@code updateWeapons} and {@code updateWeaponsRotation}).
 */
public final class VehicleWeapons {
    private final AircraftEntity vehicle;
    private final AircraftDefinition def;
    private final boolean clientSide;
    private final List<WeaponSet> sets = new ArrayList<>();
    private final int[] selected;
    private final @Nullable Guidance[] guidance;
    private final RandomSource random = RandomSource.create();

    /** One bit per mount, set while that mount shows muzzle effects. Synced by the vehicle. */
    private int useStat;
    private int prevUseStat;

    private final float[] bayAngle;
    private final float[] prevBayAngle;

    private final int[] sentAmmo;
    private final int[] sentRest;

    public final FlareDispenser flares;

    // Recoil that kicks the whole hull, from Recoil in the weapon file.
    private int recoilCount;
    private float recoilValue;
    private float recoilYaw;

    public VehicleWeapons(AircraftEntity vehicle, AircraftDefinition def) {
        this.vehicle = vehicle;
        this.def = def;
        this.clientSide = vehicle.level().isClientSide();
        for (WeaponGroup group : def.weaponGroups) {
            WeaponDefinition weapon = MCHeli.content().weapon(group.weaponType());
            if (weapon != null && !group.mounts().isEmpty()) {
                sets.add(new WeaponSet(weapon, group.mounts(), clientSide));
            }
        }
        selected = new int[def.seatCount()];
        Arrays.fill(selected, -1);
        guidance = new Guidance[sets.size()];
        for (int i = 0; i < sets.size(); i++) {
            WeaponDefinition w = sets.get(i).definition();
            if (w.type == WeaponType.AA_MISSILE || w.type == WeaponType.AT_MISSILE) {
                boolean aa = w.type == WeaponType.AA_MISSILE;
                guidance[i] = new Guidance(aa, !aa, w.ridableOnly, w.lockTime, this::canLock);
            }
        }
        bayAngle = new float[def.weaponBays.size()];
        prevBayAngle = new float[def.weaponBays.size()];
        sentAmmo = new int[sets.size()];
        sentRest = new int[sets.size()];
        Arrays.fill(sentAmmo, -1);
        flares = new FlareDispenser(vehicle, def);
    }

    public List<WeaponSet> sets() {
        return sets;
    }

    public @Nullable WeaponSet set(int id) {
        return id >= 0 && id < sets.size() ? sets.get(id) : null;
    }

    public int indexOf(String weapon) {
        for (int i = 0; i < sets.size(); i++) {
            if (sets.get(i).name().equalsIgnoreCase(weapon)) {
                return i;
            }
        }
        return -1;
    }

    public @Nullable WeaponSet byName(String weapon) {
        return set(indexOf(weapon));
    }

    public @Nullable Guidance guidance(int id) {
        return id >= 0 && id < guidance.length ? guidance[id] : null;
    }

    /** A vehicle does not lock its own crew or cargo. */
    private boolean canLock(Entity entity) {
        return entity != vehicle && !vehicle.hasPassenger(entity);
    }

    // Selection

    public int selected(int seat) {
        return seat >= 0 && seat < selected.length ? selected[seat] : -1;
    }

    public @Nullable WeaponSet current(int seat) {
        return set(selected(seat));
    }

    /** The seat that operates a weapon: its own seat, or the pilot while that seat has no player. */
    public int operatorSeat(int id) {
        WeaponSet set = set(id);
        if (set == null) {
            return -1;
        }
        WeaponMount mount = set.firstMount();
        if (mount.usableByPilot() && !(vehicle.occupant(mount.seatIndex()) instanceof Player)) {
            return 0;
        }
        return mount.seatIndex();
    }

    /** {@code getNextWeaponID}: the next weapon {@code seat} may use, stepping forwards or back. */
    public int next(int seat, int step) {
        if (sets.isEmpty() || seat < 0) {
            return -1;
        }
        int id = selected(seat);
        for (int i = 0; i < sets.size(); i++) {
            id = step >= 0 ? (id + 1) % sets.size() : id > 0 ? id - 1 : sets.size() - 1;
            WeaponMount mount = sets.get(id).firstMount();
            int owner = mount.seatIndex();
            if (owner == seat || seat == 0 && mount.usableByPilot() && !(vehicle.occupant(owner) instanceof Player)) {
                return id;
            }
        }
        return -1;
    }

    /** {@code initCurrentWeapon}: a player took a seat. Server only. */
    public void onSeated(Entity entity, int seat) {
        if (seat < 0 || seat >= selected.length) {
            return;
        }
        selected[seat] = -1;
        if (entity instanceof Player) {
            switchTo(entity, seat, next(seat, 1));
        } else {
            vehicle.publishWeaponSelection();
        }
    }

    public void onLeft(int seat) {
        if (seat >= 0 && seat < selected.length) {
            selected[seat] = -1;
            vehicle.publishWeaponSelection();
        }
    }

    /** {@code switchWeapon}. Server only; the selection reaches clients through synced data. */
    public void switchTo(Entity user, int seat, int id) {
        if (seat < 0 || seat >= selected.length || sets.isEmpty()) {
            return;
        }
        WeaponSet before = current(seat);
        if (before != null) {
            before.reload();
        }
        selected[seat] = Math.min(id, sets.size() - 1);
        WeaponSet now = current(seat);
        if (now != null) {
            now.onSwitchWeapon(vehicle.hasInfiniteAmmo(user));
        }
        vehicle.publishWeaponSelection();
    }

    public String encodeSelection() {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < selected.length; i++) {
            if (i > 0) {
                b.append(',');
            }
            b.append(selected[i]);
        }
        return b.toString();
    }

    /** Applies the server's selection on a client, running the switch delay where it changed. */
    public void decodeSelection(String encoded, @Nullable Entity localPlayer) {
        String[] parts = encoded.isEmpty() ? new String[0] : encoded.split(",");
        for (int i = 0; i < selected.length; i++) {
            int id;
            try {
                id = i < parts.length ? Integer.parseInt(parts[i]) : -1;
            } catch (NumberFormatException e) {
                id = -1;
            }
            if (selected[i] != id) {
                selected[i] = id;
                WeaponSet set = set(id);
                Entity occupant = vehicle.occupant(i);
                if (set != null && occupant != null && occupant == localPlayer) {
                    set.onSwitchWeapon(vehicle.hasInfiniteAmmo(occupant));
                    Guidance g = guidance(id);
                    if (g != null) {
                        g.clear();
                    }
                }
            }
        }
    }

    // Firing

    /**
     * {@code useCurrentWeapon}: fires the selected weapon of {@code seat} if it is ready. The
     * trigger decides what the shot does on this side.
     */
    public boolean use(Entity user, int seat, Shot shot, WeaponSet.Trigger trigger) {
        int id = selected(seat);
        WeaponSet set = set(id);
        if (set == null || !set.canUse() || vehicle.isDestroyed()) {
            return false;
        }
        shot.infiniteAmmo = vehicle.hasInfiniteAmmo(user);
        int mount = set.currentMount();
        boolean fired = set.use(shot, (s, m, sh) -> {
            Vec3 muzzle = muzzleOffset(s, m, 1.0F);
            sh.x += muzzle.x;
            sh.y += muzzle.y;
            sh.z += muzzle.z;
            return trigger.fire(s, m, sh);
        });
        if (!fired) {
            return false;
        }
        String group = set.definition().group;
        if (!group.isEmpty()) {
            for (WeaponSet other : sets) {
                if (other != set && group.equals(other.definition().group)) {
                    other.waitForGroup(shot.reload);
                }
            }
        }
        if (!clientSide) {
            int bit = bitOf(id, mount);
            if (bit < 32) {
                useStat |= 1 << bit;
            }
        }
        return true;
    }

    /** World offset of a mount from the vehicle origin; turret mounts turn with the turret. */
    public Vec3 muzzleOffset(WeaponSet set, int mount, float partialTicks) {
        WeaponMount m = set.mounts().get(mount);
        Vector3 local = m.turret() ? vehicle.onTurret(def, m.position(), partialTicks) : m.position();
        Vector3f v = vehicle.toWorldOffset(local);
        return new Vec3(v.x, v.y, v.z);
    }

    private int bitOf(int id, int mount) {
        int shift = 0;
        for (int i = 0; i < id; i++) {
            shift += sets.get(i).mounts().size();
        }
        return shift + mount;
    }

    /** Whether a mount is cooling down after a shot; missile parts are hidden meanwhile. */
    public boolean isMountUsed(int id, int mount) {
        int bit = bitOf(id, mount);
        return bit < 32 && (useStat & 1 << bit) != 0;
    }

    public int useStat() {
        return useStat;
    }

    public void setUseStat(int stat) {
        useStat = stat;
    }

    public void switchMode(int seat) {
        WeaponSet set = current(seat);
        if (set != null) {
            set.switchMode();
        }
    }

    // Ticking

    /** {@code updateWeapons} and {@code updateWeaponsRotation}, once per tick on both sides. */
    public void tick() {
        if (sets.isEmpty()) {
            flares.tick();
            return;
        }
        int previous = prevUseStat;
        if (!clientSide) {
            useStat |= usedStat();
            vehicle.publishWeaponUse(useStat);
        } else {
            useStat = vehicle.syncedWeaponUse();
        }
        prevUseStat = useStat;

        float yaw = Mth.wrapDegrees(vehicle.getYRot());
        float pitch = Mth.wrapDegrees(vehicle.getXRot());
        int bit = 0;
        for (int id = 0; id < sets.size(); id++) {
            WeaponSet set = sets.get(id);
            boolean longDelay = set.isLongDelay();
            boolean anyUsed = false;
            for (int mount = 0; mount < set.mounts().size(); mount++) {
                boolean wasUsed = bit < 32 && (previous & 1 << bit) != 0;
                boolean used = bit < 32 && (useStat & 1 << bit) != 0;
                if (longDelay && wasUsed && used) {
                    used = false;
                }
                anyUsed |= used;
                if (!wasUsed && used && set.definition().recoil > 0.0F) {
                    recoilCount = 30;
                    recoilValue = set.definition().recoil;
                    recoilYaw = set.rotationYaw;
                }
                if (clientSide && used) {
                    muzzleEffects(set, mount, yaw);
                }
                set.tickRecoil(mount, used);
                bit++;
            }
            set.tick(anyUsed);
            if (!clientSide) {
                ServerFiring.tick(vehicle, set);
            }
            updateRotation(set);
        }
        if (!clientSide) {
            useStat = 0;
            syncAmmo();
        } else {
            for (Guidance g : guidance) {
                if (g != null) {
                    g.tick();
                }
            }
        }
        if (recoilCount > 0) {
            recoilCount--;
        }
        tickBays();
        if (clientSide) {
            smoke();
        }
        flares.tick();
    }

    private int usedStat() {
        int stat = 0;
        int bit = 0;
        for (WeaponSet set : sets) {
            for (int mount = 0; mount < set.mounts().size() && bit < 32; mount++) {
                stat |= set.isUsed(mount) ? 1 << bit : 0;
                bit++;
            }
        }
        return stat;
    }

    /** Turret aim of one set, following whoever operates it, within the mount's traverse limits. */
    private void updateRotation(WeaponSet set) {
        if (vehicle.isDestroyed()) {
            return;
        }
        WeaponMount mount = set.firstMount();
        int seat = mount.seatIndex();
        Entity operator = vehicle.occupant(seat);
        if (mount.usableByPilot() && !(operator instanceof Player)) {
            operator = vehicle.occupant(0);
        }
        float hullYaw = Mth.wrapDegrees(vehicle.getYRot());
        float hullPitch = Mth.wrapDegrees(vehicle.getXRot());
        float turretYaw = vehicle.turretYaw();
        if (operator instanceof Player) {
            if ((int) mount.minYaw() != 0 || (int) mount.maxYaw() != 0) {
                float ty = mount.turret() ? turretYaw : 0.0F;
                float ey = Mth.wrapDegrees(operator.getYRot() - hullYaw - mount.defaultYaw() - ty);
                if (Math.abs((int) mount.minYaw()) < 360 && Math.abs((int) mount.maxYaw()) < 360) {
                    float targetYaw = Mth.clamp(ey, mount.minYaw(), mount.maxYaw());
                    float wy = set.rotationYaw - mount.defaultYaw() - ty;
                    if (targetYaw < wy) {
                        wy = wy - targetYaw > 15.0F ? wy - 15.0F : targetYaw;
                    } else if (targetYaw > wy) {
                        wy = targetYaw - wy > 15.0F ? wy + 15.0F : targetYaw;
                    }
                    set.rotationYaw = wy + mount.defaultYaw() + ty;
                } else {
                    set.rotationYaw = ey + ty;
                }
            }
            float ep = Mth.wrapDegrees(operator.getXRot() - hullPitch);
            set.rotationPitch = Mth.clamp(ep, mount.minPitch(), mount.maxPitch());
            set.rotationTurretYaw = 0.0F;
        } else {
            set.rotationTurretYaw = turretYaw;
        }
    }

    /** {@code updateRecoil}: the hull kick of a big gun, applied on the simulating side. */
    public float @Nullable [] recoilKick() {
        if (recoilCount < 12) {
            return null;
        }
        float rel = (float) Math.toRadians(recoilYaw - vehicle.roll());
        float kick = Mth.cos((float) Math.toRadians(recoilCount * 6)) * recoilValue;
        return new float[]{kick * Mth.cos(rel), kick * Mth.sin(rel)};
    }

    /** {@code updateWeaponBay}: bays swing open while a seated crew member has their weapon selected. */
    private void tickBays() {
        for (int i = 0; i < bayAngle.length; i++) {
            WeaponBayPart bay = def.weaponBays.get(i);
            boolean open = false;
            for (String weapon : bay.weapons()) {
                int id = indexOf(weapon);
                for (int seat = 0; seat < selected.length && id >= 0; seat++) {
                    if (selected[seat] == id && vehicle.occupant(seat) != null) {
                        open = true;
                    }
                }
            }
            prevBayAngle[i] = bayAngle[i];
            bayAngle[i] = Mth.clamp(bayAngle[i] + (open ? 3.0F : -3.0F), 0.0F, 90.0F);
        }
    }

    public float bayAngle(int bay, float partialTicks) {
        return bay < bayAngle.length ? Mth.lerp(partialTicks, prevBayAngle[bay], bayAngle[bay]) : 0.0F;
    }

    // Effects, client only

    /** {@code spawnParticleMuzzleFlash} plus the spent case of guns with {@code SetCartridge}. */
    private void muzzleEffects(WeaponSet set, int mount, float hullYaw) {
        Level level = vehicle.level();
        WeaponDefinition w = set.definition();
        Vec3 offset = muzzleOffset(set, mount, 1.0F);
        double px = vehicle.getX() + offset.x;
        double py = vehicle.getY() + offset.y;
        double pz = vehicle.getZ() + offset.z;
        Vec3 dir = Vec3.directionFromRotation(vehicle.getXRot() + set.rotationPitch, hullYaw + set.rotationYaw);
        for (MuzzleFlash mf : w.muzzleSmokes) {
            double x = px + dir.x * mf.distance();
            double y = py + dir.y * mf.distance();
            double z = pz + dir.z * mf.distance();
            for (int i = 0; i < mf.count(); i++) {
                double r = mf.range();
                level.addParticle(mf.size() >= 2.0F ? ParticleTypes.LARGE_SMOKE : ParticleTypes.SMOKE,
                        x + (random.nextDouble() - 0.5) * r, y + (random.nextDouble() - 0.5) * r, z + (random.nextDouble() - 0.5) * r,
                        (random.nextDouble() - 0.5) * 0.05, 0.02, (random.nextDouble() - 0.5) * 0.05);
            }
        }
        for (MuzzleFlash mf : w.muzzleFlashes) {
            int rgb = mf.color().argb() & 0xFFFFFF;
            level.addParticle(new DustParticleOptions(rgb, Mth.clamp(mf.size() * 4.0F, 0.5F, 4.0F)),
                    px + dir.x * mf.distance(), py + dir.y * mf.distance(), pz + dir.z * mf.distance(), 0.0, 0.0, 0.0);
        }
        if (w.cartridge != null) {
            Vec3 motion = vehicle.getDeltaMovement();
            Vec3 side = Vec3.directionFromRotation(0.0F, hullYaw + set.rotationYaw + w.cartridge.yaw() + 90.0F)
                    .scale(0.1 + w.cartridge.acceleration());
            level.addParticle(new ItemParticleOption(ParticleTypes.ITEM, Items.GOLD_NUGGET),
                    px, py, pz, motion.x + side.x, motion.y + 0.1, motion.z + side.z);
        }
    }

    /** {@code onUpdate_ParticleSmoke}: smoke generators trail while the pilot has them selected. */
    private void smoke() {
        WeaponSet set = current(0);
        if (set == null || set.definition().type != WeaponType.SMOKE || vehicle.visibleThrottle() <= 0.1F) {
            return;
        }
        WeaponDefinition w = set.definition();
        Vec3 back = Vec3.directionFromRotation(vehicle.getXRot(), vehicle.getYRot()).reverse();
        int rgb = w.color.argb() & 0xFFFFFF;
        for (int mount = 0; mount < set.mounts().size(); mount++) {
            if (random.nextFloat() > vehicle.visibleThrottle() * 1.5F) {
                continue;
            }
            Vec3 offset = muzzleOffset(set, mount, 1.0F);
            for (int i = 0; i < w.smokeNum; i++) {
                vehicle.level().addParticle(new DustParticleOptions(rgb, Mth.clamp(w.smokeSize * 2.0F, 0.5F, 4.0F)),
                        vehicle.getX() + offset.x + back.x, vehicle.getY() + offset.y + back.y, vehicle.getZ() + offset.z + back.z,
                        back.x * w.acceleration + (random.nextDouble() - 0.5) * 0.2,
                        back.y * w.acceleration + (random.nextDouble() - 0.5) * 0.2,
                        back.z * w.acceleration + (random.nextDouble() - 0.5) * 0.2);
            }
        }
    }

    // Ammunition

    /** Sends magazine and reserve counts to the crew whenever they change. Server only. */
    private void syncAmmo() {
        for (int id = 0; id < sets.size(); id++) {
            WeaponSet set = sets.get(id);
            if (set.ammo() != sentAmmo[id] || set.restAmmo() != sentRest[id]) {
                sentAmmo[id] = set.ammo();
                sentRest[id] = set.restAmmo();
                WeaponStatePacket packet = new WeaponStatePacket(vehicle.getId(), id, set.ammo(), set.restAmmo());
                for (Entity passenger : vehicle.getPassengers()) {
                    if (passenger instanceof ServerPlayer player) {
                        ModNetwork.sendToPlayer(player, packet);
                    }
                }
            }
        }
    }

    /** Forces a full resend, for a player who just boarded. */
    public void resendAmmo() {
        Arrays.fill(sentAmmo, -1);
    }

    public void applyAmmo(int id, int ammo, int rest) {
        WeaponSet set = set(id);
        if (set != null) {
            set.setAmmo(ammo);
            set.setRestAmmo(rest);
        }
    }

    /** {@code reloadAllWeapon}: tops up every magazine from its reserve. */
    public void reloadAll() {
        for (WeaponSet set : sets) {
            set.reloadMagazine();
        }
    }

    public int[] saveAmmo() {
        int[] out = new int[sets.size() * 2];
        for (int i = 0; i < sets.size(); i++) {
            out[i * 2] = sets.get(i).ammo();
            out[i * 2 + 1] = sets.get(i).restAmmo();
        }
        return out;
    }

    public void loadAmmo(int[] saved) {
        for (int i = 0; i < sets.size() && i * 2 + 1 < saved.length; i++) {
            sets.get(i).setAmmo(Math.min(saved[i * 2], sets.get(i).magazineSize()));
            sets.get(i).setRestAmmo(saved[i * 2 + 1]);
        }
    }
}
