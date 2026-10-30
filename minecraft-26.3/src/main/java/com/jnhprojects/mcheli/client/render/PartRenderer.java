package com.jnhprojects.mcheli.client.render;

import com.jnhprojects.mcheli.content.Vector3;
import com.jnhprojects.mcheli.content.aircraft.AircraftDefinition;
import com.jnhprojects.mcheli.content.aircraft.HelicopterDefinition;
import com.jnhprojects.mcheli.content.aircraft.PlaneDefinition;
import com.jnhprojects.mcheli.content.aircraft.RotorDefinition;
import com.jnhprojects.mcheli.content.aircraft.VehicleDefinition;
import com.jnhprojects.mcheli.content.aircraft.WeaponMount;
import com.jnhprojects.mcheli.content.aircraft.part.CameraPart;
import com.jnhprojects.mcheli.content.aircraft.part.CrawlerTrack;
import com.jnhprojects.mcheli.content.aircraft.part.LandingGearPart;
import com.jnhprojects.mcheli.content.aircraft.part.MovingPart;
import com.jnhprojects.mcheli.content.aircraft.part.PropellerPart;
import com.jnhprojects.mcheli.content.aircraft.part.RotatingPart;
import com.jnhprojects.mcheli.content.aircraft.part.ThrottlePart;
import com.jnhprojects.mcheli.content.aircraft.part.TrackRoller;
import com.jnhprojects.mcheli.content.aircraft.part.VehiclePart;
import com.jnhprojects.mcheli.content.aircraft.part.WeaponBayPart;
import com.jnhprojects.mcheli.content.aircraft.part.WeaponPart;
import com.jnhprojects.mcheli.content.aircraft.part.WheelPart;
import com.jnhprojects.mcheli.content.aircraft.part.WingPart;
import com.jnhprojects.mcheli.model.Mesh;
import com.jnhprojects.mcheli.model.MeshGroup;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.List;

/**
 * Submits a vehicle's hull and moving parts for one frame. A port of the part routines in
 * {@code MCH_RenderAircraft} and the per kind renderers; every transform is applied in hull space
 * on top of the pose the caller already set up.
 *
 * <p>A part is looked up as {@code $name} inside the vehicle model, or as a separate
 * {@code <vehicle>_<name>} model file, the two places the original accepted.
 */
final class PartRenderer {
    private static final Vector3 X_AXIS = new Vector3(1, 0, 0);
    private static final Vector3 DOWN_AXIS = new Vector3(0, -1, 0);

    private final AircraftDefinition def;
    private final Mesh mesh;
    private final AircraftRenderState state;
    private final PoseStack pose;
    private final SubmitNodeCollector collector;
    private final Identifier texture;
    private final RenderType opaqueType;
    private final RenderType glassType;
    private final int light;
    private final int overlay;
    /** Glass draws unlit, so its colour carries the brightness of where the vehicle stands. */
    private final int glassColor;

    PartRenderer(AircraftDefinition def, Mesh mesh, AircraftRenderState state, PoseStack pose, SubmitNodeCollector collector,
                 Identifier texture, int light, int overlay, int glassColor) {
        this.def = def;
        this.mesh = mesh;
        this.state = state;
        this.pose = pose;
        this.collector = collector;
        this.texture = texture;
        // Culled, as the original drew it. The models are full of back to back faces for thin
        // panels, and drawing both sides of those makes them fight for the same depth.
        this.opaqueType = RenderTypes.entityCutoutCull(texture);
        // The one entity translucent type that does not write depth, so water behind glass still
        // draws. It does not cull, so MeshRenderer drops the back faces itself.
        this.glassType = RenderTypes.entityTranslucentEmissive(texture);
        this.light = light;
        this.overlay = overlay;
        this.glassColor = glassColor;
    }

    void submitAll() {
        switch (def) {
            case PlaneDefinition plane -> {
                nozzles(plane);
                wings(plane);
                propellers(plane);
                submit(mesh.body());
            }
            case HelicopterDefinition heli -> {
                submit(mesh.body());
                rotors(heli);
            }
            case VehicleDefinition vehicle -> {
                submit(mesh.body());
                for (VehiclePart part : vehicle.parts) {
                    vehiclePart(vehicle, part);
                }
            }
            default -> submit(mesh.body());
        }
        weapons();
        rotatingParts();
        movingParts(def.hatches, state.hatch);
        trackRollers();
        crawlerTracks();
        steeringWheels();
        wheels();
        throttles();
        cameras();
        landingGear();
        weaponBays();
        movingParts(def.canopies, state.canopy);
    }

    // Kind specific parts

    private void rotors(HelicopterDefinition heli) {
        for (RotorDefinition rotor : heli.rotors) {
            List<MeshGroup> blade = part(rotor.part());
            if (blade.isEmpty() || isZero(rotor.axis())) {
                continue;
            }
            int step = rotor.bladeStep() > 0 ? rotor.bladeStep() : 1;
            int blades = Math.max(rotor.blades(), 1);
            for (int i = 0; i < blades; i++) {
                pose.pushPose();
                Vector3 p = rotor.position();
                translate(p, 1.0F);
                rotate(state.rotorAngle + i * step, rotor.axis());
                if (!rotor.legacyPivot()) {
                    translate(p, -1.0F);
                }
                submit(blade);
                pose.popPose();
            }
        }
    }

    private void nozzles(PlaneDefinition plane) {
        for (MovingPart nozzle : plane.nozzles) {
            pose.pushPose();
            rotateAround(nozzle.position(), state.nozzle, nozzle.axis());
            submit(part(nozzle.part()));
            pose.popPose();
        }
    }

    private void wings(PlaneDefinition plane) {
        for (WingPart wing : plane.wings) {
            pose.pushPose();
            rotateAround(wing.position(), state.wing * wing.factor(), wing.axis());
            submit(part(wing.part()));
            for (MovingPart pylon : wing.pylons()) {
                pose.pushPose();
                rotateAround(pylon.position(), state.wing * pylon.factor(), pylon.axis());
                submit(part(pylon.part()));
                pose.popPose();
            }
            pose.popPose();
        }
    }

    /** Tilt rotors follow the nozzle state, the blades spin with the engine. */
    private void propellers(PlaneDefinition plane) {
        if (!plane.hasNozzleState()) {
            return;
        }
        for (PropellerPart rotor : plane.propellers) {
            pose.pushPose();
            rotateAround(rotor.position(), state.nozzle * rotor.factor(), rotor.axis());
            submit(part(rotor.part()));
            for (PropellerPart.Blade blade : rotor.blades()) {
                List<MeshGroup> groups = part(blade.part());
                pose.pushPose();
                rotateAround(blade.position(), state.rotorAngle, blade.axis());
                for (int i = 0; i < blade.count(); i++) {
                    rotateAround(blade.position(), blade.step(), blade.axis());
                    submit(groups);
                }
                pose.popPose();
            }
            pose.popPose();
        }
    }

    /** {@code MCH_RenderVehicle.drawPart}: mount parts turn with the gunner's view. */
    private void vehiclePart(VehicleDefinition vehicle, VehiclePart part) {
        pose.pushPose();
        if (part.yaw() || part.pitch() || part.type() == VehiclePart.TYPE_ROTATING_BARREL) {
            translate(part.position(), 1.0F);
            if (part.yaw()) {
                rotate(seatYaw(0), DOWN_AXIS);
            }
            if (part.pitch()) {
                rotate(Mth.clamp(seatPitch(0), vehicle.minRotationPitch, vehicle.maxRotationPitch), X_AXIS);
            }
            translate(part.position(), -1.0F);
        }
        for (VehiclePart child : part.children()) {
            vehiclePart(vehicle, child);
        }
        boolean gunnerLooking = state.localSeat == 0 && state.localFirstPerson;
        if (part.drawFirstPerson() || !gunnerLooking) {
            submit(part(part.part()));
        }
        pose.popPose();
    }

    // Parts every kind can have

    /**
     * Gun mounts, turrets and missiles, {@code renderWeapon}. Each follows its weapon's aim, kicks
     * back as it fires, spins its barrels and, for missiles, disappears until reloaded. A part
     * whose weapon is missing follows the view of the seat that would fire it.
     */
    private void weapons() {
        float turretYaw = seatYaw(0);
        for (int i = 0; i < def.weaponParts.size(); i++) {
            WeaponPart w = def.weaponParts.get(i);
            AircraftRenderState.WeaponPartState ws = i < state.weaponParts.length ? state.weaponParts[i] : null;
            boolean live = ws != null && ws.hasWeapon;
            WeaponMount mount = mount(w);
            int seat = mount != null ? mount.seatIndex() : 0;
            if (w.hideInFirstPerson() && state.localFirstPerson && state.localSeat == seat) {
                continue;
            }
            float defaultYaw = live ? ws.defaultYaw : mount != null ? mount.defaultYaw() : 0.0F;
            float yaw;
            float pitch;
            if (live) {
                yaw = ws.yaw;
                pitch = ws.pitch;
            } else {
                yaw = Mth.wrapDegrees(seatYaw(seat) - defaultYaw);
                pitch = seatPitch(seat);
            }

            pose.pushPose();
            if (w.turret()) {
                rotateAround(def.turretPosition, turretYaw, DOWN_AXIS);
            }
            translate(w.position(), 1.0F);
            if (w.yaw()) {
                rotate(w.turret() && !live ? yaw - turretYaw : yaw, DOWN_AXIS);
            }
            if (w.turret() && live) {
                rotate(-(turretYaw - ws.turretLag), DOWN_AXIS);
            }
            // Side facing guns pitch around their own axis, which the original did by turning
            // into the gun's frame and flipping the sign for guns pointing left or right.
            boolean sideways = false;
            if ((int) defaultYaw != 0) {
                float t = Mth.wrapDegrees(defaultYaw);
                sideways = (t >= 45.0F && t <= 135.0F) || (t <= -45.0F && t >= -135.0F);
                rotate(-defaultYaw, DOWN_AXIS);
            }
            if (w.pitch()) {
                rotate(sideways ? -pitch : pitch, X_AXIS);
            }
            if (live && w.recoil() != 0.0F) {
                pose.translate(0.0F, 0.0F, w.recoil() * ws.recoil);
            }
            if (mount != null || live) {
                rotate(defaultYaw, DOWN_AXIS);
            }
            if (live && w.rotatingBarrel()) {
                rotate(ws.barrel, w.axis());
            }
            translate(w.position(), -1.0F);
            if (!w.missile() || !live || !ws.hidden) {
                submit(part(w.part()));
                for (WeaponPart.Child child : w.children()) {
                    pose.pushPose();
                    translate(child.position(), 1.0F);
                    if (child.yaw()) {
                        rotate(yaw, DOWN_AXIS);
                    }
                    if (live && (int) defaultYaw != 0) {
                        rotate(-defaultYaw, DOWN_AXIS);
                    }
                    if (child.pitch()) {
                        rotate(sideways ? -pitch : pitch, X_AXIS);
                    }
                    if (live && child.recoil() != 0.0F) {
                        pose.translate(0.0F, 0.0F, -child.recoil() * ws.recoil);
                    }
                    if (live && (int) defaultYaw != 0) {
                        rotate(defaultYaw, DOWN_AXIS);
                    }
                    translate(child.position(), -1.0F);
                    submit(part(child.part()));
                    pose.popPose();
                }
            }
            pose.popPose();
        }
    }

    private WeaponMount mount(WeaponPart part) {
        if (part.weapons().isEmpty()) {
            return null;
        }
        int group = def.weaponGroup(part.weapons().getFirst());
        return group >= 0 ? def.weaponGroups.get(group).mounts().getFirst() : null;
    }

    private void rotatingParts() {
        for (int i = 0; i < def.rotatingParts.size() && i < state.rotatingParts.length; i++) {
            RotatingPart part = def.rotatingParts.get(i);
            pose.pushPose();
            rotateAround(part.position(), state.rotatingParts[i], part.axis());
            submit(part(part.part()));
            pose.popPose();
        }
    }

    /** Hatches and canopies: swing by {@code angle * factor}, or slide along their offset. */
    private void movingParts(List<MovingPart> parts, float angle) {
        for (MovingPart part : parts) {
            pose.pushPose();
            moveOrSwing(part, angle);
            submit(part(part.part()));
            pose.popPose();
        }
    }

    private void moveOrSwing(MovingPart part, float angle) {
        if (part.slide()) {
            translate(part.position(), angle / 90.0F);
        } else {
            rotateAround(part.position(), angle * part.factor(), part.axis());
        }
    }

    private void trackRollers() {
        for (TrackRoller roller : def.trackRollers) {
            pose.pushPose();
            rotateAround(roller.position(), state.roller[roller.side()], X_AXIS);
            submit(part(roller.part()));
            pose.popPose();
        }
    }

    /**
     * Each link slides towards the next one by the track's run, so the loop appears to move.
     * The link part is modelled at the origin and placed at each point of the outline.
     */
    private void crawlerTracks() {
        for (CrawlerTrack track : def.crawlerTracks) {
            List<MeshGroup> link = part(track.part());
            List<CrawlerTrack.Link> links = track.links();
            int count = links.size() - 1;
            if (link.isEmpty() || count < 1) {
                continue;
            }
            float run = state.track[track.side()];
            for (int i = 0; i < count; i++) {
                CrawlerTrack.Link cur = links.get(i);
                CrawlerTrack.Link next = links.get((i + 1) % count);
                float angleNext = next.angle();
                if (angleNext - cur.angle() < -180.0F) {
                    angleNext += 360.0F;
                } else if (angleNext - cur.angle() > 180.0F) {
                    angleNext -= 360.0F;
                }
                pose.pushPose();
                pose.translate(0.0F, cur.y() + (next.y() - cur.y()) * run, cur.z() + (next.z() - cur.z()) * run);
                pose.rotateDegrees(Axis.XN, cur.angle() + (angleNext - cur.angle()) * run);
                submit(link);
                pose.popPose();
            }
        }
    }

    private void steeringWheels() {
        for (WheelPart wheel : def.steeringWheels) {
            pose.pushPose();
            rotateAround(wheel.position(), state.steer * wheel.steerAngle(), wheel.axis());
            submit(part(wheel.part()));
            pose.popPose();
        }
    }

    private void wheels() {
        for (WheelPart wheel : def.wheels) {
            pose.pushPose();
            rotateAround(wheel.pivot(), state.steer * wheel.steerAngle(), wheel.axis());
            rotateAround(wheel.position(), state.wheelRoll, X_AXIS);
            submit(part(wheel.part()));
            pose.popPose();
        }
    }

    private void throttles() {
        for (ThrottlePart lever : def.throttleParts) {
            pose.pushPose();
            rotateAround(lever.position(), state.throttle * lever.angle(), lever.axis());
            translate(lever.slide(), state.throttle);
            submit(part(lever.part()));
            pose.popPose();
        }
    }

    /** Sensor turrets look where the gunner looks, or the pilot when the gunner seat is empty. */
    private void cameras() {
        int seat = state.seatOccupied.length > 1 && state.seatOccupied[1] ? 1 : 0;
        for (CameraPart camera : def.cameraParts) {
            pose.pushPose();
            translate(camera.position(), 1.0F);
            if (camera.yawSync()) {
                rotate(seatYaw(seat), DOWN_AXIS);
            }
            if (camera.pitchSync()) {
                rotate(seatPitch(seat), X_AXIS);
            }
            translate(camera.position(), -1.0F);
            submit(part(camera.part()));
            pose.popPose();
        }
    }

    /**
     * Gear legs swing up with the gear state; reversed parts close as the gear rises, and gear bay
     * doors open fully halfway through and shut again.
     */
    private void landingGear() {
        float angle = state.landingGear;
        float reverse = 90.0F - angle;
        float door = Math.min(90.0F, 90.0F * Mth.sin(angle * 2.0F * Mth.DEG_TO_RAD) * 3.0F);
        for (LandingGearPart gear : def.landingGear) {
            pose.pushPose();
            translate(gear.position(), 1.0F);
            if (gear.reverse()) {
                rotate(reverse * gear.factor(), gear.axis());
            } else {
                rotate((gear.hatch() ? door : angle) * gear.factor(), gear.axis());
            }
            if (gear.secondAxis() != null) {
                rotate((gear.reverse() ? reverse : angle) * gear.secondFactor(), gear.secondAxis());
            }
            translate(gear.position(), -1.0F);
            if (gear.slide() != null) {
                float f = angle / 90.0F;
                translate(gear.slide(), gear.reverse() ? 1.0F - f : f);
            }
            submit(part(gear.part()));
            pose.popPose();
        }
    }

    /** Bays swing open while a crew member has one of their weapons selected. */
    private void weaponBays() {
        for (int i = 0; i < def.weaponBays.size(); i++) {
            WeaponBayPart bay = def.weaponBays.get(i);
            pose.pushPose();
            moveOrSwing(bay.door(), i < state.bays.length ? state.bays[i] : 0.0F);
            submit(part(bay.door().part()));
            pose.popPose();
        }
    }

    // Helpers

    private float seatYaw(int seat) {
        return seat >= 0 && seat < state.seatYaw.length ? state.seatYaw[seat] : 0.0F;
    }

    private float seatPitch(int seat) {
        return seat >= 0 && seat < state.seatPitch.length ? state.seatPitch[seat] : 0.0F;
    }

    private List<MeshGroup> part(String name) {
        List<MeshGroup> groups = mesh.part("$" + name);
        if (!groups.isEmpty()) {
            return groups;
        }
        Mesh separate = MeshCache.INSTANCE.find(def.folder(), def.name() + "_" + name);
        return separate != null ? separate.groups() : List.of();
    }

    private void submit(List<MeshGroup> groups) {
        if (groups.isEmpty()) {
            return;
        }
        GlassSplit.Split split = GlassSplit.INSTANCE.split(groups, texture);
        if (!split.opaque().isEmpty()) {
            collector.submitCustomGeometry(pose, opaqueType, (p, buffer) -> MeshRenderer.emit(split.opaque(), p, buffer, light, overlay));
        }
        if (!split.glass().isEmpty()) {
            collector.submitCustomGeometry(pose, glassType,
                    (p, buffer) -> MeshRenderer.emitFrontFaces(split.glass(), p, buffer, light, overlay, glassColor));
        }
    }

    private void translate(Vector3 v, float scale) {
        pose.translate((float) v.x() * scale, (float) v.y() * scale, (float) v.z() * scale);
    }

    private void rotateAround(Vector3 pivot, float degrees, Vector3 axis) {
        if (degrees == 0.0F || isZero(axis)) {
            return;
        }
        translate(pivot, 1.0F);
        rotate(degrees, axis);
        translate(pivot, -1.0F);
    }

    /** Like {@code glRotatef}: any axis length, a zero axis does nothing. */
    private void rotate(float degrees, Vector3 axis) {
        if (degrees == 0.0F || isZero(axis)) {
            return;
        }
        Vector3f a = new Vector3f((float) axis.x(), (float) axis.y(), (float) axis.z()).normalize();
        pose.rotateDegrees(Axis.of(a), degrees);
    }

    private static boolean isZero(Vector3 v) {
        return v.x() * v.x() + v.y() * v.y() + v.z() * v.z() < 1.0E-12;
    }
}
