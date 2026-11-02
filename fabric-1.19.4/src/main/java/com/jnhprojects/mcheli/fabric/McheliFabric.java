package com.jnhprojects.mcheli.fabric;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Placeholder entry point for Minecraft 1.19.4. It only proves that the module builds and loads;
 * the Fabric glue from fabric-26.3 replaces it when this version is ported for a final release.
 */
public final class McheliFabric implements ModInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger("MCHeli");

    @Override
    public void onInitialize() {
        LOGGER.info("MCHeli Rework placeholder loaded on Fabric for Minecraft 1.19.4; no content yet");
    }
}
