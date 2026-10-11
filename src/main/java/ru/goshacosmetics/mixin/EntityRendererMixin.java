package ru.goshacosmetics.mixin;

import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.goshacosmetics.Cosmetics;
import ru.goshacosmetics.CosmeticsConfig;

/** Красивый ник над головой. */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {
    @Inject(method = "getDisplayName", at = @At("RETURN"), cancellable = true, require = 0)
    private void gc$name(Entity entity, CallbackInfoReturnable<Text> cir) {
        if (!(entity instanceof PlayerEntity pl)) return;
        if (CosmeticsConfig.get().nametag.style <= 0) return;
        String name = pl.getGameProfile().getName();
        if (!Cosmetics.appliesTo(name)) return;
        cir.setReturnValue(Cosmetics.styledName(name));
    }
}
