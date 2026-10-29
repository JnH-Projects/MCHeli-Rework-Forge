package com.jnhprojects.mcheli.content;

import com.jnhprojects.mcheli.TestResources;
import com.jnhprojects.mcheli.content.recipe.LegacyRecipe;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The crafting recipes are generated from the definitions' {@code AddRecipe} lines when the
 * original zip is imported. This checks the whole set converts and spot checks the output: plain
 * ingredients, tags for item families and dye colours.
 */
class RecipeDataTest {
    private static LegacyRecipe.Generated generated;

    @BeforeAll
    static void generate() throws IOException {
        generated = LegacyRecipe.generate(ContentRegistry.load(TestResources.assets()));
    }

    @Test
    void everyRecipeConvertsExceptTheKnownGaps() {
        assertEquals(List.of("e767: mcheli:uav_station does not exist (yet)"), generated.skipped().stream().sorted().toList());
        assertEquals(108, generated.files().size());
    }

    @Test
    void ironFamiliesBecomeCommonTags() {
        assertEquals("""
                {
                  "type": "minecraft:crafting_shaped",
                  "category": "misc",
                  "key": {
                    "R": "#c:dusts/redstone",
                    "X": "#c:storage_blocks/iron",
                    "Y": "#c:ingots/iron"
                  },
                  "pattern": [
                    " Y ",
                    "YXY",
                    "RYR"
                  ],
                  "result": {
                    "id": "mcheli:helicopter",
                    "components": {
                      "mcheli:vehicle": "ah-6x",
                      "minecraft:item_model": "mcheli:ah-6x"
                    }
                  }
                }
                """, generated.files().get("ah-6x.json"));
    }

    @Test
    void aVehicleIngredientMatchesThatVehicleOnly() {
        assertEquals("""
                {
                  "type": "minecraft:crafting_shaped",
                  "category": "misc",
                  "key": {
                    "R": "#c:dusts/redstone",
                    "X": {"type": "mcheli:vehicle", "vehicle": "ah-6"}
                  },
                  "pattern": [
                    "RXR"
                  ],
                  "result": {
                    "id": "mcheli:helicopter",
                    "components": {
                      "mcheli:vehicle": "ah-6x",
                      "minecraft:item_model": "mcheli:ah-6x"
                    }
                  }
                }
                """, generated.files().get("ah-6x_2.json"));
    }

    @Test
    void dyesKeepTheirColour() {
        assertEquals("""
                {
                  "type": "minecraft:crafting_shaped",
                  "category": "misc",
                  "key": {
                    "B": "#c:dyes/white",
                    "G": "#minecraft:logs",
                    "L": "#c:dyes/lime",
                    "O": "#c:ingots/gold",
                    "R": "#c:dyes/red",
                    "X": "#c:storage_blocks/iron"
                  },
                  "pattern": [
                    "BXL",
                    "XXG",
                    "RXO"
                  ],
                  "result": {
                    "id": "mcheli:plane",
                    "components": {
                      "mcheli:vehicle": "macchi_mc72",
                      "minecraft:item_model": "mcheli:macchi_mc72"
                    }
                  }
                }
                """, generated.files().get("macchi_mc72.json"));
    }
}
