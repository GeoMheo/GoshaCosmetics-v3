package ru.goshacosmetics;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Плащ из своего PNG (64x32, как обычный плащ Minecraft). Хранится в config/goshacosmetics/cape.png. */
public final class CustomCape {
    public static final Identifier ID = Identifier.of("goshacosmetics", "dynamic/custom_cape");
    private static boolean loaded, tried;

    private CustomCape() {}

    public static boolean isLoaded() {
        if (!loaded && !tried) load();
        return loaded;
    }

    private static Path file() { return CosmeticsConfig.dir().resolve("cape.png"); }

    /** Открывает системное окно выбора файла. Возвращает текст результата. */
    public static String choose() {
        String path;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer filters = stack.mallocPointer(1);
            filters.put(stack.UTF8("*.png"));
            filters.flip();
            path = TinyFileDialogs.tinyfd_openFileDialog("Выбери плащ (PNG 64x32)", "", filters, "PNG", false);
        }
        if (path == null) return "Отменено";
        try {
            Files.createDirectories(CosmeticsConfig.dir());
            Files.copy(Path.of(path), file(), StandardCopyOption.REPLACE_EXISTING);
            tried = false;
            load();
            if (!loaded) return "Не удалось открыть PNG";
            CosmeticsConfig.get().cloakCustomFile = true;
            CosmeticsConfig.save();
            return "Загружено";
        } catch (Exception e) {
            return "Ошибка: " + e.getMessage();
        }
    }

    private static void load() {
        tried = true;
        loaded = false;
        if (!Files.exists(file())) return;
        try (InputStream in = Files.newInputStream(file())) {
            NativeImage img = NativeImage.read(in);
            MinecraftClient mc = MinecraftClient.getInstance();
            mc.getTextureManager().destroyTexture(ID);
            mc.getTextureManager().registerTexture(ID, new NativeImageBackedTexture(img));
            loaded = true;
        } catch (Exception e) {
            System.err.println("[GoshaCosmetics] custom cape load failed: " + e);
        }
    }
}
