package com.jnhprojects.mcheli.item;

import com.jnhprojects.mcheli.entity.AircraftEntity;
import com.jnhprojects.mcheli.entity.GltdEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import java.util.function.Supplier;

/**
 * A shoulder launched missile, {@code MCH_ItemLightWeaponBase}: the FIM-92 Stinger and the
 * FGM-148 Javelin. Held up it is a sight that locks onto targets; the attack key fires once
 * locked. It holds one missile, and its bar shows whether it is loaded: an empty launcher reloads
 * from a missile in the inventory after two seconds held but not raised.
 */
public class LauncherItem extends Item {
    /** The original's durability: 0 is loaded, the maximum is empty. */
    private static final int EMPTY = 10;

    private final String weapon;
    private final Supplier<Item> missile;

    /**
     * @param weapon  the weapon definition it fires, such as {@code fim92}
     * @param missile the item it reloads from
     */
    public LauncherItem(String weapon, Supplier<Item> missile, Properties properties) {
        super(properties.durability(EMPTY));
        this.weapon = weapon;
        this.missile = missile;
    }

    public String weapon() {
        return weapon;
    }

    public Item missile() {
        return missile.get();
    }

    public static boolean isLoaded(ItemStack stack) {
        return stack.getDamageValue() < stack.getMaxDamage();
    }

    public static void setLoaded(ItemStack stack, boolean loaded) {
        stack.setDamageValue(loaded ? 0 : stack.getMaxDamage());
    }

    /** Not from a vehicle seat or the GLTD, as in the original. */
    public static boolean canUse(Player player) {
        return !(player.getVehicle() instanceof AircraftEntity) && !(player.getVehicle() instanceof GltdEntity);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!canUse(player)) {
            return InteractionResultHolder.pass(player.getItemInHand(hand));
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }
}
