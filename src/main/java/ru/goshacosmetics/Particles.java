package ru.goshacosmetics;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;

import java.util.Random;

/** Аура вокруг игрока и след при беге. */
public final class Particles {
    static final String[] AURA = {"Выкл", "Искры", "Снежинки", "Светлячки"};
    static final String[] TRAIL = {"Выкл", "Искры", "Снежинки"};
    private static final Random RND = new Random();

    private Particles() {}

    public static void tick(MinecraftClient mc) {
        if (mc.world == null || mc.isPaused()) return;
        CosmeticsConfig c = CosmeticsConfig.get();
        if (c.aura.style <= 0 && c.trail.style <= 0) return;
        Tint at = Cosmetics.tint(c.aura), tt = Cosmetics.tint(c.trail);

        for (AbstractClientPlayerEntity pl : mc.world.getPlayers()) {
            if (pl.isInvisible() || pl.isSpectator() || !Cosmetics.appliesTo(pl.getGameProfile().getName())) continue;

            if (c.aura.style > 0 && RND.nextFloat() < 0.3f * c.aura.speed) {
                double a = RND.nextDouble() * Math.PI * 2, r = (0.45 + RND.nextDouble() * 0.25) * c.aura.scale;
                double x = pl.getX() + Math.cos(a) * r, y = pl.getY() + RND.nextDouble() * 1.8, z = pl.getZ() + Math.sin(a) * r;
                switch (c.aura.style) {
                    case 2 -> mc.world.addParticle(ParticleTypes.SNOWFLAKE, x, y, z, 0, -0.01, 0);
                    case 3 -> mc.world.addParticle(ParticleTypes.END_ROD, x, y, z, 0, 0.01, 0);
                    default -> mc.world.addParticle(new DustParticleEffect(RND.nextBoolean() ? at.light : at.primary,
                            Math.min(4f, 0.55f * c.aura.scale)), x, y, z, 0, 0.02, 0);
                }
            }
            if (c.trail.style > 0 && pl.isSprinting() && pl.isOnGround()) {
                double yr = Math.toRadians(pl.getYaw());
                double bx = Math.sin(yr) * 0.3, bz = -Math.cos(yr) * 0.3;
                for (int i = 0; i < 2; i++) {
                    double x = pl.getX() + bx + (RND.nextDouble() - 0.5) * 0.3, y = pl.getY() + 0.1, z = pl.getZ() + bz + (RND.nextDouble() - 0.5) * 0.3;
                    if (c.trail.style == 2) mc.world.addParticle(ParticleTypes.SNOWFLAKE, x, y, z, 0, 0.01, 0);
                    else mc.world.addParticle(new DustParticleEffect(i == 0 ? tt.primary : tt.light,
                            Math.min(4f, 0.8f * c.trail.scale)), x, y, z, 0, 0, 0);
                }
            }
        }
    }
}
