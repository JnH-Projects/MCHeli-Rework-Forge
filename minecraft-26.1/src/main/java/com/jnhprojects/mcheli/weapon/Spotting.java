package com.jnhprojects.mcheli.weapon;

import com.jnhprojects.mcheli.entity.AircraftEntity;
import com.jnhprojects.mcheli.entity.HelicopterEntity;
import com.jnhprojects.mcheli.entity.PlaneEntity;
import com.jnhprojects.mcheli.network.MarkerPacket;
import com.jnhprojects.mcheli.network.ModNetwork;
import com.jnhprojects.mcheli.team.TeamManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Spotting with the rangefinder, {@code MCH_Multiplay.spotEntity} and {@code markPoint}.
 *
 * <p>Spotted targets and marked points go to the spotter and their team only, as screen markers
 * the HUD draws ({@code MCH_GuiTargetMarker}); nobody else sees them.
 */
public final class Spotting {
    // The original's target filter bits.
    public static final int OTHERS = 1;
    public static final int MONSTERS = 2;
    public static final int PLAYERS = 4;
    public static final int GROUND_VEHICLES = 8;
    public static final int HELICOPTERS = 16;
    public static final int PLANES = 32;

    /** {@code RangeFinderSpotDist}. */
    public static final double RANGE = 400.0;
    /** {@code RangeFinderSpotTime} in ticks, for players and vehicles. */
    private static final int PLAYER_TIME = 15 * 20;
    /** Mobs stay marked for a minute. */
    private static final int MOB_TIME = 60 * 20;
    /** Half the width of the cone a spot covers, in degrees: the original's angle times two. */
    private static final double CONE = 40.0;

    private Spotting() {
    }

    static boolean matches(int filter, Entity entity) {
        int bit;
        if (entity instanceof PlaneEntity) {
            bit = PLANES;
        } else if (entity instanceof HelicopterEntity) {
            bit = HELICOPTERS;
        } else if (entity instanceof AircraftEntity) {
            bit = GROUND_VEHICLES;
        } else if (entity instanceof Player) {
            bit = PLAYERS;
        } else if (entity instanceof Enemy) {
            bit = MONSTERS;
        } else if (entity instanceof LivingEntity) {
            bit = OTHERS;
        } else {
            return false;
        }
        return (filter & bit) != 0;
    }

    /**
     * Marks what {@code spotter} sees within {@link #RANGE} and the cone around their view: never
     * a teammate, and only targets in plain sight.
     *
     * @return whether anything was spotted
     */
    public static boolean spot(ServerPlayer spotter, int filter) {
        ServerLevel level = spotter.level();
        Vec3 eye = spotter.getEyePosition();
        Vec3 look = spotter.getLookAngle();
        int ticks = (filter & ~(OTHERS | MONSTERS)) == 0 ? MOB_TIME : PLAYER_TIME;
        List<Integer> spotted = new ArrayList<>();
        for (Entity entity : level.getEntities(spotter, spotter.getBoundingBox().inflate(RANGE), e -> matches(filter, e))) {
            if (isFriendly(spotter, entity)) {
                continue;
            }
            Vec3 to = entity.getBoundingBox().getCenter().subtract(eye);
            double dist = to.length();
            if (dist < 1.0 || dist > RANGE || Math.toDegrees(Math.acos(to.scale(1.0 / dist).dot(look))) > CONE) {
                continue;
            }
            if (level.clip(new ClipContext(eye, entity.getBoundingBox().getCenter(), ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, spotter)).getType() == HitResult.Type.BLOCK) {
                continue;
            }
            spotted.add(entity.getId());
        }
        tellTeam(spotter, spotted, ticks);
        return !spotted.isEmpty();
    }

    /** Sends the spotted entities to {@code spotter} and their team, {@code MCH_PacketNotifySpotedEntity}. */
    public static void tellTeam(ServerPlayer spotter, List<Integer> entities, int ticks) {
        if (entities.isEmpty()) {
            return;
        }
        MarkerPacket packet = MarkerPacket.spotted(entities.stream().mapToInt(Integer::intValue).toArray(), ticks);
        for (ServerPlayer player : TeamManager.teammates(spotter)) {
            ModNetwork.sendToPlayer(player, packet);
        }
    }

    /** Teammates, and a vehicle crewed by one, are never spotted. */
    private static boolean isFriendly(ServerPlayer spotter, Entity entity) {
        if (entity instanceof AircraftEntity vehicle) {
            for (Entity crew : vehicle.getPassengers()) {
                if (crew.isAlliedTo(spotter)) {
                    return true;
                }
            }
            return false;
        }
        return entity.isAlliedTo(spotter);
    }

    /**
     * Tells the spotter and their team where they are looking, as block coordinates.
     *
     * @return whether the view ended on a block within range
     */
    public static boolean markPoint(ServerPlayer spotter) {
        return markPoint(spotter, spotter.getEyePosition(), RANGE);
    }

    /**
     * As above, looking from {@code from} along the spotter's view: a targeting pod marks from
     * the vehicle, out to {@code range}.
     */
    public static boolean markPoint(ServerPlayer spotter, Vec3 from, double range) {
        ServerLevel level = spotter.level();
        BlockHitResult hit = level.clip(new ClipContext(from, from.add(spotter.getLookAngle().scale(range)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, spotter));
        if (hit.getType() != HitResult.Type.BLOCK) {
            return false;
        }
        BlockPos pos = hit.getBlockPos();
        MarkerPacket marker = MarkerPacket.point(pos);
        Component message = Component.translatable("message.mcheli.mark_point", spotter.getDisplayName(), pos.getX(), pos.getY(), pos.getZ());
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            if (player == spotter || player.isAlliedTo(spotter)) {
                player.sendSystemMessage(message);
                ModNetwork.sendToPlayer(player, marker);
            }
        }
        return true;
    }
}
