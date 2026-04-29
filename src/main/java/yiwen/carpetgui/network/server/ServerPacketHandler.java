package yiwen.carpetgui.network.server;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import yiwen.carpetgui.CarpetGUIRewrite;
import yiwen.carpetgui.network.RuleData;
import yiwen.carpetgui.network.client.RequestRulesPayload;
import yiwen.carpetgui.network.client.RequestRuleStackPayload;
import yiwen.carpetgui.rulestack.PrefabManager;
import yiwen.carpetgui.rulestack.Prefab;

import java.util.List;

public class ServerPacketHandler {

    public static void handleRequestRules(RequestRulesPayload payload, ServerPlayer player, MinecraftServer server) {
        server.execute(() -> {
            String lang = payload.lang();
            List<RuleData> allRules = CarpetGUIRewrite.getRules(lang);
            List<String> known = payload.knownRuleNames();

            List<RuleData> toSend = known.isEmpty()
                ? allRules
                : allRules.stream().filter(r -> !known.contains(r.name)).toList();

            ServerPlayNetworking.send(player,
                new RulesPacketPayload(toSend, CarpetGUIRewrite.getDefaults(), !known.isEmpty()));
        });
    }

    public static void handleRequestRuleStack(RequestRuleStackPayload payload, ServerPlayer player, MinecraftServer server) {
        server.execute(() -> {
            PrefabManager mgr = CarpetGUIRewrite.getPrefabManager();
            if (mgr == null) return;
            Prefab active = mgr.getActivePrefab();

            List<RuleStackSyncPayload.LayerInfo> layers = active.getLayers().stream()
                .map(l -> new RuleStackSyncPayload.LayerInfo(l.getId(), l.getMessage(), l.getTimestamp(),
                    l.getChanges().stream().map(c -> new RuleStackSyncPayload.ChangeInfo(
                        c.ruleKey(), c.previousSnapshot().value(), c.previousSnapshot().isDefault(),
                        c.newSnapshot().value(), c.newSnapshot().isDefault())).toList()))
                .toList();

            List<RuleStackSyncPayload.LayerInfo> future = active.getFutureLayers().stream()
                .map(l -> new RuleStackSyncPayload.LayerInfo(l.getId(), l.getMessage(), l.getTimestamp(),
                    l.getChanges().stream().map(c -> new RuleStackSyncPayload.ChangeInfo(
                        c.ruleKey(), c.previousSnapshot().value(), c.previousSnapshot().isDefault(),
                        c.newSnapshot().value(), c.newSnapshot().isDefault())).toList()))
                .toList();

            List<RuleStackSyncPayload.ChangeInfo> pending = mgr.getPendingChanges().stream()
                .map(c -> new RuleStackSyncPayload.ChangeInfo(
                    c.ruleKey(), c.previousSnapshot().value(), c.previousSnapshot().isDefault(),
                    c.newSnapshot().value(), c.newSnapshot().isDefault())).toList();

            ServerPlayNetworking.send(player, new RuleStackSyncPayload(
                mgr.getActiveName(),
                mgr.getAllPrefabs().stream().map(Prefab::getName).toList(),
                layers, pending, future));
        });
    }
}
