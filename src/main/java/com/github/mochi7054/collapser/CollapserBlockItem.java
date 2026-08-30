package com.github.mochi7054.collapser;

import mekanism.common.item.block.ItemBlockTooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class CollapserBlockItem extends ItemBlockTooltip<CollapserBlock> {

    public CollapserBlockItem(CollapserBlock block, Properties properties) {
        super(block, true, properties);
    }

    @Override
    public net.minecraft.network.chat.MutableComponent getName(ItemStack stack) {
        TextColor tierColor = ((CollapserBlock) getBlock()).getReplicaTier().getTextColor();
        return super.getName(stack).copy().withStyle(style -> style.withColor(tierColor));
    }

    @Override
    protected void addDetails(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        super.addDetails(stack, level, tooltip, flag);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        if (net.minecraft.client.gui.screens.Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("description.replicatemekanism.collapser").withStyle(net.minecraft.ChatFormatting.GRAY));
        }
    }
}