package com.jnhprojects.mcheli.content.pack;

import java.nio.file.Path;

/**
 * One place definitions are read from: the {@code assets/mcheli} folder of a content pack, or of
 * the imported 1.0.3 content.
 *
 * @param pack   id of the pack, {@link #ORIGINAL} for the import
 * @param assets the {@code assets/mcheli} folder, possibly inside a zip file system; it need not
 *               exist
 */
public record ContentSource(String pack, Path assets) {
    /** Pack id of the imported 1.0.3 content. No content pack may use it. */
    public static final String ORIGINAL = "original";

    public boolean isOriginal() {
        return ORIGINAL.equals(pack);
    }
}
