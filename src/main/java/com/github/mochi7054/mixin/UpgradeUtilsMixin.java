package com.github.mochi7054.mixin;

import mekanism.api.Upgrade;
import mekanism.common.item.ItemUpgrade;
import mekanism.common.util.UpgradeUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.github.mochi7054.ReplicateMekanism;

@Mixin(value = UpgradeUtils.class, remap = false)
public class UpgradeUtilsMixin {

    @Inject(method = "getItem", at = @At("HEAD"), cancellable = true)
    private static void onGetItem(Upgrade upgrade, CallbackInfoReturnable<ItemUpgrade> cir) {
        if (upgrade == ReplicateMekanism.REPLICA_UPGRADE_TYPE) {
            cir.setReturnValue(ReplicateMekanism.REPLICA_UPGRADE_ITEM.get());
        }
    }
}