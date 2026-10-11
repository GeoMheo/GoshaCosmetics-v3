package ru.goshacosmetics.mixin;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.goshacosmetics.FirstPerson;

/** Щит, видимый от первого лица. */
@Mixin(HeldItemRenderer.class)
public abstract class HeldItemRendererMixin {
    @Inject(method = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;Lnet/minecraft/client/network/ClientPlayerEntity;I)V",
            at = @At("TAIL"), require = 0)
    private void gc$shield(float tickDelta, MatrixStack matrices, VertexConsumerProvider.Immediate vcp,
                           ClientPlayerEntity player, int light, CallbackInfo ci) {
        FirstPerson.renderShield(matrices, vcp, player, tickDelta);
    }
}
