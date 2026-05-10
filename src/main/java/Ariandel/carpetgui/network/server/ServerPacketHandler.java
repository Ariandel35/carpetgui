package Ariandel.carpetgui.network.server;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.FriendlyByteBuf;

import Ariandel.carpetgui.CarpetGUIRewrite;
import Ariandel.carpetgui.network.PacketIDs;
import Ariandel.carpetgui.network.RuleData;
import Ariandel.carpetgui.network.client.RequestRulesPayload;
import Ariandel.carpetgui.network.client.RequestRuleStackPayload;
import Ariandel.carpetgui.rulestack.PrefabManager;
import Ariandel.carpetgui.rulestack.Prefab;

import java.util.List;

public class ServerPacketHandler {

    public static void handleRequestRules(RequestRulesPayload payload, ServerPlayer player, MinecraftServer server) {
        server.execute(() -> {
            String lang = payload.lang;
            List<RuleData> allRules = CarpetGUIRewrite.getRules(lang);
            List<String> known = payload.knownRuleNames;

            List<RuleData> toSend = known.isEmpty()
                ? allRules
                : allRules.stream().filter(r -> !known.contains(r.name)).toList();

            FriendlyByteBuf buf = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
            new RulesPacketPayload(toSend, CarpetGUIRewrite.getDefaults(), !known.isEmpty()).write(buf);
            ServerPlayNetworking.send(player, PacketIDs.SYNC_RULES, buf);
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

            FriendlyByteBuf buf = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
            new RuleStackSyncPayload(mgr.getActiveName(),
                mgr.getAllPrefabs().stream().map(Prefab::getName).toList(),
                layers, pending, future).write(buf);
            ServerPlayNetworking.send(player, PacketIDs.RULE_STACK_SYNC, buf);
        });
    }
}
