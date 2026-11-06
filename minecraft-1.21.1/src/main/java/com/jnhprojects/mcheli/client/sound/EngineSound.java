package com.jnhprojects.mcheli.client.sound;

import com.jnhprojects.mcheli.ModSounds;
import com.jnhprojects.mcheli.content.aircraft.AircraftDefinition;
import com.jnhprojects.mcheli.content.aircraft.PitchCurve;
import com.jnhprojects.mcheli.entity.AircraftEntity;
import com.jnhprojects.mcheli.entity.EngineSoundSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/**
 * A looping sound of one vehicle: its engine, or the rotor loop some vehicles have alongside it.
 * Ported from {@code MCH_SoundUpdater}.
 *
 * <p>Volume and pitch follow the vehicle's throttle every tick, the pitch along the definition's
 * curve if it has one and otherwise the way the kind of vehicle always has. For listeners outside
 * the vehicle the sound is placed between them and the vehicle, so that the 16 block range of a
 * normal sound stretches to the definition's {@code SoundRange}, and the pitch bends slightly as the
 * vehicle approaches or leaves, which is what the original did.
 */
final class EngineSound extends RuntimeSoundInstance {
    private static final float MAX_DOPPLER = 0.2F;

    private final AircraftEntity vehicle;
    private final EngineSoundSource source;
    private final boolean rotor;
    private float doppler;
    private double lastDistance = -1.0;

    EngineSound(AircraftEntity vehicle, EngineSoundSource source, String sound, boolean rotor) {
        super(ModSounds.get(sound), SoundSource.NEUTRAL, RandomSource.create());
        this.vehicle = vehicle;
        this.source = source;
        this.rotor = rotor;
        this.looping = true;
        this.delay = 0;
        update();
    }

    @Override
    public void tick() {
        if (vehicle.isRemoved() || vehicle.definition() == null) {
            stop();
            return;
        }
        update();
        if (volume <= 0.0F) {
            stop();
        }
    }

    private void update() {
        AircraftDefinition def = vehicle.definition();
        LocalPlayer player = Minecraft.getInstance().player;
        if (def == null || player == null) {
            volume = 0.0F;
            return;
        }
        volume = (rotor ? def.rotorSoundVolume : def.soundVolume) * source.engineVolume();
        PitchCurve curve = rotor ? def.rotorSoundPitchCurve : def.soundPitchCurve;
        float pitch = def.soundPitch * (curve != null ? curve.at(vehicle.visibleThrottle()) : source.enginePitch());
        boolean aboard = player.getRootVehicle() == vehicle.getRootVehicle();
        if (aboard) {
            x = vehicle.getX();
            y = vehicle.getY();
            z = vehicle.getZ();
        } else {
            double dx = vehicle.getX() - player.getX();
            double dz = vehicle.getZ() - player.getZ();
            double distance = Math.sqrt(dx * dx + dz * dz);
            if (lastDistance >= 0.0) {
                double closing = lastDistance - distance;
                if (Math.abs(closing) > 0.3) {
                    doppler = Math.clamp(doppler + (float) (closing / 40.0), -MAX_DOPPLER, MAX_DOPPLER);
                }
            }
            doppler *= 0.9F;
            pitch += doppler;
            lastDistance = distance;

            double scale = def.soundRange / 16.0;
            x = player.getX() + dx / scale;
            y = player.getY() + (vehicle.getY() - player.getY()) / scale;
            z = player.getZ() + dz / scale;
        }
        this.pitch = Math.max(pitch, 0.0F);
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }
}
