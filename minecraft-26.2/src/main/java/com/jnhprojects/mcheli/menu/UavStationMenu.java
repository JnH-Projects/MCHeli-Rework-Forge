package com.jnhprojects.mcheli.menu;

import com.jnhprojects.mcheli.entity.UavStationEntity;
import com.jnhprojects.mcheli.item.AircraftItem;
import com.jnhprojects.mcheli.registry.ModMenus;
import net.minecraft.core.BlockPos;
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
 * The UAV station screen, {@code MCH_ContainerUavStation}: one slot for the drone to put up, the
 * launch offset with its -10, -1, +1 and +10 buttons per axis, and Continue to take the last drone
 * again. The player sits at the station while it is open, so the client finds it as their vehicle.
 */
public class UavStationMenu extends AbstractContainerMenu {
    /** Button ids: axis times four plus the step, then Continue. */
    public static final int[] STEPS = {-10, -1, 1, 10};
    public static final int BUTTON_CONTINUE = 12;

    private static final int DATA_SIZE = 4;
    private static final int DATA_CAN_CONTINUE = 3;

    private final @Nullable UavStationEntity station;
    private final ContainerData data;

    /** The client side, from the open screen packet. */
    public UavStationMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, inventory.player.getVehicle() instanceof UavStationEntity s ? s : null,
                new SimpleContainer(1), new SimpleContainerData(DATA_SIZE));
    }

    /** The server side. */
    public UavStationMenu(int containerId, Inventory inventory, UavStationEntity station) {
        this(containerId, inventory, station, station.slot(), new ServerData(station));
    }

    private UavStationMenu(int containerId, Inventory inventory, @Nullable UavStationEntity station, Container slot, ContainerData data) {
        super(ModMenus.UAV_STATION.get(), containerId);
        this.station = station;
        this.data = data;
        checkContainerSize(slot, 1);
        addSlot(new Slot(slot, 0, 20, 20) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return AircraftItem.vehicleOf(stack) != null;
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        addStandardInventorySlots(inventory, 8, 84);
        addDataSlots(data);
    }

    public @Nullable UavStationEntity station() {
        return station;
    }

    /** The launch offset along {@code axis} (0 x, 1 y, 2 z), as the server last reported it. */
    public int offset(int axis) {
        return (short) data.get(axis);
    }

    public boolean canContinue() {
        return data.get(DATA_CAN_CONTINUE) != 0;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (station == null || !(player instanceof ServerPlayer server)) {
            return false;
        }
        if (id == BUTTON_CONTINUE) {
            return station.continueControl(server);
        }
        if (id < 0 || id >= 12) {
            return false;
        }
        int[] pos = {station.offset().getX(), station.offset().getY(), station.offset().getZ()};
        pos[id / 4] += STEPS[id % 4];
        station.setOffset(new BlockPos(pos[0], pos[1], pos[2]));
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return station != null && station.isAlive() && player.getVehicle() == station;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack moved = stack.copy();
        if (index == 0) {
            if (!moveItemStackTo(stack, 1, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (AircraftItem.vehicleOf(stack) == null || !moveItemStackTo(stack, 0, 1, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return moved;
    }

    /** Whatever is left in the slot goes back to the player, as with a crafting table. */
    @Override
    public void removed(Player player) {
        super.removed(player);
        if (station != null && !player.level().isClientSide()) {
            clearContainer(player, station.slot());
        }
    }

    private record ServerData(UavStationEntity station) implements ContainerData {
        @Override
        public int get(int index) {
            BlockPos o = station.offset();
            return switch (index) {
                case 0 -> o.getX();
                case 1 -> o.getY();
                case 2 -> o.getZ();
                default -> station.lastUav() != null ? 1 : 0;
            };
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
