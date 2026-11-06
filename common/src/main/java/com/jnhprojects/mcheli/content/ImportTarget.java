package com.jnhprojects.mcheli.content;

/**
 * The pack layout {@link LegacyImporter} writes. Resource and data formats changed between the
 * supported Minecraft versions, so each version's module asks for its own.
 */
public enum ImportTarget {
    /**
     * Minecraft 26.x: every vehicle's item model has an item model definition, a crafted stack names
     * it in the {@code item_model} component, and recipe ingredients are plain ids and {@code #tags}.
     */
    MODERN,
    /**
     * Minecraft 1.21.1: there is no {@code item_model} component, so each kind's item model picks
     * the vehicle's model by {@code custom_model_data} (see {@link LegacyImporter#ICONS_FILE}), and
     * recipe ingredients are {@code {"item": ...}} and {@code {"tag": ...}} objects.
     */
    V1_21_1,
    /**
     * Minecraft 1.20.6: the 1.21.1 layout, except that data folders still have their plural names.
     */
    V1_20_6;

    /** The folder under {@code data/<namespace>} that holds recipes on this target. */
    public String recipeFolder() {
        return this == V1_20_6 ? "recipes" : "recipe";
    }
}
