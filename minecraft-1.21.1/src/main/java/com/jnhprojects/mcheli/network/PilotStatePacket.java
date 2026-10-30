package com.jnhprojects.mcheli.network;

import com.jnhprojects.mcheli.entity.AircraftEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * Sent by the pilot every tick. Position, yaw and pitch already travel through the vanilla
 * vehicle move packet; this carries the parts vanilla does not know about.
 *
 * @param keys the pilot's key state as packed by {@link AircraftEntity.ControlInput#write()}, so
 *             other players see wheels steer and tracks turn
 */
public record PilotStatePacket(int entityId, float throttle, float reverse, float roll, boolean hovering, byte keys) implements CustomPacketPayload {
    public static final Type<PilotStatePacket> TYPE = ModNetwork.type("pilot_state");
    public static final StreamCodec<RegistryFriendlyByteBuf, PilotStatePacket> STREAM_CODEC = StreamCodec.ofMember(PilotStatePacket::encode, PilotStatePacket::decode);

    public static void encode(PilotStatePacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.entityId);
        buf.writeFloat(msg.throttle);
        buf.writeFloat(msg.reverse);
        buf.writeFloat(msg.roll);
        buf.writeBoolean(msg.hovering);
        buf.writeByte(msg.keys);
    }

    public static PilotStatePacket decode(FriendlyByteBuf buf) {
        return new PilotStatePacket(buf.readVarInt(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readBoolean(), buf.readByte());
    }

    public static void handle(PilotStatePacket msg, ServerPlayer sender) {
        if (sender == null) {
            return;
        }
        // Only the player actually flying this aircraft may drive it.
        if (sender.getVehicle() instanceof AircraftEntity aircraft
                && aircraft.getId() == msg.entityId
                && aircraft.getControllingPassenger() == sender
                && Float.isFinite(msg.throttle)
                && Float.isFinite(msg.reverse)
                && Float.isFinite(msg.roll)) {
            aircraft.acceptPilotState(msg.throttle, msg.reverse, msg.roll, msg.hovering, msg.keys);
        }
    }

    @Override
    public Type<PilotStatePacket> type() {
        return TYPE;
    }
}
