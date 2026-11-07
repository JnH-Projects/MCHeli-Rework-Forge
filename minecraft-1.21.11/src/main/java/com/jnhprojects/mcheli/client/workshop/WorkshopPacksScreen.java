package com.jnhprojects.mcheli.client.workshop;

import com.jnhprojects.mcheli.MCHeli;
import com.jnhprojects.mcheli.client.ClientPacks;
import com.jnhprojects.mcheli.content.pack.ContentPack;
import com.jnhprojects.mcheli.content.pack.PackManifest;
import com.jnhprojects.mcheli.content.pack.PackProblem;
import com.jnhprojects.mcheli.workshop.PackCatalog;
import com.jnhprojects.mcheli.workshop.WorkshopException;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The package browser: the packs in {@code mcheli/packs} and those the server in use sent, who made
 * them and what is in them. A zip someone gave the player can be dropped on it to install.
 *
 * <p>This only ever reads the packs and adds to the folder. Removing one is left to the file manager
 * the Open folder button opens, since a pack can be the only copy of someone's work.
 */
public class WorkshopPacksScreen extends Screen {
    private static final int TEXT = 0xFFFFFFFF;
    private static final int DIM = 0xFFA0A0A0;
    private static final int ERROR = 0xFFFF7070;
    private static final int GOOD = 0xFF80E080;
    private static final int ROW = 22;
    private static final int TOP = 36;
    private static final int LIST_WIDTH = 170;
    private static final int LINE_HEIGHT = 10;
    private static final int VEHICLES_SHOWN = 8;

    /** A pack in the list, and where it came from. */
    private record Row(PackCatalog.Info info, boolean fromServer) {
    }

    private final List<Row> rows = new ArrayList<>();
    private final List<PackProblem> problems = new ArrayList<>();
    private int scroll;
    private int selected;
    private @Nullable Component message;
    private int messageColor = DIM;

    public WorkshopPacksScreen() {
        super(Component.translatable("gui.mcheli.workshop.packs.title"));
        read();
    }

    private void read() {
        rows.clear();
        problems.clear();
        for (ContentPack pack : ClientPacks.INSTANCE.serverPacks()) {
            try {
                rows.add(new Row(PackCatalog.info(pack), true));
            } catch (IOException e) {
                problems.add(new PackProblem(pack.id(), "could not be read: " + e));
            }
        }
        PackCatalog.Listing own = PackCatalog.read(MCHeli.packs().folder());
        for (PackCatalog.Info info : own.packs()) {
            // A pack the player has that the server also uses is listed once, as the server's.
            if (rows.stream().noneMatch(row -> row.info().hash().equals(info.hash()))) {
                rows.add(new Row(info, false));
            }
        }
        problems.addAll(own.problems());
        selected = Math.clamp(selected, 0, Math.max(rows.size() - 1, 0));
    }

    private int visibleRows() {
        return Math.max((height - TOP - 34) / ROW, 1);
    }

    @Override
    protected void init() {
        scroll = Math.clamp(scroll, 0, Math.max(rows.size() - visibleRows(), 0));
        for (int i = scroll; i < rows.size() && i < scroll + visibleRows(); i++) {
            int index = i;
            Button row = addRenderableWidget(Button.builder(Component.literal(rows.get(i).info().manifest().name()), b -> {
                selected = index;
                rebuildWidgets();
            }).bounds(10, TOP + (i - scroll) * ROW, LIST_WIDTH, 20).build());
            row.active = i != selected;
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.mcheli.workshop.packs.open_folder"), b -> openFolder())
                .bounds(width / 2 - 104, height - 26, 100, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(width / 2 + 4, height - 26, 100, 20).build());
    }

    private void openFolder() {
        Path folder = MCHeli.packs().folder();
        try {
            Files.createDirectories(folder);
            Util.getPlatform().openPath(folder);
        } catch (IOException e) {
            say(Component.literal(e.toString()), ERROR);
        }
    }

    private void say(Component text, int color) {
        message = text;
        messageColor = color;
    }

    @Override
    public void onFilesDrop(List<Path> files) {
        for (Path file : files) {
            if (!file.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".zip")) {
                say(Component.translatable("gui.mcheli.workshop.packs.not_zip", file.getFileName().toString()), ERROR);
                continue;
            }
            try {
                PackManifest installed = PackCatalog.install(file, MCHeli.packs().folder());
                say(Component.translatable("gui.mcheli.workshop.packs.installed", installed.name()), GOOD);
                read();
                rebuildWidgets();
            } catch (WorkshopException e) {
                say(Component.literal(e.getMessage()), ERROR);
            }
        }
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        int next = Math.clamp(scroll - (int) Math.signum(scrollY), 0, Math.max(rows.size() - visibleRows(), 0));
        if (next != scroll) {
            scroll = next;
            rebuildWidgets();
        }
        return true;
    }

    @Override
    public void onClose() {
        minecraft.setScreen(new WorkshopHomeScreen());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float a) {
        super.render(graphics, mouseX, mouseY, a);
        graphics.drawCenteredString(font, title, width / 2, 10, TEXT);
        if (rows.isEmpty()) {
            graphics.drawString(font, Component.translatable("gui.mcheli.workshop.packs.none"), 10, TOP, DIM);
        }
        int x = 10 + LIST_WIDTH + 10;
        int w = width - x - 10;
        int y = TOP;
        if (selected < rows.size()) {
            y = details(graphics, rows.get(selected), x, y, w) + LINE_HEIGHT;
        }
        for (PackProblem problem : problems) {
            y = wrapped(graphics, Component.translatable("gui.mcheli.workshop.packs.problem", problem.pack(), problem.message()), x, y, w, ERROR);
        }
        y = wrapped(graphics, Component.translatable("gui.mcheli.workshop.packs.drop"), x, y + 4, w, DIM);
        if (message != null) {
            wrapped(graphics, message, x, y + 4, w, messageColor);
        }
    }

    private int details(GuiGraphics graphics, Row row, int x, int y, int w) {
        PackCatalog.Info info = row.info();
        PackManifest m = info.manifest();
        y = wrapped(graphics, Component.literal(m.name() + " " + m.version()), x, y, w, TEXT);
        if (!m.author().isEmpty()) {
            y = wrapped(graphics, Component.translatable("gui.mcheli.workshop.packs.author", m.author()), x, y, w, TEXT);
        }
        if (!m.description().isEmpty()) {
            y = wrapped(graphics, Component.literal(m.description()), x, y + 2, w, DIM);
        }
        y += 4;
        y = wrapped(graphics, Component.translatable("gui.mcheli.workshop.packs.id", m.id()), x, y, w, DIM);
        // A small pack is still something, and 0.0 MB reads as an empty one.
        String megabytes = "%.1f".formatted(Math.max(info.size() / 1048576.0, 0.1));
        y = wrapped(graphics, Component.translatable("gui.mcheli.workshop.packs.size", megabytes, ContentPack.shortHash(info.hash())), x, y, w, DIM);
        Component source = row.fromServer() ? Component.translatable("gui.mcheli.workshop.packs.from_server")
                : Component.literal(info.source().getFileName().toString() + (info.zipped() ? "" : "/"));
        y = wrapped(graphics, Component.translatable("gui.mcheli.workshop.packs.source", source), x, y, w, DIM);
        y += 4;
        if (!info.vehicles().isEmpty()) {
            y = wrapped(graphics, Component.translatable("gui.mcheli.workshop.packs.vehicles", info.vehicles().size(), shortList(info.vehicles())), x, y, w, TEXT);
        }
        if (!info.weapons().isEmpty()) {
            y = wrapped(graphics, Component.translatable("gui.mcheli.workshop.packs.weapons", info.weapons().size(), shortList(info.weapons())), x, y, w, TEXT);
        }
        return y;
    }

    private static String shortList(List<String> names) {
        if (names.size() <= VEHICLES_SHOWN) {
            return String.join(", ", names);
        }
        return String.join(", ", names.subList(0, VEHICLES_SHOWN)) + ", ...";
    }

    private int wrapped(GuiGraphics graphics, Component text, int x, int y, int w, int color) {
        for (FormattedCharSequence line : font.split(text, w)) {
            graphics.drawString(font, line, x, y, color);
            y += LINE_HEIGHT;
        }
        return y;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
