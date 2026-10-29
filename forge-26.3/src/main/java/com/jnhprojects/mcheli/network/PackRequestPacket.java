package com.jnhprojects.mcheli.network;

import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Player to server: send me these packs, by content hash. The server only answers for hashes in
 * the list it sent, so a client cannot ask for anything else.
 */
public record PackRequestPacket(List<String> hashes) {

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

    public static void handle(PackRequestPacket msg, CustomPayloadEvent.Context ctx) {
        ServerPlayer sender = ctx.getSender();
        if (sender != null) {
            PackSync.request(sender, msg.hashes);
        }
    }
}
