package com.jnhprojects.mcheli.content.aircraft;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PitchCurveTest {
    private static final float NEAR = 0.0001F;

    @Test
    void followsStraightLinesBetweenItsPoints() {
        PitchCurve curve = PitchCurve.parse("0:0.6, 0.5:1, 1:1.4");
        assertEquals(0.6F, curve.at(0.0F), NEAR);
        assertEquals(0.8F, curve.at(0.25F), NEAR);
        assertEquals(1.0F, curve.at(0.5F), NEAR);
        assertEquals(1.2F, curve.at(0.75F), NEAR);
        assertEquals(1.4F, curve.at(1.0F), NEAR);
    }

    @Test
    void staysLevelBeyondItsEnds() {
        PitchCurve curve = PitchCurve.parse("0.25:0.8, 0.75:1.2");
        assertEquals(0.8F, curve.at(0.0F), NEAR);
        assertEquals(1.2F, curve.at(1.0F), NEAR);
    }

    @Test
    void keepsPitchToWhatTheSoundEngineCanPlay() {
        PitchCurve curve = PitchCurve.parse("0:0.1, 1:9");
        assertEquals(PitchCurve.MIN_PITCH, curve.pitch(0), NEAR);
        assertEquals(PitchCurve.MAX_PITCH, curve.pitch(1), NEAR);
    }

    @Test
    void refusesWhatIsNotACurve() {
        assertThrows(IllegalArgumentException.class, () -> PitchCurve.parse("1"));
        assertThrows(IllegalArgumentException.class, () -> PitchCurve.parse("0:1"));
        assertThrows(IllegalArgumentException.class, () -> PitchCurve.parse("0:1, 1"));
        assertThrows(IllegalArgumentException.class, () -> PitchCurve.parse("0:1, nope:1"));
        assertThrows(IllegalArgumentException.class, () -> PitchCurve.parse("0.5:1, 0.5:1.2"));
        assertThrows(IllegalArgumentException.class, () -> PitchCurve.parse("0:1, 1.5:1.2"));
    }

    @Test
    void readsBackWhatItWrites() {
        PitchCurve curve = PitchCurve.parse("0:0.6, 0.5:1, 1:1.25").withPitch(1, 1.1F);
        assertEquals("0:0.6, 0.5:1.1, 1:1.25", curve.text());
        assertEquals(curve.text(), PitchCurve.parse(curve.text()).text());
    }
}
