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

public class CollapserBlockEntity extends TileEntityConfigurableMachine implements MenuProvider, mekanism.common.tile.interfaces.IUpgradeTile, mekanism.common.tile.interfaces.ITierUpgradable {

    public static final FloatingLong BASE_ENERGY_PER_TICK = FloatingLong.createConst(100);
    public static final int BASE_TICKS_REQUIRED = 100;

    public int[] operatingTicks;
    public int ticksRequired = BASE_TICKS_REQUIRED;
    public boolean sorting = false;
    private boolean sortingNeeded = false;

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

        IContentsListener slotListener = () -> {
            listener.onContentsChanged();
            this.sortingNeeded = true;
        };

        for (int i = 0; i < slotCount; i++) {
            InputInventorySlot inputSlot = InputInventorySlot.at(stack -> {
                MatterCompound compound = getMatterCompoundSafe(stack);
                return compound != null && !compound.getValues().isEmpty();
            }, slotListener, inputCoords[i][0], inputCoords[i][1]);
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

            // 自動分配: BASIC以上のティアで sorting が有効かつインベントリ変更時に実行
            if (sorting && sortingNeeded && inputSlots.size() > 1) {
                sortInputSlots();
                sortingNeeded = false;
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

                        // パイプ等の場合（自身にタンクがない場合）、パイプ網（MatterNetwork）の全タンクへ搬出
                        if (!ejected) {
                            com.buuz135.replication.network.MatterNetwork matterNetwork = networkBE.getNetwork();
                            if (matterNetwork != null) {
                                java.util.List<com.hrznstudio.titanium.block_network.element.NetworkElement> targets = new java.util.ArrayList<>();
                                targets.addAll(matterNetwork.getMatterStacksHolders());
                                targets.addAll(matterNetwork.getMatterStacksConsumers());
                                for (SimpleMatterTank myTank : getMatterTanks()) {
                                    com.buuz135.replication.api.matter_fluid.MatterStack stored = myTank.getMatter();
                                    if (stored != null && !stored.isEmpty() && stored.getAmount() > 0) {
                                        for (var elem : targets) {
                                            if (elem.getLevel() == level && level.isLoaded(elem.getPos())) {
                                                var targetBE = level.getBlockEntity(elem.getPos());
                                                if (targetBE instanceof com.buuz135.replication.block.tile.NetworkBlockEntity<?> targetNetBE) {
                                                    for (var component : targetNetBE.getMatterTankComponents()) {
                                                        var compStored = component.getMatter();
                                                        if (compStored == null || compStored.isEmpty() || 
                                                            (compStored.getMatterType() != null && stored.getMatterType() != null &&
                                                             compStored.getMatterType().getName().equalsIgnoreCase(stored.getMatterType().getName()))) {
                                                            double filled = component.fill(stored, net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE);
                                                            if (filled > 0) {
                                                                myTank.drainDouble(filled, net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
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
                                        if (ejected) break;
                                    }
                                }
                            }
                        }
                    }

                    // 隣接が Forge FLUID_HANDLER の場合（Mekanism 液体パイプ、EnderIO 等）
                    if (!ejected && adjacentBE != null) {
                        adjacentBE.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER, dir.getOpposite()).ifPresent(fluidHandler -> {
                            for (SimpleMatterTank tank : getMatterTanks()) {
                                if (!tank.isEmpty()) {
                                    var fluidStack = tank.getFluidStack();
                                    if (fluidStack != null && !fluidStack.isEmpty()) {
                                        int filled = fluidHandler.fill(fluidStack, net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE);
                                        if (filled > 0) {
                                            tank.drainDouble(filled, net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
                                            fluidHandler.fill(new net.minecraftforge.fluids.FluidStack(fluidStack.getFluid(), filled), net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
                                            markForSave();
                                            break;
                                        }
                                    }
                                }
                            }
                        });
                    }
                }
            }
        }
    }

    private void sortInputSlots() {
        if (inputSlots == null || inputSlots.size() <= 1) return;

        int slotCount = inputSlots.size();

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

        // スロットをクリアせずに全アイテムを集計
        Map<ItemKey, Integer> countMap = new java.util.LinkedHashMap<>();
        for (InputInventorySlot slot : inputSlots) {
            ItemStack stack = slot.getStack();
            if (!stack.isEmpty()) {
                ItemKey key = new ItemKey(stack);
                countMap.put(key, countMap.getOrDefault(key, 0) + stack.getCount());
            }
        }
        if (countMap.isEmpty()) return;

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

        // 理想のスロット配置を計算
        ItemStack[] targetStacks = new ItemStack[slotCount];
        for (int i = 0; i < slotCount; i++) {
            targetStacks[i] = ItemStack.EMPTY;
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
                    targetStacks[currentSlotIndex] = distributed;
                }
                currentSlotIndex++;
            }
        }

        // 差分があるスロットのみ setStack
        for (int i = 0; i < slotCount; i++) {
            ItemStack current = inputSlots.get(i).getStack();
            ItemStack target = targetStacks[i];
            if (!ItemStack.matches(current, target)) {
                inputSlots.get(i).setStack(target);
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

    // ITierUpgradable Implementation
    @Override
    public void parseUpgradeData(mekanism.common.upgrade.IUpgradeData upgradeData) {
        if (upgradeData instanceof CollapserUpgradeData data) {
            this.energyContainer.setEnergy(data.energy);
            this.sorting = data.sorting;
            for (int i = 0; i < Math.min(this.inputSlots.size(), data.inputStacks.size()); i++) {
                this.inputSlots.get(i).setStack(data.inputStacks.get(i));
            }
            this.energySlot.setStack(data.energySlotStack);
            
            var tanks = this.getMatterTanks();
            for (int i = 0; i < Math.min(tanks.size(), data.matterAmounts.size()); i++) {
                tanks.get(i).setStored(data.matterAmounts.get(i));
            }
            
            for (mekanism.common.tile.component.ITileComponent component : this.getComponents()) {
                component.read(data.componentNbt);
            }
            
            if (data.operatingTicks != null && this.operatingTicks != null) {
                System.arraycopy(data.operatingTicks, 0, this.operatingTicks, 0, Math.min(this.operatingTicks.length, data.operatingTicks.length));
            }
        }
    }

    private final net.minecraftforge.common.util.LazyOptional<com.buuz135.replication.api.matter_fluid.IMatterHandler> matterHandlerCapability =
            net.minecraftforge.common.util.LazyOptional.of(() -> new com.buuz135.replication.api.matter_fluid.IMatterHandler() {
                @Override
                public int getTanks() {
                    return getMatterTanks().size();
                }

                @Override
                public com.buuz135.replication.api.matter_fluid.MatterStack getMatterInTank(int tank) {
                    var tanks = getMatterTanks();
                    if (tank >= 0 && tank < tanks.size()) {
                        return tanks.get(tank).getMatter();
                    }
                    return com.buuz135.replication.api.matter_fluid.MatterStack.EMPTY;
                }

                @Override
                public int getTankCapacity(int tank) {
                    var tanks = getMatterTanks();
                    if (tank >= 0 && tank < tanks.size()) {
                        return tanks.get(tank).getCapacity();
                    }
                    return 0;
                }

                @Override
                public boolean isMatterValid(int tank, com.buuz135.replication.api.matter_fluid.MatterStack stack) {
                    var tanks = getMatterTanks();
                    if (tank >= 0 && tank < tanks.size()) {
                        return tanks.get(tank).isMatterValid(stack);
                    }
                    return false;
                }

                @Override
                public int fill(com.buuz135.replication.api.matter_fluid.MatterStack stack, net.minecraftforge.fluids.capability.IFluidHandler.FluidAction action) {
                    if (stack == null || stack.isEmpty()) return 0;
                    for (SimpleMatterTank tank : getMatterTanks()) {
                        if (tank.isMatterValid(stack)) {
                            int filled = tank.fill(stack, action);
                            if (filled > 0) {
                                markForSave();
                                return filled;
                            }
                        }
                    }
                    return 0;
                }

                @Override
                public com.buuz135.replication.api.matter_fluid.MatterStack drain(com.buuz135.replication.api.matter_fluid.MatterStack stack, net.minecraftforge.fluids.capability.IFluidHandler.FluidAction action) {
                    if (stack == null || stack.isEmpty()) return com.buuz135.replication.api.matter_fluid.MatterStack.EMPTY;
                    for (SimpleMatterTank tank : getMatterTanks()) {
                        if (tank.isMatterValid(stack)) {
                            var drained = tank.drain(stack.getAmount(), action);
                            if (!drained.isEmpty()) {
                                markForSave();
                                return drained;
                            }
                        }
                    }
                    return com.buuz135.replication.api.matter_fluid.MatterStack.EMPTY;
                }

                @Override
                public com.buuz135.replication.api.matter_fluid.MatterStack drain(int maxDrain, net.minecraftforge.fluids.capability.IFluidHandler.FluidAction action) {
                    if (maxDrain <= 0) return com.buuz135.replication.api.matter_fluid.MatterStack.EMPTY;
                    for (SimpleMatterTank tank : getMatterTanks()) {
                        if (!tank.isEmpty()) {
                            var drained = tank.drain(maxDrain, action);
                            if (!drained.isEmpty()) {
                                markForSave();
                                return drained;
                            }
                        }
                    }
                    return com.buuz135.replication.api.matter_fluid.MatterStack.EMPTY;
                }
            });

    @NotNull
    @Override
    public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(@NotNull net.minecraftforge.common.capabilities.Capability<T> capability, @Nullable Direction side) {
        if (capability == com.buuz135.replication.ReplicationRegistry.Capabilities.MATTER_HANDLER) {
            return matterHandlerCapability.cast();
        }
        if (capability == net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER) {
            return net.minecraftforge.common.util.LazyOptional.of(() -> new com.github.mochi7054.fluid.ReplicationFluidHandler(this, getMatterTanks(), side)).cast();
        }
        return super.getCapability(capability, side);
    }

    @Override
    public mekanism.common.upgrade.IUpgradeData getUpgradeData() {
        List<ItemStack> inputs = new java.util.ArrayList<>();
        for (InputInventorySlot slot : this.inputSlots) {
            inputs.add(slot.getStack().copy());
        }
        ItemStack energyStack = this.energySlot.getStack().copy();
        
        List<Double> matterAmounts = new java.util.ArrayList<>();
        for (com.github.mochi7054.fluid.SimpleMatterTank tank : this.getMatterTanks()) {
            matterAmounts.add(tank.getStored());
        }
        
        CompoundTag componentsTag = new CompoundTag();
        for (mekanism.common.tile.component.ITileComponent component : this.getComponents()) {
            component.write(componentsTag);
        }
        
        return new CollapserUpgradeData(
            this.energyContainer.getEnergy(),
            inputs,
            energyStack,
            matterAmounts,
            componentsTag,
            this.operatingTicks != null ? this.operatingTicks.clone() : new int[0],
            this.sorting
        );
    }

    public static class CollapserUpgradeData implements mekanism.common.upgrade.IUpgradeData {
        public final mekanism.api.math.FloatingLong energy;
        public final List<ItemStack> inputStacks;
        public final ItemStack energySlotStack;
        public final List<Double> matterAmounts;
        public final CompoundTag componentNbt;
        public final int[] operatingTicks;
        public final boolean sorting;
        
        public CollapserUpgradeData(mekanism.api.math.FloatingLong energy, List<ItemStack> inputStacks, ItemStack energySlotStack, List<Double> matterAmounts, CompoundTag componentNbt, int[] operatingTicks, boolean sorting) {
            this.energy = energy;
            this.inputStacks = inputStacks;
            this.energySlotStack = energySlotStack;
            this.matterAmounts = matterAmounts;
            this.componentNbt = componentNbt;
            this.operatingTicks = operatingTicks;
            this.sorting = sorting;
        }
    }
}
