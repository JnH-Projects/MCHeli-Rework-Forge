package com.jnhprojects.mcheli.hud;

import com.jnhprojects.mcheli.TestResources;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Loads every bundled HUD script and checks what it refers to exists. */
class HudScriptTest {

    private static Map<String, HudScript> bundled() throws IOException {
        Path folder = TestResources.assets().resolve("hud");
        Map<String, HudScript> scripts = new TreeMap<>();
        try (Stream<Path> files = Files.list(folder)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".txt")).toList()) {
                String name = file.getFileName().toString().replace(".txt", "");
                try (Reader in = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                    scripts.put(name, HudScript.parse(name, in));
                }
            }
        }
        return scripts;
    }

    private static List<Expression> expressions(HudCommand command) {
        List<Expression> out = new ArrayList<>();
        switch (command) {
            case HudCommand.If c -> out.add(c.condition());
            case HudCommand.Color c -> out.add(c.argb());
            case HudCommand.Text c -> out.addAll(List.of(c.x(), c.y()));
            case HudCommand.Texture c -> out.addAll(List.of(c.left(), c.top(), c.width(), c.height(), c.u(), c.v(),
                    c.uWidth(), c.vHeight(), c.rotation()));
            case HudCommand.Rect c -> out.addAll(List.of(c.left(), c.top(), c.width(), c.height()));
            case HudCommand.Line c -> out.addAll(c.points());
            case HudCommand.LineStipple c -> {
                out.addAll(List.of(c.pattern(), c.factor()));
                out.addAll(c.points());
            }
            case HudCommand.Radar c -> out.addAll(List.of(c.rotation(), c.left(), c.top(), c.width(), c.height()));
            case HudCommand.Graduation c -> out.addAll(List.of(c.angle(), c.roll(), c.x(), c.y()));
            case HudCommand.CameraRot c -> out.addAll(List.of(c.x(), c.y()));
            case HudCommand.EndIf c -> {
            }
            case HudCommand.Exit c -> {
            }
            case HudCommand.Call c -> {
            }
        }
        return out;
    }

    @Test
    void everyBundledScriptParses() throws IOException {
        Map<String, HudScript> scripts = bundled();
        assertEquals(19, scripts.size(), scripts.keySet().toString());
        assertTrue(scripts.get("heli").commands().size() > 10);
    }

    @Test
    void scriptsOnlyReferToThingsThatExist() throws IOException {
        Map<String, HudScript> scripts = bundled();
        Path textures = TestResources.assets().resolve("textures/gui");
        List<String> problems = new ArrayList<>();
        for (HudScript script : scripts.values()) {
            for (HudCommand command : script.commands()) {
                String where = script.name() + ".txt:" + command.line();
                if (command instanceof HudCommand.Call call && !scripts.containsKey(call.hud())) {
                    problems.add(where + " calls missing " + call.hud());
                }
                if (command instanceof HudCommand.Texture texture && !Files.exists(textures.resolve(texture.name() + ".png"))) {
                    problems.add(where + " draws missing texture " + texture.name());
                }
                if (command instanceof HudCommand.Text text && text.args().contains(TextArg.NONE)) {
                    problems.add(where + " formats an unknown value");
                }
                for (Expression e : expressions(command)) {
                    for (String name : e.variables()) {
                        if (HudVariable.byKey(name) == null) {
                            problems.add(where + " reads unknown variable " + name);
                        }
                    }
                }
            }
        }
        assertEquals(List.of(), problems);
    }

    @Test
    void fourComponentColourPacksArgb() throws IOException {
        HudScript script = HudScript.parse("test", new StringReader("Color = 0x80, 255, 32, 64\n"));
        HudCommand.Color color = assertInstanceOf(HudCommand.Color.class, script.commands().getFirst());
        assertEquals(0x80FF2040L, (long) color.argb().eval(name -> 0.0));
    }

    @Test
    void textKeepsCommasOutOfTheFormat() throws IOException {
        HudScript script = HudScript.parse("test", new StringReader("DrawCenteredString = 0, -80, \"HP %d\", HP\n"));
        HudCommand.Text text = assertInstanceOf(HudCommand.Text.class, script.commands().getFirst());
        assertEquals("HP %d", text.format());
        assertEquals(List.of(TextArg.HP), text.args());
        assertTrue(text.centered());
    }

    @Test
    void unpairedIfIsAnError() {
        assertThrows(HudFormatException.class, () -> HudScript.parse("test", new StringReader("If = 1\nDrawRect = 0,0,1,1\n")));
        assertThrows(HudFormatException.class, () -> HudScript.parse("test", new StringReader("EndIf\n")));
        assertThrows(HudFormatException.class, () -> HudScript.parse("test", new StringReader("If = 1\nIf = 2\nEndIf\n")));
    }

    @Test
    void commentsAndUnknownCommandsAreIgnored() throws IOException {
        HudScript script = HudScript.parse("test", new StringReader("; a = b\nFoo = 1\n\nExit\n"));
        assertEquals(1, script.commands().size());
        assertInstanceOf(HudCommand.Exit.class, script.commands().getFirst());
    }
}
