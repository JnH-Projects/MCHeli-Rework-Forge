package com.jnhprojects.mcheli.network;

import com.jnhprojects.mcheli.entity.ClientAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

/**
 * Server to player: one chunk of a pack's zip. Chunks of a pack arrive in order, so
 * {@code offset} only lets the client check that nothing went missing.
 *
 * @param total length of the whole zip, as in the {@link PackListPacket}
 */
public record PackDataPacket(String hash, int offset, int total, byte[] data) {
    /** Well under the 1 MiB limit of a clientbound custom payload. */
    public static final int CHUNK_SIZE = 128 * 1024;

    public static void encode(PackDataPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.hash, 64);
        buf.writeVarInt(msg.offset);
        buf.writeVarInt(msg.total);
        buf.writeByteArray(msg.data);
    }

    public static PackDataPacket decode(FriendlyByteBuf buf) {
        return new PackDataPacket(buf.readUtf(64), buf.readVarInt(), buf.readVarInt(), buf.readByteArray(CHUNK_SIZE));
    }

    public static void handle(PackDataPacket msg, CustomPayloadEvent.Context ctx) {
        ClientAccess.packs().onPackData(msg);
    }
}
