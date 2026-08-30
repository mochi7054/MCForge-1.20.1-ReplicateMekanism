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
}