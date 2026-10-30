package com.jnhprojects.mcheli.item;

import com.jnhprojects.mcheli.MCHeli;
import com.jnhprojects.mcheli.ServerConfig;
import com.jnhprojects.mcheli.content.aircraft.AircraftDefinition;
import com.jnhprojects.mcheli.content.pack.ContentSource;
import com.jnhprojects.mcheli.entity.AircraftEntity;
import com.jnhprojects.mcheli.registry.ModDataComponents;
import com.jnhprojects.mcheli.registry.ModEntities;
import com.jnhprojects.mcheli.registry.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Places a vehicle, the same way a boat is placed.
 *
 * <p>There is one item per vehicle kind ({@code mcheli:helicopter}, {@code mcheli:plane} and so
 * on). Which vehicle a stack places is stored in its {@link ModDataComponents#VEHICLE} component,
 * and its look comes from the {@code item_model} component pointing at the model the import wrote
 * for that vehicle. Items are registered once at startup, so this is what lets definitions be
 * added or reloaded while the game runs.
 */
public class AircraftItem extends Item {
    private final String kind;

    public AircraftItem(String kind, Properties properties) {
        super(properties);
        this.kind = kind;
    }

    /** The vehicle kind this item holds, such as {@code helicopter}. */
    public String kind() {
        return kind;
    }

    /**
     * A stack that places {@code def}. Vehicles from the import show the item model the import
     * wrote for them, picked by the kind model's {@code custom_model_data} overrides (1.21.1 has no
     * {@code item_model} component); item models only load with the resources, so pack vehicles keep
     * their kind's own model.
     */
    public static ItemStack stackOf(AircraftDefinition def) {
        ItemStack stack = new ItemStack(ModItems.forKind(def.kind()));
        stack.set(ModDataComponents.VEHICLE.get(), def.name());
        if (def.pack().equals(ContentSource.ORIGINAL)) {
            MCHeli.legacyContent().iconIndex(def.name())
                    .ifPresent(index -> stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(index)));
        }
        return stack;
    }

    /** The definition name a stack places, or {@code null} if it is not a vehicle item or has none. */
    public static @Nullable String vehicleOf(ItemStack stack) {
        return stack.getItem() instanceof AircraftItem item ? item.definitionName(stack) : null;
    }

    protected @Nullable String definitionName(ItemStack stack) {
        return stack.get(ModDataComponents.VEHICLE.get());
    }

    @Override
    public Component getName(ItemStack stack) {
        String name = definitionName(stack);
        if (name == null) {
            return super.getName(stack);
        }
        AircraftDefinition def = MCHeli.content().any(name);
        // Vehicles added at runtime may bring no lang entry; their DisplayName stands in.
        return Component.translatableWithFallback("item.mcheli." + name, def != null ? def.displayName : name);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        HitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.ANY);
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
            return InteractionResultHolder.pass(stack);
        }

        String name = definitionName(stack);
        AircraftDefinition def = name != null ? MCHeli.content().any(name) : null;
        if (def == null) {
            if (!level.isClientSide()) {
                player.displayClientMessage(Component.translatable("message.mcheli.vehicle.unknown", String.valueOf(name)), true);
            }
            return InteractionResultHolder.fail(stack);
        }
        if (!level.isClientSide() && ServerConfig.isDisabled(def.name())) {
            player.displayClientMessage(Component.translatable("message.mcheli.vehicle.disabled", stack.getHoverName()), true);
            return InteractionResultHolder.fail(stack);
        }

        AircraftEntity vehicle = ModEntities.typeFor(def).create(level);
        if (vehicle == null) {
            return InteractionResultHolder.fail(stack);
        }
        Vec3 at = blockHit.getLocation();
        vehicle.setType(def);
        vehicle.moveTo(at.x, at.y, at.z, player.getYRot(), 0.0F);
        if (!level.noCollision(vehicle, vehicle.getBoundingBox())) {
            return InteractionResultHolder.fail(stack);
        }

        if (!level.isClientSide()) {
            level.addFreshEntity(vehicle);
            level.gameEvent(player, GameEvent.ENTITY_PLACE, at);
            stack.consume(1, player);
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
