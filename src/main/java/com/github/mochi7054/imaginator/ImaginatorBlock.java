package com.github.mochi7054.imaginator;

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

public class ImaginatorBlock extends BlockTile<ImaginatorBlockEntity, BlockTypeTile<ImaginatorBlockEntity>> implements mekanism.common.block.interfaces.IHasDescription {

    private final ReplicaTier tier;

    public ImaginatorBlock(ReplicaTier tier, BlockBehaviour.Properties properties) {
        this(tier, properties,
                () -> getTileSupplier(tier),
                () -> ReplicateMekanism.IMAGINATOR_CONTAINER,
                () -> getNextTierBlockSupplier(tier));
    }

    public ImaginatorBlock(ReplicaTier tier, BlockBehaviour.Properties properties,
                           Supplier<TileEntityTypeRegistryObject<ImaginatorBlockEntity>> tileSupplier,
                           Supplier<ContainerTypeRegistryObject<ImaginatorMenu>> containerSupplier,
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

    private static BlockTypeTile<ImaginatorBlockEntity> createBlockType(
            ReplicaTier tier,
            Supplier<TileEntityTypeRegistryObject<ImaginatorBlockEntity>> tileSupplier,
            Supplier<ContainerTypeRegistryObject<ImaginatorMenu>> containerSupplier,
            Supplier<BlockRegistryObject<?, ?>> nextTierBlockSupplier) {

        ILangEntry description = new ILangEntry() {
            @Override
            public String getTranslationKey() {
                return "container.replicatemekanism.imaginator_" + tier.getName();
            }
        };

        java.util.Set<mekanism.api.Upgrade> supported = new java.util.HashSet<>(java.util.Set.of(mekanism.api.Upgrade.SPEED, mekanism.api.Upgrade.ENERGY));
        if (ReplicateMekanism.REPLICA_UPGRADE_TYPE != null) {
            supported.add(ReplicateMekanism.REPLICA_UPGRADE_TYPE);
        }

        BlockTypeTile<ImaginatorBlockEntity> blockType = new BlockTypeTile<>(tileSupplier, description);
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
                return "description.replicatemekanism.imaginator";
            }
        };
    }

    public static TileEntityTypeRegistryObject<ImaginatorBlockEntity> getTileSupplier(ReplicaTier tier) {
        return switch (tier) {
            case STANDARD -> ReplicateMekanism.IMAGINATOR_TILE;
            case BASIC -> ReplicateMekanism.IMAGINATOR_BASIC_TILE;
            case ADVANCED -> ReplicateMekanism.IMAGINATOR_ADVANCED_TILE;
            case ELITE -> ReplicateMekanism.IMAGINATOR_ELITE_TILE;
            case ULTIMATE -> ReplicateMekanism.IMAGINATOR_ULTIMATE_TILE;
        };
    }

    public static BlockRegistryObject<?, ?> getNextTierBlockSupplier(ReplicaTier tier) {
        return switch (tier) {
            case STANDARD -> ReplicateMekanism.IMAGINATOR_BASIC_BLOCK;
            case BASIC -> ReplicateMekanism.IMAGINATOR_ADVANCED_BLOCK;
            case ADVANCED -> ReplicateMekanism.IMAGINATOR_ELITE_BLOCK;
            case ELITE -> ReplicateMekanism.IMAGINATOR_ULTIMATE_BLOCK;
            case ULTIMATE -> null;
        };
    }
}