package ru.goshacosmetics;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** config/goshacosmetics.json — вся косметика и её настройки. */
public class CosmeticsConfig {
    /** Настройки одного предмета: стиль (0 — выкл), размер, цвет (#RRGGBB или пусто = тема), скорость, сдвиг. */
    public static class Slot {
        public int style;
        public float scale = 1f;
        public String color = "";
        public float speed = 1f;
        public float offset = 0f;

        public Slot() {}
        public Slot(int style) { this.style = style; }
    }

    public boolean enabled = true;
    public boolean showOnOthers = false;
    public boolean watermark = true;
    public int shade = 0;

    public Slot hat = new Slot(0);
    public Slot ears = new Slot(0);
    public Slot horns = new Slot(0);
    public Slot glasses = new Slot(0);
    public Slot halo = new Slot(1);

    public Slot scarf = new Slot(0);
    public Slot backpack = new Slot(0);
    public Slot aura = new Slot(1);
    public Slot steps = new Slot(1);
    public Slot landing = new Slot(1);
    public Slot trail = new Slot(1);

    public Slot shield = new Slot(0);
    public boolean shieldFirstPerson = true;
    public boolean shieldThirdPerson = true;
    public boolean shieldHideOffhand = true;
    public float shieldX = 0f, shieldY = 0f;

    public Slot wings = new Slot(1);
    public Slot motion = new Slot(1);

    public Slot nametag = new Slot(0);
    public boolean nametagShowOwn = true;
    public boolean nametagIcon = true;
    public boolean nametagBold = true;

    public Slot cloak = new Slot(1);
    public int cloakTexture = 0;
    public boolean cloakCustomFile = false;
    public boolean cloakVanillaRenderer = false;

    static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static CosmeticsConfig instance = new CosmeticsConfig();

    public static CosmeticsConfig get() { return instance; }
    static void set(CosmeticsConfig c) { instance = c; }

    public Palette palette() {
        Palette[] v = Palette.values();
        return v[Math.floorMod(shade, v.length)];
    }

    static Path dir() { return FabricLoader.getInstance().getConfigDir().resolve("goshacosmetics"); }
    private static Path file() { return FabricLoader.getInstance().getConfigDir().resolve("goshacosmetics.json"); }

    public static void load() {
        try {
            if (Files.exists(file())) {
                CosmeticsConfig c = GSON.fromJson(Files.readString(file(), StandardCharsets.UTF_8), CosmeticsConfig.class);
                if (c != null) instance = c;
            }
        } catch (Exception e) {
            System.err.println("[GoshaCosmetics] config load failed: " + e);
        }
        save();
    }

    public static void save() {
        try {
            Files.writeString(file(), GSON.toJson(instance), StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.err.println("[GoshaCosmetics] config save failed: " + e);
        }
    }

    public static void reset() {
        instance = new CosmeticsConfig();
        save();
    }
}
