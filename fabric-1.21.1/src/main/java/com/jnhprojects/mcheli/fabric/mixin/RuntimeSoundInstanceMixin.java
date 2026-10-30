package com.jnhprojects.mcheli.fabric.mixin;

import com.jnhprojects.mcheli.client.sound.RuntimeSoundInstance;
import net.fabricmc.fabric.api.client.sound.v1.FabricSoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;

import java.util.concurrent.CompletableFuture;

/** Streams content pack sounds from disk through Fabric's sound instance hook. */
@Mixin(RuntimeSoundInstance.class)
abstract class RuntimeSoundInstanceMixin implements FabricSoundInstance {

    @Override
    public CompletableFuture<AudioStream> getAudioStream(SoundBufferLibrary loader, ResourceLocation id, boolean repeatInstantly) {
        return ((RuntimeSoundInstance) (Object) this).openStream(loader, id, repeatInstantly);
    }
}
