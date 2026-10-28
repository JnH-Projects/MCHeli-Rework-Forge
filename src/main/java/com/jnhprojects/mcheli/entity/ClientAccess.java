package com.jnhprojects.mcheli.entity;

import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/**
 * The few client facts common code needs, such as who the local player is. The client installs
 * the real supplier at startup; a dedicated server keeps the empty default and never loads a
 * client class through here.
 */
public final class ClientAccess {
    private static Supplier<@Nullable Entity> localPlayer = () -> null;

    private ClientAccess() {
    }

    public static void install(Supplier<@Nullable Entity> player) {
        localPlayer = player;
    }

    public static @Nullable Entity localPlayer() {
        return localPlayer.get();
    }
}
