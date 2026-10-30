package com.jnhprojects.mcheli.client;

import com.jnhprojects.mcheli.client.hud.HudRenderer;
import com.jnhprojects.mcheli.content.aircraft.AircraftDefinition;
import com.jnhprojects.mcheli.entity.AircraftEntity;
import com.jnhprojects.mcheli.weapon.Guidance;
import com.jnhprojects.mcheli.weapon.VehicleWeapons;
import com.jnhprojects.mcheli.weapon.WeaponSet;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

/**
 * The vehicle HUD layer. In first person it runs the seat's HUD script (the {@code HUD} line of
 * the definition, or the kind's default), or {@code tv_missile} while flying a TV missile, as the
 * original did; a seat whose HUD is {@code none} gets nothing. Where no script draws (third
 * person, a destroyed vehicle, a script that does not exist) it shows a plain text readout of hit
 * points, ammunition, reload, heat, lock and flares.
 */
final class WeaponHud {
    private static final int WHITE = 0xFFFFFFFF;
    private static final int AMBER = 0xFFFFC040;
    private static final int RED = 0xFFFF5050;
    private static final int GREEN = 0xFF60FF60;
    private static final String NO_HUD = "none";

    private WeaponHud() {
    }

    /** A pilot in gunner mode gets the gunner seat's HUD, as in the original. */
    private static int hudSeat(AircraftEntity vehicle, int seat) {
        return seat == 0 && vehicle.isGunnerMode() ? 1 : seat;
    }

    static void draw(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || !(player.getVehicle() instanceof AircraftEntity vehicle)) {
            return;
        }
        AircraftDefinition def = vehicle.definition();
        VehicleWeapons weapons = vehicle.weapons();
        int seat = vehicle.seatOf(player);
        if (def == null || weapons == null || seat < 0 || seat >= def.seatCount()) {
            return;
        }
        if (mc.options.getCameraType().isFirstPerson() && !vehicle.isDestroyed()) {
            String hud = CrewController.tvCamera(vehicle, seat) != null ? "tv_missile"
                    : hudSeat(vehicle, seat) < def.huds.size() ? def.huds.get(hudSeat(vehicle, seat)) : null;
            if (NO_HUD.equals(hud)) {
                // The original's way of saying a seat has no HUD at all.
                return;
            }
            if (hud != null && HudRenderer.draw(g, vehicle, def, player, seat, hud, delta.getGameTimeDeltaPartialTick(false))) {
                return;
            }
        }
        Font font = mc.font;
        int x = g.guiWidth() - 160;
        int y = g.guiHeight() - 92;

        float hp = (float) vehicle.hp() / def.maxHp;
        int hpColor = vehicle.isDestroyed() ? RED : hp < 0.5F ? AMBER : GREEN;
        g.drawString(font, Component.translatable("hud.mcheli.hp", vehicle.hp(), def.maxHp), x, y, hpColor);
        y += 11;

        WeaponSet set = weapons.current(seat);
        if (set != null) {
            g.drawString(font, set.displayName(), x, y, WHITE);
            y += 11;
            Component ammo = vehicle.hasInfiniteAmmo(player) && set.magazineSize() > 0
                    ? Component.translatable("hud.mcheli.ammo_infinite", set.ammo())
                    : set.magazineSize() > 0
                    ? Component.translatable("hud.mcheli.ammo", set.ammo(), set.restAmmo())
                    : Component.translatable("hud.mcheli.ammo_none");
            g.drawString(font, ammo, x, y, set.ammo() == 0 && set.magazineSize() > 0 ? RED : WHITE);
            y += 11;
            Component status = null;
            int color = AMBER;
            if (set.isReloading()) {
                status = Component.translatable("hud.mcheli.reloading");
            } else if (set.isOverheated()) {
                status = Component.translatable("hud.mcheli.overheat");
                color = RED;
            } else if (set.isInPreparation()) {
                status = Component.translatable("hud.mcheli.preparing");
            } else if (set.magazineSize() > 0 && set.ammo() == 0 && set.restAmmo() == 0) {
                status = Component.translatable("hud.mcheli.resupply", KeyBindings.RESUPPLY.getTranslatedKeyMessage());
                color = RED;
            }
            Guidance lock = weapons.guidance(weapons.selected(seat));
            if (status == null && lock != null && lock.lockCount() > 0) {
                status = lock.isLocked()
                        ? Component.translatable("hud.mcheli.locked")
                        : Component.translatable("hud.mcheli.locking", lock.lockCount() * 100 / Math.max(lock.lockCountMax(), 1));
                color = lock.isLocked() ? RED : AMBER;
            }
            if (status != null) {
                g.drawString(font, status, x, y, color);
                y += 11;
            }
            if (set.definition().maxHeatCount > 0) {
                int width = 100;
                int filled = Math.min(width, set.currentHeat * width / set.definition().maxHeatCount);
                g.fill(x, y + 2, x + width, y + 5, 0x80000000);
                g.fill(x, y + 2, x + filled, y + 5, set.isOverheated() ? RED : AMBER);
                y += 8;
            }
        } else if (!weapons.sets().isEmpty()) {
            g.drawString(font, Component.translatable("hud.mcheli.no_weapon"), x, y, WHITE);
            y += 11;
        }
        if (seat <= 1 && weapons.flares.isFitted()) {
            Component flare = vehicle.isFlareActive() ? Component.translatable("hud.mcheli.flare_active")
                    : weapons.flares.isPreparing() ? Component.translatable("hud.mcheli.flare_wait")
                    : Component.translatable("hud.mcheli.flare_ready", KeyBindings.FLARE.getTranslatedKeyMessage());
            g.drawString(font, flare, x, y, vehicle.isFlareActive() ? GREEN : WHITE);
        }
    }
}
