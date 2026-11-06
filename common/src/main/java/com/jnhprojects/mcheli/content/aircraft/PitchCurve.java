package com.jnhprojects.mcheli.content.aircraft;

import java.util.Locale;

/**
 * How an engine or rotor loop's pitch follows the throttle, from {@code SoundPitchCurve} and
 * {@code RotorSoundPitchCurve}: points of {@code throttle:pitch} with straight lines between them.
 * <pre>
 * SoundPitchCurve = 0:0.6, 0.5:0.9, 1:1.4
 * </pre>
 * Throttle runs from 0 to 1. Pitch stays within {@value #MIN_PITCH} to {@value #MAX_PITCH}, all the
 * sound engine will play, so a curve never promises something that cannot be heard.
 */
public final class PitchCurve {
    public static final float MIN_PITCH = 0.5F;
    public static final float MAX_PITCH = 2.0F;

    private final float[] throttle;
    private final float[] pitch;

    private PitchCurve(float[] throttle, float[] pitch) {
        this.throttle = throttle;
        this.pitch = pitch;
    }

    /**
     * @throws IllegalArgumentException when the text is not a curve; the message is meant for the
     *                                  pack's author
     */
    public static PitchCurve parse(String text) {
        String[] points = text.split(",");
        if (points.length < 2) {
            throw new IllegalArgumentException("a pitch curve needs at least two throttle:pitch points");
        }
        float[] x = new float[points.length];
        float[] y = new float[points.length];
        for (int i = 0; i < points.length; i++) {
            String[] parts = points[i].split(":");
            if (parts.length != 2) {
                throw new IllegalArgumentException("\"" + points[i].trim() + "\" is not throttle:pitch");
            }
            x[i] = Float.parseFloat(parts[0].trim());
            y[i] = Math.clamp(Float.parseFloat(parts[1].trim()), MIN_PITCH, MAX_PITCH);
            if (x[i] < 0.0F || x[i] > 1.0F) {
                throw new IllegalArgumentException("throttle " + parts[0].trim() + " is outside 0 to 1");
            }
            if (i > 0 && x[i] <= x[i - 1]) {
                throw new IllegalArgumentException("the throttle values of a pitch curve must rise from left to right");
            }
        }
        return new PitchCurve(x, y);
    }

    /** The pitch at a throttle. Beyond its first and last points the curve stays level. */
    public float at(float value) {
        if (value <= throttle[0]) {
            return pitch[0];
        }
        for (int i = 1; i < throttle.length; i++) {
            if (value <= throttle[i]) {
                float f = (value - throttle[i - 1]) / (throttle[i] - throttle[i - 1]);
                return pitch[i - 1] + (pitch[i] - pitch[i - 1]) * f;
            }
        }
        return pitch[pitch.length - 1];
    }

    public int size() {
        return throttle.length;
    }

    public float throttle(int point) {
        return throttle[point];
    }

    public float pitch(int point) {
        return pitch[point];
    }

    /** The same curve with one point at another pitch. */
    public PitchCurve withPitch(int point, float value) {
        float[] y = pitch.clone();
        y[point] = Math.clamp(value, MIN_PITCH, MAX_PITCH);
        return new PitchCurve(throttle, y);
    }

    /** The curve as a definition value, which {@link #parse} reads back. */
    public String text() {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < throttle.length; i++) {
            out.append(i > 0 ? ", " : "").append(number(throttle[i])).append(':').append(number(pitch[i]));
        }
        return out.toString();
    }

    private static String number(float value) {
        String text = String.format(Locale.ROOT, "%.3f", value);
        text = text.replaceAll("0+$", "");
        return text.endsWith(".") ? text.substring(0, text.length() - 1) : text;
    }
}
