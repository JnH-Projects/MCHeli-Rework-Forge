package com.jnhprojects.mcheli.client;

import com.jnhprojects.mcheli.team.Markers;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * {@code MCH_GuiTargetMarker}: the team's spotted targets and marked point on screen. A target
 * gets a triangle over its head with its distance; one off screen gets a diamond at the edge on
 * its side. The marked point gets a diamond with its distance and coordinates.
 */
final class TargetMarkers {
    private static final int TARGET = 0xFFFF4040;
    private static final int POINT = 0xFFFFD000;
    private static final int EDGE = 12;

    private TargetMarkers() {
    }

    static void tick() {
        Markers.tick();
    }

    static void draw(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }
        float partialTicks = delta.getGameTimeDeltaPartialTick(false);
        for (Map.Entry<Integer, Integer> spotted : Markers.spotted().entrySet()) {
            Entity entity = mc.level.getEntity(spotted.getKey());
            if (entity == null || entity == mc.player || !entity.isAlive()) {
                continue;
            }
            Vec3 top = entity.getPosition(partialTicks).add(0.0, entity.getBbHeight() + 0.5, 0.0);
            mark(g, mc, top, "▼", TARGET, "%.0fm".formatted(mc.player.distanceTo(entity)), null);
        }
        BlockPos point = Markers.point();
        if (point != null) {
            Vec3 at = Vec3.atCenterOf(point).add(0.0, 1.0, 0.0);
            String coordinates = "%d %d %d".formatted(point.getX(), point.getY(), point.getZ());
            mark(g, mc, at, "◆", POINT, "%.0fm".formatted(Math.sqrt(mc.player.distanceToSqr(at))), coordinates);
        }
    }

    private static void mark(GuiGraphics g, Minecraft mc, Vec3 at, String symbol, int color, String distance, @Nullable String extra) {
        Camera camera = mc.gameRenderer.getMainCamera();
        Vector3fc forward = camera.getLookVector();
        Vec3 offset = at.subtract(camera.getPosition());
        boolean ahead = offset.x * forward.x() + offset.y * forward.y() + offset.z * forward.z() > 0.0;
        // 26.x has projectPointToScreen for this; the view bobbing and the sprint widening are left out.
        Vector3f view = new Vector3f((float) offset.x, (float) offset.y, (float) offset.z).rotate(camera.rotation().conjugate(new Quaternionf()));
        Vector4f clip = mc.gameRenderer.getProjectionMatrix(ClientHooks.fov(mc.options.fov().get()))
                .transform(new Vector4f(view, 1.0F));
        Vec3 ndc = new Vec3(clip.x() / clip.w(), clip.y() / clip.w(), clip.z() / clip.w());
        int w = g.guiWidth();
        int h = g.guiHeight();
        double x = (ndc.x + 1.0) / 2.0 * w;
        double y = (1.0 - ndc.y) / 2.0 * h;
        Font font = mc.font;
        if (ahead && x >= 0 && x <= w && y >= 0 && y <= h) {
            g.drawCenteredString(font, symbol, (int) x, (int) y - 4, color);
            g.drawCenteredString(font, distance, (int) x, (int) y + 6, color);
            if (extra != null) {
                g.drawCenteredString(font, extra, (int) x, (int) y + 16, color);
            }
            return;
        }
        // Behind or off to the side: point the way along the screen edge.
        double dx = ahead ? x - w / 2.0 : w / 2.0 - x;
        double dy = ahead ? y - h / 2.0 : h / 2.0 - y;
        double scale = Math.max(Math.abs(dx) / (w / 2.0 - EDGE), Math.abs(dy) / (h / 2.0 - EDGE));
        if (scale <= 0.0) {
            return;
        }
        int ex = Mth.clamp((int) (w / 2.0 + dx / scale), EDGE, w - EDGE);
        int ey = Mth.clamp((int) (h / 2.0 + dy / scale), EDGE, h - EDGE);
        g.drawCenteredString(font, "◆", ex, ey - 4, color);
    }
}
