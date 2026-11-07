package com.jnhprojects.mcheli.network;

import com.jnhprojects.mcheli.MCHeli;
import com.jnhprojects.mcheli.ModSounds;
import com.jnhprojects.mcheli.content.weapon.WeaponDefinition;
import com.jnhprojects.mcheli.content.weapon.WeaponType;
import com.jnhprojects.mcheli.entity.projectile.ATMissileEntity;
import com.jnhprojects.mcheli.entity.projectile.ProjectileEntity;
import com.jnhprojects.mcheli.item.LauncherItem;
import com.jnhprojects.mcheli.registry.ModEntities;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * The player with a launcher in hand, {@code MCH_PacketLightWeaponPlayerControl}: fire at the
 * locked target, or finish a reload. The client locks and times the reload; the server checks
 * the launcher, the target and the missile in the inventory.
 *
 * @param target the locked entity, for {@link Action#FIRE}
 * @param mode   the Javelin's attack mode: 0 direct, 1 top attack
 */
public record LauncherPacket(Action action, int target, int mode) implements CustomPacketPayload {
    public static final Type<LauncherPacket> TYPE = ModNetwork.type("launcher");
    public static final StreamCodec<RegistryFriendlyByteBuf, LauncherPacket> STREAM_CODEC = StreamCodec.ofMember(LauncherPacket::encode, LauncherPacket::decode);

    /** Locks further than this are refused, with some slack over the lock range for lag. */
    private static final double MAX_TARGET_DISTANCE = 260.0;

    public enum Action {
        FIRE,
        RELOAD
    }

    public static void encode(LauncherPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.action.ordinal());
        buf.writeVarInt(msg.target);
        buf.writeVarInt(msg.mode);
    }

    public static LauncherPacket decode(FriendlyByteBuf buf) {
        int ordinal = buf.readVarInt();
        Action[] actions = Action.values();
        if (ordinal < 0 || ordinal >= actions.length) {
            throw new IllegalArgumentException("Unknown launcher action " + ordinal);
        }
        return new LauncherPacket(actions[ordinal], buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(LauncherPacket msg, ServerPlayer sender) {
        ItemStack stack = sender == null ? ItemStack.EMPTY : sender.getItemInHand(InteractionHand.MAIN_HAND);
        if (!(stack.getItem() instanceof LauncherItem launcher) || !LauncherItem.canUse(sender)) {
            return;
        }
        switch (msg.action) {
            case FIRE -> fire(sender, stack, launcher, msg.target, msg.mode);
            case RELOAD -> reload(sender, stack, launcher);
        }
    }

    private static void fire(ServerPlayer player, ItemStack stack, LauncherItem launcher, int targetId, int mode) {
        WeaponDefinition def = MCHeli.content().weapon(launcher.weapon());
        ServerLevel level = player.serverLevel();
        Entity target = level.getEntity(targetId);
        if (def == null || !LauncherItem.isLoaded(stack) || target == null || target.isRemoved()
                || target.distanceToSqr(player) > MAX_TARGET_DISTANCE * MAX_TARGET_DISTANCE) {
            return;
        }
        ProjectileEntity missile = def.type == WeaponType.AT_MISSILE
                ? ModEntities.AT_MISSILE.get().create(level, EntitySpawnReason.TRIGGERED)
                : ModEntities.AA_MISSILE.get().create(level, EntitySpawnReason.TRIGGERED);
        if (missile == null) {
            return;
        }
        float yaw = player.getYRot();
        float pitch = player.getXRot();
        Vec3 dir = Vec3.directionFromRotation(pitch, yaw);
        Vec3 at = player.getEyePosition().add(dir);
        missile.launch(at.x, at.y, at.z, dir.x, dir.y, dir.z, yaw, pitch, def.acceleration);
        missile.setWeapon(launcher.weapon());
        missile.setParameters(def, player, player);
        missile.setTarget(target);
        if (missile instanceof ATMissileEntity atMissile) {
            atMissile.guidanceType = Math.clamp(mode, 0, Math.max(def.modeNum - 1, 0));
        }
        level.addFreshEntity(missile);
        if (def.sound != null) {
            ModSounds.playAt(player, def.sound, def.soundVolume, def.soundPitch);
        }
        LauncherItem.setLoaded(stack, false);
    }

    private static void reload(ServerPlayer player, ItemStack stack, LauncherItem launcher) {
        if (LauncherItem.isLoaded(stack)) {
            return;
        }
        if (!player.getAbilities().instabuild) {
            int slot = player.getInventory().findSlotMatchingItem(new ItemStack(launcher.missile()));
            if (slot < 0) {
                return;
            }
            player.getInventory().removeItem(slot, 1);
        }
        LauncherItem.setLoaded(stack, true);
    }

    @Override
    public Type<LauncherPacket> type() {
        return TYPE;
    }
}
