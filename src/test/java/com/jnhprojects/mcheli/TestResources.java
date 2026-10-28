package com.jnhprojects.mcheli;

import com.jnhprojects.mcheli.content.LegacyImporter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Properties;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * The original MCHeli content for the tests, imported from the zip the developer keeps in
 * {@code Mod/} (the repository holds none of it). Gradle passes the zip and an output folder in;
 * the import runs once and is reused until the zip or the importer changes. Tests that need the
 * content are skipped where the zip is missing, such as on CI.
 */
public final class TestResources {
    private static Path pack;

    private TestResources() {
    }

    /** The imported {@code assets/mcheli} folder. */
    public static Path assets() {
        return pack().resolve("assets/mcheli");
    }

    /** The imported pack folder, holding {@code assets} and {@code data}. */
    public static synchronized Path pack() {
        if (pack == null) {
            pack = importZip();
        }
        return pack;
    }

    private static Path importZip() {
        Path zip = Path.of(System.getProperty("mcheli.legacyZip", "Mod/MC1.7.10_mcheli_1.0.3.zip"));
        assumeTrue(Files.isRegularFile(zip), "original MCHeli zip not available at " + zip.toAbsolutePath());
        Path out = Path.of(System.getProperty("mcheli.testContent", "build/legacy-content"));
        try {
            Properties source = new Properties();
            source.setProperty("format", Integer.toString(LegacyImporter.FORMAT));
            source.setProperty("size", Long.toString(Files.size(zip)));
            source.setProperty("modified", Long.toString(Files.getLastModifiedTime(zip).toMillis()));

            Path marker = out.resolve("import.properties");
            if (Files.isRegularFile(marker)) {
                Properties existing = new Properties();
                try (InputStream in = Files.newInputStream(marker)) {
                    existing.load(in);
                }
                if (existing.equals(source)) {
                    return out;
                }
            }

            if (Files.exists(out)) {
                try (Stream<Path> paths = Files.walk(out)) {
                    for (Path p : paths.sorted(Comparator.reverseOrder()).toList()) {
                        Files.delete(p);
                    }
                }
            }
            LegacyImporter.run(zip, out);
            try (OutputStream o = Files.newOutputStream(marker)) {
                source.store(o, null);
            }
            return out;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
