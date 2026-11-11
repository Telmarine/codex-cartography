package com.tac5studios.codexcartography.hud;

import com.tac5studios.codexcartography.config.ClientConfig;

/** World to screen for the minimap (north up, or turned with the player). */
public final class Projection {

    final double wx;
    final double wz;
    final double pixelsPerBlock;
    final double fx;
    final double fz;
    public final float cx;
    public final float cy;
    public final float radius;
    final ClientConfig.Shape shape;

    /**
     * @param yaw 180 for north up, else the player's yaw (the map turns with them)
     */
    public Projection(double wx, double wz, double pixelsPerBlock, float yaw, float cx, float cy, float radius, ClientConfig.Shape shape) {
        this.wx = wx;
        this.wz = wz;
        this.pixelsPerBlock = pixelsPerBlock;
        double r = Math.toRadians(yaw);
        this.fx = -Math.sin(r);
        this.fz = Math.cos(r);
        this.cx = cx;
        this.cy = cy;
        this.radius = radius;
        this.shape = shape;
    }

    /** Screen offset from the center for a world position. */
    public float[] offset(double x, double z) {
        double dx = x - wx;
        double dz = z - wz;
        double sx = (dx * -fz + dz * fx) * pixelsPerBlock;
        double sy = -(dx * fx + dz * fz) * pixelsPerBlock;
        return new float[] {(float) sx, (float) sy};
    }

    public float distanceFromCenter(double x, double z) {
        float[] o = offset(x, z);
        return (float) Math.sqrt(o[0] * o[0] + o[1] * o[1]);
    }

    public boolean inside(double x, double z) {
        float[] o = offset(x, z);
        float r = radius - 2;
        return Minimap.inShape(shape, o[0], o[1], r, (2 - 2 * Minimap.PLATE_CUT) * r);
    }

    /** Screen position, pinned to the map's edge when outside it. */
    public float[] toScreenClamped(double x, double z) {
        float[] o = offset(x, z);
        float r = radius - 4;
        if (!Minimap.inShape(shape, o[0], o[1], r, (2 - 2 * Minimap.PLATE_CUT) * r)) {
            float lo = 0;
            float hi = 1;
            for (int i = 0; i < 12; i++) {
                float mid = (lo + hi) / 2;
                if (Minimap.inShape(shape, o[0] * mid, o[1] * mid, r, (2 - 2 * Minimap.PLATE_CUT) * r)) lo = mid;
                else hi = mid;
            }
            o[0] *= lo;
            o[1] *= lo;
        }
        return new float[] {cx + o[0], cy + o[1]};
    }
}
