package com.jnhprojects.mcheli.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reader for Metasequoia text documents ({@code .mqo}), the format almost every MCHeli vehicle uses.
 *
 * <p>Behaviour matches {@code W_MetasequoiaObject} from the original mod:
 * <ul>
 *   <li>coordinates are divided by 100 (Metasequoia centimetres to blocks);</li>
 *   <li>triangle winding is reversed, quads are split into two triangles;</li>
 *   <li>objects with {@code shading 1} get smoothed normals, limited by their {@code facet} angle;</li>
 *   <li>object names shorter than two characters are skipped, a quirk some models depend on.</li>
 * </ul>
 * Materials, the scene block and hierarchy depth are ignored. Every object is drawn with the
 * vehicle texture.
 */
public final class MqoLoader {
    private static final float SCALE = 1.0F / 100.0F;
    private static final double DEFAULT_FACET_DEGREES = 45.0;
    private static final Pattern OBJECT = Pattern.compile("^Object\\s+\"([^\"]*)\"");
    private static final Pattern VERTICES = Pattern.compile("V\\(([^)]*)\\)");
    private static final Pattern UVS = Pattern.compile("UV\\(([^)]*)\\)");

    private MqoLoader() {
    }

    public static Mesh load(String source, InputStream in) throws IOException {
        List<MeshGroup> groups = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.ISO_8859_1))) {
            LineCursor cursor = new LineCursor(reader);
            String line;
            while ((line = cursor.next()) != null) {
                Matcher m = OBJECT.matcher(line);
                if (m.find() && m.group(1).length() >= 2) {
                    groups.add(readObject(source, m.group(1), cursor));
                }
            }
        }
        return new Mesh(source, groups);
    }

    /**
     * Reads one {@code Object} chunk. The cursor sits just after the {@code Object "name" {} line,
     * so the chunk body is at depth one. Nested chunks other than {@code vertex} and {@code face}
     * (for example {@code vertexattr}) are skipped by tracking braces.
     */
    private static MeshGroup readObject(String source, String name, LineCursor cursor) throws IOException {
        boolean smooth = false;
        double facetCos = Math.cos(Math.toRadians(DEFAULT_FACET_DEGREES));
        float[][] vertices = new float[0][];
        List<Face> faces = new ArrayList<>();
        int depth = 1;

        String line;
        while (depth > 0 && (line = cursor.next()) != null) {
            if (depth == 1 && line.startsWith("vertex ") && line.endsWith("{")) {
                vertices = readVertices(source, cursor, Integer.parseInt(line.split(" ")[1]));
            } else if (depth == 1 && line.startsWith("face ") && line.endsWith("{")) {
                readFaces(cursor, Integer.parseInt(line.split(" ")[1]), vertices, faces);
            } else if (line.endsWith("{")) {
                depth++;
            } else if (line.equals("}")) {
                depth--;
            } else if (depth == 1 && line.equalsIgnoreCase("shading 1")) {
                smooth = true;
            } else if (depth == 1 && line.startsWith("facet ")) {
                facetCos = Math.cos(Math.toRadians(Double.parseDouble(line.substring(6).trim())));
            }
        }

        if (faces.isEmpty()) {
            return new MeshGroup(name, new float[0], new float[0], new float[0]);
        }
        return buildGroup(name, vertices, faces, smooth, facetCos);
    }

    /** Reads {@code count} vertex lines and the closing brace of the chunk. */
    private static float[][] readVertices(String source, LineCursor cursor, int count) throws IOException {
        float[][] vertices = new float[count][];
        for (int i = 0; i < count; i++) {
            String line = cursor.next();
            String[] t = line == null ? new String[0] : line.split(" ");
            if (t.length != 3) {
                throw new ModelFormatException("%s:%d expected a vertex".formatted(source, cursor.lineNumber()));
            }
            vertices[i] = new float[]{
                    Float.parseFloat(t[0]) * SCALE,
                    Float.parseFloat(t[1]) * SCALE,
                    Float.parseFloat(t[2]) * SCALE};
        }
        skipToClose(cursor);
        return vertices;
    }

    /** Reads {@code count} face lines and the closing brace of the chunk. */
    private static void readFaces(LineCursor cursor, int count, float[][] vertices, List<Face> out) throws IOException {
        for (int i = 0; i < count; i++) {
            String line = cursor.next();
            if (line == null || line.equals("}")) {
                return;
            }
            parseFace(line, vertices, out);
        }
        skipToClose(cursor);
    }

    private static void skipToClose(LineCursor cursor) throws IOException {
        String line;
        while ((line = cursor.next()) != null && !line.equals("}")) {
            // Trailing data inside the chunk, normally nothing.
        }
    }

    /** Converts one MQO face line into one or two triangles with reversed winding. */
    private static void parseFace(String line, float[][] vertices, List<Face> out) {
        Matcher vm = VERTICES.matcher(line);
        if (!vm.find()) {
            return;
        }
        String[] idx = vm.group(1).trim().split(" ");
        if (idx.length != 3 && idx.length != 4) {
            return;
        }
        int[] ids = new int[idx.length];
        for (int i = 0; i < idx.length; i++) {
            ids[i] = Integer.parseInt(idx[i]);
        }
        float[] uv = new float[idx.length * 2];
        Matcher um = UVS.matcher(line);
        if (um.find()) {
            String[] t = um.group(1).trim().split(" ");
            for (int i = 0; i < Math.min(t.length, uv.length); i++) {
                uv[i] = Float.parseFloat(t[i]);
            }
        }

        out.add(new Face(vertices, new int[]{ids[2], ids[1], ids[0]},
                new float[]{uv[4], uv[5], uv[2], uv[3], uv[0], uv[1]}));
        if (ids.length == 4) {
            out.add(new Face(vertices, new int[]{ids[3], ids[2], ids[0]},
                    new float[]{uv[6], uv[7], uv[4], uv[5], uv[0], uv[1]}));
        }
    }

    private static MeshGroup buildGroup(String name, float[][] vertices, List<Face> faces, boolean smooth, double facetCos) {
        // Faces touching each vertex, so smoothing is linear instead of the original's O(n^2) scan.
        List<List<Face>> adjacency = new ArrayList<>(vertices.length);
        for (int i = 0; i < vertices.length; i++) {
            adjacency.add(new ArrayList<>(4));
        }
        for (Face face : faces) {
            for (int id : face.ids) {
                adjacency.get(id).add(face);
            }
        }

        TriangleBuilder builder = new TriangleBuilder();
        for (Face face : faces) {
            for (int corner = 0; corner < 3; corner++) {
                float[] p = vertices[face.ids[corner]];
                float[] n = face.normal;
                if (smooth) {
                    float[] sn = smoothNormal(face.normal, adjacency.get(face.ids[corner]), facetCos);
                    if (dot(face.normal, sn) >= facetCos) {
                        n = sn;
                    }
                }
                builder.vertex(p[0], p[1], p[2], n[0], n[1], n[2], face.uv[corner * 2], face.uv[corner * 2 + 1]);
            }
        }
        return builder.build(name);
    }

    private static float[] smoothNormal(float[] reference, List<Face> neighbours, double facetCos) {
        float x = 0;
        float y = 0;
        float z = 0;
        for (Face f : neighbours) {
            if (dot(f.normal, reference) >= facetCos) {
                x += f.normal[0];
                y += f.normal[1];
                z += f.normal[2];
            }
        }
        float len = (float) Math.sqrt(x * x + y * y + z * z);
        return len < 1.0E-8F ? reference : new float[]{x / len, y / len, z / len};
    }

    private static float dot(float[] a, float[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    private static final class Face {
        final int[] ids;
        final float[] uv;
        final float[] normal;

        Face(float[][] vertices, int[] ids, float[] uv) {
            this.ids = ids;
            this.uv = uv;
            this.normal = TriangleBuilder.faceNormal(vertices[ids[0]], vertices[ids[1]], vertices[ids[2]]);
        }
    }

    /** Hands out trimmed lines with whitespace runs collapsed, and tracks the line number for errors. */
    private static final class LineCursor {
        private final BufferedReader reader;
        private int lineNumber;

        LineCursor(BufferedReader reader) {
            this.reader = reader;
        }

        String next() throws IOException {
            String line = reader.readLine();
            if (line == null) {
                return null;
            }
            lineNumber++;
            return line.replaceAll("\\s+", " ").trim();
        }

        int lineNumber() {
            return lineNumber;
        }
    }
}
