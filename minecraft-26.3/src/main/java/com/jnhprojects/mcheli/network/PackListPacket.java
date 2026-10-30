package com.jnhprojects.mcheli.network;

import com.jnhprojects.mcheli.entity.ClientAccess;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.List;

/**
 * Server to player: the content packs the server uses, in load order. Sent on join and after
 * {@code /mcheli reload}. The client requests the packs it has not cached ({@link PackRequestPacket})
 * and then rebuilds its content from exactly this list, so both sides fly the same definitions.
 * The imported 1.0.3 content is never listed: every player imports their own copy.
 */
public record PackListPacket(List<Entry> packs) implements CustomPacketPayload {
    public static final Type<PackListPacket> TYPE = ModNetwork.type("pack_list");
    public static final StreamCodec<RegistryFriendlyByteBuf, PackListPacket> STREAM_CODEC = StreamCodec.ofMember(PackListPacket::encode, PackListPacket::decode);

    public static final int MAX_PACKS = 256;

    /**
     * @param size length of the pack's zip in bytes, which is what {@link PackDataPacket} sends
     */
    public record Entry(String id, String version, String hash, int size) {
    }

    public static void encode(PackListPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.packs.size());
        for (Entry entry : msg.packs) {
            buf.writeUtf(entry.id, 64);
            buf.writeUtf(entry.version, 32);
            buf.writeUtf(entry.hash, 64);
            buf.writeVarInt(entry.size);
        }
    }

    public static PackListPacket decode(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        if (count < 0 || count > MAX_PACKS) {
            throw new DecoderException("Too many content packs: " + count);
        }
        List<Entry> packs = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            packs.add(new Entry(buf.readUtf(64), buf.readUtf(32), buf.readUtf(64), buf.readVarInt()));
        }
        return new PackListPacket(List.copyOf(packs));
    }

    public static void handle(PackListPacket msg) {
        ClientAccess.packs().onPackList(msg);
    }

    @Override
    public Type<PackListPacket> type() {
        return TYPE;
    }
}
