package com.jnhprojects.mcheli.workshop;

import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The orthographic view around the model in the workshop.
 *
 * <p>In the frame the game draws GUI entities in, X points right, Y points down and Z points away
 * from the viewer, with one unit being {@link #scale()} GUI pixels. {@link #rotation()} and
 * {@link #translation()} bring a point of the model into that frame, and {@link #project} says where
 * it lands, so the gizmos drawn on top line up with the vehicle drawn underneath.
 */
public final class WorkshopCamera {
    private static final float MIN_SCALE = 1.0F;
    private static final float MAX_SCALE = 400.0F;
    /** How much of the view a framed model takes up, leaving room around it for gizmos. */
    private static final float FRAME_FILL = 0.85F;
    private static final float DEGREES_PER_PIXEL = 0.6F;
    private static final float DEFAULT_YAW = 215.0F;
    private static final float DEFAULT_PITCH = 25.0F;

    private float yaw = DEFAULT_YAW;
    private float pitch = DEFAULT_PITCH;
    private float scale = 20.0F;
    private final Vector3f target = new Vector3f();

    /** GUI pixels per block. */
    public float scale() {
        return scale;
    }

    public void orbit(double dx, double dy) {
        yaw += (float) dx * DEGREES_PER_PIXEL;
        pitch = Math.clamp(pitch + (float) dy * DEGREES_PER_PIXEL, -89.0F, 89.0F);
    }

    /** Moves the view so the point under the mouse stays under the mouse. */
    public void pan(double dx, double dy) {
        Vector3f move = unproject(dx, dy);
        target.sub(move);
    }

    /** Zooms by a factor. */
    public void zoom(double factor) {
        scale = Math.clamp((float) (scale * factor), MIN_SCALE, MAX_SCALE);
    }

    /** Looks at the middle of a box, from the starting angle, with all of it in a view of the given size. */
    public void frame(float[] min, float[] max, int width, int height) {
        yaw = DEFAULT_YAW;
        pitch = DEFAULT_PITCH;
        target.set((min[0] + max[0]) / 2.0F, (min[1] + max[1]) / 2.0F, (min[2] + max[2]) / 2.0F);
        scale = 1.0F;
        float reachX = 0.1F;
        float reachY = 0.1F;
        for (int corner = 0; corner < 8; corner++) {
            Vector3f p = project((corner & 1) == 0 ? min[0] : max[0], (corner & 2) == 0 ? min[1] : max[1], (corner & 4) == 0 ? min[2] : max[2]);
            reachX = Math.max(reachX, Math.abs(p.x));
            reachY = Math.max(reachY, Math.abs(p.y));
        }
        scale = Math.clamp(FRAME_FILL * Math.min(width / 2.0F / reachX, height / 2.0F / reachY), MIN_SCALE, MAX_SCALE);
    }

    public Quaternionf rotation() {
        return new Quaternionf().rotationZ((float) Math.PI)
                .rotateX((float) Math.toRadians(-pitch))
                .rotateY((float) Math.toRadians(yaw));
    }

    /** What to move the rotated model by, so the target lands in the middle of the view. */
    public Vector3f translation() {
        return rotation().transform(new Vector3f(target)).negate();
    }

    /**
     * Where a point of the model lands.
     *
     * @return GUI pixels from the middle of the view in X and Y, and blocks away from the viewer in Z
     */
    public Vector3f project(double x, double y, double z) {
        Vector3f v = rotation().transform(new Vector3f((float) x, (float) y, (float) z).sub(target));
        return new Vector3f(v.x * scale, v.y * scale, v.z);
    }

    /** The move in the model that shows as a move of so many GUI pixels, staying at the same depth. */
    public Vector3f unproject(double dx, double dy) {
        return rotation().conjugate().transform(new Vector3f((float) dx / scale, (float) dy / scale, 0.0F));
    }
}
