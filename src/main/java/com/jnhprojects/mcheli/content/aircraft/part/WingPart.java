package com.jnhprojects.mcheli.content.aircraft.part;

import com.jnhprojects.mcheli.content.Vector3;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code AddPartWing}, with the {@code AddPartPylon} lines that follow it. Wings fold on the
 * ground or sweep back in flight; pylons counter-rotate so the stores stay aligned.
 */
public record WingPart(String part, Vector3 position, Vector3 axis, float factor, List<MovingPart> pylons) {

    public WingPart {
        pylons = new ArrayList<>(pylons);
    }
}
