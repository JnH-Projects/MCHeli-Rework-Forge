package com.jnhprojects.mcheli.network;

import com.jnhprojects.mcheli.entity.AircraftEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/**
 * Client to server: this player is locking on to an entity. If it is a vehicle with flares, or a
 * crew member of one, its crew hears the warning. The original's {@code MCH_PacketNotifyLock}.
 */
public record LockWarningPacket(int targetId) implements CustomPacketPayload {
    public static final Type<LockWarningPacket> TYPE = ModNetwork.type("lock_warning");
    public static final StreamCodec<RegistryFriendlyByteBuf, LockWarningPacket> STREAM_CODEC = StreamCodec.ofMember(LockWarningPacket::encode, LockWarningPacket::decode);

    public static void encode(LockWarningPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.targetId);
    }

    public static LockWarningPacket decode(FriendlyByteBuf buf) {
        return new LockWarningPacket(buf.readVarInt());
    }

    public static void handle(LockWarningPacket msg, ServerPlayer sender) {
        if (sender == null || !(sender.getVehicle() instanceof AircraftEntity)) {
            return;
        }
        Entity target = sender.level().getEntity(msg.targetId);
        if (target != null && target.distanceToSqr(sender) < 400.0 * 400.0) {
            AircraftEntity vehicle = target instanceof AircraftEntity a ? a : target.getVehicle() instanceof AircraftEntity a ? a : null;
            if (vehicle != null) {
                vehicle.warnLocked();
            }
        }
    }

    @Override
    public Type<LockWarningPacket> type() {
        return TYPE;
    }
}
