package com.jnhprojects.mcheli;

import com.jnhprojects.mcheli.client.ClientSetup;
import com.jnhprojects.mcheli.content.ContentRegistry;
import com.jnhprojects.mcheli.network.ModNetwork;
import com.jnhprojects.mcheli.network.PackSync;
import com.jnhprojects.mcheli.registry.ModCreativeTabs;
import com.jnhprojects.mcheli.registry.ModDataComponents;
import com.jnhprojects.mcheli.registry.ModEntities;
import com.jnhprojects.mcheli.registry.ModItems;
import com.jnhprojects.mcheli.registry.ModParticles;
import com.jnhprojects.mcheli.registry.ModRecipes;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(MCHeli.MOD_ID)
public final class MCHeli {
    public static final String MOD_ID = "mcheli";
    public static final Logger LOGGER = LoggerFactory.getLogger(MCHeli.class);

    private static LegacyContent legacyContent;
    private static ContentPacks packs;
    private static ContentRegistry content;

    public MCHeli(FMLJavaModLoadingContext context) {
        BusGroup modBus = context.getModBusGroup();

        legacyContent = LegacyContent.prepare(FMLPaths.GAMEDIR.get());
        legacyContent.registerPacks();
        packs = new ContentPacks(FMLPaths.GAMEDIR.get(), legacyContent);
        content = packs.load(false);
        // The server config (allow list, size limits) exists from here on, before any world loads.
        ServerAboutToStartEvent.BUS.addListener(event -> content.replaceWith(packs.load(true)));
        context.registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC);
        context.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
        ModDataComponents.register(modBus);
        ModEntities.register(modBus);
        ModParticles.register(modBus);
        ModItems.register(modBus, content);
        ModCreativeTabs.register(modBus);
        ModRecipes.register(modBus);
        ModNetwork.register();
        PackSync.init();
        McheliCommand.register();

        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientSetup.init(modBus);
        }
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
