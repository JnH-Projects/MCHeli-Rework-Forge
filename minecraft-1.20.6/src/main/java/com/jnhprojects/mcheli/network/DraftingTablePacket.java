package com.jnhprojects.mcheli.network;

import com.jnhprojects.mcheli.menu.DraftingTableMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * The Create button of the drafting table, {@code MCH_DraftingTableCreatePacket}: make one of the
 * recipe with this id.
 */
public record DraftingTablePacket(ResourceLocation recipe) implements CustomPacketPayload {
    public static final Type<DraftingTablePacket> TYPE = ModNetwork.type("drafting_table");
    public static final StreamCodec<RegistryFriendlyByteBuf, DraftingTablePacket> STREAM_CODEC = StreamCodec.ofMember(DraftingTablePacket::encode, DraftingTablePacket::decode);

    public static void encode(DraftingTablePacket msg, FriendlyByteBuf buf) {
        buf.writeResourceLocation(msg.recipe);
    }

    public static DraftingTablePacket decode(FriendlyByteBuf buf) {
        return new DraftingTablePacket(buf.readResourceLocation());
    }

    public static void handle(DraftingTablePacket msg, ServerPlayer sender) {
        if (sender != null && sender.containerMenu instanceof DraftingTableMenu menu && menu.stillValid(sender)) {
            menu.craft(sender, msg.recipe);
        }
    }

    @Override
    public Type<DraftingTablePacket> type() {
        return TYPE;
    }
}
