package com.jnhprojects.mcheli.item;

import com.jnhprojects.mcheli.MCHeli;
import com.jnhprojects.mcheli.content.aircraft.AircraftDefinition;
import com.jnhprojects.mcheli.entity.AircraftEntity;
import com.jnhprojects.mcheli.registry.ModEntities;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Places the vehicle it was generated for, the same way a boat is placed. */
public class AircraftItem extends Item {
    private final String definitionName;

    public AircraftItem(String definitionName, Properties properties) {
        super(properties);
        this.definitionName = definitionName;
    }

    public String definitionName() {
        return definitionName;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        HitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.ANY);
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
            return InteractionResult.PASS;
        }

        AircraftDefinition def = MCHeli.content().any(definitionName);
        if (def == null) {
            return InteractionResult.FAIL;
        }

        AircraftEntity vehicle = ModEntities.typeFor(def).create(level, EntitySpawnReason.SPAWN_ITEM_USE);
        if (vehicle == null) {
            return InteractionResult.FAIL;
        }
        Vec3 at = blockHit.getLocation();
        vehicle.setType(def);
        vehicle.snapTo(at.x, at.y, at.z, player.getYRot(), 0.0F);
        if (!level.noCollision(vehicle, vehicle.getBoundingBox())) {
            return InteractionResult.FAIL;
        }

        if (!level.isClientSide()) {
            level.addFreshEntity(vehicle);
            level.gameEvent(player, GameEvent.ENTITY_PLACE, at);
            stack.consume(1, player);
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResult.SUCCESS;
    }
}
