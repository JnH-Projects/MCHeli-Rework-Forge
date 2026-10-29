package com.jnhprojects.mcheli;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The common sources are compiled into every platform module, so they may only use the JDK and
 * the few libraries every supported Minecraft version ships (SLF4J, JSpecify, JOML). The common
 * project already fails to compile otherwise; this test names the offending lines, and also
 * catches fully qualified names, which an import check alone would miss.
 */
class CommonBoundaryTest {
    private static final List<String> FORBIDDEN = List.of(
            "net.minecraft.", "net.minecraftforge.", "net.neoforged.", "net.fabricmc.", "com.mojang.", "org.lwjgl.");

    @Test
    void commonSourcesStayFreeOfMinecraft() throws IOException {
        Path root = Path.of(System.getProperty("mcheli.commonSources", "src/main/java"));
        assertTrue(Files.isDirectory(root), "common sources not found at " + root.toAbsolutePath());

        List<String> offences = new ArrayList<>();
        try (Stream<Path> files = Files.walk(root)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).sorted().toList()) {
                List<String> lines = Files.readAllLines(file);
                for (int i = 0; i < lines.size(); i++) {
                    String line = lines.get(i).trim();
                    if (line.startsWith("//") || line.startsWith("*") || line.startsWith("/*")) {
                        continue;
                    }
                    for (String prefix : FORBIDDEN) {
                        if (line.contains(prefix)) {
                            offences.add(root.relativize(file) + ":" + (i + 1) + ": " + line);
                        }
                    }
                }
            }
        }
        assertEquals(List.of(), offences);
    }
}
