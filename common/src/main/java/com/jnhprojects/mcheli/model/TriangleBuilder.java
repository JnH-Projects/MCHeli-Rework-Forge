package com.jnhprojects.mcheli.model;

import java.util.Arrays;

/** Growable float buffers for one {@link MeshGroup}. */
final class TriangleBuilder {
    private float[] positions = new float[96];
    private float[] normals = new float[96];
    private float[] uvs = new float[64];
    private int vertices;

    void vertex(float x, float y, float z, float nx, float ny, float nz, float u, float v) {
        if ((vertices + 1) * 3 > positions.length) {
            int capacity = positions.length * 2;
            positions = Arrays.copyOf(positions, capacity);
            normals = Arrays.copyOf(normals, capacity);
            uvs = Arrays.copyOf(uvs, capacity / 3 * 2);
        }
        int p = vertices * 3;
        positions[p] = x;
        positions[p + 1] = y;
        positions[p + 2] = z;
        normals[p] = nx;
        normals[p + 1] = ny;
        normals[p + 2] = nz;
        int t = vertices * 2;
        uvs[t] = u;
        uvs[t + 1] = v;
        vertices++;
    }

    MeshGroup build(String name) {
        return new MeshGroup(name,
                Arrays.copyOf(positions, vertices * 3),
                Arrays.copyOf(normals, vertices * 3),
                Arrays.copyOf(uvs, vertices * 2));
    }

    /** Unit face normal of triangle {@code a, b, c}, or straight up for degenerate faces. */
    static float[] faceNormal(float[] a, float[] b, float[] c) {
        float ux = b[0] - a[0], uy = b[1] - a[1], uz = b[2] - a[2];
        float vx = c[0] - a[0], vy = c[1] - a[1], vz = c[2] - a[2];
        float nx = uy * vz - uz * vy;
        float ny = uz * vx - ux * vz;
        float nz = ux * vy - uy * vx;
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len < 1.0E-8F) {
            return new float[]{0, 1, 0};
        }
        return new float[]{nx / len, ny / len, nz / len};
    }
}
