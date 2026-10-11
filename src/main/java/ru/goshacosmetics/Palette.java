package ru.goshacosmetics;

/** Голубые оттенки косметики: основной, светлый, глубокий (RGB). */
public enum Palette {
    ICE("Лёд", 0x9BE7FF, 0xE8FBFF, 0x4FC3F7),
    SKY("Небо", 0x5AB4FF, 0xA8DCFF, 0x1E88E5),
    OCEAN("Океан", 0x2F6BFF, 0x6FD8FF, 0x0D47A1);

    public final String title;
    public final int primary, light, deep;

    Palette(String title, int primary, int light, int deep) {
        this.title = title;
        this.primary = primary;
        this.light = light;
        this.deep = deep;
    }

    public Tint tint() {
        return new Tint(primary, light, deep);
    }

    public static int argb(int rgb, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (rgb & 0xFFFFFF);
    }
}
