package com.jnhprojects.mcheli.forge;

import com.jnhprojects.mcheli.MCHeli;
import com.jnhprojects.mcheli.client.RuntimeAssets;
import com.jnhprojects.mcheli.client.sound.RuntimeSoundInstance;
import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * Streams sounds that only exist in a runtime root. Forge still declares
 * {@code SoundInstance.getStream} on this Minecraft version, but its sound engine goes straight to
 * the buffer library and never calls it, so {@code RuntimeSoundInstance} has no say here and a pack
 * sound would fail to open, with nothing in the log. The field holding the library is opened by an
 * access transformer.
 */
final class PackSoundBuffers extends SoundBufferLibrary {
    private static final String FOLDER = "sounds/";
    private static final String EXTENSION = ".ogg";

    private final ResourceProvider resources;

    private PackSoundBuffers(ResourceProvider resources) {
        super(resources);
        this.resources = resources;
    }

    /** Fires on every engine (re)load, so only the first one after construction replaces anything. */
    static void install(SoundEngine engine) {
        if (!(engine.soundBuffers instanceof PackSoundBuffers)) {
            engine.soundBuffers = new PackSoundBuffers(Minecraft.getInstance().getResourceManager());
        }
    }

    @Override
    public CompletableFuture<AudioStream> getStream(Identifier location, boolean looping) {
        Path file = runtimeFile(location);
        return file != null ? RuntimeSoundInstance.streamFile(file, looping) : super.getStream(location, looping);
    }

    /** The disk file for {@code mcheli:sounds/<event>.ogg}, unless a resource pack has that sound. */
    private @Nullable Path runtimeFile(Identifier location) {
        String path = location.getPath();
        if (!location.getNamespace().equals(MCHeli.MOD_ID) || !path.startsWith(FOLDER) || !path.endsWith(EXTENSION)
                || resources.getResource(location).isPresent()) {
            return null;
        }
        return RuntimeAssets.soundFile(path.substring(FOLDER.length(), path.length() - EXTENSION.length()));
    }
}
