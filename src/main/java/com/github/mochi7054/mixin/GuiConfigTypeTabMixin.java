package com.github.mochi7054.mixin;

import mekanism.client.gui.element.tab.GuiConfigTypeTab;
import mekanism.client.gui.element.window.GuiSideConfiguration;
import mekanism.common.lib.transmitter.TransmissionType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.github.mochi7054.imaginator.ImaginatorBlockEntity;
import com.github.mochi7054.collapser.CollapserBlockEntity;

@Mixin(value = GuiConfigTypeTab.class, remap = false)
public abstract class GuiConfigTypeTabMixin {

    @Shadow private TransmissionType transmission;
    @Shadow private GuiSideConfiguration<?> config;

    @Inject(method = "renderToolTip", at = @At("HEAD"), cancellable = true)
    private void onRenderToolTip(GuiGraphics guiGraphics, int mouseX, int mouseY, CallbackInfo ci) {
        if (transmission == TransmissionType.FLUID) {
            try {
                java.lang.reflect.Field tileField = GuiSideConfiguration.class.getDeclaredField("tile");
                tileField.setAccessible(true);
                Object tile = tileField.get(config);
                if (tile instanceof ImaginatorBlockEntity || tile instanceof CollapserBlockEntity) {
                    ((GuiConfigTypeTab) (Object) this).displayTooltips(guiGraphics, mouseX, mouseY, Component.translatable("replicatemekanism.matter"));
                    ci.cancel();
                }
            } catch (Exception e) {
                // Ignore
            }
        }
    }
}