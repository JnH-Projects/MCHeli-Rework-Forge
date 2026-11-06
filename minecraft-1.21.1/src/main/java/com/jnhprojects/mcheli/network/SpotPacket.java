package com.jnhprojects.mcheli.network;

import com.jnhprojects.mcheli.ModSounds;
import com.jnhprojects.mcheli.item.RangefinderItem;
import com.jnhprojects.mcheli.weapon.Spotting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/**
 * The attack key through a rangefinder, {@code MCH_PacketIndSpotEntity}: spot players and
 * vehicles, spot mobs, or mark a point for the team. Spotting uses a charge; marking is free.
 */
public record SpotPacket(Mode mode) implements CustomPacketPayload {
    public static final Type<SpotPacket> TYPE = ModNetwork.type("spot");
    public static final StreamCodec<RegistryFriendlyByteBuf, SpotPacket> STREAM_CODEC = StreamCodec.ofMember(SpotPacket::encode, SpotPacket::decode);

    /** The rangefinder's modes, in the order its mode key steps through them. */
    public enum Mode {
        PLAYERS_AND_VEHICLES(Spotting.PLAYERS | Spotting.GROUND_VEHICLES | Spotting.HELICOPTERS | Spotting.PLANES),
        MOBS(Spotting.OTHERS | Spotting.MONSTERS),
        MARK_POINT(0);

        final int filter;

        Mode(int filter) {
            this.filter = filter;
        }
    }

    public static void encode(SpotPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.mode.ordinal());
    }

    public static SpotPacket decode(FriendlyByteBuf buf) {
        int ordinal = buf.readVarInt();
        Mode[] modes = Mode.values();
        if (ordinal < 0 || ordinal >= modes.length) {
            throw new IllegalArgumentException("Unknown spot mode " + ordinal);
        }
        return new SpotPacket(modes[ordinal]);
    }

    public static void handle(SpotPacket msg, ServerPlayer sender) {
        if (sender == null || !RangefinderItem.canUse(sender)) {
            return;
        }
        ItemStack stack = sender.getItemInHand(InteractionHand.MAIN_HAND);
        if (!(stack.getItem() instanceof RangefinderItem)) {
            stack = sender.getItemInHand(InteractionHand.OFF_HAND);
            if (!(stack.getItem() instanceof RangefinderItem)) {
                return;
            }
        }
        boolean done;
        if (msg.mode == Mode.MARK_POINT) {
            done = Spotting.markPoint(sender);
        } else if (RangefinderItem.charges(stack) > 0) {
            if (!sender.getAbilities().instabuild) {
                // An empty rangefinder stays: it only needs redstone, unlike a worn out tool.
                stack.setDamageValue(stack.getDamageValue() + 1);
            }
            done = Spotting.spot(sender, msg.mode.filter);
        } else {
            done = false;
        }
        ModSounds.playAt(sender, done ? "pi" : "ng", 1.0F, 1.0F);
    }

    @Override
    public Type<SpotPacket> type() {
        return TYPE;
    }
}
