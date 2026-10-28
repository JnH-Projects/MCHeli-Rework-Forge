package com.jnhprojects.mcheli.content.aircraft.part;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code AddCrawlerTrack = reverse, linkSpacing, x, y1/z1, y2/z2, ...}.
 *
 * <p>The outline points describe the track loop in the Y/Z plane. Links are placed every
 * {@code spacing} along that loop, each with the angle it should be drawn at, and the renderer
 * slides every link towards the next one as the track runs. Ported from
 * {@code MCH_AircraftInfo.createCrawlerTrack}, including its 0.9 spacing factor.
 *
 * @param x     track X position, which also decides the side
 * @param side  1 for positive X, 0 for negative X
 * @param links link positions in the Y/Z plane, closing the loop
 */
public record CrawlerTrack(String part, float spacing, float x, int side, List<Link> links) {

    /** Link position ({@code y}, {@code z}) and its roll around X in degrees. */
    public record Link(float y, float z, float angle) {
    }

    /**
     * Builds the link list from the outline.
     *
     * @return {@code null} with fewer than four outline points, as the original rejected those
     */
    public static CrawlerTrack create(String part, boolean reverse, float spacingSetting, float x, float[] ys, float[] zs) {
        int count = ys.length;
        if (count < 4) {
            return null;
        }
        float spacing = Math.clamp(spacingSetting, 0.001F, 1000.0F) * 0.9F;
        double[] cy = new double[count];
        double[] cz = new double[count];
        for (int i = 0; i < count; i++) {
            int src = reverse ? count - i - 1 : i;
            cy[i] = ys[src];
            cz[i] = zs[src];
        }

        List<float[]> points = new ArrayList<>();
        points.add(new float[]{(float) cy[0], (float) cz[0]});
        double dist = 0.0;
        for (int i = 0; i < count; i++) {
            double dy = cy[(i + 1) % count] - cy[i];
            double dz = cz[(i + 1) % count] - cz[i];
            dist += Math.sqrt(dy * dy + dz * dz);
            double segment = dist;
            for (int j = 1; dist >= spacing; j++) {
                points.add(new float[]{
                        (float) (cy[i] + dy * (spacing * j / segment)),
                        (float) (cz[i] + dz * (spacing * j / segment))});
                dist -= spacing;
            }
        }

        List<Link> links = new ArrayList<>(points.size());
        int n = points.size();
        for (int i = 0; i < n; i++) {
            float[] prev = points.get((i + n - 1) % n);
            float[] cur = points.get(i);
            float[] next = points.get((i + 1) % n);
            float toPrev = (float) Math.toDegrees(Math.atan2(prev[0] - cur[0], prev[1] - cur[1]));
            float toNext = (float) Math.toDegrees(Math.atan2(next[0] - cur[0], next[1] - cur[1]));
            float prevAngle = (toPrev + 360.0F) % 360.0F;
            float angle = toNext + 180.0F;
            // Average the incoming and outgoing directions at corners, as the original did.
            if ((angle < prevAngle - 0.3 || angle > prevAngle + 0.3) && angle - prevAngle < 100.0F && angle - prevAngle > -100.0F) {
                angle = (angle + prevAngle) / 2.0F;
            }
            links.add(new Link(cur[0], cur[1], angle));
        }
        return new CrawlerTrack(part, spacing, x, x >= 0.0F ? 1 : 0, List.copyOf(links));
    }
}
