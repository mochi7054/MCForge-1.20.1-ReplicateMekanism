package com.github.mochi7054.forensic;

import mekanism.common.item.block.ItemBlockTooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class ForensicChamberBlockItem extends ItemBlockTooltip<ForensicChamberBlock> {

    private static final TextColor FORENSIC_COLOR = TextColor.fromRgb(0x55FFFF);

    public ForensicChamberBlockItem(ForensicChamberBlock block, Properties properties) {
        super(block, true, properties);
    }

    @Override
    public net.minecraft.network.chat.MutableComponent getName(ItemStack stack) {
        return super.getName(stack).copy().withStyle(style -> style.withColor(FORENSIC_COLOR));
    }

    @Override
    protected void addDetails(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        super.addDetails(stack, level, tooltip, flag);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        if (net.minecraft.client.gui.screens.Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("description.replicatemekanism.forensic_chamber").withStyle(net.minecraft.ChatFormatting.GRAY));
        }
    }
}