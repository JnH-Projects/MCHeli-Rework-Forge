package com.jnhprojects.mcheli.client.hud;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2fStack;

/**
 * The drawing primitives of {@code MCH_HudItem}, on the 26.x GUI.
 *
 * <p>The original drew GL lines, points and quads at fractional GUI coordinates. The GUI now only
 * takes whole numbers, but every element keeps a copy of the pose, so each primitive is a unit
 * square stretched and turned into place. Lines are one GUI unit thick, which is what
 * {@code glLineWidth(scaleFactor)} gave; blips are two units square.
 */
final class HudCanvas {
    private static final float LINE_WIDTH = 1.0F;
    private static final float POINT_SIZE = 2.0F;

    private final GuiGraphicsExtractor g;
    private final Font font;

    HudCanvas(GuiGraphicsExtractor g, Font font) {
        this.g = g;
        this.font = font;
    }

    Matrix3x2fStack pose() {
        return g.pose();
    }

    /** Turns everything drawn until the matching {@code pop} by {@code degrees} about a point. */
    void pushRotation(double x, double y, float degrees) {
        Matrix3x2fStack pose = g.pose();
        pose.pushMatrix();
        pose.translate((float) x, (float) y);
        pose.rotate((float) Math.toRadians(degrees));
        pose.translate((float) -x, (float) -y);
    }

    void pop() {
        g.pose().popMatrix();
    }

    void line(double x0, double y0, double x1, double y1, int color) {
        double dx = x1 - x0;
        double dy = y1 - y0;
        double length = Math.sqrt(dx * dx + dy * dy);
        if (length < 1.0E-4) {
            return;
        }
        Matrix3x2fStack pose = g.pose();
        pose.pushMatrix();
        pose.translate((float) x0, (float) y0);
        pose.rotate((float) Math.atan2(dy, dx));
        pose.translate(0.0F, -LINE_WIDTH / 2.0F);
        pose.scale((float) length, LINE_WIDTH);
        g.fill(0, 0, 1, 1, color);
        pose.popMatrix();
    }

    /** {@code GL_LINES}: x, y pairs taken two points at a time. */
    void lines(double[] p, int color) {
        for (int i = 0; i + 3 < p.length; i += 4) {
            line(p[i], p[i + 1], p[i + 2], p[i + 3], color);
        }
    }

    /** {@code GL_LINE_STRIP}. */
    void strip(double[] p, int color) {
        for (int i = 0; i + 3 < p.length; i += 2) {
            line(p[i], p[i + 1], p[i + 2], p[i + 3], color);
        }
    }

    /** {@code GL_LINE_LOOP}. */
    void loop(double[] p, int color) {
        strip(p, color);
        if (p.length >= 4) {
            line(p[p.length - 2], p[p.length - 1], p[0], p[1], color);
        }
    }

    /**
     * {@code GL_LINES} with {@code glLineStipple(factor, pattern)}: each segment starts the 16 bit
     * pattern afresh, lowest bit first, every bit covering {@code factor} GUI units.
     */
    void stippled(double[] p, int color, int factor, int pattern) {
        double step = Math.max(factor, 1);
        for (int i = 0; i + 3 < p.length; i += 4) {
            double dx = p[i + 2] - p[i];
            double dy = p[i + 3] - p[i + 1];
            double length = Math.sqrt(dx * dx + dy * dy);
            if (length < 1.0E-4) {
                continue;
            }
            double ux = dx / length;
            double uy = dy / length;
            double runStart = -1.0;
            int bit = 0;
            for (double t = 0.0; t < length; t += step, bit = (bit + 1) & 15) {
                boolean on = (pattern >> bit & 1) != 0;
                if (on && runStart < 0.0) {
                    runStart = t;
                } else if (!on && runStart >= 0.0) {
                    line(p[i] + ux * runStart, p[i + 1] + uy * runStart, p[i] + ux * t, p[i + 1] + uy * t, color);
                    runStart = -1.0;
                }
            }
            if (runStart >= 0.0) {
                line(p[i] + ux * runStart, p[i + 1] + uy * runStart, p[i + 2], p[i + 3], color);
            }
        }
    }

    void rect(double x0, double y0, double x1, double y1, int color) {
        double left = Math.min(x0, x1);
        double top = Math.min(y0, y1);
        double width = Math.abs(x1 - x0);
        double height = Math.abs(y1 - y0);
        if (width <= 0.0 || height <= 0.0) {
            return;
        }
        Matrix3x2fStack pose = g.pose();
        pose.pushMatrix();
        pose.translate((float) left, (float) top);
        pose.scale((float) width, (float) height);
        g.fill(0, 0, 1, 1, color);
        pose.popMatrix();
    }

    void point(double x, double y, int color) {
        rect(x - POINT_SIZE / 2.0, y - POINT_SIZE / 2.0, x + POINT_SIZE / 2.0, y + POINT_SIZE / 2.0, color);
    }

    /**
     * {@code drawTexture}: texels {@code u, v, uWidth, vHeight} of a {@code textureWidth} by
     * {@code textureHeight} image, drawn into a box and turned about its centre. Drawn untinted,
     * as the original reset the colour to white first.
     */
    void texture(Identifier texture, double left, double top, double width, double height, double u, double v,
                 double uWidth, double vHeight, float rotation, int textureWidth, int textureHeight) {
        if (width == 0.0 || height == 0.0) {
            return;
        }
        Matrix3x2fStack pose = g.pose();
        pose.pushMatrix();
        pose.translate((float) (left + width / 2.0), (float) (top + height / 2.0));
        pose.rotate((float) Math.toRadians(rotation));
        pose.translate((float) (-width / 2.0), (float) (-height / 2.0));
        pose.scale((float) width, (float) height);
        g.blit(texture, 0, 0, 1, 1,
                (float) (u / textureWidth), (float) ((u + uWidth) / textureWidth),
                (float) (v / textureHeight), (float) ((v + vHeight) / textureHeight));
        pose.popMatrix();
    }

    void text(String text, int x, int y, int color, boolean centered) {
        if (centered) {
            g.centeredText(font, text, x, y, color);
        } else {
            g.text(font, text, x, y, color);
        }
    }
}
