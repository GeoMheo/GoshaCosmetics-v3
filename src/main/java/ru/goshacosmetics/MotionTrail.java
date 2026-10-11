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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Полупрозрачная лента-след за игроком: тянется по пройденному пути и тает. */
public final class MotionTrail {
    static final String[] NAMES = {"Выкл", "Лента", "Две полосы"};
    private static final long LIFE_MS = 600;
    private static final double MIN_STEP = 0.08;
    private static final int MAX_POINTS = 60;

    private record Pt(double x, double y, double z, long time) {}

    private static final Map<UUID, Deque<Pt>> trails = new HashMap<>();

    private MotionTrail() {}

    public static void tick(MinecraftClient mc) {
        CosmeticsConfig c = CosmeticsConfig.get();
        if (mc.world == null || c.motion.style <= 0) { trails.clear(); return; }
        long now = System.currentTimeMillis();
        long life = (long) (LIFE_MS / Math.max(0.2f, c.motion.speed));
        for (AbstractClientPlayerEntity pl : mc.world.getPlayers()) {
            UUID id = pl.getUuid();
            if (pl.isInvisible() || pl.isSpectator() || !Cosmetics.appliesTo(pl.getGameProfile().getName())) {
                trails.remove(id);
                continue;
            }
            Deque<Pt> q = trails.computeIfAbsent(id, k -> new ArrayDeque<>());
            Pt last = q.peekLast();
            double dx = last == null ? 1 : pl.getX() - last.x, dy = last == null ? 1 : pl.getY() - last.y, dz = last == null ? 1 : pl.getZ() - last.z;
            if (dx * dx + dy * dy + dz * dz > MIN_STEP * MIN_STEP) {
                if (last != null && dx * dx + dy * dy + dz * dz > 16) q.clear();   // телепорт
                q.addLast(new Pt(pl.getX(), pl.getY(), pl.getZ(), now));
                if (q.size() > MAX_POINTS) q.pollFirst();
            }
            while (!q.isEmpty() && now - q.peekFirst().time > life) q.pollFirst();
        }
    }

    public static void render(WorldRenderContext ctx) {
        CosmeticsConfig c = CosmeticsConfig.get();
        if (trails.isEmpty() || c.motion.style <= 0) return;
        VertexConsumerProvider vcp = ctx.consumers();
        MatrixStack ms = ctx.matrixStack();
        if (vcp == null || ms == null) return;
        Vec3d cam = ctx.camera().getPos();
        Matrix4f m = ms.peek().getPositionMatrix();
        VertexConsumer vc = vcp.getBuffer(RenderLayer.getLightning());
        Tint p = Cosmetics.tint(c.motion);
        long now = System.currentTimeMillis();
        long life = (long) (LIFE_MS / Math.max(0.2f, c.motion.speed));
        float h = 1.25f * c.motion.scale;

        for (Deque<Pt> q : trails.values()) {
            if (q.size() < 2) continue;
            Pt prev = null;
            for (Pt pt : q) {
                if (prev != null) {
                    float a0 = 1f - (now - prev.time) / (float) life, a1 = 1f - (now - pt.time) / (float) life;
                    if (a1 > 0) {
                        a0 = Math.max(0, a0);
                        if (c.motion.style == 2) {
                            band(vc, m, prev, pt, cam, 0.25f * c.motion.scale, 0.40f * c.motion.scale, a0, a1, p);
                            band(vc, m, prev, pt, cam, 0.85f * c.motion.scale, 1.00f * c.motion.scale, a0, a1, p);
                        } else {
                            band(vc, m, prev, pt, cam, 0.15f * c.motion.scale, h, a0, a1, p);
                        }
                    }
                }
                prev = pt;
            }
        }
        HaloRenderer.flush(vcp);
    }

    /** Вертикальная полоса между двумя точками пути: снизу ярче, сверху прозрачнее. */
    private static void band(VertexConsumer vc, Matrix4f m, Pt a, Pt b, Vec3d cam, float y0, float y1, float a0, float a1, Tint p) {
        float ax = (float) (a.x - cam.x), ay = (float) (a.y - cam.y), az = (float) (a.z - cam.z);
        float bx = (float) (b.x - cam.x), by = (float) (b.y - cam.y), bz = (float) (b.z - cam.z);
        int bot0 = Palette.argb(p.light, (int) (110 * a0)), bot1 = Palette.argb(p.light, (int) (110 * a1));
        int top0 = Palette.argb(p.primary, (int) (25 * a0)), top1 = Palette.argb(p.primary, (int) (25 * a1));
        Draw.quad(vc, m,
                ax, ay + y0, az, bot0,
                bx, by + y0, bz, bot1,
                bx, by + y1, bz, top1,
                ax, ay + y1, az, top0);
    }
}
