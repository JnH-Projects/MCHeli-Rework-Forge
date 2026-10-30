package com.jnhprojects.mcheli.weapon;

import com.jnhprojects.mcheli.content.weapon.RoundItem;
import com.jnhprojects.mcheli.content.weapon.WeaponDefinition;
import com.jnhprojects.mcheli.entity.AircraftEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Resupplying a weapon from the crew member's inventory, the {@code supplyAmmo} path of the
 * original. There the vehicle's inventory screen did it; until that screen exists a key does.
 * One supply costs the weapon's {@code Item} lines and adds {@code SuppliedNum} rounds.
 */
public final class Resupply {
    private Resupply() {
    }

    public static boolean fromInventory(ServerPlayer player, AircraftEntity vehicle) {
        VehicleWeapons weapons = vehicle.weapons();
        int seat = vehicle.seatOf(player);
        WeaponSet set = weapons != null ? weapons.current(seat) : null;
        if (set == null || vehicle.isDestroyed()) {
            return false;
        }
        WeaponDefinition w = set.definition();
        if (set.ammo() + set.restAmmo() >= set.maxAmmo()) {
            return false;
        }
        if (!player.getAbilities().instabuild) {
            if (!canPay(player.getInventory(), w)) {
                return false;
            }
            pay(player.getInventory(), w);
        }
        set.supply();
        if (set.ammo() <= 0) {
            set.reloadMagazine();
        }
        return true;
    }

    private static boolean canPay(Inventory inventory, WeaponDefinition w) {
        for (RoundItem cost : w.roundItems) {
            Item item = LegacyItems.item(cost.item(), cost.damage());
            if (item == null || inventory.countItem(item) < cost.count()) {
                return false;
            }
        }
        return true;
    }

    private static void pay(Inventory inventory, WeaponDefinition w) {
        for (RoundItem cost : w.roundItems) {
            Item item = LegacyItems.item(cost.item(), cost.damage());
            int left = cost.count();
            for (int i = 0; i < inventory.getContainerSize() && left > 0; i++) {
                ItemStack stack = inventory.getItem(i);
                if (!stack.is(item)) {
                    continue;
                }
                if (item == Items.WATER_BUCKET || item == Items.LAVA_BUCKET) {
                    // Buckets give their bucket back, one at a time.
                    inventory.setItem(i, new ItemStack(Items.BUCKET));
                    left--;
                } else {
                    int take = Math.min(left, stack.getCount());
                    stack.shrink(take);
                    left -= take;
                }
            }
        }
        inventory.setChanged();
    }
}
