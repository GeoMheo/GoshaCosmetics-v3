package ru.goshacosmetics;

import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;

/** Шарф и рюкзак. Пространство тела: верх y = 0, низ y = 0.75, ±0.25 по x, перед z = -0.125. */
final class BodyModel {
    static final String[] SCARF = {"Выкл", "Шарф", "Длинный шарф"};
    static final String[] BACKPACK = {"Выкл", "Рюкзак", "Колчан"};

    private BodyModel() {}

    private static Mesh mesh(MatrixStack ms, VertexConsumerProvider vcp) {
        return new Mesh(vcp.getBuffer(RenderLayer.getEntityCutoutNoCull(Cosmetics.WHITE)), ms.peek(),
                LightmapTextureManager.MAX_LIGHT_COORDINATE);
    }

    static void scarf(MatrixStack ms, VertexConsumerProvider vcp, CosmeticsConfig.Slot s, float t, float move) {
        Tint p = Cosmetics.tint(s);
        ms.push();
        ms.scale(s.scale, s.scale, s.scale);
        Mesh m = mesh(ms, vcp);
        int a = 0xFF000000 | p.primary, b = 0xFF000000 | p.light;
        float y0 = -0.03f, y1 = 0.09f;
        // кольцо вокруг шеи, полосками
        for (int i = 0; i < 6; i++) {
            float x0 = -0.29f + i * 0.0967f, x1 = x0 + 0.0967f;
            int c = i % 2 == 0 ? a : b;
            m.cuboid(x0, y0, -0.17f, x1, y1, -0.125f, c);
            m.cuboid(x0, y0, 0.125f, x1, y1, 0.17f, c);
        }
        m.cuboid(-0.29f, y0, -0.125f, -0.25f, y1, 0.125f, a);
        m.cuboid(0.25f, y0, -0.125f, 0.29f, y1, 0.125f, b);
        // свисающий конец
        float len = s.style == 2 ? 0.62f : 0.36f;
        float sway = (float) Math.sin(t * 0.12f * s.speed) * 6f - Math.min(1f, move) * 35f;
        ms.push();
        ms.translate(0.12f, y1, -0.17f);
        ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sway));
        Mesh tail = m.at(ms.peek());
        int seg = 4;
        for (int i = 0; i < seg; i++) {
            float ya = i * len / seg, yb = (i + 1) * len / seg;
            tail.cuboid(-0.055f, ya, -0.03f, 0.055f, yb, 0.0f, i % 2 == 0 ? a : b);
        }
        for (int i = 0; i < 4; i++) {
            float x = -0.045f + i * 0.03f;
            tail.cuboid(x, len, -0.02f, x + 0.012f, len + 0.05f, -0.008f, b);
        }
        ms.pop();
        ms.pop();
    }

    static void backpack(MatrixStack ms, VertexConsumerProvider vcp, CosmeticsConfig.Slot s, float t) {
        Tint p = Cosmetics.tint(s);
        ms.push();
        ms.translate(0, 0.3f, 0.125f);
        ms.scale(s.scale, s.scale, s.scale);
        ms.translate(0, -0.3f, -0.125f);
        Mesh m = mesh(ms, vcp);
        int deep = 0xFF000000 | p.deep, prim = 0xFF000000 | p.primary, light = 0xFF000000 | p.light;
        if (s.style == 1) {
            m.cuboid(-0.19f, 0.06f, 0.125f, 0.19f, 0.58f, 0.31f, prim);
            m.cuboid(-0.2f, 0.03f, 0.12f, 0.2f, 0.2f, 0.325f, deep);
            m.cuboid(-0.13f, 0.32f, 0.31f, 0.13f, 0.52f, 0.35f, light);
            m.cuboid(-0.03f, 0.18f, 0.32f, 0.03f, 0.24f, 0.335f, 0xFFFFFFFF);
            for (int side = -1; side <= 1; side += 2) {
                float x = side * 0.12f;
                m.cuboid(x - 0.03f, -0.012f, -0.135f, x + 0.03f, 0.0f, 0.135f, deep);
                m.cuboid(x - 0.03f, 0.0f, -0.137f, x + 0.03f, 0.48f, -0.125f, deep);
            }
        } else if (s.style == 2) {
            ms.push();
            ms.translate(0, 0.35f, 0.19f);
            ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(25));
            Mesh q = m.at(ms.peek());
            q.cuboid(-0.08f, -0.35f, -0.06f, 0.08f, 0.35f, 0.06f, prim);
            q.cuboid(-0.085f, -0.37f, -0.065f, 0.085f, -0.33f, 0.065f, light);
            for (int i = 0; i < 4; i++) {
                float x = -0.05f + i * 0.033f;
                q.cuboid(x - 0.006f, -0.6f, -0.006f, x + 0.006f, -0.35f, 0.006f, 0xFFEEEEEE);
                q.cuboid(x - 0.018f, -0.62f, -0.004f, x + 0.018f, -0.52f, 0.004f, light);
            }
            ms.pop();
            m.cuboid(-0.27f, 0.0f, -0.135f, 0.27f, 0.05f, -0.125f, deep);
        }
        ms.pop();
    }
}
