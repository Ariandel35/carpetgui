package ml.mypals.carpetgui.screen;

import com.mojang.blaze3d.platform.InputConstants;
import io.wispforest.owo.ui.base.BaseComponent;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.*;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.Minecraft;
//? if >1.19.4 {
import net.minecraft.client.gui.GuiGraphics;
//?} else {
/*import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
*///?}
//? if >= 1.21.6 {
/*import net.minecraft.client.renderer.RenderPipelines;
*///?} else if > 1.21.1 {
/*import net.minecraft.client.renderer.RenderType;
*///?}
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public class ScreenSwitcherScreen extends BaseOwoScreen<FlowLayout> {

    static final ResourceLocation SLOT_LOCATION =
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/container/gamemode_switcher.png");
    static final ResourceLocation SLOT_SPRITE =
            ResourceLocation.fromNamespaceAndPath("minecraft", "gamemode_switcher/slot");
    static final ResourceLocation SELECTION_SPRITE =
            ResourceLocation.fromNamespaceAndPath("minecraft", "gamemode_switcher/selection");

    private static final int SLOT_AREA    = 26;
    private static final int SLOT_PADDING = 5;
    private static final int ICON_OFFSET  = 5;

    private final long openTime;
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
        return OwoUIAdapter.create(this, /*? if <1.21.11 {*/Containers/*?} else {*//*UIContainers*//*?}*/::verticalFlow);
    }

    @Override
    protected void build(FlowLayout root) {
        root.horizontalAlignment(HorizontalAlignment.CENTER)
                .verticalAlignment(VerticalAlignment.CENTER)
                .sizing(Sizing.fill(100), Sizing.fill(100));

        titleLabel = /*? if <1.21.11 {*/Components/*?} else {*//*UIComponents*//*?}*/.label(
                currentlyHovered != null ? currentlyHovered.name() : Component.empty());
        titleLabel.color(Color.ofArgb(0xFFFFFFFF));
        titleLabel.margins(Insets.bottom(14));
        root.child(titleLabel);

        FlowLayout slotsRow = /*? if <1.21.11 {*/Containers/*?} else {*//*UIContainers*//*?}*/.horizontalFlow(Sizing.content(), Sizing.content());
        slotsRow.gap(SLOT_PADDING);

        slotComponents.clear();
        for (ScreenEntry entry : ENTRIES) {
            ScreenSlotComponent slot = new ScreenSlotComponent(entry);
             slot.mouseEnter().subscribe(() -> {
                 currentlyHovered = entry;
                 updateSelection();
            });
            //? if <1.21.9 {
            slot.mouseDown().subscribe((x, y, btn) -> {
             //?} else {
            /*slot.mouseDown().subscribe((mouseButtonEvent, btn) -> {
            *///?}
                openSelected();
                return true;
            });

            slotComponents.add(slot);
            slotsRow.child(slot);
        }

        root.child(slotsRow);
        updateSelection();
    }


    //? if >1.19.4 {
    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        if (checkToClose()) return;
        super.render(guiGraphics, mouseX, mouseY, delta);
    }
    //?} else {
    /*@Override
    public void render(@NotNull PoseStack poseStack, int mouseX, int mouseY, float delta) {
        if (checkToClose()) return;
        super.render(poseStack, mouseX, mouseY, delta);
    }*///?}

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
        boolean keyStillHeld = InputConstants.isKeyDown(minecraft.getWindow()/*? if <1.21.9 {*/.getWindow()/*?}*/, triggerKey);
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
        if(minecraft == null || !InputConstants.isKeyDown(Minecraft.getInstance().getWindow()/*? if <1.21.9 {*/.getWindow()/*?}*/, GLFW.GLFW_KEY_ESCAPE)){
            super.onClose();
        }
    }

    private void openSelected() {
        if (currentlyHovered != null) currentlyHovered.factory().run();
    }

    public record ScreenEntry(int index, Component name, ItemStack icon, Runnable factory) {}


    @Environment(EnvType.CLIENT)
    public class ScreenSlotComponent extends BaseComponent {

        final ScreenEntry entry;
        private boolean isSelected;

        public ScreenSlotComponent(ScreenEntry entry) {
            this.entry = entry;
            sizing(Sizing.fixed(SLOT_AREA), Sizing.fixed(SLOT_AREA));
            cursorStyle(CursorStyle.HAND);
        }
        //? if >1.19.4 {
        @Override
        public void draw(OwoUIDrawContext context, int mouseX, int mouseY,
                         float partialTicks, float delta) {
            drawSlot(context, x, y, SLOT_AREA);
            context.renderItem(entry.icon(), x + ICON_OFFSET, y + ICON_OFFSET);
            if (isSelected) {
                drawSelection(context, x, y, SLOT_AREA);
            }
        }

        private void drawSlot(OwoUIDrawContext context, int i, int j, int k) {
            //? if <= 1.20.1 {
            /*context.blit(SLOT_LOCATION, i, j, 0.0F, 75.0F, k, k, 128, 128);
            *///?} else if <1.21.4 {
            context.blitSprite(SLOT_SPRITE, i, j,  k, k);
            //?} else if <1.21.6 {
            /*context.blitSprite(RenderType::guiTextured,SLOT_SPRITE, i, j,  k, k);
            *///?} else {
            /*context.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, i, j,  k, k);
            *///?}
        }
        private void drawSelection(OwoUIDrawContext context, int i, int j, int k) {
            //? if <= 1.20.1 {
            /*context.blit(SLOT_LOCATION, i, j, 26.0F, 75.0F, k, k, 128, 128);
            *///?} else if <1.21.4 {
            context.blitSprite(SELECTION_SPRITE, i, j,  k, k);
            //?} else if <1.21.6 {
            /*context.blitSprite(RenderType::guiTextured,SELECTION_SPRITE, i, j,  k, k);
             *///?} else {
            /*context.blitSprite(RenderPipelines.GUI_TEXTURED, SELECTION_SPRITE, i, j,  k, k);
            *///?}
        }
        //?} else {

        /*@Override
        public void draw(PoseStack poseStack, int i, int i1, float v, float v1) {
            drawSlot(poseStack, x, y, SLOT_AREA);
            ScreenSwitcherScreen.this.itemRenderer.renderAndDecorateItem(poseStack, entry.icon(), x + ICON_OFFSET, y + ICON_OFFSET);
            if (isSelected) {
                drawSelection(poseStack, x, y, SLOT_AREA);
            }
        }
        private void drawSlot(PoseStack poseStack, int i, int j, int k) {
            RenderSystem.setShaderTexture(0, SLOT_LOCATION);
            poseStack.pushPose();
            poseStack.translate((float)i, (float)j, 0.0F);
            blit(poseStack, 0, 0, 0.0F, 75.0F, 26, 26, 128, 128);
            poseStack.popPose();
        }

        private void drawSelection(PoseStack poseStack, int i, int j, int k) {
            RenderSystem.setShaderTexture(0, SLOT_LOCATION);
            poseStack.pushPose();
            poseStack.translate((float)i, (float)j, 0.0F);
            blit(poseStack, 0, 0, 26.0F, 75.0F, 26, 26, 128, 128);
            poseStack.popPose();
        }
        *///?}
        @Override
        public boolean isInBoundingBox(double x, double y) {
            return x >= (double)this.x() && x < (double)(this.x() + this.width());
        }

        public void setSelected(boolean selected) {
            this.isSelected = selected;
        }
    }
}