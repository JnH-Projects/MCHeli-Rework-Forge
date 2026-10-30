package com.jnhprojects.mcheli.client.render;

import com.jnhprojects.mcheli.entity.AircraftEntity;
import com.jnhprojects.mcheli.physics.AircraftRotation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

/**
 * Tilts players riding a vehicle with its hull, as the original did from its render hook.
 *
 * <p>Vanilla keeps riders upright, so a banked plane left the pilot standing straight up through
 * the canopy. The tilt is the hull's pitch and roll expressed in the world frame, applied about
 * the rider's vehicle attachment point (the hip), which is the point that sits on the seat. The
 * rider's own body yaw is applied by the renderer afterwards, so turning in the seat still works.
 *
 * <p>The entity render dispatcher pushes a pose around every entity, so nothing needs popping here.
 */
public final class RiderPose {
    private RiderPose() {
    }

    /** Called before a living entity is drawn, with the pose the renderer is about to use. */
    public static void beforeRender(LivingEntity rider, PoseStack pose, float partialTicks) {
        if (!(rider instanceof Player) || !(rider.getVehicle() instanceof AircraftEntity vehicle)) {
            return;
        }
        float yaw = vehicle.getYRot(partialTicks);
        float pitch = vehicle.getXRot(partialTicks);
        float roll = vehicle.roll(partialTicks);
        if (pitch == 0.0F && roll == 0.0F) {
            return;
        }
        Quaternionf tilt = AircraftRotation.orientation(yaw, pitch, roll).rotateY((float) Math.toRadians(yaw));
        Vec3 hip = rider.getVehicleAttachmentPoint(vehicle);
        pose.rotateAround(tilt, (float) hip.x, (float) hip.y, (float) hip.z);
    }
}
