package ml.mypals.carpetgui.screen;

import com.mojang.blaze3d.platform.InputConstants;
import io.wispforest.owo.ui.base.BaseComponent;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.*;
import ml.mypals.carpetgui.mixin.accessors.KeyMappingAccessor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public class ScreenSwitcherScreen extends BaseOwoScreen<FlowLayout> {

    static final ResourceLocation SLOT_SPRITE =
            ResourceLocation.withDefaultNamespace("gamemode_switcher/slot");
    static final ResourceLocation SELECTION_SPRITE =
            ResourceLocation.withDefaultNamespace("gamemode_switcher/selection");

    private static final int SLOT_AREA    = 26;
    private static final int SLOT_PADDING = 5;
    private static final int ICON_OFFSET  = 5;

    private long openTime;
    private static final long HOLD_THRESHOLD = 300;

    private static int triggerKey;
    private static final List<ScreenEntry> ENTRIES = new ArrayList<>();

    public static void registerEntry(Component name, ItemStack icon, Runnable factory) {
        ENTRIES.add(new ScreenEntry(ENTRIES.size(), name, icon, factory));
    }

    public static void setTriggerKey(int key) {
        triggerKey = key;
    }
    private ScreenEntry currentlyHovered;
    private LabelComponent titleLabel;
    private final List<ScreenSlotComponent> slotComponents = new ArrayList<>();

    public ScreenSwitcherScreen(boolean firstEnter) {
        super(GameNarrator.NO_TITLE);
        this.openTime = System.currentTimeMillis();
        if(!firstEnter){
            ScreenSwitcherScreen.setTriggerKey(GLFW.GLFW_KEY_ESCAPE);
        }
        currentlyHovered = ENTRIES.isEmpty() ? null : ENTRIES.getFirst();
    }
    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, Containers::verticalFlow);
    }

    @Override
    protected void build(FlowLayout root) {
        root.horizontalAlignment(HorizontalAlignment.CENTER)
                .verticalAlignment(VerticalAlignment.CENTER)
                .sizing(Sizing.fill(100), Sizing.fill(100));

        titleLabel = Components.label(
                currentlyHovered != null ? currentlyHovered.name() : Component.empty());
        titleLabel.color(Color.ofArgb(0xFFFFFFFF));
        titleLabel.margins(Insets.bottom(14));
        root.child(titleLabel);

        FlowLayout slotsRow = Containers.horizontalFlow(Sizing.content(), Sizing.content());
        slotsRow.gap(SLOT_PADDING);

        slotComponents.clear();
        for (ScreenEntry entry : ENTRIES) {
            ScreenSlotComponent slot = new ScreenSlotComponent(entry);
             slot.mouseEnter().subscribe(() -> {
                 currentlyHovered = entry;
                 updateSelection();
            });
            slot.mouseDown().subscribe((x,y,btn) -> {
                openSelected();
                return true;
            });

            slotComponents.add(slot);
            slotsRow.child(slot);
        }

        root.child(slotsRow);
        updateSelection();
    }


    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        if (checkToClose()) return;
        super.render(guiGraphics, mouseX, mouseY, delta);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {}

    @Override
    public boolean isPauseScreen() { return false; }

    private void updateSelection() {
        if (titleLabel != null && currentlyHovered != null)
            titleLabel.text(currentlyHovered.name());
        for (ScreenSlotComponent c : slotComponents)
            c.setSelected(c.entry == currentlyHovered);
    }

    private boolean checkToClose() {
        if (minecraft == null) return false;
        boolean keyStillHeld = InputConstants.isKeyDown(minecraft.getWindow().getWindow(), triggerKey);
        long heldTime = System.currentTimeMillis() - openTime;
        if (!keyStillHeld) {
            if (heldTime < HOLD_THRESHOLD && triggerKey == GLFW.GLFW_KEY_ESCAPE) {
                minecraft.setScreen(null);
                return true;
            }
            minecraft.setScreen(null);
            openSelected();
            return true;
        }
        return false;
    }

    public void onClose(){
        if(minecraft == null || !InputConstants.isKeyDown(minecraft.getWindow().getWindow(), GLFW.GLFW_KEY_ESCAPE)){
            super.onClose();
        }
    }

    private void openSelected() {
        if (currentlyHovered != null) currentlyHovered.factory().run();
    }

    public record ScreenEntry(int index, Component name, ItemStack icon, Runnable factory) {}


    @Environment(EnvType.CLIENT)
    public static class ScreenSlotComponent extends BaseComponent {

        final ScreenEntry entry;
        private boolean isSelected;

        public ScreenSlotComponent(ScreenEntry entry) {
            this.entry = entry;
            sizing(Sizing.fixed(SLOT_AREA), Sizing.fixed(SLOT_AREA));
            cursorStyle(CursorStyle.HAND);
        }

        @Override
        public void draw(OwoUIDrawContext context, int mouseX, int mouseY,
                         float partialTicks, float delta) {
            context.blitSprite(SLOT_SPRITE, x, y, SLOT_AREA, SLOT_AREA);
            context.renderItem(entry.icon(), x + ICON_OFFSET, y + ICON_OFFSET);
            if (isSelected) {
                context.blitSprite(SELECTION_SPRITE, x, y, SLOT_AREA, SLOT_AREA);
            }
        }
        @Override
        public boolean isInBoundingBox(double x, double y) {
            return x >= (double)this.x() && x < (double)(this.x() + this.width());
        }

        public void setSelected(boolean selected) {
            this.isSelected = selected;
        }
    }
}