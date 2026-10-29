package com.jnhprojects.mcheli.hud;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * A parsed {@code assets/mcheli/hud/<name>.txt}, ported from {@code MCH_Hud} and
 * {@code MCH_HudManager}.
 *
 * <p>Each line is {@code Command = arg, arg, ...}; {@code EndIf} and {@code Exit} stand alone.
 * Anything else, comments included, is ignored the way the original ignored it. Lines with the
 * wrong number of arguments are skipped too. The differences: expressions are compiled here
 * instead of on every frame, so a broken one is reported when the file loads rather than crashing
 * the game mid-flight.
 */
public final class HudScript {
    private final String name;
    private final List<HudCommand> commands;

    public HudScript(String name, List<HudCommand> commands) {
        this.name = name;
        this.commands = List.copyOf(commands);
    }

    public String name() {
        return name;
    }

    public List<HudCommand> commands() {
        return commands;
    }

    /**
     * @param name the file name without {@code .txt}, lower case
     * @throws HudFormatException with the line number for an unpaired {@code If}/{@code EndIf}
     *                            or an expression that does not parse
     */
    public static HudScript parse(String name, Reader in) throws IOException {
        List<HudCommand> commands = new ArrayList<>();
        BufferedReader reader = in instanceof BufferedReader b ? b : new BufferedReader(in);
        boolean inIf = false;
        int lineNumber = 0;
        String raw;
        while ((raw = reader.readLine()) != null) {
            lineNumber++;
            String line = raw.trim();
            if (line.equalsIgnoreCase("endif")) {
                line = "endif=0";
            } else if (line.equalsIgnoreCase("exit")) {
                line = "exit=0";
            }
            int eq = line.indexOf('=');
            if (eq < 0 || eq == line.length() - 1) {
                continue;
            }
            String command = line.substring(0, eq).trim().toLowerCase(Locale.ROOT);
            String[] args = line.substring(eq + 1).trim().split("\\s*,\\s*");
            try {
                if (command.equals("if")) {
                    if (inIf) {
                        throw new HudFormatException("If inside an If; EndIf missing");
                    }
                    inIf = true;
                } else if (command.equals("endif")) {
                    if (!inIf) {
                        throw new HudFormatException("EndIf without an If");
                    }
                    inIf = false;
                }
                HudCommand parsed = command(command, args, lineNumber);
                if (parsed != null) {
                    commands.add(parsed);
                }
            } catch (HudFormatException e) {
                throw new HudFormatException("%s.txt line %d: %s".formatted(name, lineNumber, e.getMessage()), e);
            }
        }
        if (inIf) {
            throw new HudFormatException(name + ".txt: EndIf missing at the end of the file");
        }
        return new HudScript(name, commands);
    }

    private static HudCommand command(String command, String[] a, int line) {
        int n = a.length;
        return switch (command) {
            case "if" -> new HudCommand.If(line, expr(a[0]));
            case "endif" -> new HudCommand.EndIf(line);
            case "exit" -> new HudCommand.Exit(line);
            case "color" -> n == 1 ? new HudCommand.Color(line, expr(a[0]))
                    : n == 4 ? new HudCommand.Color(line, expr("((%s)<<24)|((%s)<<16)|((%s)<<8)|(%s)".formatted(a[0], a[1], a[2], a[3])))
                    : null;
            case "call" -> n == 1 ? new HudCommand.Call(line, a[0].toLowerCase(Locale.ROOT)) : null;
            case "drawstring", "drawcenteredstring" -> text(a, line, command.equals("drawcenteredstring"));
            case "drawtexture" -> n == 9 || n == 10
                    ? new HudCommand.Texture(line, a[0].toLowerCase(Locale.ROOT), expr(a[1]), expr(a[2]), expr(a[3]), expr(a[4]),
                    expr(a[5]), expr(a[6]), expr(a[7]), expr(a[8]), expr(n == 10 ? a[9] : "0"))
                    : null;
            case "drawrect" -> n == 4 ? new HudCommand.Rect(line, expr(a[0]), expr(a[1]), expr(a[2]), expr(a[3])) : null;
            case "drawline" -> n >= 4 && n % 2 == 0 ? new HudCommand.Line(line, exprs(a, 0)) : null;
            case "drawlinestipple" -> n >= 6 && n % 2 == 0
                    ? new HudCommand.LineStipple(line, expr(a[0]), expr(a[1]), exprs(a, 2))
                    : null;
            case "drawentityradar", "drawenemyradar" -> n == 5
                    ? new HudCommand.Radar(line, command.equals("drawenemyradar"), expr(a[0]), expr(a[1]), expr(a[2]), expr(a[3]), expr(a[4]))
                    : null;
            case "drawgraduationyaw" -> graduation(HudCommand.Graduation.Kind.YAW, a, line);
            case "drawgraduationpitch1" -> graduation(HudCommand.Graduation.Kind.PITCH_1, a, line);
            case "drawgraduationpitch2" -> graduation(HudCommand.Graduation.Kind.PITCH_2, a, line);
            case "drawgraduationpitch3" -> graduation(HudCommand.Graduation.Kind.PITCH_3, a, line);
            case "drawcamerarot" -> n == 2 ? new HudCommand.CameraRot(line, expr(a[0]), expr(a[1])) : null;
            default -> null;
        };
    }

    /** The format must be the third argument, in double quotes; arguments after it are {@link TextArg} names. */
    private static HudCommand text(String[] a, int line, boolean centered) {
        if (a.length < 3) {
            return null;
        }
        String format = a[2];
        if (format.length() < 2 || format.charAt(0) != '"' || format.charAt(format.length() - 1) != '"') {
            return null;
        }
        List<TextArg> args = Arrays.stream(a, 3, a.length).map(TextArg::of).toList();
        return new HudCommand.Text(line, expr(a[0]), expr(a[1]), format.substring(1, format.length() - 1), args, centered);
    }

    private static HudCommand graduation(HudCommand.Graduation.Kind kind, String[] a, int line) {
        return a.length == 4 ? new HudCommand.Graduation(line, kind, expr(a[0]), expr(a[1]), expr(a[2]), expr(a[3])) : null;
    }

    private static Expression expr(String text) {
        return Expression.parse(text);
    }

    private static List<Expression> exprs(String[] a, int from) {
        List<Expression> out = new ArrayList<>(a.length - from);
        for (int i = from; i < a.length; i++) {
            out.add(expr(a[i]));
        }
        return out;
    }
}
