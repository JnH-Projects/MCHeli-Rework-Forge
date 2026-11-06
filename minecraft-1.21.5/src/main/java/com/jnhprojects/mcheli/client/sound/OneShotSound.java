package com.jnhprojects.mcheli.client.sound;

import com.jnhprojects.mcheli.ModSounds;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/** A sound from a content pack played once at a point, such as a gun firing. */
public final class OneShotSound extends RuntimeSoundInstance {
    public OneShotSound(String sound, double x, double y, double z, float volume, float pitch) {
        super(ModSounds.get(sound), SoundSource.NEUTRAL, RandomSource.create());
        this.x = x;
        this.y = y;
        this.z = z;
        this.volume = volume;
        this.pitch = pitch;
    }

    @Override
    public void tick() {
        // It stays where it was started and ends when the file does.
    }
}
