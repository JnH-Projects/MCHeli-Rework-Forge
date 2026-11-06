package com.jnhprojects.mcheli.client.sound;

import com.jnhprojects.mcheli.content.aircraft.AircraftDefinition;
import com.jnhprojects.mcheli.entity.AircraftEntity;
import com.jnhprojects.mcheli.entity.EngineSoundSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.Map;

/**
 * Starts engine and rotor loops for vehicles near the player and restarts them after they fall
 * silent. The sounds themselves track their vehicle until it stops or disappears.
 */
public final class EngineSoundPlayer {
    /** Ticks to wait before restarting a loop, as the original did, so a stuttering engine does not spam. */
    private static final int RESTART_DELAY = 20;

    /** By {@link #key}, since a vehicle can have two loops. */
    private static final Map<Long, EngineSound> PLAYING = new HashMap<>();
    private static final Map<Long, Integer> COOLDOWN = new HashMap<>();

    private EngineSoundPlayer() {
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        LocalPlayer player = mc.player;
        if (level == null || player == null || mc.isPaused()) {
            if (level == null) {
                PLAYING.clear();
                COOLDOWN.clear();
            }
            return;
        }
        PLAYING.values().removeIf(EngineSound::isStopped);
        COOLDOWN.replaceAll((id, ticks) -> ticks - 1);
        COOLDOWN.values().removeIf(ticks -> ticks <= 0);

        for (Entity entity : level.entitiesForRendering()) {
            if (!(entity instanceof AircraftEntity vehicle) || !(entity instanceof EngineSoundSource source)) {
                continue;
            }
            AircraftDefinition def = vehicle.definition();
            if (def == null) {
                continue;
            }
            double range = def.soundRange * 1.2;
            if (vehicle.distanceToSqr(player) > range * range) {
                continue;
            }
            start(mc, vehicle, source, def.soundOrDefault(), def.soundVolume, false);
            start(mc, vehicle, source, def.rotorSound, def.rotorSoundVolume, true);
        }
    }

    private static void start(Minecraft mc, AircraftEntity vehicle, EngineSoundSource source, String sound, float volume, boolean rotor) {
        long key = key(vehicle, rotor);
        if (sound.isEmpty() || PLAYING.containsKey(key) || COOLDOWN.containsKey(key) || volume * source.engineVolume() <= 0.01F) {
            return;
        }
        EngineSound loop = new EngineSound(vehicle, source, sound, rotor);
        mc.getSoundManager().play(loop);
        PLAYING.put(key, loop);
        COOLDOWN.put(key, RESTART_DELAY);
    }

    private static long key(AircraftEntity vehicle, boolean rotor) {
        return (long) vehicle.getId() << 1 | (rotor ? 1L : 0L);
    }
}
