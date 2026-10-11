package ru.goshacosmetics;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/** Общее: кому рисовать, цвета, текстуры плаща, ник. */
public final class Cosmetics {
    public static final Identifier WHITE = Identifier.of("goshacosmetics", "textures/white.png");
    public static final String[] CAPE_NAMES = {"Лёд", "Звёздная ночь", "Волны", "Кристаллы", "Сияние"};
    private static final String[] CAPE_FILES = {"ice", "night", "waves", "crystal", "aurora"};

    /** true, пока рисуется предпросмотр в меню. */
    public static boolean preview = false;

    private Cosmetics() {}

    public static boolean appliesTo(String name) {
        CosmeticsConfig c = CosmeticsConfig.get();
        if (!c.enabled || name == null) return false;
        if (c.showOnOthers) return true;
        MinecraftClient mc = MinecraftClient.getInstance();
        return mc.player != null && mc.player.getGameProfile().getName().equalsIgnoreCase(name);
    }

    /** Цвет предмета: свой #RRGGBB или тема. */
    public static Tint tint(CosmeticsConfig.Slot slot) {
        String c = slot == null ? "" : slot.color;
        int rgb = Ui.parseColor(c, -1);
        return rgb == -1 ? CosmeticsConfig.get().palette().tint() : Tint.of(rgb & 0xFFFFFF);
    }

    /** Время в тиках (для анимаций вне рендера сущности). */
    public static float time() {
        return (System.currentTimeMillis() % 3_600_000L) / 50f;
    }

    public static Identifier cloakTexture() {
        CosmeticsConfig c = CosmeticsConfig.get();
        if (c.cloakCustomFile && CustomCape.isLoaded()) return CustomCape.ID;
        int i = Math.floorMod(c.cloakTexture, CAPE_FILES.length);
        return Identifier.of("goshacosmetics", "textures/capes/" + CAPE_FILES[i] + ".png");
    }

    public static int activeCount() {
        CosmeticsConfig c = CosmeticsConfig.get();
        CosmeticsConfig.Slot[] all = {c.hat, c.ears, c.horns, c.glasses, c.halo, c.scarf, c.backpack, c.aura, c.steps,
                c.landing, c.trail, c.shield, c.wings, c.nametag, c.cloak};
        int n = 0;
        for (CosmeticsConfig.Slot s : all) if (s.style > 0) n++;
        return n;
    }

    /** Ник с градиентом / переливанием и значком. */
    public static Text styledName(String name) {
        CosmeticsConfig c = CosmeticsConfig.get();
        Tint t = tint(c.nametag);
        MutableText out = Text.empty();
        if (c.nametagIcon) out.append(Text.literal("❄ ").styled(s -> s.withColor(t.light)));
        int n = name.length();
        float shift = c.nametag.style == 2 ? time() * 0.03f * c.nametag.speed : 0f;
        for (int i = 0; i < n; i++) {
            float f = n <= 1 ? 0 : i / (float) (n - 1);
            int color;
            if (c.nametag.style == 3) {
                color = Mesh.lerp(t.light, t.primary, f) & 0xFFFFFF;
            } else {
                float k = shift == 0 ? f : (float) (0.5 + 0.5 * Math.sin((f - shift) * Math.PI * 2));
                color = Mesh.lerp(t.primary, t.light, k) & 0xFFFFFF;
            }
            final int col = color;
            out.append(Text.literal(String.valueOf(name.charAt(i))).styled(s -> s.withColor(col).withBold(c.nametagBold)));
        }
        if (c.nametagIcon) out.append(Text.literal(" ❄").styled(s -> s.withColor(t.light)));
        return out;
    }
}
