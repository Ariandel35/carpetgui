package Ariandel.carpetgui.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import Ariandel.carpetgui.CarpetGUIRewriteClient;
import Ariandel.carpetgui.data.FavoritesManager;
import Ariandel.carpetgui.network.RuleData;

import java.util.*;
import java.util.stream.Collectors;

public class RuleListScreen extends Screen {

    // Layout
    private static final int TOTAL_WIDTH = 460;
    private static final int SIDEBAR_WIDTH = 120;
    private static final int PANEL_WIDTH = TOTAL_WIDTH - SIDEBAR_WIDTH;

    // Sidebar
    private static final int TAB_HEIGHT = 22;
    private static final int TAB_GAP = 2;
    private static final int SIDEBAR_TOP = 12;

    // Panel
    private static final int SEARCH_HEIGHT = 20;
    private static final int RULE_ENTRY_HEIGHT = 26;
    private static final int SCROLLBAR_W = 4;

    // Control sizing
    private static final int STAR_SIZE = 12;
    private static final int BOOLEAN_BTN_W = 54;
    private static final int BOOLEAN_BTN_H = 18;
    private static final int VALUE_BTN_W = 60;
    private static final int VALUE_BTN_H = 18;
    private static final int DROPDOWN_W = 120;
    private static final int DROPDOWN_ENTRY_H = 18;

    private EditBox searchBox;
    private String searchText = "";
    private String activeCategory = "gui.category.all";
    private int scrollOffset;
    private int maxScroll;
    private boolean instantAffect;

    private List<String> displayCategories = new ArrayList<>();
    private List<RuleData> filteredRules = new ArrayList<>();
    private int hoveredRuleIndex = -1;
    private int sidebarScrollOffset;
    private int sidebarMaxScroll;

    // Favorites
    private Set<String> favoriteRules = new HashSet<>();

    // Dropdown state (non-boolean with suggestions)
    private int dropdownRuleIndex = -1;

    // Inline edit state (non-boolean without suggestions)
    private int editingRuleIndex = -1;
    private EditBox inlineEditBox;

    public RuleListScreen(Component title, boolean instantAffect) {
        super(title);
        this.instantAffect = instantAffect;
    }

    @Override
    protected void init() {
        super.init();
        int guiLeft = getGuiLeft();
        int panelLeft = getPanelLeft();

        searchBox = new EditBox(font, panelLeft + 6, 8, PANEL_WIDTH - 12, SEARCH_HEIGHT, Component.literal("Search"));
        searchBox.setMaxLength(100);
        searchBox.setHint(Component.translatable("gui.carpetgui.search_hint"));
        searchBox.setResponder(text -> {
            searchText = text;
            scrollOffset = 0;
            rebuildFilteredList();
        });
        addRenderableWidget(searchBox);

        // Bottom buttons
        int bottomY = height - 28;
        int centerX = guiLeft + TOTAL_WIDTH / 2;
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), btn -> onClose())
                .pos(centerX - 145, bottomY).size(140, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.carpetgui.reset_defaults"), btn -> {
            if (minecraft.player != null) {
                minecraft.player.connection.sendCommand("carpet setDefault");
            }
            onClose();
        }).pos(centerX + 5, bottomY).size(140, 20).build());

        // Inline edit box (hidden initially)
        inlineEditBox = new EditBox(font, 0, 0, VALUE_BTN_W + 20, 18, Component.literal(""));
        inlineEditBox.setMaxLength(256);

        // Load favorites
        favoriteRules = new HashSet<>(CarpetGUIRewriteClient.favoriteRules);

        rebuildDisplay();
    }

    // ───── Layout helpers ─────

    private int getGuiLeft() {
        return (width - TOTAL_WIDTH) / 2;
    }

    private int getPanelLeft() {
        return getGuiLeft() + SIDEBAR_WIDTH;
    }

    private int getListTop() {
        return 8 + SEARCH_HEIGHT + 6;
    }

    private int getListHeight() {
        return height - getListTop() - 42;
    }

    // ───── Display rebuild ─────

    private void rebuildDisplay() {
        displayCategories.clear();
        displayCategories.add("gui.category.all");
        displayCategories.add("gui.category.default");
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

        for (RuleData rule : CarpetGUIRewriteClient.cachedRules.values()) {
            if (!activeCategory.equals("gui.category.all")) {
                boolean passCategory = switch (activeCategory) {
                    case "gui.category.default" ->
                        CarpetGUIRewriteClient.defaultRules.contains(rule.name);
                    case "gui.category.favorites" ->
                        favoriteRules.contains(rule.name);
                    default -> rule.categories.stream()
                            .anyMatch(e -> e.getValue().equals(activeCategory));
                };
                if (!passCategory) continue;
            }

            if (!search.isEmpty()) {
                boolean match = rule.localName.toLowerCase(Locale.ROOT).contains(search)
                    || rule.name.toLowerCase(Locale.ROOT).contains(search)
                    || rule.localDescription.toLowerCase(Locale.ROOT).contains(search)
                    || rule.manager.toLowerCase(Locale.ROOT).contains(search);
                if (!match) continue;
            }

            filteredRules.add(rule);
        }

        int listHeight = getListHeight();
        maxScroll = Math.max(0, filteredRules.size() * RULE_ENTRY_HEIGHT - listHeight);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
        hoveredRuleIndex = -1;
        dropdownRuleIndex = -1;
        editingRuleIndex = -1;
    }

    // ───── Rendering ─────

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        super.render(g, mouseX, mouseY, delta);

        int guiLeft = getGuiLeft();
        int panelLeft = getPanelLeft();
        int listTop = getListTop();
        int listHeight = getListHeight();
        int panelRight = guiLeft + TOTAL_WIDTH;

        // Main background (extend to cover bottom buttons)
        g.fill(guiLeft, SIDEBAR_TOP, panelRight, height - 6, 0xFF111111);

        // Sidebar background + divider
        g.fill(guiLeft, SIDEBAR_TOP, panelLeft, height - 6, 0xFF1A1A2E);
        g.fill(panelLeft - 1, SIDEBAR_TOP, panelLeft, height - 6, 0xFF444444);

        // Panel header line
        g.fill(panelLeft, listTop - 2, panelRight, listTop - 1, 0xFF444444);

        // Sidebar tabs
        drawSidebar(g, guiLeft, mouseX, mouseY);

        // Rule list background
        g.fill(panelLeft, listTop, panelRight - SCROLLBAR_W, listTop + listHeight, 0xFF1E1E1E);

        // Rule entries
        drawRuleList(g, panelLeft, listTop, listHeight, mouseX, mouseY);

        // Dropdown overlay (on top of rule list)
        if (dropdownRuleIndex >= 0) {
            drawDropdown(g, panelLeft, listTop, mouseX, mouseY);
        }

        // Inline edit box
        if (editingRuleIndex >= 0) {
            inlineEditBox.render(g, mouseX, mouseY, delta);
        }

        // Re-render search box on top of background fills
        if (searchBox != null) {
            searchBox.render(g, mouseX, mouseY, delta);
        }

        // Tooltip (only when no dropdown/edit active)
        if (hoveredRuleIndex >= 0 && hoveredRuleIndex < filteredRules.size()
                && dropdownRuleIndex < 0 && editingRuleIndex < 0) {
            RuleData rule = filteredRules.get(hoveredRuleIndex);
            String catStr = rule.categories.stream()
                    .map(e -> Component.translatable(e.getKey()).getString())
                    .collect(Collectors.joining(", "));
            List<Component> tooltip = List.of(
                Component.literal("§6" + rule.localName),
                Component.literal("§7" + rule.manager + " | " + rule.type.getSimpleName()
                    + (rule.isGamerule ? " | Gamerule" : "")),
                Component.literal("§a当前: §f" + rule.value + "  §8默认: " + rule.defaultValue),
                Component.literal("§7" + rule.localDescription),
                Component.literal("§8分类: " + (catStr.isEmpty() ? "无" : catStr))
            );
            g.renderComponentTooltip(font, tooltip, mouseX, mouseY);
        }
    }

    // ───── Sidebar ─────

    private void drawSidebar(GuiGraphics g, int x, int mouseX, int mouseY) {
        int availableHeight = height - 38 - SIDEBAR_TOP;
        int totalTabHeight = displayCategories.size() * (TAB_HEIGHT + TAB_GAP) - TAB_GAP;
        sidebarMaxScroll = Math.max(0, totalTabHeight - availableHeight);
        sidebarScrollOffset = Math.max(0, Math.min(sidebarScrollOffset, sidebarMaxScroll));

        int y = SIDEBAR_TOP - sidebarScrollOffset;

        for (String cat : displayCategories) {
            int tabTop = y;
            int tabBottom = y + TAB_HEIGHT;

            if (tabBottom < SIDEBAR_TOP) { y += TAB_HEIGHT + TAB_GAP; continue; }
            if (tabTop > SIDEBAR_TOP + availableHeight) break;

            String displayName = getTabDisplayName(cat);
            boolean active = cat.equals(activeCategory);
            boolean hovered = mouseX >= x && mouseX < x + SIDEBAR_WIDTH
                && mouseY >= tabTop && mouseY < tabBottom;

            int bg = active ? 0xFF3A3A5C : (hovered ? 0xFF2A2A4C : 0xFF1A1A2E);
            int drawTop = Math.max(tabTop, SIDEBAR_TOP);
            int drawBottom = Math.min(tabBottom, SIDEBAR_TOP + availableHeight);
            if (drawTop < drawBottom) {
                g.fill(x, drawTop, x + SIDEBAR_WIDTH, drawBottom, bg);
            }

            if (active) {
                g.fill(x, tabTop, x + 3, tabBottom, 0xFFFFFFAA);
            }

            int maxTextW = SIDEBAR_WIDTH - 12;
            String text = truncateToWidth(displayName, maxTextW);
            int textX = x + (SIDEBAR_WIDTH - font.width(text)) / 2;
            g.drawString(font, text, textX, tabTop + 5, active ? 0xFFFFFFAA : 0xFFCCCCCC);

            y += TAB_HEIGHT + TAB_GAP;
        }
    }

    private String getTabDisplayName(String catKey) {
        if (catKey.equals("gui.category.all")) return "全部";
        if (catKey.equals("gui.category.default")) return "默认";
        if (catKey.equals("gui.category.favorites")) return "收藏";
        return Component.translatable(catKey).getString();
    }

    // ───── Rule list ─────

    private void drawRuleList(GuiGraphics g, int x, int y, int height, int mouseX, int mouseY) {
        hoveredRuleIndex = -1;
        if (filteredRules.isEmpty()) {
            g.drawCenteredString(font, Component.translatable("gui.carpetgui.no_rules"),
                x + PANEL_WIDTH / 2, y + height / 2 - 10, 0xFF888888);
            return;
        }

        int startIndex = scrollOffset / RULE_ENTRY_HEIGHT;
        int endIndex = Math.min(filteredRules.size(), startIndex + height / RULE_ENTRY_HEIGHT + 2);

        for (int i = startIndex; i < endIndex; i++) {
            RuleData rule = filteredRules.get(i);
            int ry = y + i * RULE_ENTRY_HEIGHT - scrollOffset;
            if (ry + RULE_ENTRY_HEIGHT < y || ry > y + height) continue;

            int rowW = PANEL_WIDTH - SCROLLBAR_W;
            boolean isHovered = mouseX >= x && mouseX < x + rowW
                && mouseY >= ry && mouseY < ry + RULE_ENTRY_HEIGHT;
            if (isHovered) hoveredRuleIndex = i;

            // Row background
            int bgTop = Math.max(ry, y);
            int bgBottom = Math.min(ry + RULE_ENTRY_HEIGHT, y + height);
            int bg;
            if (isHovered) {
                bg = 0xFF3A3A3A;
            } else if (i % 2 == 0) {
                bg = 0xFF2A2A2A;
            } else {
                bg = 0xFF222222;
            }
            if (bgTop < bgBottom) {
                g.fill(x, bgTop, x + rowW, bgBottom, bg);
            }

            if (ry + 3 < y || ry + 3 >= y + height) continue;

            boolean isBool = isBooleanType(rule);
            boolean isAtDefault = rule.value.equals(rule.defaultValue);

            // Star icon
            drawStar(g, x + 4, ry, rule, mouseX, mouseY);

            // Rule name
            int starRight = x + 4 + STAR_SIZE + 3;
            int ctrlW = isBool ? BOOLEAN_BTN_W : VALUE_BTN_W;
            int nameMaxW = rowW - starRight - 10 - ctrlW - 6;
            String prefix = rule.isGamerule ? "[G] " : "";
            String nameText = truncateToWidth(prefix + rule.localName, nameMaxW);
            int nameColor = rule.isGamerule ? 0xFF88BBFF : 0xFFFFFFFF;
            g.drawString(font, nameText, starRight, ry + 5, nameColor);

            // Right-side control
            int ctrlX = x + rowW - ctrlW - 2;
            int ctrlY = ry + (RULE_ENTRY_HEIGHT - (isBool ? BOOLEAN_BTN_H : VALUE_BTN_H)) / 2;

            if (isBool) {
                paintBooleanButton(g, ctrlX, ctrlY, rule, mouseX, mouseY, isAtDefault);
            } else {
                paintValueButton(g, ctrlX, ctrlY, rule, mouseX, mouseY, isAtDefault);
            }
        }

        // Scrollbar
        if (maxScroll > 0) {
            int trackX = x + PANEL_WIDTH - SCROLLBAR_W;
            int trackH = height;
            float ratio = (float) height / (height + maxScroll);
            int thumbH = Math.max(10, (int) (trackH * ratio));
            int thumbY = y + (int) ((float) scrollOffset / maxScroll * (trackH - thumbH));
            g.fill(trackX, y, trackX + SCROLLBAR_W, y + trackH, 0xFF333333);
            g.fill(trackX, thumbY, trackX + SCROLLBAR_W, thumbY + thumbH, 0xFF888888);
        }
    }

    // ───── Star ─────

    private void drawStar(GuiGraphics g, int sx, int ry, RuleData rule, int mouseX, int mouseY) {
        int starY = ry + (RULE_ENTRY_HEIGHT - STAR_SIZE) / 2;
        boolean isFav = favoriteRules.contains(rule.name);
        boolean hoverStar = mouseX >= sx - 1 && mouseX < sx + STAR_SIZE + 3
            && mouseY >= starY - 1 && mouseY < starY + STAR_SIZE + 3;
        String ch = isFav ? "★" : "☆";
        int color = isFav ? 0xFFFFAA00 : (hoverStar ? 0xFFFFAA44 : 0xFF666666);
        g.drawString(font, ch, sx, starY, color);
    }

    // ───── Boolean button ─────

    private void paintBooleanButton(GuiGraphics g, int bx, int by,
            RuleData rule, int mouseX, int mouseY, boolean isAtDefault) {
        boolean isTrue = "true".equalsIgnoreCase(rule.value);
        boolean hovered = mouseX >= bx && mouseX < bx + BOOLEAN_BTN_W
            && mouseY >= by && mouseY < by + BOOLEAN_BTN_H;

        int bgColor = isTrue ? (hovered ? 0xFF44BB44 : 0xFF33AA33)
                             : (hovered ? 0xFFBB4444 : 0xFFAA3333);
        g.fill(bx, by, bx + BOOLEAN_BTN_W, by + BOOLEAN_BTN_H, bgColor);
        g.fill(bx, by, bx + BOOLEAN_BTN_W, by + 1, 0x44000000);

        String text = isTrue ? "true" : "false";
        int textX = bx + (BOOLEAN_BTN_W - font.width(text)) / 2;
        g.drawString(font, text, textX, by + 4, 0xFFFFFFFF);

        // Lock overlay if at default
        if (isAtDefault) {
            drawLockIcon(g, bx + BOOLEAN_BTN_W - 11, by + 3);
        }
    }

    // ───── Value button (non-boolean) ─────

    private void paintValueButton(GuiGraphics g, int bx, int by,
            RuleData rule, int mouseX, int mouseY, boolean isAtDefault) {
        boolean hovered = mouseX >= bx && mouseX < bx + VALUE_BTN_W
            && mouseY >= by && mouseY < by + VALUE_BTN_H;

        int bgColor = hovered ? 0xFF555555 : 0xFF3A3A3A;
        g.fill(bx, by, bx + VALUE_BTN_W, by + VALUE_BTN_H, bgColor);
        g.fill(bx, by, bx + VALUE_BTN_W, by + 1, 0x44000000);

        String valText = truncateToWidth(rule.value, VALUE_BTN_W - 8);
        int textColor = isAtDefault ? 0xFF88FF88 : 0xFFFFAA44;
        int textX = bx + (VALUE_BTN_W - font.width(valText)) / 2;
        g.drawString(font, valText, textX, by + 4, textColor);

        if (isAtDefault) {
            drawLockIcon(g, bx + VALUE_BTN_W - 11, by + 3);
        }
    }

    // ───── Lock icon ─────

    private void drawLockIcon(GuiGraphics g, int lx, int ly) {
        // Semi-transparent dark background
        g.fill(lx, ly, lx + 10, ly + 13, 0x66000000);
        // Lock body (6x5)
        g.fill(lx + 2, ly + 6, lx + 8, ly + 11, 0xCCFFFFFF);
        // Lock shackle (arch)
        g.fill(lx + 1, ly + 2, lx + 9, ly + 7, 0xCCFFFFFF);
        // Keyhole
        g.fill(lx + 4, ly + 7, lx + 6, ly + 10, 0xFF111111);
    }

    // ───── Dropdown ─────

    private void drawDropdown(GuiGraphics g, int panelLeft, int listTop, int mouseX, int mouseY) {
        if (dropdownRuleIndex < 0 || dropdownRuleIndex >= filteredRules.size()) return;

        RuleData rule = filteredRules.get(dropdownRuleIndex);
        if (rule.suggestions.isEmpty()) return;

        int ry = listTop + dropdownRuleIndex * RULE_ENTRY_HEIGHT - scrollOffset;
        int ddX = panelLeft + PANEL_WIDTH - SCROLLBAR_W - DROPDOWN_W - 4;
        int ddY = ry + RULE_ENTRY_HEIGHT + 1;

        ddX = Math.max(panelLeft + 4, Math.min(ddX, panelLeft + PANEL_WIDTH - DROPDOWN_W - SCROLLBAR_W - 4));
        int maxVisible = Math.min(rule.suggestions.size(), 8);
        ddY = Math.min(ddY, height - 44 - maxVisible * DROPDOWN_ENTRY_H);

        int ddH = maxVisible * DROPDOWN_ENTRY_H + 4;

        g.fill(ddX, ddY, ddX + DROPDOWN_W, ddY + ddH, 0xFF2A2A2A);
        g.fill(ddX, ddY, ddX + DROPDOWN_W, ddY + 1, 0xFF888888);
        g.fill(ddX, ddY + ddH - 1, ddX + DROPDOWN_W, ddY + ddH, 0xFF888888);
        g.fill(ddX, ddY, ddX + 1, ddY + ddH, 0xFF888888);
        g.fill(ddX + DROPDOWN_W - 1, ddY, ddX + DROPDOWN_W, ddY + ddH, 0xFF888888);

        for (int si = 0; si < maxVisible; si++) {
            String sug = rule.suggestions.get(si);
            int sy = ddY + 2 + si * DROPDOWN_ENTRY_H;
            boolean sHovered = mouseX >= ddX + 1 && mouseX < ddX + DROPDOWN_W - 1
                && mouseY >= sy && mouseY < sy + DROPDOWN_ENTRY_H;
            boolean isCurrent = sug.equals(rule.value);

            if (sHovered) {
                g.fill(ddX + 1, sy, ddX + DROPDOWN_W - 1, sy + DROPDOWN_ENTRY_H, 0xFF444444);
            }
            String display = isCurrent ? "§a✓ " + sug : sug;
            g.drawString(font, display, ddX + 4, sy + 4, isCurrent ? 0xFF88FF88 : 0xFFFFFFFF);
        }
    }

    // ───── Mouse input ─────

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;

        int guiLeft = getGuiLeft();
        int panelLeft = getPanelLeft();
        int listTop = getListTop();

        // Dropdown click
        if (dropdownRuleIndex >= 0) {
            if (handleDropdownClick((int) mouseX, (int) mouseY, panelLeft, listTop)) return true;
            dropdownRuleIndex = -1;
            return true;
        }

        // Cancel inline edit on outside click
        if (editingRuleIndex >= 0) {
            if (button == 0) applyInlineEdit();
            editingRuleIndex = -1;
            return true;
        }

        // Sidebar clicks
        if (mouseX >= guiLeft && mouseX < panelLeft
                && mouseY >= SIDEBAR_TOP && mouseY < height - 38) {
            handleSidebarClick((int) mouseX, (int) mouseY, guiLeft);
            return true;
        }

        if (button != 0) return false;
        if (hoveredRuleIndex < 0 || hoveredRuleIndex >= filteredRules.size()) return false;

        RuleData rule = filteredRules.get(hoveredRuleIndex);
        int ry = listTop + hoveredRuleIndex * RULE_ENTRY_HEIGHT - scrollOffset;

        // Star click
        if (mouseX >= panelLeft + 2 && mouseX < panelLeft + 22) {
            toggleFavorite(rule.name);
            return true;
        }

        int rowW = PANEL_WIDTH - SCROLLBAR_W;

        // Boolean button
        if (isBooleanType(rule)) {
            int bx = panelLeft + rowW - BOOLEAN_BTN_W - 2;
            if (isInRect(mouseX, mouseY, bx, ry + (RULE_ENTRY_HEIGHT - BOOLEAN_BTN_H) / 2,
                    BOOLEAN_BTN_W, BOOLEAN_BTN_H)) {
                toggleBooleanValue(rule);
                return true;
            }
        } else {
            int bx = panelLeft + rowW - VALUE_BTN_W - 2;
            if (isInRect(mouseX, mouseY, bx, ry + (RULE_ENTRY_HEIGHT - VALUE_BTN_H) / 2,
                    VALUE_BTN_W, VALUE_BTN_H)) {
                if (!rule.suggestions.isEmpty()) {
                    dropdownRuleIndex = hoveredRuleIndex;
                } else {
                    startInlineEdit(hoveredRuleIndex, bx, ry);
                }
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        int guiLeft = getGuiLeft();
        // Sidebar scroll
        if (mouseX >= guiLeft && mouseX < guiLeft + SIDEBAR_WIDTH) {
            sidebarScrollOffset = Math.max(0, Math.min(sidebarMaxScroll,
                (int) (sidebarScrollOffset - amount * 20)));
            return true;
        }
        // Rule list scroll
        if (mouseX >= getPanelLeft() && mouseX < guiLeft + TOTAL_WIDTH) {
            scrollOffset = Math.max(0, Math.min(maxScroll,
                (int) (scrollOffset - amount * 20)));
            return true;
        }
        return true;
    }

    // ───── Keyboard input ─────

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (editingRuleIndex >= 0) {
            if (keyCode == GLFW.GLFW_KEY_ENTER) {
                applyInlineEdit();
                editingRuleIndex = -1;
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                editingRuleIndex = -1;
                return true;
            }
            return inlineEditBox.keyPressed(keyCode, scanCode, modifiers);
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_F9 && !searchBox.isFocused()) {
            onClose();
            return true;
        }
        // Forward to search box if focused
        if (searchBox.isFocused()) {
            return searchBox.keyPressed(keyCode, scanCode, modifiers);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (editingRuleIndex >= 0) {
            return inlineEditBox.charTyped(chr, modifiers);
        }
        // Forward to search box if focused
        if (searchBox.isFocused()) {
            return searchBox.charTyped(chr, modifiers);
        }
        return super.charTyped(chr, modifiers);
    }

    // ───── Interaction handlers ─────

    private void handleSidebarClick(int mouseX, int mouseY, int sidebarLeft) {
        int y = SIDEBAR_TOP - sidebarScrollOffset;
        int availableHeight = height - 38 - SIDEBAR_TOP;

        for (String cat : displayCategories) {
            int tabTop = y;
            int tabBottom = y + TAB_HEIGHT;

            if (tabBottom < SIDEBAR_TOP) { y += TAB_HEIGHT + TAB_GAP; continue; }
            if (tabTop > SIDEBAR_TOP + availableHeight) break;

            if (mouseX >= sidebarLeft && mouseX < sidebarLeft + SIDEBAR_WIDTH
                    && mouseY >= tabTop && mouseY < tabBottom) {
                activeCategory = cat;
                scrollOffset = 0;
                rebuildFilteredList();
                return;
            }

            y += TAB_HEIGHT + TAB_GAP;
        }
    }

    private void toggleBooleanValue(RuleData rule) {
        String newVal = "true".equalsIgnoreCase(rule.value) ? "false" : "true";
        sendRuleCommand(rule, newVal);
        rule.value = newVal;
    }

    private void sendRuleCommand(RuleData rule, String value) {
        if (minecraft != null && minecraft.player != null) {
            minecraft.player.connection.sendCommand(
                (rule.isGamerule ? "gamerule " : "carpet ") + rule.name + " " + value);
        }
    }

    private void toggleFavorite(String ruleName) {
        if (favoriteRules.contains(ruleName)) {
            favoriteRules.remove(ruleName);
        } else {
            favoriteRules.add(ruleName);
        }
        CarpetGUIRewriteClient.favoriteRules = new HashSet<>(favoriteRules);
        FavoritesManager.save(favoriteRules);
        if (activeCategory.equals("gui.category.favorites")) {
            rebuildFilteredList();
        }
    }

    private boolean handleDropdownClick(int mouseX, int mouseY, int panelLeft, int listTop) {
        if (dropdownRuleIndex < 0 || dropdownRuleIndex >= filteredRules.size()) return false;

        RuleData rule = filteredRules.get(dropdownRuleIndex);
        if (rule.suggestions.isEmpty()) return false;

        int ry = listTop + dropdownRuleIndex * RULE_ENTRY_HEIGHT - scrollOffset;
        int ddX = panelLeft + PANEL_WIDTH - SCROLLBAR_W - DROPDOWN_W - 4;
        int ddY = ry + RULE_ENTRY_HEIGHT + 1;
        ddX = Math.max(panelLeft + 4, Math.min(ddX, panelLeft + PANEL_WIDTH - DROPDOWN_W - SCROLLBAR_W - 4));
        int maxVisible = Math.min(rule.suggestions.size(), 8);
        ddY = Math.min(ddY, height - 44 - maxVisible * DROPDOWN_ENTRY_H);

        for (int si = 0; si < maxVisible; si++) {
            int sy = ddY + 2 + si * DROPDOWN_ENTRY_H;
            if (mouseX >= ddX + 1 && mouseX < ddX + DROPDOWN_W - 1
                    && mouseY >= sy && mouseY < sy + DROPDOWN_ENTRY_H) {
                String newVal = rule.suggestions.get(si);
                sendRuleCommand(rule, newVal);
                rule.value = newVal;
                dropdownRuleIndex = -1;
                rebuildFilteredList();
                return true;
            }
        }

        return false;
    }

    private void startInlineEdit(int index, int bx, int ry) {
        editingRuleIndex = index;
        int editX = bx;
        int editY = ry + (RULE_ENTRY_HEIGHT - 18) / 2;
        inlineEditBox.setX(editX);
        inlineEditBox.setY(editY);
        inlineEditBox.setWidth(VALUE_BTN_W + 20);
        inlineEditBox.setValue(filteredRules.get(index).value);
        inlineEditBox.setFocused(true);
        searchBox.setFocused(false);
    }

    private void applyInlineEdit() {
        if (editingRuleIndex < 0 || editingRuleIndex >= filteredRules.size()) return;
        RuleData rule = filteredRules.get(editingRuleIndex);
        String newVal = inlineEditBox.getValue();
        if (!newVal.equals(rule.value)) {
            sendRuleCommand(rule, newVal);
            rule.value = newVal;
        }
        editingRuleIndex = -1;
        rebuildFilteredList();
    }

    // ───── Utilities ─────

    private boolean isBooleanType(RuleData rule) {
        return rule.type == Boolean.class || rule.type == boolean.class;
    }

    private boolean isInRect(double mx, double my, int rx, int ry, int rw, int rh) {
        return mx >= rx && mx < rx + rw && my >= ry && my < ry + rh;
    }

    private String truncateToWidth(String text, int maxWidth) {
        if (font.width(text) <= maxWidth) return text;
        for (int i = text.length() - 1; i > 0; i--) {
            if (font.width(text.substring(0, i)) <= maxWidth) {
                return text.substring(0, i);
            }
        }
        return "";
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
