package Ariandel.carpetgui.data;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class FavoritesManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FAVORITES_FILE = FabricLoader.getInstance()
            .getConfigDir().resolve("carpetgui-rewrite").resolve("favorites.json");

    public static Set<String> load() {
        if (!Files.exists(FAVORITES_FILE)) return new HashSet<>();
        try (Reader r = Files.newBufferedReader(FAVORITES_FILE, StandardCharsets.UTF_8)) {
            JsonObject root = GSON.fromJson(r, JsonObject.class);
            if (root == null) return new HashSet<>();
            JsonArray arr = root.getAsJsonArray("favorites");
            Set<String> result = new HashSet<>();
            if (arr != null) {
                for (JsonElement el : arr) {
                    result.add(el.getAsString());
                }
            }
            return result;
        } catch (Exception e) {
            return new HashSet<>();
        }
    }

    public static void save(Set<String> favorites) {
        try {
            Files.createDirectories(FAVORITES_FILE.getParent());
            JsonObject root = new JsonObject();
            JsonArray arr = new JsonArray();
            List<String> sorted = new ArrayList<>(favorites);
            Collections.sort(sorted);
            sorted.forEach(arr::add);
            root.add("favorites", arr);
            try (Writer w = new OutputStreamWriter(
                    Files.newOutputStream(FAVORITES_FILE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING),
                    StandardCharsets.UTF_8)) {
                GSON.toJson(root, w);
            }
        } catch (IOException ignored) {
        }
    }
}
