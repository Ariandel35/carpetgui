package yiwen.carpetgui.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import yiwen.carpetgui.CarpetGUIRewriteClient;
import yiwen.carpetgui.config.ConfigManager;
import yiwen.carpetgui.network.RuleData;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Main rule browser screen. Replaces the owo-lib based UI.
 * Uses vanilla Minecraft Screen + manual rendering.
 */
public class RuleListScreen extends Screen {

    private static final int TAB_HEIGHT = 24;
    private static final int SEARCH_HEIGHT = 20;
    private static final int RULE_ENTRY_HEIGHT = 22;
    private static final int PADDING = 8;

    private EditBox searchBox;
    private String searchText = "";
    private String activeCategory = "gui.category.all";
    private int scrollOffset;
    private int maxScroll;
    private boolean instantAffect;

    private List<String> displayCategories = new ArrayList<>();
    private List<RuleData> filteredRules = new ArrayList<>();
    private int hoveredRuleIndex = -1;
    private int tabScrollOffset;

    public RuleListScreen(Component title, boolean instantAffect) {
        super(title);
        this.instantAffect = instantAffect;
    }

    @Override
    protected void init() {
        super.init();
        int guiLeft = (width - 300) / 2;

        // Search box
        searchBox = new EditBox(font, guiLeft, 36, 300, SEARCH_HEIGHT, Component.literal("Search"));
        searchBox.setMaxLength(100);
        searchBox.setHint(Component.translatable("gui.carpetgui.search_hint"));
        searchBox.setResponder(text -> {
            searchText = text;
            scrollOffset = 0;
            rebuildFilteredList();
        });
        addRenderableWidget(searchBox);

        // Bottom buttons
        int btnY = height - 30;
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), btn -> onClose())
            .pos(guiLeft, btnY).size(145, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("gui.carpetgui.reset_defaults"), btn -> {
            // Send reset command
            if (minecraft.player != null) {
                minecraft.player.connection.sendCommand("carpet setDefault");
            }
            onClose();
        }).pos(guiLeft + 155, btnY).size(145, 20).build());

        rebuildDisplay();
    }

    private void rebuildDisplay() {
        displayCategories.clear();
        displayCategories.add("gui.category.all");
        displayCategories.add("gui.category.favorites");
        Set<String> seen = new LinkedHashSet<>(displayCategories);
        for (RuleData r : CarpetGUIRewriteClient.cachedRules.values()) {
            for (var entry : r.categories) {
                if (seen.add(entry.getValue())) {
                    displayCategories.add(entry.getValue());
                }
            }
        }
        rebuildFilteredList();
    }

    private void rebuildFilteredList() {
        filteredRules.clear();
        String search = searchText.toLowerCase(Locale.ROOT);
        List<String> favorites = ConfigManager.readFavorites();

        for (RuleData rule : CarpetGUIRewriteClient.cachedRules.values()) {
            // Category filter
            if (!activeCategory.equals("gui.category.all")) {
                if (activeCategory.equals("gui.category.favorites")) {
                    if (!favorites.contains(rule.name)) continue;
                } else {
                    boolean match = rule.categories.stream()
                        .anyMatch(e -> e.getValue().equals(activeCategory));
                    if (!match) continue;
                }
            }

            // Search filter
            if (!search.isEmpty()) {
                boolean match = rule.localName.toLowerCase(Locale.ROOT).contains(search)
                    || rule.name.toLowerCase(Locale.ROOT).contains(search)
                    || rule.localDescription.toLowerCase(Locale.ROOT).contains(search)
                    || rule.manager.toLowerCase(Locale.ROOT).contains(search);
                if (!match) continue;
            }

            filteredRules.add(rule);
        }

        int topHeight = 60 + TAB_HEIGHT + 4;
        maxScroll = Math.max(0, filteredRules.size() * RULE_ENTRY_HEIGHT
            - (height - topHeight - 50));
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);

        int guiLeft = (width - 300) / 2;
        int y = 60;

        // Draw category tabs (single row, horizontally scrollable)
        drawCategoryTabs(g, guiLeft, y, mouseX, mouseY);
        y += TAB_HEIGHT + 4;

        // Draw rule list background
        int listY = y;
        int listHeight = height - listY - 50;
        g.fill(guiLeft, listY, guiLeft + 300, listY + listHeight, 0xFF1E1E1E);
        g.fill(guiLeft - 2, listY - 2, guiLeft + 302, listY + listHeight + 2, 0x88000000);
        g.fill(guiLeft - 1, listY - 1, guiLeft + 301, listY + listHeight + 1, 0x88FFFFFF);

        // Draw rules
        drawRuleList(g, guiLeft, listY, listHeight, mouseX, mouseY);

        // Draw tooltip
        if (hoveredRuleIndex >= 0 && hoveredRuleIndex < filteredRules.size()) {
            RuleData rule = filteredRules.get(hoveredRuleIndex);
            List<Component> tooltip = List.of(
                Component.literal(rule.localName).withColor(0xFFAA00),
                Component.literal("§7" + rule.manager + " | " + rule.type.getSimpleName()),
                Component.literal("§a" + rule.value + "  §8(default: " + rule.defaultValue + ")"),
                Component.literal("§7" + rule.localDescription)
            );
            g.setComponentTooltipForNextFrame(font, tooltip, mouseX, mouseY);
        }
    }

    private void drawCategoryTabs(GuiGraphicsExtractor g, int x, int y, int mouseX, int mouseY) {
        int tabX = x - tabScrollOffset;
        for (String cat : displayCategories) {
            String displayName = Component.translatable(cat).getString();
            int tabW = font.width(displayName) + 12;
            // Skip tabs that are completely off-screen to the left
            if (tabX + tabW < x) { tabX += tabW + 2; continue; }
            // Stop when beyond the right edge
            if (tabX > x + 300) break;

            boolean active = cat.equals(activeCategory);
            boolean hovered = mouseX >= tabX && mouseX < tabX + tabW
                && mouseY >= y && mouseY < y + TAB_HEIGHT;

            int bg = active ? 0xFF404040 : (hovered ? 0xFF606060 : 0xFF202020);
            g.fill(tabX, y, tabX + tabW, y + TAB_HEIGHT, bg);
            g.text(font, displayName, tabX + 6, y + 6, active ? 0xFFFFFFAA : 0xFFCCCCCC);
            tabX += tabW + 2;
        }
    }

    private String truncateText(String text, int maxWidth) {
        if (font.width(text) <= maxWidth) return text;
        String ellipsis = "...";
        int ellipsisW = font.width(ellipsis);
        int maxTextW = maxWidth - ellipsisW;
        for (int i = text.length() - 1; i > 0; i--) {
            if (font.width(text.substring(0, i)) <= maxTextW) {
                return text.substring(0, i) + ellipsis;
            }
        }
        return ellipsis;
    }

    private boolean isBoolean(RuleData rule) {
        return rule.type == Boolean.class || rule.type == boolean.class;
    }

    private static final int TOGGLE_W = 32;
    private static final int TOGGLE_H = 14;
    private static final int TOGGLE_THUMB = 10;

    private void drawRuleList(GuiGraphicsExtractor g, int x, int y, int height, int mouseX, int mouseY) {
        hoveredRuleIndex = -1;
        if (filteredRules.isEmpty()) {
            g.centeredText(font, Component.translatable("gui.carpetgui.no_rules"),
                x + 150, y + height / 2 - 10, 0xFF888888);
            return;
        }

        int startIndex = scrollOffset / RULE_ENTRY_HEIGHT;
        int endIndex = Math.min(filteredRules.size(), startIndex + height / RULE_ENTRY_HEIGHT + 1);

        for (int i = startIndex; i < endIndex; i++) {
            RuleData rule = filteredRules.get(i);
            int ry = y + i * RULE_ENTRY_HEIGHT - scrollOffset;
            if (ry + RULE_ENTRY_HEIGHT < y || ry > y + height) continue;

            boolean isHovered = mouseX >= x && mouseX < x + 300
                && mouseY >= ry && mouseY < ry + RULE_ENTRY_HEIGHT;
            if (isHovered) hoveredRuleIndex = i;

            boolean isDefault = CarpetGUIRewriteClient.defaultRules.contains(rule.name);
            boolean isFav = ConfigManager.isFavorite(rule.name);
            boolean isGamerule = rule.isGamerule;

            // Row background — fully opaque, clamped to list area
            int bgTop = Math.max(ry, y);
            int bgBottom = Math.min(ry + RULE_ENTRY_HEIGHT, y + height);
            int bg;
            if (isHovered) {
                bg = 0xFF505050;
            } else if (i % 2 == 0) {
                bg = 0xFF2A2A2A;
            } else {
                bg = 0xFF222222;
            }
            if (bgTop < bgBottom) {
                g.fill(x, bgTop, x + 300, bgBottom, bg);
            }

            int textY = ry + 3;
            boolean textInBounds = textY >= y && textY < y + height;

            // Star icon (favorite)
            if (textInBounds) {
                String star = isFav ? "★" : "☆";
                int starColor = isFav ? 0xFFFFFF00 : 0xFFAAAAAA;
                g.text(font, star, x + 4, textY, starColor);

                // Rule name — truncate to fit between star and toggle/value
                int maxNameWidth = isBoolean(rule) ? 240 : 270;
                String prefix = isGamerule ? "[G] " : "";
                String displayName = truncateText(prefix + rule.localName, maxNameWidth);
                g.text(font, displayName, x + 18, textY, isGamerule ? 0xFF88BBFF : 0xFFFFFFFF);
            }

            // Right side: toggle/value always within bounds
            if (isBoolean(rule)) {
                int toggleY = ry + 4;
                if (toggleY >= y && toggleY + TOGGLE_H <= y + height) {
                    boolean isTrue = "true".equalsIgnoreCase(rule.value);
                    drawToggle(g, x + 264, toggleY, isTrue);
                }
            } else if (textInBounds) {
                String val = rule.value;
                int valW = font.width(val);
                g.text(font, val, x + 296 - valW, textY,
                    rule.value.equals(rule.defaultValue) ? 0xFF88FF88 : 0xFFFFAA44);
            }
        }
    }

    private void drawToggle(GuiGraphicsExtractor g, int tx, int ty, boolean on) {
        int trackColor = on ? 0xFF33AA33 : 0xFF666666;
        g.fill(tx, ty, tx + TOGGLE_W, ty + TOGGLE_H, trackColor);
        int thumbX = on ? tx + TOGGLE_W - TOGGLE_THUMB - 2 : tx + 2;
        int thumbColor = on ? 0xFFFFFFFF : 0xFFCCCCCC;
        g.fill(thumbX, ty + 2, thumbX + TOGGLE_THUMB, ty + TOGGLE_H - 2, thumbColor);
    }

    private boolean isClickOnToggle(double mouseX, int guiLeft) {
        int toggleX = guiLeft + 264;
        return mouseX >= toggleX && mouseX < toggleX + TOGGLE_W;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean flag) {
        if (super.mouseClicked(event, flag)) return true;

        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        int guiLeft = (width - 300) / 2;

        // Category tab click (single row, horizontally scrollable)
        int tabY = 60;
        if (mouseY >= tabY && mouseY < tabY + TAB_HEIGHT) {
            int tabX = guiLeft - tabScrollOffset;
            for (String cat : displayCategories) {
                String name = Component.translatable(cat).getString();
                int tabW = font.width(name) + 12;
                if (mouseX >= tabX && mouseX < tabX + tabW) {
                    activeCategory = cat;
                    scrollOffset = 0;
                    rebuildFilteredList();
                    return true;
                }
                tabX += tabW + 2;
            }
        }

        // Rule click
        int listY = 60 + TAB_HEIGHT + 4;
        if (button == 0 && hoveredRuleIndex >= 0 && hoveredRuleIndex < filteredRules.size()) {
            RuleData rule = filteredRules.get(hoveredRuleIndex);

            // Toggle favorite on star area click
            if (mouseX >= guiLeft && mouseX < guiLeft + 16) {
                ConfigManager.toggleFavorite(rule.name);
                rebuildFilteredList();
                return true;
            }

            // Toggle boolean value if clicking the switch area
            if (isBoolean(rule) && isClickOnToggle(mouseX, guiLeft)) {
                String newVal = "true".equalsIgnoreCase(rule.value) ? "false" : "true";
                if (minecraft != null && minecraft.player != null) {
                    minecraft.player.connection.sendCommand(
                        (rule.isGamerule ? "gamerule " : "carpet ") + rule.name + " " + newVal);
                    rule.value = newVal;
                }
                return true;
            }

            // Open edit screen (click on rule name area)
            if (minecraft != null) {
                minecraft.setScreen(new RuleEditScreen(rule, instantAffect, () -> {
                    rebuildFilteredList();
                    return null;
                }));
            }
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        // In tab area: vertical scroll → horizontal tab scroll
        if (mouseY >= 60 && mouseY < 60 + TAB_HEIGHT) {
            tabScrollOffset = Math.max(0, (int)(tabScrollOffset - scrollY * 20));
            return true;
        }
        // Native horizontal scroll anywhere → tab scroll
        if (scrollX != 0) {
            tabScrollOffset = Math.max(0, (int)(tabScrollOffset - scrollX * 20));
            return true;
        }
        scrollOffset = Math.max(0, Math.min(maxScroll,
            (int)(scrollOffset - scrollY * 20)));
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.input() == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        if (event.input() == GLFW.GLFW_KEY_F9 && !searchBox.isFocused()) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
