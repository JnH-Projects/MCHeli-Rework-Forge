package com.jnhprojects.mcheli.content;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Parses the MCHeli text format.
 *
 * <p>The format is line based. Everything before the first {@code =} is the key (case
 * insensitive), everything after it is the value. Lines without a value are ignored, and so are
 * lines starting with {@code ;}, {@code #} or {@code //}, which the shipped files use as comments.
 * Repeated keys such as {@code AddSeat} are applied in order.
 *
 * <p>Unlike the original loader, a malformed line does not discard the whole file. The line is
 * skipped, reported through {@link Result#problems()}, and the rest of the file still loads.
 */
public final class DefinitionReader {
    private static final Charset SHIFT_JIS = Charset.forName("windows-31j");

    private DefinitionReader() {
    }

    public static <T extends Definition> Result<T> read(Path file, T definition) throws IOException {
        List<String> problems = new ArrayList<>();
        Set<String> unhandled = new LinkedHashSet<>();

        try (BufferedReader reader = new BufferedReader(new StringReader(decode(Files.readAllBytes(file))))) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (lineNumber == 1 && !line.isEmpty() && line.charAt(0) == '﻿') {
                    line = line.substring(1);
                }
                line = line.trim();
                if (line.isEmpty() || line.startsWith(";") || line.startsWith("#") || line.startsWith("//")) {
                    continue;
                }
                int eq = line.indexOf('=');
                if (eq < 0 || eq == line.length() - 1) {
                    continue;
                }
                String key = DefinitionValues.lower(line.substring(0, eq));
                String value = line.substring(eq + 1).trim();
                try {
                    if (!definition.accept(key, value)) {
                        unhandled.add(key);
                    }
                } catch (RuntimeException e) {
                    problems.add("line %d (%s): %s".formatted(lineNumber, key, e));
                }
            }
        }

        try {
            definition.validate();
            return new Result<>(definition, Collections.unmodifiableList(problems), Collections.unmodifiableSet(unhandled));
        } catch (InvalidDefinitionException e) {
            problems.add("rejected: " + e.getMessage());
            return new Result<>(null, Collections.unmodifiableList(problems), Collections.unmodifiableSet(unhandled));
        }
    }

    /**
     * Files are meant to be UTF-8, but a few of the shipped ones were saved as Shift-JIS by the
     * original author's editor. Fall back to Windows-31J rather than mangling those comments.
     */
    static String decode(byte[] bytes) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
        } catch (CharacterCodingException e) {
            return new String(bytes, SHIFT_JIS);
        }
    }

    /**
     * @param definition the loaded definition, or {@code null} if validation rejected it
     */
    public record Result<T extends Definition>(T definition, List<String> problems, Set<String> unhandledKeys) {
        public boolean isValid() {
            return definition != null;
        }
    }
}
