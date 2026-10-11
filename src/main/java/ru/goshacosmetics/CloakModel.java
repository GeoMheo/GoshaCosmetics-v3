package ru.goshacosmetics;

import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

/** Развевающийся плащ с текстурой из библиотеки или своего файла. Пространство тела (y вниз, +z спина). */
final class CloakModel {
    private static final int ROWS = 10;

    private CloakModel() {}

    static void render(MatrixStack ms, VertexConsumerProvider vcp, CosmeticsConfig.Slot s, float t, float move, boolean sneak, int light) {
        float sc = s.scale;
        float w = 0.32f * sc, len = 1.0f * sc;
        float lift = (float) Math.toRadians(6 + Math.min(1f, move) * 55 + (sneak ? 22 : 0));
        int tint = Ui.parseColor(s.color, 0xFFFFFFFF);
        int back = Mesh.lerp(tint, 0x000000, 0.35f);

        VertexConsumer vc = vcp.getBuffer(RenderLayer.getEntityCutoutNoCull(Cosmetics.cloakTexture()));
        MatrixStack.Entry e = ms.peek();
        Matrix4f m = e.getPositionMatrix();

        float[] ys = new float[ROWS + 1], zs = new float[ROWS + 1];
        float y = 0, z = 0.145f;
        ys[0] = y; zs[0] = z;
        for (int i = 1; i <= ROWS; i++) {
            float f = i / (float) ROWS;
            float a = lift * f + (float) Math.sin(t * 0.18f * s.speed - f * 4.5f) * (0.10f + Math.min(1f, move) * 0.12f) * f;
            float step = len / ROWS;
            y += (float) Math.cos(a) * step;
            z += (float) Math.sin(a) * step;
            ys[i] = y; zs[i] = z;
        }
        // UV лицевой части плаща Minecraft: (1,1)-(11,17) из 64x32
        float u0 = 1 / 64f, u1 = 11 / 64f, v0 = 1 / 32f, v1 = 17 / 32f;
        for (int i = 0; i < ROWS; i++) {
            float va = v0 + (v1 - v0) * i / ROWS, vb = v0 + (v1 - v0) * (i + 1) / ROWS;
            float dy = ys[i + 1] - ys[i], dz = zs[i + 1] - zs[i];
            float nl = (float) Math.sqrt(dy * dy + dz * dz);
            float ny = -dz / nl, nz = dy / nl;
            // наружная сторона (видна сзади)
            v(vc, m, e, -w, ys[i], zs[i], tint, u1, va, light, 0, -ny, nz);
            v(vc, m, e, w, ys[i], zs[i], tint, u0, va, light, 0, -ny, nz);
            v(vc, m, e, w, ys[i + 1], zs[i + 1], tint, u0, vb, light, 0, -ny, nz);
            v(vc, m, e, -w, ys[i + 1], zs[i + 1], tint, u1, vb, light, 0, -ny, nz);
            // внутренняя сторона
            v(vc, m, e, -w, ys[i + 1], zs[i + 1] - 0.004f, back, u1, vb, light, 0, ny, -nz);
            v(vc, m, e, w, ys[i + 1], zs[i + 1] - 0.004f, back, u0, vb, light, 0, ny, -nz);
            v(vc, m, e, w, ys[i], zs[i] - 0.004f, back, u0, va, light, 0, ny, -nz);
            v(vc, m, e, -w, ys[i], zs[i] - 0.004f, back, u1, va, light, 0, ny, -nz);
        }
    }

    private static void v(VertexConsumer vc, Matrix4f m, MatrixStack.Entry e, float x, float y, float z, int color,
                          float u, float v, int light, float nx, float ny, float nz) {
        vc.vertex(m, x, y, z).color(color).texture(u, v).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(e, nx, ny, nz);
    }

    static int fullBright() { return LightmapTextureManager.MAX_LIGHT_COORDINATE; }
}
