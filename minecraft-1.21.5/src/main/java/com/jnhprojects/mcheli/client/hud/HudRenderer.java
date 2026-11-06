package com.jnhprojects.mcheli.client.hud;

import com.jnhprojects.mcheli.MCHeli;
import com.jnhprojects.mcheli.content.aircraft.AircraftDefinition;
import com.jnhprojects.mcheli.entity.AircraftEntity;
import com.jnhprojects.mcheli.hud.Expression;
import com.jnhprojects.mcheli.hud.HudCommand;
import com.jnhprojects.mcheli.hud.HudScript;
import com.jnhprojects.mcheli.hud.HudVariable;
import com.jnhprojects.mcheli.hud.TextArg;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.IllegalFormatException;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Runs HUD scripts, the drawing half of {@code MCH_Hud} and the {@code MCH_HudItem} subclasses.
 *
 * <p>An {@code If} hides every line up to its {@code EndIf}; {@code Exit} ends the current script
 * only, so a {@code Call}er carries on; the colour carries into called scripts and over to the next
 * frame. A script cannot call itself, directly or through another, the same guard the original had.
 * A line that fails to format is skipped and reported once, where the original crashed.
 */
public final class HudRenderer {
    private static final Logger LOGGER = LoggerFactory.getLogger(HudRenderer.class);
    private static final int AZIMUTH_RED = 0xFFFF0000;
    private static final String[] AZIMUTH_8 = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};
    /** {@code glLineStipple} pattern of the original's dashed lines. */
    private static final int DASHES = 0xCCCC;

    private static final HudState STATE = new HudState();
    private static final Set<String> REPORTED = new HashSet<>();

    private final HudCanvas canvas;
    private final AircraftEntity vehicle;
    private final AircraftDefinition def;
    private final LocalPlayer player;
    private final double centerX;
    private final double centerY;
    private final Set<String> running = new HashSet<>();

    private HudRenderer(HudCanvas canvas, AircraftEntity vehicle, AircraftDefinition def, LocalPlayer player, double width, double height) {
        this.canvas = canvas;
        this.vehicle = vehicle;
        this.def = def;
        this.player = player;
        this.centerX = width / 2.0;
        this.centerY = height / 2.0;
    }

    public static void tick() {
        LocalPlayer player = Minecraft.getInstance().player;
        STATE.tick(player != null && player.getVehicle() instanceof AircraftEntity vehicle ? vehicle : null);
    }

    /** Fed by the pilot's mouse steering; moves the control stick indicator. */
    public static void onPilotLook(float dYaw, float dPitch) {
        STATE.onPilotLook(dYaw, dPitch);
    }

    /**
     * Draws a HUD script for the local player's seat.
     *
     * @return false if no script by that name is loaded, so the caller can draw something else
     */
    public static boolean draw(GuiGraphics g, AircraftEntity vehicle, AircraftDefinition def, LocalPlayer player,
                               int seat, String hud, float partialTicks) {
        HudScript script = HudScripts.INSTANCE.get(hud);
        if (script == null) {
            return false;
        }
        double width = g.guiWidth();
        double height = g.guiHeight();
        STATE.update(vehicle, def, player, seat, width, height, partialTicks);
        HudRenderer renderer = new HudRenderer(new HudCanvas(g, Minecraft.getInstance().font), vehicle, def, player, width, height);
        renderer.run(script);
        return true;
    }

    private double eval(Expression e) {
        return e.eval(STATE.vars);
    }

    private double x(Expression e) {
        return centerX + eval(e);
    }

    private double y(Expression e) {
        return centerY + eval(e);
    }

    private double[] points(List<Expression> coordinates) {
        double[] out = new double[coordinates.size()];
        for (int i = 0; i + 1 < out.length; i += 2) {
            out[i] = x(coordinates.get(i));
            out[i + 1] = y(coordinates.get(i + 1));
        }
        return out;
    }

    private void run(HudScript script) {
        if (!running.add(script.name())) {
            return;
        }
        try {
            boolean hidden = false;
            for (HudCommand command : script.commands()) {
                if (command instanceof HudCommand.If c) {
                    hidden = eval(c.condition()) == 0.0;
                    continue;
                }
                if (command instanceof HudCommand.EndIf) {
                    hidden = false;
                    continue;
                }
                if (hidden) {
                    continue;
                }
                if (command instanceof HudCommand.Exit) {
                    return;
                }
                execute(script, command);
            }
        } finally {
            running.remove(script.name());
        }
    }

    private void execute(HudScript script, HudCommand command) {
        int color = STATE.color;
        switch (command) {
            case HudCommand.Color c -> {
                STATE.color = (int) (long) eval(c.argb());
                STATE.vars.set(HudVariable.COLOR, STATE.color & 0xFFFFFFFFL);
            }
            case HudCommand.Call c -> {
                HudScript called = HudScripts.INSTANCE.get(c.hud());
                if (called != null) {
                    run(called);
                }
            }
            case HudCommand.Text c -> text(script, c, color);
            case HudCommand.Texture c -> {
                ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(MCHeli.MOD_ID, "textures/gui/" + c.name() + ".png");
                int[] size = HudScripts.INSTANCE.textureSize(Minecraft.getInstance().getResourceManager(), texture);
                canvas.texture(texture, x(c.left()), y(c.top()), eval(c.width()), eval(c.height()),
                        eval(c.u()), eval(c.v()), eval(c.uWidth()), eval(c.vHeight()), (float) eval(c.rotation()), size[0], size[1]);
            }
            case HudCommand.Rect c -> {
                // The original truncated the size, not the position.
                double left = x(c.left());
                double top = y(c.top());
                canvas.rect(left, top, left + (int) eval(c.width()), top + (int) eval(c.height()), color);
            }
            case HudCommand.Line c -> canvas.strip(points(c.points()), color);
            case HudCommand.LineStipple c -> canvas.stippled(points(c.points()), color, (int) eval(c.factor()), (int) eval(c.pattern()));
            case HudCommand.Radar c -> radar(c, color);
            case HudCommand.Graduation c -> graduation(c, color);
            case HudCommand.CameraRot c -> cameraRot(x(c.x()), y(c.y()), color);
            case HudCommand.If c -> {
            }
            case HudCommand.EndIf c -> {
            }
            case HudCommand.Exit c -> {
            }
        }
    }

    /** {@code MCH_HudItemString}: skipped while a weapon value is shown and no weapon (or magazine) applies. */
    private void text(HudScript script, HudCommand.Text c, int color) {
        Object[] args = new Object[c.args().size()];
        for (int i = 0; i < args.length; i++) {
            TextArg arg = c.args().get(i);
            if ((arg.needsWeapon() && !STATE.hasWeapon()) || (arg.needsMagazine() && !STATE.hasMagazine())) {
                return;
            }
            args[i] = STATE.text(arg, vehicle, def, player);
        }
        String text;
        try {
            text = String.format(Locale.ROOT, c.format(), args);
        } catch (IllegalFormatException e) {
            String key = script.name() + ':' + c.line();
            if (REPORTED.add(key)) {
                LOGGER.warn("HUD {}.txt line {}: cannot format \"{}\": {}", script.name(), c.line(), c.format(), e.getMessage());
            }
            return;
        }
        canvas.text(text, (int) x(c.x()), (int) y(c.y()), color, c.centered());
    }

    /** {@code MCH_HudItemRadar}: blips at half scale for a 64 unit box, turned with the view. */
    private void radar(HudCommand.Radar c, int color) {
        List<double[]> blips = c.enemies() ? STATE.enemyBlips : STATE.entityBlips;
        if (blips.isEmpty()) {
            return;
        }
        double rotation = Math.toRadians(eval(c.rotation()));
        double left = x(c.left());
        double top = y(c.top());
        double w = eval(c.width());
        double h = eval(c.height());
        double cos = Math.cos(rotation);
        double sin = Math.sin(rotation);
        for (double[] blip : blips) {
            double px = blip[0] / 2.0 * w / 64.0;
            double py = blip[1] / 2.0 * h / 64.0;
            double rx = px * cos - py * sin;
            double ry = px * sin + py * cos;
            if (rx > -w / 2.0 && rx < w / 2.0 && ry > -h / 2.0 && ry < h / 2.0) {
                canvas.point(rx + left + w / 2.0, ry + top + h / 2.0, color);
            }
        }
    }

    private void graduation(HudCommand.Graduation c, int color) {
        int x = (int) x(c.x());
        int y = (int) y(c.y());
        double angle = eval(c.angle());
        canvas.pushRotation(x, y, (float) eval(c.roll()));
        switch (c.kind()) {
            case YAW -> headingTape(angle, color, x, y);
            case PITCH_1 -> pitchScale(angle, color, x, y);
            case PITCH_2 -> pitchLadder(angle, color, x, y, true);
            case PITCH_3 -> pitchLadder(angle, color, x, y, false);
        }
        canvas.pop();
    }

    /** {@code drawCommonGraduationYaw}: a tick every degree, labels every three, compass points every 45. */
    private void headingTape(double playerYaw, int color, int posX, int posY) {
        double yaw = HudState.rotate360(playerYaw);
        posX -= 90;
        double[] line = new double[76];
        int offset = (int) (yaw * 10.0) % 10;
        int degree = (int) yaw - 9;
        for (int i = 0; i < line.length / 4; i++, degree++) {
            int azimuthX = posX + i * 10 - offset;
            line[i * 4] = azimuthX;
            line[i * 4 + 1] = posY;
            line[i * 4 + 2] = azimuthX;
            line[i * 4 + 3] = posY + (degree % 45 == 0 ? 15 : degree % 3 == 0 ? 10 : 5);
            if (degree % 45 == 0) {
                canvas.text(AZIMUTH_8[Math.floorMod(degree, 360) / 45], azimuthX, posY - 10, AZIMUTH_RED, true);
            } else if (degree % 3 == 0) {
                int heading = degree + 180;
                if (heading < 0) {
                    heading += 360;
                }
                if (heading > 360) {
                    heading -= 360;
                }
                canvas.text(Integer.toString(heading), azimuthX, posY - 10, color, true);
            }
        }
        canvas.lines(line, color);
    }

    /** {@code drawCommonGraduationPitch1}: a scale of ticks on both sides that slides with the pitch. */
    private void pitchScale(double playerPitch, int color, int posX, int posY) {
        int pitch = (int) playerPitch % 360;
        int y = (int) (playerPitch * 10.0 % 10.0);
        if (y < 0) {
            y += 10;
        }
        int left = posX - 100;
        int right = posX + 100;
        posY -= 80;
        double[] line = new double[144];
        int p = playerPitch < 0.0 && y != 0 ? pitch - 9 : pitch - 8;
        for (int i = 0; i < line.length / 8; i++, p++) {
            int outer = p % 3 == 0 ? 15 : 5;
            double rowY = posY + i * 10 - y;
            line[i * 8] = left - outer;
            line[i * 8 + 1] = rowY;
            line[i * 8 + 2] = left;
            line[i * 8 + 3] = rowY;
            line[i * 8 + 4] = right + outer;
            line[i * 8 + 5] = rowY;
            line[i * 8 + 6] = right;
            line[i * 8 + 7] = rowY;
        }
        canvas.lines(line, color);
        canvas.strip(new double[]{left - 25, posY - 90, left, posY - 90, left, posY + 90, left - 25, posY + 90}, color);
        canvas.strip(new double[]{right + 25, posY - 90, right, posY - 90, right, posY + 90, right + 25, posY + 90}, color);
    }

    /**
     * {@code drawCommonGraduationPitch2}: a ladder with a rung every five degrees, dashed below
     * the horizon. {@code wide} is {@code Pitch2} with five rungs; {@code Pitch3} has three, twice as far apart.
     */
    private void pitchLadder(double playerPitch, int color, int posX, int posY, boolean wide) {
        playerPitch = -playerPitch;
        int pitchStep = (int) playerPitch / 5 * 5;
        int start = wide ? 0 : 1;
        int end = wide ? 5 : 4;
        int spacing = wide ? 1 : 2;
        double rest = playerPitch % 5.0;
        double[] line = new double[8];
        for (int i = start; i < end; i++) {
            int pitch = -(-pitchStep - 10 + i * 5);
            int halfWidth = pitch != 0 ? 50 : 100;
            int y = posY + (int) (-60 * spacing + rest * 6.0 * spacing + i * 30 * spacing);
            line[0] = posX - halfWidth;
            line[1] = y + (pitch == 0 ? 0 : pitch > 0 ? 2 : -2);
            line[2] = posX - 50;
            line[3] = y;
            line[4] = posX + halfWidth;
            line[5] = line[1];
            line[6] = posX + 50;
            line[7] = y;
            canvas.lines(line, color);
            line[0] = posX - 50;
            line[1] = y;
            line[2] = posX - 30;
            line[3] = y;
            line[4] = posX + 50;
            line[5] = y;
            line[6] = posX + 30;
            line[7] = y;
            if (pitch >= 0) {
                canvas.lines(line, color);
            } else {
                canvas.stippled(line, color, 1, DASHES);
            }
            if (pitch != 0) {
                canvas.text(Integer.toString(pitch), posX - 60, y - 4, color, true);
                canvas.text(Integer.toString(pitch), posX + 60, y - 4, color, true);
            }
        }
    }

    /** {@code MCH_HudItemCameraRot}: a frame with a box showing where the crew's view points against the hull. */
    private void cameraRot(double posX, double posY, int color) {
        canvas.loop(new double[]{posX - 21, posY - 11, posX + 21, posY - 11, posX + 21, posY + 11, posX - 21, posY + 11}, color);
        canvas.stippled(new double[]{posX - 21, posY, posX, posY, posX + 21, posY, posX, posY,
                posX, posY - 11, posX, posY, posX, posY + 11, posX, posY}, color, 1, DASHES);
        float pitch = Mth.clamp(player.getXRot(), -30.0F, 70.0F);
        pitch = (float) ((pitch - 20.0F) * 0.16);
        float yaw = Mth.clamp(Mth.wrapDegrees(player.getYRot() - vehicle.getYRot()) * 2.0F, -50.0F, 50.0F);
        yaw = (float) (yaw * 0.34);
        canvas.loop(new double[]{posX + yaw - 3, posY + pitch - 2, posX + yaw + 3, posY + pitch - 2,
                posX + yaw + 3, posY + pitch + 2, posX + yaw - 3, posY + pitch + 2}, color);
    }
}
