package com.jnhprojects.mcheli.client.render;

import com.jnhprojects.mcheli.entity.AircraftEntity;
import com.jnhprojects.mcheli.entity.AircraftRotation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLivingEvent;
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

    public static void init() {
        RenderLivingEvent.Pre.BUS.addListener(RiderPose::onRenderLiving);
    }

    private static void onRenderLiving(RenderLivingEvent.Pre<?, ?, ?> event) {
        // Render states carry no entity reference; players are the ones that keep their id.
        if (!(event.getState() instanceof AvatarRenderState state)) {
            return;
        }
        ClientLevel level = Minecraft.getInstance().level;
        Entity rider = level != null ? level.getEntity(state.id) : null;
        if (rider == null || !(rider.getVehicle() instanceof AircraftEntity vehicle)) {
            return;
        }
        float partialTicks = event.getCameraState().cameraEntityPartialTicks;
        float yaw = vehicle.getYRot(partialTicks);
        float pitch = vehicle.getXRot(partialTicks);
        float roll = vehicle.roll(partialTicks);
        if (pitch == 0.0F && roll == 0.0F) {
            return;
        }
        Quaternionf tilt = AircraftRotation.orientation(yaw, pitch, roll).rotateY((float) Math.toRadians(yaw));
        Vec3 hip = rider.getVehicleAttachmentPoint(vehicle);
        event.getPoseStack().rotateAround(tilt, (float) hip.x, (float) hip.y, (float) hip.z);
    }
}
