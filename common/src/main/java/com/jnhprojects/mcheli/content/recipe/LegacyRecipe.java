package com.jnhprojects.mcheli.content.recipe;

import com.jnhprojects.mcheli.content.ContentRegistry;
import com.jnhprojects.mcheli.content.aircraft.AircraftDefinition;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Predicate;

/**
 * Turns an {@code AddRecipe} or {@code AddShapelessRecipe} line of a vehicle definition into a
 * Minecraft 26.x crafting recipe, following {@code MCH_ItemRecipe.addShapedRecipe}:
 * <pre>
 *   AddRecipe = [count,] "row", ["row", ["row",]] C, item[, damage], C, item[, damage], ...
 *   AddShapelessRecipe = item[, damage], item[, damage], ...
 * </pre>
 * Item names and damage values are those of Minecraft 1.7.10 and are mapped by
 * {@link LegacyItems}. Unlike the original, a key letter the pattern does not use is dropped
 * (26.x refuses such recipes), and a recipe naming an item that no longer exists is reported
 * instead of crashing the game.
 */
public final class LegacyRecipe {
    private LegacyRecipe() {
    }

    /**
     * @param result      the crafted item, for example {@code mcheli:ah-6}
     * @param modItems    which {@code mcheli:} item ids exist, for recipes that use another vehicle
     * @return the recipe as JSON
     * @throws InvalidRecipeException with the reason when the line cannot become a recipe
     */
    public static String toJson(String result, String line, boolean shaped, Predicate<String> modItems) {
        String[] s = line.trim().split("\\s*,\\s*");
        return shaped ? shaped(result, s, modItems) : shapeless(result, s, modItems);
    }

    private static String shaped(String result, String[] s, Predicate<String> modItems) {
        int idx = 0;
        int count = 1;
        if (s.length > 0 && isNumber(s[0])) {
            count = Math.max(Integer.parseInt(s[0]), 1);
            idx = 1;
        }
        List<String> rows = new ArrayList<>();
        while (idx < s.length && rows.size() < 3 && s[idx].length() >= 2 && s[idx].startsWith("\"") && s[idx].endsWith("\"")) {
            rows.add(s[idx].substring(1, s[idx].length() - 1).toUpperCase(Locale.ROOT));
            idx++;
        }
        if (rows.isEmpty()) {
            throw new InvalidRecipeException("no pattern rows");
        }
        int width = rows.getFirst().length();
        for (String row : rows) {
            if (row.length() != width || width == 0 || width > 3) {
                throw new InvalidRecipeException("pattern rows must be one to three characters wide and all the same");
            }
        }

        Map<Character, String> key = new TreeMap<>();
        while (idx < s.length) {
            String symbol = s[idx++];
            if (symbol.isEmpty()) {
                continue;
            }
            if (symbol.length() != 1 || !Character.isLetter(symbol.charAt(0))) {
                throw new InvalidRecipeException("'" + symbol + "' is not a key letter");
            }
            if (idx >= s.length) {
                throw new InvalidRecipeException("key " + symbol + " has no item");
            }
            String name = s[idx++];
            int damage = 0;
            if (idx < s.length && isNumber(s[idx])) {
                damage = Integer.parseInt(s[idx++]);
            }
            key.put(Character.toUpperCase(symbol.charAt(0)), ingredient(name, damage, modItems));
        }

        // 1.7.10 left a pattern letter the key does not define as an empty slot; two bundled
        // recipes rely on that. 26.x rejects it, so blank such letters out here.
        Set<Character> used = new TreeSet<>();
        for (int r = 0; r < rows.size(); r++) {
            StringBuilder row = new StringBuilder(rows.get(r));
            for (int i = 0; i < row.length(); i++) {
                char c = row.charAt(i);
                if (c != ' ' && !key.containsKey(c)) {
                    row.setCharAt(i, ' ');
                } else if (c != ' ') {
                    used.add(c);
                }
            }
            rows.set(r, row.toString());
        }
        if (used.isEmpty()) {
            throw new InvalidRecipeException("the pattern uses no defined key");
        }
        key.keySet().retainAll(used);

        StringBuilder json = new StringBuilder();
        json.append("{\n  \"type\": \"minecraft:crafting_shaped\",\n  \"category\": \"misc\",\n  \"key\": {\n");
        int n = 0;
        for (Map.Entry<Character, String> e : key.entrySet()) {
            json.append("    \"").append(e.getKey()).append("\": \"").append(e.getValue()).append('"')
                    .append(++n < key.size() ? ",\n" : "\n");
        }
        json.append("  },\n  \"pattern\": [\n");
        for (int i = 0; i < rows.size(); i++) {
            json.append("    \"").append(rows.get(i)).append('"').append(i + 1 < rows.size() ? ",\n" : "\n");
        }
        json.append("  ],\n");
        appendResult(json, result, count);
        return json.toString();
    }

    private static String shapeless(String result, String[] s, Predicate<String> modItems) {
        List<String> ingredients = new ArrayList<>();
        for (int idx = 0; idx < s.length; idx++) {
            if (s[idx].isEmpty()) {
                continue;
            }
            int damage = 0;
            String name = s[idx];
            if (idx + 1 < s.length && isNumber(s[idx + 1])) {
                damage = Integer.parseInt(s[++idx]);
            }
            ingredients.add(ingredient(name, damage, modItems));
        }
        if (ingredients.isEmpty() || ingredients.size() > 9) {
            throw new InvalidRecipeException("a shapeless recipe needs one to nine ingredients");
        }
        StringBuilder json = new StringBuilder();
        json.append("{\n  \"type\": \"minecraft:crafting_shapeless\",\n  \"category\": \"misc\",\n  \"ingredients\": [\n");
        for (int i = 0; i < ingredients.size(); i++) {
            json.append("    \"").append(ingredients.get(i)).append('"').append(i + 1 < ingredients.size() ? ",\n" : "\n");
        }
        json.append("  ],\n");
        appendResult(json, result, 1);
        return json.toString();
    }

    private static void appendResult(StringBuilder json, String result, int count) {
        json.append("  \"result\": {\n    \"id\": \"").append(result).append('"');
        if (count > 1) {
            json.append(",\n    \"count\": ").append(count);
        }
        json.append("\n  }\n}\n");
    }

    private static String ingredient(String name, int damage, Predicate<String> modItems) {
        String lower = name.trim().toLowerCase(Locale.ROOT);
        if (lower.startsWith("mcheli:")) {
            if (!modItems.test(lower)) {
                throw new InvalidRecipeException(lower + " does not exist (yet)");
            }
            return lower;
        }
        String mapped = LegacyItems.ingredient(lower, damage);
        if (mapped == null) {
            throw new InvalidRecipeException("no modern item for " + lower + (damage != 0 ? " with damage " + damage : ""));
        }
        return mapped;
    }

    private static boolean isNumber(String s) {
        if (s.isEmpty()) {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isDigit(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /** All recipe lines of a definition, keyed by the file name each becomes. */
    public static Map<String, String> fileNames(String vehicle, List<String> lines) {
        Map<String, String> out = new LinkedHashMap<>();
        for (int i = 0; i < lines.size(); i++) {
            out.put(i == 0 ? vehicle : vehicle + "_" + (i + 1), lines.get(i));
        }
        return out;
    }

    /**
     * Every recipe of every loaded vehicle.
     *
     * @return the JSON of each recipe keyed by its file name ({@code ah-6.json}), and the lines
     *         that could not become a recipe, as {@code "<file>: <reason>"}
     */
    public static Generated generate(ContentRegistry content) {
        Set<String> items = new TreeSet<>();
        for (AircraftDefinition def : content.all()) {
            items.add("mcheli:" + def.name());
        }

        Map<String, String> files = new TreeMap<>();
        List<String> skipped = new ArrayList<>();
        for (AircraftDefinition def : content.all()) {
            for (Map.Entry<String, String> e : fileNames(def.name(), def.recipes).entrySet()) {
                try {
                    files.put(e.getKey() + ".json", toJson("mcheli:" + def.name(), e.getValue(), def.shapedRecipe, items::contains));
                } catch (InvalidRecipeException ex) {
                    skipped.add(e.getKey() + ": " + ex.getMessage());
                }
            }
        }
        return new Generated(files, skipped);
    }

    public record Generated(Map<String, String> files, List<String> skipped) {
    }
}
