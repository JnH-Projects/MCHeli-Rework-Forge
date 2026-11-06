package com.jnhprojects.mcheli;

import com.jnhprojects.mcheli.content.ContentRegistry;
import com.jnhprojects.mcheli.network.PackSync;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

/**
 * Reads the vehicle and weapon definitions from disk again while the game runs, for
 * {@code /mcheli reload}: the server config, then the imported content and the content packs.
 * Vehicles in the world pick up their new definition on their next tick (see
 * {@code AircraftEntity#definition}). Every player gets the new pack list, downloads what changed
 * and rebuilds their content the same way.
 */
public final class ContentReload {
    private ContentReload() {
    }

    /** @return the newly loaded content, already in use */
    public static ContentRegistry reload(MinecraftServer server) {
        ServerConfig.load(server.getWorldPath(LevelResource.ROOT));
        ContentRegistry fresh = MCHeli.packs().load(true);
        MCHeli.content().replaceWith(fresh);
        MCHeli.LOGGER.info("Content reloaded: {} vehicles, {} weapons", fresh.all().size(), fresh.weapons().size());
        PackSync.sendListToAll(server);
        return fresh;
    }
}
