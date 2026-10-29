package com.jnhprojects.mcheli.client;

import com.jnhprojects.mcheli.client.render.MeshCache;
import com.jnhprojects.mcheli.registry.ModCreativeTabs;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.CreativeModeTab;

/**
 * Makes the client show the content it now holds: after the server's pack list was applied, which
 * also happens after {@code /mcheli reload}.
 */
final class ClientContentReload {
    private ClientContentReload() {
    }

    /** Drops cached models and textures so edited and new files load, and rebuilds the creative tabs. */
    static void refresh() {
        Minecraft mc = Minecraft.getInstance();
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
