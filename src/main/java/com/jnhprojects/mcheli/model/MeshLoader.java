package com.jnhprojects.mcheli.model;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

/** Picks the loader from the file extension. */
public final class MeshLoader {
    private MeshLoader() {
    }

    public static Mesh load(String fileName, InputStream in) throws IOException {
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".mqo")) {
            return MqoLoader.load(fileName, in);
        }
        if (lower.endsWith(".obj")) {
            return ObjLoader.load(fileName, in);
        }
        throw new ModelFormatException("Unsupported model format: " + fileName);
    }
}
