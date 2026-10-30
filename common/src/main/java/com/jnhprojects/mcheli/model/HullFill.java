package com.jnhprojects.mcheli.model;

import com.jnhprojects.mcheli.content.Vector3;
import com.jnhprojects.mcheli.content.aircraft.BoundingBoxDefinition;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Not in the original: hit boxes for the parts of a model that its {@code BoundingBox} lines leave
 * out, such as the MD-90's tail fin. The model's surface is cut into cubes of one cell; every cell
 * a triangle passes through and no listed box already covers becomes part of the hull, and cells
 * stacked on top of each other merge into one column.
 *
 * <p>Columns stay as wide as one cell because hull boxes do not turn with the vehicle, only their
 * centres do: a long box would lie across the hull once the vehicle turned. Rotor blades and
 * propellers spin through a whole disc, so their parts are left out.
 */
public final class HullFill {
    /** The size of a cell, in blocks. */
    public static final double CELL = 1.0;
    /** A safety limit; a model this detailed gets no generated boxes rather than thousands. */
    static final int MAX_BOXES = 1500;

    private HullFill() {
    }

    /**
     * @param groups the model, every group in file order
     * @param listed the boxes the definition already has, which the fill does not repeat
     * @return boxes for the uncovered surface, in hull space, with a damage factor of 1
     */
    public static List<BoundingBoxDefinition> fill(List<MeshGroup> groups, List<BoundingBoxDefinition> listed) {
        Set<Cell> cells = new HashSet<>();
        String part = "";
        for (MeshGroup group : groups) {
            if (group.isPartHeader()) {
                part = group.name().toLowerCase(Locale.ROOT);
            }
            if (!spins(part)) {
                surface(group, cells);
            }
        }
        cells.removeIf(cell -> covered(cell, listed));
        List<BoundingBoxDefinition> boxes = columns(cells);
        return boxes.size() > MAX_BOXES ? List.of() : boxes;
    }

    /** Parts that turn through a full circle: helicopter rotors and plane propellers. */
    static boolean spins(String part) {
        return part.startsWith("$blade") || part.startsWith("$rotor") || part.startsWith("$rotpart");
    }

    /**
     * Marks every cell a triangle of {@code group} touches, by sampling each triangle at less than
     * half a cell apart.
     */
    private static void surface(MeshGroup group, Set<Cell> cells) {
        float[] p = group.positions();
        for (int i = 0; i + 8 < p.length; i += 9) {
            double ax = p[i];
            double ay = p[i + 1];
            double az = p[i + 2];
            double bx = p[i + 3] - ax;
            double by = p[i + 4] - ay;
            double bz = p[i + 5] - az;
            double cx = p[i + 6] - ax;
            double cy = p[i + 7] - ay;
            double cz = p[i + 8] - az;
            double longest = Math.max(Math.sqrt(bx * bx + by * by + bz * bz), Math.sqrt(cx * cx + cy * cy + cz * cz));
            int steps = Math.max(1, (int) Math.ceil(longest / (CELL * 0.4)));
            for (int s = 0; s <= steps; s++) {
                for (int t = 0; s + t <= steps; t++) {
                    double u = (double) s / steps;
                    double v = (double) t / steps;
                    cells.add(Cell.at(ax + bx * u + cx * v, ay + by * u + cy * v, az + bz * u + cz * v));
                }
            }
        }
    }

    /** Whether a listed box already holds the cell's centre. */
    private static boolean covered(Cell cell, List<BoundingBoxDefinition> listed) {
        double x = (cell.x + 0.5) * CELL;
        double y = (cell.y + 0.5) * CELL;
        double z = (cell.z + 0.5) * CELL;
        for (BoundingBoxDefinition box : listed) {
            Vector3 c = box.offset();
            double hw = box.width() / 2.0;
            double hh = box.height() / 2.0;
            if (Math.abs(x - c.x()) <= hw && Math.abs(y - c.y()) <= hh && Math.abs(z - c.z()) <= hw) {
                return true;
            }
        }
        return false;
    }

    /** Stacks of cells in the same column become one box each. */
    private static List<BoundingBoxDefinition> columns(Set<Cell> cells) {
        TreeMap<Long, TreeSet<Integer>> byColumn = new TreeMap<>();
        for (Cell cell : cells) {
            byColumn.computeIfAbsent(((long) cell.x << 32) | (cell.z & 0xFFFFFFFFL), k -> new TreeSet<>()).add(cell.y);
        }
        List<BoundingBoxDefinition> boxes = new ArrayList<>();
        byColumn.forEach((key, ys) -> {
            int x = (int) (key >> 32);
            int z = (int) (long) key;
            int start = ys.first();
            int previous = start;
            for (int y : ys.tailSet(start, false)) {
                if (y != previous + 1) {
                    boxes.add(column(x, z, start, previous));
                    start = y;
                }
                previous = y;
            }
            boxes.add(column(x, z, start, previous));
        });
        return boxes;
    }

    private static BoundingBoxDefinition column(int x, int z, int bottom, int top) {
        double height = (top - bottom + 1) * CELL;
        Vector3 centre = new Vector3((x + 0.5) * CELL, bottom * CELL + height / 2.0, (z + 0.5) * CELL);
        return new BoundingBoxDefinition(centre, (float) CELL, (float) height, 1.0F);
    }

    private record Cell(int x, int y, int z) {
        static Cell at(double x, double y, double z) {
            return new Cell((int) Math.floor(x / CELL), (int) Math.floor(y / CELL), (int) Math.floor(z / CELL));
        }
    }
}
