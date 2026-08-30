package com.github.mochi7054.collapser;

import com.buuz135.replication.api.IMatterType;
import com.buuz135.replication.calculation.MatterCompound;
import com.buuz135.replication.calculation.MatterValue;
import com.buuz135.replication.calculation.ReplicationCalculation;
import com.github.mochi7054.ReplicateMekanism;
import com.github.mochi7054.block.ReplicaTier;
import com.github.mochi7054.fluid.SimpleMatterTank;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.IContentsListener;
import mekanism.api.Upgrade;
import mekanism.api.math.FloatingLong;
import mekanism.common.capabilities.energy.MachineEnergyContainer;
import mekanism.common.capabilities.holder.energy.EnergyContainerHelper;
import mekanism.common.capabilities.holder.energy.IEnergyContainerHolder;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.capabilities.holder.slot.InventorySlotHelper;
import mekanism.common.inventory.container.MekanismContainer;
import mekanism.common.inventory.container.sync.SyncableBoolean;
import mekanism.common.inventory.container.sync.SyncableInt;
import mekanism.common.inventory.slot.EnergyInventorySlot;
import mekanism.common.inventory.slot.InputInventorySlot;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.tile.component.TileComponentEjector;
import mekanism.common.tile.prefab.TileEntityConfigurableMachine;
import mekanism.common.util.MekanismUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CollapserBlockEntity extends TileEntityConfigurableMachine implements MenuProvider {

    public static final FloatingLong BASE_ENERGY_PER_TICK = FloatingLong.createConst(100);
    public static final int BASE_TICKS_PER_OPERATION = 100;

    public MachineEnergyContainer<CollapserBlockEntity> energyContainer;
    public List<InputInventorySlot> inputSlots = new ArrayList<>();
    public EnergyInventorySlot energySlot;

    public final SimpleMatterTank earthTank = new SimpleMatterTank(com.buuz135.replication.api.MatterType.EARTH, 16000);
    public final SimpleMatterTank netherTank = new SimpleMatterTank(com.buuz135.replication.api.MatterType.NETHER, 16000);
    public final SimpleMatterTank organicTank = new SimpleMatterTank(com.buuz135.replication.api.MatterType.ORGANIC, 16000);
    public final SimpleMatterTank enderTank = new SimpleMatterTank(com.buuz135.replication.api.MatterType.ENDER, 16000);
    public final SimpleMatterTank metallicTank = new SimpleMatterTank(com.buuz135.replication.api.MatterType.METALLIC, 16000);
    public final SimpleMatterTank preciousTank = new SimpleMatterTank(com.buuz135.replication.api.MatterType.PRECIOUS, 16000);
    public final SimpleMatterTank livingTank = new SimpleMatterTank(com.buuz135.replication.api.MatterType.LIVING, 16000);
    public final SimpleMatterTank quantumTank = new SimpleMatterTank(com.buuz135.replication.api.MatterType.QUANTUM, 16000);

    private final List<SimpleMatterTank> tanks = List.of(
            earthTank, netherTank, organicTank, enderTank,
            metallicTank, preciousTank, livingTank, quantumTank
    );

    public int[] operatingTicks;
    public boolean sorting = false;

    public CollapserBlockEntity(BlockPos pos, BlockState state) {
        this(ReplicateMekanism.COLLAPSER_BLOCK, pos, state);
    }

    public CollapserBlockEntity(mekanism.common.registration.impl.BlockRegistryObject<?, ?> blockProvider, BlockPos pos, BlockState state) {
        super(blockProvider, pos, state);

        int slotCount = getTier().getSlots();
        this.operatingTicks = new int[slotCount];

        ejectorComponent = new TileComponentEjector(this);

        for (int i = 0; i < inputSlots.size(); i++) {
            final int slotIndex = i;
            configComponent.setupItemIOConfig(inputSlots.get(slotIndex), null, energySlot);
        }
        configComponent.setupInputConfig(TransmissionType.ENERGY, energyContainer);

        ejectorComponent.setOutputData(configComponent, TransmissionType.ITEM);
    }

    public ReplicaTier getTier() {
        if (getBlockState().getBlock() instanceof CollapserBlock collapserBlock) {
            return collapserBlock.getTier();
        }
        return ReplicaTier.STANDARD;
    }

    public ReplicaTier getReplicaTier() {
        return getTier();
    }
public List<SimpleMatterTank> getMatterTanks() {
        return tanks;
    }

    public double getProgress(int slotIndex) {
        if (slotIndex >= 0 && slotIndex < operatingTicks.length) {
            return (double) operatingTicks[slotIndex] / 100.0;
        }
        return 0.0;
    }

    public int getSlotX(int index) {
        ReplicaTier tier = getTier();
        if (tier == ReplicaTier.STANDARD) return 38;
        int spacing = 18;
        int startX = 20;
        return startX + index * spacing;
    }

    @NotNull
    @Override
    public Component getDisplayName() { return getName(); }

    @NotNull
    @Override
    public Component getName() {
        return Component.translatable("container.replicatemekanism.collapser_" + getTier().getName());
    }

    @NotNull
    @Override
    protected IEnergyContainerHolder getInitialEnergyContainers(IContentsListener listener) {
        EnergyContainerHelper builder = EnergyContainerHelper.forSideWithConfig(this::getDirection, this::getConfig);
        energyContainer = MachineEnergyContainer.input(this, listener);
        builder.addContainer(energyContainer);
        return builder.build();
    }

    @NotNull
    @Override
    protected IInventorySlotHolder getInitialInventory(IContentsListener listener) {
        InventorySlotHelper builder = InventorySlotHelper.forSideWithConfig(this::getDirection, this::getConfig);

        int slotCount = 1;
        if (getBlockState().getBlock() instanceof CollapserBlock collapserBlock) {
            slotCount = collapserBlock.getTier().getSlots();
        }

        inputSlots.clear();
        for (int i = 0; i < slotCount; i++) {
            int slotX = (slotCount == 1) ? 38 : 20 + i * 18;
            InputInventorySlot inputSlot = InputInventorySlot.at(
                    stack -> ReplicationCalculation.getMatterCompound(stack) != null,
                    listener, slotX, 22
            );
            inputSlots.add(inputSlot);
            builder.addSlot(inputSlot);
        }

        energySlot = EnergyInventorySlot.fillOrConvert(energyContainer, this::getLevel, listener, 148, 22);
        builder.addSlot(energySlot);

        return builder.build();
    }

    @Override
    protected void onUpdateServer() {
        super.onUpdateServer();
        energySlot.fillContainerOrConvert();

        if (this.sorting && inputSlots.size() > 1 && level != null && level.getGameTime() % 20 == 0) {
            sortInputSlots();
        }

        if (MekanismUtils.canFunction(this)) {
            processCollapsing();
        }
        ejectMatter();
    }

    private void processCollapsing() {
        int speedUpgrades = upgradeComponent != null ? upgradeComponent.getUpgrades(Upgrade.SPEED) : 0;
        int energyUpgrades = upgradeComponent != null ? upgradeComponent.getUpgrades(Upgrade.ENERGY) : 0;

        FloatingLong energyUsage = MekanismUtils.getEnergyPerTick(this, BASE_ENERGY_PER_TICK);
        int ticksRequired = (int) Math.max(1, BASE_TICKS_PER_OPERATION * Math.pow(0.85, speedUpgrades));

        for (int i = 0; i < inputSlots.size(); i++) {
            InputInventorySlot slot = inputSlots.get(i);
            ItemStack stack = slot.getStack();

            if (stack.isEmpty()) {
                operatingTicks[i] = 0;
                continue;
            }

            MatterCompound compound = ReplicationCalculation.getMatterCompound(stack);
            if (compound == null || compound.getValues().isEmpty()) {
                operatingTicks[i] = 0;
                continue;
            }

            if (energyContainer.getEnergy().greaterOrEqual(energyUsage)) {
                energyContainer.extract(energyUsage, Action.EXECUTE, AutomationType.INTERNAL);
                operatingTicks[i]++;

                if (operatingTicks[i] >= ticksRequired) {
                    operatingTicks[i] = 0;

                    for (MatterValue value : compound.getValues().values()) {
                        IMatterType matterType = value.getMatter();
                        SimpleMatterTank tank = getTankForType(matterType);
                        if (tank != null) {
                            tank.fill(new com.buuz135.replication.api.matter_fluid.MatterStack(matterType, (int) Math.round(value.getAmount())),
                                    net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
                        }
                    }
                    slot.shrinkStack(1, Action.EXECUTE);
                    markForSave();
                }
            }
        }
    }

    private SimpleMatterTank getTankForType(IMatterType matterType) {
        if (matterType == null) return null;
        String name = matterType.getName().toLowerCase();
        return switch (name) {
            case "earth" -> earthTank;
            case "nether" -> netherTank;
            case "organic" -> organicTank;
            case "ender" -> enderTank;
            case "metallic" -> metallicTank;
            case "precious" -> preciousTank;
            case "living" -> livingTank;
            case "quantum" -> quantumTank;
            default -> null;
        };
    }

    private void ejectMatter() {
        if (level == null || level.isClientSide) return;

        mekanism.common.tile.component.TileComponentConfig sideConfig = this.getConfig();
        if (sideConfig == null) return;

        mekanism.common.tile.component.config.ConfigInfo info = sideConfig.getConfig(TransmissionType.FLUID);
        if (info == null || !info.isEjecting()) return;

        for (Direction dir : Direction.values()) {
            mekanism.common.tile.component.config.DataType dataType = info.getDataType(mekanism.api.RelativeSide.fromDirections(getDirection(), dir));
            if (dataType == mekanism.common.tile.component.config.DataType.OUTPUT || 
                dataType == mekanism.common.tile.component.config.DataType.INPUT_OUTPUT) {
                
                BlockPos adjacent = worldPosition.relative(dir);
                if (level != null && level.isLoaded(adjacent)) {
                    net.minecraft.world.level.block.entity.BlockEntity adjacentBE = level.getBlockEntity(adjacent);
                    boolean ejected = false;

                    if (adjacentBE instanceof com.buuz135.replication.block.tile.NetworkBlockEntity<?> networkBE) {
                        for (SimpleMatterTank tank : getMatterTanks()) {
                            com.buuz135.replication.api.matter_fluid.MatterStack stored = tank.getMatter();
                            if (stored != null && !stored.isEmpty() && stored.getAmount() > 0) {
                                for (var component : networkBE.getMatterTankComponents()) {
                                    com.buuz135.replication.api.matter_fluid.MatterStack compStored = component.getMatter();
                                    if (compStored == null || compStored.isEmpty() || 
                                        (compStored.getMatterType() != null && stored.getMatterType() != null &&
                                         compStored.getMatterType().getName().equalsIgnoreCase(stored.getMatterType().getName()))) {
                                        double filled = component.fill(stored, net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE);
                                        if (filled > 0) {
                                            tank.drain((int) Math.round(filled), net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
                                            component.fill(new com.buuz135.replication.api.matter_fluid.MatterStack(stored.getMatterType(), (int) Math.round(filled)), 
                                                net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
                                            markForSave();
                                            ejected = true;
                                            break;
                                        }
                                    }
                                }
                                if (ejected) break;
                            }
                        }

                        if (!ejected) {
                            com.buuz135.replication.network.MatterNetwork matterNetwork = networkBE.getNetwork();
                            if (matterNetwork != null) {
                                java.util.List<com.hrznstudio.titanium.block_network.element.NetworkElement> targets = new java.util.ArrayList<>();
                                targets.addAll(matterNetwork.getMatterStacksHolders());
                                targets.addAll(matterNetwork.getMatterStacksConsumers());
                                for (SimpleMatterTank tank : getMatterTanks()) {
                                    com.buuz135.replication.api.matter_fluid.MatterStack stored = tank.getMatter();
                                    if (stored != null && !stored.isEmpty() && stored.getAmount() > 0) {
                                        for (var elem : targets) {
                                            if (elem.getLevel() == level && level.isLoaded(elem.getPos())) {
                                                var targetBE = level.getBlockEntity(elem.getPos());
                                                if (targetBE instanceof com.buuz135.replication.block.tile.NetworkBlockEntity<?> targetNetBE) {
                                                    for (var component : targetNetBE.getMatterTankComponents()) {
                                                        com.buuz135.replication.api.matter_fluid.MatterStack compStored = component.getMatter();
                                                        if (compStored == null || compStored.isEmpty() || 
                                                            (compStored.getMatterType() != null && stored.getMatterType() != null &&
                                                             compStored.getMatterType().getName().equalsIgnoreCase(stored.getMatterType().getName()))) {
                                                            double filled = component.fill(stored, net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE);
                                                            if (filled > 0) {
                                                                tank.drain((int) Math.round(filled), net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
                                                                component.fill(new com.buuz135.replication.api.matter_fluid.MatterStack(stored.getMatterType(), (int) Math.round(filled)), 
                                                                    net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
                                                                markForSave();
                                                                ejected = true;
                                                                break;
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            if (ejected) break;
                                        }
                                        if (ejected) break;
                                    }
                                }
                            }
                        }
                    }

                    if (!ejected) {
                        com.buuz135.replication.api.matter_fluid.IMatterHandler targetHandler = 
                            level.getBlockEntity(adjacent) != null ? level.getBlockEntity(adjacent).getCapability(com.buuz135.replication.ReplicationRegistry.Capabilities.MATTER_HANDLER, dir.getOpposite()).orElse(null) : null;
                        
                        if (targetHandler != null) {
                            for (SimpleMatterTank tank : getMatterTanks()) {
                                com.buuz135.replication.api.matter_fluid.MatterStack stored = tank.getMatter();
                                if (stored != null && !stored.isEmpty() && stored.getAmount() > 0) {
                                    double filled = targetHandler.fill(stored, net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE);
                                    if (filled > 0) {
                                        tank.drain((int) Math.round(filled), net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
                                        targetHandler.fill(
                                            new com.buuz135.replication.api.matter_fluid.MatterStack(stored.getMatterType(), (int) Math.round(filled)), 
                                            net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE
                                        );
                                        markForSave();
                                    }
                                }
                            }
                        } else {
                            net.minecraftforge.fluids.capability.IFluidHandler targetFluidHandler = 
                                level.getBlockEntity(adjacent) != null ? level.getBlockEntity(adjacent).getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER, dir.getOpposite()).orElse(null) : null;
                            if (targetFluidHandler != null) {
                                for (SimpleMatterTank tank : getMatterTanks()) {
                                    com.buuz135.replication.api.matter_fluid.MatterStack stored = tank.getMatter();
                                    if (stored != null && !stored.isEmpty() && stored.getAmount() > 0) {
                                        net.minecraft.world.level.material.Fluid fluid = com.github.mochi7054.fluid.ReplicationFluidHandler.getFluidFromMatter(stored.getMatterType());
                                        if (fluid != null) {
                                            int mBAmount = (int) Math.round(stored.getAmount() * 1000.0);
                                            net.minecraftforge.fluids.FluidStack fluidStack = new net.minecraftforge.fluids.FluidStack(fluid, mBAmount);
                                            int filled = targetFluidHandler.fill(fluidStack, net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE);
                                            if (filled > 0) {
                                                double drainedMatter = filled / 1000.0;
                                                tank.drain((int) Math.round(drainedMatter), net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
                                                targetFluidHandler.fill(
                                                    new net.minecraftforge.fluids.FluidStack(fluid, filled), 
                                                    net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE
                                                );
                                                markForSave();
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void sortInputSlots() {
        if (inputSlots.size() <= 1) return;

        List<ItemStack> collected = new ArrayList<>();
        for (InputInventorySlot slot : inputSlots) {
            ItemStack stack = slot.getStack();
            if (!stack.isEmpty()) {
                collected.add(stack.copy());
                slot.setStack(ItemStack.EMPTY);
            }
        }
        if (collected.isEmpty()) return;

        class ItemKey {
            final ItemStack stack;
            ItemKey(ItemStack stack) {
                this.stack = stack.copy();
                this.stack.setCount(1);
            }
            @Override
            public boolean equals(Object o) {
                if (o instanceof ItemKey other) {
                    return ItemStack.isSameItemSameTags(this.stack, other.stack);
                }
                return false;
            }
            @Override
            public int hashCode() {
                int result = stack.getItem().hashCode();
                if (stack.getTag() != null) {
                    result = 31 * result + stack.getTag().hashCode();
                }
                return result;
            }
        }

        java.util.Map<ItemKey, Integer> countMap = new java.util.LinkedHashMap<>();
        for (ItemStack stack : collected) {
            ItemKey key = new ItemKey(stack);
            countMap.put(key, countMap.getOrDefault(key, 0) + stack.getCount());
        }

        int slotCount = inputSlots.size();

        class SortingGroup {
            final ItemStack template;
            final int total;
            final int maxPerStack;
            int allocatedSlots;

            SortingGroup(ItemStack template, int total) {
                this.template = template;
                this.total = total;
                this.maxPerStack = template.getMaxStackSize();
                this.allocatedSlots = (total + maxPerStack - 1) / maxPerStack;
            }

            int getAverage() {
                return (total + allocatedSlots - 1) / allocatedSlots;
            }
        }

        List<SortingGroup> groups = new ArrayList<>();
        int totalAllocated = 0;
        for (var entry : countMap.entrySet()) {
            SortingGroup group = new SortingGroup(entry.getKey().stack, entry.getValue());
            groups.add(group);
            totalAllocated += group.allocatedSlots;
        }

        if (totalAllocated > slotCount) {
            totalAllocated = 0;
            for (SortingGroup g : groups) {
                g.allocatedSlots = 1;
                totalAllocated += 1;
            }
        }

        while (totalAllocated < slotCount) {
            SortingGroup bestCandidate = null;
            int maxAvg = -1;
            for (SortingGroup g : groups) {
                if (g.allocatedSlots < slotCount) {
                    int avg = g.getAverage();
                    if (avg > maxAvg && avg > 1) {
                        maxAvg = avg;
                        bestCandidate = g;
                    }
                }
            }
            if (bestCandidate != null) {
                bestCandidate.allocatedSlots++;
                totalAllocated++;
            } else {
                break;
            }
        }

        int currentSlotIdx = 0;
        for (SortingGroup g : groups) {
            int remaining = g.total;
            int slotsToUse = Math.min(g.allocatedSlots, slotCount - currentSlotIdx);
            for (int s = 0; s < slotsToUse; s++) {
                int slotsLeftForGroup = slotsToUse - s;
                int countForThisSlot = (remaining + slotsLeftForGroup - 1) / slotsLeftForGroup;
                countForThisSlot = Math.min(countForThisSlot, g.maxPerStack);

                if (countForThisSlot > 0 && currentSlotIdx < slotCount) {
                    ItemStack distributedStack = g.template.copy();
                    distributedStack.setCount(countForThisSlot);
                    inputSlots.get(currentSlotIdx).setStack(distributedStack);
                    remaining -= countForThisSlot;
                    currentSlotIdx++;
                }
            }
        }
        markForSave();
    }

    public void setSorting(boolean sorting) {
        this.sorting = sorting;
        markForSave();
    }

    @Override
    public void addContainerTrackers(MekanismContainer container) {
        super.addContainerTrackers(container);
        if (this.operatingTicks != null) {
            for (int i = 0; i < this.operatingTicks.length; i++) {
                final int idx = i;
                container.track(SyncableInt.create(() -> this.operatingTicks[idx], value -> this.operatingTicks[idx] = value));
            }
        }
        container.track(SyncableBoolean.create(() -> this.sorting, value -> this.sorting = value));
        for (SimpleMatterTank tank : getMatterTanks()) {
            container.track(mekanism.common.inventory.container.sync.SyncableDouble.create(tank::getStored, tank::setStored));
        }
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        if (this.operatingTicks != null) {
            if (tag.contains("operatingTicksArray", Tag.TAG_INT_ARRAY)) {
                int[] saved = tag.getIntArray("operatingTicksArray");
                System.arraycopy(saved, 0, this.operatingTicks, 0, Math.min(this.operatingTicks.length, saved.length));
            } else if (tag.contains("operatingTicks", Tag.TAG_INT)) {
                int saved = tag.getInt("operatingTicks");
                if (this.operatingTicks.length > 0) {
                    this.operatingTicks[0] = saved;
                }
            }
        }
        if (tag.contains("sorting")) {
            this.sorting = tag.getBoolean("sorting");
        }
        if (tag.contains("tanks")) {
            CompoundTag tanksTag = tag.getCompound("tanks");
            earthTank.setStored(tanksTag.getDouble("earth"));
            netherTank.setStored(tanksTag.getDouble("nether"));
            organicTank.setStored(tanksTag.getDouble("organic"));
            enderTank.setStored(tanksTag.getDouble("ender"));
            metallicTank.setStored(tanksTag.getDouble("metallic"));
            preciousTank.setStored(tanksTag.getDouble("precious"));
            livingTank.setStored(tanksTag.getDouble("living"));
            quantumTank.setStored(tanksTag.getDouble("quantum"));
        }
    }

    @Override
    public void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        if (this.operatingTicks != null) {
            tag.putIntArray("operatingTicksArray", this.operatingTicks);
            if (this.operatingTicks.length > 0) {
                tag.putInt("operatingTicks", this.operatingTicks[0]);
            }
        }
        tag.putBoolean("sorting", this.sorting);

        CompoundTag tanksTag = new CompoundTag();
        tanksTag.putDouble("earth", earthTank.getStored());
        tanksTag.putDouble("nether", netherTank.getStored());
        tanksTag.putDouble("organic", organicTank.getStored());
        tanksTag.putDouble("ender", enderTank.getStored());
        tanksTag.putDouble("metallic", metallicTank.getStored());
        tanksTag.putDouble("precious", preciousTank.getStored());
        tanksTag.putDouble("living", livingTank.getStored());
        tanksTag.putDouble("quantum", quantumTank.getStored());
        tag.put("tanks", tanksTag);
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, @NotNull Inventory playerInventory, @NotNull Player player) {
        return new CollapserMenu(windowId, playerInventory, this);
    }
}