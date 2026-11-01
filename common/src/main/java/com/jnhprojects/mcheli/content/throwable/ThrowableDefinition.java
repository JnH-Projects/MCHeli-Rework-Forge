package com.jnhprojects.mcheli.content.throwable;

import com.jnhprojects.mcheli.content.Definition;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.jnhprojects.mcheli.content.DefinitionValues.split;
import static com.jnhprojects.mcheli.content.DefinitionValues.toBool;
import static com.jnhprojects.mcheli.content.DefinitionValues.toFloat;
import static com.jnhprojects.mcheli.content.DefinitionValues.toInt;

/**
 * A hand thrown item from {@code assets/mcheli/throwable}, such as the M18 smoke grenades. Ported
 * from {@code MCH_ThrowableInfo}, keeping its keys, defaults and ranges. Times are read in seconds
 * and kept in ticks, as {@code checkData} did.
 */
public final class ThrowableDefinition extends Definition {
    public static final String FOLDER = "throwable";

    public String displayName;
    public int stackSize = 1;
    public float dispenseAcceleration = 1.0F;
    /** Explosion strength when the fuse runs out; 0 for a smoke grenade. */
    public int explosion;
    /** Ticks until it explodes, or starts to smoke. */
    public int timeFuse;
    /** Ticks until it disappears. */
    public int aliveTime = 200;
    /** How much of its speed it keeps when it bounces off a block. */
    public float bound = 0.2F;
    public float gravity;
    public float gravityInWater = -0.04F;
    public float smokeSize = 10.0F;
    /** Smoke puffs a tick once the fuse has run out. */
    public int smokeNum;
    public float smokeVelocityVertical = 1.0F;
    public float smokeVelocityHorizontal = 1.0F;
    /** Smoke colour, {@code 0xRRGGBB}; white by default. */
    public int smokeColor = 0xFFFFFF;
    public boolean shapedRecipe = true;
    public final List<String> recipes = new ArrayList<>();

    public ThrowableDefinition(String name) {
        super(name);
        this.displayName = name;
    }

    @Override
    protected void registerKeys(KeyTable keys) {
        keys.put("DisplayName", v -> displayName = v.trim());
        // The importer writes the lang files from these two; nothing to keep here.
        keys.put(v -> { }, "AddDisplayName", "ItemID");
        keys.put("StackSize", v -> stackSize = toInt(v, 1, 64));
        keys.put("DispenseAcceleration", v -> dispenseAcceleration = toFloat(v, 0.0F, 1000.0F));
        keys.put("Explosion", v -> explosion = toInt(v, 0, 50));
        keys.put("TimeFuse", v -> timeFuse = toInt(v, 0, 100000) * 20);
        keys.put("AliveTime", v -> aliveTime = toInt(v, 0, 1000000) * 20);
        keys.put("Bound", v -> bound = toFloat(v, 0.0F, 100000.0F));
        keys.put("Gravity", v -> gravity = toFloat(v, -50.0F, 50.0F));
        keys.put("GravityInWater", v -> gravityInWater = toFloat(v, -50.0F, 50.0F));
        keys.put("SmokeSize", v -> smokeSize = toFloat(v, 0.0F, 1000.0F));
        keys.put("SmokeNum", v -> smokeNum = toInt(v, 0, 1000));
        keys.put("SmokeVelocityVertical", v -> smokeVelocityVertical = toFloat(v, -100.0F, 100.0F));
        keys.put("SmokeVelocityHorizontal", v -> smokeVelocityHorizontal = toFloat(v, 0.0F, 1000.0F));
        keys.put("SmokeColor", v -> {
            String[] s = split(v);
            if (s.length >= 3) {
                smokeColor = toInt(s[0], 0, 255) << 16 | toInt(s[1], 0, 255) << 8 | toInt(s[2], 0, 255);
            }
        });
        keys.put("AddRecipe", v -> {
            shapedRecipe = true;
            recipes.add(v.toUpperCase(Locale.ROOT));
        });
        keys.put("AddShapelessRecipe", v -> {
            shapedRecipe = false;
            recipes.add(v.toUpperCase(Locale.ROOT));
        });
        // Read by the original but never used by a throwable: they only made sense for weapons.
        keys.put(v -> { }, "Power", "Acceleration", "AccelerationInWater", "DelayFuse", "Flaming", "SoundVolume",
                "SoundPitch", "ProximityFuseDist", "Accuracy", "Bomblet", "BombletDiff", "Particle", "DisableSmoke");
    }

    /** {@code true} for a grenade that only smokes, like the M18s. */
    public boolean smokes() {
        return explosion <= 0;
    }
}
