package ru.goshacosmetics;

import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

/**
 * Объёмная геометрия с нормалями (освещение даёт объём).
 * Нормаль считается по граням и разворачивается наружу от центра текущей фигуры.
 */
final class Mesh {
    private final VertexConsumer vc;
    private final MatrixStack.Entry entry;
    private final Matrix4f m;
    private final int light;
    private float[] center;

    Mesh(VertexConsumer vc, MatrixStack.Entry entry, int light) {
        this.vc = vc;
        this.entry = entry;
        this.m = entry.getPositionMatrix();
        this.light = light;
    }

    void center(float[] c) { this.center = c; }

    /** Тот же буфер, другая матрица (после push/rotate). */
    Mesh at(MatrixStack.Entry e) { return new Mesh(vc, e, light); }

    /** Параллелепипед по двум углам. */
    void cuboid(float x0, float y0, float z0, float x1, float y1, float z1, int color) {
        float[] prev = center;
        center = new float[]{(x0 + x1) / 2, (y0 + y1) / 2, (z0 + z1) / 2};
        float[][] p = new float[8][];
        for (int i = 0; i < 8; i++) p[i] = new float[]{(i & 1) == 0 ? x0 : x1, (i & 2) == 0 ? y0 : y1, (i & 4) == 0 ? z0 : z1};
        int[][] f = {{0, 1, 3, 2}, {4, 5, 7, 6}, {0, 1, 5, 4}, {2, 3, 7, 6}, {0, 2, 6, 4}, {1, 3, 7, 5}};
        for (int[] q : f) quad(p[q[0]], p[q[1]], p[q[2]], p[q[3]], color, color, color, color);
        center = prev;
    }

    /** Усечённый конус вдоль оси Y (y0 → y1), sides граней, phase — поворот (рад). */
    void frustumY(float cx, float cz, float y0, float y1, float r0, float r1, int sides, float phase,
                  int c0, int c1, boolean caps) {
        float[] prev = center;
        center = new float[]{cx, (y0 + y1) / 2, cz};
        for (int i = 0; i < sides; i++) {
            double a0 = phase + i * Math.PI * 2 / sides, a1 = phase + (i + 1) * Math.PI * 2 / sides;
            float[] b0 = {cx + r0 * (float) Math.cos(a0), y0, cz + r0 * (float) Math.sin(a0)};
            float[] b1 = {cx + r0 * (float) Math.cos(a1), y0, cz + r0 * (float) Math.sin(a1)};
            float[] t0 = {cx + r1 * (float) Math.cos(a0), y1, cz + r1 * (float) Math.sin(a0)};
            float[] t1 = {cx + r1 * (float) Math.cos(a1), y1, cz + r1 * (float) Math.sin(a1)};
            quad(b0, b1, t1, t0, c0, c0, c1, c1);
            if (caps) {
                float[] cb = {cx, y0, cz}, ct = {cx, y1, cz};
                quad(cb, b1, b0, cb, c0, c0, c0, c0);
                if (r1 > 0) quad(ct, t0, t1, ct, c1, c1, c1, c1);
            }
        }
        center = prev;
    }

    /** Трубка по точкам (кольца перпендикулярно направлению), радиус может сужаться до 0. */
    void tube(float[][] pts, float[] radii, int sides, int[] colors, boolean closeStart) {
        int n = pts.length;
        float[][][] rings = new float[n][][];
        for (int k = 0; k < n; k++) {
            float[] a = pts[Math.max(0, k - 1)], b = pts[Math.min(n - 1, k + 1)];
            float dx = b[0] - a[0], dy = b[1] - a[1], dz = b[2] - a[2];
            float dl = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dl < 1e-6f) { dx = 0; dy = 1; dz = 0; dl = 1; }
            dx /= dl; dy /= dl; dz /= dl;
            float ux = 0, uy = 0, uz = 1;
            if (Math.abs(dz) > 0.9f) { ux = 1; uz = 0; }
            float px = dy * uz - dz * uy, py = dz * ux - dx * uz, pz = dx * uy - dy * ux;
            float pl = (float) Math.sqrt(px * px + py * py + pz * pz);
            px /= pl; py /= pl; pz /= pl;
            float qx = dy * pz - dz * py, qy = dz * px - dx * pz, qz = dx * py - dy * px;
            rings[k] = new float[sides][];
            for (int j = 0; j < sides; j++) {
                double ang = j * Math.PI * 2 / sides;
                float c = (float) Math.cos(ang) * radii[k], s = (float) Math.sin(ang) * radii[k];
                rings[k][j] = new float[]{pts[k][0] + px * c + qx * s, pts[k][1] + py * c + qy * s, pts[k][2] + pz * c + qz * s};
            }
        }
        float[] prev = center;
        for (int k = 0; k < n - 1; k++) {
            center = new float[]{(pts[k][0] + pts[k + 1][0]) / 2, (pts[k][1] + pts[k + 1][1]) / 2, (pts[k][2] + pts[k + 1][2]) / 2};
            for (int j = 0; j < sides; j++) {
                int jj = (j + 1) % sides;
                quad(rings[k][j], rings[k][jj], rings[k + 1][jj], rings[k + 1][j], colors[k], colors[k], colors[k + 1], colors[k + 1]);
            }
        }
        if (closeStart) {
            center = pts[Math.min(1, n - 1)];
            for (int j = 0; j < sides; j++) {
                int jj = (j + 1) % sides;
                quad(pts[0], rings[0][j], rings[0][jj], pts[0], colors[0], colors[0], colors[0], colors[0]);
            }
        }
        center = prev;
    }

    /** Пластина в плоскости XY толщиной z0..z1 по контуру (веер от центра). */
    void plateXY(float[] xs, float[] ys, float z0, float z1, int[] front, int back) {
        int n = xs.length;
        float cx = 0, cy = 0;
        for (int i = 0; i < n; i++) { cx += xs[i]; cy += ys[i]; }
        cx /= n; cy /= n;
        float[] prev = center;
        center = new float[]{cx, cy, (z0 + z1) / 2};
        int cf = lerp(front[0], front[n / 2], 0.5f);
        float[] f0 = {cx, cy, z0}, b0 = {cx, cy, z1};
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            float[] fi = {xs[i], ys[i], z0}, fj = {xs[j], ys[j], z0}, bi = {xs[i], ys[i], z1}, bj = {xs[j], ys[j], z1};
            quad(f0, fi, fj, f0, cf, front[i], front[j], cf);
            quad(b0, bj, bi, b0, back, back, back, back);
            quad(fi, fj, bj, bi, front[i], front[j], back, back);
        }
        center = prev;
    }

    void quad(float[] a, float[] b, float[] c, float[] d, int ca, int cb, int cc, int cd) {
        float[] n = normal(a, b, c);
        if (n == null) n = normal(a, c, d);
        if (n == null) return;
        if (center != null) {
            float fx = (a[0] + b[0] + c[0] + d[0]) / 4 - center[0];
            float fy = (a[1] + b[1] + c[1] + d[1]) / 4 - center[1];
            float fz = (a[2] + b[2] + c[2] + d[2]) / 4 - center[2];
            if (fx * n[0] + fy * n[1] + fz * n[2] < 0) { n[0] = -n[0]; n[1] = -n[1]; n[2] = -n[2]; }
        }
        v(a, ca, 0, 0, n);
        v(b, cb, 1, 0, n);
        v(c, cc, 1, 1, n);
        v(d, cd, 0, 1, n);
    }

    void box(float cx, float cy, float cz, float h, int color) {
        float[] c = {cx, cy, cz};
        float[] prev = center;
        center = c;
        float[][] p = new float[8][];
        for (int i = 0; i < 8; i++) {
            p[i] = new float[]{cx + ((i & 1) == 0 ? -h : h), cy + ((i & 2) == 0 ? -h : h), cz + ((i & 4) == 0 ? -h : h)};
        }
        int[][] f = {{0, 1, 3, 2}, {4, 5, 7, 6}, {0, 1, 5, 4}, {2, 3, 7, 6}, {0, 2, 6, 4}, {1, 3, 7, 5}};
        for (int[] q : f) quad(p[q[0]], p[q[1]], p[q[2]], p[q[3]], color, color, color, color);
        center = prev;
    }

    private void v(float[] p, int color, float u, float t, float[] n) {
        vc.vertex(m, p[0], p[1], p[2]).color(color).texture(u, t).overlay(OverlayTexture.DEFAULT_UV).light(light)
                .normal(entry, n[0], n[1], n[2]);
    }

    private static float[] normal(float[] a, float[] b, float[] c) {
        float ux = b[0] - a[0], uy = b[1] - a[1], uz = b[2] - a[2];
        float vx = c[0] - a[0], vy = c[1] - a[1], vz = c[2] - a[2];
        float nx = uy * vz - uz * vy, ny = uz * vx - ux * vz, nz = ux * vy - uy * vx;
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len < 1e-9f) return null;
        return new float[]{nx / len, ny / len, nz / len};
    }

    static int lerp(int a, int b, float k) {
        k = Math.max(0, Math.min(1, k));
        int ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255;
        int br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255;
        return 0xFF000000 | ((int) (ar + (br - ar) * k) << 16) | ((int) (ag + (bg - ag) * k) << 8) | (int) (ab + (bb - ab) * k);
    }
}
