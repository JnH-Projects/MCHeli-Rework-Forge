package com.jnhprojects.mcheli.network;

import com.jnhprojects.mcheli.team.Markers;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server to client: the team spotted these entities for {@code ticks}, or marked {@code point},
 * {@code MCH_PacketNotifySpotedEntity} and {@code MCH_PacketNotifyMarkPoint}. Only the spotter and
 * their teammates get it.
 */
public record MarkerPacket(int[] entities, int ticks, BlockPos point) implements CustomPacketPayload {
    public static final Type<MarkerPacket> TYPE = ModNetwork.type("marker");
    public static final StreamCodec<RegistryFriendlyByteBuf, MarkerPacket> STREAM_CODEC = StreamCodec.ofMember(MarkerPacket::encode, MarkerPacket::decode);
    /** Spotting sends no point; this stands in for none. */
    private static final BlockPos NO_POINT = new BlockPos(0, Integer.MIN_VALUE, 0);
    private static final int MAX_ENTITIES = 256;

    public static MarkerPacket spotted(int[] entities, int ticks) {
        return new MarkerPacket(entities, ticks, NO_POINT);
    }

    public static MarkerPacket point(BlockPos point) {
        return new MarkerPacket(new int[0], 0, point);
    }

    public static void encode(MarkerPacket msg, FriendlyByteBuf buf) {
        buf.writeVarIntArray(msg.entities);
        buf.writeVarInt(msg.ticks);
        buf.writeBlockPos(msg.point);
    }

    public static MarkerPacket decode(FriendlyByteBuf buf) {
        return new MarkerPacket(buf.readVarIntArray(MAX_ENTITIES), buf.readVarInt(), buf.readBlockPos());
    }

    public static void handle(MarkerPacket msg) {
        if (msg.entities.length > 0) {
            Markers.spotted(msg.entities, msg.ticks);
        }
        if (!msg.point.equals(NO_POINT)) {
            Markers.point(msg.point);
        }
    }

    @Override
    public Type<MarkerPacket> type() {
        return TYPE;
    }
}
