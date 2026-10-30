package com.jnhprojects.mcheli;

import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sound events from {@code assets/mcheli/sounds.json}.
 *
 * <p>The events are not put in the registry. Content files name their sounds freely and a content
 * pack may bring its own, so they are created on demand as direct holders, which the sound packet
 * and the sound manager both accept.
 */
public final class ModSounds {
    private static final Map<String, SoundEvent> EVENTS = new ConcurrentHashMap<>();

    private ModSounds() {
    }

    public static SoundEvent get(String name) {
        return EVENTS.computeIfAbsent(name, n -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(MCHeli.MOD_ID, n)));
    }

    /** Plays {@code name} at the entity for everyone nearby. Call on the server. */
    public static void playAt(Entity entity, String name, float volume, float pitch) {
        entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), get(name), SoundSource.NEUTRAL, volume, pitch);
    }

    /** Plays {@code name} at a position for everyone nearby. Call on the server. */
    public static void playAt(Level level, double x, double y, double z, String name, float volume, float pitch) {
        level.playSound(null, x, y, z, get(name), SoundSource.NEUTRAL, volume, pitch);
    }

    /** Plays {@code name} for one player only, such as a hit confirmation. */
    public static void playTo(ServerPlayer player, String name, float volume, float pitch) {
        player.connection.send(new ClientboundSoundPacket(Holder.direct(get(name)), SoundSource.PLAYERS,
                player.getX(), player.getY(), player.getZ(), volume, pitch, player.getRandom().nextLong()));
    }
}
