package com.jnhprojects.mcheli.model;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Sorts a part's objects by the texture each one draws with, for {@code PartTexture}. A {@code $}
 * object owns the plain objects behind it, so they take its texture unless they have their own.
 */
public final class PartTextures {
    private PartTextures() {
    }

    /**
     * Objects that draw with one texture.
     *
     * @param texture its name, or {@code null} for the vehicle's current paint
     */
    public record Bucket(@Nullable String texture, List<MeshGroup> groups) {
    }

    /** The name {@code PartTexture} knows an object by: lower case, without the {@code $}. */
    public static String partName(String objectName) {
        String lower = objectName.toLowerCase(Locale.ROOT);
        return lower.startsWith("$") ? lower.substring(1) : lower;
    }

    /** @param overrides texture names by {@linkplain #partName part name} */
    public static List<Bucket> assign(List<MeshGroup> groups, Map<String, String> overrides) {
        Map<String, List<MeshGroup>> byTexture = new LinkedHashMap<>();
        List<MeshGroup> paint = new ArrayList<>();
        String inherited = null;
        for (MeshGroup group : groups) {
            String own = overrides.get(partName(group.name()));
            if (group.isPartHeader()) {
                inherited = own;
            }
            String texture = own != null ? own : inherited;
            (texture != null ? byTexture.computeIfAbsent(texture, t -> new ArrayList<>()) : paint).add(group);
        }
        List<Bucket> buckets = new ArrayList<>();
        if (!paint.isEmpty()) {
            buckets.add(new Bucket(null, List.copyOf(paint)));
        }
        byTexture.forEach((texture, list) -> buckets.add(new Bucket(texture, List.copyOf(list))));
        return List.copyOf(buckets);
    }

    /**
     * The part names of a model's objects in file order, for picking one in the workshop. The name
     * an OBJ gives everything before its first group means nothing, so it is left out.
     */
    public static List<String> names(Mesh mesh) {
        Set<String> names = new LinkedHashSet<>();
        for (MeshGroup group : mesh.groups()) {
            if (!group.name().equalsIgnoreCase("Default")) {
                names.add(partName(group.name()));
            }
        }
        return List.copyOf(names);
    }
}
