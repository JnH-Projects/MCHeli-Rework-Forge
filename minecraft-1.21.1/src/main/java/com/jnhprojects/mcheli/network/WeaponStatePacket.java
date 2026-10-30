package com.jnhprojects.mcheli.network;

import com.jnhprojects.mcheli.entity.AircraftEntity;
import com.jnhprojects.mcheli.entity.ClientAccess;
import com.jnhprojects.mcheli.weapon.VehicleWeapons;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.Entity;

/**
 * Server to crew: the magazine and reserve of one weapon after it fired, reloaded or was
 * resupplied. The original's {@code MCH_PacketNotifyAmmoNum}.
 */
public record WeaponStatePacket(int entityId, int weapon, int ammo, int rest) implements CustomPacketPayload {
    public static final Type<WeaponStatePacket> TYPE = ModNetwork.type("weapon_state");
    public static final StreamCodec<RegistryFriendlyByteBuf, WeaponStatePacket> STREAM_CODEC = StreamCodec.ofMember(WeaponStatePacket::encode, WeaponStatePacket::decode);

    public static void encode(WeaponStatePacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.entityId);
        buf.writeVarInt(msg.weapon);
        buf.writeVarInt(msg.ammo);
        buf.writeVarInt(msg.rest);
    }

    public static WeaponStatePacket decode(FriendlyByteBuf buf) {
        return new WeaponStatePacket(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(WeaponStatePacket msg) {
        Entity player = ClientAccess.localPlayer();
        if (player != null && player.level().getEntity(msg.entityId) instanceof AircraftEntity vehicle) {
            VehicleWeapons weapons = vehicle.weapons();
            if (weapons != null) {
                weapons.applyAmmo(msg.weapon, msg.ammo, msg.rest);
            }
        }
    }

    @Override
    public Type<WeaponStatePacket> type() {
        return TYPE;
    }
}
