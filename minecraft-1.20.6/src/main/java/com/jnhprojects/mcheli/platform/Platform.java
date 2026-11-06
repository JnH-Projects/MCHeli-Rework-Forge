package com.jnhprojects.mcheli.platform;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CreativeModeTab;

import java.nio.file.Path;
import java.util.Objects;

/**
 * What the shared code needs from the mod loader. Each loader module installs its implementation
 * first thing during mod construction, before anything else of ours runs.
 *
 * <p>Kept small on purpose: registration and events go the other way, with the loader module
 * calling into the shared code (see {@code Registration} and the {@code *Hooks} classes).
 */
public interface Platform {

    static Platform get() {
        Platform platform = Holder.instance;
        if (platform == null) {
            throw new IllegalStateException("No mod loader platform installed yet");
        }
        return platform;
    }

    static void install(Platform platform) {
        if (Holder.instance != null) {
            throw new IllegalStateException("Platform already installed: " + Holder.instance.loaderName());
        }
        Holder.instance = Objects.requireNonNull(platform);
    }

    /** {@code forge}, {@code neoforge} or {@code fabric}, for logs and {@code /mcheli status}. */
    String loaderName();

    /** The game directory, which holds the {@code mcheli} folder. */
    Path gameDir();

    /** The config directory, which holds the client config. */
    Path configDir();

    /** Our version as the loader read it from the mod metadata. */
    String modVersion();

    /** Whether this is a physical client, as opposed to a dedicated server. */
    boolean isClient();

    void sendToServer(CustomPacketPayload payload);

    void sendToPlayer(ServerPlayer player, CustomPacketPayload payload);

    /**
     * A builder for one of our creative tabs, placed after {@code after}. Vanilla's builder is not
     * public on every loader and only Forge and NeoForge can order tabs.
     */
    CreativeModeTab.Builder creativeTab(ResourceKey<CreativeModeTab> after);

    final class Holder {
        private static volatile Platform instance;

        private Holder() {
        }
    }
}
