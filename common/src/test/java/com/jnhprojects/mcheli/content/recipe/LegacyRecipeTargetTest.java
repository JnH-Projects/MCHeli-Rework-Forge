package com.jnhprojects.mcheli.content.recipe;

import com.jnhprojects.mcheli.content.ImportTarget;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The recipe format differs per Minecraft version; these need no original zip. */
class LegacyRecipeTargetTest {
    private static final LegacyRecipe.Vehicle AH_6X = new LegacyRecipe.Vehicle("helicopter", "ah-6x", 7);
    private static final String LINE = "\"RXR\",  X, mcheli:ah-6, R, redstone";

    @Test
    void modernRecipesUseStringsAndTheItemModel() {
        assertEquals("""
                {
                  "type": "minecraft:crafting_shaped",
                  "category": "misc",
                  "key": {
                    "R": "#c:dusts/redstone",
                    "X": {"type": "mcheli:vehicle", "neoforge:ingredient_type": "mcheli:vehicle", "fabric:type": "mcheli:vehicle", "vehicle": "ah-6"}
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
                """, LegacyRecipe.toJson(AH_6X, LINE, true, "mcheli:ah-6"::equals, ImportTarget.MODERN));
    }

    @Test
    void minecraft1211RecipesUseObjectsAndCustomModelData() {
        assertEquals("""
                {
                  "type": "minecraft:crafting_shaped",
                  "category": "misc",
                  "key": {
                    "R": {"tag": "c:dusts/redstone"},
                    "X": {"type": "mcheli:vehicle", "neoforge:ingredient_type": "mcheli:vehicle", "fabric:type": "mcheli:vehicle", "vehicle": "ah-6"}
                  },
                  "pattern": [
                    "RXR"
                  ],
                  "result": {
                    "id": "mcheli:helicopter",
                    "components": {
                      "mcheli:vehicle": "ah-6x",
                      "minecraft:custom_model_data": 7
                    }
                  }
                }
                """, LegacyRecipe.toJson(AH_6X, LINE, true, "mcheli:ah-6"::equals, ImportTarget.V1_21_1));
    }

    @Test
    void minecraft1211ShapelessIngredientsAreObjectsToo() {
        String json = LegacyRecipe.toJson(AH_6X, "iron_block,coal", false, name -> false, ImportTarget.V1_21_1);
        assertTrue(json.contains("    {\"tag\": \"c:storage_blocks/iron\"},\n    {\"item\": \"minecraft:coal\"}\n"), json);
    }
}
