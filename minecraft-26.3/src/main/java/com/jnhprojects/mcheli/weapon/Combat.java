package com.jnhprojects.mcheli.weapon;

import com.jnhprojects.mcheli.content.weapon.WeaponDefinition;
import com.jnhprojects.mcheli.entity.GroundVehicleEntity;
import com.jnhprojects.mcheli.entity.HelicopterEntity;
import com.jnhprojects.mcheli.entity.PlaneEntity;
import com.jnhprojects.mcheli.entity.TankEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** Damage rules shared by bullets and explosions. */
public final class Combat {
    private Combat() {
    }

    /** The {@code DamageFactor} kind a target belongs to, or {@code null} for anything else. */
    public static @Nullable String kind(Entity entity) {
        return switch (entity) {
            case Player p -> "player";
            case HelicopterEntity h -> "helicopter";
            case PlaneEntity p -> "plane";
            case TankEntity t -> "tank";
            case GroundVehicleEntity v -> "vehicle";
            default -> null;
        };
    }

    public static float factor(@Nullable WeaponDefinition def, Entity target) {
        String kind = kind(target);
        return def != null && kind != null ? def.damageFactor(kind) : 1.0F;
    }

    /**
     * Deals weapon damage. The original scaled every living target's invulnerability window by
     * {@code HurtResistantTime}, zero by default, so each round of a burst lands.
     */
    public static boolean hurt(Entity target, DamageSource source, float damage, @Nullable WeaponDefinition def) {
        if (!(target.level() instanceof ServerLevel level)) {
            return false;
        }
        if (target instanceof LivingEntity living) {
            living.setInvulnerableTime(0);
        }
        return target.hurtServer(level, source, damage * factor(def, target));
    }

    /** The convention tag every loader fills, so modded panes count too. */
    private static final TagKey<Block> GLASS_PANES = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("c", "glass_panes"));

    /**
     * {@code BulletBreakableBlocks} with its default list: panes, grass, flowers, vines, wheat,
     * sugar cane and lily pads, which a round shoots through.
     */
    public static boolean isBulletBreakable(BlockState state) {
        return state.is(GLASS_PANES)
                || state.is(BlockTags.FLOWERS)
                || state.is(Blocks.SHORT_GRASS) || state.is(Blocks.TALL_GRASS)
                || state.is(Blocks.FERN) || state.is(Blocks.LARGE_FERN)
                || state.is(Blocks.VINE) || state.is(Blocks.WHEAT)
                || state.is(Blocks.SUGAR_CANE) || state.is(Blocks.LILY_PAD);
    }
}
