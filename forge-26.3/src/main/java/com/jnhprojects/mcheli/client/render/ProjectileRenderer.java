package com.jnhprojects.mcheli.client.render;

import com.jnhprojects.mcheli.MCHeli;
import com.jnhprojects.mcheli.content.weapon.WeaponColor;
import com.jnhprojects.mcheli.content.weapon.WeaponDefinition;
import com.jnhprojects.mcheli.entity.projectile.BombEntity;
import com.jnhprojects.mcheli.entity.projectile.ProjectileEntity;
import com.jnhprojects.mcheli.model.Mesh;
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

/**
 * Draws rounds, rockets, missiles and bombs from {@code models/bullets}, tinted with the weapon's
 * {@code BulletColor} (its in-water colour below the surface) and always fully lit, as the
 * original's {@code MCH_RenderBulletBase} did.
 */
public class ProjectileRenderer extends EntityRenderer<ProjectileEntity, ProjectileRenderer.State> {
    private static final int FULL_BRIGHT = 0xF000F0;

    public ProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    public static class State extends EntityRenderState {
        String model = "";
        float yaw;
        float pitch;
        int color = 0xFFFFFFFF;
        boolean visible = true;
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
        WeaponColor c = w != null ? (inWater(entity.level(), entity) ? w.colorInWater : w.color) : WeaponColor.WHITE;
        state.color = c.argb();
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
        super.submit(state, pose, collector, camera);
    }
}
