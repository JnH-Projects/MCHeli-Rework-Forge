package com.jnhprojects.mcheli.content.aircraft.part;

import java.util.List;

/**
 * {@code AddPartWeaponBay} and {@code AddPartSlideWeaponBay}: doors that open while one of
 * {@code weapons} is selected by a crew member.
 */
public record WeaponBayPart(MovingPart door, List<String> weapons) {
}
