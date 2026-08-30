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
import mekanism.common.capabilities.fluid.BasicFluidTank;
import mekanism.common.capabilities.holder.energy.EnergyContainerHelper;
import mekanism.common.capabilities.holder.energy.IEnergyContainerHolder;
import mekanism.common.capabilities.holder.fluid.FluidTankHelper;
import mekanism.common.capabilities.holder.fluid.IFluidTankHolder;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.capabilities.holder.slot.InventorySlotHelper;
import mekanism.common.inventory.container.MekanismContainer;
import mekanism.common.inventory.container.sync.SyncableBoolean;
import mekanism.common.inventory.container.sync.SyncableInt;
import mekanism.common.inventory.slot.EnergyInventorySlot;
import mekanism.common.inventory.slot.InputInventorySlot;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.tile.component.TileComponentEjector;
import mekanism.common.tile.component.TileComponentConfig;
import mekanism.common.tile.component.TileComponentUpgrade;
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
import java.util.Map;

public class CollapserBlockEntity extends TileEntityConfigurableMachine implements MenuProvider, mekanism.common.tile.interfaces.IUpgradeTile {

    public static final FloatingLong BASE_ENERGY_PER_TICK = FloatingLong.createConst(100);
    public static final int BASE_TICKS_REQUIRED = 100;

    public int[] operatingTicks;
    public int ticksRequired = BASE_TICKS_REQUIRED;
    public boolean sorting = false;

    public MachineEnergyContainer<CollapserBlockEntity> energyContainer;

    // Replication Network elements
    private com.buuz135.replication.network.DefaultMatterNetworkElement networkElement = null;
    private com.buuz135.replication.network.MatterNetwork currentNetwork = null;

    // 8種類のマタータンク
    public SimpleMatterTank earthTank;
    public SimpleMatterTank netherTank;
    public SimpleMatterTank organicTank;
    public SimpleMatterTank enderTank;
    public SimpleMatterTank metallicTank;
    public SimpleMatterTank preciousTank;
    public SimpleMatterTank livingTank;
    public SimpleMatterTank quantumTank;
    public BasicFluidTank dummyFluidTank;

    public List<SimpleMatterTank> getMatterTanks() {
        return List.of(earthTank, netherTank, organicTank, enderTank,
                metallicTank, preciousTank, livingTank, quantumTank);
    }

    public List<InputInventorySlot> inputSlots;
    private EnergyInventorySlot energySlot;

    public ReplicaTier getTier() {
        try {
            if (getBlockState() != null && getBlockState().getBlock() instanceof CollapserBlock collapserBlock) {
                return collapserBlock.getTier();
            }
        } catch (Exception ignored) {}
        try {
            if (getBlockType() instanceof CollapserBlock collapserBlock) {
                return collapserBlock.getTier();
            }
        } catch (Exception ignored) {}
        return ReplicaTier.STANDARD;
    }

    private ReplicaTier getTierSafe() {
        return getTier();
    }

    public ReplicaTier getReplicaTier() {
        return getTier();
    }

    @Override
    public TileComponentUpgrade getComponent() {
        return upgradeComponent;
    }

    @Override
    public java.util.Set<Upgrade> getSupportedUpgrade() {
        return java.util.EnumSet.of(Upgrade.SPEED, Upgrade.ENERGY);
    }

    @Override
    public void recalculateUpgrades(Upgrade upgrade) {
        super.recalculateUpgrades(upgrade);
    }

    public static mekanism.common.registration.impl.BlockRegistryObject<?, ?> getProvider(BlockState state) {
        if (state != null && state.getBlock() instanceof CollapserBlock block) {
            return switch (block.getTier()) {
                case STANDARD -> ReplicateMekanism.COLLAPSER_BLOCK;
                case BASIC -> ReplicateMekanism.COLLAPSER_BASIC_BLOCK;
                case ADVANCED -> ReplicateMekanism.COLLAPSER_ADVANCED_BLOCK;
                case ELITE -> ReplicateMekanism.COLLAPSER_ELITE_BLOCK;
                case ULTIMATE -> ReplicateMekanism.COLLAPSER_ULTIMATE_BLOCK;
            };
        }
        return ReplicateMekanism.COLLAPSER_BLOCK;
    }

    public CollapserBlockEntity(BlockPos pos, BlockState state) {
        this(getProvider(state), pos, state);
    }

    public CollapserBlockEntity(mekanism.common.registration.impl.BlockRegistryObject<?, ?> blockProvider, BlockPos pos, BlockState state) {
        super(blockProvider, pos, state);

        upgradeComponent = new TileComponentUpgrade(this);
        upgradeComponent.setSupported(Upgrade.SPEED);
        upgradeComponent.setSupported(Upgrade.ENERGY);
        if (ReplicateMekanism.REPLICA_UPGRADE_TYPE != null) {
            upgradeComponent.setSupported(ReplicateMekanism.REPLICA_UPGRADE_TYPE);
        }

        configComponent = new TileComponentConfig(this, TransmissionType.ITEM, TransmissionType.ENERGY, TransmissionType.FLUID);
        
        // ITEM config: input only, no output
        configComponent.setupItemIOConfig(
            new ArrayList<>(inputSlots),
            Collections.emptyList(),
            energySlot,
            false
        );
        configComponent.setupInputConfig(TransmissionType.ENERGY, energyContainer);
        var energyConfig = configComponent.getConfig(TransmissionType.ENERGY);
        if (energyConfig != null) {
            for (mekanism.api.RelativeSide side : mekanism.api.RelativeSide.values()) {
                energyConfig.setDataType(mekanism.common.tile.component.config.DataType.INPUT, side);
            }
        }

        var itemConfig = configComponent.getConfig(TransmissionType.ITEM);
        if (itemConfig != null) {
            for (mekanism.api.RelativeSide side : mekanism.api.RelativeSide.values()) {
                itemConfig.setDataType(mekanism.common.tile.component.config.DataType.INPUT, side);
            }
        }

        // Add FLUID configuration (reused for Matter)
        configComponent.setupOutputConfig(TransmissionType.FLUID, dummyFluidTank);
        var fluidConfig = configComponent.getConfig(TransmissionType.FLUID);
        if (fluidConfig != null) {
            fluidConfig.setCanEject(true);
            fluidConfig.setEjecting(true);
            for (mekanism.api.RelativeSide side : mekanism.api.RelativeSide.values()) {
                fluidConfig.setDataType(mekanism.common.tile.component.config.DataType.OUTPUT, side);
            }
        }

        ejectorComponent = new TileComponentEjector(this);
        ejectorComponent.setOutputData(configComponent, TransmissionType.ITEM);
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
    protected IFluidTankHolder getInitialFluidTanks(IContentsListener listener) {
        int capacity = getTierSafe().getTankCapacity();
        earthTank = new SimpleMatterTank(com.buuz135.replication.ReplicationRegistry.Matter.EARTH.get(), capacity);
        netherTank = new SimpleMatterTank(com.buuz135.replication.ReplicationRegistry.Matter.NETHER.get(), capacity);
        organicTank = new SimpleMatterTank(com.buuz135.replication.ReplicationRegistry.Matter.ORGANIC.get(), capacity);
        enderTank = new SimpleMatterTank(com.buuz135.replication.ReplicationRegistry.Matter.ENDER.get(), capacity);
        metallicTank = new SimpleMatterTank(com.buuz135.replication.ReplicationRegistry.Matter.METALLIC.get(), capacity);
        preciousTank = new SimpleMatterTank(com.buuz135.replication.ReplicationRegistry.Matter.PRECIOUS.get(), capacity);
        livingTank = new SimpleMatterTank(com.buuz135.replication.ReplicationRegistry.Matter.LIVING.get(), capacity);
        quantumTank = new SimpleMatterTank(com.buuz135.replication.ReplicationRegistry.Matter.QUANTUM.get(), capacity);

        dummyFluidTank = BasicFluidTank.create(1000, listener);
        FluidTankHelper builder = FluidTankHelper.forSideWithConfig(this::getDirection, this::getConfig);
        builder.addTank(dummyFluidTank);
        return builder.build();
    }

    @NotNull
    @Override
    protected IInventorySlotHolder getInitialInventory(IContentsListener listener) {
        InventorySlotHelper builder = InventorySlotHelper.forSideWithConfig(this::getDirection, this::getConfig);
        ReplicaTier tier = getTierSafe();
        int slotCount = tier.getSlotCount();
        this.operatingTicks = new int[slotCount];
        
        int[][] inputCoords = new int[slotCount][2];
        int energyX;
        int energyY;
        if (tier == ReplicaTier.STANDARD) {
            inputCoords[0][0] = 16;
            inputCoords[0][1] = 40;
            energyX = 141;
            energyY = 40;
        } else {
            int startX;
            int gap;
            if (tier == ReplicaTier.BASIC) {
                startX = 55;
                gap = 38;
            } else if (tier == ReplicaTier.ADVANCED) {
                startX = 35;
                gap = 26;
            } else if (tier == ReplicaTier.ELITE) {
                startX = 32;
                gap = 19;
            } else { // ULTIMATE
                startX = 30;
                gap = 19;
            }
            for (int i = 0; i < slotCount; i++) {
                inputCoords[i][0] = startX + i * gap;
                inputCoords[i][1] = 17;
            }
            energyX = 10;
            energyY = 17;
        }

        if (inputSlots == null) {
            inputSlots = new ArrayList<>();
        } else {
            inputSlots.clear();
        }

        for (int i = 0; i < slotCount; i++) {
            InputInventorySlot inputSlot = InputInventorySlot.at(stack -> {
                MatterCompound compound = getMatterCompoundSafe(stack);
                return compound != null && !compound.getValues().isEmpty();
            }, listener, inputCoords[i][0], inputCoords[i][1]);
            inputSlots.add(inputSlot);
            builder.addSlot(inputSlot);
        }

        energySlot = EnergyInventorySlot.fillOrConvert(energyContainer, this::getLevel, listener, energyX, energyY);
        builder.addSlot(energySlot);
        return builder.build();
    }

    @Nullable
    public com.buuz135.replication.network.MatterNetwork getNetwork() {
        if (this.level == null) return null;
        for (Direction dir : Direction.values()) {
            BlockPos adjacent = this.worldPosition.relative(dir);
            net.minecraft.world.level.block.entity.BlockEntity adjacentBe = this.level.getBlockEntity(adjacent);
            if (adjacentBe instanceof com.buuz135.replication.block.tile.NetworkBlockEntity<?> networkBe) {
                com.buuz135.replication.network.MatterNetwork net = networkBe.getNetwork();
                if (net != null) {
                    return net;
                }
            }
        }
        return null;
    }

    public static MatterCompound getMatterCompoundSafe(ItemStack stack) {
        if (stack.isEmpty()) return null;
        try {
            MatterCompound compound = ReplicationCalculation.getMatterCompound(stack);
            if (compound != null && !compound.getValues().isEmpty()) {
                return compound;
            }
        } catch (Throwable ignored) {}
        try {
            MatterCompound clientCompound = com.buuz135.replication.calculation.client.ClientReplicationCalculation.getMatterCompound(stack);
            if (clientCompound != null && !clientCompound.getValues().isEmpty()) {
                return clientCompound;
            }
        } catch (Throwable ignored) {}
        return null;
    }

    public SimpleMatterTank getTankForMatterType(IMatterType matterType) {
        if (matterType == null) return null;
        String name = matterType.getName();
        if (name == null) return null;
        name = name.toLowerCase();
        if (name.contains("earth")) return earthTank;
        if (name.contains("nether")) return netherTank;
        if (name.contains("organic")) return organicTank;
        if (name.contains("ender")) return enderTank;
        if (name.contains("metallic")) return metallicTank;
        if (name.contains("precious")) return preciousTank;
        if (name.contains("living")) return livingTank;
        if (name.contains("quantum")) return quantumTank;
        return null;
    }

    public double getProgress(int slotIndex) {
        if (operatingTicks != null && slotIndex >= 0 && slotIndex < operatingTicks.length) {
            int ticks = this.ticksRequired > 0 ? this.ticksRequired : MekanismUtils.getTicks(this, BASE_TICKS_REQUIRED);
            return (double) operatingTicks[slotIndex] / (double) Math.max(1, ticks);
        }
        return 0.0;
    }

    public double getScaledProgress() {
        return getProgress(0);
    }

    public double getScaledProgress(int slotIndex) {
        return getProgress(slotIndex);
    }

    public int getSlotX(int index) {
        ReplicaTier tier = getTier();
        if (tier == ReplicaTier.STANDARD) return 16;
        int startX;
        int gap;
        if (tier == ReplicaTier.BASIC) {
            startX = 55;
            gap = 38;
        } else if (tier == ReplicaTier.ADVANCED) {
            startX = 35;
            gap = 26;
        } else if (tier == ReplicaTier.ELITE) {
            startX = 32;
            gap = 19;
        } else { // ULTIMATE
            startX = 30;
            gap = 19;
        }
        return startX + index * gap;
    }

    @NotNull
    @Override
    public Component getDisplayName() { return getName(); }

    @NotNull
    @Override
    public Component getName() {
        return Component.translatable("container.replicatemekanism.collapser_" + getTier().getName());
    }

    @Override
    protected void onUpdateServer() {
        super.onUpdateServer();
        try {
            int slotCount = inputSlots != null ? inputSlots.size() : getTier().getSlots();
            if (this.operatingTicks == null || this.operatingTicks.length != slotCount) {
                this.operatingTicks = new int[slotCount];
            }

            if (energySlot != null) {
                energySlot.fillContainerOrConvert();
            }

            com.buuz135.replication.network.MatterNetwork network = getNetwork();
            if (network != currentNetwork) {
                if (networkElement != null) {
                    try {
                        networkElement.leaveNetwork();
                    } catch (Exception ignored) {}
                    networkElement = null;
                }
                currentNetwork = network;
                if (currentNetwork != null && level != null) {
                    networkElement = new com.buuz135.replication.network.DefaultMatterNetworkElement(level, worldPosition);
                    networkElement.joinNetwork(currentNetwork);
                }
            }

            ticksRequired = MekanismUtils.getTicks(this, BASE_TICKS_REQUIRED);
            FloatingLong energyUsage = MekanismUtils.getEnergyPerTick(this, BASE_ENERGY_PER_TICK);

            boolean[] canOperate = new boolean[inputSlots.size()];
            MatterCompound[] slotCompounds = new MatterCompound[inputSlots.size()];

            if (MekanismUtils.canFunction(this)) {
                for (int i = 0; i < inputSlots.size(); i++) {
                    ItemStack inputStack = inputSlots.get(i).getStack();
                    if (!inputStack.isEmpty()) {
                        MatterCompound compound = getMatterCompoundSafe(inputStack);

                        if (compound != null && !compound.getValues().isEmpty()) {
                            boolean allTanksHaveSpace = true;
                            for (Map.Entry<IMatterType, MatterValue> entry : compound.getValues().entrySet()) {
                                IMatterType matterType = entry.getKey();
                                double amount = entry.getValue().getAmount();
                                SimpleMatterTank targetTank = getTankForMatterType(matterType);
                                if (targetTank == null || targetTank.getCapacity() - targetTank.getStored() < amount - 0.0001) {
                                    allTanksHaveSpace = false;
                                    break;
                                }
                            }

                            if (allTanksHaveSpace) {
                                canOperate[i] = true;
                                slotCompounds[i] = compound;
                            }
                        }
                    }
                }
            }

            boolean anyOperating = false;
            boolean wasActive = getActive();

            for (int i = 0; i < inputSlots.size(); i++) {
                if (canOperate[i]) {
                    if (energyContainer.getEnergy().greaterOrEqual(energyUsage)) {
                        energyContainer.extract(energyUsage, Action.EXECUTE, AutomationType.INTERNAL);
                        operatingTicks[i]++;
                        anyOperating = true;

                        if (operatingTicks[i] >= ticksRequired) {
                            operatingTicks[i] = 0;
                            MatterCompound compound = slotCompounds[i];
                            if (compound != null) {
                                int upgradeCount = upgradeComponent != null ? upgradeComponent.getUpgrades(ReplicateMekanism.REPLICA_UPGRADE_TYPE) : 0;
                                double multiplier = (double) (1 << upgradeCount);
                                for (Map.Entry<IMatterType, MatterValue> entry : compound.getValues().entrySet()) {
                                    IMatterType matterType = entry.getKey();
                                    double amount = entry.getValue().getAmount() * multiplier;
                                    SimpleMatterTank targetTank = getTankForMatterType(matterType);
                                    if (targetTank != null) {
                                        targetTank.fillDouble(amount, net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
                                    }
                                }
                                inputSlots.get(i).shrinkStack(1, Action.EXECUTE);
                                markForSave();
                            }
                        }
                    } else {
                        if (operatingTicks[i] > 0) {
                            operatingTicks[i] = Math.max(0, operatingTicks[i] - 2);
                            markForSave();
                        }
                    }
                } else {
                    if (operatingTicks[i] > 0) {
                        operatingTicks[i] = Math.max(0, operatingTicks[i] - 2);
                        markForSave();
                    }
                }
            }

            setActive(anyOperating);
            if (wasActive != getActive()) {
                markForSave();
            }

            // 自動分配: BASIC以上のティアで sorting が有効なら毎 tick 実行
            if (sorting && inputSlots.size() > 1 && level != null && level.getGameTime() % 20 == 0) {
                sortInputSlots();
            }

            // 自動排出 (Auto-Eject)
            ejectMatter();
        } catch (Throwable t) {
            ReplicateMekanism.LOGGER.error("Error in Collapser onUpdateServer", t);
        }
    }

    private void ejectMatter() {
        if (level == null || level.isClientSide) return;

        mekanism.common.tile.component.TileComponentConfig sideConfig = this.getConfig();
        if (sideConfig == null) return;

        var info = sideConfig.getConfig(TransmissionType.FLUID);
        if (info == null || !info.isEjecting()) return;

        for (Direction dir : Direction.values()) {
            var dataType = info.getDataType(mekanism.api.RelativeSide.fromDirections(getDirection(), dir));
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
                                    var compStored = component.getMatter();
                                    if (compStored == null || compStored.isEmpty() || 
                                        (compStored.getMatterType() != null && stored.getMatterType() != null &&
                                         compStored.getMatterType().getName().equalsIgnoreCase(stored.getMatterType().getName()))) {
                                        double filled = component.fill(stored, net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE);
                                        if (filled > 0) {
                                            tank.drainDouble(filled, net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
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

        Map<ItemKey, Integer> countMap = new java.util.LinkedHashMap<>();
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
                this.allocatedSlots = 1;
            }
        }

        List<SortingGroup> groups = new ArrayList<>();
        for (Map.Entry<ItemKey, Integer> entry : countMap.entrySet()) {
            groups.add(new SortingGroup(entry.getKey().stack, entry.getValue()));
        }

        if (groups.size() > slotCount) {
            int slotIdx = 0;
            for (ItemStack original : collected) {
                if (slotIdx < slotCount) {
                    inputSlots.get(slotIdx++).setStack(original);
                }
            }
            return;
        }

        int remainingSlots = slotCount - groups.size();
        while (remainingSlots > 0) {
            SortingGroup bestGroup = null;
            double maxExpectedPerSlot = -1;

            for (SortingGroup g : groups) {
                int currentSlots = g.allocatedSlots;
                int minSlotsNeeded = (g.total + g.maxPerStack - 1) / g.maxPerStack;
                if (currentSlots < minSlotsNeeded) {
                    double expected = (double) g.total / (currentSlots + 1);
                    if (expected > maxExpectedPerSlot) {
                        maxExpectedPerSlot = expected;
                        bestGroup = g;
                    }
                }
            }

            if (bestGroup == null) {
                for (SortingGroup g : groups) {
                    double expected = (double) g.total / (g.allocatedSlots + 1);
                    if (expected > maxExpectedPerSlot) {
                        maxExpectedPerSlot = expected;
                        bestGroup = g;
                    }
                }
            }

            if (bestGroup != null) {
                bestGroup.allocatedSlots++;
                remainingSlots--;
            } else {
                break;
            }
        }

        int currentSlotIndex = 0;
        for (SortingGroup g : groups) {
            int total = g.total;
            int k = g.allocatedSlots;

            int base = total / k;
            int rem = total % k;

            for (int i = 0; i < k; i++) {
                if (currentSlotIndex >= slotCount) break;
                int count = base + (i < rem ? 1 : 0);
                if (count > 0) {
                    ItemStack distributed = g.template.copy();
                    distributed.setCount(Math.min(count, g.maxPerStack));
                    inputSlots.get(currentSlotIndex).setStack(distributed);
                }
                currentSlotIndex++;
            }
        }
        markForSave();
    }

    public boolean isSorting() {
        return sorting;
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
        container.track(SyncableInt.create(() -> this.ticksRequired, value -> this.ticksRequired = value));
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
