package com.jnhprojects.mcheli.content.aircraft.part;

import com.jnhprojects.mcheli.content.Vector3;
import org.jspecify.annotations.Nullable;

/**
 * One {@code AddPartLG}, {@code AddPartLGRev}, {@code AddPartLGHatch} or {@code AddPartSlideRotLG}
 * entry. The gear state runs from 0 (down) to 90 (retracted).
 *
 * @param reverse     rotates by {@code 90 - state}, used for doors that close once the gear is up
 * @param hatch       opens fully at mid travel and closes again, used for gear bay doors
 * @param secondAxis  optional second rotation applied after the first, {@code null} if absent
 * @param slide       optional translation scaled by the gear state, {@code null} if absent
 */
public record LandingGearPart(
        String part,
        Vector3 position,
        Vector3 axis,
        float factor,
        boolean reverse,
        boolean hatch,
        @Nullable Vector3 secondAxis,
        float secondFactor,
        @Nullable Vector3 slide) {
}
