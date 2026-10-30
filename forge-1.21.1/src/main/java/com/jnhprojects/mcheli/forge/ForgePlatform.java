package com.jnhprojects.mcheli.forge;

import com.jnhprojects.mcheli.MCHeli;
import com.jnhprojects.mcheli.platform.Platform;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.PacketDistributor;

import java.nio.file.Path;

final class ForgePlatform implements Platform {
    @Override
    public String loaderName() {
        return "forge";
    }

    @Override
    public Path gameDir() {
        return FMLPaths.GAMEDIR.get();
    }

    @Override
    public Path configDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public String modVersion() {
        return ModList.get().getModContainerById(MCHeli.MOD_ID)
                .map(c -> c.getModInfo().getVersion().toString()).orElse("");
    }

    @Override
    public boolean isClient() {
        return FMLEnvironment.dist == Dist.CLIENT;
    }

    @Override
    public void sendToServer(CustomPacketPayload payload) {
        ForgeNetwork.channel().send(payload, PacketDistributor.SERVER.noArg());
    }

    @Override
    public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        ForgeNetwork.channel().send(payload, PacketDistributor.PLAYER.with(player));
    }

    @Override
    public CreativeModeTab.Builder creativeTab(ResourceKey<CreativeModeTab> after) {
        return CreativeModeTab.builder().withTabsBefore(after);
    }
}
