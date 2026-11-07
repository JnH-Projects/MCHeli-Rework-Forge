package com.jnhprojects.mcheli.network;

import com.jnhprojects.mcheli.menu.DraftingTableMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;

/**
 * The Create button of the drafting table, {@code MCH_DraftingTableCreatePacket}: make one of the
 * recipe the player's recipe book knows by this display id.
 */
public record DraftingTablePacket(RecipeDisplayId recipe) implements CustomPacketPayload {
    public static final Type<DraftingTablePacket> TYPE = ModNetwork.type("drafting_table");
    public static final StreamCodec<RegistryFriendlyByteBuf, DraftingTablePacket> STREAM_CODEC = StreamCodec.ofMember(DraftingTablePacket::encode, DraftingTablePacket::decode);

    public static void encode(DraftingTablePacket msg, FriendlyByteBuf buf) {
        RecipeDisplayId.STREAM_CODEC.encode(buf, msg.recipe);
    }

    public static DraftingTablePacket decode(FriendlyByteBuf buf) {
        return new DraftingTablePacket(RecipeDisplayId.STREAM_CODEC.decode(buf));
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
