package com.jnhprojects.mcheli.entity;

import com.jnhprojects.mcheli.content.Vector3;
import com.jnhprojects.mcheli.physics.AircraftRotation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Works out the pitch and roll of a ground vehicle from where its wheels touch the terrain.
 * Ported from {@code MCH_WheelManager}.
 *
 * <p>The original spawned an invisible entity per wheel and let each one fall onto the ground
 * under its mount point. Here every wheel is a one block collision box moved with vanilla's
 * collision code, which gives the same result without extra entities to sync. Each left/right
 * pair spans a plane with the centre of gravity; the weighted average of their normals is the
 * ground normal the hull leans towards.
 */
final class TerrainFollower {
    private static final double STEP_HEIGHT = 1.5;
    private static final double MAX_DRIFT = 2.0;
    private static final double SNAP_DISTANCE = 8.0;

    private final AircraftEntity vehicle;
    private Probe[] probes = new Probe[0];
    private Vector3 centre = Vector3.ZERO;
    private double wheelBase = 1.0;
    float targetPitch;
    float targetRoll;

    TerrainFollower(AircraftEntity vehicle) {
        this.vehicle = vehicle;
    }

    /**
     * @param wheels positive X wheel positions, front to back; the mirrored wheel is added for each
     * @param centre centre of gravity in hull space
     */
    void create(List<Vector3> wheels, Vector3 centre) {
        this.centre = centre;
        probes = new Probe[wheels.size() * 2];
        double minZ = Double.MAX_VALUE;
        double maxZ = -Double.MAX_VALUE;
        for (int i = 0; i < probes.length; i++) {
            Vector3 w = wheels.get(i / 2);
            Vector3 local = new Vector3(i % 2 == 0 ? w.x() : -w.x(), w.y(), w.z());
            Probe probe = new Probe(local, local.z() >= centre.z());
            Vec3 at = vehicle.toWorld(local);
            probe.setPos(at.x, at.y + 1.0, at.z);
            probes[i] = probe;
            minZ = Math.min(minZ, local.z());
            maxZ = Math.max(maxZ, local.z());
        }
        wheelBase = Math.max(maxZ - minZ, 1.0E-3);
    }

    boolean isEmpty() {
        return probes.length == 0;
    }

    /**
     * Drags the wheels along with the hull, lets them settle on the terrain and derives the target
     * attitude from them.
     *
     * @return extra horizontal motion from sliding down steep slopes
     */
    Vec3 move(double mx, double mz) {
        for (Probe probe : probes) {
            Vec3 target = vehicle.toWorld(probe.local);
            if (target.distanceToSqr(probe.x, probe.y, probe.z) > SNAP_DISTANCE * SNAP_DISTANCE) {
                // Teleported, or created before the vehicle was placed: start over under the hull.
                probe.setPos(target.x, target.y + 1.0, target.z);
            }
            double dx = target.x - probe.x + mx;
            double dy = (target.y - probe.y) * 0.15;
            double dz = target.z - probe.z + mz;
            probe.move(dx, dy, dz);
            probe.move(0.0, -0.1, 0.0);
        }

        int pairs = probes.length / 2;
        // If neither half touches the ground, the outermost pair of that half carries the hull,
        // which makes the vehicle tip over a crest instead of floating level.
        int rear = pairs - 1;
        for (int i = 0; i < pairs; i++) {
            if (!probes[i * 2].front && (probes[i * 2].onGround || probes[i * 2 + 1].onGround)) {
                rear = -1;
                break;
            }
        }
        if (rear >= 0) {
            probes[rear * 2].onGround = true;
            probes[rear * 2 + 1].onGround = true;
        }
        int front = 0;
        for (int i = pairs - 1; i >= 0; i--) {
            if (probes[i * 2].front && (probes[i * 2].onGround || probes[i * 2 + 1].onGround)) {
                front = -1;
                break;
            }
        }
        if (front >= 0 && pairs > 0) {
            probes[front * 2].onGround = true;
            probes[front * 2 + 1].onGround = true;
        }

        Vec3 wc = vehicle.toWorld(centre).subtract(vehicle.position());
        double cx = vehicle.getX() + wc.x;
        double cy = vehicle.getY() + centre.y();
        double cz = vehicle.getZ() + wc.z;
        Vec3 normal = Vec3.ZERO;
        for (int i = 0; i < pairs; i++) {
            Probe w1 = probes[i * 2];
            Probe w2 = probes[i * 2 + 1];
            Vec3 v1 = new Vec3(w1.x - cx, w1.y - cy, w1.z - cz);
            Vec3 v2 = new Vec3(w2.x - cx, w2.y - cy, w2.z - cz);
            Vec3 n = (w1.local.z() >= 0.0 ? v2.cross(v1) : v1.cross(v2)).normalize();
            double weight = w1.onGround || w2.onGround ? Math.abs(w1.local.z() / wheelBase) : 0.0;
            normal = normal.add(n.scale(weight));
        }
        normal = normal.normalize();

        Vec3 slide = Vec3.ZERO;
        if (normal.y > 0.01 && normal.y < 0.7) {
            slide = new Vec3(normal.x / 50.0, 0.0, normal.z / 50.0);
        }

        // Into the hull's frame, then read pitch and roll off the normal.
        double yaw = Math.toRadians(vehicle.getYRot());
        double c = Math.cos(yaw);
        double s = Math.sin(yaw);
        double lx = normal.x * c + normal.z * s;
        double lz = normal.z * c - normal.x * s;
        float pitch = (float) (90.0 - Math.toDegrees(Math.atan2(normal.y, lz)));
        float roll = (float) -(90.0 - Math.toDegrees(Math.atan2(normal.y, lx)));
        if (normal.lengthSqr() < 1.0E-6) {
            pitch = vehicle.getXRot();
            roll = vehicle.roll;
        }

        var def = vehicle.definition();
        float pitchStep = def != null ? def.onGroundPitchFactor : 0.0F;
        float rollStep = def != null ? def.onGroundRollFactor : 0.0F;
        targetPitch = Mth.clamp(pitch, vehicle.getXRot() - pitchStep, vehicle.getXRot() + pitchStep);
        targetRoll = Mth.clamp(roll, vehicle.roll - rollStep, vehicle.roll + rollStep);

        // Wheels that fell behind (stuck on a ledge) are pulled back under the hull.
        for (Probe probe : probes) {
            Vec3 v = vehicle.position().add(new Vec3(AircraftRotation.toWorld(vehicle.getYRot(), targetPitch, targetRoll,
                    probe.local.x(), probe.local.y(), probe.local.z())));
            boolean pulled = false;
            double x = probe.x;
            double z = probe.z;
            if (x > v.x + MAX_DRIFT || x < v.x - MAX_DRIFT) {
                x = Mth.clamp(x, v.x - MAX_DRIFT, v.x + MAX_DRIFT);
                pulled = true;
            }
            if (z > v.z + MAX_DRIFT || z < v.z - MAX_DRIFT) {
                z = Mth.clamp(z, v.z - MAX_DRIFT, v.z + MAX_DRIFT);
                pulled = true;
            }
            if (pulled) {
                probe.setPos(x, v.y + STEP_HEIGHT / 2.0, z);
            }
        }
        return slide;
    }

    /** One wheel: a one block box that falls onto the terrain and climbs steps up to 1.5 high. */
    private final class Probe {
        final Vector3 local;
        /** In front of the centre of gravity. */
        final boolean front;
        double x;
        double y;
        double z;
        boolean onGround;

        Probe(Vector3 local, boolean front) {
            this.local = local;
            this.front = front;
        }

        void setPos(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        private AABB box() {
            return new AABB(x - 0.5, y, z - 0.5, x + 0.5, y + 1.0, z + 0.5);
        }

        void move(double dx, double dy, double dz) {
            AABB box = box();
            Vec3 wanted = new Vec3(dx, dy, dz);
            Vec3 moved = collide(wanted, box);
            boolean blockedSideways = moved.x != dx || moved.z != dz;
            boolean wasGrounded = onGround || (moved.y != dy && dy < 0.0);
            if (blockedSideways && wasGrounded) {
                // Same step-up rule as the original wheel entity: climb, move, settle back down.
                Vec3 up = collide(new Vec3(0.0, STEP_HEIGHT, 0.0), box);
                AABB raised = box.move(up);
                Vec3 across = collide(new Vec3(dx, 0.0, dz), raised);
                AABB shifted = raised.move(across);
                Vec3 down = collide(new Vec3(0.0, -STEP_HEIGHT, 0.0), shifted);
                Vec3 stepped = up.add(across).add(down);
                if (stepped.horizontalDistanceSqr() > moved.horizontalDistanceSqr()) {
                    moved = stepped;
                }
            }
            x += moved.x;
            y += moved.y;
            z += moved.z;
            onGround = dy < 0.0 && moved.y != dy;
        }

        private Vec3 collide(Vec3 movement, AABB box) {
            return Entity.collideBoundingBox(vehicle, movement, box, vehicle.level(), List.of());
        }
    }
}
