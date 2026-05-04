package Ariandel.carpetgui.translation;

import carpet.CarpetServer;
import carpet.CarpetExtension;
import carpet.utils.Translations;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TranslationHelper {

    private static final Map<String, Map<String, String>> cache = new ConcurrentHashMap<>();

    private static Map<String, String> loadLanguage(String lang) {
        Map<String, String> result = new HashMap<>();

        // Load Carpet's main translations for this language
        Map<String, String> main = Translations.getTranslationFromResourcePath("assets/carpet/lang/" + lang + ".json");
        if (main != null) {
            result.putAll(main);
        }

        // Load extension translations (lower priority — don't override Carpet's keys)
        if (CarpetServer.extensions != null) {
            for (CarpetExtension ext : CarpetServer.extensions) {
                try {
                    Map<String, String> extTranslations = ext.canHasTranslations(lang);
                    if (extTranslations != null) {
                        extTranslations.forEach((k, v) -> result.putIfAbsent(k, v));
                    }
                } catch (Exception ignored) {
                }
            }
        }

        return result;
    }

    private static final String FORCED_LANG = "zh_cn";

    private static Map<String, String> getMap(String lang) {
        return cache.computeIfAbsent(FORCED_LANG, TranslationHelper::loadLanguage);
    }

    public static String getNameTranslation(String lang, String managerId, String ruleName) {
        String key = managerId + ".rule." + ruleName + ".name";
        return getMap(lang).getOrDefault(key, ruleName);
    }

    public static String getDescTranslation(String lang, String managerId, String ruleName) {
        String key = managerId + ".rule." + ruleName + ".desc";
        return getMap(lang).getOrDefault(key, "");
    }

    public static String getCategoryTranslation(String lang, String managerId, String category) {
        String key = managerId + ".category." + category;
        return getMap(lang).getOrDefault(key, category);
    }
}
