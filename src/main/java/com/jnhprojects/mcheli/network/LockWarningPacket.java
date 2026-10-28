package com.jnhprojects.mcheli.network;

import com.jnhprojects.mcheli.entity.AircraftEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.network.CustomPayloadEvent;

/**
 * Client to server: this player is locking on to an entity. If it is a vehicle with flares, or a
 * crew member of one, its crew hears the warning. The original's {@code MCH_PacketNotifyLock}.
 */
public record LockWarningPacket(int targetId) {

    public static void encode(LockWarningPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.targetId);
    }

    public static LockWarningPacket decode(FriendlyByteBuf buf) {
        return new LockWarningPacket(buf.readVarInt());
    }

    public static void handle(LockWarningPacket msg, CustomPayloadEvent.Context ctx) {
        ServerPlayer sender = ctx.getSender();
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
}
