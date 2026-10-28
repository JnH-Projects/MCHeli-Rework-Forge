package com.jnhprojects.mcheli.hud;

import java.util.List;

/**
 * One line of a HUD script, the {@code MCH_HudItem} subclasses. Positions are relative to the
 * centre of the screen in GUI units; every number is an {@link Expression}.
 */
public sealed interface HudCommand {
    /** Line in the file, for error messages. */
    int line();

    /** {@code If}: the commands up to the next {@code EndIf} run only while this is not zero. */
    record If(int line, Expression condition) implements HudCommand {
    }

    record EndIf(int line) implements HudCommand {
    }

    /** {@code Exit}: stops the current script; a caller carries on after its {@code Call}. */
    record Exit(int line) implements HudCommand {
    }

    /** {@code Color}: the ARGB colour of everything drawn after it, carried into called scripts. */
    record Color(int line, Expression argb) implements HudCommand {
    }

    /** {@code Call}: runs another script by name. */
    record Call(int line, String hud) implements HudCommand {
    }

    /** {@code DrawString} and {@code DrawCenteredString}: a {@link String#format} line. */
    record Text(int line, Expression x, Expression y, String format, List<TextArg> args, boolean centered) implements HudCommand {
    }

    /**
     * {@code DrawTexture}: part of {@code textures/gui/<name>.png}, in texels of the image, drawn
     * into a box and turned by {@code rotation} degrees about the box's centre.
     */
    record Texture(int line, String name, Expression left, Expression top, Expression width, Expression height,
                   Expression u, Expression v, Expression uWidth, Expression vHeight, Expression rotation) implements HudCommand {
    }

    record Rect(int line, Expression left, Expression top, Expression width, Expression height) implements HudCommand {
    }

    /** {@code DrawLine}: a strip through every point, given as x, y pairs. */
    record Line(int line, List<Expression> points) implements HudCommand {
    }

    /**
     * {@code DrawLineStipple}: separate segments between point pairs, dashed by a 16 bit
     * {@code glLineStipple} pattern stretched by {@code factor}.
     */
    record LineStipple(int line, Expression pattern, Expression factor, List<Expression> points) implements HudCommand {
    }

    /** {@code DrawEntityRadar} and {@code DrawEnemyRadar}: blips of living things within 64 blocks. */
    record Radar(int line, boolean enemies, Expression rotation, Expression left, Expression top,
                 Expression width, Expression height) implements HudCommand {
    }

    /** {@code DrawGraduationYaw} and {@code DrawGraduationPitch1..3}: heading tape and pitch ladders. */
    record Graduation(int line, Kind kind, Expression angle, Expression roll, Expression x, Expression y) implements HudCommand {
        public enum Kind { YAW, PITCH_1, PITCH_2, PITCH_3 }
    }

    /** {@code DrawCameraRot}: the gunner camera's aim relative to the hull. */
    record CameraRot(int line, Expression x, Expression y) implements HudCommand {
    }
}
