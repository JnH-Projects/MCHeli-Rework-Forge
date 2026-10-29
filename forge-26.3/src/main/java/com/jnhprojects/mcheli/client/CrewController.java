package com.jnhprojects.mcheli.client;

import com.jnhprojects.mcheli.MCHeli;
import com.jnhprojects.mcheli.client.hud.HudRenderer;
import com.jnhprojects.mcheli.content.Vector3;
import com.jnhprojects.mcheli.content.aircraft.AircraftDefinition;
import com.jnhprojects.mcheli.content.aircraft.CameraPosition;
import com.jnhprojects.mcheli.content.aircraft.SeatDefinition;
import com.jnhprojects.mcheli.content.weapon.WeaponType;
import com.jnhprojects.mcheli.entity.AircraftEntity;
import com.jnhprojects.mcheli.physics.AircraftRotation;
import com.jnhprojects.mcheli.entity.GroundVehicleEntity;
import com.jnhprojects.mcheli.entity.HelicopterEntity;
import com.jnhprojects.mcheli.entity.TankEntity;
import com.jnhprojects.mcheli.entity.projectile.TvMissileEntity;
import com.jnhprojects.mcheli.network.CrewActionPacket;
import com.jnhprojects.mcheli.network.ModNetwork;
import com.jnhprojects.mcheli.network.PilotStatePacket;
import com.jnhprojects.mcheli.weapon.VehicleWeapons;
import com.jnhprojects.mcheli.weapon.WeaponSet;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

/**
 * Turns local player input into vehicle control, for whichever seat the player occupies.
 *
 * <p>Keys are sampled once per client tick. The mouse is handled every frame, the way the
 * original did it: for helicopters and planes, vanilla first turns the player's view, then the
 * change is fed to the aircraft and the view is snapped back onto the aircraft's nose. Everyone
 * else looks around freely, within the limits of their seat, and turrets follow their view.
 *
 * <p>Gunner seats and camera positions move the first person eye to the point the definition
 * gives, which is how the original's camera entity behaved. Every other seat keeps its eye above
 * the seat along the hull's up axis, so it tilts with the vehicle.
 */
final class CrewController {
    /** Two presses of jump within this many ticks eject the player. */
    private static final int EJECT_WINDOW = 10;

    private static boolean jumpWasDown;
    private static int lastJumpPress = -100;
    private static int ticks;
    private static boolean tracking;
    private static float lastYaw;
    private static float lastPitch;

    private static @Nullable AircraftEntity viewVehicle;
    private static int viewSeat = -1;
    private static int cameraIndex;
    private static boolean gunnerSight;

    private static @Nullable MethodHandle setCameraPosition;
    private static boolean cameraLookupFailed;

    private CrewController() {
    }

    static void init() {
        TickEvent.ClientTickEvent.Post.BUS.addListener(event -> onClientTick());
        ViewportEvent.ComputeCameraAngles.BUS.addListener(CrewController::onCameraAngles);
        RenderHandEvent.BUS.addListener(CrewController::onRenderHand);
    }

    /**
     * Like the original, the first person hand stays hidden in a vehicle: always for the pilot,
     * and for anyone else who looks through a camera or has a weapon to aim.
     */
    private static boolean onRenderHand(RenderHandEvent event) {
        LocalPlayer player = Minecraft.getInstance().player;
        AircraftEntity vehicle = vehicleOf(player);
        if (vehicle == null) {
            return false;
        }
        AircraftDefinition def = vehicle.definition();
        int seat = vehicle.seatOf(player);
        if (seat < 0 || seat >= def.seatCount()) {
            return false;
        }
        VehicleWeapons weapons = vehicle.weapons();
        boolean armed = weapons != null && weapons.selected(seat) >= 0;
        return seat == 0 || armed || tvCamera(vehicle, seat) != null || eyePosition(vehicle, def, seat) != null;
    }

    private static @Nullable AircraftEntity vehicleOf(@Nullable LocalPlayer player) {
        return player != null && player.getVehicle() instanceof AircraftEntity vehicle && vehicle.definition() != null ? vehicle : null;
    }

    private static void onClientTick() {
        ticks++;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        AircraftEntity vehicle = vehicleOf(player);
        if (vehicle == null) {
            jumpWasDown = false;
            tracking = false;
            viewVehicle = null;
            viewSeat = -1;
            return;
        }
        int seat = vehicle.seatOf(player);
        if (vehicle != viewVehicle || seat != viewSeat) {
            // New vehicle or seat: start from the default view.
            viewVehicle = vehicle;
            viewSeat = seat;
            cameraIndex = 0;
            gunnerSight = false;
            tracking = false;
        }

        Input keys = player.input.keyPresses;
        boolean jumpPressed = keys.jump() && !jumpWasDown;
        jumpWasDown = keys.jump();
        boolean pilot = seat == 0 && vehicle.getControllingPassenger() == player;

        if (pilot) {
            AircraftEntity.ControlInput input = vehicle.input();
            input.throttleUp = keys.forward();
            input.throttleDown = keys.backward();
            input.left = keys.left();
            input.right = keys.right();
            input.freeLook = isFreeLook(vehicle);
            input.brake = vehicle instanceof TankEntity && keys.jump();
            if (input.brake) {
                // The hand brake also stops pivot turns once the vehicle has come to rest.
                input.throttleUp = false;
                input.throttleDown = false;
                double dx = vehicle.getX() - vehicle.xo;
                double dz = vehicle.getZ() - vehicle.zo;
                if (vehicle.visibleThrottle() <= 0.03F && dx * dx + dz * dz < 0.01) {
                    input.left = false;
                    input.right = false;
                }
            }
            if (jumpPressed && vehicle instanceof HelicopterEntity heli) {
                heli.toggleHovering();
            }
            ModNetwork.sendToServer(new PilotStatePacket(vehicle.getId(), vehicle.visibleThrottle(), vehicle.reverse(),
                    vehicle.roll(), vehicle.isHoveringMode(), input.write()));
        }

        // Eject with a double tap, except for helicopter pilots whose jump key toggles hover.
        if (jumpPressed && vehicle.canEject(player) && !(pilot && vehicle instanceof HelicopterEntity)) {
            if (ticks - lastJumpPress <= EJECT_WINDOW) {
                send(vehicle, CrewActionPacket.Action.EJECT);
                lastJumpPress = -100;
            } else {
                lastJumpPress = ticks;
            }
        }

        while (KeyBindings.SWITCH_SEAT.consumeClick()) {
            send(vehicle, CrewActionPacket.Action.SWITCH_SEAT);
        }
        while (KeyBindings.SWITCH_CAMERA.consumeClick()) {
            switchCamera(vehicle, seat);
        }
        while (KeyBindings.LANDING_GEAR.consumeClick()) {
            if (pilot) {
                send(vehicle, CrewActionPacket.Action.LANDING_GEAR);
            }
        }
        while (KeyBindings.FOLD.consumeClick()) {
            if (pilot) {
                send(vehicle, CrewActionPacket.Action.FOLD);
            }
        }
        while (KeyBindings.VTOL.consumeClick()) {
            if (pilot) {
                send(vehicle, CrewActionPacket.Action.VTOL);
            }
        }
        while (KeyBindings.RACK_LOAD.consumeClick()) {
            if (pilot) {
                send(vehicle, CrewActionPacket.Action.RACK_LOAD);
            }
        }
        while (KeyBindings.RACK_UNLOAD.consumeClick()) {
            if (pilot) {
                send(vehicle, CrewActionPacket.Action.RACK_UNLOAD);
            }
        }
    }

    private static void send(AircraftEntity vehicle, CrewActionPacket.Action action) {
        ModNetwork.sendToServer(new CrewActionPacket(vehicle.getId(), action));
    }

    /** Pilot cycles the camera positions; gunners that may leave their sight toggle it. */
    private static void switchCamera(AircraftEntity vehicle, int seat) {
        AircraftDefinition def = vehicle.definition();
        if (def == null || seat < 0 || seat >= def.seatCount()) {
            return;
        }
        SeatDefinition s = def.seats.get(seat);
        if (seat == 0) {
            switchPilotView(vehicle, def);
        } else if (s.gunner() && s.switchGunner()) {
            gunnerSight = !gunnerSight;
        }
    }

    /**
     * The pilot's switch mode key, after {@code KeySwitchMode}: leave gunner mode, otherwise step
     * through the extra camera positions, otherwise enter gunner mode where the vehicle allows it,
     * otherwise go to the first extra camera. Unlike the original, leaving gunner mode never
     * depends on the plane still meeting the conditions for entering it.
     */
    private static void switchPilotView(AircraftEntity vehicle, AircraftDefinition def) {
        int cameras = def.cameraPositions.size();
        if (vehicle.isGunnerMode()) {
            send(vehicle, CrewActionPacket.Action.GUNNER_MODE);
            cameraIndex = 0;
        } else if (cameraIndex > 0) {
            cameraIndex = (cameraIndex + 1) % cameras;
        } else if (vehicle.canSwitchGunnerMode()) {
            send(vehicle, CrewActionPacket.Action.GUNNER_MODE);
        } else if (cameras >= 2) {
            cameraIndex = 1;
        }
    }

    /**
     * Free look for the pilot. Helicopters and planes look around while the key is held; tanks
     * start from their {@code DefaultFreelook} setting and the key swaps it while held.
     */
    private static boolean isFreeLook(AircraftEntity vehicle) {
        boolean held = KeyBindings.FREE_LOOK.isDown();
        if (vehicle instanceof TankEntity) {
            AircraftDefinition def = vehicle.definition();
            return def != null && def.defaultFreelook != held;
        }
        return held;
    }

    private static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        AircraftEntity vehicle = vehicleOf(player);
        if (vehicle == null || mc.isPaused()) {
            return;
        }
        AircraftDefinition def = vehicle.definition();
        int seat = vehicle.seatOf(player);
        if (seat < 0 || seat >= def.seatCount()) {
            return;
        }
        float partialTicks = event.getPartialTick();
        boolean firstPerson = mc.options.getCameraType().isFirstPerson();

        if (seat == 0 && vehicle.steersWithMouse() && !vehicle.isGunnerMode() && vehicle.getControllingPassenger() == player) {
            steerWithMouse(vehicle, player, event, partialTicks, firstPerson);
        } else {
            tracking = false;
            constrainView(vehicle, def, def.seats.get(seat), seat, player);
        }

        TvMissileEntity missile = tvCamera(vehicle, seat);
        if (!(firstPerson && missile != null)) {
            // Every seat of every kind tilts with the hull, measured along the way the rider looks.
            event.setRoll(AircraftRotation.cameraRoll(vehicle.getYRot(partialTicks), vehicle.getXRot(partialTicks),
                    vehicle.roll(partialTicks), event.getYaw(), event.getPitch()));
        }
        if (firstPerson && missile != null) {
            placeCamera(event.getCamera(), missile.getPosition(partialTicks));
        } else if (firstPerson) {
            Vector3 eye = eyePosition(vehicle, def, seat);
            moveCamera(event.getCamera(), vehicle, def, eye != null ? eye : seatEye(vehicle, def, seat, player), seat, partialTicks);
        }
    }

    /**
     * The normal eye, in hull space. Vanilla puts it straight above the rider in world space,
     * which leaves the head outside the cockpit once the hull banks or pitches; measured along the
     * hull's own up axis it stays where the rider model's head is drawn.
     */
    private static Vector3 seatEye(AircraftEntity vehicle, AircraftDefinition def, int seat, LocalPlayer player) {
        Vector3 pos = def.seats.get(seat).position();
        double up = player.getEyeHeight() - player.getVehicleAttachmentPoint(vehicle).y;
        return new Vector3(pos.x(), pos.y() + up, pos.z());
    }

    /** The phase one pilot model: mouse movement becomes aircraft rotation, the view stays on the nose. */
    private static void steerWithMouse(AircraftEntity vehicle, LocalPlayer player, ViewportEvent.ComputeCameraAngles event,
                                       float partialTicks, boolean firstPerson) {
        boolean freeLook = KeyBindings.FREE_LOOK.isDown();
        boolean ownPitch = vehicle.isHovering();
        if (!freeLook) {
            if (tracking) {
                float dYaw = Mth.wrapDegrees(player.getYRot() - lastYaw);
                float dPitch = player.getXRot() - lastPitch;
                vehicle.applyPilotLook(dYaw, dPitch);
                HudRenderer.onPilotLook(dYaw, dPitch);
            }
            lockView(player, vehicle.getYRot(), vehicle.yRotO, ownPitch ? null : vehicle.getXRot(), vehicle.xRotO);
            lastYaw = player.getYRot();
            lastPitch = player.getXRot();
            tracking = true;
        } else {
            // Re-acquire the nose smoothly once free look is released.
            tracking = false;
        }
        if (firstPerson && !freeLook) {
            event.setYaw(vehicle.getYRot(partialTicks));
            if (!ownPitch) {
                event.setPitch(vehicle.getXRot(partialTicks));
            }
        }
    }

    /** Seat limits for everyone who does not fly with the mouse. */
    private static void constrainView(AircraftEntity vehicle, AircraftDefinition def, SeatDefinition seat, int index, LocalPlayer player) {
        boolean freeLookHeld = KeyBindings.FREE_LOOK.isDown();
        if (seat.fixedRotation() && !freeLookHeld) {
            lockView(player, vehicle.getYRot() + seat.fixedYaw(), vehicle.yRotO + seat.fixedYaw(),
                    vehicle.getXRot() + seat.fixedPitch(), vehicle.xRotO + seat.fixedPitch());
            return;
        }
        boolean pilot = index == 0;
        if (pilot && vehicle instanceof TankEntity && !isFreeLook(vehicle)) {
            lockView(player, vehicle.getYRot(), vehicle.yRotO, vehicle.getXRot(), vehicle.xRotO);
            return;
        }
        float min;
        float max;
        if (pilot && (vehicle instanceof TankEntity || vehicle instanceof GroundVehicleEntity)) {
            min = def.minRotationPitch;
            max = def.maxRotationPitch;
        } else if (seat.gunner() || (pilot && vehicle.isGunnerMode())) {
            min = seat.minPitch();
            max = seat.maxPitch();
        } else {
            return;
        }
        // Limits are relative to the hull's tilt along the view direction.
        double rel = Math.toRadians(Mth.wrapDegrees(player.getYRot() - vehicle.getYRot()));
        float tilt = (float) (vehicle.getXRot() * Math.cos(rel) + vehicle.roll() * Math.sin(rel));
        float pitch = Mth.clamp(player.getXRot(), tilt + min, tilt + max);
        pitch = Mth.clamp(pitch, -90.0F, 90.0F);
        if (pitch != player.getXRot()) {
            player.setXRot(pitch);
            player.xRotO = pitch;
        }
    }

    private static void lockView(LocalPlayer player, float yaw, float yawO, @Nullable Float pitch, float pitchO) {
        player.setYRot(yaw);
        player.yRotO = yawO;
        player.setYHeadRot(yaw);
        player.setYBodyRot(yaw);
        if (pitch != null) {
            player.setXRot(pitch);
            player.xRotO = pitchO;
        }
    }

    /**
     * Where the first person eye goes, in hull space, or {@code null} for the normal eye. Mirrors
     * the original's choice between the pilot's camera positions and a gunner seat's sight.
     */
    private static @Nullable Vector3 eyePosition(AircraftEntity vehicle, AircraftDefinition def, int seat) {
        SeatDefinition s = def.seats.get(seat);
        boolean sight = s.gunner() && (!s.switchGunner() || gunnerSight);
        CameraPosition camera = null;
        if (seat == 0) {
            if (cameraIndex > 0 && cameraIndex < def.cameraPositions.size()) {
                camera = def.cameraPositions.get(cameraIndex);
            } else if (def.uav || def.alwaysCameraView || sight || vehicle.isGunnerMode() || vehicle instanceof GroundVehicleEntity) {
                camera = def.cameraPositions.getFirst();
            }
        } else if (sight) {
            camera = s.camera() != null && s.camera() != CameraPosition.DEFAULT ? s.camera() : def.cameraPositions.getFirst();
        }
        // A camera at the origin means the file never set one; the normal eye is better there.
        if (camera == null || camera.position().equals(Vector3.ZERO)) {
            return null;
        }
        return camera.position();
    }

    private static void moveCamera(Camera camera, AircraftEntity vehicle, AircraftDefinition def, Vector3 eye, int seat, float partialTicks) {
        MethodHandle setter = cameraSetter();
        if (setter == null) {
            return;
        }
        SeatDefinition s = def.seats.get(seat);
        Vector3 local = s.rotatingSeat() ? vehicle.onTurret(def, eye, partialTicks) : eye;
        Vector3f offset = AircraftRotation.toWorld(vehicle.getYRot(partialTicks), vehicle.getXRot(partialTicks), vehicle.roll(partialTicks),
                local.x(), local.y(), local.z());
        placeCamera(camera, new Vec3(Mth.lerp(partialTicks, vehicle.xo, vehicle.getX()) + offset.x,
                Mth.lerp(partialTicks, vehicle.yo, vehicle.getY()) + offset.y,
                Mth.lerp(partialTicks, vehicle.zo, vehicle.getZ()) + offset.z));
    }

    /**
     * The TV missile this seat is flying, whose nose camera replaces the eye. Like the original,
     * only while the seat has a TV missile selected in its guided mode.
     */
    static @Nullable TvMissileEntity tvCamera(AircraftEntity vehicle, int seat) {
        TvMissileEntity missile = vehicle.tvMissile();
        VehicleWeapons weapons = vehicle.weapons();
        WeaponSet set = weapons != null ? weapons.current(seat) : null;
        if (missile == null || set == null || set.definition().type != WeaponType.TV_MISSILE || set.mode() != 0) {
            return null;
        }
        return missile;
    }

    private static void placeCamera(Camera camera, Vec3 at) {
        MethodHandle setter = cameraSetter();
        if (setter == null) {
            return;
        }
        try {
            setter.invoke(camera, at);
        } catch (Throwable t) {
            MCHeli.LOGGER.warn("Could not move the camera; the first person eye stays where vanilla puts it", t);
            setCameraPosition = null;
            cameraLookupFailed = true;
        }
    }

    /** {@code Camera.setPosition} is protected. Looked up once; on failure the feature turns itself off. */
    private static @Nullable MethodHandle cameraSetter() {
        if (setCameraPosition == null && !cameraLookupFailed) {
            try {
                setCameraPosition = MethodHandles.lookup().unreflect(
                        ObfuscationReflectionHelper.findMethod(Camera.class, "setPosition", Vec3.class));
            } catch (Exception e) {
                MCHeli.LOGGER.warn("Camera.setPosition is not accessible; the first person eye stays where vanilla puts it", e);
                cameraLookupFailed = true;
            }
        }
        return setCameraPosition;
    }
}
