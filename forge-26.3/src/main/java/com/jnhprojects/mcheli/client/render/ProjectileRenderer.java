package com.jnhprojects.mcheli.client.render;

import com.jnhprojects.mcheli.MCHeli;
import com.jnhprojects.mcheli.content.weapon.WeaponColor;
import com.jnhprojects.mcheli.content.weapon.WeaponDefinition;
import com.jnhprojects.mcheli.entity.projectile.AAMissileEntity;
import com.jnhprojects.mcheli.entity.projectile.ASMissileEntity;
import com.jnhprojects.mcheli.entity.projectile.ATMissileEntity;
import com.jnhprojects.mcheli.entity.projectile.BombEntity;
import com.jnhprojects.mcheli.entity.projectile.BulletEntity;
import com.jnhprojects.mcheli.entity.projectile.MarkerRocketEntity;
import com.jnhprojects.mcheli.entity.projectile.ProjectileEntity;
import com.jnhprojects.mcheli.entity.projectile.RocketEntity;
import com.jnhprojects.mcheli.entity.projectile.TvMissileEntity;
import com.jnhprojects.mcheli.model.Mesh;
import com.jnhprojects.mcheli.particle.GlowParticleOptions;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Draws rounds, rockets, missiles and bombs from {@code models/bullets}, tinted with the weapon's
 * {@code BulletColor} (its in-water colour below the surface) and always fully lit, as the
 * original's {@code MCH_RenderBulletBase} did.
 *
 * <p>Not in the original: rounds trail a tracer and rocket motors burn with a glow, faint by day
 * and bright at night, so a firefight in the dark can be followed by eye. Nothing glows under
 * water.
 */
public class ProjectileRenderer extends EntityRenderer<ProjectileEntity, ProjectileRenderer.State> {
    private static final int FULL_BRIGHT = 0xF000F0;
    /** Warm tracer colour for rounds whose weapon keeps the default white tint. */
    private static final int TRACER = 0xFFC46A;
    /** The longest tracer ribbon, in blocks; fast rounds cover far more than this per tick. */
    private static final double MAX_TRACER = 6.0;
    /** How far behind the projectile's centre the motor glow sits, in blocks. */
    private static final double MOTOR_OFFSET = 0.6;

    public ProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    enum GlowKind {
        NONE,
        TRACER,
        MOTOR
    }

    public static class State extends EntityRenderState {
        String model = "";
        float yaw;
        float pitch;
        int color = 0xFFFFFFFF;
        boolean visible = true;
        GlowKind glowKind = GlowKind.NONE;
        float glow;
        int glowColor = TRACER;
        /** Distance covered over the last tick, which points the tracer and the motor glow. */
        Vec3 travel = Vec3.ZERO;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(ProjectileEntity entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.model = entity.model();
        state.yaw = entity.getYRot(partialTicks);
        state.pitch = entity.getXRot(partialTicks);
        state.visible = !(entity instanceof BombEntity bomb) || bomb.isVisible();
        WeaponDefinition w = entity.weapon();
        boolean inWater = inWater(entity.level(), entity);
        WeaponColor c = w != null ? (inWater ? w.colorInWater : w.color) : WeaponColor.WHITE;
        state.color = c.argb();

        state.glowKind = inWater ? GlowKind.NONE : glowKind(entity);
        if (state.glowKind != GlowKind.NONE) {
            state.glow = Glow.strength(entity.level(), entity.blockPosition());
            state.travel = new Vec3(entity.getX() - entity.xo, entity.getY() - entity.yo, entity.getZ() - entity.zo);
            if (state.glowKind == GlowKind.MOTOR) {
                state.glowColor = GlowParticleOptions.FIRE;
            } else {
                // A weapon with its own tint keeps it for the tracer; the default white burns warm.
                int tint = state.color & 0xFFFFFF;
                state.glowColor = tint == 0xFFFFFF ? TRACER : tint;
            }
        }
    }

    private static GlowKind glowKind(ProjectileEntity entity) {
        if (entity instanceof BulletEntity) {
            return GlowKind.TRACER;
        }
        if (entity instanceof RocketEntity || entity instanceof MarkerRocketEntity || entity instanceof AAMissileEntity
                || entity instanceof ASMissileEntity || entity instanceof ATMissileEntity || entity instanceof TvMissileEntity) {
            return GlowKind.MOTOR;
        }
        return GlowKind.NONE;
    }

    /** The original looked for water at the round and up to two blocks above it. */
    private static boolean inWater(Level level, ProjectileEntity e) {
        for (int y = 0; y < 3; y++) {
            if (level.getFluidState(BlockPos.containing(e.getX() + 0.5, e.getY() + 1.5 - y, e.getZ() + 0.5)).is(FluidTags.WATER)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        Mesh mesh = state.visible ? MeshCache.INSTANCE.find("bullets", state.model) : null;
        if (mesh != null) {
            Identifier texture = Identifier.fromNamespaceAndPath(MCHeli.MOD_ID, "textures/bullets/" + state.model + ".png");
            pose.pushPose();
            pose.rotateDegrees(Axis.YP, -state.yaw);
            pose.rotateDegrees(Axis.XP, state.pitch);
            int color = state.color;
            collector.submitCustomGeometry(pose, RenderTypes.entityTranslucentCull(texture),
                    (p, buffer) -> MeshRenderer.emit(mesh.groups(), p, buffer, FULL_BRIGHT, OverlayTexture.NO_OVERLAY, color));
            pose.popPose();
        }
        if (state.visible) {
            submitGlow(state, pose, collector, camera);
        }
        super.submit(state, pose, collector, camera);
    }

    private static void submitGlow(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.glowKind == GlowKind.NONE || state.glow <= 0.0F) {
            return;
        }
        double speed = state.travel.length();
        Vec3 back = speed > 1.0E-3 ? state.travel.scale(-1.0 / speed) : Vec3.ZERO;
        switch (state.glowKind) {
            case TRACER -> {
                Vec3 toCamera = new Vec3(state.x - camera.pos.x, state.y - camera.pos.y, state.z - camera.pos.z);
                Glow.streak(pose, collector, toCamera, back.scale(Math.min(speed, MAX_TRACER)), 0.08F, state.glowColor, state.glow);
                Glow.halo(pose, collector, camera, Vec3.ZERO, 0.35F, state.glowColor, state.glow);
            }
            case MOTOR -> {
                Vec3 nozzle = back.scale(MOTOR_OFFSET);
                Glow.halo(pose, collector, camera, nozzle, 1.1F, state.glowColor, state.glow);
                Glow.halo(pose, collector, camera, nozzle, 0.4F, GlowParticleOptions.FLASH, state.glow);
            }
            default -> {
            }
        }
    }
}
