package yiwen.carpetgui.network.client;

import net.minecraft.client.Minecraft;
import yiwen.carpetgui.CarpetGUIRewriteClient;
import yiwen.carpetgui.data.RulesCacheManager;
import yiwen.carpetgui.data.ConfigManager;
import yiwen.carpetgui.network.RuleData;
import yiwen.carpetgui.network.server.RulesPacketPayload;
import yiwen.carpetgui.network.server.RuleStackSyncPayload;
import yiwen.carpetgui.rulestack.RuleStackData;
import yiwen.carpetgui.screen.RuleListScreen;
import yiwen.carpetgui.screen.RuleStackScreen;
import net.minecraft.network.chat.Component;

import java.util.*;

public class ClientPacketHandler {

    public static void handleRules(RulesPacketPayload payload) {
        Minecraft.getInstance().execute(() -> {
            var client = Minecraft.getInstance();
            CarpetGUIRewriteClient.hasModOnServer = true;

            if (!payload.isPartial()) {
                // Full sync
                CarpetGUIRewriteClient.cachedRules.clear();
                for (RuleData r : payload.rules()) {
                    CarpetGUIRewriteClient.cachedRules.put(r.name, r);
                }
                CarpetGUIRewriteClient.cachedCategories = collectCategories();
                CarpetGUIRewriteClient.defaultRules.clear();
                for (String d : payload.defaults().split(";")) {
                    if (!d.isBlank()) CarpetGUIRewriteClient.defaultRules.add(d);
                }
                CarpetGUIRewriteClient.favoriteRules = ConfigManager.readFavorites();
            } else {
                // Partial update
                for (RuleData r : payload.rules()) {
                    CarpetGUIRewriteClient.cachedRules.put(r.name, r);
                }
                CarpetGUIRewriteClient.cachedCategories = collectCategories();
                CarpetGUIRewriteClient.favoriteRules = ConfigManager.readFavorites();
            }

            CarpetGUIRewriteClient.requesting = false;

            // Save cache async
            new Thread(() -> {
                RulesCacheManager.save(new ArrayList<>(CarpetGUIRewriteClient.cachedRules.values()),
                    payload.defaults(), client.getLanguageManager().getSelected());
            }, "carpetgui-cache").start();

            // Open screen
            client.setScreen(new RuleListScreen(Component.literal("Carpet Rules"), true));
        });
    }

    public static void handleRuleStack(RuleStackSyncPayload payload) {
        Minecraft.getInstance().execute(() -> {
            List<RuleStackData.LayerData> layers = payload.layers().stream()
                .map(l -> new RuleStackData.LayerData(l.id(), l.message(), l.timestamp(),
                    l.changes().stream().map(c -> new RuleStackData.Change(
                        c.ruleKey(), c.prevValue(), c.prevIsDefault(),
                        c.newValue(), c.newIsDefault())).toList()))
                .toList();
            List<RuleStackData.LayerData> future = payload.futureLayers().stream()
                .map(l -> new RuleStackData.LayerData(l.id(), l.message(), l.timestamp(),
                    l.changes().stream().map(c -> new RuleStackData.Change(
                        c.ruleKey(), c.prevValue(), c.prevIsDefault(),
                        c.newValue(), c.newIsDefault())).toList()))
                .toList();
            List<RuleStackData.Change> pending = payload.pendingChanges().stream()
                .map(c -> new RuleStackData.Change(
                    c.ruleKey(), c.prevValue(), c.prevIsDefault(),
                    c.newValue(), c.newIsDefault())).toList();

            RuleStackData data = new RuleStackData(payload.activePrefabName(),
                payload.allPrefabNames(), layers, pending, future);
            RuleStackScreen.onSync(data);
        });
    }

    private static List<String> collectCategories() {
        Set<String> cats = new LinkedHashSet<>();
        cats.add("gui.category.all");
        cats.add("gui.category.favorites");
        for (RuleData r : CarpetGUIRewriteClient.cachedRules.values()) {
            for (var entry : r.categories) {
                cats.add(entry.getValue());
            }
        }
        return new ArrayList<>(cats);
    }
}
