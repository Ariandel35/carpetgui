package yiwen.carpetgui.translation;

import carpet.utils.Translations;

public class TranslationHelper {

    public static String getNameTranslation(String lang, String managerId, String ruleName) {
        String key = managerId + ".rule." + ruleName + ".name";
        return Translations.tr(key, ruleName);
    }

    public static String getDescTranslation(String lang, String managerId, String ruleName) {
        String key = managerId + ".rule." + ruleName + ".desc";
        return Translations.tr(key, "");
    }

    public static String getCategoryTranslation(String lang, String managerId, String category) {
        String key = managerId + ".category." + category;
        return Translations.tr(key, category);
    }
}
