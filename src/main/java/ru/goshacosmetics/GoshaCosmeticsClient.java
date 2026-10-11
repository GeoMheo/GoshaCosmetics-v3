package ru.goshacosmetics;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

public class GoshaCosmeticsClient implements ClientModInitializer {
    private static KeyBinding menuKey;
    private static KeyBinding toggleKey;

    @Override
    public void onInitializeClient() {
        CosmeticsConfig.load();

        menuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.goshacosmetics.menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, "category.goshacosmetics"));
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.goshacosmetics.toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, "category.goshacosmetics"));

        LivingEntityFeatureRendererRegistrationCallback.EVENT.register((entityType, entityRenderer, registrationHelper, context) -> {
            if (entityRenderer instanceof PlayerEntityRenderer playerRenderer) {
                registrationHelper.register(new CosmeticsFeature(playerRenderer));
            }
        });

        WorldRenderEvents.AFTER_ENTITIES.register(Footprints::render);

        // «GoshaCosmetics» справа вверху экрана
        HudRenderCallback.EVENT.register((ctx, tickCounter) -> {
            net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
            if (!CosmeticsConfig.get().watermark || mc.options.hudHidden) return;
            Text brand = Ui.brand();
            ctx.drawTextWithShadow(mc.textRenderer, brand, ctx.getScaledWindowWidth() - mc.textRenderer.getWidth(brand) - 4, 4, 0xFFFFFFFF);
        });
        WorldRenderEvents.AFTER_ENTITIES.register(HaloRenderer::render);
        WorldRenderEvents.AFTER_ENTITIES.register(MotionTrail::render);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (menuKey.wasPressed()) {
                if (client.currentScreen == null) client.setScreen(new CosmeticsScreen());
            }
            while (toggleKey.wasPressed()) {
                CosmeticsConfig c = CosmeticsConfig.get();
                c.enabled = !c.enabled;
                CosmeticsConfig.save();
                client.inGameHud.setOverlayMessage(Text.literal("Косметика: " + (c.enabled ? "вкл" : "выкл"))
                        .formatted(c.enabled ? Formatting.AQUA : Formatting.GRAY), false);
            }
            Footprints.tick(client);
            MotionTrail.tick(client);
            Particles.tick(client);
        });
    }
}
