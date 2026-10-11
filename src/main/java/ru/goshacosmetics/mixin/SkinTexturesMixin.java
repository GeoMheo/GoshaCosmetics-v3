package ru.goshacosmetics.mixin;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.SkinTextures;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.goshacosmetics.Cosmetics;
import ru.goshacosmetics.CosmeticsConfig;

/** Плащ: обычный рендер Minecraft с нашей текстурой, либо скрываем ванильный (рисуем свой). */
@Mixin(AbstractClientPlayerEntity.class)
public abstract class SkinTexturesMixin {
    @Inject(method = "getSkinTextures", at = @At("RETURN"), cancellable = true)
    private void gc$cape(CallbackInfoReturnable<SkinTextures> cir) {
        CosmeticsConfig c = CosmeticsConfig.get();
        if (c.cloak.style <= 0) return;
        AbstractClientPlayerEntity self = (AbstractClientPlayerEntity) (Object) this;
        if (!Cosmetics.appliesTo(self.getGameProfile().getName())) return;
        SkinTextures t = cir.getReturnValue();
        if (c.cloakVanillaRenderer) {
            cir.setReturnValue(new SkinTextures(t.texture(), t.textureUrl(), Cosmetics.cloakTexture(), Cosmetics.cloakTexture(), t.model(), t.secure()));
        } else {
            cir.setReturnValue(new SkinTextures(t.texture(), t.textureUrl(), null, t.elytraTexture(), t.model(), t.secure()));
        }
    }
}
