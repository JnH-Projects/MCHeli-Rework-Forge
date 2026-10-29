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
 * Starts engine loops for vehicles near the player and restarts them after they fall silent.
 * The sounds themselves track their vehicle until it stops or disappears.
 */
public final class EngineSoundPlayer {
    /** Ticks to wait before restarting a loop, as the original did, so a stuttering engine does not spam. */
    private static final int RESTART_DELAY = 20;

    private static final Map<Integer, EngineSound> PLAYING = new HashMap<>();
    private static final Map<Integer, Integer> COOLDOWN = new HashMap<>();

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
            int id = vehicle.getId();
            AircraftDefinition def = vehicle.definition();
            if (def == null || PLAYING.containsKey(id) || COOLDOWN.containsKey(id)) {
                continue;
            }
            String sound = def.soundOrDefault();
            double range = def.soundRange * 1.2;
            if (sound.isEmpty() || vehicle.distanceToSqr(player) > range * range) {
                continue;
            }
            if (def.soundVolume * source.engineVolume() > 0.01F) {
                EngineSound loop = new EngineSound(vehicle, source, sound);
                mc.getSoundManager().play(loop);
                PLAYING.put(id, loop);
                COOLDOWN.put(id, RESTART_DELAY);
            }
        }
    }
}
