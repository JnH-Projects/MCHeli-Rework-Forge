package com.jnhprojects.mcheli.client.sound;

import com.jnhprojects.mcheli.client.RuntimeAssets;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.JOrbisAudioStream;
import net.minecraft.client.sounds.LoopingAudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
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
        WeighedSoundEvents events = super.resolve(manager);
        if (events != null) {
            return events;
        }
        file = RuntimeAssets.soundFile(identifier.getPath());
        if (file == null) {
            return null;
        }
        // Streamed, so the file is read as it plays and never cached as a buffer.
        sound = new Sound(identifier, new ConstantFloat(1.0F), new ConstantFloat(1.0F), 1, Sound.Type.FILE, true, false, 16);
        return new WeighedSoundEvents(identifier, null);
    }

    /**
     * NeoForge calls this through its sound instance extension, which it overrides without saying
     * so. Fabric has no such method and calls {@link #openStream} from a mixin. Forge declares it
     * too but never calls it on this version, see {@code PackSoundBuffers} in its module.
     */
    public CompletableFuture<AudioStream> getStream(SoundBufferLibrary buffers, Sound sound, boolean looping) {
        return openStream(buffers, sound.getPath(), looping);
    }

    /** Streams the file found on disk, or the resource {@code path} when there is none. */
    public CompletableFuture<AudioStream> openStream(SoundBufferLibrary buffers, Identifier path, boolean looping) {
        Path source = file;
        if (source == null) {
            return buffers.getStream(path, looping);
        }
        return streamFile(source, looping);
    }

    /** Streams an OGG file from disk, read as it plays and never cached as a buffer. */
    public static CompletableFuture<AudioStream> streamFile(Path source, boolean looping) {
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
