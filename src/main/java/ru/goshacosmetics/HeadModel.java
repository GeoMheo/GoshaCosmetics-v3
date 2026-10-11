package ru.goshacosmetics;

import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;

/** Ушки, рога, очки. Пространство головы: верх y = -0.5, перед головы z = -0.25. */
final class HeadModel {
    static final String[] EARS = {"Выкл", "Кошачьи", "Лисьи", "Заячьи"};
    static final String[] HORNS = {"Выкл", "Демона", "Ледяные", "Оленьи"};
    static final String[] GLASSES = {"Выкл", "Очки", "Визор", "Монокль"};
    private static final float T = -0.5f;

    private HeadModel() {}

    private static Mesh mesh(MatrixStack ms, VertexConsumerProvider vcp) {
        return new Mesh(vcp.getBuffer(RenderLayer.getEntityCutoutNoCull(Cosmetics.WHITE)), ms.peek(),
                LightmapTextureManager.MAX_LIGHT_COORDINATE);
    }

    private static void scaled(MatrixStack ms, CosmeticsConfig.Slot s, float pivotY) {
        ms.translate(0, pivotY, 0);
        ms.scale(s.scale, s.scale, s.scale);
        ms.translate(0, -pivotY, 0);
    }

    // ---------- ушки ----------
    static void ears(MatrixStack ms, VertexConsumerProvider vcp, CosmeticsConfig.Slot s, float t) {
        Tint p = Cosmetics.tint(s);
        ms.push();
        scaled(ms, s, T);
        Mesh base = mesh(ms, vcp);
        int prim = 0xFF000000 | p.primary, light = 0xFF000000 | p.light, deep = 0xFF000000 | p.deep;
        for (int side = -1; side <= 1; side += 2) {
            float x = side * 0.16f;
            switch (s.style) {
                case 1 -> ear(base, x, 0.075f, 0.15f, prim, light);
                case 2 -> ear(base, x * 1.1f, 0.10f, 0.23f, prim, 0xFFFFFFFF);
                case 3 -> {
                    float wob = (float) Math.sin(t * 0.12f * s.speed + side) * 8f;
                    ms.push();
                    ms.translate(x, T, 0);
                    ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * 10f + wob));
                    Mesh m = base.at(ms.peek());
                    m.cuboid(-0.04f, 0, -0.02f, 0.04f, -0.40f, 0.02f, prim);
                    m.cuboid(-0.025f, -0.04f, -0.026f, 0.025f, -0.36f, -0.02f, light);
                    ms.pop();
                }
                default -> { }
            }
        }
        ms.pop();
    }

    private static void ear(Mesh m, float x, float hw, float h, int outer, int inner) {
        float z0 = -0.03f, z1 = 0.03f;
        float[] a0 = {x - hw, T, z0}, b0 = {x + hw, T, z0}, c0 = {x, T - h, z0};
        float[] a1 = {x - hw, T, z1}, b1 = {x + hw, T, z1}, c1 = {x, T - h, z1};
        m.center(new float[]{x, T - h / 3, 0});
        m.quad(a0, b0, c0, c0, inner, inner, inner, inner);
        m.quad(a1, b1, c1, c1, outer, outer, outer, outer);
        m.quad(a0, b0, b1, a1, outer, outer, outer, outer);
        m.quad(b0, c0, c1, b1, outer, inner, outer, outer);
        m.quad(c0, a0, a1, c1, inner, outer, outer, outer);
    }

    // ---------- рога ----------
    static void horns(MatrixStack ms, VertexConsumerProvider vcp, CosmeticsConfig.Slot s, float t) {
        Tint p = Cosmetics.tint(s);
        ms.push();
        scaled(ms, s, T);
        Mesh m = mesh(ms, vcp);
        for (int side = -1; side <= 1; side += 2) {
            switch (s.style) {
                case 1 -> { // демон: изогнутые назад
                    int n = 8;
                    float[][] pts = new float[n][];
                    float[] r = new float[n];
                    int[] c = new int[n];
                    for (int k = 0; k < n; k++) {
                        float f = k / (float) (n - 1);
                        pts[k] = new float[]{side * (0.17f + 0.10f * f), T + 0.04f - 0.30f * f + 0.10f * f * f, -0.05f + 0.26f * f * f};
                        r[k] = 0.055f * (1 - f) + 0.004f;
                        c[k] = Mesh.lerp(p.deep, p.light, f);
                    }
                    m.tube(pts, r, 8, c, true);
                }
                case 2 -> { // ледяные кристаллы
                    for (int k = 0; k < 3; k++) {
                        float ang = 0.35f + k * 0.35f;
                        float len = 0.30f - k * 0.07f;
                        float x0 = side * (0.14f + k * 0.03f), y0 = T + 0.02f, z0 = -0.05f + k * 0.07f;
                        float[][] pts = {{x0, y0, z0}, {x0 + side * (float) Math.sin(ang) * len * 0.6f, y0 - (float) Math.cos(ang) * len * 0.6f, z0},
                                {x0 + side * (float) Math.sin(ang) * len, y0 - (float) Math.cos(ang) * len, z0}};
                        m.tube(pts, new float[]{0.04f, 0.045f, 0.001f}, 6,
                                new int[]{0xFF000000 | p.deep, 0xFF000000 | p.primary, 0xFFFFFFFF}, true);
                    }
                }
                case 3 -> { // оленьи
                    float[][] main = {{side * 0.15f, T + 0.03f, 0}, {side * 0.22f, T - 0.18f, 0.03f}, {side * 0.30f, T - 0.38f, 0.02f}};
                    int bc = 0xFF000000 | p.primary, tc = 0xFF000000 | p.light;
                    m.tube(main, new float[]{0.035f, 0.028f, 0.012f}, 6, new int[]{bc, bc, tc}, true);
                    for (int k = 0; k < 3; k++) {
                        float f = 0.3f + 0.25f * k;
                        float[] st = {side * (0.15f + 0.15f * f), T + 0.03f - 0.41f * f, 0.02f};
                        float[][] br = {st, {st[0] + side * 0.12f, st[1] - 0.10f, st[2] - 0.02f}};
                        m.tube(br, new float[]{0.02f, 0.008f}, 6, new int[]{bc, tc}, false);
                    }
                }
                default -> { }
            }
        }
        ms.pop();
    }

    // ---------- очки ----------
    static void glasses(MatrixStack ms, VertexConsumerProvider vcp, CosmeticsConfig.Slot s, float t) {
        Tint p = Cosmetics.tint(s);
        ms.push();
        ms.translate(0, s.offset * 0.1f, 0);
        scaled(ms, s, -0.22f);
        Mesh m = mesh(ms, vcp);
        int frame = 0xFF000000 | p.deep, lens = 0xFF000000 | p.primary, shine = 0xFF000000 | p.light;
        float z = -0.262f, cy = -0.22f;
        switch (s.style) {
            case 1 -> {
                for (int side = -1; side <= 1; side += 2) {
                    float cx = side * 0.125f;
                    m.cuboid(cx - 0.085f, cy - 0.065f, z - 0.012f, cx + 0.085f, cy - 0.05f, z, frame);
                    m.cuboid(cx - 0.085f, cy + 0.05f, z - 0.012f, cx + 0.085f, cy + 0.065f, z, frame);
                    m.cuboid(cx - 0.085f, cy - 0.065f, z - 0.012f, cx - 0.07f, cy + 0.065f, z, frame);
                    m.cuboid(cx + 0.07f, cy - 0.065f, z - 0.012f, cx + 0.085f, cy + 0.065f, z, frame);
                    m.cuboid(cx - 0.07f, cy - 0.05f, z - 0.006f, cx + 0.07f, cy + 0.05f, z - 0.002f, lens);
                    m.cuboid(cx - 0.05f, cy - 0.04f, z - 0.008f, cx - 0.02f, cy - 0.01f, z - 0.006f, shine);
                    m.cuboid(side * 0.255f, cy - 0.06f, z, side * 0.27f, cy - 0.045f, 0.1f, frame);
                }
                m.cuboid(-0.04f, cy - 0.04f, z - 0.012f, 0.04f, cy - 0.025f, z, frame);
            }
            case 2 -> {
                m.cuboid(-0.275f, cy - 0.075f, z - 0.03f, 0.275f, cy + 0.06f, z, lens);
                m.cuboid(-0.275f, cy - 0.085f, z - 0.035f, 0.275f, cy - 0.075f, z, shine);
                m.cuboid(-0.2f, cy - 0.04f, z - 0.034f, 0.2f, cy - 0.02f, z - 0.03f, shine);
                m.cuboid(-0.28f, cy - 0.05f, z, -0.265f, cy + 0.04f, 0.26f, frame);
                m.cuboid(0.265f, cy - 0.05f, z, 0.28f, cy + 0.04f, 0.26f, frame);
            }
            case 3 -> {
                float cx = 0.125f;
                int n = 16;
                float[][] pts = new float[n + 1][];
                float[] r = new float[n + 1];
                int[] c = new int[n + 1];
                for (int k = 0; k <= n; k++) {
                    double a = k * Math.PI * 2 / n;
                    pts[k] = new float[]{cx + (float) Math.cos(a) * 0.075f, cy + (float) Math.sin(a) * 0.075f, z - 0.008f};
                    r[k] = 0.01f;
                    c[k] = shine;
                }
                m.tube(pts, r, 6, c, false);
                m.cuboid(cx - 0.06f, cy - 0.06f, z - 0.004f, cx + 0.06f, cy + 0.06f, z - 0.001f, lens);
                float[][] chain = {{cx + 0.07f, cy + 0.04f, z - 0.008f}, {cx + 0.13f, cy + 0.14f, z + 0.02f}, {cx + 0.2f, cy + 0.2f, z + 0.1f}};
                m.tube(chain, new float[]{0.005f, 0.005f, 0.005f}, 4, new int[]{shine, shine, shine}, false);
            }
            default -> { }
        }
        ms.pop();
    }
}
