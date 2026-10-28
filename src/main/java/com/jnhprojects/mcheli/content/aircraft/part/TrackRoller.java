package com.jnhprojects.mcheli.content.aircraft.part;

import com.jnhprojects.mcheli.content.Vector3;

/**
 * {@code AddTrackRoller}: a road wheel under a crawler track. It turns with the track on its side.
 *
 * @param side 1 for positive X (left), 0 for negative X (right)
 */
public record TrackRoller(String part, Vector3 position, int side) {
}
