package com.github.mochi7054.collapser;

import com.github.mochi7054.block.ReplicaTier;
import com.github.mochi7054.client.gui.ReplicationGuiFluidBar;
import com.github.mochi7054.client.gui.ReplicationGuiVerticalPowerBar;
import com.github.mochi7054.network.ToggleAutoSortPacket;
import mekanism.client.gui.GuiConfigurableTile;
import mekanism.client.gui.element.slot.GuiSlot;
import mekanism.client.gui.element.slot.SlotType;
import mekanism.client.gui.element.tab.GuiEnergyTab;
import mekanism.common.inventory.container.slot.ContainerSlotType;
import mekanism.common.inventory.container.slot.InventoryContainerSlot;
import mekanism.common.tile.component.config.DataType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public class CollapserScreen extends GuiConfigurableTile<CollapserBlockEntity, CollapserMenu> {

    private static final ResourceLocation REPLICATION_BACKGROUND = new ResourceLocation("replication", "textures/gui/background.png");
    private static final ResourceLocation PROGRESS_DOWN_TEXTURE = new ResourceLocation("replicatemekanism", "textures/gui/progress_down.png");
    private static final ResourceLocation CUSTOM_SLOT_TEXTURE = new ResourceLocation("replicatemekanism", "textures/gui/slot.png");
    private static final ResourceLocation SORTING_ICON = new ResourceLocation("mekanism", "gui/sorting.png");

    public CollapserScreen(CollapserMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        ReplicaTier tier = menu.getTileEntity().getTier();
        this.imageWidth = switch (tier) {
            case STANDARD, BASIC, ADVANCED -> 174;
            case ELITE -> 180;
            case ULTIMATE -> 218;
        };
        this.imageHeight = tier == ReplicaTier.STANDARD ? 174 : 184;
        this.inventoryLabelX = switch (tier) {
            case STANDARD, BASIC, ADVANCED -> 8;
            case ELITE -> 10;
            case ULTIMATE -> 29;
        };
        this.inventoryLabelY = tier == ReplicaTier.STANDARD ? 82 : 92;
        this.titleLabelY = tier == ReplicaTier.STANDARD ? 10 : 7;
        this.dynamicSlots = true;
    }

    @Override
    protected void addGuiElements() {
        super.addGuiElements();
        CollapserBlockEntity tile = menu.getTileEntity();
        ReplicaTier tier = tile.getTier();

        if (tier == ReplicaTier.STANDARD) {
            this.addRenderableWidget(new ReplicationGuiFluidBar(this, tile.earthTank, 70, 25, 5, 42, false));
            this.addRenderableWidget(new ReplicationGuiFluidBar(this, tile.netherTank, 78, 25, 5, 42, false));
            this.addRenderableWidget(new ReplicationGuiFluidBar(this, tile.organicTank, 86, 25, 5, 42, false));
            this.addRenderableWidget(new ReplicationGuiFluidBar(this, tile.enderTank, 94, 25, 5, 42, false));
            this.addRenderableWidget(new ReplicationGuiFluidBar(this, tile.metallicTank, 102, 25, 5, 42, false));
            this.addRenderableWidget(new ReplicationGuiFluidBar(this, tile.preciousTank, 110, 25, 5, 42, false));
            this.addRenderableWidget(new ReplicationGuiFluidBar(this, tile.livingTank, 118, 25, 5, 42, false));
            this.addRenderableWidget(new ReplicationGuiFluidBar(this, tile.quantumTank, 126, 25, 5, 42, false));

            this.addRenderableWidget(new ReplicationGuiVerticalPowerBar(this, tile.energyContainer, 162, 25, 42));
            this.addRenderableWidget(new CollapserGuiProgress(() -> tile.getProgress(0), this, 41, 41));
        } else {
            int fluidStartX = (this.imageWidth - 142) / 2;
            this.addRenderableWidget(new ReplicationGuiFluidBar(this, tile.earthTank, fluidStartX, 84, 16, 5, true));
            this.addRenderableWidget(new ReplicationGuiFluidBar(this, tile.netherTank, fluidStartX + 18, 84, 16, 5, true));
            this.addRenderableWidget(new ReplicationGuiFluidBar(this, tile.organicTank, fluidStartX + 36, 84, 16, 5, true));
            this.addRenderableWidget(new ReplicationGuiFluidBar(this, tile.enderTank, fluidStartX + 54, 84, 16, 5, true));
            this.addRenderableWidget(new ReplicationGuiFluidBar(this, tile.metallicTank, fluidStartX + 72, 84, 16, 5, true));
            this.addRenderableWidget(new ReplicationGuiFluidBar(this, tile.preciousTank, fluidStartX + 90, 84, 16, 5, true));
            this.addRenderableWidget(new ReplicationGuiFluidBar(this, tile.livingTank, fluidStartX + 108, 84, 16, 5, true));
            this.addRenderableWidget(new ReplicationGuiFluidBar(this, tile.quantumTank, fluidStartX + 126, 84, 16, 5, true));

            this.addRenderableWidget(new ReplicationGuiVerticalPowerBar(this, tile.energyContainer, this.imageWidth - 12, 25, 42));

            for (int i = 0; i < tile.getTier().getSlots(); i++) {
                final int idx = i;
                int arrowX = tile.getSlotX(i) + 5;
                this.addRenderableWidget(new ReplicationGuiProgressDown(() -> tile.getProgress(idx), this, arrowX, 39));
            }
        }

        this.addRenderableWidget(new GuiEnergyTab(this, tile.energyContainer, () -> true));

        if (tier != ReplicaTier.STANDARD) {
            this.addRenderableWidget(new GuiCollapserSortingTab(this, tile));
        }

        for (net.minecraft.client.gui.components.events.GuiEventListener listener : this.children()) {
            if (listener instanceof mekanism.client.gui.element.GuiElement element) {
                if (element.getClass().getName().contains("GuiSideHolder")
                        && element.getRelativeX() < 0 && element.getRelativeY() < 10) {
                    element.visible = false;
                }
            }
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
                } else if (slotType == ContainerSlotType.INPUT || slotType == ContainerSlotType.OUTPUT
                        || slotType == ContainerSlotType.EXTRA) {
                    type = SlotType.NORMAL;
                } else if (slotType == ContainerSlotType.POWER) {
                    type = SlotType.POWER;
                } else {
                    type = SlotType.NORMAL;
                }

                GuiSlot guiSlot = new CollapserGuiSlot(type, this, slot.x - 1, slot.y - 1, containerSlot);

                boolean isPlayerSlot = slot instanceof mekanism.common.inventory.container.slot.MainInventorySlot ||
                                       slot instanceof mekanism.common.inventory.container.slot.HotBarSlot;

                if (!isPlayerSlot && (slotType == ContainerSlotType.IGNORED
                        || containerSlot instanceof mekanism.common.inventory.container.slot.VirtualInventoryContainerSlot)) {
                    guiSlot.visible = false;
                }

                containerSlot.addWarnings(guiSlot);
                mekanism.common.inventory.container.slot.SlotOverlay overlay = containerSlot.getSlotOverlay();
                if (overlay != null) {
                    guiSlot.with(overlay);
                }

                this.addRenderableWidget(guiSlot);
            } else {
                GuiSlot guiSlot = new CollapserGuiSlot(SlotType.NORMAL, this, slot.x - 1, slot.y - 1, slot);
                this.addRenderableWidget(guiSlot);
            }
        }
    }

    private static class CollapserGuiSlot extends GuiSlot {
        private final Slot slot;

        public CollapserGuiSlot(SlotType type, mekanism.client.gui.IGuiWrapper gui, int x, int y, Slot slot) {
            super(type, gui, x, y);
            this.slot = slot;
        }

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {}

        @Override
        public void drawBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
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
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {}

        @Override
        public void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput output) {}

        @Override
        public void drawBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
            guiGraphics.blit(REPLICATION_BACKGROUND, this.relativeX, this.relativeY, 177, 61, 22, 15, 256, 256);
            double progress = progressSupplier.getAsDouble();
            if (progress > 0) {
                int width = (int) (progress * 22);
                if (width > 0) {
                    guiGraphics.blit(REPLICATION_BACKGROUND, this.relativeX, this.relativeY, 177, 77, width, 15, 256, 256);
                }
            }
        }
    }

    private static class ReplicationGuiProgressDown extends mekanism.client.gui.element.GuiElement {
        private final java.util.function.DoubleSupplier progressSupplier;

        public ReplicationGuiProgressDown(java.util.function.DoubleSupplier progressSupplier, mekanism.client.gui.IGuiWrapper gui, int x, int y) {
            super(gui, x, y, 8, 15);
            this.progressSupplier = progressSupplier;
        }

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {}

        @Override
        public void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput output) {}

        @Override
        public void drawBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
            guiGraphics.blit(PROGRESS_DOWN_TEXTURE, this.relativeX, this.relativeY, 0, 0, 8, 15, 16, 15);
            double progress = progressSupplier.getAsDouble();
            if (progress > 0) {
                int height = (int) (progress * 15);
                if (height > 0) {
                    guiGraphics.blit(PROGRESS_DOWN_TEXTURE, this.relativeX, this.relativeY, 8, 0, 8, height, 16, 15);
                }
            }
        }
    }

    private void drawMachineArea(GuiGraphics guiGraphics, int x, int y, int width, boolean standard) {
        if (standard) {
            guiGraphics.blit(REPLICATION_BACKGROUND, x, y, 0, 0, width, 88);
        } else {
            guiGraphics.blit(REPLICATION_BACKGROUND, x, y, 0, 0, 8, 80);
            for (int dx = 8; dx < width - 6; dx++) {
                guiGraphics.blit(REPLICATION_BACKGROUND, x + dx, y, 150, 0, 1, 80);
            }
            guiGraphics.blit(REPLICATION_BACKGROUND, x + width - 6, y, 168, 0, 6, 80);

            for (int dy = 0; dy < 10; dy++) {
                int curY = y + 80 + dy;
                guiGraphics.blit(REPLICATION_BACKGROUND, x, curY, 0, 80, 8, 1);
                for (int dx = 8; dx < width - 6; dx++) {
                    guiGraphics.blit(REPLICATION_BACKGROUND, x + dx, curY, 150, 80, 1, 1);
                }
                guiGraphics.blit(REPLICATION_BACKGROUND, x + width - 6, curY, 168, 80, 6, 1);
            }

            int curY = y + 90;
            guiGraphics.blit(REPLICATION_BACKGROUND, x, curY, 0, 80, 8, 8);
            for (int dx = 8; dx < width - 6; dx++) {
                guiGraphics.blit(REPLICATION_BACKGROUND, x + dx, curY, 150, 80, 1, 8);
            }
            guiGraphics.blit(REPLICATION_BACKGROUND, x + width - 6, curY, 168, 80, 6, 8);
        }
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
        if (this.getXSize() < 8 || this.getYSize() < 8) return;
        ReplicaTier tier = menu.getTileEntity().getTier();
        boolean standard = tier == ReplicaTier.STANDARD;

        int xOffset = switch (tier) {
            case STANDARD, BASIC, ADVANCED -> 8;
            case ELITE -> 10;
            case ULTIMATE -> 29;
        };

        drawMachineArea(guiGraphics, this.leftPos, this.topPos, this.imageWidth, standard);
        drawInventoryArea(guiGraphics, this.leftPos, this.topPos + (standard ? 88 : 98), this.imageWidth, xOffset);
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
        public GuiCollapserSortingTab(mekanism.client.gui.IGuiWrapper gui, CollapserBlockEntity tile) {
            super(SORTING_ICON, gui, tile, -26, 62, 35, 18, true);
        }

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {}

        @Override
        public void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput output) {}

        @Override
        protected void colorTab(GuiGraphics guiGraphics) {
            mekanism.client.render.MekanismRenderer.color(guiGraphics, mekanism.client.SpecialColors.TAB_FACTORY_SORT);
        }

        @Override
        public void drawBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
            super.drawBackground(guiGraphics, mouseX, mouseY, partialTicks);
            Component stateText = mekanism.common.util.text.BooleanStateDisplay.OnOff.of(dataSource.sorting).getTextComponent();
            drawTextScaledBound(guiGraphics, stateText, this.relativeX + 3, this.relativeY + 24, this.titleTextColor(), 21.0F);
        }

        @Override
        public void renderToolTip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
            super.renderToolTip(guiGraphics, mouseX, mouseY);
            this.displayTooltips(guiGraphics, mouseX, mouseY, mekanism.common.MekanismLang.AUTO_SORT.translate());
        }

        @Override
        public void onClick(double mouseX, double mouseY, int button) {
            com.github.mochi7054.ReplicateMekanism.PACKET_HANDLER.sendToServer(
                new ToggleAutoSortPacket(dataSource.getBlockPos())
            );
        }
    }
}
