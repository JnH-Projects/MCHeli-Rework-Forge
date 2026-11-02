package com.jnhprojects.mcheli.forge;

import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Placeholder entry point for Minecraft 1.20.2. It only proves that the module builds and loads;
 * the Forge glue from forge-26.3 replaces it when this version is ported for a final release.
 */
@Mod("mcheli")
public final class McheliForge {

    private static final Logger LOGGER = LoggerFactory.getLogger("MCHeli");

    public McheliForge() {
        LOGGER.info("MCHeli Rework placeholder loaded on Forge for Minecraft 1.20.2; no content yet");
    }
}
