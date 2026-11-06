package com.jnhprojects.mcheli.workshop;

import com.jnhprojects.mcheli.ContentReload;
import com.jnhprojects.mcheli.MCHeli;
import com.jnhprojects.mcheli.content.aircraft.AircraftDefinition;
import com.jnhprojects.mcheli.entity.AircraftEntity;
import com.jnhprojects.mcheli.registry.ModEntities;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Flying a vehicle straight from the workshop. The pack was just saved on the machine this server
 * runs on, so loading it again is enough. That only holds for the owner of a single player or LAN
 * game: a dedicated server does not get a pack until an operator approves it, and then it is no
 * longer a test, so the screen does not offer it there and the server refuses it too.
 */
public final class TestFlight {
    /** How far up to look for room when the vehicle does not fit where the player stands. */
    private static final int MAX_RISE = 48;

    private TestFlight() {
    }

    public static void start(ServerPlayer player, String vehicle) {
        MinecraftServer server = player.level().getServer();
        if (!server.isSingleplayerOwner(player.getGameProfile())) {
            player.displayClientMessage(Component.translatable("message.mcheli.workshop.test_flight_owner_only"), true);
            return;
        }
        if (player.isPassenger()) {
            player.displayClientMessage(Component.translatable("message.mcheli.workshop.test_flight_riding"), true);
            return;
        }
        ContentReload.reload(server);
        AircraftDefinition def = MCHeli.content().any(vehicle);
        if (def == null) {
            player.sendSystemMessage(Component.translatable("message.mcheli.workshop.test_flight_no_vehicle", vehicle));
            MCHeli.packs().reportProblems(player::sendSystemMessage);
            return;
        }
        ServerLevel level = player.serverLevel();
        AircraftEntity aircraft = ModEntities.typeFor(def).create(level);
        if (aircraft == null) {
            return;
        }
        aircraft.setType(def);
        Vec3 start = player.position();
        for (int rise = 0; rise <= MAX_RISE; rise++) {
            aircraft.moveTo(start.x, start.y + rise, start.z, player.getYRot(), 0.0F);
            if (level.noCollision(aircraft, aircraft.getBoundingBox())) {
                break;
            }
        }
        aircraft.startTestFlight(start);
        level.addFreshEntity(aircraft);
        player.startRiding(aircraft, true);
    }
}
