package com.jnhprojects.mcheli.physics;

import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Orientation maths for aircraft.
 *
 * <p>Angles follow Minecraft conventions: yaw grows clockwise seen from above, positive pitch is
 * nose down and positive roll banks to the right. The renderer applies them as yaw about -Y, then
 * pitch about X, then roll about Z, so the orientation quaternion is
 * {@code rotY(-yaw) * rotX(pitch) * rotZ(roll)}.
 */
public final class AircraftRotation {
    private AircraftRotation() {
    }

    public static Quaternionf orientation(float yaw, float pitch, float roll) {
        return new Quaternionf()
                .rotateY((float) Math.toRadians(-yaw))
                .rotateX((float) Math.toRadians(pitch))
                .rotateZ((float) Math.toRadians(roll));
    }

    /**
     * Applies a rotation expressed in the aircraft's own frame, the way the original
     * {@code setAngles} matrix chain did. Pulling back on the stick while banked therefore turns
     * the aircraft instead of only raising the nose.
     *
     * @return {@code {yaw, pitch, roll}} in degrees, roll wrapped to [-180, 180)
     */
    public static float[] applyLocal(float yaw, float pitch, float roll, float dYaw, float dPitch, float dRoll) {
        Quaternionf q = orientation(yaw, pitch, roll)
                .rotateZ((float) Math.toRadians(dRoll))
                .rotateX((float) Math.toRadians(dPitch))
                .rotateY((float) Math.toRadians(-dYaw));
        Vector3f euler = q.getEulerAnglesYXZ(new Vector3f());
        return new float[]{
                (float) -Math.toDegrees(euler.y),
                (float) Math.toDegrees(euler.x),
                wrap((float) Math.toDegrees(euler.z))};
    }

    /** Rotates a point given in aircraft space into world space, relative to the aircraft origin. */
    public static Vector3f toWorld(float yaw, float pitch, float roll, double x, double y, double z) {
        return orientation(yaw, pitch, roll).transform(new Vector3f((float) x, (float) y, (float) z));
    }

    /**
     * The roll to give {@code Camera.setRotation} so that a view along {@code viewYaw, viewPitch}
     * keeps the hull's up axis at the top of the screen. Looking ahead that is the hull's own roll,
     * looking out of the side it is the hull's pitch, and anything between is a mix of the two.
     *
     * <p>The camera builds its rotation as {@code rotationYXZ(PI - yaw, -pitch, roll)} with its
     * forward along -Z, so a positive camera roll banks the view to the left, the opposite of
     * {@link #orientation}. A pilot looking down the nose therefore gets the negated hull roll.
     *
     * @return degrees, or 0 when looking straight along the hull's up axis
     */
    public static float cameraRoll(float yaw, float pitch, float roll, float viewYaw, float viewPitch) {
        Vector3f up = orientation(yaw, pitch, roll).transform(new Vector3f(0.0F, 1.0F, 0.0F));
        Quaternionf view = new Quaternionf().rotationYXZ(
                (float) (Math.PI - Math.toRadians(viewYaw)), (float) -Math.toRadians(viewPitch), 0.0F);
        float onUp = up.dot(view.transform(new Vector3f(0.0F, 1.0F, 0.0F)));
        float onLeft = up.dot(view.transform(new Vector3f(-1.0F, 0.0F, 0.0F)));
        if (onUp * onUp + onLeft * onLeft < 1.0E-6F) {
            return 0.0F;
        }
        return (float) Math.toDegrees(Math.atan2(onLeft, onUp));
    }

    public static float wrap(float degrees) {
        float d = degrees % 360.0F;
        if (d >= 180.0F) {
            d -= 360.0F;
        } else if (d < -180.0F) {
            d += 360.0F;
        }
        return d;
    }
}
