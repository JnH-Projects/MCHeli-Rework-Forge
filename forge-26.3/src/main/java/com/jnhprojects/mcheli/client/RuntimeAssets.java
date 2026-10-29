package com.jnhprojects.mcheli.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Vehicle textures, models and sounds that can change while the game runs, without a resource
 * reload (F3 + T), which would stall the game for every pack loaded and is not available on a
 * server at all.
 *
 * <p>Lookups try the resource manager first, so resource packs keep the last word, then the
 * runtime roots: folders laid out like {@code assets/mcheli} that content can add at any time.
 * Content packs and, later, the workshop register their folders here.
 * <ul>
 *   <li>Textures found only in a runtime root are uploaded as a {@link DynamicTexture} the first
 *       time a vehicle draws them. {@link #refreshTextures} uploads every vehicle texture again
 *       from disk, which is how a content reload shows edited textures.</li>
 *   <li>Models are read through {@link #open} by the mesh cache, which a reload clears.</li>
 *   <li>Sounds found only in a runtime root are streamed from the file
 *       ({@link #soundFile}); see {@code RuntimeSoundInstance}.</li>
 * </ul>
 */
public final class RuntimeAssets {
    private static final Logger LOGGER = LoggerFactory.getLogger(RuntimeAssets.class);

    /** Searched in order, so the pack loaded last comes first. Replaced as a whole. */
    private static volatile List<Path> roots = List.of();
    /** Every vehicle texture asked for, and whether we uploaded it ourselves. */
    private static final Map<Identifier, Boolean> TEXTURES = new ConcurrentHashMap<>();

    private RuntimeAssets() {
    }

    /**
     * Sets the folders laid out like {@code assets/mcheli} that are searched after the resource
     * packs, the first one first. Content packs set theirs whenever the packs in use change.
     */
    public static void setRoots(List<Path> folders) {
        roots = List.copyOf(folders);
    }

    /** Opens an {@code mcheli} asset from the resource packs, or else from a runtime root. */
    public static @Nullable InputStream open(Identifier id) throws IOException {
        Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(id);
        if (resource.isPresent()) {
            return resource.get().open();
        }
        Path file = find(id.getPath());
        return file != null ? Files.newInputStream(file) : null;
    }

    /**
     * A texture ready to bind. The first request for a texture no resource pack has, but a runtime
     * root does, uploads it. Cheap after the first call, so renderers can call it every frame.
     */
    public static Identifier texture(Identifier id) {
        TEXTURES.computeIfAbsent(id, key -> {
            if (Minecraft.getInstance().getResourceManager().getResource(key).isPresent()) {
                return false;
            }
            return upload(key);
        });
        return id;
    }

    /**
     * Uploads every texture vehicles have drawn so far again, read fresh from disk. Resource pack
     * textures are replaced too, until the next resource reload loads them the normal way. One that
     * no longer exists anywhere, because its pack is gone, is released.
     */
    public static void refreshTextures() {
        for (Map.Entry<Identifier, Boolean> entry : TEXTURES.entrySet()) {
            boolean ours = upload(entry.getKey());
            if (!ours && entry.getValue()) {
                Minecraft.getInstance().getTextureManager().release(entry.getKey());
            }
            entry.setValue(ours);
        }
    }

    private static boolean upload(Identifier id) {
        try (InputStream in = open(id)) {
            if (in == null) {
                return false;
            }
            NativeImage image = NativeImage.read(in);
            Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(id::toString, image));
            return true;
        } catch (IOException e) {
            LOGGER.warn("Could not load texture {}", id, e);
            return false;
        }
    }

    /**
     * Drops the textures we uploaded, so the texture manager loads them again the normal way.
     * Called on resource reload.
     */
    public static void clearTextures() {
        TEXTURES.forEach((id, ours) -> {
            if (ours) {
                Minecraft.getInstance().getTextureManager().release(id);
            }
        });
        TEXTURES.clear();
    }

    /**
     * The OGG file for a sound event of ours that no resource pack defines, such as {@code heli}
     * from {@code sounds/heli.ogg} in a runtime root. Numbered variants ({@code heli1.ogg}) count.
     */
    public static @Nullable Path soundFile(String event) {
        Path file = find("sounds/" + event + ".ogg");
        return file != null ? file : find("sounds/" + event + "1.ogg");
    }

    private static @Nullable Path find(String path) {
        for (Path root : roots) {
            Path file = root.resolve(path).normalize();
            if (file.startsWith(root) && Files.isRegularFile(file)) {
                return file;
            }
        }
        return null;
    }
}
