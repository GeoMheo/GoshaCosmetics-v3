package ru.goshacosmetics;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.MutableText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import net.minecraft.util.Language;

/** Стиль меню: тёмный фон, голубые акценты. */
final class Ui {
    static final int BG = 0xF00D1018;
    static final int SIDE = 0xF0090B11;
    static final int ROW = 0xFF151A24;
    static final int ROW_HOVER = 0xFF1C2433;
    static final int BOX = 0xFF0E1219;
    static final int BORDER = 0xFF232C3B;
    static final int ACCENT = 0xFF4FC3F7;
    static final int LIGHT = 0xFFB3E9FF;
    static final int MUTED = 0xFF8A93A6;
    static final int WHITE = 0xFFFFFFFF;
    static final int GREEN = 0xFF4CE07A;
    static final int RED = 0xFFF0535B;

    private Ui() {}

    static void border(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x, y, x + w, y + 1, color);
        ctx.fill(x, y + h - 1, x + w, y + h, color);
        ctx.fill(x, y, x + 1, y + h, color);
        ctx.fill(x + w - 1, y, x + w, y + h, color);
    }

    /** "#RRGGBB" → 0xFFRRGGBB; пусто/ошибка → fallback. */
    static int parseColor(String hex, int fallback) {
        if (hex == null) return fallback;
        String h = hex.trim();
        if (h.startsWith("#")) h = h.substring(1);
        if (!h.matches("[0-9a-fA-F]{6}")) return fallback;
        return 0xFF000000 | Integer.parseInt(h, 16);
    }

    /** Текст с плавным переходом цвета по буквам. */
    static MutableText gradient(String s, int from, int to, boolean bold) {
        MutableText out = Text.empty();
        int n = s.length();
        for (int i = 0; i < n; i++) {
            int c = Mesh.lerp(from, to, n <= 1 ? 0 : i / (float) (n - 1)) & 0xFFFFFF;
            out.append(Text.literal(String.valueOf(s.charAt(i))).styled(st -> st.withColor(c).withBold(bold)));
        }
        return out;
    }

    static Text brand() {
        return gradient("GoshaCosmetics", 0x7FDBFF, 0x2F6BFF, true);
    }

    static OrderedText fit(TextRenderer tr, Text t, int maxW) {
        if (maxW <= 0) return OrderedText.EMPTY;
        if (tr.getWidth(t) <= maxW) return t.asOrderedText();
        int ell = tr.getWidth("…");
        StringVisitable cut = tr.trimToWidth(t, Math.max(0, maxW - ell));
        return Language.getInstance().reorder(StringVisitable.concat(cut, StringVisitable.plain("…")));
    }
}
