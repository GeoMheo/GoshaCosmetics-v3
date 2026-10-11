package ru.goshacosmetics.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.goshacosmetics.CosmeticsConfig;

/** Показывать свой ник в виде от третьего лица. */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    @Inject(method = "hasLabel(Lnet/minecraft/entity/LivingEntity;D)Z", at = @At("RETURN"), cancellable = true, require = 0)
    private void gc$own(LivingEntity entity, double distance, CallbackInfoReturnable<Boolean> cir) {
        CosmeticsConfig c = CosmeticsConfig.get();
        MinecraftClient mc = MinecraftClient.getInstance();
        if (c.enabled && c.nametag.style > 0 && c.nametagShowOwn && entity == mc.player
                && !mc.options.getPerspective().isFirstPerson() && !mc.options.hudHidden) {
            cir.setReturnValue(true);
        }
    }
}
