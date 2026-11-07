package com.jnhprojects.mcheli.network;

import com.jnhprojects.mcheli.entity.ClientAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server to player: play a sound a content pack brought, once, at a point. The game's own sound
 * packet ends at the sound manager, which only knows the sounds in {@code sounds.json}, and a pack
 * has none: it would be silent. See {@code ModSounds}.
 */
public record PackSoundPacket(String sound, double x, double y, double z, float volume, float pitch) implements CustomPacketPayload {
    public static final Type<PackSoundPacket> TYPE = ModNetwork.type("pack_sound");
    public static final StreamCodec<RegistryFriendlyByteBuf, PackSoundPacket> STREAM_CODEC = StreamCodec.ofMember(PackSoundPacket::encode, PackSoundPacket::decode);

    private static final int MAX_NAME = 256;

    public static void encode(PackSoundPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.sound, MAX_NAME);
        buf.writeDouble(msg.x);
        buf.writeDouble(msg.y);
        buf.writeDouble(msg.z);
        buf.writeFloat(msg.volume);
        buf.writeFloat(msg.pitch);
    }

    public static PackSoundPacket decode(FriendlyByteBuf buf) {
        return new PackSoundPacket(buf.readUtf(MAX_NAME), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readFloat(), buf.readFloat());
    }

    public static void handle(PackSoundPacket msg) {
        ClientAccess.packs().onPackSound(msg);
    }

    @Override
    public Type<PackSoundPacket> type() {
        return TYPE;
    }
}
