package com.jnhprojects.mcheli.client.render;

import com.jnhprojects.mcheli.MCHeli;
import com.jnhprojects.mcheli.client.RuntimeAssets;
import com.jnhprojects.mcheli.model.Mesh;
import com.jnhprojects.mcheli.model.MeshLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lazily loads vehicle models, from the resource packs (so they can replace them) or else from the
 * runtime roots of {@link RuntimeAssets}. Models are parsed the first time a vehicle of that kind
 * is drawn, and dropped on resource reload and on content reload ({@link #clear}).
 */
public final class MeshCache implements ResourceManagerReloadListener {
    public static final MeshCache INSTANCE = new MeshCache();
    private static final Logger LOGGER = LoggerFactory.getLogger(MeshCache.class);
    private static final String[] EXTENSIONS = {".mqo", ".obj"};

    private final Map<String, Optional<Mesh>> meshes = new ConcurrentHashMap<>();

    private MeshCache() {
    }

    /**
     * @param folder content folder, for example {@code helicopters}, or empty for the models root
     * @param name   definition name
     * @return the mesh, or {@code null} if no model exists or it failed to load
     */
    public @Nullable Mesh get(String folder, String name) {
        return meshes.computeIfAbsent(folder + '/' + name, key -> load(folder, name, true)).orElse(null);
    }

    /** Like {@link #get}, for optional models such as separate part files; a missing one is not logged. */
    public @Nullable Mesh find(String folder, String name) {
        return meshes.computeIfAbsent(folder + '/' + name, key -> load(folder, name, false)).orElse(null);
    }

    private static Optional<Mesh> load(String folder, String name, boolean required) {
        for (String extension : EXTENSIONS) {
            Identifier id = Identifier.fromNamespaceAndPath(MCHeli.MOD_ID,
                    (folder.isEmpty() ? "models/" : "models/" + folder + "/") + name + extension);
            try (InputStream in = RuntimeAssets.open(id)) {
                if (in == null) {
                    continue;
                }
                Mesh mesh = MeshLoader.load(id.toString(), in);
                LOGGER.debug("Loaded {}", mesh);
                return Optional.of(mesh);
            } catch (IOException | RuntimeException e) {
                LOGGER.error("Failed to load model {}", id, e);
                return Optional.empty();
            }
        }
        if (required) {
            LOGGER.warn("No model found for {}/{}", folder, name);
        }
        return Optional.empty();
    }

    /** Forgets every model and glass split, so the next frame reads them again from disk. */
    public void clear() {
        meshes.clear();
        GlassSplit.INSTANCE.clear();
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        clear();
        RuntimeAssets.clearTextures();
    }
}
