package com.jnhprojects.mcheli.weapon;

import com.jnhprojects.mcheli.MCHeli;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * Damage types defined in {@code data/mcheli/damage_type}. Bullets and missiles use their own type
 * so death messages name the gun instead of calling it a thrown snowball, which is what the
 * original's {@code causeThrownDamage} amounted to.
 */
public final class ModDamageTypes {
    public static final ResourceKey<DamageType> BULLET =
            ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.fromNamespaceAndPath(MCHeli.MOD_ID, "bullet"));

    private ModDamageTypes() {
    }

    public static DamageSource bullet(Level level, Entity projectile, @Nullable Entity owner) {
        return new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(BULLET), projectile, owner);
    }
}
