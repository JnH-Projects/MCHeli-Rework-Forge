package com.jnhprojects.mcheli.workshop;

import com.jnhprojects.mcheli.content.DefinitionValues;
import com.jnhprojects.mcheli.content.Vector3;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.StringJoiner;

/**
 * One placed part: a line of the definition, read through its {@link PartType}. Changing a value
 * gives the new text of the line; values the change does not touch keep the text they had.
 *
 * @param line  the line in the document
 * @param key   the key as the file spells it
 * @param value the line's values
 */
public record WorkshopPart(PartType type, int line, String key, String value) {
    private static final int DECIMALS = 4;

    private String[] tokens() {
        return DefinitionValues.split(value);
    }

    /** One value of a field, or its fallback where the line stops short. */
    public String get(PartField field, int component) {
        String[] tokens = tokens();
        if (field.kind() == PartField.Kind.REST) {
            return field.index() < tokens.length
                    ? String.join(", ", Arrays.copyOfRange(tokens, field.index(), tokens.length))
                    : field.fallback();
        }
        int at = field.index() + component;
        return at < tokens.length ? tokens[at] : PartType.fallback(field, component);
    }

    /** A field's three numbers, with anything that is not a number read as zero. */
    public Vector3 vector(PartField field) {
        return new Vector3(numberAt(field, 0), numberAt(field, 1), numberAt(field, 2));
    }

    private double numberAt(PartField field, int component) {
        try {
            return Double.parseDouble(get(field, component).trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    /** The line's values with one value of a field changed. */
    public String with(PartField field, int component, String text) {
        String[] old = tokens();
        if (field.kind() == PartField.Kind.REST) {
            String[] kept = Arrays.copyOf(old, Math.max(field.index(), 0));
            fillGaps(kept, old.length);
            StringJoiner out = new StringJoiner(", ");
            for (String token : kept) {
                out.add(token);
            }
            out.add(text.trim());
            return out.toString();
        }
        int at = field.index() + component;
        String[] tokens = Arrays.copyOf(old, Math.max(old.length, at + 1));
        fillGaps(tokens, old.length);
        tokens[at] = text.trim();
        return join(tokens, old.length);
    }

    /** The line's values with a field's three numbers changed. */
    public String with(PartField field, Vector3 v) {
        String[] old = tokens();
        int at = field.index();
        String[] tokens = Arrays.copyOf(old, Math.max(old.length, at + 3));
        fillGaps(tokens, old.length);
        tokens[at] = number(v.x());
        tokens[at + 1] = number(v.y());
        tokens[at + 2] = number(v.z());
        return join(tokens, old.length);
    }

    /** Where a change reaches past the end of the line, the values in between take their fallbacks. */
    private void fillGaps(String[] tokens, int from) {
        for (PartField field : type.fields()) {
            for (int i = 0; i < field.kind().width(); i++) {
                int at = field.index() + i;
                if (at >= from && at < tokens.length && tokens[at] == null) {
                    tokens[at] = PartType.fallback(field, i);
                }
            }
        }
    }

    private String join(String[] tokens, int current) {
        int length = type.lengthFor(tokens, current);
        String[] kept = Arrays.copyOf(tokens, Math.max(length, 0));
        fillGaps(kept, current);
        return String.join(", ", kept);
    }

    /** A number as it goes into a file: at most four decimals and no trailing zeros. */
    public static String number(double value) {
        BigDecimal rounded = BigDecimal.valueOf(value).setScale(DECIMALS, RoundingMode.HALF_UP).stripTrailingZeros();
        return rounded.signum() == 0 ? "0" : rounded.toPlainString();
    }
}
