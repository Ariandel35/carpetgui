package yiwen.carpetgui.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import yiwen.carpetgui.config.ConfigManager;
import yiwen.carpetgui.network.RuleData;

/**
 * Rule detail/edit screen. Pure vanilla rendering.
 */
public class RuleEditScreen extends Screen {

    private final RuleData rule;
    private final boolean instantAffect;
    private final Supplier<Void> onSave;
    private EditBox valueInput;
    private String currentValue;
    private int selectedSuggestion = -1;
    private List<String> suggestions;

    public RuleEditScreen(RuleData rule, boolean instantAffect, Supplier<Void> onSave) {
        super(Component.literal(rule.localName));
        this.rule = rule;
        this.instantAffect = instantAffect;
        this.onSave = onSave;
        this.currentValue = rule.value;
        this.suggestions = new ArrayList<>(rule.suggestions);
    }

    @Override
    protected void init() {
        super.init();
        int centerX = width / 2;
        int y = 50;

        // Rule name header
        addRenderableWidget(Button.builder(
            Component.literal(ConfigManager.isFavorite(rule.name) ? "★ " + rule.localName : "☆ " + rule.localName),
            btn -> { ConfigManager.toggleFavorite(rule.name); rebuildWidgets(); })
            .pos(centerX - 150, y).size(300, 20).build());
        y += 24;

        // Manager & type info
        addRenderableWidget(Button.builder(
            Component.literal("§7" + rule.manager + " | " + rule.type.getSimpleName()
                + (rule.isGamerule ? " | Gamerule" : "")), btn -> {})
            .pos(centerX - 150, y).size(300, 20).build());
        y += 24;

        // Description
        addRenderableWidget(Button.builder(
            Component.literal("§7" + truncate(rule.localDescription, 80)), btn -> {})
            .pos(centerX - 150, y).size(300, 20).build());
        y += 28;

        // Default value display
        addRenderableWidget(Button.builder(
            Component.literal("§8Default: " + rule.defaultValue), btn -> {})
            .pos(centerX - 150, y).size(300, 20).build());
        y += 24;

        // Current value
        addRenderableWidget(Button.builder(
            Component.literal("§eCurrent: " + currentValue), btn -> {})
            .pos(centerX - 150, y).size(300, 20).build());
        y += 28;

        // Value input or suggestion buttons
        if (rule.type == Boolean.class || rule.type == boolean.class) {
            // Toggle button for boolean
            boolean isTrue = "true".equalsIgnoreCase(currentValue);
            addRenderableWidget(Button.builder(
                Component.literal("  " + (isTrue ? "§a✓ true" : "§7true") + "  "),
                btn -> { currentValue = "true"; applyChange(); })
                .pos(centerX - 80, y).size(75, 20).build());
            addRenderableWidget(Button.builder(
                Component.literal("  " + (!isTrue ? "§c✓ false" : "§7false") + "  "),
                btn -> { currentValue = "false"; applyChange(); })
                .pos(centerX + 5, y).size(75, 20).build());
            y += 24;
        } else if (!suggestions.isEmpty() && suggestions.size() <= 8) {
            // Suggestion buttons
            int sx = centerX - 150;
            for (String sug : suggestions) {
                boolean isCurrent = sug.equals(currentValue);
                addRenderableWidget(Button.builder(
                    Component.literal(isCurrent ? "§a✓ " + sug : sug),
                    btn -> { currentValue = sug; applyChange(); })
                    .pos(sx, y).size(Math.min(140, font.width(sug) + 20), 20).build());
                sx += 155;
                if (sx > centerX + 150) { sx = centerX - 150; y += 24; }
            }
            y += 24;
        } else {
            // Free text input
            valueInput = new EditBox(font, centerX - 150, y, 300, 20, Component.literal("Value"));
            valueInput.setValue(currentValue);
            valueInput.setResponder(val -> currentValue = val);
            valueInput.setMaxLength(256);
            addRenderableWidget(valueInput);
            y += 28;

            // Apply button
            addRenderableWidget(Button.builder(
                Component.literal("§2Apply"), btn -> applyChange())
                .pos(centerX - 40, y).size(80, 20).build());
            y += 24;
        }

        y += 12;

        // Back and reset buttons
        addRenderableWidget(Button.builder(
            Component.translatable("gui.back"),
            btn -> { onSave.get(); onClose(); })
            .pos(centerX - 110, y).size(100, 20).build());

        addRenderableWidget(Button.builder(
            Component.literal("§6Reset to default"),
            btn -> { currentValue = rule.defaultValue; applyChange(); })
            .pos(centerX + 10, y).size(100, 20).build());
    }

    private void applyChange() {
        if (minecraft == null || minecraft.player == null) return;

        String cmd;
        if (rule.isGamerule) {
            cmd = "gamerule " + rule.name + " " + currentValue;
        } else {
            cmd = "carpet " + rule.name + " " + currentValue;
        }
        minecraft.player.connection.sendCommand(cmd);
        rule.value = currentValue;
        rebuildWidgets();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.input() == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            onSave.get();
            onClose();
            return true;
        }
        if (event.input() == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER && valueInput != null && valueInput.isFocused()) {
            applyChange();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private String truncate(String s, int max) {
        return s.length() > max ? s.substring(0, max - 3) + "..." : s;
    }
}
