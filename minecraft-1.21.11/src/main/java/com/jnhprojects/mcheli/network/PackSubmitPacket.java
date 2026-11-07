package com.jnhprojects.mcheli.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * Player to server: one chunk of a pack the player made, offered to the server's operators. See
 * {@link PackSubmissions}, which treats all of this as untrusted.
 *
 * @param offset where this chunk goes; 0 starts a new submission
 * @param total  length of the whole zip
 */
public record PackSubmitPacket(int offset, int total, byte[] data) implements CustomPacketPayload {
    public static final Type<PackSubmitPacket> TYPE = ModNetwork.type("pack_submit");
    public static final StreamCodec<RegistryFriendlyByteBuf, PackSubmitPacket> STREAM_CODEC = StreamCodec.ofMember(PackSubmitPacket::encode, PackSubmitPacket::decode);

    /** Under the 32 KiB limit of a serverbound custom payload, which is far lower than the other way. */
    public static final int CHUNK_SIZE = 30 * 1024;

    public static void encode(PackSubmitPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.offset);
        buf.writeVarInt(msg.total);
        buf.writeByteArray(msg.data);
    }

    public static PackSubmitPacket decode(FriendlyByteBuf buf) {
        return new PackSubmitPacket(buf.readVarInt(), buf.readVarInt(), buf.readByteArray(CHUNK_SIZE));
    }

    public static void handle(PackSubmitPacket msg, ServerPlayer sender) {
        PackSubmissions.receive(sender, msg);
    }

    @Override
    public Type<PackSubmitPacket> type() {
        return TYPE;
    }
}
