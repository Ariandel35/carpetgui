package yiwen.carpetgui.config;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class ConfigManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_FILE = FabricLoader.getInstance()
        .getConfigDir().resolve("carpetgui-rewrite").resolve("config.json");

    public static void init() {
        try {
            Files.createDirectories(CONFIG_FILE.getParent());
            if (!Files.exists(CONFIG_FILE)) {
                JsonObject root = new JsonObject();
                root.add("favorites", new JsonArray());
                writeJson(root);
            }
        } catch (IOException e) {
            // ignore
        }
    }

    public static List<String> readFavorites() {
        JsonObject root = readJson();
        if (root == null) return new ArrayList<>();
        JsonArray arr = root.getAsJsonArray("favorites");
        if (arr == null) return new ArrayList<>();
        List<String> result = new ArrayList<>();
        for (JsonElement e : arr) result.add(e.getAsString());
        return result;
    }

    public static void setFavorites(List<String> favorites) {
        JsonObject root = readJson();
        if (root == null) root = new JsonObject();
        JsonArray arr = new JsonArray();
        for (String f : favorites) arr.add(f);
        root.add("favorites", arr);
        writeJson(root);
    }

    public static boolean isFavorite(String ruleName) {
        return readFavorites().contains(ruleName);
    }

    public static void toggleFavorite(String ruleName) {
        List<String> favs = new ArrayList<>(readFavorites());
        if (favs.contains(ruleName)) {
            favs.remove(ruleName);
        } else {
            favs.add(ruleName);
        }
        setFavorites(favs);
    }

    private static JsonObject readJson() {
        if (!Files.exists(CONFIG_FILE)) return null;
        try (Reader r = new InputStreamReader(Files.newInputStream(CONFIG_FILE), StandardCharsets.UTF_8)) {
            return GSON.fromJson(r, JsonObject.class);
        } catch (IOException e) {
            return null;
        }
    }

    private static void writeJson(JsonObject obj) {
        try (Writer w = new OutputStreamWriter(Files.newOutputStream(CONFIG_FILE,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING), StandardCharsets.UTF_8)) {
            GSON.toJson(obj, w);
        } catch (IOException e) {
            // ignore
        }
    }
}
