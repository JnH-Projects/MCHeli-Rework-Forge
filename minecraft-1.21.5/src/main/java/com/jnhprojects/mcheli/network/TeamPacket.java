package com.jnhprojects.mcheli.network;

import com.jnhprojects.mcheli.team.TeamManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * A button of the team manager, {@code MCH_PacketIndMultiplayCommand}. Operators only; the
 * server checks again.
 *
 * @param name  the new team's name, for {@link Action#CREATE_TEAM}
 * @param color the new team's colour, as a vanilla team colour id
 */
public record TeamPacket(Action action, String name, int color, boolean friendlyFire) implements CustomPacketPayload {
    public static final Type<TeamPacket> TYPE = ModNetwork.type("team");
    public static final StreamCodec<RegistryFriendlyByteBuf, TeamPacket> STREAM_CODEC = StreamCodec.ofMember(TeamPacket::encode, TeamPacket::decode);

    public enum Action {
        PVP,
        SHUFFLE,
        CREATE_TEAM,
        JUMP_TO_SPAWN,
        DESTROY_ALL
    }

    public TeamPacket(Action action) {
        this(action, "", 0, false);
    }

    public static void encode(TeamPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.action.ordinal());
        buf.writeUtf(msg.name, 16);
        buf.writeVarInt(msg.color);
        buf.writeBoolean(msg.friendlyFire);
    }

    public static TeamPacket decode(FriendlyByteBuf buf) {
        int ordinal = buf.readVarInt();
        Action[] actions = Action.values();
        if (ordinal < 0 || ordinal >= actions.length) {
            throw new IllegalArgumentException("Unknown team action " + ordinal);
        }
        return new TeamPacket(actions[ordinal], buf.readUtf(16), buf.readVarInt(), buf.readBoolean());
    }

    public static void handle(TeamPacket msg, ServerPlayer sender) {
        if (sender == null || !TeamManager.mayManage(sender)) {
            return;
        }
        switch (msg.action) {
            case PVP -> TeamManager.togglePvp(sender);
            case SHUFFLE -> TeamManager.shuffle(sender);
            case CREATE_TEAM -> TeamManager.create(sender, msg.name, msg.color, msg.friendlyFire);
            case JUMP_TO_SPAWN -> TeamManager.jumpToSpawn(sender);
            case DESTROY_ALL -> TeamManager.destroyAll(sender);
        }
    }

    @Override
    public Type<TeamPacket> type() {
        return TYPE;
    }
}
