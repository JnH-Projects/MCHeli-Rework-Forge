package com.jnhprojects.mcheli.entity;

import com.jnhprojects.mcheli.ModSounds;
import com.jnhprojects.mcheli.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.LinearInterpolationHandler;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A cargo container, {@code MCH_EntityContainer}: a crate with a large chest's worth of room that
 * sits on the ground, floats, can be pushed around, and rides the racks of the vehicles that carry
 * {@code container}. Dropped from a rack high enough it comes down under a parachute.
 */
public class ContainerEntity extends Entity {
    /** What racks call it in their {@code AddRack} lines. */
    public static final String CARGO_NAME = "container";
    private static final int SLOTS = 54;
    /** Punches add up; the original broke it past 40 with 20 per point of damage. */
    private static final int BREAK_DAMAGE = 40;

    private final SimpleContainer items = new SimpleContainer(SLOTS);
    private int damageTaken;

    public ContainerEntity(EntityType<? extends ContainerEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() || isPassenger()) {
            return;
        }
        if (damageTaken > 0) {
            damageTaken--;
        }
        Vec3 m = getDeltaMovement();
        double my = m.y;
        if (isInWater() || isInLava()) {
            // Bobs up to the surface rather than sinking, as the original's boat physics did.
            my = (my < 0.0 ? my / 2.0 : my) + (isEyeInFluid(FluidTags.WATER) ? 0.04 : 0.007);
        } else {
            my -= 0.04;
        }
        double mx = m.x;
        double mz = m.z;
        if (onGround()) {
            mx *= 0.9;
            mz *= 0.9;
        }
        move(MoverType.SELF, new Vec3(mx, my, mz));
        setDeltaMovement(mx * 0.99, my * 0.95, mz * 0.99);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (player.isSecondaryUseActive()) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new SimpleMenuProvider((id, inventory, p) -> ChestMenu.sixRows(id, inventory, items),
                    Component.translatable("entity.mcheli.container")));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (isInvulnerableToBase(source) || isRemoved() || !(source.getEntity() instanceof Player player)) {
            // Only players take it apart; explosions and stray rounds leave the cargo alone.
            return false;
        }
        ModSounds.playAt(this, "hit", 1.0F, 1.3F);
        damageTaken += (int) (damage * 20.0F);
        boolean creative = player.getAbilities().instabuild;
        if (creative || damageTaken > BREAK_DAMAGE) {
            if (!creative) {
                spawnAtLocation(level, ModItems.CONTAINER.get());
            }
            Containers.dropContents(level, this, items);
            discard();
        }
        return true;
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean isPushable() {
        return !isPassenger();
    }

    @Override
    public boolean canBeCollidedWith(@Nullable Entity other) {
        return !isPassenger();
    }

    @Override
    public @Nullable ItemStack getPickResult() {
        return new ItemStack(ModItems.CONTAINER.get());
    }

    @Override
    protected InterpolationHandler createInterpolationHandler() {
        return LinearInterpolationHandler.create(this, 3);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        items.clearContent();
        ContainerHelper.loadAllItems(input, items.getItems());
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        ContainerHelper.saveAllItems(output, items.getItems(), false);
    }
}
