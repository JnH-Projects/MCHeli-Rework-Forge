package com.jnhprojects.mcheli.content.recipe;

import java.util.Map;
import java.util.function.IntFunction;

import static java.util.Map.entry;

/**
 * The Minecraft 1.7.10 item names the bundled recipes use, mapped to 26.x ingredients.
 *
 * <p>Where the old item had become a family of blocks or items, a tag stands in, so any colour of
 * wool or kind of log works as it did; the common {@code c:} tags also let other mods' iron,
 * glass or dyes count. Damage values only mattered for dyes and wool, which became separate items
 * per colour, and for coal, where 1 was charcoal.
 */
final class LegacyItems {
    /** Dye colours by their 1.7.10 damage value: 0 ink sac, 4 lapis, 15 bone meal and so on. */
    private static final String[] DYE = {
            "black", "red", "green", "brown", "blue", "purple", "cyan", "light_gray",
            "gray", "pink", "lime", "yellow", "light_blue", "magenta", "orange", "white"};

    private static final Map<String, String> PLAIN = Map.ofEntries(
            entry("iron_block", "#c:storage_blocks/iron"),
            entry("iron_ingot", "#c:ingots/iron"),
            entry("gold_block", "#c:storage_blocks/gold"),
            entry("gold_ingot", "#c:ingots/gold"),
            entry("diamond_block", "#c:storage_blocks/diamond"),
            entry("diamond", "#c:gems/diamond"),
            entry("redstone_block", "#c:storage_blocks/redstone"),
            entry("redstone", "#c:dusts/redstone"),
            entry("coal_block", "#c:storage_blocks/coal"),
            entry("glass", "#c:glass_blocks/colorless"),
            entry("glass_pane", "#c:glass_panes/colorless"),
            entry("gunpowder", "#c:gunpowders"),
            entry("feather", "#c:feathers"),
            entry("fish", "#c:foods/raw_fish"),
            entry("chest", "#c:chests/wooden"),
            entry("water_bucket", "#c:buckets/water"),
            entry("stick", "#c:rods/wooden"),
            entry("log", "#minecraft:logs"),
            entry("planks", "#minecraft:planks"),
            entry("boat", "#minecraft:boats"),
            entry("iron_bars", "minecraft:iron_bars"),
            entry("dispenser", "minecraft:dispenser"),
            entry("furnace", "minecraft:furnace"),
            entry("tnt", "minecraft:tnt"),
            entry("piston", "minecraft:piston"),
            entry("paper", "minecraft:paper"),
            entry("heavy_weighted_pressure_plate", "minecraft:heavy_weighted_pressure_plate"),
            entry("comparator", "minecraft:comparator"),
            entry("anvil", "minecraft:anvil"),
            entry("wheat_seeds", "minecraft:wheat_seeds"),
            entry("stone_hoe", "minecraft:stone_hoe"),
            entry("stone", "minecraft:stone"),
            entry("redstone_lamp", "minecraft:redstone_lamp"),
            // Potions became one item with contents; any potion stands in for the regeneration one.
            entry("potion", "minecraft:potion"));

    private static final Map<String, IntFunction<String>> BY_DAMAGE = Map.of(
            "dye", damage -> damage >= 0 && damage < DYE.length ? "#c:dyes/" + DYE[damage] : null,
            // Plain wool was white; any colour will do now, the original never asked for one.
            "wool", damage -> damage == 0 ? "#minecraft:wool" : null,
            "coal", damage -> damage == 0 ? "minecraft:coal" : damage == 1 ? "minecraft:charcoal" : null);

    private LegacyItems() {
    }

    /** @return an item id or {@code #tag}, or {@code null} if the item has no 26.x counterpart */
    static String ingredient(String name, int damage) {
        IntFunction<String> byDamage = BY_DAMAGE.get(name);
        if (byDamage != null) {
            return byDamage.apply(damage);
        }
        return PLAIN.get(name);
    }
}
