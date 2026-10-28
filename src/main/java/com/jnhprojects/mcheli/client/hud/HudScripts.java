package com.jnhprojects.mcheli.client.hud;

import com.jnhprojects.mcheli.MCHeli;
import com.jnhprojects.mcheli.hud.HudFormatException;
import com.jnhprojects.mcheli.hud.HudScript;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * The HUD scripts in {@code assets/mcheli/hud}, read through the resource manager so resource
 * packs can replace or add them, and reloaded with F3 + T. A script that fails to parse is logged
 * and left out; the vehicles using it fall back to the plain readout.
 */
public final class HudScripts implements ResourceManagerReloadListener {
    public static final HudScripts INSTANCE = new HudScripts();
    private static final Logger LOGGER = LoggerFactory.getLogger(HudScripts.class);
    private static final String FOLDER = "hud";
    private static final int DEFAULT_TEXTURE_SIZE = 256;

    private Map<String, HudScript> scripts = Map.of();
    private final Map<Identifier, int[]> textureSizes = new HashMap<>();

    private HudScripts() {
    }

    public @Nullable HudScript get(String name) {
        return scripts.get(name.toLowerCase(Locale.ROOT));
    }

    @Override
    public void onResourceManagerReload(ResourceManager resources) {
        Map<String, HudScript> loaded = new HashMap<>();
        resources.listResources(FOLDER, id -> id.getNamespace().equals(MCHeli.MOD_ID) && id.getPath().endsWith(".txt"))
                .forEach((id, resource) -> {
                    String path = id.getPath();
                    String name = path.substring(FOLDER.length() + 1, path.length() - 4).toLowerCase(Locale.ROOT);
                    try (Reader in = resource.openAsReader()) {
                        loaded.put(name, HudScript.parse(name, in));
                    } catch (IOException | HudFormatException e) {
                        LOGGER.error("Skipping HUD {}: {}", id, e.getMessage());
                    }
                });
        scripts = Map.copyOf(loaded);
        textureSizes.clear();
        LOGGER.info("Loaded {} HUD scripts", scripts.size());
    }

    /**
     * Width and height of a GUI texture in pixels, read from the PNG header once. The original
     * fell back to 256 when it could not tell, and so does this.
     */
    int[] textureSize(ResourceManager resources, Identifier texture) {
        return textureSizes.computeIfAbsent(texture, id -> readSize(resources.getResource(id)));
    }

    private static int[] readSize(Optional<Resource> resource) {
        if (resource.isPresent()) {
            try (InputStream in = resource.get().open(); DataInputStream data = new DataInputStream(in)) {
                // 8 byte signature, IHDR length and type, then width and height as big endian ints.
                data.skipNBytes(16);
                int width = data.readInt();
                int height = data.readInt();
                if (width > 0 && height > 0) {
                    return new int[]{width, height};
                }
            } catch (IOException e) {
                LOGGER.warn("Could not read the size of a HUD texture", e);
            }
        }
        return new int[]{DEFAULT_TEXTURE_SIZE, DEFAULT_TEXTURE_SIZE};
    }
}
