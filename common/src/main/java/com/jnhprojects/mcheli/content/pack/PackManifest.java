package com.jnhprojects.mcheli.content.pack;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * The {@code pack.properties} file at the top of a content pack:
 * <pre>
 * id = rework-example
 * name = MCHeli Rework example pack
 * author = JayMar921
 * version = 1.0.0
 * description = One small helicopter, to show how a pack is put together.
 * </pre>
 * Only {@code id} is required. It names the pack in logs, the allow list and the client cache, so
 * it is limited to lower case letters, digits, {@code -}, {@code _} and {@code .}. The other values
 * are shown to players and are cut to a sensible length, since servers send them to every client.
 */
public record PackManifest(String id, String name, String author, String version, String description) {
    public static final String FILE = "pack.properties";

    private static final Pattern ID = Pattern.compile("[a-z0-9][a-z0-9_.-]{0,63}");

    public static PackManifest read(InputStream in) throws IOException, PackException {
        Properties p = new Properties();
        p.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        String id = p.getProperty("id", "").trim();
        if (!ID.matcher(id).matches()) {
            throw new PackException("%s: id \"%s\" must be 1 to 64 characters of a-z, 0-9, '-', '_' and '.'"
                    .formatted(FILE, id));
        }
        if (id.equals(ContentSource.ORIGINAL)) {
            throw new PackException(FILE + ": the id \"original\" is reserved for the imported 1.0.3 content");
        }
        return new PackManifest(id, value(p, "name", id, 64), value(p, "author", "", 64),
                value(p, "version", "1.0.0", 32), value(p, "description", "", 256));
    }

    private static String value(Properties p, String key, String fallback, int maxLength) {
        String value = p.getProperty(key, "").trim();
        if (value.isEmpty()) {
            return fallback;
        }
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }
}
