package com.jnhprojects.mcheli.client;

import com.jnhprojects.mcheli.ModSounds;
import com.jnhprojects.mcheli.content.weapon.WeaponDefinition;
import com.jnhprojects.mcheli.entity.AircraftEntity;
import com.jnhprojects.mcheli.network.CrewActionPacket;
import com.jnhprojects.mcheli.network.FireWeaponPacket;
import com.jnhprojects.mcheli.network.LockWarningPacket;
import com.jnhprojects.mcheli.network.ModNetwork;
import com.jnhprojects.mcheli.weapon.Guidance;
import com.jnhprojects.mcheli.weapon.Shot;
import com.jnhprojects.mcheli.weapon.VehicleWeapons;
import com.jnhprojects.mcheli.weapon.WeaponSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The crew member's trigger: fires, switches weapons and modes, drops flares and resupplies.
 *
 * <p>Like the original, the client fires first. Its copy of the weapon decides whether the shot
 * can happen (ready, loaded, locked on, a target in sight) and works out the option values; only
 * then is the server asked, which fires the real projectile.
 */
final class WeaponController {
    private WeaponController() {
    }

    private record Seat(AircraftEntity vehicle, VehicleWeapons weapons, int seat) {
    }

    private static @Nullable Seat seat(@Nullable LocalPlayer player) {
        if (player == null || !(player.getVehicle() instanceof AircraftEntity vehicle) || vehicle.definition() == null) {
            return null;
        }
        VehicleWeapons weapons = vehicle.weapons();
        int seat = vehicle.seatOf(player);
        if (weapons == null || seat < 0 || seat >= vehicle.definition().seatCount()) {
            return null;
        }
        return new Seat(vehicle, weapons, seat);
    }

    /** The use key is the trigger while the seat has a weapon, so it must not also use items. */
    static boolean onUseItem() {
        Seat s = seat(Minecraft.getInstance().player);
        return s != null && s.weapons.selected(s.seat) >= 0;
    }

    static boolean onPickBlock() {
        Seat s = seat(Minecraft.getInstance().player);
        if (s == null || s.weapons.sets().isEmpty()) {
            return false;
        }
        send(s.vehicle, Minecraft.getInstance().player != null && Minecraft.getInstance().player.isShiftKeyDown()
                ? CrewActionPacket.Action.PREVIOUS_WEAPON : CrewActionPacket.Action.NEXT_WEAPON);
        click();
        return true;
    }

    static void onClientTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        Seat s = seat(player);
        if (s == null) {
            return;
        }
        while (KeyBindings.SWITCH_WEAPON.consumeClick()) {
            send(s.vehicle, player.isShiftKeyDown() ? CrewActionPacket.Action.PREVIOUS_WEAPON : CrewActionPacket.Action.NEXT_WEAPON);
            click();
        }
        while (KeyBindings.WEAPON_MODE.consumeClick()) {
            WeaponSet set = s.weapons.current(s.seat);
            if (set != null && set.switchMode()) {
                send(s.vehicle, CrewActionPacket.Action.WEAPON_MODE);
                click();
            }
        }
        while (KeyBindings.FLARE.consumeClick()) {
            if (s.seat <= 1 && s.weapons.flares.use()) {
                send(s.vehicle, CrewActionPacket.Action.FLARE);
                click();
            }
        }
        while (KeyBindings.RESUPPLY.consumeClick()) {
            send(s.vehicle, CrewActionPacket.Action.RESUPPLY);
        }

        if (mc.screen == null && mc.options.keyUse.isDown() && !s.vehicle.isDestroyed()) {
            fire(s, player);
        }
    }

    private static void fire(Seat s, LocalPlayer player) {
        int id = s.weapons.selected(s.seat);
        if (id < 0 || s.weapons.operatorSeat(id) != s.seat) {
            return;
        }
        Shot shot = new Shot(s.vehicle, player);
        boolean fired = s.weapons.use(player, s.seat, shot, (set, mount, sh) -> clientShot(s, id, set, sh, player));
        if (fired) {
            ModNetwork.sendToServer(new FireWeaponPacket(s.vehicle.getId(), id, shot.option1, shot.option2));
        }
    }

    /** The client half of each weapon's {@code shot}: can it fire, and with which options. */
    private static boolean clientShot(Seat s, int id, WeaponSet set, Shot shot, LocalPlayer player) {
        WeaponDefinition w = set.definition();
        return switch (w.type) {
            case MACHINE_GUN_2, ROCKET, MARKER_ROCKET -> {
                shot.option1 = set.mode();
                yield true;
            }
            case AA_MISSILE, AT_MISSILE -> {
                Guidance g = s.weapons.guidance(id);
                if (g == null) {
                    yield false;
                }
                boolean locked = g.lock(player, viewPoint(player));
                if (g.shouldNotifyTarget() && g.target() != null) {
                    ModNetwork.sendToServer(new LockWarningPacket(g.target().getId()));
                    player.level().playLocalSound(player, ModSounds.get("lockon"), SoundSource.PLAYERS, 1.0F, 1.0F);
                }
                if (!locked || g.lastLocked() == null) {
                    yield false;
                }
                shot.option1 = g.lastLocked().getId();
                shot.option2 = set.mode();
                yield true;
            }
            case TV_MISSILE -> {
                shot.option1 = set.mode();
                shot.option2 = 0;
                yield true;
            }
            case AS_MISSILE -> landAhead(s.vehicle, player, 1.62, 200.0, false) != null;
            case CAS -> landAhead(s.vehicle, player, 2.0, 80.0, false) != null;
            case TORPEDO -> !w.guidedTorpedo || landAhead(player, player, 0.0, 100.0, true) != null;
            case DUMMY, SMOKE -> false;
            case null -> false;
            default -> true;
        };
    }

    /**
     * Where the player sees from: in first person the camera, which CrewController moves to gunner
     * sights and launcher cameras; otherwise the player's eyes.
     */
    private static Vec3 viewPoint(LocalPlayer player) {
        Minecraft mc = Minecraft.getInstance();
        return mc.options.getCameraType().isFirstPerson() ? mc.gameRenderer.getMainCamera().getPosition() : player.getEyePosition();
    }

    /** A block along the player's view: dry ground for strike weapons, water for guided torpedoes. */
    private static @Nullable BlockHitResult landAhead(Entity origin, LocalPlayer player, double eyeHeight, double range, boolean water) {
        Vec3 from = origin.position().add(0.0, eyeHeight, 0.0);
        Vec3 to = from.add(Vec3.directionFromRotation(player.getXRot(), player.getYRot()).scale(range));
        BlockHitResult hit = player.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (hit.getType() != HitResult.Type.BLOCK) {
            return null;
        }
        boolean wet = false;
        BlockPos pos = hit.getBlockPos();
        for (Direction d : Direction.values()) {
            wet |= player.level().getFluidState(pos.relative(d)).is(FluidTags.WATER);
        }
        return wet == water ? hit : null;
    }

    private static void send(AircraftEntity vehicle, CrewActionPacket.Action action) {
        ModNetwork.sendToServer(new CrewActionPacket(vehicle.getId(), action));
    }

    private static void click() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            player.level().playLocalSound(player, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.3F, 1.0F);
        }
    }
}
