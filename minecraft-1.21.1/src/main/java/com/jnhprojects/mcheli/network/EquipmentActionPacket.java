package com.jnhprojects.mcheli.network;

import com.jnhprojects.mcheli.entity.GltdEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * A request from a player using a piece of equipment they sit at, such as the GLTD. Like
 * {@link CrewActionPacket} the client only asks; the equipment checks that the sender is the one
 * using it.
 */
public record EquipmentActionPacket(int entityId, Action action) implements CustomPacketPayload {
    public static final Type<EquipmentActionPacket> TYPE = ModNetwork.type("equipment_action");
    public static final StreamCodec<RegistryFriendlyByteBuf, EquipmentActionPacket> STREAM_CODEC =
            StreamCodec.ofMember(EquipmentActionPacket::encode, EquipmentActionPacket::decode);

    public enum Action {
        /** Mark the spot under the sight and call in the A-10. */
        CALL_STRIKE,
        /** Turn night vision on for the sender. */
        NIGHT_VISION_ON,
        /** Turn the sender's night vision off again. */
        NIGHT_VISION_OFF
    }

    public static void encode(EquipmentActionPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.entityId);
        buf.writeVarInt(msg.action.ordinal());
    }

    public static EquipmentActionPacket decode(FriendlyByteBuf buf) {
        int entityId = buf.readVarInt();
        int ordinal = buf.readVarInt();
        Action[] actions = Action.values();
        if (ordinal < 0 || ordinal >= actions.length) {
            throw new IllegalArgumentException("Unknown equipment action " + ordinal);
        }
        return new EquipmentActionPacket(entityId, actions[ordinal]);
    }

    public static void handle(EquipmentActionPacket msg, ServerPlayer sender) {
        if (sender == null || sender.getVehicle() == null || sender.getVehicle().getId() != msg.entityId) {
            return;
        }
        if (sender.getVehicle() instanceof GltdEntity gltd) {
            switch (msg.action) {
                case CALL_STRIKE -> gltd.callStrike(sender);
                case NIGHT_VISION_ON, NIGHT_VISION_OFF -> gltd.setNightVision(sender, msg.action == Action.NIGHT_VISION_ON);
            }
        }
    }

    @Override
    public Type<EquipmentActionPacket> type() {
        return TYPE;
    }
}
