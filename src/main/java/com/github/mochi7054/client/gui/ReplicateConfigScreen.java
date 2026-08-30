package com.github.mochi7054.client.gui;

import com.github.mochi7054.config.Config;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.gui.widget.ForgeSlider;

public class ReplicateConfigScreen extends Screen {

    private final Screen parent;
    private ForgeSlider maxStackSlider;

    public ReplicateConfigScreen(Screen parent) {
        super(Component.translatable("replicatemekanism.configuration.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        int currentVal = Config.getReplicaUpgradeMaxStack();

        // 1 - 8 のスライダー
        this.maxStackSlider = new ForgeSlider(
                this.width / 2 - 120, this.height / 2 - 20,
                240, 20,
                Component.translatable("replicatemekanism.configuration.replicaUpgradeMaxStack").append(": "),
                Component.empty(),
                1.0, 8.0,
                (double) currentVal,
                1.0, 0, true
        );
        this.addRenderableWidget(this.maxStackSlider);

        // 完了 / 保存ボタン
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> {
            int newVal = this.maxStackSlider.getValueInt();
            Config.REPLICA_UPGRADE_MAX_STACK.set(newVal);
            Config.SPEC.save();
            if (this.minecraft != null) {
                this.minecraft.setScreen(this.parent);
            }
        }).bounds(this.width / 2 - 100, this.height / 2 + 30, 200, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);

        Component tooltip = Component.translatable("replicatemekanism.configuration.replicaUpgradeMaxStack.tooltip");
        graphics.drawCenteredString(this.font, tooltip, this.width / 2, this.height / 2 - 40, 0xAAAAAA);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }
}