package com.github.mochi7054.imaginator;

import mekanism.common.item.block.ItemBlockTooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class ImaginatorBlockItem extends ItemBlockTooltip<ImaginatorBlock> {

    public ImaginatorBlockItem(ImaginatorBlock block, Properties properties) {
        super(block, true, properties);
    }

    @Override
    public net.minecraft.network.chat.MutableComponent getName(ItemStack stack) {
        TextColor tierColor = ((ImaginatorBlock) getBlock()).getReplicaTier().getTextColor();
        return super.getName(stack).copy().withStyle(style -> style.withColor(tierColor));
    }

    @Override
    protected void addDetails(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        super.addDetails(stack, level, tooltip, flag);
    }
}