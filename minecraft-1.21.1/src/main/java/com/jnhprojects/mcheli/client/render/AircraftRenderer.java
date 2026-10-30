package com.jnhprojects.mcheli.client.render;

import com.jnhprojects.mcheli.MCHeli;
import com.jnhprojects.mcheli.client.RuntimeAssets;
import com.jnhprojects.mcheli.content.aircraft.AircraftDefinition;
import com.jnhprojects.mcheli.content.aircraft.part.WeaponPart;
import com.jnhprojects.mcheli.weapon.VehicleWeapons;
import com.jnhprojects.mcheli.weapon.WeaponSet;
import com.jnhprojects.mcheli.entity.AircraftEntity;
import com.jnhprojects.mcheli.entity.AnimationState;
import com.jnhprojects.mcheli.entity.HelicopterEntity;
import com.jnhprojects.mcheli.entity.PartState;
import com.jnhprojects.mcheli.entity.PlaneEntity;
import com.jnhprojects.mcheli.model.Mesh;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * Draws any MCHeli vehicle from its MQO/OBJ model, following {@code MCH_RenderAircraft}: the hull,
 * the kind's own moving parts (rotors, wings, gun mounts) and then the parts every kind can have.
 */
public class AircraftRenderer<T extends AircraftEntity> extends EntityRenderer<T> {
    /** Reused every frame; rendering is single threaded. */
    private final AircraftRenderState state = new AircraftRenderState();

    public AircraftRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 2.0F;
    }

    /** What 26.x splits off as the render state: everything the frame needs, read from the entity. */
    private void extractRenderState(T entity, AircraftRenderState state, float partialTicks, int light) {
        state.lightCoords = light;
        AircraftDefinition def = entity.definition();
        state.definition = def;
        state.texture = entity.texture();
        state.yaw = entity.getYRot(partialTicks);
        state.pitch = entity.getXRot(partialTicks);
        state.roll = entity.roll(partialTicks);
        if (def == null) {
            return;
        }

        state.rotorAngle = switch (entity) {
            case HelicopterEntity heli -> heli.rotorAngle(partialTicks);
            case PlaneEntity plane -> plane.rotorAngle(partialTicks);
            default -> 0.0F;
        };
        state.canopy = angle(entity.canopy(), partialTicks);
        state.landingGear = angle(entity.landingGear(), partialTicks);
        state.hatch = angle(entity.hatch(), partialTicks);
        if (entity instanceof PlaneEntity plane) {
            state.nozzle = angle(plane.nozzle(), partialTicks);
            state.wing = angle(plane.wing(), partialTicks);
        } else {
            state.nozzle = 0.0F;
            state.wing = 0.0F;
        }

        AnimationState anim = entity.animation;
        state.throttle = Mth.lerp(partialTicks, anim.prevThrottle, anim.throttle);
        state.wheelRoll = Mth.lerp(partialTicks, anim.prevWheelRoll, anim.wheelRoll);
        state.steer = Mth.lerp(partialTicks, anim.prevSteer, anim.steer);
        for (int i = 0; i < 2; i++) {
            state.roller[i] = Mth.lerp(partialTicks, anim.prevRoller[i], anim.roller[i]);
            state.track[i] = lerpWrapped(anim.prevTrack[i], anim.track[i], partialTicks);
        }
        if (state.rotatingParts.length != anim.rotatingParts.length) {
            state.rotatingParts = new float[anim.rotatingParts.length];
        }
        for (int i = 0; i < anim.rotatingParts.length; i++) {
            state.rotatingParts[i] = Mth.lerp(partialTicks, anim.prevRotatingParts[i], anim.rotatingParts[i]);
        }

        int seats = def.seatAndRackCount();
        if (state.seatYaw.length != seats) {
            state.seatYaw = new float[seats];
            state.seatPitch = new float[seats];
            state.seatOccupied = new boolean[seats];
        }
        for (int i = 0; i < seats; i++) {
            state.seatYaw[i] = anim.seatYaw(i, partialTicks);
            state.seatPitch[i] = anim.seatPitch(i, partialTicks);
            state.seatOccupied[i] = entity.occupant(i) != null;
        }
        Minecraft mc = Minecraft.getInstance();
        state.localSeat = mc.player != null ? entity.seatOf(mc.player) : -1;
        state.localFirstPerson = mc.options.getCameraType().isFirstPerson();
        state.glassColor = glassColor(entity, state.lightCoords);
        extractWeapons(entity, def, state, partialTicks);
    }

    /**
     * Glass is drawn without the light map, so shade it by hand: as bright as the brighter of block
     * light and the sky light left after night or rain darkens it.
     */
    private static int glassColor(AircraftEntity entity, int lightCoords) {
        int sky = Math.max(LightTexture.sky(lightCoords) - entity.level().getSkyDarken(), 0);
        int light = Math.max(LightTexture.block(lightCoords), sky);
        int shade = (int) ((0.3F + 0.7F * light / 15.0F) * 255.0F);
        return 0xFF000000 | shade << 16 | shade << 8 | shade;
    }

    /** {@code renderWeapon}'s inputs: each part's weapon aim, recoil, barrel spin and reload state. */
    private static void extractWeapons(AircraftEntity entity, AircraftDefinition def, AircraftRenderState state, float partialTicks) {
        VehicleWeapons weapons = entity.weapons();
        int parts = def.weaponParts.size();
        if (state.weaponParts.length != parts) {
            state.weaponParts = new AircraftRenderState.WeaponPartState[parts];
            for (int i = 0; i < parts; i++) {
                state.weaponParts[i] = new AircraftRenderState.WeaponPartState();
            }
        }
        WeaponSet previous = null;
        int mount = 0;
        for (int i = 0; i < parts; i++) {
            WeaponPart part = def.weaponParts.get(i);
            AircraftRenderState.WeaponPartState out = state.weaponParts[i];
            WeaponSet set = weapons != null && !part.weapons().isEmpty() ? weapons.byName(part.weapons().getFirst()) : null;
            if (set != previous) {
                mount = 0;
                previous = set;
            }
            out.hasWeapon = set != null;
            if (set == null) {
                continue;
            }
            float yaw = set.rotationYaw;
            float prevYaw = set.prevRotationYaw;
            if (yaw - prevYaw > 180.0F) {
                prevYaw += 360.0F;
            } else if (yaw - prevYaw < -180.0F) {
                prevYaw -= 360.0F;
            }
            out.yaw = Mth.lerp(partialTicks, prevYaw, yaw) - set.defaultRotationYaw;
            out.pitch = Mth.lerp(partialTicks, set.prevRotationPitch, set.rotationPitch);
            out.defaultYaw = set.defaultRotationYaw;
            out.turretLag = set.rotationTurretYaw;
            out.barrel = Mth.lerp(partialTicks, set.prevBarrel, set.barrel);
            // A part shared by several weapons kicks back with whichever fired hardest.
            float recoil = set.recoil(0, partialTicks);
            for (String name : part.weapons()) {
                WeaponSet other = weapons.byName(name);
                if (other != null) {
                    recoil = Math.max(recoil, other.recoil(0, partialTicks));
                }
            }
            out.recoil = recoil;
            out.hidden = part.missile() && weapons.isMountUsed(weapons.indexOf(set.name()), mount);
            mount++;
        }
        if (state.bays.length != def.weaponBays.size()) {
            state.bays = new float[def.weaponBays.size()];
        }
        for (int i = 0; i < state.bays.length; i++) {
            state.bays[i] = weapons != null ? weapons.bayAngle(i, partialTicks) : 0.0F;
        }
    }

    private static float angle(@Nullable PartState part, float partialTicks) {
        return part != null ? part.angle(partialTicks) : 0.0F;
    }

    /** Interpolates a value that wraps from 1 back to 0. */
    private static float lerpWrapped(float prev, float current, float partialTicks) {
        float d = current - prev;
        if (d > 0.5F) {
            d -= 1.0F;
        } else if (d < -0.5F) {
            d += 1.0F;
        }
        float v = prev + d * partialTicks;
        return v - Mth.floor(v);
    }

    @Override
    public boolean shouldRender(T entity, Frustum frustum, double camX, double camY, double camZ) {
        return entity.shouldRender(camX, camY, camZ) && frustum.isVisible(cullingBox(entity));
    }

    /** The hit box is far smaller than most models, so cull on a generous box instead. */
    private static AABB cullingBox(AircraftEntity entity) {
        AircraftDefinition def = entity.definition();
        double reach = def != null ? Math.max(8.0, def.markerWidth * 4.0) : 8.0;
        return entity.getBoundingBox().inflate(reach, reach / 2.0, reach);
    }

    @Override
    public void render(T entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffers, int light) {
        extractRenderState(entity, state, partialTicks, light);
        AircraftDefinition def = state.definition;
        Mesh mesh = def != null ? MeshCache.INSTANCE.get(def.folder(), def.name()) : null;
        if (mesh != null) {
            // Canopies and windshields are semi-transparent texels; PartRenderer draws those
            // triangles blended, as the original did, and everything else as cutout.
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(-state.yaw));
            poseStack.mulPose(Axis.XP.rotationDegrees(state.pitch));
            poseStack.mulPose(Axis.ZP.rotationDegrees(state.roll));
            new PartRenderer(def, mesh, state, poseStack, buffers, texture(def.folder(), state.texture), state.lightCoords,
                    OverlayTexture.NO_OVERLAY, state.glassColor).submitAll();
            poseStack.popPose();
        }
        super.render(entity, entityYaw, partialTicks, poseStack, buffers, light);
    }

    /** Vehicles bind their own textures per part; this is only for vanilla's bookkeeping. */
    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return texture(entity.definition() != null ? entity.definition().folder() : "", entity.texture());
    }

    private static ResourceLocation texture(String folder, String name) {
        return RuntimeAssets.texture(ResourceLocation.fromNamespaceAndPath(MCHeli.MOD_ID, "textures/" + folder + "/" + name + ".png"));
    }
}
