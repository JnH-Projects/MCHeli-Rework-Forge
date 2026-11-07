package com.jnhprojects.mcheli.client.workshop;

import com.jnhprojects.mcheli.entity.AircraftEntity;
import com.jnhprojects.mcheli.network.ModNetwork;
import com.jnhprojects.mcheli.network.PackSubmitPacket;
import com.jnhprojects.mcheli.network.TestFlightPacket;
import net.minecraft.client.Minecraft;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;

/**
 * What the workshop does beyond its screens: takes the player to a test flight and back, and sends
 * a pack to the server. The screen stays alive during a flight, so the undo history and the view are
 * as they were when the pilot gets out.
 */
public final class WorkshopClient {
    /** If no vehicle has come after this long, the server turned the flight down and said why. */
    private static final int SPAWN_TIMEOUT_TICKS = 100;
    /** About 1.2 MB a second, so a download-sized pack does not crowd out the game's own packets. */
    private static final int SUBMIT_CHUNKS_PER_TICK = 2;

    private static @Nullable WorkshopScreen waiting;
    private static boolean airborne;
    private static int ticks;
    private static byte @Nullable [] submitting;
    private static int submitted;

    private WorkshopClient() {
    }

    static void startTestFlight(WorkshopScreen screen, String vehicle) {
        waiting = screen;
        airborne = false;
        ticks = 0;
        Minecraft.getInstance().gui.setScreen(null);
        ModNetwork.sendToServer(new TestFlightPacket(vehicle));
    }

    /** Starts sending a pack's zip to the server, a few chunks a tick. */
    static void submit(byte[] zip) {
        submitting = zip;
        submitted = 0;
    }

    /** How far along the zip being sent is, from 0 to 100, or -1 when none is. */
    static int submitProgress() {
        byte[] zip = submitting;
        return zip != null ? (int) (submitted * 100L / zip.length) : -1;
    }

    public static void tick() {
        tickSubmit();
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

    private static void tickSubmit() {
        byte[] zip = submitting;
        if (zip == null) {
            return;
        }
        for (int i = 0; i < SUBMIT_CHUNKS_PER_TICK && submitted < zip.length; i++) {
            int end = Math.min(submitted + PackSubmitPacket.CHUNK_SIZE, zip.length);
            ModNetwork.sendToServer(new PackSubmitPacket(submitted, zip.length, Arrays.copyOfRange(zip, submitted, end)));
            submitted = end;
        }
        if (submitted >= zip.length) {
            submitting = null;
        }
    }

    public static void loggedOut() {
        submitting = null;
        WorkshopScreen screen = waiting;
        waiting = null;
        if (screen != null) {
            screen.releaseAssets();
        }
    }
}
