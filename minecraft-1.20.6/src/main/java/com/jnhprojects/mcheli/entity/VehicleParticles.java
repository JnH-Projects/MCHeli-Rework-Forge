package com.jnhprojects.mcheli.entity;

import com.jnhprojects.mcheli.content.Vector3;
import com.jnhprojects.mcheli.content.aircraft.AircraftDefinition;
import com.jnhprojects.mcheli.content.aircraft.ParticleSplash;
import com.jnhprojects.mcheli.content.aircraft.PlaneDefinition;
import com.jnhprojects.mcheli.content.aircraft.part.MovingPart;
import com.jnhprojects.mcheli.particle.SmokeParticleOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * The spray, dust and exhaust a vehicle throws up, all client side:
 * <ul>
 *   <li>{@code onUpdate_ParticleSandCloud}: a helicopter's downwash raises a ring of dust off the
 *       ground or water below it; boats and amphibians with {@code EnableSeaSurfaceParticle} raise
 *       one off the sea while under way.</li>
 *   <li>{@code onUpdate_ParticleSplash}: each {@code AddParticleSplash} point sprays while it moves
 *       through water.</li>
 *   <li>{@code onUpdate_ParticleNozzle}: jet nozzles smoke with the throttle, more when the blast
 *       hits the ground, as on a Harrier in the hover.</li>
 *   <li>{@code onUpdate_ParticleLandingGear}: a plane rolling fast kicks up bits of the ground.</li>
 * </ul>
 */
final class VehicleParticles {
    private static final int WHITE = 0xFFFFFF;

    private VehicleParticles() {
    }

    static void tick(AircraftEntity vehicle, AircraftDefinition def) {
        if (vehicle.isDestroyed()) {
            return;
        }
        switch (vehicle) {
            case HelicopterEntity heli -> sandCloud(heli, def, false);
            case PlaneEntity plane -> {
                if (def instanceof PlaneDefinition planeDef) {
                    nozzles(plane, planeDef);
                }
                landingGear(plane);
                splashes(plane, def);
                sandCloud(plane, def, true);
            }
            case TankEntity tank -> {
                splashes(tank, def);
                sandCloud(tank, def, true);
            }
            default -> {
            }
        }
    }

    private static void sandCloud(AircraftEntity vehicle, AircraftDefinition def, boolean seaOnly) {
        if (seaOnly && !def.seaSurfaceParticles || def.particlesScale <= 0.01F) {
            return;
        }
        Level level = vehicle.level();
        RandomSource random = vehicle.level().getRandom();
        float scale = def.particlesScale * 3.0F * (seaOnly ? 2.0F : 1.0F);
        double throttle = Math.min(vehicle.visibleThrottle() * 2.0, 1.0);
        // A boat needs some water under it before it sprays; the original counted water blocks.
        int count = seaOnly ? (int) (scale * 7.0F) : 0;
        int range = (int) (scale * 10.0F) + 1;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int bx = Mth.floor(vehicle.getX() + 0.5);
        int by = Mth.floor(vehicle.getY() + 0.5);
        int bz = Mth.floor(vehicle.getZ() + 0.5);
        int y = 0;
        double groundY = 0.0;
        boolean found = false;
        for (; y < range && !found; y++) {
            for (int x = -1; x <= 1 && !found; x++) {
                for (int z = -1; z <= 1 && !found; z++) {
                    BlockState state = level.getBlockState(pos.set(bx + x, by - y, bz + z));
                    if (state.isAir()) {
                        continue;
                    }
                    if (seaOnly && state.getFluidState().is(FluidTags.WATER)) {
                        count--;
                    }
                    if (count <= 0) {
                        groundY = vehicle.getY() + 1.0 + scale / 5.0F - y;
                        found = true;
                    }
                }
            }
        }
        if (!found) {
            return;
        }
        double density = (range - y + 1) / (5.0 * scale) / 2.0;
        for (int k = 0; k < (int) (throttle * 6.0 * density); k++) {
            float angle = random.nextFloat() * Mth.TWO_PI;
            double dx = Mth.cos(angle);
            double dz = Mth.sin(angle);
            level.addAlwaysVisibleParticle(new SmokeParticleOptions(WHITE, scale * 5.0F, (int) (10.0F * scale) + 1), true,
                    vehicle.getX() + dx * scale * 3.0, groundY + (random.nextDouble() - 0.5) * scale, vehicle.getZ() + dz * scale * 3.0,
                    scale * dx * 0.3, scale * -0.02, scale * dz * 0.3);
        }
    }

    private static void splashes(AircraftEntity vehicle, AircraftDefinition def) {
        if (def.splashes.isEmpty()) {
            return;
        }
        double mx = vehicle.getX() - vehicle.xo;
        double mz = vehicle.getZ() - vehicle.zo;
        double dist = Math.min(mx * mx + mz * mz, 1.0);
        RandomSource random = vehicle.level().getRandom();
        for (ParticleSplash splash : def.splashes) {
            for (int i = 0; i < splash.count(); i++) {
                if (dist > 0.03 + random.nextFloat() * 0.1) {
                    splash(vehicle, splash, -mx * splash.acceleration(), -mz * splash.acceleration(), splash.size() * (0.5 + dist * 0.5));
                }
            }
        }
    }

    private static void splash(AircraftEntity vehicle, ParticleSplash splash, double mx, double mz, double size) {
        RandomSource random = vehicle.level().getRandom();
        Vec3 at = vehicle.toWorld(splash.position()).add(random.nextDouble() - 0.5, (random.nextDouble() - 0.5) * 0.5, random.nextDouble() - 0.5);
        BlockPos block = BlockPos.containing(at.x + 0.5, at.y, at.z + 0.5);
        if (!vehicle.level().getFluidState(block).is(FluidTags.WATER)) {
            return;
        }
        int age = splash.age() + (int) (random.nextFloat() * 0.5F * splash.age());
        vehicle.level().addAlwaysVisibleParticle(new SmokeParticleOptions(WHITE, (float) size * (random.nextFloat() * 0.2F + 0.8F), age), true,
                at.x, at.y, at.z, mx + (random.nextFloat() - 0.5) * 0.7, splash.motionY(), mz + (random.nextFloat() - 0.5) * 0.7);
    }

    private static void nozzles(PlaneEntity plane, PlaneDefinition def) {
        float throttle = plane.visibleThrottle();
        if (def.nozzles.isEmpty() || throttle <= 0.1F) {
            return;
        }
        RandomSource random = plane.level().getRandom();
        PartState nozzle = plane.nozzle();
        float tilt = nozzle != null ? nozzle.angle() * Mth.DEG_TO_RAD : 0.0F;
        // Out of the back, turned down as the nozzles swing towards the hover.
        Vector3f blast = plane.toWorldOffset(new Vector3(0.0, -Mth.sin(tilt), -Mth.cos(tilt)));
        for (MovingPart part : def.nozzles) {
            if (random.nextFloat() > throttle * 1.5F) {
                continue;
            }
            Vec3 at = plane.toWorld(part.position()).add(blast.x, blast.y, blast.z);
            BlockPos ahead = BlockPos.containing(at.x + blast.x * 3.0, at.y + blast.y * 3.0, at.z + blast.z * 3.0);
            float spread = plane.level().getBlockState(ahead).isAir() ? 0.7F : 2.0F;
            plane.level().addAlwaysVisibleParticle(new SmokeParticleOptions(WHITE, 5.0F * def.particlesScale, 30), true, at.x, at.y, at.z,
                    blast.x + (random.nextFloat() - 0.5F) * spread, blast.y, blast.z + (random.nextFloat() - 0.5F) * spread);
        }
    }

    private static void landingGear(PlaneEntity plane) {
        double dx = plane.getX() - plane.xo;
        double dz = plane.getZ() - plane.zo;
        if (!plane.onGround() || dx * dx + dz * dz <= 0.01) {
            return;
        }
        BlockPos below = BlockPos.containing(plane.getX(), plane.getY() - 0.5, plane.getZ());
        BlockState state = plane.level().getBlockState(below);
        if (state.getRenderShape() == RenderShape.INVISIBLE) {
            return;
        }
        RandomSource random = plane.level().getRandom();
        float width = plane.getBbWidth();
        plane.level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, state),
                plane.getX() + (random.nextFloat() - 0.5) * width, plane.getBoundingBox().minY + 0.1, plane.getZ() + (random.nextFloat() - 0.5) * width,
                -dx * 4.0, 1.5, -dz * 4.0);
    }
}
