package com.jnhprojects.mcheli.menu;

import com.jnhprojects.mcheli.content.aircraft.AircraftDefinition;
import com.jnhprojects.mcheli.entity.AircraftEntity;
import com.jnhprojects.mcheli.entity.VehicleInventory;
import com.jnhprojects.mcheli.item.FuelItem;
import com.jnhprojects.mcheli.item.ParachuteItem;
import com.jnhprojects.mcheli.registry.ModMenus;
import com.jnhprojects.mcheli.weapon.Resupply;
import com.jnhprojects.mcheli.weapon.VehicleWeapons;
import com.jnhprojects.mcheli.weapon.WeaponSet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * The vehicle screen, {@code MCH_AircraftGuiContainer}: fuel slots, a parachute slot per ejection
 * seat, reloading the weapons from the crew member's own items and the way into the cargo chest.
 *
 * <p>Only crew open it, so the client finds the vehicle as the one its player rides. The weapon
 * rounds and which weapons can be reloaded right now come from the server through data slots,
 * since a client only hears about the ammunition of the seat it sits in.
 */
public class VehicleMenu extends AbstractContainerMenu {
    /** Weapons the screen can show; more than any vehicle in the original has. */
    public static final int MAX_WEAPONS = 32;
    /** Button ids from zero up reload that weapon; this one opens the cargo chest. */
    public static final int BUTTON_CARGO = 1000;

    public static final int FUEL_X = 8;
    public static final int FUEL_Y = 30;
    public static final int PARACHUTE_X = 8;
    public static final int PARACHUTE_Y = 66;
    public static final int INVENTORY_Y = 102;

    private static final int DATA_RELOADABLE = MAX_WEAPONS;
    private static final int DATA_SIZE = MAX_WEAPONS + 2;

    private final @Nullable AircraftEntity vehicle;
    private final Container equipment;
    private final ContainerData data;
    private final int parachuteSlots;
    private boolean supplied;

    /** The client side, from the open screen packet. */
    public VehicleMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, inventory.player.getVehicle() instanceof AircraftEntity v ? v : null,
                new SimpleContainer(VehicleInventory.EQUIPMENT_SLOTS), new SimpleContainerData(DATA_SIZE));
    }

    /** The server side. */
    public VehicleMenu(int containerId, Inventory inventory, AircraftEntity vehicle) {
        this(containerId, inventory, vehicle, vehicle.inventory().equipment(), new ServerData(vehicle, inventory.player));
    }

    private VehicleMenu(int containerId, Inventory inventory, @Nullable AircraftEntity vehicle, Container equipment, ContainerData data) {
        super(ModMenus.VEHICLE.get(), containerId);
        this.vehicle = vehicle;
        this.equipment = equipment;
        this.data = data;
        AircraftDefinition def = vehicle != null ? vehicle.definition() : null;
        parachuteSlots = def != null ? VehicleInventory.parachuteSlots(def) : 0;
        checkContainerSize(equipment, VehicleInventory.EQUIPMENT_SLOTS);

        for (int i = 0; i < VehicleInventory.FUEL_SLOTS; i++) {
            addSlot(new Slot(equipment, i, FUEL_X + i * 18, FUEL_Y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return stack.getItem() instanceof FuelItem;
                }
            });
        }
        for (int i = 0; i < VehicleInventory.PARACHUTE_SLOTS; i++) {
            boolean active = i < parachuteSlots;
            addSlot(new Slot(equipment, VehicleInventory.FUEL_SLOTS + i, PARACHUTE_X + i * 18, PARACHUTE_Y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return active && stack.getItem() instanceof ParachuteItem;
                }

                @Override
                public boolean isActive() {
                    return active;
                }
            });
        }
        addStandardInventorySlots(inventory, 8, INVENTORY_Y);
        addDataSlots(data);
    }

    public @Nullable AircraftEntity vehicle() {
        return vehicle;
    }

    /** Parachute slots in use: one per ejection seat. */
    public int parachuteSlots() {
        return parachuteSlots;
    }

    /** Magazine and reserve of a weapon together, as the server last reported them. */
    public int rounds(int weapon) {
        return weapon >= 0 && weapon < MAX_WEAPONS ? data.get(weapon) : 0;
    }

    /** Whether the server would reload this weapon now: the vehicle is down and the items are there. */
    public boolean canReload(int weapon) {
        if (weapon < 0 || weapon >= MAX_WEAPONS) {
            return false;
        }
        int bits = (data.get(DATA_RELOADABLE) & 0xFFFF) | (data.get(DATA_RELOADABLE + 1) & 0xFFFF) << 16;
        return (bits & 1 << weapon) != 0;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (vehicle == null || !(player instanceof ServerPlayer server)) {
            return false;
        }
        if (id == BUTTON_CARGO) {
            return vehicle.openCargo(server);
        }
        if (id >= 0 && id < MAX_WEAPONS && Resupply.supply(server, vehicle, id)) {
            supplied = true;
            return true;
        }
        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        return vehicle != null && equipment.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack moved = stack.copy();
        int equipmentEnd = VehicleInventory.EQUIPMENT_SLOTS;
        if (index < equipmentEnd) {
            if (!moveItemStackTo(stack, equipmentEnd, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.getItem() instanceof FuelItem) {
            if (!moveItemStackTo(stack, 0, VehicleInventory.FUEL_SLOTS, false)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.getItem() instanceof ParachuteItem && parachuteSlots > 0) {
            int first = VehicleInventory.FUEL_SLOTS;
            if (!moveItemStackTo(stack, first, first + parachuteSlots, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return moved;
    }

    /** As in the original, closing the screen after a resupply loads every magazine from its reserve. */
    @Override
    public void removed(Player player) {
        super.removed(player);
        if (supplied && vehicle != null && vehicle.weapons() != null && !player.level().isClientSide()) {
            vehicle.weapons().reloadAll();
        }
    }

    /** Rounds per weapon and the reload bits, worked out on the server whenever the menu syncs. */
    private record ServerData(AircraftEntity vehicle, Player player) implements ContainerData {
        @Override
        public int get(int index) {
            VehicleWeapons weapons = vehicle.weapons();
            if (weapons == null) {
                return 0;
            }
            if (index < MAX_WEAPONS) {
                WeaponSet set = weapons.set(index);
                // Data slots travel as shorts.
                return set != null ? Math.min(set.ammo() + set.restAmmo(), Short.MAX_VALUE) : 0;
            }
            int bits = 0;
            if (player instanceof ServerPlayer server) {
                for (int i = 0; i < Math.min(weapons.sets().size(), MAX_WEAPONS); i++) {
                    if (Resupply.canSupply(server, vehicle, i)) {
                        bits |= 1 << i;
                    }
                }
            }
            return index == DATA_RELOADABLE ? bits & 0xFFFF : bits >>> 16;
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return DATA_SIZE;
        }
    }
}
