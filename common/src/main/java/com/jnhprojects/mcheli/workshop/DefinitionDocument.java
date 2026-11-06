package com.jnhprojects.mcheli.workshop;

import com.jnhprojects.mcheli.content.DefinitionValues;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * A definition file as the workshop edits it: its lines, in order, as text.
 *
 * <p>The editor changes single lines and leaves all others exactly as they were, so comments, keys
 * the editor has no panel for and keys from newer versions survive a round trip. The rules for what
 * counts as an entry are those of {@code DefinitionReader}.
 */
public final class DefinitionDocument {
    private final List<String> lines = new ArrayList<>();

    /** One {@code key = value} line. */
    public record Entry(int line, String key, String value) {
    }

    public DefinitionDocument(String text) {
        String clean = !text.isEmpty() && text.charAt(0) == '﻿' ? text.substring(1) : text;
        lines.addAll(clean.lines().toList());
    }

    /** The file's text, with a line break after every line. */
    public String text() {
        StringBuilder out = new StringBuilder();
        for (String line : lines) {
            out.append(line).append('\n');
        }
        return out.toString();
    }

    /** A copy of the lines, for the undo history. */
    public List<String> snapshot() {
        return List.copyOf(lines);
    }

    public void restore(List<String> snapshot) {
        lines.clear();
        lines.addAll(snapshot);
    }

    public int size() {
        return lines.size();
    }

    /** Every entry in file order. Comments, blank lines and lines with no value are left out. */
    public List<Entry> entries() {
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            Entry entry = entry(i);
            if (entry != null) {
                entries.add(entry);
            }
        }
        return entries;
    }

    /** The entry on a line, or {@code null} if that line holds none. */
    public @Nullable Entry entry(int line) {
        String text = lines.get(line).trim();
        if (text.isEmpty() || text.startsWith(";") || text.startsWith("#") || text.startsWith("//")) {
            return null;
        }
        int eq = text.indexOf('=');
        if (eq < 0 || eq == text.length() - 1) {
            return null;
        }
        return new Entry(line, text.substring(0, eq).trim(), text.substring(eq + 1).trim());
    }

    /** The value that takes effect for a single value key: the last one, as lines apply in order. */
    public @Nullable String value(String key) {
        Entry entry = last(key);
        return entry != null ? entry.value() : null;
    }

    private @Nullable Entry last(String key) {
        String wanted = DefinitionValues.lower(key);
        for (int i = lines.size() - 1; i >= 0; i--) {
            Entry entry = entry(i);
            if (entry != null && DefinitionValues.lower(entry.key()).equals(wanted)) {
                return entry;
            }
        }
        return null;
    }

    /**
     * Sets a single value key: changes the line that takes effect, or adds one at the end. With
     * {@code null} every line of the key goes, which brings back the default.
     */
    public void set(String key, @Nullable String value) {
        if (value == null) {
            String wanted = DefinitionValues.lower(key);
            for (int i = lines.size() - 1; i >= 0; i--) {
                Entry entry = entry(i);
                if (entry != null && DefinitionValues.lower(entry.key()).equals(wanted)) {
                    lines.remove(i);
                }
            }
            return;
        }
        Entry entry = last(key);
        if (entry != null) {
            setLine(entry.line(), entry.key(), value);
        } else {
            lines.add(format(key, value));
        }
    }

    /**
     * The value of a key that repeats, one line per thing it is about, such as
     * {@code PartTexture = blade0, my-blade}: the line whose first value is {@code first}.
     */
    public @Nullable String keyed(String key, String first) {
        Entry entry = lastKeyed(key, first);
        return entry != null ? entry.value() : null;
    }

    /**
     * Sets the line of a repeating key whose first value is {@code first}, or adds one at the end.
     * With {@code null} every such line goes.
     */
    public void setKeyed(String key, String first, @Nullable String value) {
        if (value != null) {
            Entry entry = lastKeyed(key, first);
            if (entry != null) {
                setLine(entry.line(), entry.key(), value);
            } else {
                lines.add(format(key, value));
            }
            return;
        }
        for (Entry entry = lastKeyed(key, first); entry != null; entry = lastKeyed(key, first)) {
            lines.remove(entry.line());
        }
    }

    private @Nullable Entry lastKeyed(String key, String first) {
        String wantedKey = DefinitionValues.lower(key);
        String wanted = bare(first);
        for (int i = lines.size() - 1; i >= 0; i--) {
            Entry entry = entry(i);
            if (entry != null && DefinitionValues.lower(entry.key()).equals(wantedKey)
                    && bare(entry.value().split(",", 2)[0]).equals(wanted)) {
                return entry;
            }
        }
        return null;
    }

    /** Model objects can be written with or without their {@code $}. */
    private static String bare(String name) {
        String lower = DefinitionValues.lower(name.trim());
        return lower.startsWith("$") ? lower.substring(1) : lower;
    }

    public void setLine(int line, String key, String value) {
        lines.set(line, format(key, value));
    }

    /** Puts a new line before the line now at {@code line}, moving that one and the rest down. */
    public void insert(int line, String key, String value) {
        lines.add(line, format(key, value));
    }

    public void removeLine(int line) {
        lines.remove(line);
    }

    private static String format(String key, String value) {
        return key + " = " + value;
    }
}
