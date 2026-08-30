package com.github.mochi7054.client.gui;

import com.github.mochi7054.fluid.SimpleMatterTank;
import com.mojang.blaze3d.systems.RenderSystem;
import mekanism.api.math.FloatingLong;
import mekanism.client.gui.IGuiWrapper;
import mekanism.client.gui.element.GuiElement;
import mekanism.common.capabilities.energy.MachineEnergyContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class ReplicationGuiVerticalPowerBar extends GuiElement {

    private static final ResourceLocation REPLICATION_BACKGROUND = new ResourceLocation("replication", "textures/gui/background.png");

    private final MachineEnergyContainer<?> energyContainer;

    public ReplicationGuiVerticalPowerBar(IGuiWrapper gui, MachineEnergyContainer<?> energyContainer, int x, int y, int height) {
        super(gui, x, y, 4, height);
        this.energyContainer = energyContainer;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        drawBackground(guiGraphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public void drawBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        if (energyContainer == null) return;

        FloatingLong energy = energyContainer.getEnergy();
        FloatingLong max = energyContainer.getMaxEnergy();

        if (max.isZero()) return;

        double ratio = Math.min(1.0, Math.max(0.0, energy.divide(max).doubleValue()));
        int fillHeight = (int) Math.round(this.height * ratio);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        if (fillHeight > 0) {
            int greenColor = 0xFF38FF70;
            guiGraphics.fill(this.relativeX, this.relativeY + this.height - fillHeight, this.relativeX + this.width, this.relativeY + this.height, greenColor);
        }

        RenderSystem.disableBlend();
    }

    @Override
    public void renderToolTip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderToolTip(guiGraphics, mouseX, mouseY);
        if (energyContainer != null) {
            String energyStr = mekanism.common.util.MekanismUtils.getEnergyDisplayShort(energyContainer.getEnergy()).getString();
            String maxStr = mekanism.common.util.MekanismUtils.getEnergyDisplayShort(energyContainer.getMaxEnergy()).getString();
            this.displayTooltips(guiGraphics, mouseX, mouseY, Component.literal(energyStr + " / " + maxStr));
        }
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput output) {}
}