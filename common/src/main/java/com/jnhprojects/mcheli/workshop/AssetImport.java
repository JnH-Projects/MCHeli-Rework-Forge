package com.jnhprojects.mcheli.workshop;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Checks a PNG or OGG before the workshop copies it into a pack, and says what is wrong in terms its
 * author can act on. A pack holding a bad file would still load, then quietly show or play nothing.
 */
public final class AssetImport {
    /** Servers allow 8 MB for a whole pack by default, so one file should stay well under it. */
    private static final long LARGE_FILE = 2L << 20;
    private static final int LARGE_TEXTURE = 2048;
    private static final int MAX_TEXTURE = 8192;
    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};
    /** The signature, then the IHDR chunk's length and name, then its width and height. */
    private static final int PNG_SIZE_OFFSET = 16;
    private static final int PNG_HEADER_BYTES = 24;
    private static final byte[] OGG_SIGNATURE = {'O', 'g', 'g', 'S'};
    private static final int OGG_FIRST_PAGE_BYTES = 64;

    private AssetImport() {
    }

    /** A usable PNG, with what its author may want to fix. */
    public record Texture(int width, int height, List<String> warnings) {
    }

    /** @throws WorkshopException when the game could not use the file */
    public static Texture texture(Path file) throws WorkshopException {
        String name = checkFile(file, ".png", "a PNG image");
        try (InputStream in = Files.newInputStream(file)) {
            byte[] head = in.readNBytes(PNG_HEADER_BYTES);
            if (head.length < PNG_HEADER_BYTES || !startsWith(head, PNG_SIGNATURE)) {
                throw new WorkshopException(name + " is not really a PNG. Save it again as PNG from your image editor.");
            }
            DataInputStream size = new DataInputStream(new ByteArrayInputStream(head, PNG_SIZE_OFFSET, 8));
            int width = size.readInt();
            int height = size.readInt();
            if (width <= 0 || height <= 0 || width > MAX_TEXTURE || height > MAX_TEXTURE) {
                throw new WorkshopException("%s is %d by %d. A texture can be at most %d by %d.".formatted(name, width, height, MAX_TEXTURE, MAX_TEXTURE));
            }
            List<String> warnings = new ArrayList<>();
            if (Math.max(width, height) > LARGE_TEXTURE) {
                warnings.add("%s is %d by %d, which costs a lot of video memory for every player.".formatted(name, width, height));
            }
            warnAboutSize(file, name, warnings);
            return new Texture(width, height, List.copyOf(warnings));
        } catch (IOException e) {
            throw new WorkshopException("Could not read %s: %s".formatted(name, e));
        }
    }

    /**
     * Minecraft plays Ogg Vorbis only. An OGG file can hold other codecs, and those fail silently
     * when they play, so the codec is checked here.
     *
     * @return what the author may want to fix
     * @throws WorkshopException when the game could not use the file
     */
    public static List<String> sound(Path file) throws WorkshopException {
        String name = checkFile(file, ".ogg", "an OGG Vorbis sound");
        try (InputStream in = Files.newInputStream(file)) {
            byte[] head = in.readNBytes(OGG_FIRST_PAGE_BYTES);
            if (!startsWith(head, OGG_SIGNATURE)) {
                throw new WorkshopException(name + " is not really an OGG file. Export it again as OGG Vorbis.");
            }
            String first = new String(head, StandardCharsets.ISO_8859_1);
            if (first.contains("OpusHead")) {
                throw new WorkshopException(name + " holds Opus audio, which the game cannot play. Export it again as OGG Vorbis.");
            }
            if (!first.contains("vorbis")) {
                throw new WorkshopException(name + " does not hold Vorbis audio, which is all the game can play. Export it again as OGG Vorbis.");
            }
            List<String> warnings = new ArrayList<>();
            warnAboutSize(file, name, warnings);
            return List.copyOf(warnings);
        } catch (IOException e) {
            throw new WorkshopException("Could not read %s: %s".formatted(name, e));
        }
    }

    private static String checkFile(Path file, String extension, String what) throws WorkshopException {
        if (!Files.isRegularFile(file)) {
            throw new WorkshopException("No file at " + file);
        }
        String name = file.getFileName().toString();
        if (!name.toLowerCase(Locale.ROOT).endsWith(extension)) {
            throw new WorkshopException("%s is not %s.".formatted(name, what));
        }
        return name;
    }

    private static void warnAboutSize(Path file, String name, List<String> warnings) throws IOException {
        long size = Files.size(file);
        if (size > LARGE_FILE) {
            warnings.add("%s is %.1f MB. Servers allow 8 MB per pack unless their config says otherwise.".formatted(name, size / 1048576.0));
        }
    }

    private static boolean startsWith(byte[] bytes, byte[] prefix) {
        return bytes.length >= prefix.length && Arrays.equals(bytes, 0, prefix.length, prefix, 0, prefix.length);
    }
}
