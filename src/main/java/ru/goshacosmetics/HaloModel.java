package ru.goshacosmetics;

import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

/**
 * Нимбы разных стилей, центр — (0,0,0).
 * up = +1 в мире (y вверх), -1 на модели (y вниз) — чтобы шипы короны смотрели вверх в обоих случаях.
 */
final class HaloModel {
    static final String[] NAMES = {"Выкл", "Кольцо", "Гироскоп", "Ледяная корона", "Звёзды"};

    private HaloModel() {}

    static void render(MatrixStack ms, VertexConsumerProvider vcp, Tint p, float t, int style, float up) {
        Mesh mesh = new Mesh(vcp.getBuffer(RenderLayer.getEntityCutoutNoCull(Cosmetics.WHITE)), ms.peek(),
                LightmapTextureManager.MAX_LIGHT_COORDINATE);
        float spin = t * 0.04f;
        switch (style) {
            case 2 -> {
                float tx = 0.45f * (float) Math.cos(t * 0.05f), tz = 0.45f * (float) Math.sin(t * 0.05f);
                torus(mesh, 0.23f, 0.018f, tx, tz, spin, p, t);
                torus(mesh, 0.30f, 0.018f, -tx, -tz, -spin * 1.3f, p, t);
                mesh.box(0, 0, 0, 0.03f, 0xFF000000 | p.light);
            }
            case 3 -> {
                torus(mesh, 0.24f, 0.016f, 0, 0, spin, p, t);
                for (int i = 0; i < 10; i++) {
                    double a = spin * 0.5 + i * Math.PI * 2 / 10;
                    float h = (i % 2 == 0 ? 0.13f : 0.08f) + 0.01f * (float) Math.sin(t * 0.2f + i);
                    spike(mesh, (float) Math.cos(a) * 0.24f, (float) Math.sin(a) * 0.24f, 0.026f, h * up, p);
                }
            }
            case 4 -> {
                for (int i = 0; i < 6; i++) {
                    double a = -t * 0.06f + i * Math.PI * 2 / 6;
                    float y = (float) Math.sin(t * 0.15f + i * 1.1f) * 0.04f;
                    star(mesh, (float) Math.cos(a) * 0.28f, y, (float) Math.sin(a) * 0.28f, 0.045f, t * 0.1f + i, p);
                }
            }
            default -> {
                torus(mesh, 0.27f, 0.034f, 0, 0, spin, p, t);
                for (int k = 0; k < 4; k++) {
                    double a = -t * 0.09f + k * Math.PI / 2;
                    float y = (float) Math.sin(t * 0.17f + k * 1.7f) * 0.05f;
                    mesh.box((float) (Math.cos(a) * 0.32f), y, (float) (Math.sin(a) * 0.32f), 0.012f, 0xFF000000 | p.light);
                }
            }
        }

        VertexConsumer glow = vcp.getBuffer(RenderLayer.getLightning());
        Matrix4f m = ms.peek().getPositionMatrix();
        float r = style == 2 ? 0.30f : style == 4 ? 0.28f : style == 3 ? 0.24f : 0.27f;
        ring(glow, m, r + 0.03f, r + 0.12f, Palette.argb(p.primary, style == 4 ? 60 : 110), Palette.argb(p.primary, 0));
        ring(glow, m, r - 0.10f, r - 0.03f, Palette.argb(p.light, 0), Palette.argb(p.light, style == 4 ? 50 : 90));
    }

    /** Тор с наклоном (rx, rz) и вращением spin; по кругу бежит блик. */
    private static void torus(Mesh mesh, float R, float tube, float rx, float rz, float spin, Tint p, float t) {
        int seg = 40, sides = 10;
        for (int i = 0; i < seg; i++) {
            double a0 = spin + i * Math.PI * 2 / seg, a1 = spin + (i + 1) * Math.PI * 2 / seg;
            int c0 = shine(p, a0, t), c1 = shine(p, a1, t);
            double am = (a0 + a1) / 2;
            mesh.center(rot(new float[]{(float) (R * Math.cos(am)), 0, (float) (R * Math.sin(am))}, rx, rz));
            for (int j = 0; j < sides; j++) {
                double b0 = j * Math.PI * 2 / sides, b1 = (j + 1) * Math.PI * 2 / sides;
                mesh.quad(rot(pt(R, tube, a0, b0), rx, rz), rot(pt(R, tube, a0, b1), rx, rz),
                        rot(pt(R, tube, a1, b1), rx, rz), rot(pt(R, tube, a1, b0), rx, rz), c0, c0, c1, c1);
            }
        }
    }

    private static float[] pt(float R, float tube, double a, double b) {
        float r = R + tube * (float) Math.cos(b);
        return new float[]{r * (float) Math.cos(a), tube * (float) Math.sin(b), r * (float) Math.sin(a)};
    }

    private static float[] rot(float[] v, float rx, float rz) {
        float cx = (float) Math.cos(rx), sx = (float) Math.sin(rx), cz = (float) Math.cos(rz), sz = (float) Math.sin(rz);
        float y1 = v[1] * cx - v[2] * sx, z1 = v[1] * sx + v[2] * cx;
        float x2 = v[0] * cz - y1 * sz, y2 = v[0] * sz + y1 * cz;
        return new float[]{x2, y2, z1};
    }

    private static int shine(Tint p, double a, float t) {
        float k = (float) Math.max(0, Math.cos(a * 2 - t * 0.15f));
        return Mesh.lerp(p.primary, p.light, 0.35f + 0.65f * k);
    }

    /** Ледяной шип (четырёхгранная пирамида). */
    private static void spike(Mesh mesh, float x, float z, float hw, float h, Tint p) {
        float[][] b = {{x - hw, 0, z - hw}, {x + hw, 0, z - hw}, {x + hw, 0, z + hw}, {x - hw, 0, z + hw}};
        float[] tip = {x, h, z};
        mesh.center(new float[]{x, h / 3, z});
        int base = 0xFF000000 | p.primary, top = 0xFFFFFFFF;
        for (int i = 0; i < 4; i++) {
            int j = (i + 1) % 4;
            mesh.quad(b[i], b[j], tip, tip, base, base, top, top);
        }
        mesh.quad(b[0], b[1], b[2], b[3], base, base, base, base);
    }

    /** Объёмная «звезда»-октаэдр, вращается. */
    private static void star(Mesh mesh, float x, float y, float z, float r, float spin, Tint p) {
        float c = (float) Math.cos(spin) * r, s = (float) Math.sin(spin) * r;
        float[][] v = {{x + c, y, z + s}, {x - s, y, z + c}, {x - c, y, z - s}, {x + s, y, z - c}};
        float[] top = {x, y + r * 1.4f, z}, bot = {x, y - r * 1.4f, z};
        mesh.center(new float[]{x, y, z});
        int a = 0xFF000000 | p.light, b = 0xFF000000 | p.primary;
        for (int i = 0; i < 4; i++) {
            int j = (i + 1) % 4;
            mesh.quad(v[i], v[j], top, top, i % 2 == 0 ? a : b, i % 2 == 0 ? a : b, 0xFFFFFFFF, 0xFFFFFFFF);
            mesh.quad(v[j], v[i], bot, bot, b, b, a, a);
        }
    }

    private static void ring(VertexConsumer vc, Matrix4f m, float r1, float r2, int cIn, int cOut) {
        int seg = 40;
        for (int i = 0; i < seg; i++) {
            double a0 = i * Math.PI * 2 / seg, a1 = (i + 1) * Math.PI * 2 / seg;
            float c0 = (float) Math.cos(a0), s0 = (float) Math.sin(a0), c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
            Draw.quad(vc, m,
                    r1 * c0, 0, r1 * s0, cIn,
                    r2 * c0, 0, r2 * s0, cOut,
                    r2 * c1, 0, r2 * s1, cOut,
                    r1 * c1, 0, r1 * s1, cIn);
        }
    }
}
