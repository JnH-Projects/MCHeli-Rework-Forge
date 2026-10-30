package com.jnhprojects.mcheli.network;

import com.jnhprojects.mcheli.entity.AircraftEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;

/**
 * Client to server: the player right-clicked a vehicle's hull outside its main hit box. The
 * server repeats the reach check against the hull boxes, then treats it as an ordinary click.
 *
 * @param location where on the hull the click landed, in world coordinates
 */
public record BoardVehiclePacket(int entityId, Vec3 location) implements CustomPacketPayload {
    public static final Type<BoardVehiclePacket> TYPE = ModNetwork.type("board_vehicle");
    public static final StreamCodec<RegistryFriendlyByteBuf, BoardVehiclePacket> STREAM_CODEC = StreamCodec.ofMember(BoardVehiclePacket::encode, BoardVehiclePacket::decode);

    /** The same slack vanilla allows on top of the player's reach for a click on an entity. */
    private static final double REACH_SLACK = 3.0;

    public static void encode(BoardVehiclePacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.entityId);
        buf.writeDouble(msg.location.x);
        buf.writeDouble(msg.location.y);
        buf.writeDouble(msg.location.z);
    }

    public static BoardVehiclePacket decode(FriendlyByteBuf buf) {
        return new BoardVehiclePacket(buf.readVarInt(), new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()));
    }

    public static void handle(BoardVehiclePacket msg, ServerPlayer sender) {
        if (sender == null || !(sender.level().getEntity(msg.entityId) instanceof AircraftEntity vehicle) || vehicle.isRemoved()) {
            return;
        }
        Vec3 eye = sender.getEyePosition();
        double reach = sender.entityInteractionRange() + REACH_SLACK;
        // The click must land on the hull, and the hull must be within reach.
        if (eye.distanceToSqr(msg.location) > reach * reach) {
            return;
        }
        Vec3 through = eye.add(msg.location.subtract(eye).scale(1.05));
        boolean onHull = vehicle.clipHitBoxes(eye, through, 0.5).isPresent();
        vehicle.consumeHitBoxFactor();
        if (onHull) {
            vehicle.interact(sender, InteractionHand.MAIN_HAND, msg.location.subtract(vehicle.position()));
        }
    }

    @Override
    public Type<BoardVehiclePacket> type() {
        return TYPE;
    }
}
