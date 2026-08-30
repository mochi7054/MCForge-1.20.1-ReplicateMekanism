package com.github.mochi7054.item;

import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ColoredNameItem extends Item {
    private final TextColor color;

    public ColoredNameItem(Properties properties, TextColor color) {
        super(properties);
        this.color = color;
    }

    public ColoredNameItem(Properties properties, int rgb) {
        super(properties);
        this.color = TextColor.fromRgb(rgb);
    }

    @Override
    public MutableComponent getName(ItemStack stack) {
        return super.getName(stack).copy().withStyle(style -> style.withColor(color));
    }
}