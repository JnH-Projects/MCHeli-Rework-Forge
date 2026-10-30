package com.jnhprojects.mcheli.network;

import com.jnhprojects.mcheli.entity.ClientAccess;
import com.jnhprojects.mcheli.weapon.ExplosionEffect;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.Entity;

/**
 * Server to nearby players: draw an MCHeli explosion here. The original's
 * {@code MCH_PacketEffectExplosion}, plus a flag for the bigger fireball of a crashing vehicle.
 */
public record ExplosionEffectPacket(double x, double y, double z, float size, boolean inWater, boolean crash) implements CustomPacketPayload {
    public static final Type<ExplosionEffectPacket> TYPE = ModNetwork.type("explosion_effect");
    public static final StreamCodec<RegistryFriendlyByteBuf, ExplosionEffectPacket> STREAM_CODEC = StreamCodec.ofMember(ExplosionEffectPacket::encode, ExplosionEffectPacket::decode);

    public ExplosionEffectPacket(double x, double y, double z, float size, boolean inWater) {
        this(x, y, z, size, inWater, false);
    }

    public static void encode(ExplosionEffectPacket msg, FriendlyByteBuf buf) {
        buf.writeDouble(msg.x);
        buf.writeDouble(msg.y);
        buf.writeDouble(msg.z);
        buf.writeFloat(msg.size);
        buf.writeBoolean(msg.inWater);
        buf.writeBoolean(msg.crash);
    }

    public static ExplosionEffectPacket decode(FriendlyByteBuf buf) {
        return new ExplosionEffectPacket(buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readFloat(), buf.readBoolean(),
                buf.readBoolean());
    }

    public static void handle(ExplosionEffectPacket msg) {
        Entity player = ClientAccess.localPlayer();
        if (player == null || !Float.isFinite(msg.size)) {
            return;
        }
        float size = Math.min(msg.size, 50.0F);
        if (msg.crash) {
            ExplosionEffect.crash(player.level(), msg.x, msg.y, msg.z, Math.min(size, ExplosionEffect.MAX_CRASH_SIZE));
        } else {
            ExplosionEffect.play(player.level(), msg.x, msg.y, msg.z, size, msg.inWater);
        }
    }

    @Override
    public Type<ExplosionEffectPacket> type() {
        return TYPE;
    }
}
