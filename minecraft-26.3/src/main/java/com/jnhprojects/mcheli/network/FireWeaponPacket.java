package com.jnhprojects.mcheli.network;

import com.jnhprojects.mcheli.entity.AircraftEntity;
import com.jnhprojects.mcheli.weapon.ServerFiring;
import com.jnhprojects.mcheli.weapon.Shot;
import com.jnhprojects.mcheli.weapon.VehicleWeapons;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * Client to server: the crew member's selected weapon fired on their side, so fire it for real.
 * The client already checked readiness, ammunition and lock; the server checks again with its own
 * timers and only trusts the two option values, a locked entity and a fire mode.
 *
 * @param weapon the weapon the client fired, ignored unless it is still the seat's selection
 */
public record FireWeaponPacket(int entityId, int weapon, int option1, int option2) implements CustomPacketPayload {
    public static final Type<FireWeaponPacket> TYPE = ModNetwork.type("fire_weapon");
    public static final StreamCodec<RegistryFriendlyByteBuf, FireWeaponPacket> STREAM_CODEC = StreamCodec.ofMember(FireWeaponPacket::encode, FireWeaponPacket::decode);

    public static void encode(FireWeaponPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.entityId);
        buf.writeVarInt(msg.weapon);
        buf.writeVarInt(msg.option1);
        buf.writeVarInt(msg.option2);
    }

    public static FireWeaponPacket decode(FriendlyByteBuf buf) {
        return new FireWeaponPacket(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(FireWeaponPacket msg, ServerPlayer sender) {
        if (sender == null || !(sender.getVehicle() instanceof AircraftEntity vehicle) || vehicle.getId() != msg.entityId) {
            return;
        }
        VehicleWeapons weapons = vehicle.weapons();
        int seat = vehicle.seatOf(sender);
        if (weapons == null || weapons.selected(seat) != msg.weapon || weapons.operatorSeat(msg.weapon) != seat) {
            return;
        }
        Shot shot = new Shot(vehicle, sender);
        shot.option1 = msg.option1;
        shot.option2 = msg.option2;
        weapons.use(sender, seat, shot, ServerFiring.INSTANCE);
    }

    @Override
    public Type<FireWeaponPacket> type() {
        return TYPE;
    }
}
