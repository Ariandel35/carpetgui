package yiwen.carpetgui.data;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import yiwen.carpetgui.network.RuleData;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class RulesCacheManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CACHE_FILE = FabricLoader.getInstance()
        .getConfigDir().resolve("carpetgui-rewrite").resolve("rule_cache.json");

    public static void save(List<RuleData> rules, String defaults, String lang) {
        try {
            Files.createDirectories(CACHE_FILE.getParent());
            JsonObject root = new JsonObject();
            root.addProperty("defaults", defaults);
            root.addProperty("lang", lang);

            JsonArray arr = new JsonArray();
            for (RuleData r : rules) {
                JsonObject obj = new JsonObject();
                obj.addProperty("manager", r.manager);
                obj.addProperty("name", r.name);
                obj.addProperty("localName", r.localName);
                obj.addProperty("type", r.type.getName());
                obj.addProperty("defaultValue", r.defaultValue);
                obj.addProperty("description", r.description);
                obj.addProperty("localDescription", r.localDescription);
                obj.addProperty("isGamerule", r.isGamerule);
                JsonArray suggs = new JsonArray();
                r.suggestions.forEach(suggs::add);
                obj.add("suggestions", suggs);
                JsonArray cats = new JsonArray();
                for (var e : r.categories) {
                    JsonObject cat = new JsonObject();
                    cat.addProperty("key", e.getKey());
                    cat.addProperty("value", e.getValue());
                    cats.add(cat);
                }
                obj.add("categories", cats);
                arr.add(obj);
            }
            root.add("rules", arr);

            try (Writer w = new OutputStreamWriter(Files.newOutputStream(CACHE_FILE,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING), StandardCharsets.UTF_8)) {
                GSON.toJson(root, w);
            }
        } catch (IOException e) {
            // ignore
        }
    }

    public static Optional<CacheResult> load() {
        if (!Files.exists(CACHE_FILE)) return Optional.empty();
        try (Reader r = new InputStreamReader(Files.newInputStream(CACHE_FILE), StandardCharsets.UTF_8)) {
            JsonObject root = GSON.fromJson(r, JsonObject.class);
            String defaults = root.has("defaults") ? root.get("defaults").getAsString() : "";
            List<RuleData> rules = new ArrayList<>();
            for (JsonElement el : root.getAsJsonArray("rules")) {
                JsonObject obj = el.getAsJsonObject();
                List<String> suggs = new ArrayList<>();
                obj.getAsJsonArray("suggestions").forEach(s -> suggs.add(s.getAsString()));
                List<Map.Entry<String, String>> cats = new ArrayList<>();
                obj.getAsJsonArray("categories").forEach(c -> {
                    JsonObject cat = c.getAsJsonObject();
                    cats.add(Map.entry(cat.get("key").getAsString(), cat.get("value").getAsString()));
                });
                RuleData rd = new RuleData(
                    obj.get("manager").getAsString(),
                    obj.get("name").getAsString(),
                    obj.get("localName").getAsString(),
                    RuleData.getRuleType(obj.get("type").getAsString()),
                    obj.get("defaultValue").getAsString(),
                    obj.get("defaultValue").getAsString(),
                    obj.get("description").getAsString(),
                    obj.get("localDescription").getAsString(),
                    suggs, cats);
                rd.isGamerule = obj.get("isGamerule").getAsBoolean();
                rules.add(rd);
            }
            return Optional.of(new CacheResult(rules, defaults));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public record CacheResult(List<RuleData> rules, String defaults) {}
}
