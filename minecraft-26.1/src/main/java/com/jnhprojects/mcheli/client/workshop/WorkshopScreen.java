package com.jnhprojects.mcheli.client.workshop;

import com.jnhprojects.mcheli.MCHeli;
import com.jnhprojects.mcheli.client.RuntimeAssets;
import com.jnhprojects.mcheli.client.render.AircraftRenderState;
import com.jnhprojects.mcheli.client.render.MeshCache;
import com.jnhprojects.mcheli.client.sound.OneShotSound;
import com.jnhprojects.mcheli.content.Vector3;
import com.jnhprojects.mcheli.content.aircraft.AircraftDefinition;
import com.jnhprojects.mcheli.content.aircraft.PitchCurve;
import com.jnhprojects.mcheli.content.aircraft.WeaponSound;
import com.jnhprojects.mcheli.content.pack.PackManifest;
import com.jnhprojects.mcheli.content.weapon.WeaponDefinition;
import com.jnhprojects.mcheli.platform.Platform;
import com.jnhprojects.mcheli.registry.ModEntities;
import com.jnhprojects.mcheli.workshop.AssetImport;
import com.jnhprojects.mcheli.workshop.EditHistory;
import com.jnhprojects.mcheli.workshop.PartField;
import com.jnhprojects.mcheli.workshop.PartType;
import com.jnhprojects.mcheli.workshop.TuningKey;
import com.jnhprojects.mcheli.workshop.WorkshopCamera;
import com.jnhprojects.mcheli.workshop.WorkshopException;
import com.jnhprojects.mcheli.workshop.WorkshopPart;
import com.jnhprojects.mcheli.workshop.WorkshopProject;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix3x2fStack;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import java.util.function.Consumer;

/**
 * The workshop's editor. The vehicle turns in the middle, drawn by the game's own vehicle renderer
 * through a GUI entity, so what shows is what will fly. The parts list is on the left, the values
 * of what is selected on the right, and gizmos for every part lie over the model.
 *
 * <p>Gizmos are plain GUI drawing at the points {@link WorkshopCamera#project} gives. They always
 * show, even when the part is inside the hull, which is where most seats and pivots are.
 *
 * <p>Left drag on empty space turns the view, right drag moves it, the wheel zooms. Dragging a
 * handle moves the part in the plane of the screen and an arrow along its axis, snapping to a
 * sixteenth of a block unless Shift is held.
 */
public class WorkshopScreen extends Screen {
    private static final int LEFT = 112;
    private static final int RIGHT = 144;
    private static final int TOP = 26;
    private static final int BOTTOM = 26;
    private static final int LIST_ROW = 16;
    private static final int FIELD_ROW = 20;
    private static final int LABEL_ROW = 10;

    private static final int PANEL = 0xFF181818;
    private static final int VIEW = 0xFF2A2F36;
    private static final int GRID = 0xFF3A414A;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int DIM = 0xFFA0A0A0;
    private static final int WARNING = 0xFFFFC040;
    private static final int INVALID = 0xFFFF6060;
    private static final int X_COLOR = 0xFFFF5050;
    private static final int Y_COLOR = 0xFF60E060;
    private static final int Z_COLOR = 0xFF5090FF;
    private static final int SELECTED = 0xFFFFFFFF;
    private static final int HIT_BOX = 0xFF00E0FF;
    private static final int AXIS = 0xFFFF60FF;

    private static final int HANDLE_RADIUS = 3;
    private static final double PICK_DISTANCE = 6.0;
    private static final double ARROW_LENGTH = 34.0;
    private static final double SNAP = 16.0;
    private static final int GRID_HALF_SIZE = 16;
    private static final int GRID_STEP = 2;
    private static final double ZOOM_STEP = 1.15;

    private static final int PREVIEW_SIZE = 64;
    private static final int PLOT_HEIGHT = 56;
    private static final float PITCH_SNAP = 20.0F;
    private static final String DEFAULT_CURVE = "0:0.7, 0.25:0.85, 0.5:1, 0.75:1.15, 1:1.3";

    private enum Tab {
        PARTS, PAINT, SOUND, TUNING, PACK
    }

    private enum Drag {
        NONE, ORBIT, PAN, PLANE, AXIS_X, AXIS_Y, AXIS_Z, CURVE
    }

    private record Label(Component text, int x, int y, int color) {
    }

    private record WrappedLine(FormattedCharSequence text, int x, int y) {
    }

    private record MergeKey(int line, String field, int component) {
    }

    private final WorkshopProject project;
    private final WorkshopCamera camera = new WorkshopCamera();
    private final EditHistory<List<String>> history = new EditHistory<>();
    private final List<Label> labels = new ArrayList<>();
    private final List<WrappedLine> wrapped = new ArrayList<>();
    /** The boxes showing the selected part's values, to follow a gizmo while it is dragged. */
    private final Map<PartField, EditBox[]> boxes = new HashMap<>();
    private final List<PartType> types = new ArrayList<>();

    private Tab tab = Tab.PARTS;
    private int addType;
    private int selected = -1;
    private @Nullable PartField activeHandle;
    private int listScroll;
    private int panelScroll;
    /** Where the last row of the right panel ended, to stop scrolling there. */
    private int panelEnd;
    private boolean framed;
    private boolean syncing;
    private boolean manifestChanged;
    private @Nullable Component message;
    private int messageColor = DIM;
    private int nextDrag;

    private Drag drag = Drag.NONE;
    private double dragX;
    private double dragY;
    private Vector3 dragStart = Vector3.ZERO;

    private @Nullable Button undoButton;
    private @Nullable Button redoButton;
    private @Nullable AircraftRenderState renderState;

    /** The part the Paint tab is on, {@code null} for the vehicle as a whole. */
    private @Nullable String paintPart;
    private WorkshopProject.SoundSlot soundSlot = new WorkshopProject.SoundSlot(WorkshopProject.SoundKind.ENGINE, null);
    /** Survives the rebuild an import or a scroll causes. */
    private String assetPath = "";
    private @Nullable Identifier previewTexture;
    private int previewX;
    private int previewY;
    private int previewWidth;
    private int previewHeight;
    private int previewTextureWidth;
    private int previewTextureHeight;
    /** Where the curve is drawn, or a height of 0 when none shows. */
    private int plotX;
    private int plotY;
    private int plotWidth;
    private int plotHeight;
    private int curvePoint;

    public WorkshopScreen(WorkshopProject project) {
        super(Component.translatable("gui.mcheli.workshop.title"));
        this.project = project;
        for (PartType type : PartType.values()) {
            if (type.appliesTo(project.kind())) {
                types.add(type);
            }
        }
        RuntimeAssets.setWorkshopRoot(project.assets());
        MeshCache.INSTANCE.clear();
        rebuildRenderState();
        if (!project.model().warnings().isEmpty()) {
            say(Component.literal(project.model().warnings().getFirst()), WARNING);
        }
    }

    private void say(Component text, int color) {
        message = text;
        messageColor = color;
    }

    private void rebuildRenderState() {
        AircraftDefinition def = project.definition();
        AircraftRenderState state = new AircraftRenderState();
        state.entityType = ModEntities.typeFor(def);
        state.definition = def;
        state.texture = def.textures().getFirst();
        state.inGui = true;
        renderState = state;
    }

    // Layout

    private int viewLeft() {
        return LEFT;
    }

    private int viewRight() {
        return Math.max(width - RIGHT, LEFT + 1);
    }

    private int viewTop() {
        return TOP;
    }

    private int viewBottom() {
        return Math.max(height - BOTTOM, TOP + 1);
    }

    private int centerX() {
        return (viewLeft() + viewRight()) / 2;
    }

    private int centerY() {
        return (viewTop() + viewBottom()) / 2;
    }

    private boolean overView(double x, double y) {
        return x >= viewLeft() && x < viewRight() && y >= viewTop() && y < viewBottom();
    }

    @Override
    protected void init() {
        labels.clear();
        wrapped.clear();
        boxes.clear();
        previewTexture = null;
        plotHeight = 0;
        if (!framed) {
            camera.frame(project.model().min(), project.model().max(), viewRight() - viewLeft(), viewBottom() - viewTop());
            framed = true;
        }
        topBar();
        switch (tab) {
            case PARTS -> {
                partsList();
                partPanel();
            }
            case PAINT -> {
                paintList();
                paintPanel();
            }
            case SOUND -> {
                soundList();
                soundPanel();
            }
            case TUNING -> tuningPanel();
            case PACK -> packPanel();
        }
        updateButtons();
    }

    private void topBar() {
        List<Button.Builder> buttons = new ArrayList<>();
        for (Tab each : Tab.values()) {
            buttons.add(Button.builder(Component.translatable("gui.mcheli.workshop.tab." + each.name().toLowerCase(Locale.ROOT)), b -> {
                tab = each;
                panelScroll = 0;
                listScroll = 0;
                rebuildWidgets();
            }));
        }
        buttons.add(Button.builder(Component.translatable("gui.mcheli.workshop.undo"), b -> undo()));
        buttons.add(Button.builder(Component.translatable("gui.mcheli.workshop.redo"), b -> redo()));
        buttons.add(Button.builder(Component.translatable("gui.mcheli.workshop.save"), b -> save()));
        buttons.add(Button.builder(Component.translatable("gui.mcheli.workshop.export"), b -> export()));
        Button.Builder flight = Button.builder(Component.translatable("gui.mcheli.workshop.test_flight"), b -> testFlight());
        boolean canFly = minecraft.hasSingleplayerServer();
        if (!canFly) {
            flight.tooltip(Tooltip.create(Component.translatable("gui.mcheli.workshop.test_flight_owner_only")));
        }
        buttons.add(flight);
        buttons.add(Button.builder(Component.translatable("gui.done"), b -> onClose()));

        int w = Math.min((width - 4) / buttons.size(), 62);
        int x = 2;
        for (int i = 0; i < buttons.size(); i++) {
            Button button = addRenderableWidget(buttons.get(i).bounds(x, 2, w - 1, 20).build());
            if (i < Tab.values().length) {
                button.active = Tab.values()[i] != tab;
            } else if (i == Tab.values().length) {
                undoButton = button;
            } else if (i == Tab.values().length + 1) {
                redoButton = button;
            } else if (i == buttons.size() - 2) {
                button.active = canFly;
            }
            x += w;
        }
    }

    private void updateButtons() {
        if (undoButton != null && redoButton != null) {
            undoButton.active = history.canUndo();
            redoButton.active = history.canRedo();
        }
    }

    // The parts list

    private void partsList() {
        int y = TOP + 2;
        PartType adding = types.get(addType);
        addRenderableWidget(Button.builder(Component.literal("<"), b -> stepAddType(-1)).bounds(2, y, 14, 16).build());
        labels.add(new Label(Component.literal(font.plainSubstrByWidth(partName(adding).getString(), LEFT - 54)), 18, y + 4, TEXT));
        addRenderableWidget(Button.builder(Component.literal(">"), b -> stepAddType(1)).bounds(LEFT - 34, y, 14, 16).build());
        addRenderableWidget(Button.builder(Component.literal("+"), b -> addPart())
                .tooltip(Tooltip.create(Component.translatable("gui.mcheli.workshop.add")))
                .bounds(LEFT - 18, y, 16, 16).build());

        List<WorkshopPart> parts = project.parts();
        int rows = listRows();
        listScroll = Math.clamp(listScroll, 0, Math.max(parts.size() - rows, 0));
        Map<PartType, Integer> numbers = new EnumMap<>(PartType.class);
        for (int i = 0; i < parts.size(); i++) {
            WorkshopPart part = parts.get(i);
            int number = numbers.merge(part.type(), 1, Integer::sum);
            if (i < listScroll || i >= listScroll + rows) {
                continue;
            }
            Component name = Component.literal(partName(part.type()).getString() + (part.type().isSingle() ? "" : " " + number));
            Button row = addRenderableWidget(Button.builder(name, b -> select(part.line()))
                    .bounds(2, TOP + 22 + (i - listScroll) * LIST_ROW, LEFT - 4, LIST_ROW).build());
            row.active = part.line() != selected;
        }
    }

    private int listRows() {
        return Math.max((viewBottom() - TOP - 24) / LIST_ROW, 1);
    }

    private Component partName(PartType type) {
        return Component.translatable("gui.mcheli.workshop.part." + type.id());
    }

    private void stepAddType(int direction) {
        addType = Math.floorMod(addType + direction, types.size());
        rebuildWidgets();
    }

    private void select(int line) {
        selected = line;
        WorkshopPart part = project.partAt(line);
        activeHandle = part != null ? part.type().mainPosition() : null;
        panelScroll = 0;
        history.endMerge();
        rebuildWidgets();
    }

    private void addPart() {
        PartType type = types.get(addType);
        String value = type.defaultValue();
        if (type.fields().getFirst().kind() == PartField.Kind.WEAPON) {
            value = value.replaceFirst("^none", firstWeapon());
        }
        List<String> before = project.document().snapshot();
        int line = project.addPart(type, selected, value);
        if (line < 0) {
            say(Component.translatable("gui.mcheli.workshop.needs_parent", partName(type.parent())), WARNING);
            return;
        }
        if (!project.document().snapshot().equals(before)) {
            history.record(before, null);
        }
        changed();
        select(line);
    }

    private String firstWeapon() {
        TreeSet<String> names = weaponNames();
        return names.isEmpty() ? "none" : names.first();
    }

    private static TreeSet<String> weaponNames() {
        TreeSet<String> names = new TreeSet<>();
        for (WeaponDefinition weapon : MCHeli.content().weapons()) {
            names.add(weapon.name());
        }
        return names;
    }

    private void deleteSelected() {
        WorkshopPart part = project.partAt(selected);
        if (part == null) {
            return;
        }
        history.record(project.document().snapshot(), null);
        // Its children only mean something behind it, so they go along.
        for (WorkshopPart child : project.parts().reversed()) {
            if (child.line() > part.line() && child.type().parent() == part.type()
                    && isChildOf(child.line(), part)) {
                project.document().removeLine(child.line());
            }
        }
        project.document().removeLine(part.line());
        selected = -1;
        activeHandle = null;
        changed();
        rebuildWidgets();
    }

    private boolean isChildOf(int line, WorkshopPart parent) {
        for (WorkshopPart other : project.parts()) {
            if (other.line() > parent.line() && other.line() < line && other.type() == parent.type()) {
                return false;
            }
        }
        return true;
    }

    /** A copy on the other side of the vehicle, since most parts come in pairs. */
    private void mirrorSelected() {
        WorkshopPart part = project.partAt(selected);
        if (part == null || part.type().isSingle()) {
            return;
        }
        String value = part.value();
        for (PartField field : part.type().fields()) {
            if (field.kind() == PartField.Kind.POSITION) {
                WorkshopPart current = new WorkshopPart(part.type(), part.line(), part.key(), value);
                Vector3 v = current.vector(field);
                value = current.with(field, new Vector3(-v.x(), v.y(), v.z()));
            }
        }
        history.record(project.document().snapshot(), null);
        int line = project.addPart(part.type(), selected, value);
        changed();
        select(line);
    }

    // The panels on the right

    /** Lays out one row at a time from the top of the right panel, skipping what is scrolled away. */
    private final class Rows {
        private final int x = width - RIGHT + 2;
        private final int w = RIGHT - 4;
        private int y = TOP + 2 - panelScroll;

        boolean shows(int height) {
            return y >= TOP && y + height <= viewBottom() - 22;
        }

        void label(Component text, int color) {
            if (shows(LABEL_ROW)) {
                labels.add(new Label(text, x, y + 1, color));
            }
            y += LABEL_ROW;
            panelEnd = y;
        }

        void next() {
            y += FIELD_ROW;
            panelEnd = y;
        }
    }

    private void partPanel() {
        Rows rows = new Rows();
        WorkshopPart part = project.partAt(selected);
        if (part == null) {
            for (FormattedCharSequence line : font.split(Component.translatable("gui.mcheli.workshop.select_hint"), rows.w)) {
                wrapped.add(new WrappedLine(line, rows.x, rows.y));
                rows.y += LABEL_ROW;
            }
            return;
        }
        rows.label(partName(part.type()), TEXT);
        for (PartField field : part.type().fields()) {
            fieldRow(rows, part, field);
        }
        int bottom = viewBottom() - 20;
        addRenderableWidget(Button.builder(Component.translatable("gui.mcheli.workshop.delete"), b -> deleteSelected())
                .bounds(rows.x, bottom, rows.w / 2 - 1, 18).build());
        Button mirror = addRenderableWidget(Button.builder(Component.translatable("gui.mcheli.workshop.mirror"), b -> mirrorSelected())
                .tooltip(Tooltip.create(Component.translatable("gui.mcheli.workshop.mirror_hint")))
                .bounds(rows.x + rows.w / 2 + 1, bottom, rows.w / 2 - 1, 18).build());
        mirror.active = !part.type().isSingle();
    }

    private Component fieldName(PartField field) {
        return Component.translatable("gui.mcheli.workshop.field." + field.id());
    }

    private void fieldRow(Rows rows, WorkshopPart part, PartField field) {
        switch (field.kind()) {
            case BOOLEAN -> {
                boolean on = Boolean.parseBoolean(part.get(field, 0).trim());
                if (rows.shows(FIELD_ROW)) {
                    addRenderableWidget(Button.builder(Component.empty().append(fieldName(field)).append(": ")
                                    .append(Component.translatable(on ? "gui.mcheli.workshop.on" : "gui.mcheli.workshop.off")), b -> {
                                applyField(field, 0, String.valueOf(!on));
                                rebuildWidgets();
                            })
                            .bounds(rows.x, rows.y, rows.w, FIELD_ROW - 2).build());
                }
                rows.next();
            }
            case POSITION, VECTOR -> {
                rows.label(fieldName(field), field.kind() == PartField.Kind.POSITION ? handleColor(field) : DIM);
                if (rows.shows(FIELD_ROW)) {
                    EditBox[] row = new EditBox[3];
                    int w = (rows.w - 4) / 3;
                    for (int i = 0; i < 3; i++) {
                        int component = i;
                        row[i] = numberBox(rows.x + i * (w + 2), rows.y, w, part.get(field, i), false,
                                text -> applyField(field, component, text));
                    }
                    boxes.put(field, row);
                }
                rows.next();
            }
            case NUMBER, WHOLE_NUMBER -> {
                if (rows.shows(FIELD_ROW)) {
                    labels.add(new Label(trimmed(fieldName(field), rows.w - 62), rows.x, rows.y + 5, DIM));
                    numberBox(rows.x + rows.w - 58, rows.y, 58, part.get(field, 0), field.kind() == PartField.Kind.WHOLE_NUMBER,
                            text -> applyField(field, 0, text));
                }
                rows.next();
            }
            case WEAPON -> {
                rows.label(fieldName(field), DIM);
                if (rows.shows(FIELD_ROW)) {
                    weaponRow(rows, part, field);
                }
                rows.next();
            }
            case REST -> {
                rows.label(fieldName(field), DIM);
                if (rows.shows(FIELD_ROW)) {
                    textBox(rows.x, rows.y, rows.w, part.get(field, 0), text -> applyField(field, 0, text));
                }
                rows.next();
            }
        }
    }

    private void weaponRow(Rows rows, WorkshopPart part, PartField field) {
        List<String> names = new ArrayList<>(weaponNames());
        String current = part.get(field, 0).trim();
        EditBox box = textBox(rows.x + 15, rows.y, rows.w - 30, current, text -> applyField(field, 0, text));
        Consumer<Integer> step = direction -> {
            if (!names.isEmpty()) {
                box.setValue(names.get(Math.floorMod(names.indexOf(box.getValue().trim()) + direction, names.size())));
            }
        };
        addRenderableWidget(Button.builder(Component.literal("<"), b -> step.accept(-1)).bounds(rows.x, rows.y, 14, FIELD_ROW - 2).build());
        addRenderableWidget(Button.builder(Component.literal(">"), b -> step.accept(1)).bounds(rows.x + rows.w - 14, rows.y, 14, FIELD_ROW - 2).build());
    }

    private Component trimmed(Component text, int maxWidth) {
        return Component.literal(font.plainSubstrByWidth(text.getString(), maxWidth));
    }

    private EditBox numberBox(int x, int y, int w, String value, boolean whole, Consumer<String> onValid) {
        EditBox box = new EditBox(font, x, y, w, FIELD_ROW - 2, Component.empty());
        box.setMaxLength(32);
        box.setValue(value.trim());
        box.moveCursorToStart(false);
        box.setResponder(text -> {
            if (syncing) {
                return;
            }
            boolean valid = isNumber(text, whole);
            box.setTextColor(valid ? 0xFFE0E0E0 : INVALID);
            if (valid) {
                onValid.accept(text);
            }
        });
        return addRenderableWidget(box);
    }

    private static boolean isNumber(String text, boolean whole) {
        try {
            if (whole) {
                Integer.parseInt(text.trim());
            } else {
                Float.parseFloat(text.trim());
            }
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private EditBox textBox(int x, int y, int w, String value, Consumer<String> onChange) {
        EditBox box = new EditBox(font, x, y, w, FIELD_ROW - 2, Component.empty());
        box.setMaxLength(1024);
        box.setValue(value);
        box.moveCursorToStart(false);
        box.setResponder(text -> {
            if (!syncing) {
                onChange.accept(text);
            }
        });
        return addRenderableWidget(box);
    }

    private void applyField(PartField field, int component, String text) {
        WorkshopPart part = project.partAt(selected);
        if (part == null) {
            return;
        }
        history.record(project.document().snapshot(), new MergeKey(selected, field.id(), component));
        project.document().setLine(part.line(), part.key(), part.with(field, component, text));
        changed();
    }

    private void tuningPanel() {
        Rows rows = new Rows();
        AircraftDefinition blank = project.kind().create("blank");
        for (TuningKey key : TuningKey.ALL) {
            if (!key.applies().test(blank)) {
                continue;
            }
            String inFile = project.document().value(key.key());
            String shown = inFile != null ? inFile : key.defaultFor(project.kind());
            Component name = Component.translatable("gui.mcheli.workshop.tuning." + key.id());
            if (rows.shows(FIELD_ROW)) {
                switch (key.type()) {
                    case NUMBER -> {
                        labels.add(new Label(trimmed(name, rows.w - 62), rows.x, rows.y + 5, inFile != null ? TEXT : DIM));
                        tuningBox(rows, key, shown);
                    }
                    case SWITCH -> {
                        boolean on = Boolean.parseBoolean(shown.trim());
                        addRenderableWidget(Button.builder(Component.empty().append(name).append(": ")
                                        .append(Component.translatable(on ? "gui.mcheli.workshop.on" : "gui.mcheli.workshop.off")), b -> {
                                    setTuning(key, String.valueOf(!on));
                                    rebuildWidgets();
                                })
                                .bounds(rows.x, rows.y, rows.w, FIELD_ROW - 2).build());
                    }
                    case CHOICE -> addRenderableWidget(Button.builder(Component.empty().append(name).append(": ").append(shown.trim()), b -> {
                                int next = (key.choices().indexOf(shown.trim().toLowerCase(Locale.ROOT)) + 1) % key.choices().size();
                                setTuning(key, key.choices().get(next));
                                rebuildWidgets();
                            })
                            .bounds(rows.x, rows.y, rows.w, FIELD_ROW - 2).build());
                }
            }
            rows.next();
        }
    }

    private void tuningBox(Rows rows, TuningKey key, String shown) {
        EditBox box = new EditBox(font, rows.x + rows.w - 58, rows.y, 58, FIELD_ROW - 2, Component.empty());
        box.setMaxLength(16);
        box.setValue(shown.trim());
        box.moveCursorToStart(false);
        box.setResponder(text -> {
            if (text.isBlank()) {
                // An empty box goes back to the default.
                box.setTextColor(0xFFE0E0E0);
                setTuning(key, null);
            } else if (key.accepts(text)) {
                box.setTextColor(0xFFE0E0E0);
                setTuning(key, text.trim());
            } else {
                box.setTextColor(INVALID);
            }
        });
        addRenderableWidget(box);
    }

    private void setTuning(TuningKey key, @Nullable String value) {
        history.record(project.document().snapshot(), key.key());
        project.document().set(key.key(), value);
        changed();
    }

    private void packPanel() {
        Rows rows = new Rows();
        PackManifest m = project.manifest();
        packRow(rows, "gui.mcheli.workshop.pack.display_name", project.document().value("DisplayName") != null ? project.document().value("DisplayName") : project.vehicle(), text -> {
            history.record(project.document().snapshot(), "DisplayName");
            project.document().set("DisplayName", text.isBlank() ? null : text.trim());
            changed();
        });
        packRow(rows, "gui.mcheli.workshop.pack.name", m.name(), text -> manifest(new PackManifest(m.id(), text, project.manifest().author(), project.manifest().version(), project.manifest().description())));
        packRow(rows, "gui.mcheli.workshop.pack.author", m.author(), text -> manifest(new PackManifest(m.id(), project.manifest().name(), text, project.manifest().version(), project.manifest().description())));
        packRow(rows, "gui.mcheli.workshop.pack.version", m.version(), text -> manifest(new PackManifest(m.id(), project.manifest().name(), project.manifest().author(), text, project.manifest().description())));
        packRow(rows, "gui.mcheli.workshop.pack.description", m.description(), text -> manifest(new PackManifest(m.id(), project.manifest().name(), project.manifest().author(), project.manifest().version(), text)));
        rows.label(Component.translatable("gui.mcheli.workshop.pack.id", m.id()), DIM);
        rows.label(Component.translatable("gui.mcheli.workshop.pack.vehicle", project.vehicle()), DIM);
        // Whoever hosts the game has the pack already. A guest or a dedicated server's player does not.
        if (!minecraft.hasSingleplayerServer() && rows.shows(FIELD_ROW)) {
            addRenderableWidget(Button.builder(Component.translatable("gui.mcheli.workshop.submit"), b -> submit())
                    .tooltip(Tooltip.create(Component.translatable("gui.mcheli.workshop.submit_hint")))
                    .bounds(rows.x, rows.y, rows.w, FIELD_ROW - 2).build());
            rows.next();
        }
    }

    private void submit() {
        try {
            Path zip = project.export(Platform.get().gameDir().resolve(MCHeli.MOD_ID).resolve("workshop"));
            manifestChanged = false;
            history.endMerge();
            WorkshopClient.submit(Files.readAllBytes(zip));
            // The editor covers the chat, where the server's answer will come.
            say(Component.translatable("gui.mcheli.workshop.submit_sent"), DIM);
        } catch (WorkshopException e) {
            say(Component.literal(e.getMessage()), INVALID);
        } catch (IOException e) {
            say(Component.literal(e.toString()), INVALID);
        }
    }

    // The Paint tab

    private void paintList() {
        List<String> parts = project.texturableParts();
        int rows = listRows();
        listScroll = Math.clamp(listScroll, 0, Math.max(parts.size() + 1 - rows, 0));
        for (int i = listScroll; i <= parts.size() && i < listScroll + rows; i++) {
            String part = i == 0 ? null : parts.get(i - 1);
            Component name = part == null ? Component.translatable("gui.mcheli.workshop.paint.whole") : Component.literal(part);
            Button row = addRenderableWidget(Button.builder(name, b -> {
                paintPart = part;
                panelScroll = 0;
                rebuildWidgets();
            }).bounds(2, TOP + 2 + (i - listScroll) * LIST_ROW, LEFT - 4, LIST_ROW).build());
            row.active = !Objects.equals(part, paintPart);
        }
    }

    private void paintPanel() {
        if (paintPart != null && !project.texturableParts().contains(paintPart)) {
            paintPart = null;
        }
        Rows rows = new Rows();
        String own = paintPart != null ? project.partTexture(paintPart) : project.mainTexture();
        rows.label(paintPart != null ? Component.literal(paintPart) : Component.translatable("gui.mcheli.workshop.paint.whole"), TEXT);
        rows.label(trimmed(own != null ? Component.translatable("gui.mcheli.workshop.paint.current", own + ".png")
                : Component.translatable("gui.mcheli.workshop.paint.vehicle_paint"), rows.w), DIM);
        preview(rows, own != null ? own : project.mainTexture());
        assetRow(rows, "gui.mcheli.workshop.paint.file", path -> importTexture(path));
        if (paintPart != null && own != null && rows.shows(FIELD_ROW)) {
            addRenderableWidget(Button.builder(Component.translatable("gui.mcheli.workshop.clear"), b -> {
                history.record(project.document().snapshot(), null);
                project.clearPartTexture(paintPart);
                changed();
                rebuildWidgets();
            }).bounds(rows.x, rows.y, rows.w, FIELD_ROW - 2).build());
            rows.next();
        }
        hint(rows, "gui.mcheli.workshop.paint.drop");
    }

    /** Reserves room for the texture itself, fitted into a square, and remembers it for drawing. */
    private void preview(Rows rows, String texture) {
        Path file = project.assets().resolve("textures").resolve(project.kind().folder()).resolve(texture + ".png");
        if (rows.shows(PREVIEW_SIZE) && Files.isRegularFile(file)) {
            try {
                AssetImport.Texture image = AssetImport.texture(file);
                float scale = (float) PREVIEW_SIZE / Math.max(image.width(), image.height());
                previewTexture = RuntimeAssets.texture(textureId(texture));
                previewX = rows.x;
                previewY = rows.y;
                previewWidth = Math.max(Math.round(image.width() * scale), 1);
                previewHeight = Math.max(Math.round(image.height() * scale), 1);
                previewTextureWidth = image.width();
                previewTextureHeight = image.height();
            } catch (WorkshopException e) {
                labels.add(new Label(trimmed(Component.literal(e.getMessage()), rows.w), rows.x, rows.y, INVALID));
            }
        }
        rows.y += PREVIEW_SIZE + 2;
        panelEnd = rows.y;
    }

    private Identifier textureId(String name) {
        return Identifier.fromNamespaceAndPath(MCHeli.MOD_ID, "textures/" + project.kind().folder() + "/" + name + ".png");
    }

    /** A box for a file's path with an Import button, for when dropping a file is not an option. */
    private void assetRow(Rows rows, String name, Consumer<Path> onImport) {
        rows.label(Component.translatable(name), DIM);
        if (rows.shows(FIELD_ROW)) {
            int buttonWidth = 40;
            textBox(rows.x, rows.y, rows.w - buttonWidth - 2, assetPath, text -> assetPath = text);
            addRenderableWidget(Button.builder(Component.translatable("gui.mcheli.workshop.import"), b -> {
                try {
                    onImport.accept(Path.of(assetPath.trim().replace("\"", "")));
                } catch (InvalidPathException e) {
                    say(Component.translatable("gui.mcheli.workshop.new.bad_path", assetPath), INVALID);
                }
            }).bounds(rows.x + rows.w - buttonWidth, rows.y, buttonWidth, FIELD_ROW - 2).build());
        }
        rows.next();
    }

    private void hint(Rows rows, String key) {
        for (FormattedCharSequence line : font.split(Component.translatable(key), rows.w)) {
            if (rows.shows(LABEL_ROW)) {
                wrapped.add(new WrappedLine(line, rows.x, rows.y));
            }
            rows.y += LABEL_ROW;
            panelEnd = rows.y;
        }
    }

    private void importTexture(Path png) {
        List<String> before = project.document().snapshot();
        try {
            WorkshopProject.ImportedTexture imported = project.importTexture(paintPart, png);
            if (!project.document().snapshot().equals(before)) {
                history.record(before, null);
            }
            RuntimeAssets.refreshTexture(textureId(imported.name()));
            // Glass is worked out from the texture, so the model's split has to be redone.
            MeshCache.INSTANCE.clear();
            assetPath = "";
            changed();
            rebuildWidgets();
            List<String> warnings = imported.checked().warnings();
            say(warnings.isEmpty() ? Component.translatable("gui.mcheli.workshop.paint.imported", imported.name() + ".png")
                    : Component.literal(warnings.getFirst()), warnings.isEmpty() ? DIM : WARNING);
        } catch (WorkshopException e) {
            say(Component.literal(e.getMessage()), INVALID);
        }
    }

    // The Sound tab

    private Component slotName(WorkshopProject.SoundSlot slot) {
        return switch (slot.kind()) {
            case ENGINE -> Component.translatable("gui.mcheli.workshop.sound.engine");
            case ROTOR -> Component.translatable("gui.mcheli.workshop.sound.rotor");
            case WEAPON -> Component.literal(slot.weapon());
        };
    }

    private void soundList() {
        List<WorkshopProject.SoundSlot> slots = project.soundSlots();
        if (!slots.contains(soundSlot)) {
            soundSlot = slots.getFirst();
        }
        int rows = listRows();
        listScroll = Math.clamp(listScroll, 0, Math.max(slots.size() - rows, 0));
        for (int i = listScroll; i < slots.size() && i < listScroll + rows; i++) {
            WorkshopProject.SoundSlot slot = slots.get(i);
            Button row = addRenderableWidget(Button.builder(slotName(slot), b -> {
                soundSlot = slot;
                panelScroll = 0;
                rebuildWidgets();
            }).bounds(2, TOP + 2 + (i - listScroll) * LIST_ROW, LEFT - 4, LIST_ROW).build());
            row.active = !slot.equals(soundSlot);
        }
    }

    /** The sound the game would play for a slot, or empty for none. */
    private String effectiveSound(WorkshopProject.SoundSlot slot) {
        String own = project.sound(slot);
        if (own != null) {
            return own;
        }
        return switch (slot.kind()) {
            case ENGINE -> project.definition().soundOrDefault();
            case ROTOR -> "";
            case WEAPON -> {
                WeaponDefinition weapon = MCHeli.content().weapon(slot.weapon());
                yield weapon != null ? weapon.sound : "";
            }
        };
    }

    private void soundPanel() {
        Rows rows = new Rows();
        String own = project.sound(soundSlot);
        rows.label(slotName(soundSlot), TEXT);
        rows.label(trimmed(own != null ? Component.translatable("gui.mcheli.workshop.sound.current", own + ".ogg") : Component.translatable(switch (soundSlot.kind()) {
            case ENGINE -> "gui.mcheli.workshop.sound.default";
            case ROTOR -> "gui.mcheli.workshop.sound.none";
            case WEAPON -> "gui.mcheli.workshop.sound.weapon_default";
        }), rows.w), DIM);
        assetRow(rows, "gui.mcheli.workshop.sound.file", path -> importSound(path));
        if (rows.shows(FIELD_ROW)) {
            int half = rows.w / 2 - 1;
            Button play = addRenderableWidget(Button.builder(Component.translatable("gui.mcheli.workshop.sound.play"), b -> playSlot())
                    .bounds(rows.x, rows.y, half, FIELD_ROW - 2).build());
            play.active = !effectiveSound(soundSlot).isEmpty();
            Button clear = addRenderableWidget(Button.builder(Component.translatable("gui.mcheli.workshop.clear"), b -> {
                history.record(project.document().snapshot(), null);
                project.setSound(soundSlot, null);
                changed();
                rebuildWidgets();
            }).bounds(rows.x + rows.w - half, rows.y, half, FIELD_ROW - 2).build());
            clear.active = own != null;
        }
        rows.next();
        switch (soundSlot.kind()) {
            case ENGINE -> loopControls(rows, "SoundVolume", project.definition().soundVolume, "SoundPitchCurve");
            case ROTOR -> loopControls(rows, "RotorSoundVolume", project.definition().rotorSoundVolume, "RotorSoundPitchCurve");
            case WEAPON -> weaponPitchRow(rows);
        }
        hint(rows, "gui.mcheli.workshop.sound.drop");
    }

    private void loopControls(Rows rows, String volumeKey, float volume, String curveKey) {
        if (rows.shows(FIELD_ROW)) {
            labels.add(new Label(Component.translatable("gui.mcheli.workshop.sound.volume"), rows.x, rows.y + 5, DIM));
            numberBox(rows.x + rows.w - 58, rows.y, 58, WorkshopPart.number(volume), false, text -> {
                history.record(project.document().snapshot(), volumeKey);
                project.document().set(volumeKey, text.trim());
                changed();
            });
        }
        rows.next();
        PitchCurve curve = curve(curveKey);
        if (curve != null) {
            rows.label(Component.translatable("gui.mcheli.workshop.sound.curve"), DIM);
            if (rows.shows(PLOT_HEIGHT)) {
                plotX = rows.x + 2;
                plotY = rows.y + 2;
                plotWidth = rows.w - 4;
                plotHeight = PLOT_HEIGHT - 4;
            }
            rows.y += PLOT_HEIGHT;
            panelEnd = rows.y;
        }
        if (rows.shows(FIELD_ROW)) {
            addRenderableWidget(Button.builder(Component.translatable(curve == null ? "gui.mcheli.workshop.sound.curve_add" : "gui.mcheli.workshop.sound.curve_remove"), b -> {
                history.record(project.document().snapshot(), null);
                project.document().set(curveKey, curve == null ? DEFAULT_CURVE : null);
                changed();
                rebuildWidgets();
            }).bounds(rows.x, rows.y, rows.w, FIELD_ROW - 2).build());
        }
        rows.next();
    }

    private void weaponPitchRow(Rows rows) {
        WeaponSound own = project.definition().weaponSounds.get(soundSlot.weapon());
        if (own != null && rows.shows(FIELD_ROW)) {
            labels.add(new Label(Component.translatable("gui.mcheli.workshop.sound.pitch"), rows.x, rows.y + 5, DIM));
            String shown = own.pitch() != null ? WorkshopPart.number(own.pitch()) : "1";
            numberBox(rows.x + rows.w - 58, rows.y, 58, shown, false, text -> {
                history.record(project.document().snapshot(), "WeaponSound " + soundSlot.weapon());
                project.document().setKeyed("WeaponSound", soundSlot.weapon(), soundSlot.weapon() + ", " + own.sound() + ", " + text.trim());
                changed();
            });
        }
        rows.next();
    }

    private String curveKey() {
        return soundSlot.kind() == WorkshopProject.SoundKind.ROTOR ? "RotorSoundPitchCurve" : "SoundPitchCurve";
    }

    private @Nullable PitchCurve curve(String key) {
        String text = project.document().value(key);
        if (text == null) {
            return null;
        }
        try {
            return PitchCurve.parse(text);
        } catch (IllegalArgumentException e) {
            // Mistyped by hand. The line is reported below the view, and there is nothing to draw.
            return null;
        }
    }

    private void importSound(Path ogg) {
        List<String> before = project.document().snapshot();
        try {
            WorkshopProject.ImportedSound imported = project.importSound(soundSlot, ogg);
            if (!project.document().snapshot().equals(before)) {
                history.record(before, null);
            }
            assetPath = "";
            changed();
            rebuildWidgets();
            say(imported.warnings().isEmpty() ? Component.translatable("gui.mcheli.workshop.sound.imported", imported.name() + ".ogg")
                    : Component.literal(imported.warnings().getFirst()), imported.warnings().isEmpty() ? DIM : WARNING);
        } catch (WorkshopException e) {
            say(Component.literal(e.getMessage()), INVALID);
        }
    }

    /** Plays the slot's sound once, at full throttle if it has a curve, where the player stands. */
    private void playSlot() {
        String sound = effectiveSound(soundSlot);
        if (sound.isEmpty() || minecraft.player == null) {
            return;
        }
        float pitch = 1.0F;
        if (soundSlot.kind() != WorkshopProject.SoundKind.WEAPON) {
            PitchCurve curve = curve(curveKey());
            pitch = curve != null ? curve.at(1.0F) : 1.0F;
        }
        minecraft.getSoundManager().play(new OneShotSound(sound, minecraft.player.getX(), minecraft.player.getY(), minecraft.player.getZ(), 1.0F, pitch));
    }

    /** A file on the window goes wherever the tab that is open can take it. */
    @Override
    public void onFilesDrop(List<Path> files) {
        if (files.isEmpty()) {
            return;
        }
        switch (tab) {
            case PAINT -> importTexture(files.getFirst());
            case SOUND -> importSound(files.getFirst());
            default -> say(Component.translatable("gui.mcheli.workshop.drop_where"), WARNING);
        }
    }

    private double plotPointX(PitchCurve curve, int point) {
        return plotX + curve.throttle(point) * plotWidth;
    }

    private double plotPointY(float pitch) {
        return plotY + plotHeight * (1.0F - (pitch - PitchCurve.MIN_PITCH) / (PitchCurve.MAX_PITCH - PitchCurve.MIN_PITCH));
    }

    private int curvePointAt(double x, double y) {
        PitchCurve curve = tab == Tab.SOUND && plotHeight > 0 ? curve(curveKey()) : null;
        if (curve == null) {
            return -1;
        }
        for (int i = 0; i < curve.size(); i++) {
            if (Math.hypot(x - plotPointX(curve, i), y - plotPointY(curve.pitch(i))) < PICK_DISTANCE) {
                return i;
            }
        }
        return -1;
    }

    private void dragCurve(double y) {
        PitchCurve curve = curve(curveKey());
        if (curve == null || curvePoint >= curve.size()) {
            return;
        }
        float f = 1.0F - (float) (y - plotY) / plotHeight;
        float pitch = PitchCurve.MIN_PITCH + f * (PitchCurve.MAX_PITCH - PitchCurve.MIN_PITCH);
        history.record(project.document().snapshot(), "drag" + nextDrag);
        project.document().set(curveKey(), curve.withPitch(curvePoint, Math.round(pitch * PITCH_SNAP) / PITCH_SNAP).text());
        changed();
    }

    private void plot(GuiGraphicsExtractor graphics) {
        PitchCurve curve = plotHeight > 0 ? curve(curveKey()) : null;
        if (curve == null) {
            return;
        }
        graphics.fill(plotX, plotY, plotX + plotWidth, plotY + plotHeight, VIEW);
        // Normal pitch, for scale.
        graphics.fill(plotX, (int) plotPointY(1.0F), plotX + plotWidth, (int) plotPointY(1.0F) + 1, GRID);
        for (int i = 1; i < curve.size(); i++) {
            segment(graphics, plotPointX(curve, i - 1), plotPointY(curve.pitch(i - 1)), plotPointX(curve, i), plotPointY(curve.pitch(i)), Y_COLOR);
        }
        for (int i = 0; i < curve.size(); i++) {
            square(graphics, plotPointX(curve, i), plotPointY(curve.pitch(i)), HANDLE_RADIUS, drag == Drag.CURVE && curvePoint == i ? SELECTED : Y_COLOR);
        }
    }

    private void manifest(PackManifest manifest) {
        project.setManifest(manifest);
        manifestChanged = true;
    }

    private void packRow(Rows rows, String name, String value, Consumer<String> onChange) {
        rows.label(Component.translatable(name), DIM);
        if (rows.shows(FIELD_ROW)) {
            textBox(rows.x, rows.y, rows.w, value, onChange);
        }
        rows.next();
    }

    // Edits

    private void changed() {
        project.refresh();
        rebuildRenderState();
        message = null;
        updateButtons();
    }

    private void undo() {
        if (history.canUndo()) {
            project.document().restore(history.undo(project.document().snapshot()));
            afterHistory();
        }
    }

    private void redo() {
        if (history.canRedo()) {
            project.document().restore(history.redo(project.document().snapshot()));
            afterHistory();
        }
    }

    private void afterHistory() {
        if (project.partAt(selected) == null) {
            selected = -1;
            activeHandle = null;
        }
        changed();
        rebuildWidgets();
    }

    private void save() {
        try {
            project.save();
            manifestChanged = false;
            history.endMerge();
            say(Component.translatable("gui.mcheli.workshop.saved", MCHeli.MOD_ID + "/packs/" + project.root().getFileName()), DIM);
        } catch (WorkshopException e) {
            say(Component.literal(e.getMessage()), INVALID);
        }
    }

    private void export() {
        try {
            Path zip = project.export(Platform.get().gameDir().resolve(MCHeli.MOD_ID).resolve("workshop"));
            manifestChanged = false;
            history.endMerge();
            say(Component.translatable("gui.mcheli.workshop.exported", MCHeli.MOD_ID + "/workshop/" + zip.getFileName()), DIM);
        } catch (WorkshopException e) {
            say(Component.literal(e.getMessage()), INVALID);
        }
    }

    private void testFlight() {
        for (String problem : project.problems()) {
            if (problem.startsWith("rejected")) {
                say(Component.literal(problem), INVALID);
                return;
            }
        }
        try {
            project.save();
            manifestChanged = false;
        } catch (WorkshopException e) {
            say(Component.literal(e.getMessage()), INVALID);
            return;
        }
        WorkshopClient.startTestFlight(this, project.vehicle());
    }

    @Override
    public void onClose() {
        if (project.isDirty() || manifestChanged) {
            minecraft.setScreen(new ConfirmScreen(saveIt -> {
                if (saveIt) {
                    try {
                        project.save();
                    } catch (WorkshopException e) {
                        say(Component.literal(e.getMessage()), INVALID);
                        minecraft.setScreen(this);
                        return;
                    }
                }
                closeEditor();
            }, Component.translatable("gui.mcheli.workshop.unsaved"), Component.translatable("gui.mcheli.workshop.unsaved_question"),
                    Component.translatable("gui.mcheli.workshop.save"), Component.translatable("gui.mcheli.workshop.discard")));
            return;
        }
        closeEditor();
    }

    private void closeEditor() {
        releaseAssets();
        minecraft.setScreen(new WorkshopHomeScreen());
    }

    void releaseAssets() {
        RuntimeAssets.setWorkshopRoot(null);
        MeshCache.INSTANCE.clear();
    }

    // Input

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.hasControlDownWithQuirk()) {
            if (event.key() == InputConstants.KEY_Z) {
                if (event.hasShiftDown()) {
                    redo();
                } else {
                    undo();
                }
                return true;
            }
            if (event.key() == InputConstants.KEY_Y) {
                redo();
                return true;
            }
            if (event.key() == InputConstants.KEY_S) {
                save();
                return true;
            }
        }
        if (!(getFocused() instanceof EditBox)) {
            if (event.key() == InputConstants.KEY_R) {
                camera.frame(project.model().min(), project.model().max(), viewRight() - viewLeft(), viewBottom() - viewTop());
                return true;
            }
            if (event.key() == InputConstants.KEY_DELETE) {
                deleteSelected();
                return true;
            }
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        int point = curvePointAt(event.x(), event.y());
        if (point >= 0) {
            setFocused(null);
            drag = Drag.CURVE;
            curvePoint = point;
            nextDrag++;
            history.endMerge();
            return true;
        }
        if (!overView(event.x(), event.y())) {
            return false;
        }
        setFocused(null);
        dragX = 0.0;
        dragY = 0.0;
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT) {
            drag = Drag.PAN;
            return true;
        }
        drag = pickArrow(event.x(), event.y());
        if (drag == Drag.NONE && !pickHandle(event.x(), event.y())) {
            drag = Drag.ORBIT;
        }
        if (drag != Drag.ORBIT) {
            startMoving();
        }
        return true;
    }

    private void startMoving() {
        WorkshopPart part = project.partAt(selected);
        if (part == null || activeHandle == null) {
            drag = Drag.ORBIT;
            return;
        }
        dragStart = part.vector(activeHandle);
        nextDrag++;
        history.endMerge();
    }

    /** Which arrow of the selected handle is under the mouse. */
    private Drag pickArrow(double x, double y) {
        Vector3 at = activePosition();
        if (at == null) {
            return Drag.NONE;
        }
        Drag[] arrows = {Drag.AXIS_X, Drag.AXIS_Y, Drag.AXIS_Z};
        for (int i = 0; i < 3; i++) {
            double[] tip = arrowTip(at, i);
            if (distanceToSegment(x, y, screenX(at), screenY(at), tip[0], tip[1]) < PICK_DISTANCE * 0.8
                    && Math.hypot(x - screenX(at), y - screenY(at)) > HANDLE_RADIUS + 1) {
                return arrows[i];
            }
        }
        return Drag.NONE;
    }

    /** Selects the handle under the mouse, the nearest to the viewer if several overlap. */
    private boolean pickHandle(double x, double y) {
        WorkshopPart best = null;
        PartField bestField = null;
        double bestDepth = Double.MAX_VALUE;
        for (WorkshopPart part : project.parts()) {
            for (PartField field : part.type().fields()) {
                if (field.kind() != PartField.Kind.POSITION) {
                    continue;
                }
                Vector3 v = part.vector(field);
                Vector3f p = camera.project(v.x(), v.y(), v.z());
                if (Math.hypot(x - (centerX() + p.x), y - (centerY() + p.y)) < PICK_DISTANCE && p.z < bestDepth) {
                    best = part;
                    bestField = field;
                    bestDepth = p.z;
                }
            }
        }
        if (best == null) {
            return false;
        }
        boolean samePart = best.line() == selected;
        selected = best.line();
        activeHandle = bestField;
        if (!samePart) {
            rebuildWidgets();
        }
        drag = Drag.PLANE;
        return true;
    }

    private static double distanceToSegment(double x, double y, double x0, double y0, double x1, double y1) {
        double dx = x1 - x0;
        double dy = y1 - y0;
        double lengthSq = dx * dx + dy * dy;
        double t = lengthSq > 0.0 ? Math.clamp(((x - x0) * dx + (y - y0) * dy) / lengthSq, 0.0, 1.0) : 0.0;
        return Math.hypot(x - (x0 + t * dx), y - (y0 + t * dy));
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        switch (drag) {
            case NONE -> {
                return super.mouseDragged(event, dx, dy);
            }
            case ORBIT -> camera.orbit(dx, dy);
            case PAN -> camera.pan(dx, dy);
            case CURVE -> dragCurve(event.y());
            case PLANE, AXIS_X, AXIS_Y, AXIS_Z -> {
                dragX += dx;
                dragY += dy;
                moveActive(event.hasShiftDown());
            }
        }
        return true;
    }

    private void moveActive(boolean precise) {
        WorkshopPart part = project.partAt(selected);
        if (part == null || activeHandle == null) {
            return;
        }
        double x = dragStart.x();
        double y = dragStart.y();
        double z = dragStart.z();
        if (drag == Drag.PLANE) {
            Vector3f move = camera.unproject(dragX, dragY);
            x += move.x;
            y += move.y;
            z += move.z;
        } else {
            int axis = drag.ordinal() - Drag.AXIS_X.ordinal();
            Vector3f origin = camera.project(x, y, z);
            Vector3f unit = camera.project(x + (axis == 0 ? 1 : 0), y + (axis == 1 ? 1 : 0), z + (axis == 2 ? 1 : 0));
            double ax = unit.x - origin.x;
            double ay = unit.y - origin.y;
            double lengthSq = ax * ax + ay * ay;
            if (lengthSq < 1.0) {
                // The axis points straight at the viewer, so there is no telling how far.
                return;
            }
            double amount = (dragX * ax + dragY * ay) / lengthSq;
            x += axis == 0 ? amount : 0.0;
            y += axis == 1 ? amount : 0.0;
            z += axis == 2 ? amount : 0.0;
        }
        Vector3 moved = precise ? new Vector3(x, y, z) : new Vector3(snap(x), snap(y), snap(z));
        history.record(project.document().snapshot(), "drag" + nextDrag);
        project.document().setLine(part.line(), part.key(), part.with(activeHandle, moved));
        changed();
        syncBoxes(moved);
    }

    private static double snap(double value) {
        return Math.round(value * SNAP) / SNAP;
    }

    private void syncBoxes(Vector3 v) {
        EditBox[] row = boxes.get(activeHandle);
        if (row != null) {
            syncing = true;
            row[0].setValue(WorkshopPart.number(v.x()));
            row[1].setValue(WorkshopPart.number(v.y()));
            row[2].setValue(WorkshopPart.number(v.z()));
            // A box scrolled to its end could hide a minus sign.
            for (EditBox box : row) {
                box.moveCursorToStart(false);
            }
            syncing = false;
        }
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (drag != Drag.NONE) {
            drag = Drag.NONE;
            history.endMerge();
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (overView(x, y)) {
            camera.zoom(scrollY > 0 ? ZOOM_STEP : 1.0 / ZOOM_STEP);
            return true;
        }
        if (x < LEFT) {
            listScroll = Math.max(listScroll - (int) Math.signum(scrollY), 0);
        } else if (scrollY > 0 || panelEnd > viewBottom() - 22) {
            panelScroll = Math.max(panelScroll - (int) Math.signum(scrollY) * FIELD_ROW, 0);
        }
        rebuildWidgets();
        return true;
    }

    // Drawing

    private double screenX(Vector3 v) {
        return centerX() + camera.project(v.x(), v.y(), v.z()).x;
    }

    private double screenY(Vector3 v) {
        return centerY() + camera.project(v.x(), v.y(), v.z()).y;
    }

    private @Nullable Vector3 activePosition() {
        WorkshopPart part = project.partAt(selected);
        return part != null && activeHandle != null ? part.vector(activeHandle) : null;
    }

    private double[] arrowTip(Vector3 at, int axis) {
        double length = ARROW_LENGTH / camera.scale();
        Vector3 tip = new Vector3(at.x() + (axis == 0 ? length : 0.0), at.y() + (axis == 1 ? length : 0.0), at.z() + (axis == 2 ? length : 0.0));
        return new double[]{screenX(tip), screenY(tip)};
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        graphics.fill(0, 0, width, height, PANEL);
        graphics.fill(viewLeft(), viewTop(), viewRight(), viewBottom(), VIEW);
        graphics.enableScissor(viewLeft(), viewTop(), viewRight(), viewBottom());
        grid(graphics);
        graphics.disableScissor();
        graphics.nextStratum();
        if (renderState != null) {
            graphics.entity(renderState, camera.scale(), camera.translation(), camera.rotation(), null,
                    viewLeft(), viewTop(), viewRight(), viewBottom());
        }
    }

    /** The ground the vehicle stands on, Y = 0, with its middle line along the nose. */
    private void grid(GuiGraphicsExtractor graphics) {
        for (int i = -GRID_HALF_SIZE; i <= GRID_HALF_SIZE; i += GRID_STEP) {
            int color = i == 0 ? DIM : GRID;
            line(graphics, new Vector3(i, 0, -GRID_HALF_SIZE), new Vector3(i, 0, GRID_HALF_SIZE), color);
            line(graphics, new Vector3(-GRID_HALF_SIZE, 0, i), new Vector3(GRID_HALF_SIZE, 0, i), color);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        graphics.enableScissor(viewLeft(), viewTop(), viewRight(), viewBottom());
        gizmos(graphics);
        graphics.disableScissor();
        super.extractRenderState(graphics, mouseX, mouseY, a);
        for (Label label : labels) {
            graphics.text(font, label.text(), label.x(), label.y(), label.color());
        }
        for (WrappedLine line : wrapped) {
            graphics.text(font, line.text(), line.x(), line.y(), DIM);
        }
        if (previewTexture != null) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, previewTexture, previewX, previewY, 0.0F, 0.0F, previewWidth, previewHeight,
                    previewTextureWidth, previewTextureHeight, previewTextureWidth, previewTextureHeight);
        }
        plot(graphics);
        statusLine(graphics);
    }

    /** What the player should know now, over two lines, and which vehicle this is in the view's corner. */
    private void statusLine(GuiGraphicsExtractor graphics) {
        int y = viewBottom() + 3;
        Component first;
        int color;
        int sending = WorkshopClient.submitProgress();
        if (sending >= 0) {
            first = Component.translatable("gui.mcheli.workshop.submit_sending", sending);
            color = DIM;
        } else if (message != null) {
            first = message;
            color = messageColor;
        } else if (!project.problems().isEmpty()) {
            first = Component.literal(project.problems().getFirst());
            color = WARNING;
        } else {
            first = Component.translatable("gui.mcheli.workshop.hint");
            color = DIM;
        }
        List<FormattedCharSequence> lines = font.split(first, width - 8);
        for (int i = 0; i < Math.min(lines.size(), 2); i++) {
            graphics.text(font, lines.get(i), 4, y + i * LABEL_ROW, color);
        }
        String file = (project.isDirty() || manifestChanged ? "* " : "") + project.manifest().id() + " / " + project.vehicle();
        graphics.text(font, font.plainSubstrByWidth(file, viewRight() - viewLeft() - 8), viewLeft() + 4, viewTop() + 4, DIM);
    }

    private void gizmos(GuiGraphicsExtractor graphics) {
        for (WorkshopPart part : project.parts()) {
            boolean active = part.line() == selected;
            if (part.type() == PartType.HIT_BOX) {
                hitBox(graphics, part, active);
            }
            for (PartField field : part.type().fields()) {
                if (field.kind() != PartField.Kind.POSITION) {
                    continue;
                }
                Vector3 v = part.vector(field);
                int color = active && field == activeHandle ? SELECTED : handleColor(part.type());
                square(graphics, screenX(v), screenY(v), active ? HANDLE_RADIUS + 1 : HANDLE_RADIUS, color);
            }
            if (active) {
                axes(graphics, part);
            }
        }
        Vector3 at = activePosition();
        if (at != null) {
            int[] colors = {X_COLOR, Y_COLOR, Z_COLOR};
            for (int i = 0; i < 3; i++) {
                double[] tip = arrowTip(at, i);
                segment(graphics, screenX(at), screenY(at), tip[0], tip[1], colors[i]);
                square(graphics, tip[0], tip[1], 2, colors[i]);
            }
        }
        // The nose, for a model that gives no hint which way it faces.
        Vector3 nose = new Vector3(0, 0, GRID_HALF_SIZE);
        graphics.text(font, "+Z", (int) screenX(nose), (int) screenY(nose), Z_COLOR);
    }

    /** A rotor or hinge is easier to place with its axis in view. */
    private void axes(GuiGraphicsExtractor graphics, WorkshopPart part) {
        PartField main = part.type().mainPosition();
        if (main == null) {
            return;
        }
        Vector3 from = part.vector(main);
        for (PartField field : part.type().fields()) {
            if (field.kind() == PartField.Kind.VECTOR && field.id().equals("axis")) {
                Vector3 axis = part.vector(field);
                double length = Math.sqrt(axis.x() * axis.x() + axis.y() * axis.y() + axis.z() * axis.z());
                if (length > 0.0) {
                    double reach = 2.0 * ARROW_LENGTH / camera.scale() / length;
                    line(graphics, new Vector3(from.x() - axis.x() * reach, from.y() - axis.y() * reach, from.z() - axis.z() * reach),
                            new Vector3(from.x() + axis.x() * reach, from.y() + axis.y() * reach, from.z() + axis.z() * reach), AXIS);
                }
            }
        }
    }

    private void hitBox(GuiGraphicsExtractor graphics, WorkshopPart part, boolean active) {
        PartType type = part.type();
        Vector3 c = part.vector(type.mainPosition());
        float w = number(part, type.fields().get(1)) / 2.0F;
        float h = number(part, type.fields().get(2)) / 2.0F;
        int color = active ? SELECTED : HIT_BOX;
        double[] xs = {c.x() - w, c.x() + w};
        double[] ys = {c.y() - h, c.y() + h};
        double[] zs = {c.z() - w, c.z() + w};
        for (double y : ys) {
            for (double z : zs) {
                line(graphics, new Vector3(xs[0], y, z), new Vector3(xs[1], y, z), color);
            }
            for (double x : xs) {
                line(graphics, new Vector3(x, y, zs[0]), new Vector3(x, y, zs[1]), color);
            }
        }
        for (double x : xs) {
            for (double z : zs) {
                line(graphics, new Vector3(x, ys[0], z), new Vector3(x, ys[1], z), color);
            }
        }
    }

    private static float number(WorkshopPart part, PartField field) {
        try {
            return Float.parseFloat(part.get(field, 0).trim());
        } catch (NumberFormatException e) {
            return 0.0F;
        }
    }

    private int handleColor(PartField field) {
        return activeHandle == field ? SELECTED : DIM;
    }

    private static int handleColor(PartType type) {
        return switch (type.group()) {
            case SEATS -> 0xFF40D0FF;
            case CAMERAS -> 0xFFFFE040;
            case ROTORS -> 0xFF80FF80;
            case WINGS -> 0xFFC0FF40;
            case WHEELS, TRACKS -> 0xFFFFA040;
            case LANDING_GEAR -> 0xFFFF8080;
            case CANOPIES -> 0xFFC080FF;
            case WEAPONS -> 0xFFFF4040;
            case HIT_BOXES -> HIT_BOX;
            case ENGINE -> 0xFFFF80C0;
        };
    }

    private void line(GuiGraphicsExtractor graphics, Vector3 from, Vector3 to, int color) {
        segment(graphics, screenX(from), screenY(from), screenX(to), screenY(to), color);
    }

    /** The GUI only fills whole numbers, but keeps the pose, so a line is a turned unit square. */
    private static void segment(GuiGraphicsExtractor graphics, double x0, double y0, double x1, double y1, int color) {
        double dx = x1 - x0;
        double dy = y1 - y0;
        double length = Math.sqrt(dx * dx + dy * dy);
        if (length < 1.0E-3) {
            return;
        }
        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        pose.translate((float) x0, (float) y0);
        pose.rotate((float) Math.atan2(dy, dx));
        pose.translate(0.0F, -0.5F);
        pose.scale((float) length, 1.0F);
        graphics.fill(0, 0, 1, 1, color);
        pose.popMatrix();
    }

    private static void square(GuiGraphicsExtractor graphics, double x, double y, int radius, int color) {
        int cx = (int) Math.round(x);
        int cy = (int) Math.round(y);
        graphics.fill(cx - radius, cy - radius, cx + radius + 1, cy + radius + 1, 0xFF000000);
        graphics.fill(cx - radius + 1, cy - radius + 1, cx + radius, cy + radius, color);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
