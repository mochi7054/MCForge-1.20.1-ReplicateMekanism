package com.github.mochi7054.collapser;

import com.github.mochi7054.ReplicateMekanism;
import com.github.mochi7054.block.ReplicaTier;
import com.github.mochi7054.client.gui.ReplicationGuiFluidBar;
import com.github.mochi7054.client.gui.ReplicationGuiVerticalPowerBar;
import com.github.mochi7054.fluid.SimpleMatterTank;
import com.mojang.blaze3d.systems.RenderSystem;
import mekanism.api.math.FloatingLong;
import mekanism.client.gui.GuiConfigurableTile;
import mekanism.client.gui.element.slot.GuiSlot;
import mekanism.client.gui.element.slot.SlotType;
import mekanism.client.gui.element.tab.GuiEnergyTab;
import mekanism.client.gui.element.tab.GuiRedstoneControlTab;
import mekanism.client.gui.element.tab.GuiSecurityTab;
import mekanism.client.gui.element.tab.window.GuiUpgradeWindowTab;
import mekanism.common.inventory.container.slot.ContainerSlotType;
import mekanism.common.inventory.container.slot.InventoryContainerSlot;
import mekanism.common.tile.component.config.DataType;
import mekanism.common.util.text.BooleanStateDisplay;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import java.util.List;

public class CollapserScreen extends GuiConfigurableTile<CollapserBlockEntity, CollapserMenu> {

    private static final ResourceLocation REPLICATION_BACKGROUND = new ResourceLocation("replication", "textures/gui/background.png");
    private static final ResourceLocation PROGRESS_DOWN_TEXTURE = new ResourceLocation("replicatemekanism", "textures/gui/progress_down.png");
    private static final ResourceLocation CUSTOM_SLOT_TEXTURE = new ResourceLocation("replicatemekanism", "textures/gui/slot.png");

    public CollapserScreen(CollapserMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 174;
        this.imageHeight = 220;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 127;
        this.titleLabelY = 5;
        this.dynamicSlots = true;
    }

    @Override
    protected void addGuiElements() {
        super.addGuiElements();
        CollapserBlockEntity tile = menu.getTileEntity();
        ReplicaTier tier = tile.getReplicaTier();

        this.addRenderableWidget(new GuiEnergyTab(this, tile.energyContainer, () -> true));
        
        
        

        if (tier != ReplicaTier.STANDARD) {
            this.addRenderableWidget(new GuiCollapserSortingTab(this, tile));
        }

        this.addRenderableWidget(new ReplicationGuiVerticalPowerBar(this, tile.energyContainer, 162, 17, 98));

        int slotCount = tile.inputSlots.size();
        for (int i = 0; i < slotCount; i++) {
            final int index = i;
            if (tier == ReplicaTier.STANDARD) {
                this.addRenderableWidget(new CollapserGuiProgress(
                        () -> tile.getProgress(index), this, 66, 42));
            } else {
                int arrowX = tile.getSlotX(i) + 5;
                this.addRenderableWidget(new ReplicationGuiProgressDown(
                        () -> tile.getProgress(index), this, arrowX, 40));
            }
        }

        List<SimpleMatterTank> tanks = tile.getMatterTanks();
        for (int i = 0; i < tanks.size(); i++) {
            SimpleMatterTank tank = tanks.get(i);
            int barY = 60 + i * 7;
            this.addRenderableWidget(new ReplicationGuiFluidBar(this, tank, 30, barY, 126, 6, true));
        }
    }

    @Override
    protected void addSlots() {
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot slot = menu.slots.get(i);

            if (slot instanceof InventoryContainerSlot containerSlot) {
                ContainerSlotType slotType = containerSlot.getSlotType();

                DataType dataType = findDataType(containerSlot);
                SlotType type;
                if (dataType != null) {
                    type = SlotType.get(dataType);
                } else if (slotType == ContainerSlotType.INPUT ||
                        slotType == ContainerSlotType.OUTPUT ||
                        slotType == ContainerSlotType.EXTRA) {
                    type = SlotType.NORMAL;
                } else if (slotType == ContainerSlotType.POWER) {
                    type = SlotType.POWER;
                } else {
                    type = SlotType.NORMAL;
                }

                GuiSlot guiSlot = new ReplicationGuiSlot(type, this, slot.x - 1, slot.y - 1, containerSlot);

                boolean isPlayerSlot = slot instanceof mekanism.common.inventory.container.slot.MainInventorySlot ||
                        slot instanceof mekanism.common.inventory.container.slot.HotBarSlot;

                if (!isPlayerSlot && (slotType == ContainerSlotType.IGNORED ||
                        containerSlot instanceof mekanism.common.inventory.container.slot.VirtualInventoryContainerSlot)) {
                    guiSlot.visible = false;
                }

                containerSlot.addWarnings(guiSlot);
                mekanism.common.inventory.container.slot.SlotOverlay overlay = containerSlot.getSlotOverlay();
                if (overlay != null) {
                    guiSlot.with(overlay);
                }

                this.addRenderableWidget(guiSlot);
            } else {
                GuiSlot guiSlot = new ReplicationGuiSlot(SlotType.NORMAL, this, slot.x - 1, slot.y - 1, slot);
                this.addRenderableWidget(guiSlot);
            }
        }
    }

    private static class ReplicationGuiSlot extends GuiSlot {
        private final Slot slot;

        public ReplicationGuiSlot(SlotType type, mekanism.client.gui.IGuiWrapper gui, int x, int y, Slot slot) {
            super(type, gui, x, y);
            this.slot = slot;
        }

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
            if (!this.isRenderAboveSlots()) {
                this.customDraw(guiGraphics);
            }
        }

        @Override
        public void drawBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
            if (this.isRenderAboveSlots()) {
                this.customDraw(guiGraphics);
            }
        }

        private boolean isRenderAboveSlots() {
            try {
                java.lang.reflect.Field field = GuiSlot.class.getDeclaredField("renderAboveSlots");
                field.setAccessible(true);
                return field.getBoolean(this);
            } catch (Exception e) {
                return false;
            }
        }

        private void customDraw(GuiGraphics guiGraphics) {
            guiGraphics.blit(CUSTOM_SLOT_TEXTURE, this.relativeX, this.relativeY, 0, 0, 18, 18, 18, 18);

            if (slot instanceof InventoryContainerSlot containerSlot) {
                mekanism.common.inventory.container.slot.SlotOverlay overlay = containerSlot.getSlotOverlay();
                if (overlay != null) {
                    guiGraphics.blit(overlay.getTexture(), this.relativeX, this.relativeY,
                            0f, 0f, overlay.getWidth(), overlay.getHeight(),
                            overlay.getWidth(), overlay.getHeight());
                }
            }
            this.drawContents(guiGraphics);
        }
    }

    private static class CollapserGuiProgress extends mekanism.client.gui.element.GuiElement {
        private final java.util.function.DoubleSupplier progressSupplier;

        public CollapserGuiProgress(java.util.function.DoubleSupplier progressSupplier,
                                    mekanism.client.gui.IGuiWrapper gui, int x, int y) {
            super(gui, x, y, 22, 15);
            this.progressSupplier = progressSupplier;
        }

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
            guiGraphics.blit(REPLICATION_BACKGROUND, this.relativeX, this.relativeY, 177, 61, 22, 15, 256, 256);
            double progress = progressSupplier.getAsDouble();
            if (progress > 0) {
                int width = (int) (progress * 22);
                if (width > 0) {
                    guiGraphics.blit(REPLICATION_BACKGROUND, this.relativeX, this.relativeY, 177, 77, width, 15, 256, 256);
                }
            }
        }

        @Override
        public void drawBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
            this.renderWidget(guiGraphics, mouseX, mouseY, partialTicks);
        }

        @Override
        public void updateWidgetNarration(NarrationElementOutput output) {
        }
    }

    private static class ReplicationGuiProgressDown extends mekanism.client.gui.element.GuiElement {
        private final java.util.function.DoubleSupplier progressSupplier;

        public ReplicationGuiProgressDown(java.util.function.DoubleSupplier progressSupplier, mekanism.client.gui.IGuiWrapper gui, int x, int y) {
            super(gui, x, y, 8, 15);
            this.progressSupplier = progressSupplier;
        }

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
            guiGraphics.blit(PROGRESS_DOWN_TEXTURE, this.relativeX, this.relativeY, 0, 0, 8, 15, 16, 15);
            double progress = progressSupplier.getAsDouble();
            if (progress > 0) {
                int fillHeight = (int) Math.round(progress * 15);
                if (fillHeight > 0) {
                    guiGraphics.blit(PROGRESS_DOWN_TEXTURE, this.relativeX, this.relativeY, 8, 0, 8, fillHeight, 16, 15);
                }
            }
        }

        @Override
        public void drawBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
            this.renderWidget(guiGraphics, mouseX, mouseY, partialTicks);
        }

        @Override
        public void updateWidgetNarration(NarrationElementOutput output) {
        }
    }

    private void drawMachineArea(GuiGraphics guiGraphics, int x, int y, int width) {
        guiGraphics.blit(REPLICATION_BACKGROUND, x, y, 0, 0, width, 130);
    }

    private void drawInventoryArea(GuiGraphics guiGraphics, int left, int top, int width, int xOffset) {
        guiGraphics.blit(REPLICATION_BACKGROUND, left, top, 0, 96, 6, 4);
        for (int x = 6; x < width - 6; x++) {
            guiGraphics.blit(REPLICATION_BACKGROUND, left + x, top, 150, 96, 1, 4);
        }
        guiGraphics.blit(REPLICATION_BACKGROUND, left + width - 6, top, 168, 96, 6, 4);

        int midY = top + 4;
        guiGraphics.blit(REPLICATION_BACKGROUND, left, midY, 0, 100, 6, 80);
        guiGraphics.fill(left + 6, midY, left + width - 6, midY + 76, 0xFF252A37);
        guiGraphics.blit(REPLICATION_BACKGROUND, left + width - 6, midY, 168, 100, 6, 80);

        int botY = top + 80;
        guiGraphics.blit(REPLICATION_BACKGROUND, left, botY, 0, 176, 6, 6);
        for (int x = 6; x < width - 6; x++) {
            guiGraphics.blit(REPLICATION_BACKGROUND, left + x, botY, 150, 176, 1, 6);
        }
        guiGraphics.blit(REPLICATION_BACKGROUND, left + width - 6, botY, 168, 176, 6, 6);

        if (xOffset > 6) {
            guiGraphics.fill(left + 6, botY, left + xOffset, botY + 2, 0xFF252A37);
        }
        if (left + xOffset + 162 < left + width - 6) {
            guiGraphics.fill(left + xOffset + 161, botY, left + width - 5, botY + 2, 0xFF252A37);
            guiGraphics.fill(left + width - 6, top, left + width - 5, botY, 0xFF252A37);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        mekanism.client.render.MekanismRenderer.resetColor(guiGraphics);
        if (this.getXSize() < 8 || this.getYSize() < 8) {
            return;
        }
        drawMachineArea(guiGraphics, this.leftPos, this.topPos, this.imageWidth);
        drawInventoryArea(guiGraphics, this.leftPos, this.topPos + 130, this.imageWidth, 8);
    }

    @Override
    protected void drawForegroundText(GuiGraphics graphics, int mouseX, int mouseY) {
        int titleWidth = this.font.width(this.title);
        int titleX = (this.imageWidth - titleWidth) / 2;
        graphics.drawString(this.font, this.title, titleX, this.titleLabelY, 0xFF38FF70, false);
        graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xFF38FF70, false);
        super.drawForegroundText(graphics, mouseX, mouseY);
    }

        private static class GuiCollapserSortingTab extends mekanism.client.gui.element.GuiInsetElement<CollapserBlockEntity> {
        @Override
        public void updateWidgetNarration(NarrationElementOutput output) {}

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
            drawBackground(guiGraphics, mouseX, mouseY, partialTicks);
        }

        public GuiCollapserSortingTab(mekanism.client.gui.IGuiWrapper gui, CollapserBlockEntity tile) {
            super(new ResourceLocation("mekanism", "gui/sorting.png"), gui, tile, -26, 62, 35, 18, true);
        }

        @Override
        public void drawBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
            super.drawBackground(guiGraphics, mouseX, mouseY, partialTicks);
            Component stateText = BooleanStateDisplay.OnOff.of(this.dataSource.sorting).getTextComponent();
            this.drawTextScaledBound(guiGraphics, stateText, this.relativeX + 3, this.relativeY + 24, this.titleTextColor(), 21.0f);
        }

        @Override
        public void renderToolTip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
            super.renderToolTip(guiGraphics, mouseX, mouseY);
            this.displayTooltips(guiGraphics, mouseX, mouseY, mekanism.common.MekanismLang.AUTO_SORT.translate());
        }

        @Override
        protected void colorTab(GuiGraphics guiGraphics) {
            mekanism.client.render.MekanismRenderer.color(guiGraphics, mekanism.client.SpecialColors.TAB_FACTORY_SORT);
        }

        @Override
        public void onClick(double mouseX, double mouseY, int button) {
            ReplicateMekanism.PACKET_HANDLER.sendToServer(new com.github.mochi7054.network.ToggleAutoSortPacket(this.dataSource.getBlockPos()));
        }
    }
}