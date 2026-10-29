package com.jnhprojects.mcheli.client;

import com.jnhprojects.mcheli.MCHeli;
import com.jnhprojects.mcheli.client.render.MeshCache;
import com.jnhprojects.mcheli.content.ContentRegistry;
import com.jnhprojects.mcheli.registry.ModCreativeTabs;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;

import java.io.IOException;

/** The client half of {@code /mcheli reload}. */
final class ClientContentReload {
    private ClientContentReload() {
    }

    static void run() {
        Minecraft mc = Minecraft.getInstance();
        // In single player the integrated server already swapped the shared registry.
        if (mc.getSingleplayerServer() == null) {
            try {
                MCHeli.content().replaceWith(ContentRegistry.load(MCHeli.legacyContent().assets()));
            } catch (IOException e) {
                MCHeli.LOGGER.error("Could not reload the MCHeli content", e);
                if (mc.player != null) {
                    mc.player.sendSystemMessage(Component.translatable("commands.mcheli.reload.failed", e.getMessage()));
                }
            }
        }

        MeshCache.INSTANCE.clear();
        RuntimeAssets.refreshTextures();
        if (mc.level != null && mc.player != null) {
            boolean operator = mc.options.operatorItemsTab().get() && mc.player.canUseGameMasterBlocks();
            CreativeModeTab.ItemDisplayParameters params =
                    new CreativeModeTab.ItemDisplayParameters(mc.level.enabledFeatures(), operator, mc.level.registryAccess());
            ModCreativeTabs.all().forEach(tab -> tab.buildContents(params));
        }
    }
}
