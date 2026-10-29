package com.jnhprojects.mcheli;

import com.jnhprojects.mcheli.content.ContentRegistry;
import com.jnhprojects.mcheli.network.ContentReloadPacket;
import com.jnhprojects.mcheli.network.ModNetwork;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;

/**
 * Reads the vehicle and weapon definitions from disk again while the game runs, for
 * {@code /mcheli reload}. Vehicles in the world pick up their new definition on their next tick
 * (see {@code AircraftEntity#definition}); clients are told to refresh models and textures.
 *
 * <p>Until content packs sync definitions (phase 6), each side reads its own copy of the imported
 * folder: on a dedicated server, clients only see the change if their files changed the same way.
 */
public final class ContentReload {
    private ContentReload() {
    }

    /**
     * @return the newly loaded content, already in use
     * @throws IOException when the folder could not be read; the old content then stays in place
     */
    public static ContentRegistry reload(MinecraftServer server) throws IOException {
        ContentRegistry fresh = ContentRegistry.load(MCHeli.legacyContent().assets());
        MCHeli.content().replaceWith(fresh);
        MCHeli.LOGGER.info("Content reloaded: {} vehicles, {} weapons", fresh.all().size(), fresh.weapons().size());
        ModNetwork.sendToAll(server, new ContentReloadPacket());
        return fresh;
    }
}
