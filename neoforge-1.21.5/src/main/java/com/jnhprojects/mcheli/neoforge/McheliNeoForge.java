package com.jnhprojects.mcheli.neoforge;

import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Placeholder entry point for Minecraft 1.21.5. It only proves that the module builds and loads;
 * the NeoForge glue from neoforge-26.3 replaces it when this version is ported for a final release.
 */
@Mod("mcheli")
public final class McheliNeoForge {

    private static final Logger LOGGER = LoggerFactory.getLogger("MCHeli");

    public McheliNeoForge() {
        LOGGER.info("MCHeli Rework placeholder loaded on NeoForge for Minecraft 1.21.5; no content yet");
    }
}
