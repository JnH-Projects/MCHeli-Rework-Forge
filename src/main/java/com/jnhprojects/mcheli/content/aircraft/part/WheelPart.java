package com.jnhprojects.mcheli.content.aircraft.part;

import com.jnhprojects.mcheli.content.Vector3;

/**
 * {@code AddPartWheel} and {@code AddPartSteeringWheel}.
 *
 * <p>A road wheel rolls around X through {@code position}, and steers by {@code steer * steerAngle}
 * degrees around {@code axis} through {@code pivot}. A steering wheel only uses the steering
 * rotation, with {@code pivot} equal to {@code position}.
 *
 * @param steerAngle degrees at full lock; zero for wheels that do not steer
 */
public record WheelPart(String part, Vector3 position, Vector3 axis, float steerAngle, Vector3 pivot) {
}
