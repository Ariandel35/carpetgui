package Ariandel.carpetgui;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import Ariandel.carpetgui.data.FavoritesManager;
import Ariandel.carpetgui.network.RuleData;
import Ariandel.carpetgui.network.client.ClientPacketHandler;
import Ariandel.carpetgui.network.client.RequestRulesPayload;
import Ariandel.carpetgui.network.server.*;
import Ariandel.carpetgui.screen.RuleListScreen;

import java.util.*;

public class CarpetGUIRewriteClient implements ClientModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("carpetgui-client");

    public static KeyMapping openGuiKey;
    public static boolean hasModOnServer;
    public static boolean requesting;
    public static Map<String, RuleData> cachedRules = new LinkedHashMap<>();
    public static List<String> cachedCategories = new ArrayList<>();
    public static List<String> defaultRules = new ArrayList<>();
    public static List<RuleData> incompleteServerRules = new ArrayList<>();
    public static Set<String> favoriteRules = new HashSet<>();

    @Override
    public void onInitializeClient() {

        favoriteRules = FavoritesManager.load();

        openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.carpetgui.open",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F9,
            "category.carpetgui.main"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (openGuiKey.consumeClick()) {
                openRuleScreen(true);
            }
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            hasModOnServer = false;
            incompleteServerRules.clear();
        });

        ClientPlayNetworking.registerGlobalReceiver(HelloPacketPayload.TYPE,
            (payload, ctx) -> hasModOnServer = true);

        ClientPlayNetworking.registerGlobalReceiver(RulesPacketPayload.TYPE,
            (payload, ctx) -> ClientPacketHandler.handleRules(payload));

        ClientPlayNetworking.registerGlobalReceiver(RuleStackSyncPayload.TYPE,
            (payload, ctx) -> ClientPacketHandler.handleRuleStack(payload));
    }

    public static void openRuleScreen(boolean instantAffect) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        if (hasModOnServer) {
            String lang = client.getLanguageManager().getSelected();
            ClientPlayNetworking.send(new RequestRulesPayload(lang, List.of()));
            requesting = true;
        } else {
            client.setScreen(new RuleListScreen(Component.literal("Carpet Rules"), instantAffect));
        }
    }

    public static String getServerAddress(Minecraft client) {
        ServerData data = client.getCurrentServer();
        if (data != null) return data.ip;
        if (client.hasSingleplayerServer()) return "singleplayer";
        return null;
    }
}
