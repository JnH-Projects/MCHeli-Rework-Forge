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
 * Resupplying a weapon from a crew member's own items, the {@code supplyAmmo} path of the
 * original's vehicle screen. One supply costs the weapon's {@code Item} lines and adds
 * {@code SuppliedNum} rounds; it only works while the vehicle stands on something.
 */
public final class Resupply {
    private Resupply() {
    }

    /** {@code canPlayerSupplyAmmo}: whether {@link #supply} would do anything right now. */
    public static boolean canSupply(ServerPlayer player, AircraftEntity vehicle, int weapon) {
        VehicleWeapons weapons = vehicle.weapons();
        WeaponSet set = weapons != null ? weapons.set(weapon) : null;
        if (set == null || vehicle.isDestroyed() || player.getVehicle() != vehicle || !vehicle.canSupply()) {
            return false;
        }
        if (set.ammo() + set.restAmmo() >= set.maxAmmo()) {
            return false;
        }
        return player.getAbilities().instabuild || canPay(player.getInventory(), set.definition());
    }

    /** Pays for and adds one supply of {@code weapon}. Server only. */
    public static boolean supply(ServerPlayer player, AircraftEntity vehicle, int weapon) {
        if (!canSupply(player, vehicle, weapon)) {
            return false;
        }
        WeaponSet set = vehicle.weapons().set(weapon);
        if (!player.getAbilities().instabuild) {
            pay(player.getInventory(), set.definition());
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
