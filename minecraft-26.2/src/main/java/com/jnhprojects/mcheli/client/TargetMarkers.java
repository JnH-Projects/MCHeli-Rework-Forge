package com.jnhprojects.mcheli.client;

import com.jnhprojects.mcheli.team.Markers;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;
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

    static void draw(GuiGraphicsExtractor g, DeltaTracker delta) {
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

    private static void mark(GuiGraphicsExtractor g, Minecraft mc, Vec3 at, String symbol, int color, String distance, @Nullable String extra) {
        Camera camera = mc.gameRenderer.mainCamera();
        Vector3fc forward = camera.forwardVector();
        Vec3 offset = at.subtract(camera.position());
        boolean ahead = offset.x * forward.x() + offset.y * forward.y() + offset.z * forward.z() > 0.0;
        Vec3 ndc = mc.gameRenderer.projectPointToScreen(at);
        int w = g.guiWidth();
        int h = g.guiHeight();
        double x = (ndc.x + 1.0) / 2.0 * w;
        double y = (1.0 - ndc.y) / 2.0 * h;
        Font font = mc.font;
        if (ahead && x >= 0 && x <= w && y >= 0 && y <= h) {
            g.centeredText(font, symbol, (int) x, (int) y - 4, color);
            g.centeredText(font, distance, (int) x, (int) y + 6, color);
            if (extra != null) {
                g.centeredText(font, extra, (int) x, (int) y + 16, color);
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
        g.centeredText(font, "◆", ex, ey - 4, color);
    }
}
