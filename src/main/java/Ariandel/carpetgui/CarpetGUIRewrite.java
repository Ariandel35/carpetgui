package Ariandel.carpetgui;

import carpet.CarpetExtension;
import carpet.CarpetServer;
import carpet.api.settings.RuleHelper;
import carpet.api.settings.SettingsManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.network.FriendlyByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import Ariandel.carpetgui.network.PacketIDs;
import Ariandel.carpetgui.network.RuleData;
import Ariandel.carpetgui.network.client.RequestRulesPayload;
import Ariandel.carpetgui.network.client.RequestRuleStackPayload;
import Ariandel.carpetgui.network.server.*;
import Ariandel.carpetgui.rulestack.RuleStackCommand;
import Ariandel.carpetgui.rulestack.PrefabManager;
import Ariandel.carpetgui.translation.TranslationHelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;

public class CarpetGUIRewrite implements ModInitializer, CarpetExtension {

    public static final String MOD_ID = "carpetgui";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static PrefabManager prefabManager;

    @Override
    public void onInitialize() {
        CarpetServer.manageExtension(this);

        CommandRegistrationCallback.EVENT.register((dispatcher, ctx, env) -> {
            RuleStackCommand.register(dispatcher);
        });

        ServerPlayNetworking.registerGlobalReceiver(PacketIDs.REQUEST_RULES,
            (server, player, handler, buf, sender) -> {
                RequestRulesPayload payload = new RequestRulesPayload(buf);
                ServerPacketHandler.handleRequestRules(payload, player, server);
            });

        ServerPlayNetworking.registerGlobalReceiver(PacketIDs.REQUEST_RULE_STACK,
            (server, player, handler, buf, sender) -> {
                RequestRuleStackPayload payload = new RequestRuleStackPayload(buf);
                ServerPacketHandler.handleRequestRuleStack(payload, player, server);
            });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            FriendlyByteBuf helloBuf = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
            new HelloPacketPayload().write(helloBuf);
            sender.sendPacket(PacketIDs.HELLO, helloBuf);
        });
    }

    public static List<RuleData> getRules(String lang) {
        List<RuleData> rules = new ArrayList<>(getRules(CarpetServer.settingsManager, lang));
        for (CarpetExtension ext : CarpetServer.extensions) {
            SettingsManager mgr = ext.extensionSettingsManager();
            if (mgr != null && mgr != CarpetServer.settingsManager) {
                rules.addAll(getRules(mgr, lang));
            }
        }
        return rules;
    }

    private static List<RuleData> getRules(SettingsManager mgr, String lang) {
        List<RuleData> rules = new ArrayList<>();
        String managerId = mgr.identifier();
        mgr.getCarpetRules().forEach(rule -> {
            String name = rule.name();
            String localName = TranslationHelper.getNameTranslation(lang, managerId, name);
            String desc = RuleHelper.translatedDescription(rule);
            String localDesc = TranslationHelper.getDescTranslation(lang, managerId, name);

            rules.add(new RuleData(managerId, name, localName, rule.type(),
                rule.defaultValue().toString(), rule.value().toString(),
                desc, localDesc, rule.suggestions().stream().toList(),
                rule.categories().stream()
                    .map(cat -> Map.entry(cat,
                        TranslationHelper.getCategoryTranslation(lang, managerId, cat)))
                    .toList()));
        });
        return rules;
    }

    public static String getDefaults() {
        StringBuilder sb = new StringBuilder();
        readDefaults(getDefaultsConfig()).forEach(c -> sb.append(c).append(";"));
        return sb.toString();
    }

    private static List<String> readDefaults(Path path) {
        try (BufferedReader r = Files.newBufferedReader(path)) {
            List<String> result = new ArrayList<>();
            String line;
            while ((line = r.readLine()) != null) {
                line = line.replaceAll("[\\r\\n]", "");
                String[] fields = line.split("\\s+", 2);
                if (fields.length > 1 && !fields[0].startsWith("#") && !fields[1].startsWith("#")) {
                    result.add(fields[0]);
                }
            }
            return result;
        } catch (IOException e) {
            return List.of();
        }
    }

    private static Path getDefaultsConfig() {
        return CarpetServer.minecraft_server
            .getWorldPath(LevelResource.ROOT)
            .resolve(CarpetServer.settingsManager.identifier() + ".conf");
    }

    @Override
    public void onServerLoadedWorlds(MinecraftServer server) {
        prefabManager = new PrefabManager(server);
        prefabManager.init();
    }

    @Override
    public void onServerClosed(MinecraftServer server) {
        prefabManager = null;
    }

    public static PrefabManager getPrefabManager() { return prefabManager; }

    public static void forEachManager(Consumer<SettingsManager> consumer) {
        consumer.accept(CarpetServer.settingsManager);
        for (CarpetExtension e : CarpetServer.extensions) {
            SettingsManager m = e.extensionSettingsManager();
            if (m != null) consumer.accept(m);
        }
    }
}
