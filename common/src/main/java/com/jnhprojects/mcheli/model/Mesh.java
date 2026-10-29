package com.jnhprojects.mcheli.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A loaded vehicle model.
 *
 * <p>MCHeli models use object names to mark movable parts. A part is introduced by an object whose
 * name starts with {@code $} (for example {@code $body} or {@code $blade0}) and owns every object
 * after it up to the next {@code $} object. {@link #part(String)} reproduces that rule so models
 * authored for the original mod render the same way.
 */
public final class Mesh {
    private final String source;
    private final List<MeshGroup> groups;
    private final Map<String, List<MeshGroup>> parts = new ConcurrentHashMap<>();

    public Mesh(String source, List<MeshGroup> groups) {
        this.source = source;
        this.groups = List.copyOf(groups);
    }

    public String source() {
        return source;
    }

    public List<MeshGroup> groups() {
        return groups;
    }

    public boolean contains(String name) {
        for (MeshGroup group : groups) {
            if (group.name().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Groups that make up {@code name}. For a {@code $} name this includes the trailing child
     * objects, otherwise only objects with exactly that name. Returns an empty list if the part
     * does not exist.
     */
    public List<MeshGroup> part(String name) {
        // Renderers ask for the same parts every frame, so remember the answer.
        return parts.computeIfAbsent(name.toLowerCase(Locale.ROOT), this::findPart);
    }

    private List<MeshGroup> findPart(String name) {
        List<MeshGroup> out = new ArrayList<>();
        boolean header = name.startsWith("$");
        for (int i = 0; i < groups.size(); i++) {
            MeshGroup group = groups.get(i);
            if (!group.name().equalsIgnoreCase(name)) {
                continue;
            }
            out.add(group);
            if (header) {
                while (i + 1 < groups.size() && !groups.get(i + 1).isPartHeader()) {
                    out.add(groups.get(++i));
                }
            }
        }
        return Collections.unmodifiableList(out);
    }

    /** The hull: {@code $body} when present, otherwise the whole model, as in the original renderer. */
    public List<MeshGroup> body() {
        return contains("$body") ? part("$body") : groups;
    }

    public int triangleCount() {
        int n = 0;
        for (MeshGroup group : groups) {
            n += group.vertexCount() / 3;
        }
        return n;
    }

    @Override
    public String toString() {
        return String.format(Locale.ROOT, "Mesh[%s, %d groups, %d triangles]", source, groups.size(), triangleCount());
    }
}
