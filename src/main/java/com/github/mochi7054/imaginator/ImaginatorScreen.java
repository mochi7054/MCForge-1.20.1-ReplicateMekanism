package com.github.mochi7054.imaginator;

import com.github.mochi7054.ReplicateMekanism;
import com.github.mochi7054.block.ReplicaTier;
import com.github.mochi7054.client.gui.ReplicationGuiFluidBar;
import com.github.mochi7054.client.gui.ReplicationGuiVerticalPowerBar;
import com.github.mochi7054.fluid.SimpleMatterTank;
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
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import java.util.List;

public class ImaginatorScreen extends GuiConfigurableTile<ImaginatorBlockEntity, ImaginatorMenu> {

    private static final ResourceLocation REPLICATION_BACKGROUND = new ResourceLocation("replication", "textures/gui/background.png");
    private static final ResourceLocation PROGRESS_DOWN_TEXTURE = new ResourceLocation("replicatemekanism", "textures/gui/progress_down.png");
    private static final ResourceLocation CUSTOM_SLOT_TEXTURE = new ResourceLocation("replicatemekanism", "textures/gui/slot.png");
    private static final ResourceLocation SORTING_ICON = new ResourceLocation("mekanism", "gui/sorting.png");

    public ImaginatorScreen(ImaginatorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        ReplicaTier tier = menu.getTileEntity().getTier();
        this.imageWidth = switch (tier) {
            case STANDARD -> 195;
            case BASIC, ADVANCED, ELITE -> 213;
            case ULTIMATE -> 231;
        };
        this.imageHeight = tier == ReplicaTier.STANDARD ? 172 : 182;
        this.inventoryLabelX = switch (tier) {
            case STANDARD, BASIC, ADVANCED -> 8;
            case ELITE -> 10;
            case ULTIMATE -> 29;
        };
        this.inventoryLabelY = tier == ReplicaTier.STANDARD ? 78 : 88;
        this.titleLabelY = 5;
        this.dynamicSlots = true;
    }

    @Override
    protected void addGuiElements() {
        super.addGuiElements();
        ImaginatorBlockEntity tile = menu.getTileEntity();
        ReplicaTier tier = tile.getTier();

        this.addRenderableWidget(new GuiEnergyTab(this, tile.energyContainer, () -> true));
        
        
        

        if (tier != ReplicaTier.STANDARD) {
            this.addRenderableWidget(new GuiImaginatorSortingTab(this, tile));
        }

        int pBarX = this.imageWidth - 12;
        this.addRenderableWidget(new ReplicationGuiVerticalPowerBar(this, tile.energyContainer, pBarX, 17, 50));

        int slotCount = tile.inputSlots.size();
        for (int i = 0; i < slotCount; i++) {
            final int index = i;
            if (tier == ReplicaTier.STANDARD) {
                this.addRenderableWidget(new ReplicationGuiProgress(
                        () -> tile.getProgress(index), this, 102, 42));
            } else {
                int arrowX = tile.getSlotX(i) + 5;
                this.addRenderableWidget(new ReplicationGuiProgressDown(
                        () -> tile.getProgress(index), this, arrowX, 40));
            }
        }

        List<SimpleMatterTank> tanks = tile.getMatterTanks();
        for (int i = 0; i < tanks.size(); i++) {
            SimpleMatterTank tank = tanks.get(i);
            int barY = 17 + i * 7;
            this.addRenderableWidget(new ReplicationGuiFluidBar(this, tank, 8, barY, 44, 6, false));
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

                int x = slot.x - 1;
                int y = slot.y - 1;

                this.addRenderableWidget(new ReplicationGuiSlot(type, this, x, y, slot));
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
                    guiGraphics.blit(overlay.getTexture(), this.relativeX, this.relativeY, 0.0F, 0.0F, overlay.getWidth(), overlay.getHeight(), overlay.getWidth(), overlay.getHeight());
                }
            }
            
            this.drawContents(guiGraphics);
        }
    }

    private static class ReplicationGuiProgress extends mekanism.client.gui.element.GuiElement {
        private final java.util.function.DoubleSupplier progressSupplier;

        public ReplicationGuiProgress(java.util.function.DoubleSupplier progressSupplier, mekanism.client.gui.IGuiWrapper gui, int x, int y) {
            super(gui, x, y, 22, 15);
            this.progressSupplier = progressSupplier;
        }

        @Override
        public void renderWidget(net.minecraft.client.gui.GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
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
        public void drawBackground(net.minecraft.client.gui.GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
            this.renderWidget(guiGraphics, mouseX, mouseY, partialTicks);
        }

        @Override
        public void updateWidgetNarration(NarrationElementOutput output) {}
    }

    private static class ReplicationGuiProgressDown extends mekanism.client.gui.element.GuiElement {
        private final java.util.function.DoubleSupplier progressSupplier;

        public ReplicationGuiProgressDown(java.util.function.DoubleSupplier progressSupplier, mekanism.client.gui.IGuiWrapper gui, int x, int y) {
            super(gui, x, y, 8, 15);
            this.progressSupplier = progressSupplier;
        }

        @Override
        public void renderWidget(net.minecraft.client.gui.GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
            guiGraphics.blit(PROGRESS_DOWN_TEXTURE, this.relativeX, this.relativeY, 0, 0, 8, 15, 16, 15);
            double progress = progressSupplier.getAsDouble();
            if (progress > 0) {
                int height = (int) (progress * 15);
                if (height > 0) {
                    guiGraphics.blit(PROGRESS_DOWN_TEXTURE, this.relativeX, this.relativeY, 8, 0, 8, height, 16, 15);
                }
            }
        }

        @Override
        public void drawBackground(net.minecraft.client.gui.GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
            this.renderWidget(guiGraphics, mouseX, mouseY, partialTicks);
        }

        @Override
        public void updateWidgetNarration(NarrationElementOutput output) {}
    }

    private void drawMachineArea(net.minecraft.client.gui.GuiGraphics guiGraphics, int x, int y, int width, boolean standard) {
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

    private void drawInventoryArea(net.minecraft.client.gui.GuiGraphics guiGraphics, int left, int top, int width, int xOffset) {
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
    protected void renderBg(net.minecraft.client.gui.GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        mekanism.client.render.MekanismRenderer.resetColor(guiGraphics);
        if (this.getXSize() < 8 || this.getYSize() < 8) {
            return;
        }
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

        private static class GuiImaginatorSortingTab extends mekanism.client.gui.element.GuiInsetElement<ImaginatorBlockEntity> {
        @Override
        public void updateWidgetNarration(NarrationElementOutput output) {}

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
            drawBackground(guiGraphics, mouseX, mouseY, partialTicks);
        }

        public GuiImaginatorSortingTab(mekanism.client.gui.IGuiWrapper gui, ImaginatorBlockEntity tile) {
            super(SORTING_ICON, gui, tile, -26, 62, 35, 18, true);
        }

        @Override
        public void renderToolTip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
            super.renderToolTip(guiGraphics, mouseX, mouseY);
            this.displayTooltips(guiGraphics, mouseX, mouseY, Component.translatable("gui.replicatemekanism.task_sharing"));
        }

        @Override
        protected void colorTab(GuiGraphics guiGraphics) {
            mekanism.client.render.MekanismRenderer.color(guiGraphics, mekanism.client.SpecialColors.TAB_FACTORY_SORT);
        }

        @Override
        public void drawBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
            super.drawBackground(guiGraphics, mouseX, mouseY, partialTicks);
            Component stateText = mekanism.common.util.text.BooleanStateDisplay.OnOff.of(dataSource.sorting).getTextComponent();
            this.drawTextScaledBound(guiGraphics, stateText, this.relativeX + 3, this.relativeY + 24, this.titleTextColor(), 21.0f);
        }

        @Override
        public void onClick(double mouseX, double mouseY, int button) {
            ReplicateMekanism.PACKET_HANDLER.sendToServer(
                new com.github.mochi7054.network.ToggleAutoSortPacket(dataSource.getBlockPos())
            );
        }
    }
}