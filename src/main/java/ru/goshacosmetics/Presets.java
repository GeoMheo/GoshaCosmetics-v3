package ru.goshacosmetics;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** 5 слотов пресетов: config/goshacosmetics/preset1.json … preset5.json */
public final class Presets {
    public static final int COUNT = 5;

    private Presets() {}

    private static Path file(int i) { return CosmeticsConfig.dir().resolve("preset" + i + ".json"); }

    public static boolean exists(int i) { return Files.exists(file(i)); }

    public static void save(int i) {
        try {
            Files.createDirectories(CosmeticsConfig.dir());
            Files.writeString(file(i), CosmeticsConfig.GSON.toJson(CosmeticsConfig.get()), StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.err.println("[GoshaCosmetics] preset save failed: " + e);
        }
    }

    public static boolean load(int i) {
        try {
            CosmeticsConfig c = CosmeticsConfig.GSON.fromJson(Files.readString(file(i), StandardCharsets.UTF_8), CosmeticsConfig.class);
            if (c == null) return false;
            CosmeticsConfig.set(c);
            CosmeticsConfig.save();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static void delete(int i) {
        try { Files.deleteIfExists(file(i)); } catch (Exception ignored) { }
    }
}
