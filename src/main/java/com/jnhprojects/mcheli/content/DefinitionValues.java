package com.jnhprojects.mcheli.content;

import java.util.Locale;

/**
 * Value conversions shared by every definition type.
 *
 * <p>These mirror the helpers in the original {@code MCH_BaseInfo} so that existing content files
 * keep their exact meaning, including the silent clamping of out of range numbers.
 */
public final class DefinitionValues {
    private DefinitionValues() {
    }

    public static String[] split(String data) {
        return data.split("\s*,\s*");
    }

    public static String[] splitSlash(String data) {
        return data.split("\s*/\s*");
    }

    public static boolean toBool(String s) {
        return s.trim().equalsIgnoreCase("true");
    }

    public static boolean toBool(String s, boolean fallback) {
        String t = s.trim();
        if (t.equalsIgnoreCase("true")) {
            return true;
        }
        return !t.equalsIgnoreCase("false") && fallback;
    }

    public static float toFloat(String s) {
        return Float.parseFloat(s.trim());
    }

    public static float toFloat(String s, float min, float max) {
        return Math.clamp(toFloat(s), min, max);
    }

    public static double toDouble(String s) {
        return Double.parseDouble(s.trim());
    }

    public static int toInt(String s) {
        return Integer.parseInt(s.trim());
    }

    public static int toInt(String s, int min, int max) {
        return Math.clamp(toInt(s), min, max);
    }

    public static Vector3 toVec(String x, String y, String z) {
        return new Vector3(toDouble(x), toDouble(y), toDouble(z));
    }

    /** Accepts {@code 0xRRGGBB}, {@code #RRGGBB} or a bare hex string, like the original. */
    public static int hex(String s) {
        String t = s.trim();
        if (t.startsWith("0x") || t.startsWith("0X") || t.startsWith("#")) {
            return (int) (Long.decode(t) & 0xFFFFFFFFL);
        }
        return (int) (Long.decode("0x" + t) & 0xFFFFFFFFL);
    }

    public static String lower(String s) {
        return s.trim().toLowerCase(Locale.ROOT);
    }

    /** Equivalent of {@code MathHelper.wrapAngleTo180_float}. */
    public static float wrapDegrees(float degrees) {
        float d = degrees % 360.0F;
        if (d >= 180.0F) {
            d -= 360.0F;
        }
        if (d < -180.0F) {
            d += 360.0F;
        }
        return d;
    }
}
