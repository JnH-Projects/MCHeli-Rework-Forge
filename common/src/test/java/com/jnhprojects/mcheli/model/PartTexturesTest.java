package com.jnhprojects.mcheli.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PartTexturesTest {
    private static MeshGroup group(String name) {
        return new MeshGroup(name, new float[0], new float[0], new float[0]);
    }

    @Test
    void noOverrideLeavesEverythingInThePaint() {
        List<MeshGroup> groups = List.of(group("$body"), group("trim"));
        List<PartTextures.Bucket> buckets = PartTextures.assign(groups, Map.of());
        assertEquals(1, buckets.size());
        assertNull(buckets.getFirst().texture());
        assertEquals(groups, buckets.getFirst().groups());
    }

    @Test
    void plainObjectsFollowThePartThatOwnsThem() {
        MeshGroup body = group("$body");
        MeshGroup trim = group("trim");
        MeshGroup blade = group("$blade0");
        MeshGroup tip = group("tip");
        List<PartTextures.Bucket> buckets = PartTextures.assign(List.of(body, trim, blade, tip), Map.of("blade0", "my-blade"));
        assertEquals(2, buckets.size());
        assertEquals(List.of(body, trim), buckets.get(0).groups());
        assertEquals("my-blade", buckets.get(1).texture());
        assertEquals(List.of(blade, tip), buckets.get(1).groups());
    }

    @Test
    void aPlainObjectCanHaveItsOwnTexture() {
        MeshGroup body = group("$body");
        MeshGroup cockpit = group("Cockpit");
        MeshGroup trim = group("trim");
        List<PartTextures.Bucket> buckets = PartTextures.assign(List.of(body, cockpit, trim), Map.of("cockpit", "my-cockpit"));
        assertEquals(List.of(body, trim), buckets.get(0).groups());
        assertEquals("my-cockpit", buckets.get(1).texture());
        assertEquals(List.of(cockpit), buckets.get(1).groups());
    }

    @Test
    void namesLeaveOutWhatAnObjCallsItsFirstGroup() {
        Mesh mesh = new Mesh("test", List.of(group("Default"), group("$Body"), group("trim"), group("$blade0")));
        assertEquals(List.of("body", "trim", "blade0"), PartTextures.names(mesh));
    }
}
