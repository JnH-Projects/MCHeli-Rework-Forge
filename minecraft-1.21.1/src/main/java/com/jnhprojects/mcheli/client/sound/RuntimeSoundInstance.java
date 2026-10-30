package com.jnhprojects.mcheli.client.sound;

import com.jnhprojects.mcheli.client.RuntimeAssets;
import net.minecraft.Util;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.JOrbisAudioStream;
import net.minecraft.client.sounds.LoopingAudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.ConstantFloat;
import org.jspecify.annotations.Nullable;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * A sound that plays through the sound manager when a resource pack defines its event, and
 * otherwise streams the OGG file a runtime root holds for it ({@link RuntimeAssets#soundFile}).
 * Content added while the game runs can bring sounds this way without touching
 * {@code sounds.json} or reloading resources.
 */
public abstract class RuntimeSoundInstance extends AbstractTickableSoundInstance {
    private @Nullable Path file;

    protected RuntimeSoundInstance(SoundEvent event, SoundSource source, RandomSource random) {
        super(event, source, random);
    }

    @Override
    public @Nullable WeighedSoundEvents resolve(SoundManager manager) {
        WeighedSoundEvents events = manager.getSoundEvent(location);
        if (events != null) {
            sound = events.getSound(random);
            return events;
        }
        file = RuntimeAssets.soundFile(location.getPath());
        if (file == null) {
            // What vanilla does for an unknown event: play nothing.
            sound = SoundManager.EMPTY_SOUND;
            return null;
        }
        // Streamed, so the file is read as it plays and never cached as a buffer.
        sound = new Sound(location, ConstantFloat.of(1.0F), ConstantFloat.of(1.0F), 1, Sound.Type.FILE, true, false, 16);
        return new WeighedSoundEvents(location, null);
    }

    /**
     * Forge and NeoForge call this through their sound instance extension, which it overrides
     * without saying so: Fabric has no such method and calls {@link #openStream} from a mixin.
     */
    public CompletableFuture<AudioStream> getStream(SoundBufferLibrary buffers, Sound sound, boolean looping) {
        return openStream(buffers, sound.getPath(), looping);
    }

    /** Streams the file found on disk, or the resource {@code path} when there is none. */
    public CompletableFuture<AudioStream> openStream(SoundBufferLibrary buffers, ResourceLocation path, boolean looping) {
        Path source = file;
        if (source == null) {
            return buffers.getStream(path, looping);
        }
        return CompletableFuture.supplyAsync(() -> {
            try {
                InputStream in = new BufferedInputStream(Files.newInputStream(source));
                return looping ? new LoopingAudioStream(JOrbisAudioStream::new, in) : new JOrbisAudioStream(in);
            } catch (IOException e) {
                throw new CompletionException(e);
            }
        }, Util.nonCriticalIoPool());
    }
}
