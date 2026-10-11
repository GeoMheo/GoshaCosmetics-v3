package ru.goshacosmetics;

import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

/**
 * Объёмные крылья разных стилей. Плоскость крыла: u — вдоль размаха, v — вверх, w — назад.
 * Координаты тела: y вниз, +z — спина.
 */
final class WingModel {
    static final String[] NAMES = {"Выкл", "Ангел", "Бабочка", "Демон", "Фея", "Кристалл"};
    private static final float ROOT_X = 0.07f, ROOT_Y = 0.22f, ROOT_Z = 0.15f;

    private final int s;
    private final float scale, cosP, sinP, cosF, sinF;

    private WingModel(int side, float sweepDeg, float flapDeg, float scale) {
        this.s = side;
        this.scale = scale;
        double p = Math.toRadians(sweepDeg), f = Math.toRadians(flapDeg);
        cosP = (float) Math.cos(p); sinP = (float) Math.sin(p);
        cosF = (float) Math.cos(f); sinF = (float) Math.sin(f);
    }

    private float[] at(float u, float v, float w) {
        u *= scale; v *= scale;
        float uf = u * cosF - v * sinF, vf = u * sinF + v * cosF;
        return new float[]{s * (ROOT_X + uf * cosP - w * sinP), ROOT_Y - vf, ROOT_Z + uf * sinP + w * cosP};
    }

    static void render(MatrixStack ms, VertexConsumerProvider vcp, Tint p, float t, float move, boolean sneak, int style) {
        if (style <= 0) return;
        float mv = Math.max(0f, Math.min(1f, move));
        float sweep, flap, scale = 0.9f;
        switch (style) {
            case 2 -> { // бабочка: складывает крылья назад
                float k = 0.5f + 0.5f * (float) Math.sin(t * (0.25f + mv * 0.25f));
                sweep = sneak ? 70f : 18f + 52f * k; flap = 0; scale = 0.85f;
            }
            case 3 -> { // демон: медленные мощные взмахи
                flap = (float) Math.sin(t * (0.07f + mv * 0.16f)) * (sneak ? 3f : 8f + mv * 14f);
                sweep = (sneak ? 12f : 20f) + flap * 0.4f;
            }
            case 4 -> { // фея: быстрое трепетание
                flap = (float) Math.sin(t * 1.5f) * 6f;
                sweep = (sneak ? 40f : 28f) + (float) Math.sin(t * 1.5f) * 10f; scale = 0.85f;
            }
            case 5 -> { // кристалл: почти неподвижен, парит
                flap = (float) Math.sin(t * 0.06f) * 4f;
                sweep = (sneak ? 14f : 22f) + (float) Math.sin(t * 0.05f) * 4f;
            }
            default -> { // ангел
                flap = (float) Math.sin(t * (0.09f + mv * 0.2f)) * (sneak ? 3f : 9f + mv * 14f);
                sweep = (sneak ? 10f : 24f) + flap * 0.4f;
            }
        }
        WingModel[] wings = {new WingModel(-1, sweep, flap, scale), new WingModel(1, sweep, flap, scale)};

        Mesh mesh = new Mesh(vcp.getBuffer(RenderLayer.getEntityCutoutNoCull(Cosmetics.WHITE)), ms.peek(),
                LightmapTextureManager.MAX_LIGHT_COORDINATE);
        for (WingModel w : wings) {
            switch (style) {
                case 2 -> w.butterfly(mesh, p);
                case 3 -> w.demon(mesh, p);
                case 4 -> w.fairy(mesh, p);
                case 5 -> w.crystal(mesh, p, t);
                default -> w.angel(mesh, p, t);
            }
        }
        VertexConsumer glow = vcp.getBuffer(RenderLayer.getLightning());
        Matrix4f m = ms.peek().getPositionMatrix();
        for (WingModel w : wings) {
            switch (style) {
                case 2 -> w.butterflyGlow(glow, m, p);
                case 3 -> w.demonGlow(glow, m, p);
                case 4 -> w.fairyGlow(glow, m, p, t);
                case 5 -> w.crystalGlow(glow, m, p, t);
                default -> w.angelGlow(glow, m, p, t);
            }
        }
    }

    // ================= ангел =================
    private static float armU(float t) { return 0.05f + 1.0f * t; }
    private static float armV(float t) { return 0.08f + 0.55f * t - 0.42f * t * t; }
    private static float flutter(float t, int i) { return (float) Math.sin(t * 0.25f + i * 0.9f) * 2.5f; }

    private void angel(Mesh mesh, Tint p, float t) {
        int seg = 8;
        for (int i = 0; i < seg; i++) {
            float t0 = i / (float) seg, t1 = (i + 1) / (float) seg;
            rod(mesh, armU(t0), armV(t0), armU(t1), armV(t1), 0, 0.045f - 0.028f * t0, 0.045f - 0.028f * t1,
                    Mesh.lerp(p.deep, p.primary, t0), Mesh.lerp(p.deep, p.primary, t1));
        }
        for (int i = 0; i < 6; i++) {
            float k = i / 5f, tt = 0.06f + 0.44f * k;
            blade(mesh, armU(tt), armV(tt), -82f + 30f * k + flutter(t, i), 0.46f + 0.10f * k, 0.13f, 0.022f, i * 0.003f, p.deep, p.primary);
        }
        for (int i = 0; i < 6; i++) {
            float k = i / 5f, tt = 0.52f + 0.48f * k;
            blade(mesh, armU(tt), armV(tt), -48f + 36f * k + flutter(t, i + 6), 0.58f + 0.17f * k * (1.2f - k), 0.12f, 0.022f,
                    0.02f + i * 0.003f, p.primary, p.light);
        }
        for (int i = 0; i < 7; i++) {
            float k = i / 6f, tt = 0.08f + 0.82f * k;
            blade(mesh, armU(tt), armV(tt) - 0.02f, -62f + 34f * k, 0.24f, 0.10f, 0.018f, -0.03f, p.primary, p.light);
        }
    }

    private void angelGlow(VertexConsumer vc, Matrix4f m, Tint p, float t) {
        for (int i = 0; i < 6; i++) {
            float k = i / 5f, tt = 0.52f + 0.48f * k;
            double a = Math.toRadians(-48f + 36f * k + flutter(t, i + 6));
            float dx = (float) Math.cos(a), dy = (float) Math.sin(a), qx = -dy, qy = dx;
            float len = (0.58f + 0.17f * k * (1.2f - k)) * 1.08f, h = 0.09f;
            float u0 = armU(tt), v0 = armV(tt), w = 0.06f;
            float um = u0 + dx * len * 0.5f, vm = v0 + dy * len * 0.5f;
            glowQuad(vc, m, new float[]{u0, um + qx * h, u0 + dx * len, um - qx * h},
                    new float[]{v0, vm + qy * h, v0 + dy * len, vm - qy * h}, w,
                    Palette.argb(p.primary, 25), Palette.argb(p.light, 75), Palette.argb(p.light, 0), Palette.argb(p.light, 75));
        }
    }

    // ================= бабочка =================
    private static final float[] BU_U = {0.02f, 0.18f, 0.42f, 0.66f, 0.80f, 0.74f, 0.52f, 0.22f};
    private static final float[] BU_V = {0.04f, 0.30f, 0.52f, 0.58f, 0.46f, 0.22f, 0.06f, 0.00f};
    private static final float[] BL_U = {0.02f, 0.26f, 0.46f, 0.48f, 0.32f, 0.14f, 0.04f};
    private static final float[] BL_V = {-0.02f, -0.06f, -0.22f, -0.42f, -0.54f, -0.42f, -0.18f};

    private void butterfly(Mesh mesh, Tint p) {
        int rim = Mesh.lerp(p.deep, 0x000000, 0.55f);
        lobe(mesh, BU_U, BU_V, 0.32f, 0.26f, p, rim);
        lobe(mesh, BL_U, BL_V, 0.24f, -0.26f, p, rim);
        // пятна
        float[][] spots = {{0.62f, 0.46f}, {0.71f, 0.33f}, {0.40f, -0.36f}, {0.30f, -0.46f}};
        for (float[] sp : spots) disc(mesh, sp[0], sp[1], 0.032f, -0.016f, 0xFF000000 | p.light);
        // прожилки
        for (int i = 1; i < BU_U.length - 1; i += 2) rod(mesh, 0.03f, 0.02f, BU_U[i] * 0.9f, BU_V[i] * 0.9f, -0.012f, 0.006f, 0.004f, rim, rim);
        for (int i = 1; i < BL_U.length - 1; i += 2) rod(mesh, 0.03f, -0.02f, BL_U[i] * 0.9f, BL_V[i] * 0.9f, -0.012f, 0.006f, 0.004f, rim, rim);
    }

    private void lobe(Mesh mesh, float[] pu, float[] pv, float cu, float cv, Tint p, int rim) {
        int n = pu.length;
        int[] col = new int[n];
        for (int i = 0; i < n; i++) {
            float d = (float) Math.sqrt(pu[i] * pu[i] + pv[i] * pv[i]);
            col[i] = Mesh.lerp(p.deep, p.primary, d / 0.8f);
        }
        plate(mesh, pu, pv, col, cu, cv, 0f, 0.016f, Mesh.lerp(p.deep, p.primary, 0.4f));
        // тёмная кайма — чуть больший контур позади
        float[] ru = new float[n], rv = new float[n];
        int[] rc = new int[n];
        for (int i = 0; i < n; i++) {
            ru[i] = cu + (pu[i] - cu) * 1.07f; rv[i] = cv + (pv[i] - cv) * 1.07f; rc[i] = rim;
        }
        plate(mesh, ru, rv, rc, cu, cv, 0.014f, 0.006f, rim);
    }

    private void butterflyGlow(VertexConsumer vc, Matrix4f m, Tint p) {
        glowFan(vc, m, BU_U, BU_V, 0.32f, 0.26f, 1.15f, 0.03f, Palette.argb(p.primary, 55), Palette.argb(p.light, 0));
        glowFan(vc, m, BL_U, BL_V, 0.24f, -0.26f, 1.15f, 0.03f, Palette.argb(p.primary, 55), Palette.argb(p.light, 0));
    }

    // ================= демон =================
    private static final float[][] DEMON_TIPS = {{1.10f, 0.30f}, {1.00f, -0.05f}, {0.80f, -0.38f}, {0.48f, -0.58f}};

    private void demon(Mesh mesh, Tint p) {
        float[] sh = {0.02f, 0.02f}, el = {0.35f, 0.30f}, wr = {0.70f, 0.42f}, an = {0.06f, -0.45f};
        int bone = Mesh.lerp(p.deep, 0x000000, 0.45f);
        int claw = 0xFF000000 | p.light;
        rod(mesh, sh[0], sh[1], el[0], el[1], 0, 0.040f, 0.030f, bone, bone);
        rod(mesh, el[0], el[1], wr[0], wr[1], 0, 0.030f, 0.022f, bone, bone);
        for (float[] tip : DEMON_TIPS) {
            rod(mesh, wr[0], wr[1], tip[0], tip[1], 0, 0.018f, 0.006f, bone, bone);
            mesh.box(at(tip[0], tip[1], 0)[0], at(tip[0], tip[1], 0)[1], at(tip[0], tip[1], 0)[2], 0.014f, claw);
        }
        mesh.box(at(wr[0], wr[1] + 0.06f, 0)[0], at(wr[0], wr[1] + 0.06f, 0)[1], at(wr[0], wr[1] + 0.06f, 0)[2], 0.02f, claw);
        // перепонки между пальцами с волнистым краем
        for (int i = 0; i < DEMON_TIPS.length - 1; i++) {
            float[] a = DEMON_TIPS[i], b = DEMON_TIPS[i + 1];
            float mu = (a[0] + b[0]) / 2 + (wr[0] - (a[0] + b[0]) / 2) * 0.18f;
            float mvv = (a[1] + b[1]) / 2 + (wr[1] - (a[1] + b[1]) / 2) * 0.18f;
            float[] pu = {wr[0], a[0], mu, b[0]}, pv = {wr[1], a[1], mvv, b[1]};
            int[] col = {0xFF000000 | p.deep, 0xFF000000 | p.primary, Mesh.lerp(p.deep, p.primary, 0.6f), 0xFF000000 | p.primary};
            plate(mesh, pu, pv, col, (wr[0] + a[0] + b[0]) / 3, (wr[1] + a[1] + b[1]) / 3, 0.008f, 0.010f, Mesh.lerp(p.deep, p.primary, 0.5f));
        }
        // внутренняя перепонка до тела
        float[] last = DEMON_TIPS[DEMON_TIPS.length - 1];
        float mu = (last[0] + an[0]) / 2 + (el[0] - (last[0] + an[0]) / 2) * 0.15f;
        float mvv = (last[1] + an[1]) / 2 + (el[1] - (last[1] + an[1]) / 2) * 0.15f;
        float[] pu = {sh[0], el[0], wr[0], last[0], mu, an[0]}, pv = {sh[1], el[1], wr[1], last[1], mvv, an[1]};
        int d = 0xFF000000 | p.deep, pr = 0xFF000000 | p.primary;
        plate(mesh, pu, pv, new int[]{d, d, d, pr, Mesh.lerp(p.deep, p.primary, 0.6f), pr}, 0.3f, -0.08f, 0.008f, 0.010f, d);
    }

    private void demonGlow(VertexConsumer vc, Matrix4f m, Tint p) {
        float[] pu = {0.70f, 1.10f, 1.00f, 0.80f, 0.48f, 0.06f}, pv = {0.42f, 0.30f, -0.05f, -0.38f, -0.58f, -0.45f};
        glowFan(vc, m, pu, pv, 0.5f, 0.0f, 1.08f, 0.04f, Palette.argb(p.primary, 40), Palette.argb(p.light, 0));
    }

    // ================= фея =================
    private void fairy(Mesh mesh, Tint p) {
        fairyWing(mesh, p, 38f, 0.80f, 0.26f);
        fairyWing(mesh, p, -28f, 0.55f, 0.18f);
    }

    private void fairyWing(Mesh mesh, Tint p, float angle, float len, float wid) {
        float[][] e = ellipse(angle, len, wid, 18);
        int rim = 0xFF000000 | p.light;
        for (int i = 0; i < e[0].length; i++) {
            int j = (i + 1) % e[0].length;
            rod(mesh, e[0][i], e[1][i], e[0][j], e[1][j], 0, 0.006f, 0.006f, rim, rim);
        }
        double a = Math.toRadians(angle);
        rod(mesh, 0.02f, 0f, 0.02f + (float) Math.cos(a) * len, (float) Math.sin(a) * len, 0, 0.006f, 0.003f,
                0xFF000000 | p.primary, rim);
    }

    private void fairyGlow(VertexConsumer vc, Matrix4f m, Tint p, float t) {
        int a = 70 + (int) (25 * Math.sin(t * 0.3f));
        for (float[] wing : new float[][]{{38f, 0.80f, 0.26f}, {-28f, 0.55f, 0.18f}}) {
            float[][] e = ellipse(wing[0], wing[1], wing[2], 18);
            double ang = Math.toRadians(wing[0]);
            float cu = 0.02f + (float) Math.cos(ang) * wing[1] / 2, cv = (float) Math.sin(ang) * wing[1] / 2;
            glowFan(vc, m, e[0], e[1], cu, cv, 1f, 0f, Palette.argb(p.primary, a), Palette.argb(p.light, a / 2));
            glowFan(vc, m, e[0], e[1], cu, cv, 1.12f, 0.02f, Palette.argb(p.light, 25), Palette.argb(p.primary, 0));
        }
    }

    private static float[][] ellipse(float angleDeg, float len, float wid, int n) {
        double a = Math.toRadians(angleDeg);
        float dx = (float) Math.cos(a), dy = (float) Math.sin(a), qx = -dy, qy = dx;
        float cu = 0.02f + dx * len / 2, cv = dy * len / 2;
        float[] u = new float[n], v = new float[n];
        for (int i = 0; i < n; i++) {
            double th = i * Math.PI * 2 / n;
            float c = (float) Math.cos(th) * len / 2, s = (float) Math.sin(th) * wid / 2;
            u[i] = cu + dx * c + qx * s;
            v[i] = cv + dy * c + qy * s;
        }
        return new float[][]{u, v};
    }

    // ================= кристалл =================
    private static final float[] CR_A = {-45f, -22f, 0f, 20f, 40f, 60f, 80f};
    private static final float[] CR_L = {0.45f, 0.70f, 0.92f, 0.82f, 0.70f, 0.55f, 0.40f};

    private void crystal(Mesh mesh, Tint p, float t) {
        for (int i = 0; i < CR_A.length; i++) {
            double a = Math.toRadians(CR_A[i]);
            float bob = (float) Math.sin(t * 0.1f + i * 1.3f) * 0.03f;
            float cu = 0.06f + 0.03f * i + (float) Math.cos(a) * bob, cv = 0.05f + (float) Math.sin(a) * bob;
            shard(mesh, cu, cv, CR_A[i], CR_L[i], 0.045f + 0.015f * (float) Math.sin(i * 2.1f), (i % 3) * 0.01f, p);
        }
    }

    private void shard(Mesh mesh, float cu, float cv, float angleDeg, float len, float r, float w0, Tint p) {
        double a = Math.toRadians(angleDeg);
        float dx = (float) Math.cos(a), dy = (float) Math.sin(a), qx = -dy, qy = dx;
        int n = 6;
        float[][] base = new float[n][], mid = new float[n][];
        for (int k = 0; k < n; k++) {
            double ph = k * Math.PI * 2 / n;
            float c = (float) Math.cos(ph), s = (float) Math.sin(ph);
            base[k] = at(cu + qx * c * r * 0.6f, cv + qy * c * r * 0.6f, w0 + s * r * 0.6f);
            mid[k] = at(cu + dx * len * 0.72f + qx * c * r, cv + dy * len * 0.72f + qy * c * r, w0 + s * r);
        }
        float[] tip = at(cu + dx * len, cv + dy * len, w0);
        float[] bc = at(cu, cv, w0);
        mesh.center(at(cu + dx * len * 0.5f, cv + dy * len * 0.5f, w0));
        int deep = 0xFF000000 | p.deep;
        for (int k = 0; k < n; k++) {
            int j = (k + 1) % n;
            int face = (k % 2 == 0) ? (0xFF000000 | p.light) : (0xFF000000 | p.primary);
            mesh.quad(base[k], base[j], mid[j], mid[k], deep, deep, face, face);
            mesh.quad(mid[k], mid[j], tip, tip, face, face, 0xFFFFFFFF, 0xFFFFFFFF);
            mesh.quad(bc, base[j], base[k], bc, deep, deep, deep, deep);
        }
    }

    private void crystalGlow(VertexConsumer vc, Matrix4f m, Tint p, float t) {
        for (int i = 0; i < CR_A.length; i++) {
            double a = Math.toRadians(CR_A[i]);
            float dx = (float) Math.cos(a), dy = (float) Math.sin(a), qx = -dy, qy = dx;
            float cu = 0.06f + 0.03f * i, cv = 0.05f, len = CR_L[i] * 1.1f, h = 0.09f;
            int al = 45 + (int) (30 * Math.sin(t * 0.2f + i));
            glowQuad(vc, m, new float[]{cu, cu + dx * len * 0.7f + qx * h, cu + dx * len, cu + dx * len * 0.7f - qx * h},
                    new float[]{cv, cv + dy * len * 0.7f + qy * h, cv + dy * len, cv + dy * len * 0.7f - qy * h}, 0.03f,
                    Palette.argb(p.primary, 10), Palette.argb(p.light, al), Palette.argb(p.light, 0), Palette.argb(p.light, al));
        }
    }

    // ================= общие детали =================

    /** Стержень квадратного сечения от (u0,v0) до (u1,v1). */
    private void rod(Mesh mesh, float u0, float v0, float u1, float v1, float w, float h0, float h1, int c0, int c1) {
        float du = u1 - u0, dv = v1 - v0, l = (float) Math.sqrt(du * du + dv * dv);
        if (l < 1e-5f) return;
        float qx = -dv / l, qy = du / l;
        float[][] a = {at(u0 + qx * h0, v0 + qy * h0, w - h0), at(u0 + qx * h0, v0 + qy * h0, w + h0),
                at(u0 - qx * h0, v0 - qy * h0, w + h0), at(u0 - qx * h0, v0 - qy * h0, w - h0)};
        float[][] b = {at(u1 + qx * h1, v1 + qy * h1, w - h1), at(u1 + qx * h1, v1 + qy * h1, w + h1),
                at(u1 - qx * h1, v1 - qy * h1, w + h1), at(u1 - qx * h1, v1 - qy * h1, w - h1)};
        mesh.center(at((u0 + u1) / 2, (v0 + v1) / 2, w));
        for (int i = 0; i < 4; i++) {
            int j = (i + 1) % 4;
            mesh.quad(a[i], a[j], b[j], b[i], c0, c0, c1, c1);
        }
        mesh.quad(a[0], a[1], a[2], a[3], c0, c0, c0, c0);
        mesh.quad(b[0], b[1], b[2], b[3], c1, c1, c1, c1);
    }

    /** Пластина с толщиной по контуру (лицо, спинка, боковины). */
    private void plate(Mesh mesh, float[] pu, float[] pv, int[] col, float cu, float cv, float w, float thick, int cCenter) {
        int n = pu.length;
        float wf = w - thick / 2, wb = w + thick / 2;
        mesh.center(at(cu, cv, w));
        float[] cf = at(cu, cv, wf), cb = at(cu, cv, wb);
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            float[] fi = at(pu[i], pv[i], wf), fj = at(pu[j], pv[j], wf);
            float[] bi = at(pu[i], pv[i], wb), bj = at(pu[j], pv[j], wb);
            mesh.quad(cf, fi, fj, cf, cCenter, col[i], col[j], cCenter);
            mesh.quad(cb, bj, bi, cb, cCenter, col[j], col[i], cCenter);
            mesh.quad(fi, fj, bj, bi, col[i], col[j], col[j], col[i]);
        }
    }

    private void blade(Mesh mesh, float u0, float v0, float angleDeg, float len, float width, float thick,
                       float w, int cRoot, int cTip) {
        double a = Math.toRadians(angleDeg);
        float dx = (float) Math.cos(a), dy = (float) Math.sin(a), qx = -dy, qy = dx;
        float[] fr = {0f, 0.5f, 0.85f, 1f}, hw = {0.35f, 0.5f, 0.38f, 0f};
        int n = fr.length * 2 - 1;
        float[] pu = new float[n], pv = new float[n];
        int[] col = new int[n];
        for (int i = 0; i < fr.length; i++) {
            float cu = u0 + dx * len * fr[i], cv = v0 + dy * len * fr[i], h = hw[i] * width;
            int r = n - 1 - i;
            pu[i] = cu + qx * h; pv[i] = cv + qy * h; col[i] = Mesh.lerp(cRoot, cTip, fr[i]);
            pu[r] = cu - qx * h; pv[r] = cv - qy * h; col[r] = col[i];
        }
        plate(mesh, pu, pv, col, u0 + dx * len * 0.45f, v0 + dy * len * 0.45f, w, thick, Mesh.lerp(cRoot, cTip, 0.45f));
    }

    private void disc(Mesh mesh, float cu, float cv, float r, float w, int color) {
        int n = 8;
        float[] pu = new float[n], pv = new float[n];
        int[] col = new int[n];
        for (int i = 0; i < n; i++) {
            double a = i * Math.PI * 2 / n;
            pu[i] = cu + (float) Math.cos(a) * r; pv[i] = cv + (float) Math.sin(a) * r; col[i] = color;
        }
        plate(mesh, pu, pv, col, cu, cv, w, 0.006f, color);
    }

    private void glowQuad(VertexConsumer vc, Matrix4f m, float[] u, float[] v, float w, int c0, int c1, int c2, int c3) {
        float[] a = at(u[0], v[0], w), b = at(u[1], v[1], w), c = at(u[2], v[2], w), d = at(u[3], v[3], w);
        Draw.quad(vc, m, a[0], a[1], a[2], c0, b[0], b[1], b[2], c1, c[0], c[1], c[2], c2, d[0], d[1], d[2], c3);
    }

    /** Свечение-веер по контуру (контур можно увеличить относительно центра). */
    private void glowFan(VertexConsumer vc, Matrix4f m, float[] pu, float[] pv, float cu, float cv, float grow, float w,
                         int cCenter, int cEdge) {
        float[] c = at(cu, cv, w);
        int n = pu.length;
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            float[] a = at(cu + (pu[i] - cu) * grow, cv + (pv[i] - cv) * grow, w);
            float[] b = at(cu + (pu[j] - cu) * grow, cv + (pv[j] - cv) * grow, w);
            Draw.quad(vc, m, c[0], c[1], c[2], cCenter, a[0], a[1], a[2], cEdge, b[0], b[1], b[2], cEdge, c[0], c[1], c[2], cCenter);
        }
    }
}
