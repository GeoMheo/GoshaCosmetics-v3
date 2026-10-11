package ru.goshacosmetics;

/** Три оттенка одного цвета: основной, светлый, глубокий (RGB без альфы). */
public final class Tint {
    public final int primary, light, deep;

    public Tint(int primary, int light, int deep) {
        this.primary = primary & 0xFFFFFF;
        this.light = light & 0xFFFFFF;
        this.deep = deep & 0xFFFFFF;
    }

    public static Tint of(int rgb) {
        return new Tint(rgb, Mesh.lerp(rgb, 0xFFFFFF, 0.55f), Mesh.lerp(rgb, 0x000000, 0.45f));
    }
}
