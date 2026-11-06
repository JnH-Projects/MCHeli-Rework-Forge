package com.jnhprojects.mcheli.client.workshop;

import com.jnhprojects.mcheli.MCHeli;
import com.jnhprojects.mcheli.content.aircraft.AircraftDefinition;
import com.jnhprojects.mcheli.content.pack.ContentSource;
import com.jnhprojects.mcheli.workshop.ModelImport;
import com.jnhprojects.mcheli.workshop.WorkshopException;
import com.jnhprojects.mcheli.workshop.WorkshopKind;
import com.jnhprojects.mcheli.workshop.WorkshopProject;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/**
 * Where the workshop starts: the vehicles in the content packs on the left, ready to open, and on
 * the right a form to start a new one from an OBJ file, which can be dropped on the window.
 */
public class WorkshopHomeScreen extends Screen {
    private static final int TEXT = 0xFFFFFFFF;
    private static final int DIM = 0xFFA0A0A0;
    private static final int ERROR = 0xFFFF7070;
    private static final int ROW = 22;
    private static final int TOP = 36;
    private static final int FORM_WIDTH = 190;
    private static final int MAX_LIST_WIDTH = 300;
    private static final int LINE_HEIGHT = 10;

    private final List<WorkshopProject.Entry> entries;
    private int scroll;
    private WorkshopKind kind = WorkshopKind.HELICOPTER;
    private String vehicleText = "";
    private String packText = "";
    private String modelText = "";
    private @Nullable Component error;
    private @Nullable EditBox vehicleBox;
    private @Nullable EditBox packBox;
    private @Nullable EditBox modelBox;

    public WorkshopHomeScreen() {
        super(Component.translatable("gui.mcheli.workshop.title"));
        entries = WorkshopProject.list(MCHeli.packs().folder());
    }

    @Override
    protected void init() {
        listRows();
        form();
    }

    private int listWidth() {
        return Math.clamp(width - FORM_WIDTH - 30, 100, MAX_LIST_WIDTH);
    }

    private int visibleRows() {
        return Math.max((height - TOP - 34) / ROW, 1);
    }

    private void listRows() {
        int rows = visibleRows();
        for (int i = scroll; i < entries.size() && i < scroll + rows; i++) {
            WorkshopProject.Entry entry = entries.get(i);
            String suffix = entry.zipped() ? " (zip)" : "";
            addRenderableWidget(Button.builder(Component.literal(entry.pack() + " / " + entry.vehicle() + suffix), b -> open(entry))
                    .bounds(10, TOP + (i - scroll) * ROW, listWidth(), 20).build());
        }
    }

    private void form() {
        int x = width - FORM_WIDTH - 10;
        int y = TOP;
        vehicleBox = box(x, y + 10, Component.translatable("gui.mcheli.workshop.new.vehicle"), vehicleText, 64);
        vehicleBox.setResponder(text -> {
            // The pack follows the vehicle's name until the player gives it its own.
            if (packBox != null && packBox.getValue().equals(vehicleText)) {
                packBox.setValue(text);
            }
            vehicleText = text;
        });
        packBox = box(x, y + 44, Component.translatable("gui.mcheli.workshop.new.pack"), packText, 64);
        packBox.setResponder(text -> packText = text);
        addRenderableWidget(Button.builder(kindName(), b -> {
            kind = WorkshopKind.values()[(kind.ordinal() + 1) % WorkshopKind.values().length];
            b.setMessage(kindName());
        }).bounds(x, y + 68, FORM_WIDTH, 20).build());
        modelBox = box(x, y + 102, Component.translatable("gui.mcheli.workshop.new.model"), modelText, 1024);
        modelBox.setResponder(text -> modelText = text);
        addRenderableWidget(Button.builder(Component.translatable("gui.mcheli.workshop.new.create"), b -> create())
                .bounds(x, y + 126, FORM_WIDTH, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.mcheli.workshop.home.packs"), b -> minecraft.setScreen(new WorkshopPacksScreen()))
                .bounds(width / 2 - 104, height - 26, 100, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(width / 2 + 4, height - 26, 100, 20).build());
    }

    private EditBox box(int x, int y, Component name, String value, int maxLength) {
        EditBox box = new EditBox(font, x, y, FORM_WIDTH, 18, name);
        box.setMaxLength(maxLength);
        box.setValue(value);
        return addRenderableWidget(box);
    }

    private Component kindName() {
        return Component.translatable("gui.mcheli.workshop.new.kind", Component.translatable("gui.mcheli.workshop.kind." + kind.id()));
    }

    private void open(WorkshopProject.Entry entry) {
        try {
            minecraft.setScreen(new WorkshopScreen(WorkshopProject.open(MCHeli.packs().folder(), entry)));
        } catch (WorkshopException e) {
            error = Component.literal(e.getMessage());
        }
    }

    private void create() {
        String vehicle = vehicleText.trim();
        AircraftDefinition existing = MCHeli.content().any(vehicle);
        if (existing != null && existing.pack().equals(ContentSource.ORIGINAL)) {
            // The import's models come from a resource pack, which would win over this vehicle's.
            error = Component.translatable("gui.mcheli.workshop.new.name_taken", vehicle);
            return;
        }
        try {
            Path obj = Path.of(modelText.trim().replace("\"", ""));
            String author = minecraft.player != null ? minecraft.player.getGameProfile().name() : "";
            WorkshopProject project = WorkshopProject.create(MCHeli.packs().folder(), packText.trim(), kind, vehicle, author,
                    obj, ModelImport.findTexture(obj));
            minecraft.setScreen(new WorkshopScreen(project));
        } catch (InvalidPathException e) {
            error = Component.translatable("gui.mcheli.workshop.new.bad_path", modelText);
        } catch (WorkshopException e) {
            error = Component.literal(e.getMessage());
        }
    }

    /** A file dropped on the window fills in the form, its name standing in for the vehicle's. */
    @Override
    public void onFilesDrop(List<Path> files) {
        if (files.isEmpty() || modelBox == null || vehicleBox == null) {
            return;
        }
        Path file = files.getFirst();
        modelBox.setValue(file.toString());
        if (vehicleText.isEmpty()) {
            String name = file.getFileName().toString();
            int dot = name.lastIndexOf('.');
            vehicleBox.setValue((dot > 0 ? name.substring(0, dot) : name).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "-"));
        }
        error = null;
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        int max = Math.max(entries.size() - visibleRows(), 0);
        int next = Math.clamp(scroll - (int) Math.signum(scrollY), 0, max);
        if (next != scroll) {
            scroll = next;
            rebuildWidgets();
        }
        return true;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float a) {
        super.render(graphics, mouseX, mouseY, a);
        graphics.drawCenteredString(font, title, width / 2, 10, TEXT);
        graphics.drawString(font, Component.translatable("gui.mcheli.workshop.home.open"), 10, TOP - 12, DIM);
        if (entries.isEmpty()) {
            graphics.drawString(font, Component.translatable("gui.mcheli.workshop.home.none"), 10, TOP, DIM);
        }
        int x = width - FORM_WIDTH - 10;
        graphics.drawString(font, Component.translatable("gui.mcheli.workshop.home.new"), x, TOP - 12, DIM);
        graphics.drawString(font, Component.translatable("gui.mcheli.workshop.new.vehicle"), x, TOP, DIM);
        graphics.drawString(font, Component.translatable("gui.mcheli.workshop.new.pack"), x, TOP + 34, DIM);
        graphics.drawString(font, Component.translatable("gui.mcheli.workshop.new.model"), x, TOP + 92, DIM);
        int y = TOP + 150;
        for (var line : font.split(Component.translatable("gui.mcheli.workshop.home.drop"), FORM_WIDTH)) {
            graphics.drawString(font, line, x, y, DIM);
            y += LINE_HEIGHT;
        }
        if (error != null) {
            y += 2;
            for (var line : font.split(error, FORM_WIDTH)) {
                graphics.drawString(font, line, x, y, ERROR);
                y += LINE_HEIGHT;
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
