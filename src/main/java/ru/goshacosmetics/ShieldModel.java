package ru.goshacosmetics;

import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

/** Щиты. Локально: центр (0,0,0), y вверх, лицевая сторона смотрит в -z, высота ~0.9. */
final class ShieldModel {
    static final String[] NAMES = {"Выкл", "Круглый", "Рыцарский", "Кристалл", "Энергощит"};

    private ShieldModel() {}

    static void render(MatrixStack ms, VertexConsumerProvider vcp, CosmeticsConfig.Slot s, float t) {
        Tint p = Cosmetics.tint(s);
        Mesh m = new Mesh(vcp.getBuffer(RenderLayer.getEntityCutoutNoCull(Cosmetics.WHITE)), ms.peek(),
                LightmapTextureManager.MAX_LIGHT_COORDINATE);
        int deep = 0xFF000000 | p.deep, prim = 0xFF000000 | p.primary, light = 0xFF000000 | p.light;
        switch (s.style) {
            case 1 -> { // круглый
                int n = 24;
                float[] xs = new float[n], ys = new float[n];
                int[] col = new int[n];
                for (int i = 0; i < n; i++) {
                    double a = i * Math.PI * 2 / n;
                    xs[i] = (float) Math.cos(a) * 0.36f; ys[i] = (float) Math.sin(a) * 0.36f;
                    col[i] = Mesh.lerp(p.deep, p.primary, 0.5f + 0.5f * (float) Math.sin(a));
                }
                m.plateXY(xs, ys, -0.02f, 0.02f, col, deep);
                float[][] rim = new float[n + 1][];
                float[] rr = new float[n + 1];
                int[] rc = new int[n + 1];
                for (int i = 0; i <= n; i++) {
                    double a = i * Math.PI * 2 / n;
                    rim[i] = new float[]{(float) Math.cos(a) * 0.36f, (float) Math.sin(a) * 0.36f, -0.01f};
                    rr[i] = 0.03f; rc[i] = light;
                }
                m.tube(rim, rr, 6, rc, false);
                m.cuboid(-0.25f, -0.03f, -0.035f, 0.25f, 0.03f, -0.02f, light);
                m.cuboid(-0.03f, -0.25f, -0.035f, 0.03f, 0.25f, -0.02f, light);
                m.box(0, 0, -0.05f, 0.06f, 0xFFFFFFFF);
            }
            case 2 -> { // рыцарский (каплевидный) со снежинкой
                float[] xs = {-0.30f, 0.30f, 0.32f, 0.17f, 0f, -0.17f, -0.32f};
                float[] ys = {0.42f, 0.42f, 0.05f, -0.30f, -0.50f, -0.30f, 0.05f};
                int[] col = new int[xs.length];
                for (int i = 0; i < xs.length; i++) col[i] = Mesh.lerp(p.deep, p.primary, (ys[i] + 0.5f) / 0.92f);
                float[] bx = new float[xs.length], by = new float[xs.length];
                int[] bc = new int[xs.length];
                for (int i = 0; i < xs.length; i++) { bx[i] = xs[i] * 1.08f; by[i] = ys[i] * 1.07f - 0.005f; bc[i] = light; }
                m.plateXY(bx, by, 0.0f, 0.025f, bc, light);
                m.plateXY(xs, ys, -0.025f, 0.0f, col, deep);
                for (int k = 0; k < 6; k++) {
                    double a = k * Math.PI / 3;
                    bar(m, 0, 0.02f, (float) Math.cos(a) * 0.2f, 0.02f + (float) Math.sin(a) * 0.2f, -0.032f, 0.018f, light);
                }
                m.box(0, 0.02f, -0.035f, 0.035f, 0xFFFFFFFF);
            }
            case 3 -> { // кристалл: гранёный шестиугольник
                int n = 6;
                float[] xs = new float[n], ys = new float[n];
                int[] col = new int[n];
                for (int i = 0; i < n; i++) {
                    double a = Math.PI / 2 + i * Math.PI * 2 / n;
                    xs[i] = (float) Math.cos(a) * 0.40f; ys[i] = (float) Math.sin(a) * 0.40f; col[i] = prim;
                }
                m.plateXY(xs, ys, -0.015f, 0.02f, col, deep);
                float[] apex = {0, 0, -0.14f};
                m.center(new float[]{0, 0, 0});
                for (int i = 0; i < n; i++) {
                    int j = (i + 1) % n;
                    float[] a = {xs[i], ys[i], -0.015f}, b = {xs[j], ys[j], -0.015f};
                    int c = i % 2 == 0 ? light : prim;
                    m.quad(a, b, apex, apex, c, c, 0xFFFFFFFF, 0xFFFFFFFF);
                }
            }
            case 4 -> { // энергощит: рамка + соты из света
                for (int i = 0; i < 6; i++) {
                    double a0 = Math.PI / 2 + i * Math.PI / 3, a1 = Math.PI / 2 + (i + 1) * Math.PI / 3;
                    bar(m, (float) Math.cos(a0) * 0.42f, (float) Math.sin(a0) * 0.42f,
                            (float) Math.cos(a1) * 0.42f, (float) Math.sin(a1) * 0.42f, 0, 0.02f, light);
                }
            }
            default -> { }
        }
        if (s.style == 4) {
            VertexConsumer glow = vcp.getBuffer(RenderLayer.getLightning());
            Matrix4f mat = ms.peek().getPositionMatrix();
            float r = 0.075f;
            int idx = 0;
            for (int q = -3; q <= 3; q++) {
                for (int rr = -3; rr <= 3; rr++) {
                    float x = r * 1.75f * (q + rr * 0.5f), y = r * 1.5f * rr;
                    if (x * x + y * y > 0.33f * 0.33f) continue;
                    int a = 60 + (int) (60 * Math.sin(t * 0.2f * s.speed + idx * 1.3f));
                    hex(glow, mat, x, y, r * 0.92f, Palette.argb(p.light, a), Palette.argb(p.primary, a / 3));
                    idx++;
                }
            }
        }
    }

    /** Брусок от (x0,y0) до (x1,y1) в плоскости XY. */
    private static void bar(Mesh m, float x0, float y0, float x1, float y1, float z, float w, int c) {
        float dx = x1 - x0, dy = y1 - y0, l = (float) Math.sqrt(dx * dx + dy * dy);
        if (l < 1e-5f) return;
        float px = -dy / l * w / 2, py = dx / l * w / 2;
        float[] xs = {x0 + px, x1 + px, x1 - px, x0 - px}, ys = {y0 + py, y1 + py, y1 - py, y0 - py};
        m.plateXY(xs, ys, z - 0.01f, z + 0.01f, new int[]{c, c, c, c}, c);
    }

    private static void hex(VertexConsumer vc, Matrix4f m, float cx, float cy, float r, int cIn, int cOut) {
        for (int i = 0; i < 6; i++) {
            double a0 = i * Math.PI / 3, a1 = (i + 1) * Math.PI / 3;
            float x0 = cx + (float) Math.cos(a0) * r, y0 = cy + (float) Math.sin(a0) * r;
            float x1 = cx + (float) Math.cos(a1) * r, y1 = cy + (float) Math.sin(a1) * r;
            Draw.quad(vc, m, cx, cy, -0.01f, cIn, x0, y0, -0.01f, cOut, x1, y1, -0.01f, cOut, cx, cy, -0.01f, cIn);
        }
    }
}
