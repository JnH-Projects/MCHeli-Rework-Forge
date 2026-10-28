package com.jnhprojects.mcheli.network;

import com.jnhprojects.mcheli.MCHeli;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

public final class ModNetwork {
    /** Bump whenever a packet layout changes. Clients and servers must match exactly. */
    private static final int PROTOCOL_VERSION = 4;

    private static final SimpleChannel CHANNEL = ChannelBuilder
            .named(Identifier.fromNamespaceAndPath(MCHeli.MOD_ID, "main"))
            .networkProtocolVersion(PROTOCOL_VERSION)
            .simpleChannel()
            .play()
                .serverbound()
                    .addMain(PilotStatePacket.class,
                            StreamCodec.<RegistryFriendlyByteBuf, PilotStatePacket>ofMember(PilotStatePacket::encode, PilotStatePacket::decode),
                            PilotStatePacket::handle)
                    .addMain(CrewActionPacket.class,
                            StreamCodec.<RegistryFriendlyByteBuf, CrewActionPacket>ofMember(CrewActionPacket::encode, CrewActionPacket::decode),
                            CrewActionPacket::handle)
                    .addMain(FireWeaponPacket.class,
                            StreamCodec.<RegistryFriendlyByteBuf, FireWeaponPacket>ofMember(FireWeaponPacket::encode, FireWeaponPacket::decode),
                            FireWeaponPacket::handle)
                    .addMain(BoardVehiclePacket.class,
                            StreamCodec.<RegistryFriendlyByteBuf, BoardVehiclePacket>ofMember(BoardVehiclePacket::encode, BoardVehiclePacket::decode),
                            BoardVehiclePacket::handle)
                    .addMain(LockWarningPacket.class,
                            StreamCodec.<RegistryFriendlyByteBuf, LockWarningPacket>ofMember(LockWarningPacket::encode, LockWarningPacket::decode),
                            LockWarningPacket::handle)
                .clientbound()
                    .addMain(WeaponStatePacket.class,
                            StreamCodec.<RegistryFriendlyByteBuf, WeaponStatePacket>ofMember(WeaponStatePacket::encode, WeaponStatePacket::decode),
                            WeaponStatePacket::handle)
                    .addMain(ExplosionEffectPacket.class,
                            StreamCodec.<RegistryFriendlyByteBuf, ExplosionEffectPacket>ofMember(ExplosionEffectPacket::encode, ExplosionEffectPacket::decode),
                            ExplosionEffectPacket::handle)
            .build();

    private ModNetwork() {
    }

    /** Forces the channel to be built during mod construction. */
    public static void register() {
        MCHeli.LOGGER.debug("Network channel {} v{}", CHANNEL.getName(), CHANNEL.getProtocolVersion());
    }

    public static void sendToServer(Object message) {
        CHANNEL.send(message, PacketDistributor.SERVER.noArg());
    }

    public static void sendToPlayer(ServerPlayer player, Object message) {
        CHANNEL.send(message, PacketDistributor.PLAYER.with(player));
    }

    /** Sends to every player in {@code level} within {@code range} blocks of the point. */
    public static void sendNear(ServerLevel level, double x, double y, double z, double range, Object message) {
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(x, y, z) < range * range) {
                sendToPlayer(player, message);
            }
        }
    }
}
