package com.jnhprojects.mcheli.weapon;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

/**
 * Maps the 1.7.10 item names and damage values in the content files to today's items. Names
 * that still exist resolve directly; the few that were renamed or flattened are listed here.
 */
public final class LegacyItems {
    private static final String[] DYES = {
            "ink_sac", "red_dye", "green_dye", "cocoa_beans", "lapis_lazuli", "purple_dye", "cyan_dye", "light_gray_dye",
            "gray_dye", "pink_dye", "lime_dye", "yellow_dye", "light_blue_dye", "magenta_dye", "orange_dye", "bone_meal"};

    private LegacyItems() {
    }

    public static @Nullable Item item(String name, int damage) {
        if (name.isEmpty()) {
            return null;
        }
        String id = switch (name) {
            case "dye" -> DYES[Math.floorMod(damage, DYES.length)];
            case "boat" -> "oak_boat";
            case "reeds" -> "sugar_cane";
            case "wool" -> "white_wool";
            case "log" -> "oak_log";
            case "planks" -> "oak_planks";
            default -> name.startsWith("minecraft:") ? name.substring("minecraft:".length()) : name;
        };
        Identifier key = Identifier.tryParse(id.contains(":") ? id : "minecraft:" + id);
        if (key == null) {
            return null;
        }
        Item item = BuiltInRegistries.ITEM.getValue(key);
        return item == Items.AIR ? null : item;
    }
}
