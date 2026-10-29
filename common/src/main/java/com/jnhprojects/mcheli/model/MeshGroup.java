package com.jnhprojects.mcheli.model;

/**
 * One named object from a model file, flattened into triangles.
 *
 * <p>Arrays are laid out per vertex: three floats of position and normal, two floats of UV.
 * Every three vertices form one triangle, already in counter-clockwise order.
 */
public record MeshGroup(String name, float[] positions, float[] normals, float[] uvs) {
    public int vertexCount() {
        return positions.length / 3;
    }

    public boolean isPartHeader() {
        return !name.isEmpty() && name.charAt(0) == '$';
    }
}
