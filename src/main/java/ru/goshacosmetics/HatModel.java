package ru.goshacosmetics;

import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;

/** Шапки. Пространство головы: верх головы y = -0.5 (y вниз), голова ±0.25. */
final class HatModel {
    static final String[] NAMES = {"Выкл", "Цилиндр", "Корона", "Колпак мага", "Шапка с помпоном", "Кепка", "Ведро-шлем"};
    private static final float T = -0.5f;

    private HatModel() {}

    static void render(MatrixStack ms, VertexConsumerProvider vcp, CosmeticsConfig.Slot slot, float t) {
        Tint p = Cosmetics.tint(slot);
        ms.push();
        ms.translate(0, T - slot.offset * 0.5f, 0);
        ms.scale(slot.scale, slot.scale, slot.scale);
        ms.translate(0, -T, 0);
        Mesh m = new Mesh(vcp.getBuffer(RenderLayer.getEntityCutoutNoCull(Cosmetics.WHITE)), ms.peek(),
                LightmapTextureManager.MAX_LIGHT_COORDINATE);
        int deep = 0xFF000000 | p.deep, prim = 0xFF000000 | p.primary, light = 0xFF000000 | p.light;
        switch (slot.style) {
            case 1 -> { // цилиндр
                m.frustumY(0, 0, T, T - 0.03f, 0.43f, 0.43f, 28, 0, deep, deep, true);
                m.frustumY(0, 0, T - 0.03f, T - 0.47f, 0.27f, 0.29f, 28, 0, deep, prim, true);
                m.frustumY(0, 0, T - 0.05f, T - 0.12f, 0.285f, 0.29f, 28, 0, light, light, false);
                m.cuboid(-0.04f, T - 0.10f, -0.30f, 0.04f, T - 0.06f, -0.28f, 0xFFFFFFFF);
            }
            case 2 -> { // корона
                float h = 0.275f;
                m.cuboid(-h, T + 0.02f, -h, h, T - 0.10f, -h + 0.03f, prim);
                m.cuboid(-h, T + 0.02f, h - 0.03f, h, T - 0.10f, h, prim);
                m.cuboid(-h, T + 0.02f, -h, -h + 0.03f, T - 0.10f, h, prim);
                m.cuboid(h - 0.03f, T + 0.02f, -h, h, T - 0.10f, h, prim);
                for (int i = 0; i < 12; i++) {
                    float k = (i % 3) / 2f;
                    float x, z;
                    int side = i / 3;
                    float along = -h + 0.06f + k * (2 * h - 0.12f);
                    x = side == 0 ? along : side == 1 ? h - 0.015f : side == 2 ? -along : -h + 0.015f;
                    z = side == 0 ? -h + 0.015f : side == 1 ? along : side == 2 ? h - 0.015f : -along;
                    float sh = (i % 3 == 1) ? 0.17f : 0.10f;
                    spike(m, x, T - 0.10f, z, 0.035f, sh, prim, light);
                }
                for (int i = 0; i < 4; i++) {
                    float a = i * 0.5f * (float) Math.PI;
                    float x = (float) Math.sin(a) * (h + 0.005f), z = -(float) Math.cos(a) * (h + 0.005f);
                    m.box(x, T - 0.04f, z, 0.022f, 0xFFFFFFFF);
                }
            }
            case 3 -> { // колпак мага (кончик загнут назад)
                m.frustumY(0, 0, T, T - 0.025f, 0.52f, 0.52f, 28, 0, deep, deep, true);
                int rings = 6;
                float[][] pts = new float[rings + 1][];
                float[] rad = new float[rings + 1];
                int[] col = new int[rings + 1];
                for (int k = 0; k <= rings; k++) {
                    float f = k / (float) rings;
                    pts[k] = new float[]{0, T - 0.025f - 0.75f * f, 0.25f * f * f};
                    rad[k] = 0.33f * (1 - f) + 0.004f;
                    col[k] = Mesh.lerp(p.deep, p.primary, f);
                }
                m.tube(pts, rad, 16, col, true);
                for (int i = 0; i < 5; i++) {
                    float f = 0.15f + i * 0.14f;
                    double a = i * 2.4;
                    float r = 0.33f * (1 - f) + 0.01f;
                    m.box((float) Math.cos(a) * r, T - 0.025f - 0.75f * f, (float) Math.sin(a) * r + 0.25f * f * f, 0.018f, light);
                }
            }
            case 4 -> { // шапка с помпоном
                m.cuboid(-0.29f, T + 0.10f, -0.29f, 0.29f, T + 0.01f, 0.29f, light);
                m.cuboid(-0.275f, T + 0.01f, -0.275f, 0.275f, T - 0.10f, 0.275f, prim);
                m.cuboid(-0.24f, T - 0.10f, -0.24f, 0.24f, T - 0.17f, 0.24f, light);
                m.cuboid(-0.18f, T - 0.17f, -0.18f, 0.18f, T - 0.22f, 0.18f, prim);
                float bob = (float) Math.sin(t * 0.15f * slot.speed) * 0.01f;
                m.box(0, T - 0.29f + bob, 0, 0.07f, 0xFFFFFFFF);
                m.box(0.04f, T - 0.32f + bob, 0.02f, 0.045f, light);
                m.box(-0.04f, T - 0.30f + bob, -0.03f, 0.045f, light);
            }
            case 5 -> { // кепка
                m.cuboid(-0.27f, T + 0.07f, -0.27f, 0.27f, T - 0.07f, 0.27f, prim);
                m.cuboid(-0.22f, T - 0.07f, -0.22f, 0.22f, T - 0.11f, 0.22f, prim);
                m.cuboid(-0.24f, T + 0.06f, -0.52f, 0.24f, T + 0.08f, -0.27f, deep);
                m.cuboid(-0.03f, T - 0.11f, -0.03f, 0.03f, T - 0.14f, 0.03f, light);
                m.cuboid(-0.275f, T + 0.0f, -0.12f, -0.27f, T - 0.06f, 0.12f, light);
            }
            case 6 -> { // ведро-шлем
                m.frustumY(0, 0, T + 0.18f, T - 0.20f, 0.40f, 0.33f, 4, (float) Math.PI / 4, deep, prim, false);
                m.frustumY(0, 0, T - 0.20f, T - 0.22f, 0.33f, 0.33f, 4, (float) Math.PI / 4, prim, prim, true);
                m.cuboid(-0.29f, T + 0.13f, -0.30f, 0.29f, T + 0.16f, -0.28f, light);
                m.cuboid(-0.005f, T - 0.22f, -0.005f, 0.005f, T - 0.30f, 0.005f, light);
                m.box(0, T - 0.32f, 0, 0.02f, 0xFFFFFFFF);
            }
            default -> { }
        }
        ms.pop();
    }

    static void spike(Mesh m, float x, float y, float z, float hw, float h, int base, int tip) {
        float[][] b = {{x - hw, y, z - hw}, {x + hw, y, z - hw}, {x + hw, y, z + hw}, {x - hw, y, z + hw}};
        float[] top = {x, y - h, z};
        m.center(new float[]{x, y - h / 3, z});
        for (int i = 0; i < 4; i++) {
            int j = (i + 1) % 4;
            m.quad(b[i], b[j], top, top, base, base, tip, tip);
        }
        m.quad(b[0], b[1], b[2], b[3], base, base, base, base);
    }
}
