package com.jnhprojects.mcheli.client;

import com.jnhprojects.mcheli.entity.GltdEntity;
import com.jnhprojects.mcheli.network.EquipmentActionPacket;
import com.jnhprojects.mcheli.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * The local player at a GLTD, {@code MCH_ClientGLTDTickHandler} and {@code MCH_GuiGLTD}: the
 * view moves to the sight, the zoom and camera mode keys work as in a vehicle's sight, the use
 * key calls the A-10 on the spot under the cross, and a small readout says when it can be called
 * again.
 */
final class GltdController {
    /** The original zoomed smoothly up to ten; the vehicle sights double each press. */
    private static final int MAX_ZOOM = 8;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int AMBER = 0xFFFFC040;
    private static final int CROSS = 0xC0FF4040;

    /** Edge of the use key, so holding it calls one strike rather than one per tick. */
    private static boolean useWasDown;

    private GltdController() {
    }

    static @Nullable GltdEntity gltdOf(@Nullable LocalPlayer player) {
        return player != null && player.getVehicle() instanceof GltdEntity gltd ? gltd : null;
    }

    static void onClientTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        GltdEntity gltd = gltdOf(player);
        if (gltd == null) {
            useWasDown = false;
            return;
        }
        while (KeyBindings.FOLD.consumeClick()) {
            VehicleCamera.zoomStep(player, MAX_ZOOM);
        }
        while (KeyBindings.CAMERA_MODE.consumeClick()) {
            VehicleCamera.nextMode(player, on -> send(gltd,
                    on ? EquipmentActionPacket.Action.NIGHT_VISION_ON : EquipmentActionPacket.Action.NIGHT_VISION_OFF));
        }
        boolean useDown = mc.screen == null && mc.options.keyUse.isDown();
        if (useDown && !useWasDown) {
            send(gltd, EquipmentActionPacket.Action.CALL_STRIKE);
        }
        useWasDown = useDown;
    }

    /** The use key belongs to the sight while sitting at a GLTD. */
    static boolean onUseItem() {
        return gltdOf(Minecraft.getInstance().player) != null;
    }

    /** First person looks out of the sight rather than from behind the tripod. */
    static void onCameraAngles(CameraAngles angles) {
        Minecraft mc = Minecraft.getInstance();
        GltdEntity gltd = gltdOf(mc.player);
        if (gltd != null && mc.options.getCameraType().isFirstPerson()) {
            CrewController.placeCamera(angles.camera(), gltd.sightPosition(angles.partialTick()));
        }
    }

    static boolean hidesHand() {
        return gltdOf(Minecraft.getInstance().player) != null;
    }

    static void drawHud(GuiGraphics g, float delta) {
        Minecraft mc = Minecraft.getInstance();
        GltdEntity gltd = gltdOf(mc.player);
        if (gltd == null || !mc.options.getCameraType().isFirstPerson()) {
            return;
        }
        int cx = g.guiWidth() / 2;
        int cy = g.guiHeight() / 2;
        g.fill(cx - 12, cy, cx - 3, cy + 1, CROSS);
        g.fill(cx + 4, cy, cx + 13, cy + 1, CROSS);
        g.fill(cx, cy - 12, cx + 1, cy - 3, CROSS);
        g.fill(cx, cy + 4, cx + 1, cy + 13, CROSS);

        Font font = mc.font;
        int x = cx + 20;
        int y = cy + 20;
        int wait = gltd.waitTicks();
        if (wait > 0) {
            g.drawString(font, Component.translatable("hud.mcheli.gltd.wait", (wait + 19) / 20), x, y, AMBER);
        } else {
            g.drawString(font, Component.translatable("hud.mcheli.gltd.ready"), x, y, WHITE);
        }
        g.drawString(font, Component.translatable("hud.mcheli.zoom", Math.round(VehicleCamera.zoom())), x, y + 10, WHITE);
    }

    private static void send(GltdEntity gltd, EquipmentActionPacket.Action action) {
        ModNetwork.sendToServer(new EquipmentActionPacket(gltd.getId(), action));
    }
}
