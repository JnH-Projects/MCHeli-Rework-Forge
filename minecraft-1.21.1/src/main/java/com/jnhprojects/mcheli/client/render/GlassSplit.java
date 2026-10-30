package com.jnhprojects.mcheli.client.render;

import com.jnhprojects.mcheli.client.RuntimeAssets;
import com.jnhprojects.mcheli.model.MeshGroup;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Sorts a model's triangles into opaque ones and see-through glass, by the alpha of the texture
 * under each triangle.
 *
 * <p>Drawing a whole vehicle translucent made its glass write depth before the game drew water,
 * so water behind a canopy vanished. Opaque triangles now draw as cutout, like any entity, and
 * only the glass draws translucent, through a type that leaves the depth buffer alone.
 */
final class GlassSplit {
    static final GlassSplit INSTANCE = new GlassSplit();
    private static final Logger LOGGER = LoggerFactory.getLogger(GlassSplit.class);
    /** Texels between these alphas are glass; fully clear ones are cut out, solid ones opaque. */
    private static final int MIN_GLASS_ALPHA = 10;
    private static final int MAX_GLASS_ALPHA = 245;

    record Split(List<MeshGroup> opaque, List<MeshGroup> glass) {
        static final Split EMPTY = new Split(List.of(), List.of());
    }

    private record Alpha(int width, int height, byte[] values) {
        int at(float u, float v) {
            int x = Math.floorMod((int) Math.floor(u * width), width);
            int y = Math.floorMod((int) Math.floor(v * height), height);
            return values[y * width + x] & 0xFF;
        }
    }

    private final Map<ResourceLocation, Optional<Alpha>> textures = new HashMap<>();
    private final Map<List<MeshGroup>, Map<ResourceLocation, Split>> splits = new IdentityHashMap<>();

    private GlassSplit() {
    }

    /** Called from the render thread only. */
    Split split(List<MeshGroup> groups, ResourceLocation texture) {
        if (groups.isEmpty()) {
            return Split.EMPTY;
        }
        return splits.computeIfAbsent(groups, g -> new HashMap<>()).computeIfAbsent(texture, t -> compute(groups, t));
    }

    void clear() {
        textures.clear();
        splits.clear();
    }

    private Split compute(List<MeshGroup> groups, ResourceLocation texture) {
        Alpha alpha = textures.computeIfAbsent(texture, GlassSplit::load).orElse(null);
        if (alpha == null) {
            return new Split(groups, List.of());
        }
        List<MeshGroup> opaque = new ArrayList<>();
        List<MeshGroup> glass = new ArrayList<>();
        for (MeshGroup group : groups) {
            split(group, alpha, opaque, glass);
        }
        return new Split(List.copyOf(opaque), List.copyOf(glass));
    }

    private static void split(MeshGroup group, Alpha alpha, List<MeshGroup> opaque, List<MeshGroup> glass) {
        float[] uv = group.uvs();
        int triangles = group.vertexCount() / 3;
        boolean[] isGlass = new boolean[triangles];
        int glassCount = 0;
        for (int t = 0; t < triangles; t++) {
            int i = t * 6;
            float u = (uv[i] + uv[i + 2] + uv[i + 4]) / 3.0F;
            float v = (uv[i + 1] + uv[i + 3] + uv[i + 5]) / 3.0F;
            int a = alpha.at(u, v);
            if (a > MIN_GLASS_ALPHA && a < MAX_GLASS_ALPHA) {
                isGlass[t] = true;
                glassCount++;
            }
        }
        if (glassCount == 0) {
            opaque.add(group);
        } else if (glassCount == triangles) {
            glass.add(group);
        } else {
            opaque.add(pick(group, isGlass, false, triangles - glassCount));
            glass.add(pick(group, isGlass, true, glassCount));
        }
    }

    private static MeshGroup pick(MeshGroup group, boolean[] isGlass, boolean wanted, int count) {
        float[] p = new float[count * 9];
        float[] n = new float[count * 9];
        float[] uv = new float[count * 6];
        int out = 0;
        for (int t = 0; t < isGlass.length; t++) {
            if (isGlass[t] != wanted) {
                continue;
            }
            System.arraycopy(group.positions(), t * 9, p, out * 9, 9);
            System.arraycopy(group.normals(), t * 9, n, out * 9, 9);
            System.arraycopy(group.uvs(), t * 6, uv, out * 6, 6);
            out++;
        }
        return new MeshGroup(group.name(), p, n, uv);
    }

    private static Optional<Alpha> load(ResourceLocation texture) {
        try (InputStream in = RuntimeAssets.open(texture)) {
            if (in == null) {
                return Optional.empty();
            }
            return Optional.of(read(in));
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("Could not read {} to find its glass; drawing it opaque", texture, e);
            return Optional.empty();
        }
    }

    private static Alpha read(InputStream in) throws IOException {
        try (NativeImage image = NativeImage.read(in)) {
            int w = image.getWidth();
            int h = image.getHeight();
            byte[] values = new byte[w * h];
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    values[y * w + x] = (byte) (image.getPixelRGBA(x, y) >>> 24);
                }
            }
            return new Alpha(w, h, values);
        }
    }
}
