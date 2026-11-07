package com.jnhprojects.mcheli.entity;

import com.jnhprojects.mcheli.ModSounds;

/**
 * Open/closed state of a group of moving parts (canopy, landing gear, hatches, wings, VTOL nozzles).
 * Ported from {@code MCH_Parts}.
 *
 * <p>The target state is one bit of a synched integer, so only the server changes it and every
 * side sees the same value. The angle runs from 0 to {@link #max} at {@link #speed} degrees per tick
 * and is advanced locally on every side, which keeps the animation smooth without sending angles.
 */
public final class PartState {
    private final AircraftEntity owner;
    private final int bit;
    private final float max;
    private final float speed;
    private final Sounds sounds;

    private float angle;
    private float prevAngle;

    PartState(AircraftEntity owner, int bit, float speed, Sounds sounds) {
        this.owner = owner;
        this.bit = bit;
        this.max = 90.0F;
        this.speed = speed;
        this.sounds = sounds;
    }

    /** Target state: {@code true} means open, folded, retracted or tilted, depending on the part. */
    public boolean status() {
        return (owner.partBits() & (1 << bit)) != 0;
    }

    /** Fully back in the default position. */
    public boolean isOff() {
        return !status() && angle <= 0.02F;
    }

    /** Fully in the switched position. */
    public boolean isOn() {
        return status() && angle >= max - 0.02F;
    }

    public float angle() {
        return angle;
    }

    public float angle(float partialTicks) {
        return prevAngle + (angle - prevAngle) * partialTicks;
    }

    /** Progress from 0 to 1. */
    public float factor() {
        return angle / max;
    }

    public float factor(float partialTicks) {
        return angle(partialTicks) / max;
    }

    /** Changes the target state. Server only; clients follow through synched data. */
    void set(boolean on, boolean playSound) {
        if (owner.level().isClientSide() || status() == on) {
            return;
        }
        owner.setPartBit(bit, on);
        if (playSound) {
            play(sounds.switching);
            play(on ? sounds.startOn : sounds.startOff);
        }
        tick();
    }

    /** Jumps straight to {@code on} without animating, used for parts that start deployed. */
    void force(boolean on) {
        if (!owner.level().isClientSide()) {
            owner.setPartBit(bit, on);
        }
        angle = prevAngle = on ? max : 0.0F;
    }

    void tick() {
        prevAngle = angle;
        if (status()) {
            if (angle < max) {
                angle = Math.min(angle + speed, max);
                if (angle >= max) {
                    play(sounds.endOn);
                }
            }
        } else if (angle > 0.0F) {
            angle = Math.max(angle - speed, 0.0F);
            if (angle <= 0.0F) {
                play(sounds.endOff);
            }
        }
    }

    private void play(Cue cue) {
        if (cue != null && !owner.level().isClientSide()) {
            ModSounds.playAt(owner, cue.sound(), 1.0F, cue.pitch());
        }
    }

    /** One sound from {@code sounds.json} with its pitch. */
    record Cue(String sound, float pitch) {
    }

    /** What plays when the part starts and finishes moving each way; {@code null} entries stay silent. */
    record Sounds(Cue startOn, Cue endOn, Cue startOff, Cue endOff, Cue switching) {
        private static final Cue CLICK = new Cue("plane_cc", 1.0F);
        private static final Cue CLICK_LOW = new Cue("plane_cc", 0.5F);

        static final Sounds HATCH = new Sounds(null, CLICK, null, CLICK, new Cue("plane_cv", 0.5F));
        static final Sounds CANOPY = new Sounds(null, CLICK, null, CLICK, null);
        static final Sounds LANDING_GEAR = new Sounds(CLICK_LOW, CLICK_LOW, CLICK_LOW, CLICK_LOW, new Cue("plane_cv", 0.75F));
        static final Sounds NOZZLE = new Sounds(CLICK_LOW, CLICK_LOW, CLICK_LOW, CLICK_LOW, new Cue("plane_cv", 0.5F));
        static final Sounds WING = new Sounds(CLICK_LOW, CLICK_LOW, CLICK_LOW, CLICK_LOW, null);
    }
}
