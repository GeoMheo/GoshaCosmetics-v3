package ru.goshacosmetics;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Виджеты меню в стиле «строка: подпись слева, значение справа». */
final class Widgets {
    private Widgets() {}

    static TextRenderer tr() { return MinecraftClient.getInstance().textRenderer; }

    /** Вкладка слева: иконка + название, выбранная подсвечена. */
    static final class Tab extends ButtonWidget {
        private final String icon, title;
        private final BooleanSupplier selected;

        Tab(int x, int y, int w, int h, String icon, String title, BooleanSupplier selected, PressAction p) {
            super(x, y, w, h, Text.literal(title), p, DEFAULT_NARRATION_SUPPLIER);
            this.icon = icon; this.title = title; this.selected = selected;
        }

        @Override
        protected void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
            int x = getX(), y = getY(), w = getWidth(), h = getHeight();
            boolean sel = selected.getAsBoolean();
            if (sel) {
                ctx.fill(x, y, x + w, y + h, 0xFF1A2130);
                ctx.fill(x, y, x + 2, y + h, Ui.ACCENT);
            } else if (isHovered()) {
                ctx.fill(x, y, x + w, y + h, 0xFF131823);
            }
            int c = sel ? Ui.WHITE : isHovered() ? 0xFFCDD3DE : Ui.MUTED;
            ctx.drawTextWithShadow(tr(), Text.literal(icon), x + 8, y + (h - 8) / 2, sel ? Ui.ACCENT : c);
            ctx.drawTextWithShadow(tr(), Text.literal(title), x + 22, y + (h - 8) / 2, c);
        }
    }

    /** Строка с кнопкой-значением справа (ВКЛ/ВЫКЛ, ◀ стиль ▶, «Выбрать» и т.п.). */
    static final class Row extends ButtonWidget {
        private final String label;
        private final Supplier<Text> value;

        Row(int x, int y, int w, int h, String label, Supplier<Text> value, PressAction p) {
            super(x, y, w, h, Text.literal(label), p, DEFAULT_NARRATION_SUPPLIER);
            this.label = label; this.value = value;
        }

        @Override
        protected void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
            int x = getX(), y = getY(), w = getWidth(), h = getHeight();
            ctx.fill(x, y, x + w, y + h, isHovered() ? Ui.ROW_HOVER : Ui.ROW);
            Text v = value.get();
            int bw = Math.max(70, tr().getWidth(v) + 14);
            int bx = x + w - bw - 3;
            ctx.drawTextWithShadow(tr(), Ui.fit(tr(), Text.literal(label), bx - x - 12), x + 7, y + (h - 8) / 2, Ui.WHITE);
            ctx.fill(bx, y + 2, bx + bw, y + h - 2, Ui.BOX);
            Ui.border(ctx, bx, y + 2, bw, h - 4, isHovered() ? Ui.ACCENT : Ui.BORDER);
            ctx.drawCenteredTextWithShadow(tr(), v, bx + bw / 2, y + (h - 8) / 2, Ui.WHITE);
        }
    }

    /** Небольшая кнопка с цветной подсветкой. */
    static final class Small extends ButtonWidget {
        private final int accent;

        Small(int x, int y, int w, int h, Text msg, int accent, PressAction p) {
            super(x, y, w, h, msg, p, DEFAULT_NARRATION_SUPPLIER);
            this.accent = accent;
        }

        @Override
        protected void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
            int x = getX(), y = getY(), w = getWidth(), h = getHeight();
            ctx.fill(x, y, x + w, y + h, !active ? 0xFF10131A : isHovered() ? Ui.ROW_HOVER : Ui.BOX);
            Ui.border(ctx, x, y, w, h, isHovered() && active ? accent : Ui.BORDER);
            ctx.drawCenteredTextWithShadow(tr(), getMessage(), x + w / 2, y + (h - 8) / 2, active ? accent : 0xFF555B66);
        }
    }

    /** Подпись / заголовок раздела. */
    static final class Label extends ClickableWidget {
        private final int color;
        private final boolean header;

        Label(int x, int y, int w, int h, Text text, int color, boolean header) {
            super(x, y, w, h, text);
            this.color = color; this.header = header;
            this.active = false;
        }

        @Override
        protected void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
            int x = getX(), y = getY();
            ctx.drawTextWithShadow(tr(), Ui.fit(tr(), getMessage(), getWidth()), x + (header ? 2 : 7), y + (getHeight() - 8) / 2, color);
            if (header) ctx.fill(x + 2, y + getHeight() - 1, x + getWidth(), y + getHeight(), Ui.BORDER);
        }

        @Override
        protected void appendClickableNarrations(NarrationMessageBuilder builder) { }
    }

    /** Ползунок: подпись слева, полоса и значение справа. */
    static final class Slider extends SliderWidget {
        private final String label, fmt;
        private final float min, max;
        private final Consumer<Float> set;

        Slider(int x, int y, int w, int h, String label, float min, float max, float val, String fmt, Consumer<Float> set) {
            super(x, y, w, h, Text.empty(), Math.max(0, Math.min(1, (val - min) / (max - min))));
            this.label = label; this.min = min; this.max = max; this.fmt = fmt; this.set = set;
            updateMessage();
        }

        private float cur() { return min + (float) value * (max - min); }

        @Override
        protected void updateMessage() { setMessage(Text.literal(String.format(Locale.ROOT, fmt, cur()))); }

        @Override
        protected void applyValue() { set.accept(cur()); }

        @Override
        public void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
            int x = getX(), y = getY(), w = getWidth(), h = getHeight();
            ctx.fill(x, y, x + w, y + h, isHovered() ? Ui.ROW_HOVER : Ui.ROW);
            int bw = Math.min(150, w / 2);
            int bx = x + w - bw - 3;
            ctx.drawTextWithShadow(tr(), Ui.fit(tr(), Text.literal(label), bx - x - 50), x + 7, y + (h - 8) / 2, Ui.WHITE);
            Text v = getMessage();
            ctx.drawTextWithShadow(tr(), v, bx - 6 - tr().getWidth(v), y + (h - 8) / 2, Ui.LIGHT);
            int ty = y + h / 2 - 1;
            ctx.fill(bx, ty, bx + bw, ty + 3, Ui.BOX);
            int kx = bx + (int) (value * (bw - 6));
            ctx.fill(bx, ty, kx + 3, ty + 3, Ui.ACCENT);
            ctx.fill(kx, y + 3, kx + 6, y + h - 3, isHovered() ? Ui.WHITE : Ui.LIGHT);
        }
    }

    /** Палитра + поле #RRGGBB. Пустой цвет = «A» (тема). */
    static List<ClickableWidget> colorPicker(int x, int y, Supplier<String> get, Consumer<String> set) {
        String[] pal = {"", "#4FC3F7", "#9BE7FF", "#2F6BFF", "#0D47A1", "#7C4DFF", "#E040FB", "#FF4081", "#FF5252",
                "#FFAB40", "#FFD740", "#69F0AE", "#FFFFFF"};
        List<ClickableWidget> out = new ArrayList<>();
        TextFieldWidget hex = new TextFieldWidget(tr(), x, y + 1, 62, 14, Text.literal("hex"));
        int sx = x;
        for (String p : pal) {
            out.add(new Swatch(sx, y + 1, 14, p, () -> norm(get.get()).equalsIgnoreCase(p), b -> {
                set.accept(p);
                hex.setText(p);
            }));
            sx += 17;
        }
        hex.setX(sx + 4);
        hex.setMaxLength(7);
        hex.setText(norm(get.get()));
        hex.setChangedListener(s -> {
            String t = s.trim();
            if (t.isEmpty()) set.accept("");
            else if (t.matches("#?[0-9a-fA-F]{6}")) set.accept((t.startsWith("#") ? t : "#" + t).toUpperCase(Locale.ROOT));
        });
        out.add(hex);
        return out;
    }

    private static String norm(String s) { return s == null ? "" : s.trim(); }

    static final class Swatch extends ButtonWidget {
        private final String color;
        private final Supplier<Boolean> selected;

        Swatch(int x, int y, int size, String color, Supplier<Boolean> selected, PressAction onPress) {
            super(x, y, size, size, Text.literal(color.isEmpty() ? "Авто" : color), onPress, DEFAULT_NARRATION_SUPPLIER);
            this.color = color;
            this.selected = selected;
            setTooltip(Tooltip.of(Text.literal(color.isEmpty() ? "Авто — цвет темы" : color)));
        }

        @Override
        protected void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
            int x = getX(), y = getY(), w = getWidth(), h = getHeight();
            boolean sel = Boolean.TRUE.equals(selected.get());
            if (color.isEmpty()) {
                ctx.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF1C2433);
                ctx.drawCenteredTextWithShadow(tr(), Text.literal("A"), x + w / 2, y + (h - 8) / 2, 0xFFAAAAAA);
            } else {
                ctx.fill(x + 1, y + 1, x + w - 1, y + h - 1, Ui.parseColor(color, 0xFF888888));
            }
            Ui.border(ctx, x, y, w, h, sel ? 0xFFFFFFFF : isHovered() ? 0xFFAAAAAA : Ui.BORDER);
        }
    }
}
