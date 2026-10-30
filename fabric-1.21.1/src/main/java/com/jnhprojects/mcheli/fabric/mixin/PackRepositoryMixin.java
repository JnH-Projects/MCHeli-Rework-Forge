package com.jnhprojects.mcheli.fabric.mixin;

import com.jnhprojects.mcheli.CommonHooks;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.RepositorySource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.Arrays;

/**
 * Adds the imported original content to every pack repository, client resources and server data
 * alike, which is what Forge and NeoForge do through their pack finder event.
 */
@Mixin(PackRepository.class)
abstract class PackRepositoryMixin {

    @ModifyVariable(method = "<init>", at = @At("HEAD"), argsOnly = true)
    private static RepositorySource[] mcheli$addImportedContent(RepositorySource[] sources) {
        RepositorySource[] withOurs = Arrays.copyOf(sources, sources.length + 1);
        withOurs[sources.length] = CommonHooks.packSource();
        return withOurs;
    }
}
