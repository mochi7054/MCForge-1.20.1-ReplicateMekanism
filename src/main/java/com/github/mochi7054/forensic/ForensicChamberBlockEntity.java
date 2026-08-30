package com.github.mochi7054.forensic;

import com.buuz135.replication.api.pattern.IMatterPatternHolder;
import com.buuz135.replication.api.pattern.IMatterPatternModifier;
import com.buuz135.replication.api.pattern.IMatterPatternModifier.ModifierAction;
import com.buuz135.replication.api.pattern.MatterPattern;
import com.buuz135.replication.calculation.MatterCompound;
import com.buuz135.replication.calculation.ReplicationCalculation;
import com.github.mochi7054.ReplicateMekanism;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.IContentsListener;
import mekanism.api.math.FloatingLong;
import mekanism.common.capabilities.energy.MachineEnergyContainer;
import mekanism.common.capabilities.holder.energy.EnergyContainerHelper;
import mekanism.common.capabilities.holder.energy.IEnergyContainerHolder;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.capabilities.holder.slot.InventorySlotHelper;
import mekanism.common.inventory.slot.BasicInventorySlot;
import mekanism.common.inventory.slot.EnergyInventorySlot;
import mekanism.common.inventory.slot.OutputInventorySlot;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.tile.component.TileComponentEjector;
import mekanism.common.tile.component.TileComponentConfig;
import mekanism.common.tile.prefab.TileEntityConfigurableMachine;
import mekanism.common.util.MekanismUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Nameable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ForensicChamberBlockEntity extends TileEntityConfigurableMachine implements Nameable, mekanism.common.tile.interfaces.IUpgradeTile {

    public static final FloatingLong SCAN_ENERGY_COST = FloatingLong.createConst(10_000);
    public static final FloatingLong MAX_ENERGY = FloatingLong.createConst(40_000);

    public MachineEnergyContainer<ForensicChamberBlockEntity> energyContainer;
    public BasicInventorySlot inputSlot;
    public BasicInventorySlot chipInputSlot;
    public OutputInventorySlot chipOutputSlot;
    public EnergyInventorySlot energySlot;
    private final mekanism.common.tile.component.TileComponentUpgrade upgradeComponent;

    public ForensicChamberBlockEntity(BlockPos pos, BlockState state) {
        super(ReplicateMekanism.FORENSIC_CHAMBER_BLOCK, pos, state);
        upgradeComponent = new mekanism.common.tile.component.TileComponentUpgrade(this);
        upgradeComponent.setSupported(mekanism.api.Upgrade.ENERGY);
        if (ReplicateMekanism.REPLICA_UPGRADE_TYPE != null) upgradeComponent.setSupported(ReplicateMekanism.REPLICA_UPGRADE_TYPE);

        configComponent = new TileComponentConfig(this, TransmissionType.ITEM, TransmissionType.ENERGY);
        ejectorComponent = new TileComponentEjector(this);

        configComponent.setupItemIOExtraConfig(inputSlot, chipOutputSlot, chipInputSlot, energySlot);
        configComponent.setupInputConfig(TransmissionType.ENERGY, energyContainer);

        ejectorComponent.setOutputData(configComponent, TransmissionType.ITEM);
    }

    @NotNull
    @Override
    public mekanism.common.tile.component.TileComponentUpgrade getComponent() { return upgradeComponent; }

    @Override
    public java.util.Set<mekanism.api.Upgrade> getSupportedUpgrade() {
        return java.util.EnumSet.of(mekanism.api.Upgrade.ENERGY);
    }

    @Override
    public void recalculateUpgrades(mekanism.api.Upgrade upgrade) { super.recalculateUpgrades(upgrade); }

    public Component getName() {
        return Component.translatable("container.replicatemekanism.forensic_chamber");
    }

    @NotNull
    @Override
    protected IEnergyContainerHolder getInitialEnergyContainers(IContentsListener listener) {
        EnergyContainerHelper builder = EnergyContainerHelper.forSideWithConfig(this::getDirection, this::getConfig);
        energyContainer = MachineEnergyContainer.input(this, listener);
        builder.addContainer(energyContainer);
        return builder.build();
    }

    @SuppressWarnings("unchecked")
    private static <T> List<?> getPatternsRaw(IMatterPatternHolder<T> holder, Object target) {
        return holder.getPatterns((T) target);
    }

    @NotNull
    @Override
    protected IInventorySlotHolder getInitialInventory(IContentsListener listener) {
        InventorySlotHelper builder = InventorySlotHelper.forSideWithConfig(this::getDirection, this::getConfig);

        inputSlot = BasicInventorySlot.at(stack -> {
            MatterCompound compound = ReplicationCalculation.getMatterCompound(stack);
            return compound != null && !compound.getValues().isEmpty();
        }, listener, 32, 42);

        chipInputSlot = BasicInventorySlot.at(stack -> stack.getItem() instanceof IMatterPatternModifier, listener, 74, 42);

        chipOutputSlot = OutputInventorySlot.at(listener, 116, 42);

        energySlot = EnergyInventorySlot.fillOrConvert(energyContainer, this::getLevel, listener, 142, 42);

        builder.addSlot(inputSlot);
        builder.addSlot(chipInputSlot);
        builder.addSlot(chipOutputSlot);
        builder.addSlot(energySlot);

        return builder.build();
    }

    @SuppressWarnings("unchecked")
    private static <T> ModifierAction addPatternRaw(IMatterPatternModifier<T> modifier, Object chip, ItemStack target, float completion) {
        return (ModifierAction) modifier.addPattern((T) chip, target, completion);
    }

    public void tryAutoScan() {
        if (level == null || level.isClientSide || !MekanismUtils.canFunction(this)) return;

        ItemStack inputStack = inputSlot.getStack();
        ItemStack chipStack = chipInputSlot.getStack();

        if (inputStack.isEmpty() || chipStack.isEmpty()) {
            return;
        }

        if (energyContainer.getEnergy().smallerThan(SCAN_ENERGY_COST)) {
            return;
        }

        if (!(chipStack.getItem() instanceof IMatterPatternModifier<?> modifier)) {
            return;
        }

        ItemStack targetItem = inputStack.getItem().getDefaultInstance();

        if (chipStack.getItem() instanceof IMatterPatternHolder<?> holder) {
            List<?> patterns = getPatternsRaw(holder, chipStack);
            if (patterns != null) {
                for (Object obj : patterns) {
                    if (obj instanceof MatterPattern pattern && !pattern.getStack().isEmpty()) {
                        if (ItemStack.isSameItemSameTags(pattern.getStack(), targetItem)) {
                            if (pattern.getCompletion() >= 1.0f) {
                                return;
                            }
                        }
                    }
                }
            }
        }

        ItemStack chipCopy = chipStack.copy();
        chipCopy.setCount(1);

        ModifierAction action = addPatternRaw(modifier, chipCopy, targetItem, 1.0f);
        
        if (action != null && action.getPattern() != null) {
            ItemStack outputStack = chipOutputSlot.getStack();
            if (outputStack.isEmpty()) {
                chipOutputSlot.setStack(chipCopy);
            } else if (ItemStack.isSameItemSameTags(outputStack, chipCopy) && outputStack.getCount() + 1 <= outputStack.getMaxStackSize()) {
                outputStack.grow(1);
            } else {
                return;
            }

            chipInputSlot.shrinkStack(1, Action.EXECUTE);
            inputStack = inputSlot.getStack();
            if (!inputStack.isEmpty()) {
                inputSlot.shrinkStack(1, Action.EXECUTE);
            }
            energyContainer.extract(SCAN_ENERGY_COST, Action.EXECUTE, AutomationType.INTERNAL);

            markForSave();
        }
    }

    @Override
    protected void onUpdateServer() {
        super.onUpdateServer();
        energySlot.fillContainerOrConvert();
        tryAutoScan();
    }
}