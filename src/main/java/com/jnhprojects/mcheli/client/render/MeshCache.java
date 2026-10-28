package com.jnhprojects.mcheli.client.render;

import com.jnhprojects.mcheli.MCHeli;
import com.jnhprojects.mcheli.model.Mesh;
import com.jnhprojects.mcheli.model.MeshLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
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
 * Lazily loads vehicle models through the resource manager, so resource packs can replace them.
 * Models are parsed the first time a vehicle of that kind is drawn and dropped on resource reload.
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
        ResourceManager resources = Minecraft.getInstance().getResourceManager();
        for (String extension : EXTENSIONS) {
            Identifier id = Identifier.fromNamespaceAndPath(MCHeli.MOD_ID,
                    (folder.isEmpty() ? "models/" : "models/" + folder + "/") + name + extension);
            Optional<Resource> resource = resources.getResource(id);
            if (resource.isEmpty()) {
                continue;
            }
            try (InputStream in = resource.get().open()) {
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

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        meshes.clear();
        GlassSplit.INSTANCE.clear();
    }
}
