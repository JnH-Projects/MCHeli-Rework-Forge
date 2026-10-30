package com.jnhprojects.mcheli;

import com.jnhprojects.mcheli.content.ContentRegistry;
import com.jnhprojects.mcheli.platform.Platform;
import com.jnhprojects.mcheli.registry.ModCreativeTabs;
import com.jnhprojects.mcheli.registry.ModDataComponents;
import com.jnhprojects.mcheli.registry.ModEntities;
import com.jnhprojects.mcheli.registry.ModItems;
import com.jnhprojects.mcheli.registry.ModParticles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The mod, minus the loader. Each loader module's entry point installs its {@link Platform}, calls
 * {@link #init()}, registers what {@code Registration} collected and wires its events to
 * {@link CommonHooks} (and, on a client, {@code ClientHooks}).
 */
public final class MCHeli {
    public static final String MOD_ID = "mcheli";
    public static final Logger LOGGER = LoggerFactory.getLogger(MCHeli.class);

    private static LegacyContent legacyContent;
    private static ContentPacks packs;
    private static ContentRegistry content;

    private MCHeli() {
    }

    /**
     * Imports the original content if needed, loads the definitions and declares the registry
     * entries. The vehicle list must be known here: 1.0.0 had an item per vehicle, and those items
     * are still registered so old worlds keep them.
     */
    public static void init() {
        Platform platform = Platform.get();
        LOGGER.info("MCHeli Rework {} on {}", platform.modVersion(), platform.loaderName());
        legacyContent = LegacyContent.prepare(platform.gameDir());
        packs = new ContentPacks(platform.gameDir(), legacyContent);
        content = packs.load(false);
        ModDataComponents.init();
        ModEntities.init();
        ModParticles.init();
        ModItems.init(content);
        ModCreativeTabs.init();
    }

    /**
     * Vehicle and weapon definitions, from the imported original content and the content packs.
     * Available from mod construction onwards. Always the same object; a reload changes what it
     * holds.
     */
    public static ContentRegistry content() {
        return content;
    }

    /** Where the original vehicle content came from, and why it is missing if it is. */
    public static LegacyContent legacyContent() {
        return legacyContent;
    }

    /** The content packs in {@code mcheli/packs} and the ones in use. */
    public static ContentPacks packs() {
        return packs;
    }
}
