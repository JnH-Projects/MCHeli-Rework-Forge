package com.jnhprojects.mcheli.registry;

import com.jnhprojects.mcheli.MCHeli;
import com.jnhprojects.mcheli.content.aircraft.AircraftDefinition;
import com.jnhprojects.mcheli.content.aircraft.HelicopterDefinition;
import com.jnhprojects.mcheli.content.aircraft.PlaneDefinition;
import com.jnhprojects.mcheli.content.aircraft.TankDefinition;
import com.jnhprojects.mcheli.entity.AircraftEntity;
import com.jnhprojects.mcheli.entity.GroundVehicleEntity;
import com.jnhprojects.mcheli.entity.HelicopterEntity;
import com.jnhprojects.mcheli.entity.PlaneEntity;
import com.jnhprojects.mcheli.entity.TankEntity;
import com.jnhprojects.mcheli.entity.projectile.A10Entity;
import com.jnhprojects.mcheli.entity.projectile.AAMissileEntity;
import com.jnhprojects.mcheli.entity.projectile.ASMissileEntity;
import com.jnhprojects.mcheli.entity.projectile.ATMissileEntity;
import com.jnhprojects.mcheli.entity.projectile.BombEntity;
import com.jnhprojects.mcheli.entity.projectile.BulletEntity;
import com.jnhprojects.mcheli.entity.projectile.DispensedItemEntity;
import com.jnhprojects.mcheli.entity.projectile.FlareEntity;
import com.jnhprojects.mcheli.entity.projectile.MarkerRocketEntity;
import com.jnhprojects.mcheli.entity.projectile.ProjectileEntity;
import com.jnhprojects.mcheli.entity.projectile.RocketEntity;
import com.jnhprojects.mcheli.entity.projectile.TorpedoEntity;
import com.jnhprojects.mcheli.entity.projectile.TvMissileEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * One entity type per vehicle kind. The concrete vehicle is stored on the entity, the same approach
 * the original took, so adding content never changes the entity registry.
 */
public final class ModEntities {
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MCHeli.MOD_ID);

    public static final RegistryObject<EntityType<HelicopterEntity>> HELICOPTER = register("helicopter", HelicopterEntity::new);
    public static final RegistryObject<EntityType<PlaneEntity>> PLANE = register("plane", PlaneEntity::new);
    public static final RegistryObject<EntityType<TankEntity>> TANK = register("tank", TankEntity::new);
    public static final RegistryObject<EntityType<GroundVehicleEntity>> VEHICLE = register("vehicle", GroundVehicleEntity::new);

    public static final RegistryObject<EntityType<BulletEntity>> BULLET = projectile("bullet", BulletEntity::new);
    public static final RegistryObject<EntityType<RocketEntity>> ROCKET = projectile("rocket", RocketEntity::new);
    public static final RegistryObject<EntityType<BombEntity>> BOMB = projectile("bomb", BombEntity::new);
    public static final RegistryObject<EntityType<AAMissileEntity>> AA_MISSILE = projectile("aa_missile", AAMissileEntity::new);
    public static final RegistryObject<EntityType<ASMissileEntity>> AS_MISSILE = projectile("as_missile", ASMissileEntity::new);
    public static final RegistryObject<EntityType<ATMissileEntity>> AT_MISSILE = projectile("at_missile", ATMissileEntity::new);
    public static final RegistryObject<EntityType<TvMissileEntity>> TV_MISSILE = projectile("tv_missile", TvMissileEntity::new);
    public static final RegistryObject<EntityType<TorpedoEntity>> TORPEDO = projectile("torpedo", TorpedoEntity::new);
    public static final RegistryObject<EntityType<MarkerRocketEntity>> MARKER_ROCKET = projectile("marker_rocket", MarkerRocketEntity::new);
    public static final RegistryObject<EntityType<DispensedItemEntity>> DISPENSED_ITEM = projectile("dispensed_item", DispensedItemEntity::new);
    public static final RegistryObject<EntityType<FlareEntity>> FLARE = ENTITY_TYPES.register("flare", () -> EntityType.Builder
            .<FlareEntity>of(FlareEntity::new, MobCategory.MISC)
            .sized(1.0F, 1.0F)
            .clientTrackingRange(16)
            .updateInterval(2)
            .fireImmune()
            .noSave()
            .build(ENTITY_TYPES.key("flare")));
    public static final RegistryObject<EntityType<A10Entity>> A10 = ENTITY_TYPES.register("a10", () -> EntityType.Builder
            .<A10Entity>of(A10Entity::new, MobCategory.MISC)
            .sized(5.0F, 3.0F)
            .clientTrackingRange(32)
            .updateInterval(1)
            .fireImmune()
            .noSave()
            .build(ENTITY_TYPES.key("a10")));

    private ModEntities() {
    }

    /** Projectiles are a metre cube like the original's, and never saved. */
    private static <T extends ProjectileEntity> RegistryObject<EntityType<T>> projectile(String name, EntityType.EntityFactory<T> factory) {
        return ENTITY_TYPES.register(name, () -> EntityType.Builder.of(factory, MobCategory.MISC)
                .sized(1.0F, 1.0F)
                .clientTrackingRange(16)
                .updateInterval(1)
                .fireImmune()
                .noSave()
                .build(ENTITY_TYPES.key(name)));
    }

    private static <T extends AircraftEntity> RegistryObject<EntityType<T>> register(String name, EntityType.EntityFactory<T> factory) {
        return ENTITY_TYPES.register(name, () -> EntityType.Builder.of(factory, MobCategory.MISC)
                .sized(2.0F, 0.7F)
                .clientTrackingRange(16)
                .updateInterval(1)
                .fireImmune()
                .build(ENTITY_TYPES.key(name)));
    }

    /** The entity type that hosts a definition of this kind. */
    public static EntityType<? extends AircraftEntity> typeFor(AircraftDefinition def) {
        if (def instanceof HelicopterDefinition) {
            return HELICOPTER.get();
        }
        if (def instanceof PlaneDefinition) {
            return PLANE.get();
        }
        if (def instanceof TankDefinition) {
            return TANK.get();
        }
        return VEHICLE.get();
    }

    public static void register(BusGroup modBus) {
        ENTITY_TYPES.register(modBus);
    }
}
