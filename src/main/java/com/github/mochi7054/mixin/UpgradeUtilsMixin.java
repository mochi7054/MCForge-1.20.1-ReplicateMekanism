package com.github.mochi7054.mixin;

import mekanism.api.Upgrade;
import mekanism.common.util.UpgradeUtils;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.github.mochi7054.ReplicateMekanism;

@Mixin(value = UpgradeUtils.class, remap = false)
public class UpgradeUtilsMixin {

    @Inject(method = "getStack(Lmekanism/api/Upgrade;I)Lnet/minecraft/world/item/ItemStack;", at = @At("HEAD"), cancellable = true)
    private static void onGetStack(Upgrade upgrade, int count, CallbackInfoReturnable<ItemStack> cir) {
        if (upgrade == ReplicateMekanism.REPLICA_UPGRADE_TYPE) {
            cir.setReturnValue(new ItemStack(ReplicateMekanism.REPLICA_UPGRADE_ITEM.get(), count));
        }
    }

    @Inject(method = "getInfo", at = @At("HEAD"), cancellable = true)
    private static void onGetInfo(net.minecraft.world.level.block.entity.BlockEntity tile, Upgrade upgrade, CallbackInfoReturnable<java.util.List<net.minecraft.network.chat.Component>> cir) {
        if (upgrade == ReplicateMekanism.REPLICA_UPGRADE_TYPE) {
            java.util.List<net.minecraft.network.chat.Component> info = new java.util.ArrayList<>();
            int installed = 0;
            if (tile instanceof mekanism.common.tile.interfaces.IUpgradeTile upgradeTile) {
                var component = upgradeTile.getComponent();
                if (component != null) {
                    installed = component.getUpgrades(ReplicateMekanism.REPLICA_UPGRADE_TYPE);
                }
            }
            
            int currentMult = 1 << installed;
            int maxInstalled = upgrade.getMax();
            int maxMult = 1 << maxInstalled;

            info.add(net.minecraft.network.chat.Component.translatable("upgrade.replicatemekanism.replica.effect", currentMult, maxMult));
            cir.setReturnValue(info);
        }
    }
}