package com.jnhprojects.mcheli.hud;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Checks the HUD expression subset against the original library's {@code evalDouble} results. */
class ExpressionTest {
    private static final Map<String, Double> VARS = Map.of("hp_rto", 0.5, "reloading", 1.0, "altitude", 8.0, "plyr_yaw", 30.0);

    private static double eval(String text) {
        return Expression.parse(text).eval(name -> VARS.getOrDefault(name, 0.0));
    }

    @Test
    void arithmeticFollowsJavaPrecedence() {
        assertEquals(7.0, eval("1 + 2 * 3"));
        assertEquals(9.0, eval("(1 + 2) * 3"));
        assertEquals(-2.0, eval("-plyr_yaw / 15"));
        assertEquals(1.0, eval("10 % 3"));
        assertEquals(144.0 + 32.0, eval("144+32"));
    }

    @Test
    void divisionIsNeverIntegral() {
        assertEquals(0.5, eval("1/2"));
    }

    @Test
    void comparisonsAndTernaryGiveTheOriginalColours() {
        assertEquals(0xFF28D448L, (long) eval("hp_rto > 0.2 ? 0xFF28D448 : 0xFFDF0408"));
        assertEquals(0xFFDF0408L, (long) eval("altitude <= 10 ? 0xFFDF0408 : 0xFF28D448"));
        assertEquals(1.0, eval("reloading == 1 && hp_rto < 1"));
        assertEquals(0.0, eval("!reloading"));
    }

    @Test
    void logicalOperatorsReturnAnOperand() {
        assertEquals(5.0, eval("2 && 5"));
        assertEquals(2.0, eval("2 || 5"));
        assertEquals(0.0, eval("0 && unknown_variable"));
    }

    @Test
    void colourComponentsCombineIntoArgb() {
        assertEquals(0x80FF2040L, (long) eval("((0x80)<<24)|((255)<<16)|((32)<<8)|(64)"));
        assertEquals(0xFF0000L, (long) eval("#FF0000"));
        assertEquals(8.0, eval("32 >> 2"));
        assertEquals(-1.0, eval("~0"));
    }

    @Test
    void nestedTernaryIsRightAssociative() {
        assertEquals(2.0, eval("0 ? 1 : 1 ? 2 : 3"));
    }

    @Test
    void powerBindsTighterThanSign() {
        assertEquals(-4.0, eval("-2**2"));
    }

    @Test
    void namesAreCaseInsensitive() {
        Expression e = Expression.parse("HP_RTO * 100 + Altitude");
        assertEquals(Set.of("hp_rto", "altitude"), e.variables());
        assertEquals(58.0, e.eval(name -> VARS.getOrDefault(name, 0.0)));
    }

    @Test
    void malformedExpressionsAreRejected() {
        assertThrows(HudFormatException.class, () -> Expression.parse("1 +"));
        assertThrows(HudFormatException.class, () -> Expression.parse("(1 + 2"));
        assertThrows(HudFormatException.class, () -> Expression.parse("1 2"));
        assertThrows(HudFormatException.class, () -> Expression.parse("a $ b"));
    }
}
