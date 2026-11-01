package com.jnhprojects.mcheli.team;

import com.jnhprojects.mcheli.entity.AircraftEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.TeamColor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * The team manager's actions, {@code MCH_Multiplay} and {@code MCH_MultiplayPacketHandler}: for
 * operators running a match. Teams are the vanilla scoreboard teams, so {@code /team} and the
 * player list see the same ones.
 */
public final class TeamManager {
    private TeamManager() {
    }

    /** Operators only, as the original checked the scoreboard command's permission. */
    public static boolean mayManage(Player player) {
        return player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }

    /** Switches the {@code pvp} game rule in every dimension and says which way it went. */
    public static void togglePvp(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        boolean pvp = !player.level().getGameRules().get(GameRules.PVP);
        for (ServerLevel level : server.getAllLevels()) {
            level.getGameRules().set(GameRules.PVP, pvp, server);
        }
        broadcast(server, Component.translatable(pvp ? "message.mcheli.team.pvp_on" : "message.mcheli.team.pvp_off"));
    }

    /** {@code shuffleTeam}: every player online goes into one of the teams in turn, in random order. */
    public static void shuffle(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        Scoreboard scoreboard = server.getScoreboard();
        List<PlayerTeam> teams = new ArrayList<>(scoreboard.getPlayerTeams());
        if (teams.isEmpty()) {
            player.sendOverlayMessage(Component.translatable("message.mcheli.team.no_teams"));
            return;
        }
        List<ServerPlayer> players = new ArrayList<>(server.getPlayerList().getPlayers());
        Collections.shuffle(players);
        Collections.shuffle(teams);
        for (int i = 0; i < players.size(); i++) {
            scoreboard.addPlayerToTeam(players.get(i).getScoreboardName(), teams.get(i % teams.size()));
        }
        broadcast(server, Component.translatable("message.mcheli.team.shuffled", teams.size()));
    }

    /** The original's create team screen: a name, a colour and friendly fire. */
    public static void create(ServerPlayer player, String name, int color, boolean friendlyFire) {
        Scoreboard scoreboard = player.level().getServer().getScoreboard();
        String trimmed = name.trim();
        if (trimmed.isEmpty() || trimmed.length() > 16 || trimmed.contains(" ") || scoreboard.getPlayerTeam(trimmed) != null) {
            player.sendOverlayMessage(Component.translatable("message.mcheli.team.bad_name", trimmed));
            return;
        }
        PlayerTeam team = scoreboard.addPlayerTeam(trimmed);
        team.setColor(Optional.of(TeamColor.VALUES.get(Math.floorMod(color, TeamColor.VALUES.size()))));
        team.setAllowFriendlyFire(friendlyFire);
        player.sendOverlayMessage(Component.translatable("message.mcheli.team.created", trimmed));
    }

    /** {@code jumpSpawnPoint}: every player back to where they would respawn. */
    public static void jumpToSpawn(ServerPlayer player) {
        for (ServerPlayer target : List.copyOf(player.level().getServer().getPlayerList().getPlayers())) {
            target.stopRiding();
            target.teleport(target.findRespawnPositionAndUseSpawnBlock(false, TeleportTransition.DO_NOTHING));
        }
    }

    /** {@code destoryAllAircraft}: every loaded vehicle is gone, its crew left standing. */
    public static void destroyAll(ServerPlayer player) {
        int count = 0;
        for (ServerLevel level : player.level().getServer().getAllLevels()) {
            for (AircraftEntity vehicle : level.getEntities(EntityTypeTest.forClass(AircraftEntity.class), e -> true)) {
                vehicle.ejectPassengers();
                vehicle.discard();
                count++;
            }
        }
        player.sendOverlayMessage(Component.translatable("message.mcheli.team.destroyed", count));
    }

    /** {@code entity} and everyone on its team. */
    public static List<ServerPlayer> teammates(ServerPlayer entity) {
        List<ServerPlayer> out = new ArrayList<>();
        for (ServerPlayer player : entity.level().getServer().getPlayerList().getPlayers()) {
            if (player == entity || player.isAlliedTo(entity)) {
                out.add(player);
            }
        }
        return out;
    }

    private static void broadcast(MinecraftServer server, Component message) {
        server.getPlayerList().broadcastSystemMessage(message, false);
    }
}
