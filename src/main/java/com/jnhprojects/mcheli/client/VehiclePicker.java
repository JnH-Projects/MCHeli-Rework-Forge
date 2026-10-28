package com.jnhprojects.mcheli.client;

import com.jnhprojects.mcheli.entity.AircraftEntity;
import com.jnhprojects.mcheli.network.BoardVehiclePacket;
import com.jnhprojects.mcheli.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.InputEvent;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * Right-clicking anywhere on a vehicle's hull boards it.
 *
 * <p>Vanilla only picks an entity by its main hit box, which for most MCHeli vehicles is the
 * default two by 0.7 block box at their feet; the hull's real shape lives in its extra
 * {@code BoundingBox} lines. In the original those boxes were clickable too. Inflating the main box
 * instead would change how the vehicle collides with terrain, so the use key looks for them here.
 */
final class VehiclePicker {
    private VehiclePicker() {
    }

    static void init() {
        InputEvent.InteractionKeyMappingTriggered.UseItem.BUS.addListener(VehiclePicker::onUseItem);
    }

    private static boolean onUseItem(InputEvent.InteractionKeyMappingTriggered.UseItem event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || event.getHand() != InteractionHand.MAIN_HAND || player.getVehicle() instanceof AircraftEntity) {
            return false;
        }
        // Vanilla already handles a click on the main box.
        if (mc.hitResult instanceof EntityHitResult hit && hit.getEntity() instanceof AircraftEntity) {
            return false;
        }
        Hit hit = pick(player, mc.hitResult);
        if (hit == null) {
            return false;
        }
        ModNetwork.sendToServer(new BoardVehiclePacket(hit.vehicle.getId(), hit.location));
        return true;
    }

    private record Hit(AircraftEntity vehicle, Vec3 location) {
    }

    /** The nearest vehicle hull along the view, closer than the block or entity vanilla picked. */
    private static @Nullable Hit pick(Player player, @Nullable HitResult vanilla) {
        double range = player.entityInteractionRange();
        Vec3 eye = player.getEyePosition();
        Vec3 to = eye.add(player.getViewVector(1.0F).scale(range));
        double limit = vanilla != null && vanilla.getType() != HitResult.Type.MISS ? eye.distanceToSqr(vanilla.getLocation()) : range * range;
        Hit best = null;
        for (AircraftEntity vehicle : player.level().getEntitiesOfClass(AircraftEntity.class, new AABB(eye, to).inflate(16.0))) {
            Optional<Vec3> at = vehicle.clipHitBoxes(eye, to, 0.2);
            vehicle.consumeHitBoxFactor();
            if (at.isPresent() && eye.distanceToSqr(at.get()) < limit) {
                limit = eye.distanceToSqr(at.get());
                best = new Hit(vehicle, at.get());
            }
        }
        return best;
    }
}
