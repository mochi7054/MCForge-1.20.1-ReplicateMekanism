package com.github.mochi7054.client.gui;

import mekanism.api.energy.IEnergyContainer;
import mekanism.client.gui.IGuiWrapper;
import mekanism.client.gui.element.bar.GuiVerticalPowerBar;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public class ReplicationGuiVerticalPowerBar extends GuiVerticalPowerBar {

    private static final ResourceLocation POWER_BAR_OVERLAY = new ResourceLocation("replicatemekanism", "textures/gui/bar/vertical_power.png");

    public ReplicationGuiVerticalPowerBar(IGuiWrapper gui, IEnergyContainer container, int x, int y, int height) {
        super(gui, container, x, y, height);
    }

    @Override
    public void drawBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        int x = this.relativeX;
        int y = this.relativeY;
        int w = 6;
        int h = this.height;

        // Draw custom Replication dark background inside the bar
        guiGraphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF181A24);

        // Draw the energy fill overlay
        double level = getHandler().getLevel();
        if (level > 0) {
            renderBarOverlay(guiGraphics, mouseX, mouseY, partialTicks, level);
        }

        // Draw Replication green border
        drawReplicationBorder(guiGraphics);
    }

    @Override
    protected ResourceLocation getResource() {
        return POWER_BAR_OVERLAY;
    }

    private void drawReplicationBorder(GuiGraphics guiGraphics) {
        int x = this.relativeX;
        int y = this.relativeY;
        int w = 6;
        int h = this.height;

        int lightGreen = 0xFF72E567;
        int darkGreen = 0xFF158C82;
        int cornerColor = 0xFF19A683;

        guiGraphics.fill(x + 1, y, x + w - 1, y + 1, darkGreen);
        guiGraphics.fill(x, y + 1, x + 1, y + h - 1, darkGreen);
        guiGraphics.fill(x + 1, y + h - 1, x + w - 1, y + h, lightGreen);
        guiGraphics.fill(x + w - 1, y + 1, x + w, y + h - 1, lightGreen);

        guiGraphics.fill(x, y, x + 1, y + 1, cornerColor);
        guiGraphics.fill(x + w - 1, y, x + w, y + 1, cornerColor);
        guiGraphics.fill(x, y + h - 1, x + 1, y + h, cornerColor);
        guiGraphics.fill(x + w - 1, y + h - 1, x + w, y + h, cornerColor);
    }
}
