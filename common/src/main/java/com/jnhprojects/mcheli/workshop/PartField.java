package com.jnhprojects.mcheli.workshop;

/**
 * One value in a part's line, for example the position in {@code AddSeat = x, y, z}.
 *
 * @param id       names the field in the language files, {@code gui.mcheli.workshop.field.<id>}
 * @param kind     how it is edited
 * @param index    where it starts among the line's comma separated values
 * @param fallback the value used until it is set, the same the definition reader falls back to.
 *                 Three comma separated numbers for a vector.
 */
public record PartField(String id, Kind kind, int index, String fallback) {
    public enum Kind {
        NUMBER(1),
        WHOLE_NUMBER(1),
        BOOLEAN(1),
        /** A loaded weapon's name. */
        WEAPON(1),
        /** A point on the vehicle, which gets a gizmo. */
        POSITION(3),
        /** A direction or distance, three numbers with no gizmo of their own. */
        VECTOR(3),
        /** Every value from here to the end of the line, edited as text. */
        REST(1);

        private final int width;

        Kind(int width) {
            this.width = width;
        }

        /** How many comma separated values the field takes up. */
        public int width() {
            return width;
        }
    }

    static PartField position(int index) {
        return new PartField("position", Kind.POSITION, index, "0, 0, 0");
    }

    static PartField position(String id, int index) {
        return new PartField(id, Kind.POSITION, index, "0, 0, 0");
    }

    static PartField vector(String id, int index, String fallback) {
        return new PartField(id, Kind.VECTOR, index, fallback);
    }

    static PartField number(String id, int index, String fallback) {
        return new PartField(id, Kind.NUMBER, index, fallback);
    }

    static PartField whole(String id, int index, String fallback) {
        return new PartField(id, Kind.WHOLE_NUMBER, index, fallback);
    }

    static PartField bool(String id, int index, String fallback) {
        return new PartField(id, Kind.BOOLEAN, index, fallback);
    }

    static PartField weapon(int index) {
        return new PartField("weapon", Kind.WEAPON, index, "none");
    }

    static PartField rest(String id, int index, String fallback) {
        return new PartField(id, Kind.REST, index, fallback);
    }
}
