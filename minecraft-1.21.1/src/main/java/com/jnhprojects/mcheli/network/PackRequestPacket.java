package com.jnhprojects.mcheli.network;

import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Player to server: send me these packs, by content hash. The server only answers for hashes in
 * the list it sent, so a client cannot ask for anything else.
 */
public record PackRequestPacket(List<String> hashes) implements CustomPacketPayload {
    public static final Type<PackRequestPacket> TYPE = ModNetwork.type("pack_request");
    public static final StreamCodec<RegistryFriendlyByteBuf, PackRequestPacket> STREAM_CODEC = StreamCodec.ofMember(PackRequestPacket::encode, PackRequestPacket::decode);

    public static void encode(PackRequestPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.hashes.size());
        for (String hash : msg.hashes) {
            buf.writeUtf(hash, 64);
        }
    }

    public static PackRequestPacket decode(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        if (count < 0 || count > PackListPacket.MAX_PACKS) {
            throw new DecoderException("Too many content packs requested: " + count);
        }
        List<String> hashes = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            hashes.add(buf.readUtf(64));
        }
        return new PackRequestPacket(List.copyOf(hashes));
    }

    public static void handle(PackRequestPacket msg, ServerPlayer sender) {
        if (sender != null) {
            PackSync.request(sender, msg.hashes);
        }
    }

    @Override
    public Type<PackRequestPacket> type() {
        return TYPE;
    }
}
