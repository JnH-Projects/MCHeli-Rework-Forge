package com.jnhprojects.mcheli.client;

import com.jnhprojects.mcheli.MCHeli;
import com.jnhprojects.mcheli.ModSounds;
import com.jnhprojects.mcheli.item.RangefinderItem;
import com.jnhprojects.mcheli.network.ModNetwork;
import com.jnhprojects.mcheli.network.SpotPacket;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;

/**
 * Looking through the rangefinder, {@code MCH_ClientToolTickHandler} and {@code MCH_GuiRangeFinder}.
 * Once it has been held up for a moment the scope covers the view and reads the range; the zoom
 * key and the weapon mode key zoom in and out while held, the flare key picks what to spot, and
 * the attack key spots.
 */
final class RangefinderController {
    private static final Identifier SCOPE = Identifier.fromNamespaceAndPath(MCHeli.MOD_ID, "textures/gui/rangefinder.png");
    /** Ticks of holding it up before the scope shows, as in the original. */
    private static final int RAISE_TICKS = 8;
    private static final int COOLDOWN = 60;
    private static final float MIN_ZOOM = 1.2F;
    private static final float MAX_ZOOM = 10.0F;
    private static final double RANGE = 300.0;
    private static final int GREEN = 0xFF10FF20;
    private static final int RED = 0xFFFF1010;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int GREY = 0xFF404040;

    private static float zoom = 2.0F;
    private static SpotPacket.Mode mode = SpotPacket.Mode.PLAYERS_AND_VEHICLES;
    private static int cooldown;

    private RangefinderController() {
    }

    /** The rangefinder the player is looking through, or empty. */
    private static ItemStack scope(LocalPlayer player) {
        if (player == null || !player.isUsingItem() || player.getTicksUsingItem() <= RAISE_TICKS || !RangefinderItem.canUse(player)) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = player.getUseItem();
        return stack.getItem() instanceof RangefinderItem ? stack : ItemStack.EMPTY;
    }

    static boolean isScoped() {
        return !scope(Minecraft.getInstance().player).isEmpty();
    }

    static void onClientTick() {
        if (cooldown > 0) {
            cooldown--;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        ItemStack stack = scope(player);
        if (stack.isEmpty()) {
            zoom = 2.0F;
            return;
        }
        if (KeyBindings.FOLD.isDown() && zoom < MAX_ZOOM) {
            zoom = Math.min(zoom + zoom / 10.0F, MAX_ZOOM);
            player.level().playLocalSound(player, ModSounds.get("zoom"), SoundSource.PLAYERS, 0.05F, 1.0F);
        }
        if (KeyBindings.WEAPON_MODE.isDown() && zoom > MIN_ZOOM) {
            zoom = Math.max(zoom - zoom / 10.0F, MIN_ZOOM);
            player.level().playLocalSound(player, ModSounds.get("zoom"), SoundSource.PLAYERS, 0.05F, 0.9F);
        }
        while (KeyBindings.FLARE.consumeClick()) {
            mode = SpotPacket.Mode.values()[(mode.ordinal() + 1) % SpotPacket.Mode.values().length];
            // Alone in a world there is no one else to spot, as the original reasoned.
            boolean alone = mc.hasSingleplayerServer() && !mc.getSingleplayerServer().isPublished();
            if (alone && mode == SpotPacket.Mode.PLAYERS_AND_VEHICLES) {
                mode = SpotPacket.Mode.MOBS;
            }
            player.level().playLocalSound(player, ModSounds.get("lockon"), SoundSource.PLAYERS, 1.0F, 0.9F);
        }
        if (mc.options.keyAttack.isDown() && cooldown == 0) {
            if (mode != SpotPacket.Mode.MARK_POINT && RangefinderItem.charges(stack) <= 0) {
                player.level().playLocalSound(player, ModSounds.get("ng"), SoundSource.PLAYERS, 1.0F, 1.0F);
            } else {
                cooldown = COOLDOWN;
                ModNetwork.sendToServer(new SpotPacket(mode));
            }
        }
    }

    static float fov(float fov) {
        return isScoped() ? fov / zoom : fov;
    }

    static void drawHud(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        ItemStack stack = scope(player);
        if (stack.isEmpty() || !mc.options.getCameraType().isFirstPerson()) {
            return;
        }
        int w = g.guiWidth();
        int h = g.guiHeight();
        int size = Math.max(w, h);
        g.blit(RenderPipelines.GUI_TEXTURED, SCOPE, (w - size) / 2, (h - size) / 2, 0.0F, 0.0F, size, size, 256, 256, 256, 256);

        Font font = mc.font;
        int cx = w / 2;
        int cy = h / 2;
        int charge = RangefinderItem.charges(stack) * 100 / RangefinderItem.CHARGES;
        g.text(font, Component.translatable("hud.mcheli.rangefinder.charge", charge), cx - 80, cy + 50, charge > 0 ? GREEN : RED);
        if (charge <= 0) {
            g.text(font, Component.translatable("hud.mcheli.rangefinder.empty"), cx - 80, cy + 60, RED);
        } else {
            Vec3 eye = player.getEyePosition();
            HitResult hit = player.level().clip(new ClipContext(eye, eye.add(player.getLookAngle().scale(RANGE)),
                    ClipContext.Block.OUTLINE, ClipContext.Fluid.ANY, player));
            String range = hit.getType() == HitResult.Type.MISS ? "----" : String.format("%4d", Math.round(hit.getLocation().distanceTo(eye)));
            g.text(font, Component.translatable("hud.mcheli.rangefinder.range", range), cx - 20, cy + 50, GREEN);
        }
        g.fill(cx - 80, cy + 44, cx - 50, cy + 46, GREEN);
        g.fill(cx - 80, cy + 44, cx - 80 + cooldown / 2, cy + 46, RED);
        g.text(font, String.format("x%.1f", zoom), cx - 80, cy + 30, WHITE);
        int y = cy + 20;
        for (SpotPacket.Mode m : SpotPacket.Mode.values()) {
            String prefix = m == mode ? "> " : "  ";
            g.text(font, Component.literal(prefix).append(Component.translatable("hud.mcheli.rangefinder." + m.name().toLowerCase(Locale.ROOT))),
                    cx + 50, y, m == mode ? WHITE : GREY);
            y += 10;
        }
    }
}
