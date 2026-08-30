package com.github.mochi7054.client.gui;

import com.buuz135.replication.api.IMatterType;
import com.github.mochi7054.fluid.SimpleMatterTank;
import com.mojang.blaze3d.systems.RenderSystem;
import mekanism.client.gui.IGuiWrapper;
import mekanism.client.gui.element.GuiElement;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class ReplicationGuiFluidBar extends GuiElement {

    private static final ResourceLocation REPLICATION_BACKGROUND = new ResourceLocation("replication", "textures/gui/background.png");

    private final SimpleMatterTank tank;
    private final boolean horizontal;

    public ReplicationGuiFluidBar(IGuiWrapper gui, SimpleMatterTank tank, int x, int y, int width, int height, boolean horizontal) {
        super(gui, x, y, width, height);
        this.tank = tank;
        this.horizontal = horizontal;
    }

    

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {}

    @Override
    public void drawBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        double stored = tank.getStored();
        double capacity = tank.getCapacity();

        if (capacity <= 0 || stored <= 0.001) {
            return;
        }

        double ratio = Math.min(1.0, Math.max(0.0, stored / capacity));
        int barColor = getColorForMatter(tank.getMatterType());

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        if (horizontal) {
            int fillWidth = (int) Math.round(this.width * ratio);
            if (fillWidth > 0) {
                guiGraphics.fill(this.relativeX, this.relativeY, this.relativeX + fillWidth, this.relativeY + this.height, barColor);
            }
        } else {
            int fillHeight = (int) Math.round(this.height * ratio);
            if (fillHeight > 0) {
                guiGraphics.fill(this.relativeX, this.relativeY + this.height - fillHeight, this.relativeX + this.width, this.relativeY + this.height, barColor);
            }
        }

        RenderSystem.disableBlend();
    }

    @Override
    public void renderToolTip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderToolTip(guiGraphics, mouseX, mouseY);
        IMatterType type = tank.getMatterType();
        String name = type != null ? type.getName() : "Empty";
        String text = String.format("%s: %.1f / %.1f", name, tank.getStored(), (double) tank.getCapacity());
        this.displayTooltips(guiGraphics, mouseX, mouseY, Component.literal(text));
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput output) {}

    private int getColorForMatter(IMatterType type) {
        if (type == null) return 0xFF555555;
        try {
            float[] c = type.getColor().get();
            if (c != null && c.length >= 3) {
                int r = (int) (c[0] * 255.0f);
                int g = (int) (c[1] * 255.0f);
                int b = (int) (c[2] * 255.0f);
                return 0xFF000000 | (r << 16) | (g << 8) | b;
            }
        } catch (Exception ignored) {}
        return 0xFF38FF70;
    }
}