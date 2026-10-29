package com.jnhprojects.mcheli.network;

import com.jnhprojects.mcheli.entity.AircraftEntity;
import com.jnhprojects.mcheli.entity.PlaneEntity;
import com.jnhprojects.mcheli.weapon.Resupply;
import com.jnhprojects.mcheli.weapon.VehicleWeapons;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

/**
 * A one-off request from a crew member: lower the gear, fold the wings, change seats and so on.
 *
 * <p>The client only asks; the server checks that the sender sits in that vehicle, in a seat
 * allowed to do it, and then applies the same conditions the original checked before switching.
 * The result reaches every client through synched data.
 */
public record CrewActionPacket(int entityId, Action action) {

    public enum Action {
        /** Raise or lower the landing gear. Pilot only. */
        LANDING_GEAR,
        /** Fold or unfold hatches, wings or swept wings. Pilot only. */
        FOLD,
        /** Tilt the nozzles for vertical flight. Pilot only. */
        VTOL,
        /** Eject from seat zero or one. */
        EJECT,
        /** Move to the next free crew seat. */
        SWITCH_SEAT,
        /** Load nearby cargo onto the racks, or drive onto a carrier's rack. Pilot only. */
        RACK_LOAD,
        /** Drop the last cargo, or leave the rack this vehicle rides on. Pilot only. */
        RACK_UNLOAD,
        /** Select the next weapon this seat may use. */
        NEXT_WEAPON,
        /** Select the previous weapon this seat may use. */
        PREVIOUS_WEAPON,
        /** Cycle the fire mode of the selected weapon. */
        WEAPON_MODE,
        /** Release flares. Pilot and first crew seat. */
        FLARE,
        /** Pay the selected weapon's {@code Item} cost from the inventory for one resupply. */
        RESUPPLY,
        /** Hand the controls to the autopilot and man the weapons, or take them back. Pilot only. */
        GUNNER_MODE
    }

    public static void encode(CrewActionPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.entityId);
        buf.writeVarInt(msg.action.ordinal());
    }

    public static CrewActionPacket decode(FriendlyByteBuf buf) {
        int entityId = buf.readVarInt();
        int ordinal = buf.readVarInt();
        Action[] actions = Action.values();
        if (ordinal < 0 || ordinal >= actions.length) {
            throw new IllegalArgumentException("Unknown crew action " + ordinal);
        }
        return new CrewActionPacket(entityId, actions[ordinal]);
    }

    public static void handle(CrewActionPacket msg, CustomPayloadEvent.Context ctx) {
        ServerPlayer sender = ctx.getSender();
        if (sender == null || !(sender.getVehicle() instanceof AircraftEntity vehicle) || vehicle.getId() != msg.entityId) {
            return;
        }
        boolean pilot = vehicle.getControllingPassenger() == sender;
        switch (msg.action) {
            case LANDING_GEAR -> {
                if (pilot) {
                    vehicle.toggleLandingGear();
                }
            }
            case FOLD -> {
                if (pilot) {
                    vehicle.toggleFold();
                }
            }
            case VTOL -> {
                if (pilot && vehicle instanceof PlaneEntity plane) {
                    plane.toggleVtol();
                }
            }
            case GUNNER_MODE -> {
                if (pilot) {
                    vehicle.setGunnerMode(!vehicle.isGunnerMode());
                }
            }
            case EJECT -> vehicle.eject(sender);
            case SWITCH_SEAT -> vehicle.switchSeat(sender);
            case RACK_LOAD -> {
                if (pilot) {
                    if (!vehicle.boardNearbyRack()) {
                        vehicle.loadNearbyCargo();
                    }
                }
            }
            case RACK_UNLOAD -> {
                if (pilot) {
                    if (vehicle.isPassenger()) {
                        vehicle.leaveRack();
                    } else {
                        vehicle.unloadCargo();
                    }
                }
            }
            case NEXT_WEAPON, PREVIOUS_WEAPON -> {
                VehicleWeapons weapons = vehicle.weapons();
                int seat = vehicle.seatOf(sender);
                if (weapons != null && !vehicle.isDestroyed()) {
                    int next = weapons.next(seat, msg.action == Action.NEXT_WEAPON ? 1 : -1);
                    if (next >= 0) {
                        weapons.switchTo(sender, seat, next);
                    }
                }
            }
            case WEAPON_MODE -> {
                VehicleWeapons weapons = vehicle.weapons();
                if (weapons != null) {
                    weapons.switchMode(vehicle.seatOf(sender));
                }
            }
            case FLARE -> {
                VehicleWeapons weapons = vehicle.weapons();
                if (weapons != null && vehicle.seatOf(sender) <= 1) {
                    weapons.flares.use();
                }
            }
            case RESUPPLY -> Resupply.fromInventory(sender, vehicle);
        }
    }
}
