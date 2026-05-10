package Ariandel.carpetgui.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import Ariandel.carpetgui.rulestack.RuleStackData;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Rule change history screen (rule stack).
 */
public class RuleStackScreen extends Screen {

    public static RuleStackScreen INSTANCE;
    public static RuleStackData syncedData;
    private int scrollOffset;

    public static void onSync(RuleStackData data) {
        syncedData = data;
        if (INSTANCE != null) {
            INSTANCE.rebuildWidgets();
        } else {
            Minecraft.getInstance().setScreen(new RuleStackScreen());
        }
    }

    public RuleStackScreen() {
        super(Component.literal("Rule History"));
        INSTANCE = this;
    }

    @Override
    protected void init() {
        super.init();
        int centerX = width / 2;

        addRenderableWidget(Button.builder(
            Component.translatable("gui.back"), btn -> {
                INSTANCE = null;
                onClose();
            }).pos(centerX - 50, height - 30).size(100, 20).build());

        if (syncedData == null) return;

        addRenderableWidget(Button.builder(
            Component.literal("Active: " + syncedData.activePrefabName), btn -> {})
            .pos(centerX - 150, 30).size(300, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        super.render(g, mouseX, mouseY, delta);

        if (syncedData == null) {
            g.drawCenteredString(font, "No rule stack data available", width / 2, height / 2, 0xFFFFFFFF);
            return;
        }

        int y = 56;
        int x = (width - 400) / 2;
        var sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        for (int i = 0; i < syncedData.layers.size(); i++) {
            var layer = syncedData.layers.get(i);
            int ry = y + i * 40 - scrollOffset;
            if (ry < 40 || ry > height - 40) continue;

            g.fill(x, ry, x + 400, ry + 38, 0x33000000);
            g.drawString(font, "Layer: " + layer.id(), x + 4, ry + 2, 0xFFFFAA00);
            g.drawString(font, layer.message(), x + 4, ry + 14, 0xFFAAAAAA);
            g.drawString(font, sdf.format(new Date(layer.timestamp())), x + 4, ry + 26, 0xFF888888);
            g.drawString(font, layer.changes().size() + " changes",
                x + 396 - font.width(layer.changes().size() + " changes"), ry + 14, 0xFF55AAFF);
        }

        if (!syncedData.pendingChanges.isEmpty()) {
            int py = y + syncedData.layers.size() * 40 + 10 - scrollOffset;
            g.drawString(font, "Pending: " + syncedData.pendingChanges.size() + " changes",
                x + 4, py, 0xFFFF5555);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scrollOffset = Math.max(0, scrollOffset - (int)(scrollY * 20));
        return true;
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
