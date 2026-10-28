package com.jnhprojects.mcheli.hud;

/** A HUD script or expression that cannot be parsed. */
public class HudFormatException extends RuntimeException {
    public HudFormatException(String message) {
        super(message);
    }

    public HudFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}
