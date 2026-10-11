package ru.goshacosmetics;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Нимб не прикреплён к голове: он «плывёт» за ней на пружине — отстаёт при движении,
 * слегка раскачивается и наклоняется против направления движения, но не повторяет повороты головы.
 */
public final class HaloRenderer {
    private static final double K = 90, DAMP = 13;    // жёсткость и затухание пружины

    private static final class State {
        double x, y, z, vx, vy, vz;
        long last;
    }

    private static final Map<UUID, State> states = new HashMap<>();

    private HaloRenderer() {}

    /** Рисуем свои слои сразу, пока матрицы камеры верные (иначе возможны «осколки» на экране). */
    static void flush(VertexConsumerProvider vcp) {
        if (vcp instanceof VertexConsumerProvider.Immediate imm) {
            imm.draw(RenderLayer.getEntityCutoutNoCull(Cosmetics.WHITE));
            imm.draw(RenderLayer.getLightning());
        }
    }

    public static void render(WorldRenderContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        CosmeticsConfig c = CosmeticsConfig.get();
        if (mc.world == null || c.halo.style <= 0) { states.clear(); return; }
        VertexConsumerProvider vcp = ctx.consumers();
        MatrixStack ms = ctx.matrixStack();
        if (vcp == null || ms == null) return;

        float td = ctx.tickCounter().getTickDelta(false);
        Vec3d cam = ctx.camera().getPos();
        long now = System.nanoTime();
        Tint p = Cosmetics.tint(c.halo);

        for (AbstractClientPlayerEntity pl : mc.world.getPlayers()) {
            UUID id = pl.getUuid();
            if (pl.isInvisible() || pl.isSpectator() || !Cosmetics.appliesTo(pl.getGameProfile().getName())) {
                states.remove(id);
                continue;
            }
            Vec3d base = pl.getLerpedPos(td);
            double tx = base.x, ty = base.y + pl.getEyeHeight(pl.getPose()) + 0.45 + c.halo.offset * 0.5, tz = base.z;

            State s = states.get(id);
            if (s == null) {
                s = new State();
                s.x = tx; s.y = ty; s.z = tz; s.last = now;
                states.put(id, s);
            }
            double dt = Math.min(0.05, (now - s.last) / 1e9);
            s.last = now;
            double dx = tx - s.x, dy = ty - s.y, dz = tz - s.z;
            if (dx * dx + dy * dy + dz * dz > 9) {          // телепорт — без «полёта» через полкарты
                s.x = tx; s.y = ty; s.z = tz; s.vx = s.vy = s.vz = 0;
            } else {
                s.vx += (dx * K - s.vx * DAMP) * dt;
                s.vy += (dy * K - s.vy * DAMP) * dt;
                s.vz += (dz * K - s.vz * DAMP) * dt;
                s.x += s.vx * dt; s.y += s.vy * dt; s.z += s.vz * dt;
            }

            boolean firstPersonSelf = pl == mc.getCameraEntity() && mc.options.getPerspective().isFirstPerson();
            if (firstPersonSelf) continue;

            float t = (pl.age + td) * c.halo.speed;
            float bob = (float) Math.sin(t * 0.08f) * 0.03f;
            float tiltX = MathHelper.clamp((float) (s.vz * 0.12), -0.45f, 0.45f);
            float tiltZ = MathHelper.clamp((float) (-s.vx * 0.12), -0.45f, 0.45f);

            ms.push();
            ms.translate(s.x - cam.x, s.y - cam.y + bob, s.z - cam.z);
            ms.multiply(RotationAxis.POSITIVE_X.rotation(tiltX));
            ms.multiply(RotationAxis.POSITIVE_Z.rotation(tiltZ));
            ms.scale(c.halo.scale, c.halo.scale, c.halo.scale);
            HaloModel.render(ms, vcp, p, t, c.halo.style, 1f);
            ms.pop();
        }
        flush(vcp);
    }
}
