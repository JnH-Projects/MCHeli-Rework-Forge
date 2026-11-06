package com.jnhprojects.mcheli.fabric;

import com.jnhprojects.mcheli.MCHeli;
import com.jnhprojects.mcheli.platform.Platform;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.nio.file.Path;
import java.util.function.BiFunction;

final class FabricPlatform implements Platform {
    @Override
    public String loaderName() {
        return "fabric";
    }

    @Override
    public Path gameDir() {
        return FabricLoader.getInstance().getGameDir();
    }

    @Override
    public Path configDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public String modVersion() {
        return FabricLoader.getInstance().getModContainer(MCHeli.MOD_ID)
                .map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("");
    }

    @Override
    public boolean isClient() {
        return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;
    }

    @Override
    public void sendToServer(CustomPacketPayload payload) {
        ClientPlayNetworking.send(payload);
    }

    @Override
    public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        ServerPlayNetworking.send(player, payload);
    }

    /** Fabric orders tabs by registration, which already puts ours one after another. */
    @Override
    public <T extends BlockEntity> BlockEntityType<T> blockEntityType(BiFunction<BlockPos, BlockState, T> factory, Block block) {
        return FabricBlockEntityTypeBuilder.create(factory::apply, block).build();
    }

    @Override
    public CreativeModeTab.Builder creativeTab(ResourceKey<CreativeModeTab> after) {
        return FabricItemGroup.builder();
    }
}
