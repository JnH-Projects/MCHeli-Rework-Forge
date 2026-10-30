package com.jnhprojects.mcheli.network;

import com.jnhprojects.mcheli.MCHeli;
import com.jnhprojects.mcheli.platform.Platform;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Our packets. Each one is a custom payload with its own id and the same encoding on every loader,
 * so a client on one loader can talk to a server on another. The loader module registers the
 * lists below with its own networking API; handlers always run on the main thread.
 */
public final class ModNetwork {
    /** Bump whenever a packet layout changes. Clients and servers must match exactly. */
    public static final int PROTOCOL_VERSION = 6;

    /** Client to server. The handler gets the sending player. */
    public static final List<Serverbound<?>> SERVERBOUND = List.of(
            new Serverbound<>(PilotStatePacket.TYPE, PilotStatePacket.STREAM_CODEC, PilotStatePacket::handle),
            new Serverbound<>(CrewActionPacket.TYPE, CrewActionPacket.STREAM_CODEC, CrewActionPacket::handle),
            new Serverbound<>(FireWeaponPacket.TYPE, FireWeaponPacket.STREAM_CODEC, FireWeaponPacket::handle),
            new Serverbound<>(BoardVehiclePacket.TYPE, BoardVehiclePacket.STREAM_CODEC, BoardVehiclePacket::handle),
            new Serverbound<>(LockWarningPacket.TYPE, LockWarningPacket.STREAM_CODEC, LockWarningPacket::handle),
            new Serverbound<>(PackRequestPacket.TYPE, PackRequestPacket.STREAM_CODEC, PackRequestPacket::handle));

    /** Server to client. Handlers touch client classes, so they only ever run on a client. */
    public static final List<Clientbound<?>> CLIENTBOUND = List.of(
            new Clientbound<>(WeaponStatePacket.TYPE, WeaponStatePacket.STREAM_CODEC, WeaponStatePacket::handle),
            new Clientbound<>(ExplosionEffectPacket.TYPE, ExplosionEffectPacket.STREAM_CODEC, ExplosionEffectPacket::handle),
            new Clientbound<>(PackListPacket.TYPE, PackListPacket.STREAM_CODEC, PackListPacket::handle),
            new Clientbound<>(PackDataPacket.TYPE, PackDataPacket.STREAM_CODEC, PackDataPacket::handle));

    private ModNetwork() {
    }

    static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> type(String name) {
        return new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(MCHeli.MOD_ID, name));
    }

    public static void sendToServer(CustomPacketPayload message) {
        Platform.get().sendToServer(message);
    }

    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload message) {
        Platform.get().sendToPlayer(player, message);
    }

    public static void sendToAll(MinecraftServer server, CustomPacketPayload message) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            sendToPlayer(player, message);
        }
    }

    /** Sends to every player in {@code level} within {@code range} blocks of the point. */
    public static void sendNear(ServerLevel level, double x, double y, double z, double range, CustomPacketPayload message) {
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(x, y, z) < range * range) {
                sendToPlayer(player, message);
            }
        }
    }

    public record Serverbound<T extends CustomPacketPayload>(CustomPacketPayload.Type<T> type,
                                                             StreamCodec<RegistryFriendlyByteBuf, T> codec,
                                                             BiConsumer<T, ServerPlayer> handler) {
    }

    public record Clientbound<T extends CustomPacketPayload>(CustomPacketPayload.Type<T> type,
                                                             StreamCodec<RegistryFriendlyByteBuf, T> codec,
                                                             Consumer<T> handler) {
    }
}
