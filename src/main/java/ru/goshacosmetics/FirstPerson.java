package ru.goshacosmetics;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;

/** Щит слева внизу экрана от первого лица (как предмет во второй руке). */
public final class FirstPerson {
    private FirstPerson() {}

    public static void renderShield(MatrixStack ms, VertexConsumerProvider.Immediate vcp, ClientPlayerEntity player, float td) {
        CosmeticsConfig c = CosmeticsConfig.get();
        if (!c.enabled || c.shield.style <= 0 || !c.shieldFirstPerson || player == null) return;
        if (c.shieldHideOffhand && !player.getOffHandStack().isEmpty()) return;
        if (!MinecraftClient.getInstance().options.getPerspective().isFirstPerson()) return;

        float t = (player.age + td);
        float sway = (float) Math.sin(t * 0.05f) * 0.01f;
        ms.push();
        ms.translate(-0.62f + c.shieldX * 0.5f, -0.48f + c.shieldY * 0.5f + sway, -1.0f);
        ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(18));
        ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-4));
        float s = 0.62f * c.shield.scale;
        ms.scale(s, s, s);
        ShieldModel.render(ms, vcp, c.shield, t);
        ms.pop();
        vcp.draw();
    }
}
