package com.jnhprojects.mcheli.forge;

import com.jnhprojects.mcheli.MCHeli;
import com.jnhprojects.mcheli.network.ModNetwork;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.Channel;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.payload.PayloadFlow;

/**
 * Registers the shared packets on a Forge payload channel. Each packet travels under its own id,
 * as on NeoForge and Fabric; the channel only adds Forge's version check.
 */
final class ForgeNetwork {
    private static Channel<CustomPacketPayload> channel;

    private ForgeNetwork() {
    }

    static void register() {
        PayloadFlow<RegistryFriendlyByteBuf, CustomPacketPayload> flow = ChannelBuilder
                .named(ResourceLocation.fromNamespaceAndPath(MCHeli.MOD_ID, "main"))
                .networkProtocolVersion(ModNetwork.PROTOCOL_VERSION)
                .payloadChannel()
                .play()
                .serverbound();
        for (ModNetwork.Serverbound<?> packet : ModNetwork.SERVERBOUND) {
            flow = serverbound(flow, packet);
        }
        flow = flow.clientbound();
        for (ModNetwork.Clientbound<?> packet : ModNetwork.CLIENTBOUND) {
            flow = clientbound(flow, packet);
        }
        channel = flow.build();
    }

    static Channel<CustomPacketPayload> channel() {
        return channel;
    }

    private static <T extends CustomPacketPayload> PayloadFlow<RegistryFriendlyByteBuf, CustomPacketPayload> serverbound(
            PayloadFlow<RegistryFriendlyByteBuf, CustomPacketPayload> flow, ModNetwork.Serverbound<T> packet) {
        return flow.add(packet.type(), packet.codec(), (msg, ctx) -> {
            ctx.enqueueWork(() -> packet.handler().accept(msg, ctx.getSender()));
            ctx.setPacketHandled(true);
        });
    }

    private static <T extends CustomPacketPayload> PayloadFlow<RegistryFriendlyByteBuf, CustomPacketPayload> clientbound(
            PayloadFlow<RegistryFriendlyByteBuf, CustomPacketPayload> flow, ModNetwork.Clientbound<T> packet) {
        return flow.add(packet.type(), packet.codec(), (msg, ctx) -> {
            ctx.enqueueWork(() -> packet.handler().accept(msg));
            ctx.setPacketHandled(true);
        });
    }
}
