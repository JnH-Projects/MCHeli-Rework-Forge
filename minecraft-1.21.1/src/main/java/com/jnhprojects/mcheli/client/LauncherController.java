package com.jnhprojects.mcheli.client;

import com.jnhprojects.mcheli.MCHeli;
import com.jnhprojects.mcheli.ModSounds;
import com.jnhprojects.mcheli.content.weapon.WeaponDefinition;
import com.jnhprojects.mcheli.content.weapon.WeaponType;
import com.jnhprojects.mcheli.item.LauncherItem;
import com.jnhprojects.mcheli.network.LauncherPacket;
import com.jnhprojects.mcheli.network.ModNetwork;
import com.jnhprojects.mcheli.weapon.Guidance;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * The Stinger and Javelin in the player's hands, {@code MCH_ClientLightWeaponTickHandler} and
 * {@code MCH_GuiLightWeapon}. Raised, the launcher's sight covers the view and its guidance locks
 * onto what the cross holds; the attack key fires once the lock is complete. Z steps the zoom, X
 * the Javelin's direct and top attack modes. An empty launcher held down reloads itself from a
 * missile in the inventory.
 */
final class LauncherController {
    /** Ticks of raising before the sight and the lock start, as in the original. */
    private static final int RAISE_TICKS = 10;
    private static final int RELOAD_TICKS = 40;
    /** Top attack climbs first and needs this much room, measured flat. */
    private static final double TOP_ATTACK_MIN_DISTANCE = 40.0;
    private static final int GREEN = 0xFF2AD848;
    private static final int RED = 0xFFDF0808;
    private static final int WHITE = 0xFFFFFFFF;

    private static @Nullable String weapon;
    private static @Nullable Guidance guidance;
    private static int zoomIndex;
    private static int mode;
    private static int reloadTicks;
    private static int lockSoundTicks;
    private static boolean attackWasDown;

    private LauncherController() {
    }

    private static ItemStack launcher(@Nullable LocalPlayer player) {
        if (player == null || !LauncherItem.canUse(player)) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
        return stack.getItem() instanceof LauncherItem ? stack : ItemStack.EMPTY;
    }

    /** Whether the player holds a launcher up to the eye. */
    static boolean isRaised() {
        LocalPlayer player = Minecraft.getInstance().player;
        ItemStack stack = launcher(player);
        return !stack.isEmpty() && player.isUsingItem() && player.getUseItem() == stack && player.getTicksUsingItem() > RAISE_TICKS;
    }

    private static @Nullable WeaponDefinition definition(ItemStack stack) {
        return MCHeli.content().weapon(((LauncherItem) stack.getItem()).weapon());
    }

    static void onClientTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        ItemStack stack = launcher(player);
        WeaponDefinition def = stack.isEmpty() ? null : definition(stack);
        if (def == null) {
            weapon = null;
            guidance = null;
            reloadTicks = 0;
            attackWasDown = false;
            return;
        }
        String name = ((LauncherItem) stack.getItem()).weapon();
        if (!name.equals(weapon) || guidance == null) {
            weapon = name;
            boolean air = def.type != WeaponType.AT_MISSILE;
            guidance = new Guidance(air, !air, def.ridableOnly, def.lockTime, e -> e != player && !e.isAlliedTo(player));
            zoomIndex = 0;
            mode = 0;
        }
        boolean raised = isRaised();
        boolean attackDown = raised && mc.screen == null && mc.options.keyAttack.isDown();
        boolean attackPressed = attackDown && !attackWasDown;
        attackWasDown = attackDown;

        if (LauncherItem.isLoaded(stack)) {
            reloadTicks = 0;
            if (raised) {
                if (guidance.lock(player, player.getEyePosition())) {
                    lockSoundTicks = 0;
                }
                playLockSound(player);
            } else {
                guidance.clear();
            }
        } else {
            guidance.clear();
            reload(player, stack);
        }
        guidance.tick();

        if (!raised) {
            return;
        }
        while (KeyBindings.FOLD.consumeClick()) {
            if (def.zoom.length > 1) {
                zoomIndex = (zoomIndex + 1) % def.zoom.length;
                player.level().playLocalSound(player, ModSounds.get("zoom"), SoundSource.PLAYERS, 0.5F, 1.0F);
            }
        }
        while (KeyBindings.WEAPON_MODE.consumeClick()) {
            if (def.modeNum > 1) {
                mode = (mode + 1) % def.modeNum;
                player.level().playLocalSound(player, ModSounds.get("pi"), SoundSource.PLAYERS, 0.5F, 0.9F);
            }
        }
        if (attackPressed) {
            Entity target = guidance.lastLocked();
            if (LauncherItem.isLoaded(stack) && guidance.isLocked() && target != null && canFire(player, target)) {
                ModNetwork.sendToServer(new LauncherPacket(LauncherPacket.Action.FIRE, target.getId(), mode));
                guidance.clear();
            } else {
                player.level().playLocalSound(player, ModSounds.get("ng"), SoundSource.PLAYERS, 0.5F, 1.0F);
            }
        }
    }

    /** Top attack needs room to climb and dive. */
    private static boolean canFire(LocalPlayer player, @Nullable Entity target) {
        if (mode == 0 || target == null) {
            return true;
        }
        double dx = target.getX() - player.getX();
        double dz = target.getZ() - player.getZ();
        return Math.sqrt(dx * dx + dz * dz) >= TOP_ATTACK_MIN_DISTANCE;
    }

    /** The tone quickens as the lock builds. */
    private static void playLockSound(LocalPlayer player) {
        if (guidance == null || guidance.lockCount() <= 0) {
            lockSoundTicks = 0;
            return;
        }
        if (lockSoundTicks > 0) {
            lockSoundTicks--;
            return;
        }
        lockSoundTicks = Math.max(2, (int) (7 * (1.0 - (double) guidance.lockCount() / guidance.lockCountMax())));
        player.level().playLocalSound(player, ModSounds.get("lockon"), SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    /** Held down and empty, with a missile to hand: two seconds to load it. */
    private static void reload(LocalPlayer player, ItemStack stack) {
        LauncherItem item = (LauncherItem) stack.getItem();
        boolean hasMissile = player.getAbilities().instabuild || player.getInventory().findSlotMatchingItem(new ItemStack(item.missile())) >= 0;
        if (!hasMissile || player.isUsingItem()) {
            reloadTicks = 0;
            return;
        }
        reloadTicks++;
        if (reloadTicks == 10) {
            player.level().playLocalSound(player, ModSounds.get("fim92_reload"), SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        if (reloadTicks == RELOAD_TICKS) {
            ModNetwork.sendToServer(new LauncherPacket(LauncherPacket.Action.RELOAD, 0, 0));
        }
    }

    static float fov(float fov) {
        if (!isRaised()) {
            return fov;
        }
        WeaponDefinition def = definition(launcher(Minecraft.getInstance().player));
        if (def == null || def.zoom.length == 0) {
            return fov;
        }
        float zoom = def.zoom[Math.min(zoomIndex, def.zoom.length - 1)];
        return zoom > 1.0F ? fov / zoom : fov;
    }

    static void drawHud(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (!mc.options.getCameraType().isFirstPerson()) {
            return;
        }
        ItemStack stack = launcher(player);
        if (stack.isEmpty()) {
            return;
        }
        Font font = mc.font;
        if (!isRaised()) {
            if (!LauncherItem.isLoaded(stack) && reloadTicks > 0) {
                g.drawString(font, Component.translatable("hud.mcheli.launcher.reloading"), g.guiWidth() / 2 - 30, g.guiHeight() / 2 + 20, WHITE);
            }
            return;
        }
        WeaponDefinition def = definition(stack);
        int w = g.guiWidth();
        int h = g.guiHeight();
        int size = Math.max(w, h);
        String texture = def != null && def.type == WeaponType.AT_MISSILE ? "javelin" : "stinger";
        g.blit(ResourceLocation.fromNamespaceAndPath(MCHeli.MOD_ID, "textures/gui/" + texture + ".png"), (w - size) / 2, (h - size) / 2, size, size, 0.0F, 0.0F, 256, 256, 256, 256);

        int x = w / 2 + 20;
        int y = h / 2 + 20;
        if (!LauncherItem.isLoaded(stack)) {
            g.drawString(font, Component.translatable("hud.mcheli.launcher.empty"), x, y, RED);
        } else if (guidance != null && guidance.isLocked()) {
            Entity target = guidance.lastLocked();
            g.drawString(font, Component.translatable("hud.mcheli.locked"), x, y, canFire(player, target) ? GREEN : RED);
            if (target != null) {
                g.drawString(font, String.format("[%.0f m]", target.distanceTo(player)), x, y + 10, GREEN);
            }
        } else if (guidance != null && guidance.lockCount() > 0) {
            g.drawString(font, Component.translatable("hud.mcheli.locking", guidance.lockCount() * 100 / Math.max(guidance.lockCountMax(), 1)), x, y, WHITE);
        }
        if (def != null && def.modeNum > 1) {
            g.drawString(font, Component.translatable(mode == 0 ? "hud.mcheli.launcher.direct" : "hud.mcheli.launcher.top_attack"), x, y + 20, WHITE);
        }
        if (def != null && def.zoom.length > 0) {
            g.drawString(font, Component.translatable("hud.mcheli.zoom", String.format("%.1f", def.zoom[Math.min(zoomIndex, def.zoom.length - 1)])),
                    x, y + 30, WHITE);
        }
    }
}
