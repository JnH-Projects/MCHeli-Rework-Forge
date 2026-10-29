package com.jnhprojects.mcheli.content;

import com.jnhprojects.mcheli.content.recipe.LegacyRecipe;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Converts the MCHeli 1.0.3 (Minecraft 1.7.10) distribution zip into a pack folder that Minecraft
 * 26.x loads as both a resource pack and a data pack.
 *
 * <p>The vehicles, models, textures and sounds are EMB4's and are not redistributed with this mod.
 * Players download the original zip themselves and the mod runs this on start, so the conversion
 * has to produce exactly what the content code expects:
 * <ul>
 *   <li>Every path is lower-cased and characters that are no longer legal in resource identifiers
 *       ({@code #} and upper case letters) are replaced.</li>
 *   <li>{@code textures/items} and {@code textures/blocks} move to the singular folder names.</li>
 *   <li>{@code .lang} files become JSON with the 26.x translation keys; display names declared in
 *       the definitions win over the old lang files.</li>
 *   <li>{@code sounds.json} is built with the same grouping rule the original used at runtime.</li>
 *   <li>Every item texture gets an item model and an item model definition.</li>
 *   <li>The definitions' {@code AddRecipe} lines become {@code data/mcheli/recipe}.</li>
 * </ul>
 *
 * <p>The translation keys the rework added itself (HUD text, key bindings, entity names) are not
 * written here. They ship in the mod jar, and Minecraft merges lang files across packs.
 */
public final class LegacyImporter {
    /**
     * Bump whenever the output changes, so existing installs import again. 2: recipes craft the
     * generic vehicle items of 1.1.0. 3: vehicle ingredients name
     * their type for every loader.
     */
    public static final int FORMAT = 3;

    private static final String ASSET_ROOT = "mcheli/assets/mcheli/";
    private static final Set<String> SKIPPED_EXTENSIONS = Set.of("xcf");
    private static final Set<String> DEFINITION_FOLDERS = Set.of("helicopters", "planes", "tanks", "vehicles", "throwable");
    private static final Pattern LANG_ITEM = Pattern.compile("^item\\.mcheli:(.+)\\.name$");
    private static final Pattern LANG_TILE = Pattern.compile("^tile\\.(.+)\\.name$");
    private static final Pattern LANG_ACHIEVEMENT = Pattern.compile("^achievement\\.McHeli(\\w+?)(\\.desc)?$");

    private final Path assetsOut;
    private final Path recipesOut;
    private final Map<String, Map<String, String>> translations = new TreeMap<>();
    private final Set<String> soundFiles = new TreeSet<>();
    private final Set<String> itemTextures = new TreeSet<>();
    private int copied;

    private LegacyImporter(Path packRoot) {
        this.assetsOut = packRoot.resolve("assets/mcheli");
        this.recipesOut = packRoot.resolve("data/mcheli/recipe");
    }

    /**
     * @param zip      the original {@code MC1.7.10_mcheli_1.0.3.zip}, or a zip with the same layout
     * @param packRoot an empty or missing folder that receives {@code assets} and {@code data}
     * @throws IOException when the zip cannot be read or holds no {@code mcheli/assets/mcheli} folder
     */
    public static Summary run(Path zip, Path packRoot) throws IOException {
        return new LegacyImporter(packRoot).importFrom(zip);
    }

    private Summary importFrom(Path zipPath) throws IOException {
        try (ZipFile zip = new ZipFile(zipPath.toFile(), StandardCharsets.UTF_8)) {
            List<ZipEntry> entries = new ArrayList<>();
            for (Enumeration<? extends ZipEntry> e = zip.entries(); e.hasMoreElements(); ) {
                ZipEntry entry = e.nextElement();
                if (!entry.isDirectory() && entry.getName().startsWith(ASSET_ROOT)) {
                    entries.add(entry);
                }
            }
            if (entries.isEmpty()) {
                throw new IOException(zipPath.getFileName() + " has no " + ASSET_ROOT + " folder; is it the MCHeli 1.0.3 zip?");
            }
            for (ZipEntry entry : entries) {
                migrate(zip, entry);
            }
        }

        writeSounds();
        writeItemModels();
        writeTranslations();

        LegacyRecipe.Generated recipes = LegacyRecipe.generate(ContentRegistry.load(assetsOut));
        for (Map.Entry<String, String> e : recipes.files().entrySet()) {
            write(recipesOut.resolve(e.getKey()), e.getValue());
        }
        return new Summary(copied, soundFiles.size(), itemTextures.size(), recipes.files().size(), recipes.skipped());
    }

    /**
     * @param skippedRecipes recipe lines that could not become a recipe, as {@code "<file>: <reason>"}
     */
    public record Summary(int files, int sounds, int itemModels, int recipes, List<String> skippedRecipes) {
    }

    private void migrate(ZipFile zip, ZipEntry entry) throws IOException {
        String relative = entry.getName().substring(ASSET_ROOT.length());
        String extension = extensionOf(relative);

        // Top level files are the original authoring guides (Readme_*.txt and the parameter PDF).
        if (SKIPPED_EXTENSIONS.contains(extension) || !relative.contains("/")) {
            return;
        }
        if (relative.startsWith("lang/")) {
            readLang(zip, entry, relative);
            return;
        }

        String target = sanitize(relative);
        if (target.startsWith("textures/items/")) {
            target = "textures/item/" + target.substring("textures/items/".length());
            itemTextures.add(stripExtension(target.substring("textures/item/".length())));
        } else if (target.startsWith("textures/blocks/")) {
            target = "textures/block/" + target.substring("textures/blocks/".length());
        } else if (target.startsWith("sounds/") && extension.equals("ogg")) {
            soundFiles.add(stripExtension(target.substring("sounds/".length())));
        }

        String folder = target.substring(0, target.indexOf('/'));
        if (DEFINITION_FOLDERS.contains(folder) && extension.equals("txt")) {
            readDefinitionNames(zip, entry, stripExtension(target.substring(folder.length() + 1)));
        }

        Path out = assetsOut.resolve(target).normalize();
        if (!out.startsWith(assetsOut)) {
            throw new IOException("Refusing to write outside the pack: " + entry.getName());
        }
        Files.createDirectories(out.getParent());
        try (InputStream in = zip.getInputStream(entry)) {
            Files.write(out, in.readAllBytes());
        }
        copied++;
    }

    /** Resource identifiers only allow {@code [a-z0-9/._-]}. */
    static String sanitize(String path) {
        StringBuilder sb = new StringBuilder(path.length());
        for (char c : path.toLowerCase(Locale.ROOT).toCharArray()) {
            boolean legal = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '/' || c == '.' || c == '_' || c == '-';
            sb.append(legal ? c : '_');
        }
        return sb.toString();
    }

    private void readLang(ZipFile zip, ZipEntry entry, String relative) throws IOException {
        String locale = stripExtension(relative.substring("lang/".length())).toLowerCase(Locale.ROOT);
        Map<String, String> table = translations.computeIfAbsent(locale, k -> new TreeMap<>());
        for (String line : readLines(zip, entry)) {
            int eq = line.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            String key = mapLangKey(line.substring(0, eq).trim());
            if (key != null) {
                table.putIfAbsent(key, line.substring(eq + 1).trim());
            }
        }
    }

    private static String mapLangKey(String key) {
        Matcher m = LANG_ITEM.matcher(key);
        if (m.matches()) {
            return "item.mcheli." + sanitize(m.group(1));
        }
        m = LANG_TILE.matcher(key);
        if (m.matches()) {
            return "block.mcheli." + sanitize(m.group(1));
        }
        m = LANG_ACHIEVEMENT.matcher(key);
        if (m.matches()) {
            String id = m.group(1).replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
            return "advancements.mcheli." + id + (m.group(2) != null ? ".description" : ".title");
        }
        return null;
    }

    private void readDefinitionNames(ZipFile zip, ZipEntry entry, String id) throws IOException {
        String key = "item.mcheli." + id;
        for (String line : readLines(zip, entry)) {
            int eq = line.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            String name = line.substring(0, eq).trim().toLowerCase(Locale.ROOT);
            String value = line.substring(eq + 1).trim();
            if (name.equals("displayname")) {
                translations.computeIfAbsent("en_us", k -> new TreeMap<>()).put(key, value);
            } else if (name.equals("adddisplayname")) {
                String[] parts = value.split("\\s*,\\s*", 2);
                if (parts.length == 2) {
                    translations.computeIfAbsent(parts[0].toLowerCase(Locale.ROOT), k -> new TreeMap<>()).put(key, parts[1]);
                }
            }
        }
    }

    /**
     * Same grouping rule as the original runtime generator: a trailing digit marks a variant,
     * so {@code wrench1..3} become one event called {@code wrench}.
     */
    private void writeSounds() throws IOException {
        Map<String, List<String>> events = new TreeMap<>();
        for (String file : soundFiles) {
            char last = file.charAt(file.length() - 1);
            String event = last >= '0' && last <= '9' ? file.substring(0, file.length() - 1) : file;
            events.computeIfAbsent(event, k -> new ArrayList<>()).add(file);
        }

        StringBuilder json = new StringBuilder("{\n");
        int i = 0;
        for (Map.Entry<String, List<String>> e : events.entrySet()) {
            json.append("  \"").append(e.getKey()).append("\": {\n    \"sounds\": [");
            for (int j = 0; j < e.getValue().size(); j++) {
                json.append(j > 0 ? ", " : "").append("\"mcheli:").append(e.getValue().get(j)).append('"');
            }
            json.append("]\n  }").append(++i < events.size() ? "," : "").append('\n');
        }
        json.append("}\n");
        write(assetsOut.resolve("sounds.json"), json.toString());
    }

    private void writeItemModels() throws IOException {
        for (String name : itemTextures) {
            write(assetsOut.resolve("items/" + name + ".json"), """
                    {
                      "model": {
                        "type": "minecraft:model",
                        "model": "mcheli:item/%s"
                      }
                    }
                    """.formatted(name));
            write(assetsOut.resolve("models/item/" + name + ".json"), """
                    {
                      "parent": "minecraft:item/generated",
                      "textures": {
                        "layer0": "mcheli:item/%s"
                      }
                    }
                    """.formatted(name));
        }
    }

    private void writeTranslations() throws IOException {
        for (Map.Entry<String, Map<String, String>> e : translations.entrySet()) {
            StringBuilder json = new StringBuilder("{\n");
            int i = 0;
            for (Map.Entry<String, String> t : e.getValue().entrySet()) {
                json.append("  ").append(quote(t.getKey())).append(": ").append(quote(t.getValue()))
                        .append(++i < e.getValue().size() ? "," : "").append('\n');
            }
            write(assetsOut.resolve("lang/" + e.getKey() + ".json"), json.append("}\n").toString());
        }
    }

    private static List<String> readLines(ZipFile zip, ZipEntry entry) throws IOException {
        try (InputStream in = zip.getInputStream(entry)) {
            String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            if (!text.isEmpty() && text.charAt(0) == '﻿') {
                text = text.substring(1);
            }
            return text.lines().toList();
        }
    }

    private static void write(Path target, String content) throws IOException {
        Files.createDirectories(target.getParent());
        Files.writeString(target, content, StandardCharsets.UTF_8);
    }

    private static String quote(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\t' -> sb.append("\\t");
                default -> sb.append(c);
            }
        }
        return sb.append('"').toString();
    }

    private static String extensionOf(String path) {
        int dot = path.lastIndexOf('.');
        return dot < 0 ? "" : path.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static String stripExtension(String path) {
        int dot = path.lastIndexOf('.');
        return dot < 0 ? path : path.substring(0, dot);
    }
}
