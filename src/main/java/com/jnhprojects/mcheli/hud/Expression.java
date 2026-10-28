package com.jnhprojects.mcheli.hud;

import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/**
 * A compiled HUD expression such as {@code hp_rto > 0.2 ? 0xFF28D448 : 0xFFDF0408}.
 *
 * <p>The original evaluated these with a general purpose expression library ({@code mcheli.eval},
 * a copy of hishidama's eval), re-parsing every expression every frame. HUD scripts use only its
 * arithmetic, so this is a small parser for that subset that compiles once. Evaluation follows the
 * library's {@code evalDouble} path, which matters for a few operators:
 * <ul>
 *   <li>everything is a double, so {@code 1 / 2} is {@code 0.5};</li>
 *   <li>{@code <<} and {@code >>} multiply and divide by powers of two, {@code >>>} does the same
 *       on the absolute value;</li>
 *   <li>{@code &}, {@code |}, {@code ^} and {@code ~} work on the value truncated to a long;</li>
 *   <li>{@code &&} and {@code ||} short circuit and return an operand, not 1;</li>
 *   <li>comparisons and {@code !} give 1 or 0, and {@code **} is a power that binds tighter than
 *       a sign.</li>
 * </ul>
 * Numbers may be decimal, {@code 0x} hex, {@code 0b} binary or {@code #} hex (the original
 * rewrote {@code #} to {@code 0x}). Variable names are case insensitive.
 */
public final class Expression {
    /** Supplies variable values by lower case name. */
    @FunctionalInterface
    public interface Variables {
        double get(String name);
    }

    private interface Node {
        double eval(Variables vars);
    }

    private final String source;
    private final Node root;
    private final Set<String> variables;

    private Expression(String source, Node root, Set<String> variables) {
        this.source = source;
        this.root = root;
        this.variables = variables;
    }

    /** @throws HudFormatException if the text is not a valid expression */
    public static Expression parse(String text) {
        Parser parser = new Parser(text);
        Node root = parser.ternary();
        if (parser.peek() != null) {
            throw parser.error("unexpected '" + parser.peek() + "'");
        }
        return new Expression(text, root, Set.copyOf(parser.names));
    }

    public double eval(Variables vars) {
        return root.eval(vars);
    }

    /** Names of the variables this expression reads, lower case. */
    public Set<String> variables() {
        return variables;
    }

    @Override
    public String toString() {
        return source;
    }

    private static boolean truth(double v) {
        return v != 0.0;
    }

    private static double bool(boolean b) {
        return b ? 1.0 : 0.0;
    }

    /** Precedence climbing over the token stream, lowest precedence first. */
    private static final class Parser {
        private static final String[] OPERATORS = {
                ">>>", "**", "<<", ">>", "<=", ">=", "==", "!=", "&&", "||",
                "+", "-", "*", "/", "%", "<", ">", "&", "|", "^", "!", "~", "?", ":", "(", ")"};

        private final String text;
        private final Set<String> names = new TreeSet<>();
        private int pos;
        private String token;
        private int tokenStart;

        Parser(String text) {
            this.text = text;
            advance();
        }

        String peek() {
            return token;
        }

        HudFormatException error(String message) {
            return new HudFormatException("%s at column %d of '%s'".formatted(message, tokenStart + 1, text));
        }

        private boolean accept(String op) {
            if (op.equals(token)) {
                advance();
                return true;
            }
            return false;
        }

        private void expect(String op) {
            if (!accept(op)) {
                throw error(token == null ? "expected '" + op + "' at the end" : "expected '" + op + "'");
            }
        }

        private void advance() {
            while (pos < text.length() && Character.isWhitespace(text.charAt(pos))) {
                pos++;
            }
            tokenStart = pos;
            if (pos >= text.length()) {
                token = null;
                return;
            }
            char c = text.charAt(pos);
            if (Character.isLetterOrDigit(c) || c == '_' || c == '.' || c == '#') {
                pos++;
                while (pos < text.length() && (Character.isLetterOrDigit(text.charAt(pos)) || text.charAt(pos) == '_' || text.charAt(pos) == '.')) {
                    pos++;
                }
                token = text.substring(tokenStart, pos);
                return;
            }
            for (String op : OPERATORS) {
                if (text.startsWith(op, pos)) {
                    pos += op.length();
                    token = op;
                    return;
                }
            }
            throw error("unexpected character '" + c + "'");
        }

        Node ternary() {
            Node condition = binary(0);
            if (accept("?")) {
                Node a = ternary();
                expect(":");
                Node b = ternary();
                return vars -> truth(condition.eval(vars)) ? a.eval(vars) : b.eval(vars);
            }
            return condition;
        }

        /** Binary operator levels from {@code ||} down to {@code * / %}. */
        private static final String[][] LEVELS = {
                {"||"}, {"&&"}, {"|"}, {"^"}, {"&"}, {"==", "!="}, {"<", "<=", ">", ">="},
                {"<<", ">>", ">>>"}, {"+", "-"}, {"*", "/", "%"}};

        private Node binary(int level) {
            if (level == LEVELS.length) {
                return unary();
            }
            Node left = binary(level + 1);
            while (true) {
                String op = operatorAt(level);
                if (op == null) {
                    return left;
                }
                advance();
                left = combine(op, left, binary(level + 1));
            }
        }

        private String operatorAt(int level) {
            for (String op : LEVELS[level]) {
                if (op.equals(token)) {
                    return op;
                }
            }
            return null;
        }

        private static Node combine(String op, Node l, Node r) {
            return switch (op) {
                case "||" -> vars -> {
                    double v = l.eval(vars);
                    return truth(v) ? v : r.eval(vars);
                };
                case "&&" -> vars -> {
                    double v = l.eval(vars);
                    return truth(v) ? r.eval(vars) : v;
                };
                case "|" -> vars -> (long) l.eval(vars) | (long) r.eval(vars);
                case "^" -> vars -> (long) l.eval(vars) ^ (long) r.eval(vars);
                case "&" -> vars -> (long) l.eval(vars) & (long) r.eval(vars);
                case "==" -> vars -> bool(l.eval(vars) == r.eval(vars));
                case "!=" -> vars -> bool(l.eval(vars) != r.eval(vars));
                case "<" -> vars -> bool(l.eval(vars) < r.eval(vars));
                case "<=" -> vars -> bool(l.eval(vars) <= r.eval(vars));
                case ">" -> vars -> bool(l.eval(vars) > r.eval(vars));
                case ">=" -> vars -> bool(l.eval(vars) >= r.eval(vars));
                case "<<" -> vars -> l.eval(vars) * Math.pow(2.0, r.eval(vars));
                case ">>" -> vars -> l.eval(vars) / Math.pow(2.0, r.eval(vars));
                case ">>>" -> vars -> Math.abs(l.eval(vars)) / Math.pow(2.0, r.eval(vars));
                case "+" -> vars -> l.eval(vars) + r.eval(vars);
                case "-" -> vars -> l.eval(vars) - r.eval(vars);
                case "*" -> vars -> l.eval(vars) * r.eval(vars);
                case "/" -> vars -> l.eval(vars) / r.eval(vars);
                case "%" -> vars -> l.eval(vars) % r.eval(vars);
                default -> throw new IllegalStateException(op);
            };
        }

        private Node unary() {
            if (accept("-")) {
                Node n = unary();
                return vars -> -n.eval(vars);
            }
            if (accept("+")) {
                return unary();
            }
            if (accept("!")) {
                Node n = unary();
                return vars -> bool(!truth(n.eval(vars)));
            }
            if (accept("~")) {
                Node n = unary();
                return vars -> ~(long) n.eval(vars);
            }
            return power();
        }

        private Node power() {
            Node base = primary();
            while (accept("**")) {
                Node b = base;
                Node exponent = primary();
                base = vars -> Math.pow(b.eval(vars), exponent.eval(vars));
            }
            return base;
        }

        private Node primary() {
            String t = token;
            if (t == null) {
                throw error("expression ends early");
            }
            if (accept("(")) {
                Node inner = ternary();
                expect(")");
                return inner;
            }
            char c = t.charAt(0);
            if (Character.isDigit(c) || c == '.' || c == '#') {
                double value = number(t);
                advance();
                return vars -> value;
            }
            if (Character.isLetter(c) || c == '_') {
                String name = t.toLowerCase(Locale.ROOT);
                names.add(name);
                advance();
                return vars -> vars.get(name);
            }
            throw error("unexpected '" + t + "'");
        }

        private double number(String t) {
            try {
                String s = t.toLowerCase(Locale.ROOT);
                if (s.startsWith("#")) {
                    return Long.parseUnsignedLong(s.substring(1), 16);
                }
                if (s.endsWith("l")) {
                    s = s.substring(0, s.length() - 1);
                }
                if (s.startsWith("0x")) {
                    return Long.parseUnsignedLong(s.substring(2), 16);
                }
                if (s.startsWith("0b")) {
                    return Long.parseUnsignedLong(s.substring(2), 2);
                }
                return Double.parseDouble(s);
            } catch (NumberFormatException e) {
                throw error("bad number '" + t + "'");
            }
        }
    }
}
