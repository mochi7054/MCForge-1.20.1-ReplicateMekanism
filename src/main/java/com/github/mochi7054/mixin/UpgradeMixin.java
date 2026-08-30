package com.github.mochi7054.mixin;

import java.util.Arrays;
import mekanism.api.Upgrade;
import mekanism.api.text.APILang;
import mekanism.api.text.EnumColor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Upgrade.class, remap = false)
public abstract class UpgradeMixin {

    @Shadow
    @Final
    @Mutable
    private static Upgrade[] $VALUES;

    @Shadow
    @Final
    @Mutable
    private static Upgrade[] UPGRADES;

    @Inject(method = "<clinit>", at = @At("RETURN"))
    private static void onClinit(CallbackInfo ci) {
        Upgrade[] oldValues = $VALUES;
        int newOrdinal = oldValues.length;

        int maxStack = com.github.mochi7054.config.Config.getReplicaUpgradeMaxStack();
        Upgrade replicaUpgrade = UpgradeInvoker.createUpgrade("REPLICA", newOrdinal, "replica", null, null, maxStack, EnumColor.DARK_BLUE);
        com.github.mochi7054.ReplicateMekanism.REPLICA_UPGRADE_TYPE = replicaUpgrade;

        Upgrade[] newValues = Arrays.copyOf(oldValues, oldValues.length + 1);
        newValues[newOrdinal] = replicaUpgrade;
        $VALUES = newValues;

        if (UPGRADES != null) {
            Upgrade[] newUpgrades = Arrays.copyOf(UPGRADES, UPGRADES.length + 1);
            newUpgrades[newOrdinal] = replicaUpgrade;
            UPGRADES = newUpgrades;
        }
    }

    @Inject(method = "getTranslationKey", at = @At("HEAD"), cancellable = true)
    private void onGetTranslationKey(org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<String> cir) {
        Upgrade self = (Upgrade) (Object) this;
        if (self == com.github.mochi7054.ReplicateMekanism.REPLICA_UPGRADE_TYPE) {
            cir.setReturnValue("upgrade.replicatemekanism.replica");
        }
    }

    @Inject(method = "getDescription", at = @At("HEAD"), cancellable = true)
    private void onGetDescription(org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<net.minecraft.network.chat.Component> cir) {
        Upgrade self = (Upgrade) (Object) this;
        if (self == com.github.mochi7054.ReplicateMekanism.REPLICA_UPGRADE_TYPE) {
            cir.setReturnValue(net.minecraft.network.chat.Component.translatable("upgrade.replicatemekanism.replica.desc"));
        }
    }
}