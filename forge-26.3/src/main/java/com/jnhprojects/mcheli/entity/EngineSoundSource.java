package com.jnhprojects.mcheli.entity;

/**
 * A vehicle with an engine loop. The client sound player polls these values every tick; the
 * definition's {@code SoundVolume} and {@code SoundPitch} are applied on top.
 */
public interface EngineSoundSource {
    /** Loudness before the definition's multiplier. Zero silences the loop. */
    float engineVolume();

    /** Pitch before the definition's multiplier. */
    float enginePitch();
}
