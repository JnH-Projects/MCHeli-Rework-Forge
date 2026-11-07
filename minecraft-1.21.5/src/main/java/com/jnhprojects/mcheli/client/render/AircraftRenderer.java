package com.jnhprojects.mcheli.client.render;

import com.jnhprojects.mcheli.MCHeli;
import com.jnhprojects.mcheli.client.RuntimeAssets;
import com.jnhprojects.mcheli.content.aircraft.AircraftDefinition;
import com.jnhprojects.mcheli.content.aircraft.HelicopterDefinition;
import com.jnhprojects.mcheli.content.aircraft.RepellingHook;
import com.jnhprojects.mcheli.content.aircraft.RotorDefinition;
import com.jnhprojects.mcheli.content.aircraft.SearchLight;
import com.jnhprojects.mcheli.content.aircraft.part.WeaponPart;
import com.jnhprojects.mcheli.entity.AircraftEntity;
import com.jnhprojects.mcheli.entity.AnimationState;
import com.jnhprojects.mcheli.entity.HelicopterEntity;
import com.jnhprojects.mcheli.entity.PartState;
import com.jnhprojects.mcheli.entity.PlaneEntity;
import com.jnhprojects.mcheli.model.Mesh;
import com.jnhprojects.mcheli.weapon.VehicleWeapons;
import com.jnhprojects.mcheli.weapon.WeaponSet;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Draws any MCHeli vehicle from its MQO/OBJ model, following {@code MCH_RenderAircraft}: the hull,
 * the kind's own moving parts (rotors, wings, gun mounts) and then the parts every kind can have.
 */
public class AircraftRenderer<T extends AircraftEntity> extends EntityRenderer<T, AircraftRenderState> {
    private static final float[] HULL_COLOR = {0.0F, 0.88F, 1.0F};
    private static final float[] GENERATED_COLOR = {1.0F, 0.75F, 0.0F};
    /** Ropes take the colour of brown wool. */
    private static final ResourceLocation ROPE = ResourceLocation.withDefaultNamespace("textures/block/brown_wool.png");
    private static final float ROPE_WIDTH = 0.03F;

    public AircraftRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 2.0F;
    }

    /**
     * With F3+B on, outlines the hull boxes next to vanilla's main box: what projectiles hit and
     * what other vehicles collide with. The definition's own boxes are cyan, the ones generated
     * from the model amber.
     */
    private static void showHull(AircraftRenderState state, PoseStack pose, MultiBufferSource buffers) {
        if (state.hull.isEmpty()) {
            return;
        }
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        for (int i = 0; i < state.hull.size(); i++) {
            float[] c = i < state.hullListed ? HULL_COLOR : GENERATED_COLOR;
            ShapeRenderer.renderLineBox(pose, lines, state.hull.get(i), c[0], c[1], c[2], 1.0F);
        }
    }

    @Override
    public AircraftRenderState createRenderState() {
        return new AircraftRenderState();
    }

    @Override
    public void extractRenderState(T entity, AircraftRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        AircraftDefinition def = entity.definition();
        state.definition = def;
        state.texture = entity.texture();
        state.yaw = entity.getYRot(partialTicks);
        state.pitch = entity.getXRot(partialTicks);
        state.roll = entity.roll(partialTicks);
        if (def == null) {
            return;
        }
        state.hull.clear();
        if (entityRenderDispatcher.shouldRenderHitBoxes()) {
            // The first is the main box, which vanilla draws itself; then the listed boxes, then the generated ones.
            List<AABB> hull = entity.hullBoxes();
            Vec3 offset = entity.getPosition(partialTicks);
            for (int i = 1; i < hull.size(); i++) {
                state.hull.add(hull.get(i).move(-offset.x, -offset.y, -offset.z).move(offset.subtract(entity.position())));
            }
            state.hullListed = def.boundingBoxes.size();
        }

        state.rotorAngle = switch (entity) {
            case HelicopterEntity heli -> heli.rotorAngle(partialTicks);
            case PlaneEntity plane -> plane.rotorAngle(partialTicks);
            default -> 0.0F;
        };
        state.canopy = angle(entity.canopy(), partialTicks);
        state.landingGear = angle(entity.landingGear(), partialTicks);
        state.hatch = angle(entity.hatch(), partialTicks);
        if (entity instanceof HelicopterEntity heli && def instanceof HelicopterDefinition heliDef) {
            bladeAngles(heli, heliDef, state, partialTicks);
        }
        state.ropeLength = entity.isRepelling() ? entity.ropeLength() : 0.0F;
        state.searchLight = entity.isSearchLightOn();
        if (state.searchLight) {
            Entity aimer = entity.searchLightAimer();
            state.searchLightYaw = aimer != null ? aimer.getYRot(partialTicks) : state.yaw;
            state.searchLightPitch = aimer != null ? aimer.getXRot(partialTicks) : state.pitch;
        }
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
        state.glassColor = glassColor(entity, getPackedLightCoords(entity, partialTicks));
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
    protected AABB getBoundingBoxForCulling(T entity) {
        // The hit box is far smaller than most models, so cull on a generous box instead.
        AircraftDefinition def = entity.definition();
        double reach = def != null ? Math.max(8.0, def.markerWidth * 4.0) : 8.0;
        return entity.getBoundingBox().inflate(reach, reach / 2.0, reach);
    }

    @Override
    public void render(AircraftRenderState state, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
        draw(state, poseStack, buffers, packedLight);
        super.render(state, poseStack, buffers, packedLight);
    }

    /** Draws the vehicle, without the name tag and the like a world entity also gets. The workshop preview uses this. */
    public static void draw(AircraftRenderState state, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
        AircraftDefinition def = state.definition;
        showHull(state, poseStack, buffers);
        Mesh mesh = def != null ? MeshCache.INSTANCE.get(def.folder(), def.name()) : null;
        if (mesh != null) {
            // Canopies and windshields are semi-transparent texels; PartRenderer draws those
            // triangles blended, as the original did, and everything else as cutout.
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(-state.yaw));
            poseStack.mulPose(Axis.XP.rotationDegrees(state.pitch));
            poseStack.mulPose(Axis.ZP.rotationDegrees(state.roll));
            new PartRenderer(def, mesh, state, poseStack, buffers, texture(def.folder(), state.texture), packedLight,
                    OverlayTexture.NO_OVERLAY, state.glassColor).submitAll();
            if (state.ropeLength < 0.0F) {
                ropes(def, state, poseStack, buffers, packedLight);
            }
            if (state.searchLight) {
                searchLights(def, state, poseStack, buffers);
            }
            poseStack.popPose();
        }
    }

    /**
     * {@code renderLight}: each lamp's cone. A turning light follows the crew's aim; a fixed one
     * points along the vehicle, and a steering one also turns with the front wheels.
     */
    private static void searchLights(AircraftDefinition def, AircraftRenderState state, PoseStack pose, MultiBufferSource buffers) {
        for (SearchLight light : def.searchLights) {
            pose.pushPose();
            pose.translate(light.position().x(), light.position().y(), light.position().z());
            if (!light.fixed()) {
                pose.mulPose(Axis.YN.rotationDegrees(state.searchLightYaw - state.yaw + light.yaw()));
                pose.mulPose(Axis.XP.rotationDegrees(state.searchLightPitch + 90.0F - state.pitch + light.pitch()));
            } else {
                float steer = light.steering() ? -state.steer * light.steeringRotation() : 0.0F;
                pose.mulPose(Axis.YN.rotationDegrees(light.yaw() + steer));
                pose.mulPose(Axis.XP.rotationDegrees(90.0F + light.pitch()));
            }
            Glow.cone(pose, buffers, light.height(), light.width() / 2.0F, light.colorStart(), light.colorEnd());
            pose.popPose();
        }
    }

    private static void bladeAngles(HelicopterEntity heli, HelicopterDefinition def, AircraftRenderState state, float partialTicks) {
        if (state.bladeAngles.length != def.rotors.size()) {
            state.bladeAngles = new float[def.rotors.size()][];
        }
        for (int r = 0; r < def.rotors.size(); r++) {
            RotorDefinition rotor = def.rotors.get(r);
            int blades = Math.max(rotor.blades(), 1);
            int step = rotor.bladeStep() > 0 ? rotor.bladeStep() : 1;
            if (state.bladeAngles[r] == null || state.bladeAngles[r].length != blades) {
                state.bladeAngles[r] = new float[blades];
            }
            for (int b = 0; b < blades; b++) {
                state.bladeAngles[r][b] = heli.bladeAngle(r, b, step, partialTicks);
            }
        }
    }

    /** {@code renderRope}: a dark line straight down from each hook, in the vehicle's own frame. */
    private static void ropes(AircraftDefinition def, AircraftRenderState state, PoseStack pose, MultiBufferSource buffers, int light) {
        float length = state.ropeLength;
        Geometry.submit(buffers, pose, RenderType.entityCutoutNoCull(ROPE), (p, buffer) -> {
            for (RepellingHook hook : def.repellingHooks) {
                float x = (float) hook.position().x();
                float y = (float) hook.position().y();
                float z = (float) hook.position().z();
                // Two thin crossed strips, so the rope shows from every side.
                ropeStrip(p, buffer, x - ROPE_WIDTH, z, x + ROPE_WIDTH, z, y, length, light);
                ropeStrip(p, buffer, x, z - ROPE_WIDTH, x, z + ROPE_WIDTH, y, length, light);
            }
        });
    }

    private static void ropeStrip(PoseStack.Pose p, VertexConsumer buffer, float x0, float z0, float x1, float z1, float y, float length, int light) {
        ropeVertex(p, buffer, x0, y, z0, 0.0F, 0.0F, light);
        ropeVertex(p, buffer, x1, y, z1, 1.0F, 0.0F, light);
        ropeVertex(p, buffer, x1, y + length, z1, 1.0F, 1.0F, light);
        ropeVertex(p, buffer, x0, y + length, z0, 0.0F, 1.0F, light);
    }

    private static void ropeVertex(PoseStack.Pose p, VertexConsumer buffer, float x, float y, float z, float u, float v, int light) {
        buffer.addVertex(p, x, y, z).setColor(0xFFFFFFFF).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, 0.0F, 1.0F, 0.0F);
    }

    private static ResourceLocation texture(String folder, String name) {
        return RuntimeAssets.texture(ResourceLocation.fromNamespaceAndPath(MCHeli.MOD_ID, "textures/" + folder + "/" + name + ".png"));
    }
}
