package ru.goshacosmetics;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Светящиеся следы шагов и круг-волна при приземлении. */
public final class Footprints {
    static final String[] NAMES = {"Выкл", "Шаги", "Снежинки", "Звёзды"};
    private static final long STEP_MS = 4000, RIPPLE_MS = 700;
    private static final double STRIDE = 0.62;
    private static final int MAX_STEPS = 80;

    private record Step(double x, double y, double z, float yaw, boolean left, long time) {}
    private record Ripple(double x, double y, double z, long time) {}

    private static final List<Step> steps = new ArrayList<>();
    private static final List<Ripple> ripples = new ArrayList<>();
    private static final Map<UUID, double[]> last = new HashMap<>();   // x, z, левая(1)/правая(0)
    private static final Map<UUID, Boolean> wasOnGround = new HashMap<>();
    private static final Map<UUID, Float> lastFall = new HashMap<>();

    private Footprints() {}

    public static void tick(MinecraftClient mc) {
        if (mc.world == null) {
            steps.clear(); ripples.clear(); last.clear(); wasOnGround.clear(); lastFall.clear();
            return;
        }
        CosmeticsConfig c = CosmeticsConfig.get();
        long now = System.currentTimeMillis();
        steps.removeIf(s -> now - s.time > STEP_MS);
        ripples.removeIf(r -> now - r.time > RIPPLE_MS);

        for (AbstractClientPlayerEntity pl : mc.world.getPlayers()) {
            if (pl.isInvisible() || pl.isSpectator() || !Cosmetics.appliesTo(pl.getGameProfile().getName())) continue;
            UUID id = pl.getUuid();
            boolean onGround = pl.isOnGround();

            Boolean was = wasOnGround.put(id, onGround);
            Float fall = lastFall.put(id, pl.fallDistance);
            if (c.landing.style > 0 && onGround && was != null && !was && fall != null && fall > 1.2f) {
                ripples.add(new Ripple(pl.getX(), pl.getY(), pl.getZ(), now));
            }

            double[] l = last.get(id);
            if (l == null) { last.put(id, new double[]{pl.getX(), pl.getZ(), 0}); continue; }
            if (!onGround) continue;
            double dx = pl.getX() - l[0], dz = pl.getZ() - l[1];
            if (dx * dx + dz * dz < STRIDE * STRIDE) continue;
            boolean left = l[2] == 0;
            if (c.steps.style > 0) {
                float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
                steps.add(new Step(pl.getX(), pl.getY(), pl.getZ(), yaw, left, now));
                if (steps.size() > MAX_STEPS) steps.remove(0);
            }
            l[0] = pl.getX();
            l[1] = pl.getZ();
            l[2] = left ? 1 : 0;
        }
    }

    public static void render(WorldRenderContext ctx) {
        if (steps.isEmpty() && ripples.isEmpty()) return;
        VertexConsumerProvider vcp = ctx.consumers();
        MatrixStack ms = ctx.matrixStack();
        if (vcp == null || ms == null) return;
        Vec3d cam = ctx.camera().getPos();
        CosmeticsConfig cfg = CosmeticsConfig.get();
        Tint p = Cosmetics.tint(cfg.steps);
        Tint lp = Cosmetics.tint(cfg.landing);
        float ss = cfg.steps.scale, ls = cfg.landing.scale;
        VertexConsumer vc = vcp.getBuffer(RenderLayer.getLightning());
        Matrix4f m = ms.peek().getPositionMatrix();
        long now = System.currentTimeMillis();
        int style = Math.max(1, cfg.steps.style);

        for (Step s : steps) {
            float life = (now - s.time) / (float) STEP_MS;
            int alpha = (int) (170 * (1 - life) * (1 - life));
            double yr = Math.toRadians(s.yaw);
            float fx = (float) -Math.sin(yr), fz = (float) Math.cos(yr);   // вперёд
            float rx = (float) Math.cos(yr), rz = (float) Math.sin(yr);    // вбок
            float side = s.left ? 0.11f : -0.11f;
            float cx = (float) (s.x - cam.x) + rx * side;
            float cy = (float) (s.y - cam.y) + 0.015f;
            float cz = (float) (s.z - cam.z) + rz * side;
            switch (style) {
                case 2 -> snowflake(vc, m, cx, cy, cz, (float) yr + (s.left ? 0.3f : -0.3f), 0.11f * ss, p, alpha);
                case 3 -> star(vc, m, cx, cy, cz, (float) yr, 0.10f * ss, p, alpha);
                default -> {
                    oval(vc, m, cx, cy, cz, fx, fz, rx, rz, 0.065f * ss, 0.11f * ss, p, alpha);                       // подошва
                    oval(vc, m, cx + fx * 0.15f * ss, cy, cz + fz * 0.15f * ss, fx, fz, rx, rz, 0.055f * ss, 0.055f * ss, p, alpha); // носок
                }
            }
        }

        for (Ripple r : ripples) {
            float k = (now - r.time) / (float) RIPPLE_MS;
            float radius = (0.3f + 1.6f * k) * ls;
            int alpha = (int) (200 * (1 - k));
            float x = (float) (r.x - cam.x), y = (float) (r.y - cam.y) + 0.02f, z = (float) (r.z - cam.z);
            ring(vc, m, x, y, z, radius - 0.10f, radius, Palette.argb(lp.primary, 0), Palette.argb(lp.light, alpha));
            ring(vc, m, x, y, z, radius, radius + 0.06f, Palette.argb(lp.light, alpha), Palette.argb(lp.primary, 0));
        }
        HaloRenderer.flush(vcp);
    }

    /** Снежинка: 6 лучей с веточками. */
    private static void snowflake(VertexConsumer vc, Matrix4f m, float cx, float cy, float cz, float rot, float r, Tint p, int alpha) {
        int core = Palette.argb(p.light, alpha), tip = Palette.argb(p.primary, alpha / 3);
        for (int k = 0; k < 6; k++) {
            double a = rot + k * Math.PI / 3;
            float ex = cx + (float) Math.cos(a) * r, ez = cz + (float) Math.sin(a) * r;
            line(vc, m, cx, cz, ex, ez, cy, 0.012f, core, tip);
            float bx = cx + (float) Math.cos(a) * r * 0.6f, bz = cz + (float) Math.sin(a) * r * 0.6f;
            for (int sgn = -1; sgn <= 1; sgn += 2) {
                double b = a + sgn * Math.PI / 4;
                line(vc, m, bx, bz, bx + (float) Math.cos(b) * r * 0.35f, bz + (float) Math.sin(b) * r * 0.35f, cy, 0.009f, core, tip);
            }
        }
    }

    /** Пятиконечная звезда. */
    private static void star(VertexConsumer vc, Matrix4f m, float cx, float cy, float cz, float rot, float r, Tint p, int alpha) {
        int center = Palette.argb(p.light, alpha), edge = Palette.argb(p.primary, alpha / 2);
        for (int k = 0; k < 10; k++) {
            double a0 = rot + k * Math.PI / 5, a1 = rot + (k + 1) * Math.PI / 5;
            float r0 = (k % 2 == 0) ? r : r * 0.42f, r1 = (k % 2 == 0) ? r * 0.42f : r;
            float x0 = cx + (float) Math.cos(a0) * r0, z0 = cz + (float) Math.sin(a0) * r0;
            float x1 = cx + (float) Math.cos(a1) * r1, z1 = cz + (float) Math.sin(a1) * r1;
            Draw.quad(vc, m, cx, cy, cz, center, x0, cy, z0, edge, x1, cy, z1, edge, cx, cy, cz, center);
        }
    }

    /** Плоская линия на земле. */
    private static void line(VertexConsumer vc, Matrix4f m, float x0, float z0, float x1, float z1, float y, float w, int c0, int c1) {
        float dx = x1 - x0, dz = z1 - z0, l = (float) Math.sqrt(dx * dx + dz * dz);
        if (l < 1e-5f) return;
        float px = -dz / l * w, pz = dx / l * w;
        Draw.quad(vc, m, x0 + px, y, z0 + pz, c0, x1 + px, y, z1 + pz, c1, x1 - px, y, z1 - pz, c1, x0 - px, y, z0 - pz, c0);
    }

    private static void oval(VertexConsumer vc, Matrix4f m, float cx, float cy, float cz,
                             float fx, float fz, float rx, float rz, float w, float h, Tint p, int alpha) {
        int n = 14;
        int center = Palette.argb(p.light, alpha);
        int edge = Palette.argb(p.primary, alpha / 3);
        for (int i = 0; i < n; i++) {
            double a0 = i * Math.PI * 2 / n, a1 = (i + 1) * Math.PI * 2 / n;
            float x0 = cx + rx * w * (float) Math.cos(a0) + fx * h * (float) Math.sin(a0);
            float z0 = cz + rz * w * (float) Math.cos(a0) + fz * h * (float) Math.sin(a0);
            float x1 = cx + rx * w * (float) Math.cos(a1) + fx * h * (float) Math.sin(a1);
            float z1 = cz + rz * w * (float) Math.cos(a1) + fz * h * (float) Math.sin(a1);
            Draw.quad(vc, m, cx, cy, cz, center, x0, cy, z0, edge, x1, cy, z1, edge, cx, cy, cz, center);
        }
    }

    private static void ring(VertexConsumer vc, Matrix4f m, float x, float y, float z, float r1, float r2, int cIn, int cOut) {
        int n = 40;
        for (int i = 0; i < n; i++) {
            double a0 = i * Math.PI * 2 / n, a1 = (i + 1) * Math.PI * 2 / n;
            float c0 = (float) Math.cos(a0), s0 = (float) Math.sin(a0), c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
            Draw.quad(vc, m,
                    x + r1 * c0, y, z + r1 * s0, cIn,
                    x + r2 * c0, y, z + r2 * s0, cOut,
                    x + r2 * c1, y, z + r2 * s1, cOut,
                    x + r1 * c1, y, z + r1 * s1, cIn);
        }
    }
}
