package com.jnhprojects.mcheli.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Reader for Wavefront {@code .obj} files.
 *
 * <p>Follows {@code W_WavefrontObject}: {@code g} always starts a new group, {@code o} only does
 * so when its name begins with {@code $}. Texture V is flipped, positions are used unscaled, and
 * quads are split into two triangles. Normals from the file are used when present, otherwise the
 * face normal is computed.
 */
public final class ObjLoader {
    private ObjLoader() {
    }

    public static Mesh load(String source, InputStream in) throws IOException {
        List<float[]> positions = new ArrayList<>();
        List<float[]> normals = new ArrayList<>();
        List<float[]> uvs = new ArrayList<>();
        List<MeshGroup> groups = new ArrayList<>();

        String groupName = "Default";
        TriangleBuilder builder = null;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.ISO_8859_1))) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.replaceAll("\\s+", " ").trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                String[] t = line.split(" ");
                switch (t[0]) {
                    case "v" -> positions.add(floats(t, 3));
                    case "vn" -> normals.add(floats(t, 3));
                    case "vt" -> {
                        float[] uv = floats(t, 2);
                        uv[1] = 1.0F - uv[1];
                        uvs.add(uv);
                    }
                    case "g", "o" -> {
                        if (t.length < 2 || (t[0].equals("o") && !t[1].startsWith("$"))) {
                            continue;
                        }
                        if (builder != null) {
                            groups.add(builder.build(groupName));
                        }
                        groupName = t[1];
                        builder = new TriangleBuilder();
                    }
                    case "f" -> {
                        if (builder == null) {
                            builder = new TriangleBuilder();
                        }
                        if (t.length != 4 && t.length != 5) {
                            throw new ModelFormatException("%s:%d only triangles and quads are supported".formatted(source, lineNumber));
                        }
                        addFace(builder, t, positions, normals, uvs);
                    }
                    default -> {
                        // mtllib, usemtl, s and friends carry nothing the renderer needs.
                    }
                }
            }
        }
        if (builder != null) {
            groups.add(builder.build(groupName));
        }
        return new Mesh(source, groups);
    }

    private static void addFace(TriangleBuilder builder, String[] t, List<float[]> positions, List<float[]> normals, List<float[]> uvs) {
        int corners = t.length - 1;
        float[][] p = new float[corners][];
        float[][] n = new float[corners][];
        float[][] uv = new float[corners][];
        for (int i = 0; i < corners; i++) {
            String[] parts = t[i + 1].split("/", -1);
            p[i] = positions.get(index(parts[0], positions.size()));
            uv[i] = parts.length > 1 && !parts[1].isEmpty() ? uvs.get(index(parts[1], uvs.size())) : new float[2];
            n[i] = parts.length > 2 && !parts[2].isEmpty() ? normals.get(index(parts[2], normals.size())) : null;
        }
        emit(builder, p, n, uv, 0, 1, 2);
        if (corners == 4) {
            emit(builder, p, n, uv, 0, 2, 3);
        }
    }

    private static void emit(TriangleBuilder builder, float[][] p, float[][] n, float[][] uv, int a, int b, int c) {
        float[] face = TriangleBuilder.faceNormal(p[a], p[b], p[c]);
        for (int i : new int[]{a, b, c}) {
            float[] normal = n[i] != null ? n[i] : face;
            builder.vertex(p[i][0], p[i][1], p[i][2], normal[0], normal[1], normal[2], uv[i][0], uv[i][1]);
        }
    }

    /** OBJ indices are one based and may be negative, counting back from the last element. */
    private static int index(String token, int size) {
        int i = Integer.parseInt(token);
        return i < 0 ? size + i : i - 1;
    }

    private static float[] floats(String[] t, int count) {
        float[] out = new float[count];
        for (int i = 0; i < count && i + 1 < t.length; i++) {
            out[i] = Float.parseFloat(t[i + 1]);
        }
        return out;
    }
}
