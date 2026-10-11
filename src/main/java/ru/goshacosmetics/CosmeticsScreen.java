package ru.goshacosmetics;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Меню косметики: вкладки слева, настройки справа, предпросмотр, «GoshaCosmetics» справа вверху. */
public class CosmeticsScreen extends Screen {
    private enum Tab {
        HAT("♔", "Шапка"), HEAD("☺", "Голова"), BODY("♙", "Тело"), SHIELD("⛨", "Щит"), WINGS("❦", "Крылья"),
        NAMETAG("✎", "Ник"), CLOAK("⚑", "Плащ"), PROFILE("☻", "Профиль"), PRESETS("☰", "Пресеты"), SETTINGS("⚙", "Настройки");

        final String icon, title;
        Tab(String icon, String title) { this.icon = icon; this.title = title; }
    }

    private static Tab tab = Tab.WINGS;
    private static final int SIDE_W = 112, ROW_H = 18, GAP = 2;

    private int px, py, pw, ph, previewW, sideX, cx, cw, top, bottom, cy;
    private final List<ClickableWidget> rows = new ArrayList<>();
    private final List<Integer> baseY = new ArrayList<>();
    private int scroll, maxScroll;
    private String status = "";
    private boolean resetArmed;

    public CosmeticsScreen() {
        super(Text.literal("GoshaCosmetics"));
    }

    @Override
    protected void init() {
        pw = Math.min(width - 12, 640);
        ph = Math.min(height - 12, 360);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        previewW = pw >= 520 ? 120 : 0;
        sideX = px + previewW;
        cx = sideX + SIDE_W + 10;
        cw = px + pw - 10 - cx;
        top = py + 28;
        bottom = py + ph - 8;

        int y = py + 28;
        for (Tab t : Tab.values()) {
            if (t == Tab.PROFILE) y += 10;
            final Tab tt = t;
            addDrawableChild(new Widgets.Tab(sideX + 4, y, SIDE_W - 8, 18, tt.icon, tt.title, () -> tab == tt, b -> {
                CosmeticsConfig.save();
                tab = tt;
                scroll = 0;
                resetArmed = false;
                status = "";
                clearAndInit();
            }));
            y += 20;
        }

        rows.clear();
        baseY.clear();
        cy = top;
        build();
        maxScroll = Math.max(0, cy - bottom + 4);
        scroll = MathHelper.clamp(scroll, 0, maxScroll);
        applyScroll();
    }

    // ===================== содержимое вкладок =====================

    private void build() {
        CosmeticsConfig c = CosmeticsConfig.get();
        switch (tab) {
            case HAT -> slot("Шапка", c.hat, HatModel.NAMES, "Покачивание помпона", "Высота");
            case HEAD -> {
                slot("Ушки", c.ears, HeadModel.EARS, "Покачивание", null);
                slot("Рога", c.horns, HeadModel.HORNS, null, null);
                slot("Очки", c.glasses, HeadModel.GLASSES, null, "Высота");
                slot("Нимб", c.halo, HaloModel.NAMES, "Скорость вращения", "Высота");
            }
            case BODY -> {
                slot("Шарф", c.scarf, BodyModel.SCARF, "Колыхание", null);
                slot("Рюкзак", c.backpack, BodyModel.BACKPACK, null, null);
                slot("Аура", c.aura, Particles.AURA, "Плотность", null);
                slot("Следы шагов", c.steps, Footprints.NAMES, null, null);
                slot("Волна при приземлении", c.landing, new String[]{"Выкл", "Волна"}, null, null);
                slot("След при беге", c.trail, Particles.TRAIL, null, null);
            }
            case SHIELD -> {
                slot("Щит", c.shield, ShieldModel.NAMES, "Мерцание (энергощит)", null);
                header("Отображение");
                toggle("Виден от первого лица", () -> c.shieldFirstPerson, v -> c.shieldFirstPerson = v);
                toggle("Виден от третьего лица", () -> c.shieldThirdPerson, v -> c.shieldThirdPerson = v);
                toggle("Прятать, если в левой руке предмет", () -> c.shieldHideOffhand, v -> c.shieldHideOffhand = v);
                slider("Сдвиг от первого лица: X", -1f, 1f, () -> c.shieldX, v -> c.shieldX = v, "%.2f");
                slider("Сдвиг от первого лица: Y", -1f, 1f, () -> c.shieldY, v -> c.shieldY = v, "%.2f");
            }
            case WINGS -> slot("Крылья", c.wings, WingModel.NAMES, "Скорость взмахов", null);
            case NAMETAG -> {
                header("Ник над головой");
                cycle("Стиль", new String[]{"Выкл", "Градиент", "Переливание", "Лёд"}, () -> c.nametag.style, v -> c.nametag.style = v);
                toggle("Показывать свой ник (F5)", () -> c.nametagShowOwn, v -> c.nametagShowOwn = v);
                toggle("Значок ❄", () -> c.nametagIcon, v -> c.nametagIcon = v);
                toggle("Жирный шрифт", () -> c.nametagBold, v -> c.nametagBold = v);
                slider("Скорость переливания", 0.2f, 3f, () -> c.nametag.speed, v -> c.nametag.speed = v, "%.1f×");
                color("Цвет", () -> c.nametag.color, v -> c.nametag.color = v);
                if (client != null && client.player != null) {
                    label(Text.empty().append(Text.literal("Пример: ").formatted(Formatting.GRAY))
                            .append(c.nametag.style > 0 ? Cosmetics.styledName(client.player.getGameProfile().getName())
                                    : Text.literal(client.player.getGameProfile().getName())), Ui.WHITE);
                }
            }
            case CLOAK -> {
                header("Плащ");
                toggle("Плащ", () -> c.cloak.style > 0, v -> c.cloak.style = v ? 1 : 0);
                rowButton("Библиотека плащей", () -> Text.literal(c.cloakCustomFile ? "Свой файл"
                        : "◀ " + Cosmetics.CAPE_NAMES[Math.floorMod(c.cloakTexture, Cosmetics.CAPE_NAMES.length)] + " ▶"), () -> {
                    if (c.cloakCustomFile) c.cloakCustomFile = false;
                    else c.cloakTexture = (c.cloakTexture + 1) % Cosmetics.CAPE_NAMES.length;
                });
                rowButton("Свой файл (PNG 64×32)", () -> Text.literal("Выбрать").styled(s -> s.withColor(Ui.ACCENT & 0xFFFFFF)), () -> {
                    status = CustomCape.choose();
                });
                toggle("Обычный рендер Minecraft", () -> c.cloakVanillaRenderer, v -> c.cloakVanillaRenderer = v);
                if (!c.cloakVanillaRenderer) {
                    slider("Размер", 0.5f, 1.6f, () -> c.cloak.scale, v -> c.cloak.scale = v, "%.2f×");
                    slider("Скорость колыхания", 0.2f, 3f, () -> c.cloak.speed, v -> c.cloak.speed = v, "%.1f×");
                    color("Оттенок (A — без оттенка)", () -> c.cloak.color, v -> c.cloak.color = v);
                }
            }
            case PROFILE -> {
                header("Профиль");
                String nick = client != null && client.player != null ? client.player.getGameProfile().getName() : "—";
                label(Text.empty().append(Text.literal("Игрок: ").formatted(Formatting.GRAY)).append(Text.literal(nick).formatted(Formatting.WHITE, Formatting.BOLD)), Ui.WHITE);
                label(Text.empty().append(Text.literal("Активно предметов: ").formatted(Formatting.GRAY))
                        .append(Text.literal(String.valueOf(Cosmetics.activeCount())).styled(s -> s.withColor(Ui.ACCENT & 0xFFFFFF).withBold(true))), Ui.WHITE);
                toggle("Косметика включена", () -> c.enabled, v -> c.enabled = v);
                toggle("Показывать на других игроках", () -> c.showOnOthers, v -> c.showOnOthers = v);
                label(Text.literal("Косметику видишь только ты — это клиентский мод.").formatted(Formatting.DARK_GRAY), Ui.MUTED);
            }
            case PRESETS -> {
                header("Пресеты");
                for (int i = 1; i <= Presets.COUNT; i++) {
                    final int n = i;
                    boolean has = Presets.exists(n);
                    label(Text.empty().append(Text.literal("Пресет " + n + "  ").formatted(Formatting.WHITE, Formatting.BOLD))
                            .append(Text.literal(has ? "сохранён" : "пусто").formatted(has ? Formatting.AQUA : Formatting.DARK_GRAY)), Ui.WHITE);
                    int bw = (cw - 8) / 3;
                    Widgets.Small save = new Widgets.Small(cx, cy, bw, 16, Text.literal("Сохранить"), Ui.GREEN, b -> {
                        Presets.save(n); status = "Пресет " + n + " сохранён"; reopen();
                    });
                    Widgets.Small load = new Widgets.Small(cx + bw + 4, cy, bw, 16, Text.literal("Загрузить"), Ui.ACCENT, b -> {
                        status = Presets.load(n) ? "Пресет " + n + " загружен" : "Не удалось загрузить"; reopen();
                    });
                    Widgets.Small del = new Widgets.Small(cx + 2 * (bw + 4), cy, bw, 16, Text.literal("Удалить"), Ui.RED, b -> {
                        Presets.delete(n); status = "Пресет " + n + " удалён"; reopen();
                    });
                    load.active = has;
                    del.active = has;
                    add(save); add(load); add(del);
                    cy += 16 + 6;
                }
            }
            case SETTINGS -> {
                header("Настройки");
                cycle("Тема (цвет по умолчанию)", names(Palette.values()), () -> c.shade, v -> c.shade = v);
                toggle("Косметика включена", () -> c.enabled, v -> c.enabled = v);
                toggle("Показывать на других игроках", () -> c.showOnOthers, v -> c.showOnOthers = v);
                toggle("Надпись GoshaCosmetics на экране", () -> c.watermark, v -> c.watermark = v);
                rowButton("Сбросить все настройки", () -> Text.literal(resetArmed ? "Точно?" : "Сбросить")
                        .styled(s -> s.withColor(Ui.RED & 0xFFFFFF)), () -> {
                    if (!resetArmed) { resetArmed = true; return; }
                    resetArmed = false;
                    CosmeticsConfig.reset();
                    status = "Настройки сброшены";
                });
                label(Text.literal("Меню: K · клавиши меняются в «Управлении»").formatted(Formatting.DARK_GRAY), Ui.MUTED);
            }
        }
    }

    private static String[] names(Palette[] v) {
        String[] out = new String[v.length];
        for (int i = 0; i < v.length; i++) out[i] = v[i].title;
        return out;
    }

    /** Типовой блок предмета: стиль, размер, цвет (+скорость, +высота). */
    private void slot(String title, CosmeticsConfig.Slot s, String[] styles, String speedLabel, String offsetLabel) {
        header(title);
        cycle("Стиль", styles, () -> s.style, v -> s.style = v);
        slider("Размер", 0.5f, 2f, () -> s.scale, v -> s.scale = v, "%.2f×");
        if (speedLabel != null) slider(speedLabel, 0.2f, 3f, () -> s.speed, v -> s.speed = v, "%.1f×");
        if (offsetLabel != null) slider(offsetLabel, -1f, 1f, () -> s.offset, v -> s.offset = v, "%.2f");
        color("Цвет", () -> s.color, v -> s.color = v);
    }

    // ===================== строки =====================

    private void add(ClickableWidget w) {
        addDrawableChild(w);
        rows.add(w);
        baseY.add(w.getY());
    }

    private void reopen() {
        clearAndInit();
    }

    private void header(String title) {
        if (cy > top) cy += 4;
        add(new Widgets.Label(cx, cy, cw, 14, Text.literal(title).styled(s -> s.withBold(true)), Ui.ACCENT, true));
        cy += 16;
    }

    private void label(Text text, int color) {
        add(new Widgets.Label(cx, cy, cw, 14, text, color, false));
        cy += 16;
    }

    private void toggle(String label, Supplier<Boolean> get, Consumer<Boolean> set) {
        add(new Widgets.Row(cx, cy, cw, ROW_H, label,
                () -> get.get() ? Text.literal("ВКЛ").styled(s -> s.withColor(Ui.GREEN & 0xFFFFFF))
                        : Text.literal("ВЫКЛ").styled(s -> s.withColor(Ui.RED & 0xFFFFFF)),
                b -> { set.accept(!get.get()); CosmeticsConfig.save(); }));
        cy += ROW_H + GAP;
    }

    private void cycle(String label, String[] names, Supplier<Integer> get, Consumer<Integer> set) {
        add(new Widgets.Row(cx, cy, cw, ROW_H, label, () -> {
            int i = Math.floorMod(get.get(), names.length);
            return Text.literal("◀ " + names[i] + " ▶").styled(s -> s.withColor(i == 0 && names[0].equals("Выкл")
                    ? Ui.MUTED & 0xFFFFFF : Ui.LIGHT & 0xFFFFFF));
        }, b -> {
            set.accept((Math.floorMod(get.get(), names.length) + 1) % names.length);
            CosmeticsConfig.save();
        }));
        cy += ROW_H + GAP;
    }

    private void rowButton(String label, Supplier<Text> value, Runnable action) {
        add(new Widgets.Row(cx, cy, cw, ROW_H, label, value, b -> { action.run(); CosmeticsConfig.save(); }));
        cy += ROW_H + GAP;
    }

    private void slider(String label, float min, float max, Supplier<Float> get, Consumer<Float> set, String fmt) {
        add(new Widgets.Slider(cx, cy, cw, ROW_H, label, min, max, get.get(), fmt, set));
        cy += ROW_H + GAP;
    }

    private void color(String label, Supplier<String> get, Consumer<String> set) {
        add(new Widgets.Label(cx, cy, cw, 12, Text.literal(label), Ui.MUTED, false));
        cy += 12;
        for (ClickableWidget w : Widgets.colorPicker(cx + 7, cy, get, set)) add(w);
        cy += 18 + GAP;
    }

    private void applyScroll() {
        for (int i = 0; i < rows.size(); i++) {
            ClickableWidget w = rows.get(i);
            int y = baseY.get(i) - scroll;
            w.setY(y);
            boolean vis = y >= top - 1 && y + w.getHeight() <= bottom + 1;
            w.visible = vis;
        }
    }

    // ===================== отрисовка =====================

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (mouseX >= cx && maxScroll > 0) {
            scroll = MathHelper.clamp(scroll - (int) (verticalAmount * 20), 0, maxScroll);
            applyScroll();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.renderBackground(ctx, mouseX, mouseY, delta);
        ctx.fill(px, py, px + pw, py + ph, Ui.BG);
        Ui.border(ctx, px, py, pw, ph, Ui.BORDER);
        ctx.fill(sideX, py + 22, sideX + SIDE_W, py + ph - 1, Ui.SIDE);
        if (previewW > 0) ctx.fill(px + 1, py + 22, px + previewW, py + ph - 1, 0xF0080A0F);
        // верхняя полоса
        ctx.fill(px + 1, py + 1, px + pw - 1, py + 22, 0xFF0A0D14);
        ctx.fill(px + 1, py + 22, px + pw - 1, py + 23, Ui.ACCENT);
        ctx.drawTextWithShadow(textRenderer, Text.literal(tab.icon + "  " + tab.title).formatted(Formatting.BOLD),
                px + 8, py + 8, Ui.WHITE);
        Text brand = Ui.brand();
        ctx.drawTextWithShadow(textRenderer, brand, px + pw - 8 - textRenderer.getWidth(brand), py + 8, Ui.WHITE);
        // разделитель вкладок
        int sepY = py + 28 + 7 * 20 + 4;
        ctx.fill(sideX + 10, sepY, sideX + SIDE_W - 10, sepY + 1, Ui.BORDER);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        if (client != null && client.player != null) {
            if (previewW > 0) {
                drawPreview(ctx, px + 4, py + 30, px + previewW - 4, py + ph - 14, mouseX, mouseY);
            } else if (tab == Tab.PROFILE) {
                drawPreview(ctx, cx, cy - scroll + 4, cx + cw, bottom, mouseX, mouseY);
            }
        }
        if (!status.isEmpty()) {
            ctx.drawTextWithShadow(textRenderer, Text.literal(status), cx, py + ph - 6 - 9 + 12 > height ? py + ph - 18 : py + ph + 3, Ui.LIGHT);
        }
        if (maxScroll > 0) {
            int bh = Math.max(14, (bottom - top) * (bottom - top) / (bottom - top + maxScroll));
            int by = top + (int) ((bottom - top - bh) * (scroll / (float) maxScroll));
            ctx.fill(px + pw - 5, by, px + pw - 3, by + bh, Ui.BORDER);
        }
    }

    private void drawPreview(DrawContext ctx, int x1, int y1, int x2, int y2, int mouseX, int mouseY) {
        if (y2 - y1 < 30) return;
        int size = Math.max(16, Math.min((x2 - x1) / 2, (y2 - y1) / 3));
        Cosmetics.preview = true;
        try {
            InventoryScreen.drawEntity(ctx, x1, y1, x2, y2, size, 0.0625f, mouseX, mouseY, client.player);
        } finally {
            Cosmetics.preview = false;
        }
    }

    @Override
    public void removed() {
        CosmeticsConfig.save();
        super.removed();
    }

    @Override
    public boolean shouldPause() { return false; }
}
