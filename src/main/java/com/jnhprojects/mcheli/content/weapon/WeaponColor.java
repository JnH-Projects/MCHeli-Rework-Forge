package com.jnhprojects.mcheli.content.weapon;

/** A colour from {@code BulletColor} and friends, components clamped to [0, 1] like {@code MCH_Color}. */
public record WeaponColor(float a, float r, float g, float b) {
    public static final WeaponColor WHITE = new WeaponColor(1.0F, 1.0F, 1.0F, 1.0F);

    public WeaponColor {
        a = clamp(a);
        r = clamp(r);
        g = clamp(g);
        b = clamp(b);
    }

    public int argb() {
        return (int) (a * 255.0F + 0.5F) << 24 | (int) (r * 255.0F + 0.5F) << 16 | (int) (g * 255.0F + 0.5F) << 8 | (int) (b * 255.0F + 0.5F);
    }

    private static float clamp(float f) {
        return f < 0.0F ? 0.0F : Math.min(f, 1.0F);
    }
}
