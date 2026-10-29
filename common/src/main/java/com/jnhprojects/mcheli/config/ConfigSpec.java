package com.jnhprojects.mcheli.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * A small TOML config with the layout Forge's config specs wrote: {@code [section]} tables, flat
 * keys, comments above each key. Files written by MCHeli Rework 1.0.0 and the first betas keep
 * their values. Fabric has no config system of its own, so every loader uses this one.
 *
 * <p>Reading never fails: a missing file, an unknown key or a bad value gives the default, and
 * {@link #load(Path)} writes the file back with every key and comment in place, like Forge did.
 * Until the first load, and after {@link #unload()}, every value is its default.
 */
public final class ConfigSpec {
    private final List<Section> sections;
    private volatile boolean loaded;

    private ConfigSpec(List<Section> sections) {
        this.sections = List.copyOf(sections);
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean isLoaded() {
        return loaded;
    }

    /** Reads {@code file}, or the defaults if it is missing or unreadable, then writes it back complete. */
    public void load(Path file) throws IOException {
        Map<String, String> raw = Files.isRegularFile(file) ? parse(Files.readString(file, StandardCharsets.UTF_8)) : Map.of();
        for (Section section : sections) {
            for (Value<?> value : section.values) {
                value.read(raw.get(section.name + "." + value.key));
            }
        }
        loaded = true;
        Files.createDirectories(file.toAbsolutePath().getParent());
        Files.writeString(file, write(), StandardCharsets.UTF_8);
    }

    /** Back to the defaults, for when the world that owns the file closes. */
    public void unload() {
        loaded = false;
        for (Section section : sections) {
            for (Value<?> value : section.values) {
                value.reset();
            }
        }
    }

    /** The file as {@link #load(Path)} writes it. */
    public String write() {
        StringBuilder out = new StringBuilder();
        for (Section section : sections) {
            if (!out.isEmpty()) {
                out.append('\n');
            }
            for (String line : section.comment) {
                out.append('#').append(line).append('\n');
            }
            out.append('[').append(section.name).append("]\n");
            for (Value<?> value : section.values) {
                for (String line : value.comment) {
                    out.append("\t#").append(line).append('\n');
                }
                if (value.range != null) {
                    out.append("\t#Range: ").append(value.range).append('\n');
                }
                out.append('\t').append(value.key).append(" = ").append(value.format()).append('\n');
            }
        }
        return out.toString();
    }

    /**
     * Flattens a TOML document into {@code section.key -> raw value}. Only what this class writes
     * is understood: tables, bare keys, and single or multi-line arrays.
     */
    static Map<String, String> parse(String text) {
        Map<String, String> values = new LinkedHashMap<>();
        String section = "";
        String[] lines = text.split("\\R");
        for (int i = 0; i < lines.length; i++) {
            String line = stripComment(lines[i]).trim();
            if (line.isEmpty()) {
                continue;
            }
            if (line.startsWith("[") && line.endsWith("]")) {
                section = line.substring(1, line.length() - 1).trim();
                continue;
            }
            int eq = line.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            String key = line.substring(0, eq).trim();
            StringBuilder value = new StringBuilder(line.substring(eq + 1).trim());
            // An array may run over several lines.
            while (value.toString().startsWith("[") && !closes(value) && i + 1 < lines.length) {
                value.append(' ').append(stripComment(lines[++i]).trim());
            }
            values.put(section.isEmpty() ? key : section + "." + key, value.toString());
        }
        return values;
    }

    private static boolean closes(CharSequence array) {
        return array.toString().stripTrailing().endsWith("]");
    }

    /** Drops a {@code #} comment, unless the {@code #} is inside a quoted string. */
    private static String stripComment(String line) {
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"' && (i == 0 || line.charAt(i - 1) != '\\')) {
                quoted = !quoted;
            } else if (c == '#' && !quoted) {
                return line.substring(0, i);
            }
        }
        return line;
    }

    private static String quote(String s) {
        return '"' + s.replace("\\", "\\\\").replace("\"", "\\\"") + '"';
    }

    private static String unquote(String s) {
        String t = s.trim();
        if (t.length() >= 2 && (t.startsWith("\"") && t.endsWith("\"") || t.startsWith("'") && t.endsWith("'"))) {
            return t.substring(1, t.length() - 1).replace("\\\"", "\"").replace("\\\\", "\\");
        }
        throw new IllegalArgumentException("Not a string: " + s);
    }

    private record Section(String name, List<String> comment, List<Value<?>> values) {
    }

    /** One setting. {@link #get()} is safe from any thread. */
    public abstract static sealed class Value<T> permits BooleanValue, IntValue, DoubleValue, StringListValue {
        private final String key;
        private final List<String> comment;
        private final T defaultValue;
        private final String range;
        private volatile T value;

        Value(String key, List<String> comment, T defaultValue, String range) {
            this.key = key;
            this.comment = comment;
            this.defaultValue = defaultValue;
            this.range = range;
            this.value = defaultValue;
        }

        public T get() {
            return value;
        }

        public T getDefault() {
            return defaultValue;
        }

        void read(String raw) {
            if (raw == null) {
                value = defaultValue;
                return;
            }
            try {
                value = Objects.requireNonNull(parse(raw.trim()));
            } catch (RuntimeException e) {
                value = defaultValue;
            }
        }

        void reset() {
            value = defaultValue;
        }

        abstract T parse(String raw);

        abstract String format();
    }

    public static final class BooleanValue extends Value<Boolean> {
        BooleanValue(String key, List<String> comment, boolean defaultValue) {
            super(key, comment, defaultValue, null);
        }

        @Override
        Boolean parse(String raw) {
            return switch (raw.toLowerCase(Locale.ROOT)) {
                case "true" -> true;
                case "false" -> false;
                default -> throw new IllegalArgumentException(raw);
            };
        }

        @Override
        String format() {
            return get().toString();
        }
    }

    public static final class IntValue extends Value<Integer> {
        private final int min;
        private final int max;

        IntValue(String key, List<String> comment, int defaultValue, int min, int max) {
            super(key, comment, defaultValue, min + " ~ " + max);
            this.min = min;
            this.max = max;
        }

        @Override
        Integer parse(String raw) {
            int v = Integer.parseInt(raw);
            if (v < min || v > max) {
                throw new IllegalArgumentException(raw);
            }
            return v;
        }

        @Override
        String format() {
            return get().toString();
        }
    }

    public static final class DoubleValue extends Value<Double> {
        private final double min;
        private final double max;

        DoubleValue(String key, List<String> comment, double defaultValue, double min, double max) {
            super(key, comment, defaultValue, min + " ~ " + max);
            this.min = min;
            this.max = max;
        }

        @Override
        Double parse(String raw) {
            double v = Double.parseDouble(raw);
            if (!(v >= min && v <= max)) {
                throw new IllegalArgumentException(raw);
            }
            return v;
        }

        @Override
        String format() {
            return get().toString();
        }
    }

    public static final class StringListValue extends Value<List<String>> {
        StringListValue(String key, List<String> comment, List<String> defaultValue) {
            super(key, comment, List.copyOf(defaultValue), null);
        }

        @Override
        List<String> parse(String raw) {
            if (!raw.startsWith("[") || !raw.endsWith("]")) {
                throw new IllegalArgumentException(raw);
            }
            String body = raw.substring(1, raw.length() - 1).trim();
            List<String> items = new ArrayList<>();
            if (body.isEmpty()) {
                return List.of();
            }
            for (String item : splitArray(body)) {
                if (!item.isBlank()) {
                    items.add(unquote(item));
                }
            }
            return List.copyOf(items);
        }

        /** Splits on commas outside quotes. */
        private static List<String> splitArray(String body) {
            List<String> parts = new ArrayList<>();
            StringBuilder current = new StringBuilder();
            boolean quoted = false;
            for (int i = 0; i < body.length(); i++) {
                char c = body.charAt(i);
                if (c == '"' && (i == 0 || body.charAt(i - 1) != '\\')) {
                    quoted = !quoted;
                }
                if (c == ',' && !quoted) {
                    parts.add(current.toString());
                    current.setLength(0);
                } else {
                    current.append(c);
                }
            }
            parts.add(current.toString());
            return parts;
        }

        @Override
        String format() {
            List<String> quoted = new ArrayList<>();
            for (String s : get()) {
                quoted.add(quote(s));
            }
            return "[" + String.join(", ", quoted) + "]";
        }
    }

    /** Collects sections and values in file order. */
    public static final class Builder {
        private final List<Section> sections = new ArrayList<>();
        private Section current;
        private List<String> pendingComment = List.of();

        private Builder() {
        }

        /** The comment for the next section or value. */
        public Builder comment(String... lines) {
            List<String> spaced = new ArrayList<>();
            for (String line : lines) {
                spaced.add(" " + line);
            }
            pendingComment = List.copyOf(spaced);
            return this;
        }

        public Builder push(String section) {
            current = new Section(section, takeComment(), new ArrayList<>());
            sections.add(current);
            return this;
        }

        public Builder pop() {
            current = null;
            return this;
        }

        public BooleanValue define(String key, boolean defaultValue) {
            return add(new BooleanValue(key, takeComment(), defaultValue));
        }

        public IntValue defineInRange(String key, int defaultValue, int min, int max) {
            return add(new IntValue(key, takeComment(), defaultValue, min, max));
        }

        public DoubleValue defineInRange(String key, double defaultValue, double min, double max) {
            return add(new DoubleValue(key, takeComment(), defaultValue, min, max));
        }

        public StringListValue defineList(String key, List<String> defaultValue) {
            return add(new StringListValue(key, takeComment(), defaultValue));
        }

        public ConfigSpec build() {
            return new ConfigSpec(sections);
        }

        private List<String> takeComment() {
            List<String> comment = pendingComment;
            pendingComment = List.of();
            return comment;
        }

        private <V extends Value<?>> V add(V value) {
            if (current == null) {
                throw new IllegalStateException("Values belong in a section; call push first");
            }
            current.values.add(value);
            return value;
        }
    }
}
