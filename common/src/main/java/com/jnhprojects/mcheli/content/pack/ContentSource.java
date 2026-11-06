package com.jnhprojects.mcheli.content.pack;

import org.jspecify.annotations.Nullable;

import java.nio.file.Path;

/**
 * One place definitions are read from: the {@code assets/mcheli} folder of a content pack, or of
 * the imported 1.0.3 content.
 *
 * @param pack     id of the pack, {@link #ORIGINAL} for the import
 * @param assets   the {@code assets/mcheli} folder, possibly inside a zip file system; it need not
 *                 exist
 * @param manifest what the pack says about itself, for crediting its author, or {@code null} for
 *                 the import
 */
public record ContentSource(String pack, Path assets, @Nullable PackManifest manifest) {
    /** Pack id of the imported 1.0.3 content. No content pack may use it. */
    public static final String ORIGINAL = "original";

    public ContentSource(String pack, Path assets) {
        this(pack, assets, null);
    }

    public boolean isOriginal() {
        return ORIGINAL.equals(pack);
    }
}
