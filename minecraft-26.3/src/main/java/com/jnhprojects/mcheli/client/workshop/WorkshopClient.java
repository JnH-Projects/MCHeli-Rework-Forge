package com.jnhprojects.mcheli.client.workshop;

import com.jnhprojects.mcheli.entity.AircraftEntity;
import com.jnhprojects.mcheli.network.ModNetwork;
import com.jnhprojects.mcheli.network.TestFlightPacket;
import net.minecraft.client.Minecraft;
import org.jspecify.annotations.Nullable;

/**
 * Takes the player from the editor to a test flight and back. The screen stays alive in between, so
 * the undo history and the view are as they were when the pilot gets out.
 */
public final class WorkshopClient {
    /** If no vehicle has come after this long, the server turned the flight down and said why. */
    private static final int SPAWN_TIMEOUT_TICKS = 100;

    private static @Nullable WorkshopScreen waiting;
    private static boolean airborne;
    private static int ticks;

    private WorkshopClient() {
    }

    static void startTestFlight(WorkshopScreen screen, String vehicle) {
        waiting = screen;
        airborne = false;
        ticks = 0;
        Minecraft.getInstance().gui.setScreen(null);
        ModNetwork.sendToServer(new TestFlightPacket(vehicle));
    }

    public static void tick() {
        WorkshopScreen screen = waiting;
        Minecraft mc = Minecraft.getInstance();
        if (screen == null || mc.player == null) {
            return;
        }
        boolean flying = mc.player.getVehicle() instanceof AircraftEntity;
        airborne |= flying;
        ticks++;
        if (airborne ? !flying : ticks > SPAWN_TIMEOUT_TICKS) {
            waiting = null;
            // Not over another screen the player has opened in flight, such as the pause menu.
            if (mc.gui.screen() == null) {
                mc.gui.setScreen(screen);
            }
        }
    }

    public static void loggedOut() {
        WorkshopScreen screen = waiting;
        waiting = null;
        if (screen != null) {
            screen.releaseAssets();
        }
    }
}
