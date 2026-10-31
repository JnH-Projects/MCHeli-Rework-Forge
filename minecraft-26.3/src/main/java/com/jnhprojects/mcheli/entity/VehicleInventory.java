package com.jnhprojects.mcheli.entity;

import com.jnhprojects.mcheli.content.aircraft.AircraftDefinition;
import com.jnhprojects.mcheli.item.FuelItem;
import com.jnhprojects.mcheli.item.ParachuteItem;
import com.jnhprojects.mcheli.supply.SupplyRules;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * What a vehicle carries besides its crew, {@code MCH_AircraftInventory} and the container half of
 * {@code W_EntityContainer}: the equipment slots of the vehicle screen (three fuel cans and a
 * parachute per ejection seat) and the cargo chest of vehicles with an {@code InventorySize}.
 *
 * <p>The cargo chest always has room for the largest size, 54 slots, and the screen shows as many
 * rows as the definition asks for. Items in rows a content reload took away are kept rather than
 * lost, and still drop when the vehicle is destroyed.
 */
public final class VehicleInventory {
    public static final int FUEL_SLOTS = 3;
    public static final int PARACHUTE_SLOTS = 2;
    public static final int EQUIPMENT_SLOTS = FUEL_SLOTS + PARACHUTE_SLOTS;
    public static final int CARGO_SLOTS = 54;
    /** How far from the vehicle's centre a player outside may keep a screen open, past its width. */
    private static final double REACH = 12.0;

    private final AircraftEntity vehicle;
    private final SimpleContainer equipment = new GuardedContainer(EQUIPMENT_SLOTS);
    private final SimpleContainer cargo = new GuardedContainer(CARGO_SLOTS);

    VehicleInventory(AircraftEntity vehicle) {
        this.vehicle = vehicle;
    }

    public SimpleContainer equipment() {
        return equipment;
    }

    public SimpleContainer cargo() {
        return cargo;
    }

    /** {@code isUseableByPlayer}: the crew, or someone standing next to the vehicle. */
    public boolean stillValid(Player player) {
        if (vehicle.isRemoved() || vehicle.definition() == null) {
            return false;
        }
        if (player.getVehicle() == vehicle) {
            return true;
        }
        double reach = REACH + vehicle.definition().bodyWidth / 2.0;
        return player.distanceToSqr(vehicle) <= reach * reach;
    }

    // Fuel

    /**
     * {@code updateFuel}: every half second, while the vehicle stands on something, each fuel can
     * in a fuel slot pours up to 100 units into the tank. Server only.
     *
     * @return whether any fuel went in
     */
    boolean pourFuelCans(int ticks) {
        if (ticks % SupplyRules.FUEL_SLOT_INTERVAL != 0 || !vehicle.canSupply()) {
            return false;
        }
        boolean poured = false;
        for (int slot = 0; slot < FUEL_SLOTS; slot++) {
            ItemStack can = equipment.getItem(slot);
            if (!(can.getItem() instanceof FuelItem)) {
                continue;
            }
            int amount = SupplyRules.fuelFromCan(vehicle.maxFuel() - vehicle.fuel(), FuelItem.contents(can));
            if (amount > 0) {
                vehicle.refuel(amount);
                can.setDamageValue(can.getDamageValue() + amount);
                poured = true;
            }
        }
        if (poured) {
            equipment.setChanged();
        }
        return poured;
    }

    // Parachutes

    /** Parachute slots the vehicle screen offers: one per ejection seat. */
    public static int parachuteSlots(AircraftDefinition def) {
        return Math.min(def.ejectionSeats(), PARACHUTE_SLOTS);
    }

    /**
     * {@code consumeParachute}: takes one parachute out of the parachute slots for a crew member
     * who bails out.
     *
     * @return false when there is none left
     */
    boolean takeParachute() {
        for (int slot = FUEL_SLOTS; slot < EQUIPMENT_SLOTS; slot++) {
            if (equipment.getItem(slot).getItem() instanceof ParachuteItem) {
                equipment.removeItem(slot, 1);
                return true;
            }
        }
        return false;
    }

    // Saving and dropping

    void save(ValueOutput output) {
        ContainerHelper.saveAllItems(output.child("Equipment"), equipment.getItems(), false);
        ContainerHelper.saveAllItems(output.child("Cargo"), cargo.getItems(), false);
    }

    void load(ValueInput input) {
        equipment.clearContent();
        cargo.clearContent();
        input.child("Equipment").ifPresent(in -> ContainerHelper.loadAllItems(in, equipment.getItems()));
        input.child("Cargo").ifPresent(in -> ContainerHelper.loadAllItems(in, cargo.getItems()));
    }

    /** {@code setDead}: everything aboard falls out where the vehicle was. Server only. */
    void dropAll() {
        Containers.dropContents(vehicle.level(), vehicle, equipment);
        Containers.dropContents(vehicle.level(), vehicle, cargo);
    }

    /** A container that only stays open for players {@link #stillValid(Player)} allows. */
    private final class GuardedContainer extends SimpleContainer {
        GuardedContainer(int size) {
            super(size);
        }

        @Override
        public boolean stillValid(Player player) {
            return VehicleInventory.this.stillValid(player);
        }
    }
}
