package yiwen.carpetgui.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import java.util.*;

import yiwen.carpetgui.CarpetGUIRewriteClient;
import yiwen.carpetgui.network.RuleData;

/**
 * Batch rule group management screen.
 * Allows selecting multiple rules and applying values to them.
 */
public class RuleGroupScreen extends Screen {

    private final Set<String> selectedRules = new LinkedHashSet<>();
    private List<RuleData> allRules = new ArrayList<>();
    private int scrollOffset;

    public RuleGroupScreen() {
        super(Component.literal("Rule Groups"));
    }

    @Override
    protected void init() {
        super.init();
        allRules = new ArrayList<>(CarpetGUIRewriteClient.cachedRules.values());

        int centerX = width / 2;

        // Select all / deselect all
        addRenderableWidget(Button.builder(
            Component.literal("Select All"),
            btn -> { selectedRules.addAll(allRules.stream().map(r -> r.name).toList()); })
            .pos(centerX - 150, 30).size(145, 20).build());

        addRenderableWidget(Button.builder(
            Component.literal("Deselect All"),
            btn -> selectedRules.clear())
            .pos(centerX + 5, 30).size(145, 20).build());

        // Reset selected to defaults
        addRenderableWidget(Button.builder(
            Component.literal("Reset Selected to Defaults"),
            btn -> {
                if (minecraft != null && minecraft.player != null) {
                    for (String name : selectedRules) {
                        RuleData r = CarpetGUIRewriteClient.cachedRules.get(name);
                        if (r != null) {
                            minecraft.player.connection.sendCommand(
                                (r.isGamerule ? "gamerule " : "carpet ") + name + " " + r.defaultValue);
                        }
                    }
                }
                onClose();
            })
            .pos(centerX - 150, height - 30).size(300, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);

        int y = 56;
        int x = (width - 300) / 2;
        int startIdx = scrollOffset / 22;

        for (int i = startIdx; i < Math.min(allRules.size(), startIdx + 20); i++) {
            RuleData rule = allRules.get(i);
            int ry = y + (i - startIdx) * 22;

            boolean selected = selectedRules.contains(rule.name);
            boolean hovered = mouseX >= x && mouseX < x + 300
                && mouseY >= ry && mouseY < ry + 22;

            int bg = selected ? 0xFF335533 : (hovered ? 0xFF444444 : (i % 2 == 0 ? 0x22000000 : 0x11000000));
            g.fill(x, ry, x + 300, ry + 22, bg);

            String prefix = selected ? "§a[✓] " : "§7[ ] ";
            g.text(font, prefix + rule.localName + "  §7= " + rule.value, x + 4, ry + 5, 0xFFFFFFFF);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean flag) {
        if (super.mouseClicked(event, flag)) return true;

        double mouseX = event.x();
        double mouseY = event.y();
        int x = (width - 300) / 2;
        int y = 56;
        int startIdx = scrollOffset / 22;

        for (int i = startIdx; i < Math.min(allRules.size(), startIdx + 20); i++) {
            int ry = y + (i - startIdx) * 22;
            if (mouseX >= x && mouseX < x + 300 && mouseY >= ry && mouseY < ry + 22) {
                String name = allRules.get(i).name;
                if (selectedRules.contains(name)) {
                    selectedRules.remove(name);
                } else {
                    selectedRules.add(name);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scrollOffset = Math.max(0, scrollOffset - (int)(scrollY * 20));
        return true;
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
