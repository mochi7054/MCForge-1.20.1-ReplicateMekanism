package com.github.mochi7054.collapser;

import com.github.mochi7054.ReplicateMekanism;
import com.github.mochi7054.block.ReplicaTier;
import mekanism.api.math.FloatingLong;
import mekanism.api.text.ILangEntry;
import mekanism.common.block.prefab.BlockTile;
import mekanism.common.content.blocktype.BlockTypeTile;
import mekanism.common.registration.impl.BlockRegistryObject;
import mekanism.common.registration.impl.ContainerTypeRegistryObject;
import mekanism.common.registration.impl.TileEntityTypeRegistryObject;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.function.Supplier;

public class CollapserBlock extends BlockTile<CollapserBlockEntity, BlockTypeTile<CollapserBlockEntity>> implements mekanism.common.block.interfaces.IHasDescription {

    private final ReplicaTier tier;

    public CollapserBlock(ReplicaTier tier, BlockBehaviour.Properties properties) {
        this(tier, properties,
                () -> getTileSupplier(tier),
                () -> ReplicateMekanism.COLLAPSER_CONTAINER,
                () -> getNextTierBlockSupplier(tier));
    }

    public CollapserBlock(ReplicaTier tier, BlockBehaviour.Properties properties,
                          Supplier<TileEntityTypeRegistryObject<CollapserBlockEntity>> tileSupplier,
                          Supplier<ContainerTypeRegistryObject<CollapserMenu>> containerSupplier,
                          Supplier<BlockRegistryObject<?, ?>> nextTierBlockSupplier) {
        super(createBlockType(tier, tileSupplier, containerSupplier, nextTierBlockSupplier), properties);
        this.tier = tier;
    }

    public ReplicaTier getReplicaTier() {
        return tier;
    }

    public ReplicaTier getTier() {
        return tier;
    }

    private static BlockTypeTile<CollapserBlockEntity> createBlockType(
            ReplicaTier tier,
            Supplier<TileEntityTypeRegistryObject<CollapserBlockEntity>> tileSupplier,
            Supplier<ContainerTypeRegistryObject<CollapserMenu>> containerSupplier,
            Supplier<BlockRegistryObject<?, ?>> nextTierBlockSupplier) {

        ILangEntry description = new ILangEntry() {
            @Override
            public String getTranslationKey() {
                return "container.replicatemekanism.collapser_" + tier.getName();
            }
        };

        java.util.Set<mekanism.api.Upgrade> supported = new java.util.HashSet<>(java.util.Set.of(mekanism.api.Upgrade.SPEED, mekanism.api.Upgrade.ENERGY));
        if (ReplicateMekanism.REPLICA_UPGRADE_TYPE != null) {
            supported.add(ReplicateMekanism.REPLICA_UPGRADE_TYPE);
        }

        BlockTypeTile<CollapserBlockEntity> blockType = new BlockTypeTile<>(tileSupplier, description);
        blockType.add(
                new mekanism.common.block.attribute.AttributeEnergy(() -> FloatingLong.create(50), tier::getEnergyCapacity),
                new mekanism.common.block.attribute.AttributeGui(containerSupplier::get, description),
                new mekanism.common.block.attribute.AttributeUpgradeSupport(supported),
                new mekanism.common.block.attribute.AttributeStateFacing(),
                mekanism.common.block.attribute.Attributes.ACTIVE,
                mekanism.common.block.attribute.Attributes.SECURITY,
                mekanism.common.block.attribute.Attributes.REDSTONE
        );
        return blockType;
    }

    @Override
    public ILangEntry getDescription() {
        return new ILangEntry() {
            @Override
            public String getTranslationKey() {
                return "description.replicatemekanism.collapser";
            }
        };
    }

    public static TileEntityTypeRegistryObject<CollapserBlockEntity> getTileSupplier(ReplicaTier tier) {
        if (tier == ReplicaTier.BASIC) return ReplicateMekanism.COLLAPSER_BASIC_TILE;
        if (tier == ReplicaTier.ADVANCED) return ReplicateMekanism.COLLAPSER_ADVANCED_TILE;
        if (tier == ReplicaTier.ELITE) return ReplicateMekanism.COLLAPSER_ELITE_TILE;
        if (tier == ReplicaTier.ULTIMATE) return ReplicateMekanism.COLLAPSER_ULTIMATE_TILE;
        return ReplicateMekanism.COLLAPSER_TILE;
    }

    public static BlockRegistryObject<?, ?> getNextTierBlockSupplier(ReplicaTier tier) {
        if (tier == ReplicaTier.STANDARD) return ReplicateMekanism.COLLAPSER_BASIC_BLOCK;
        if (tier == ReplicaTier.BASIC) return ReplicateMekanism.COLLAPSER_ADVANCED_BLOCK;
        if (tier == ReplicaTier.ADVANCED) return ReplicateMekanism.COLLAPSER_ELITE_BLOCK;
        if (tier == ReplicaTier.ELITE) return ReplicateMekanism.COLLAPSER_ULTIMATE_BLOCK;
        return null;
    }
}