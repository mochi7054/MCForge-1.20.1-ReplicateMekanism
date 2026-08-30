package com.github.mochi7054.item;

import com.github.mochi7054.ReplicateMekanism;
import mekanism.api.Upgrade;
import mekanism.common.item.ItemUpgrade;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ReplicaUpgradeItem extends ItemUpgrade {

    private static final TextColor REPLICA_COLOR = TextColor.fromRgb(0x38FF70);

    public ReplicaUpgradeItem(Properties properties) {
        super(ReplicateMekanism.REPLICA_UPGRADE_TYPE, properties);
    }

    @Override
    public Upgrade getUpgradeType(ItemStack stack) {
        return ReplicateMekanism.REPLICA_UPGRADE_TYPE;
    }

    @Override
    public net.minecraft.network.chat.MutableComponent getName(ItemStack stack) {
        return super.getName(stack).copy().withStyle(style -> style.withColor(REPLICA_COLOR));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
    }
}