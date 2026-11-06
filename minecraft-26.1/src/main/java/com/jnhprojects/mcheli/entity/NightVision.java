package com.jnhprojects.mcheli.entity;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * Night vision through a sight, {@code MCH_Camera.updateViewer}: a short effect kept topped up while
 * the viewer has it switched on, and taken away again when they switch it off or leave.
 */
public final class NightVision {
    private static final int TICKS = 300;

    private NightVision() {
    }

    /** Called every tick for each viewer. It never runs below ten seconds, where vanilla starts to flicker the view. */
    public static void topUp(Entity viewer) {
        if (viewer instanceof LivingEntity living) {
            MobEffectInstance current = living.getEffect(MobEffects.NIGHT_VISION);
            if (current == null || current.getDuration() < TICKS - 40) {
                living.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, TICKS, 0, true, false, false));
            }
        }
    }

    /** Removes ours, leaving a longer one from a potion alone. */
    public static void end(Entity viewer) {
        if (viewer instanceof LivingEntity living) {
            MobEffectInstance current = living.getEffect(MobEffects.NIGHT_VISION);
            if (current != null && current.getDuration() <= TICKS) {
                living.removeEffect(MobEffects.NIGHT_VISION);
            }
        }
    }
}
