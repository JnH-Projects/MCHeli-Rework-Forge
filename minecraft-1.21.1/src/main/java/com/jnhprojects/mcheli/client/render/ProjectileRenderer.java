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
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

/**
 * Draws rounds, rockets, missiles and bombs from {@code models/bullets}, tinted with the weapon's
 * {@code BulletColor} (its in-water colour below the surface) and always fully lit, as the
 * original's {@code MCH_RenderBulletBase} did.
 *
 * <p>Not in the original: rounds trail a tracer and rocket motors burn with a glow, faint by day
 * and bright at night, so a firefight in the dark can be followed by eye. Nothing glows under
 * water.
 */
public class ProjectileRenderer extends EntityRenderer<ProjectileEntity> {
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

    /** What 26.x splits off as the render state. */
    static class State {
        double x;
        double y;
        double z;
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

    private final State state = new State();

    private static void extractRenderState(ProjectileEntity entity, State state, float partialTicks) {
        state.x = Mth.lerp(partialTicks, entity.xo, entity.getX());
        state.y = Mth.lerp(partialTicks, entity.yo, entity.getY());
        state.z = Mth.lerp(partialTicks, entity.zo, entity.getZ());
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
    public void render(ProjectileEntity entity, float entityYaw, float partialTicks, PoseStack pose, MultiBufferSource buffers, int light) {
        extractRenderState(entity, state, partialTicks);
        Mesh mesh = state.visible ? MeshCache.INSTANCE.find("bullets", state.model) : null;
        if (mesh != null) {
            ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(MCHeli.MOD_ID, "textures/bullets/" + state.model + ".png");
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(-state.yaw));
            pose.mulPose(Axis.XP.rotationDegrees(state.pitch));
            MeshRenderer.emit(mesh.groups(), pose.last(), buffers.getBuffer(RenderType.entityTranslucentCull(texture)), FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY, state.color);
            pose.popPose();
        }
        if (state.visible) {
            submitGlow(state, pose, buffers);
        }
        super.render(entity, entityYaw, partialTicks, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(ProjectileEntity entity) {
        return ResourceLocation.fromNamespaceAndPath(MCHeli.MOD_ID, "textures/bullets/" + entity.model() + ".png");
    }

    private Quaternionf camera() {
        return entityRenderDispatcher.cameraOrientation();
    }

    private void submitGlow(State state, PoseStack pose, MultiBufferSource buffers) {
        if (state.glowKind == GlowKind.NONE || state.glow <= 0.0F) {
            return;
        }
        double speed = state.travel.length();
        Vec3 back = speed > 1.0E-3 ? state.travel.scale(-1.0 / speed) : Vec3.ZERO;
        switch (state.glowKind) {
            case TRACER -> {
                Vec3 cameraPos = entityRenderDispatcher.camera.getPosition();
                Vec3 toCamera = new Vec3(state.x - cameraPos.x, state.y - cameraPos.y, state.z - cameraPos.z);
                Glow.streak(pose, buffers, toCamera, back.scale(Math.min(speed, MAX_TRACER)), 0.08F, state.glowColor, state.glow);
                Glow.halo(pose, buffers, camera(), Vec3.ZERO, 0.35F, state.glowColor, state.glow);
            }
            case MOTOR -> {
                Vec3 nozzle = back.scale(MOTOR_OFFSET);
                Glow.halo(pose, buffers, camera(), nozzle, 1.1F, state.glowColor, state.glow);
                Glow.halo(pose, buffers, camera(), nozzle, 0.4F, GlowParticleOptions.FLASH, state.glow);
            }
            default -> {
            }
        }
    }
}
