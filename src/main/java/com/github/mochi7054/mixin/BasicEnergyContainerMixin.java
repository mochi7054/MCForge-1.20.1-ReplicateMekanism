package com.github.mochi7054.mixin;

import mekanism.common.capabilities.energy.BasicEnergyContainer;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.math.FloatingLong;
import mekanism.common.tile.base.TileEntityMekanism;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.github.mochi7054.recipe.ReplicaRecipeTracker;

@Mixin(value = BasicEnergyContainer.class, remap = false)
public abstract class BasicEnergyContainerMixin implements com.github.mochi7054.IOwnerTrackedContainer {

    private TileEntityMekanism replicateMekanism$owner;

    @Override
    public TileEntityMekanism getReplicateMekanism$owner() {
        return this.replicateMekanism$owner;
    }

    @Override
    public void setReplicateMekanism$owner(TileEntityMekanism owner) {
        this.replicateMekanism$owner = owner;
    }

    @Inject(method = "getMaxEnergy", at = @At("RETURN"), cancellable = true)
    private void onGetMaxEnergy(CallbackInfoReturnable<FloatingLong> cir) {
        if (replicateMekanism$owner instanceof mekanism.generators.common.tile.TileEntityGenerator tile) {
            int mult = ReplicaRecipeTracker.getReplicaMultiplier(tile);
            if (mult > 1) {
                FloatingLong orig = cir.getReturnValue();
                cir.setReturnValue(orig.multiply(mult));
            }
        }
    }

    @ModifyVariable(
        method = "insert",
        at = @At("HEAD"),
        ordinal = 0,
        argsOnly = true
    )
    private FloatingLong modifyInsertAmount(FloatingLong modified, FloatingLong amount, Action action, AutomationType automationType) {
        if (action.execute() && automationType == AutomationType.INTERNAL && replicateMekanism$owner instanceof mekanism.generators.common.tile.TileEntityGenerator tile) {
            int mult = ReplicaRecipeTracker.getReplicaMultiplier(tile);
            if (mult > 1) {
                return amount.multiply(mult);
            }
        }
        return modified;
    }
}