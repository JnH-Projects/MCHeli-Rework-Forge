package com.jnhprojects.mcheli.entity;

import com.jnhprojects.mcheli.network.PackDataPacket;
import com.jnhprojects.mcheli.network.PackListPacket;
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
    private static PackReceiver packs = new PackReceiver() {
        @Override
        public void onPackList(PackListPacket packet) {
        }

        @Override
        public void onPackData(PackDataPacket packet) {
        }
    };

    private ClientAccess() {
    }

    public static void install(Supplier<@Nullable Entity> player, PackReceiver receiver) {
        localPlayer = player;
        packs = receiver;
    }

    public static @Nullable Entity localPlayer() {
        return localPlayer.get();
    }

    /** Where the server's content pack packets go. Called on the client's main thread. */
    public static PackReceiver packs() {
        return packs;
    }

    /** The client half of the content pack sync. */
    public interface PackReceiver {
        void onPackList(PackListPacket packet);

        void onPackData(PackDataPacket packet);
    }
}
