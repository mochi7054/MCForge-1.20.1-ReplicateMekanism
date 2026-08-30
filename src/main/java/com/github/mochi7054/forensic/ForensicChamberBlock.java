package com.github.mochi7054.forensic;

import com.github.mochi7054.ReplicateMekanism;
import mekanism.api.math.FloatingLong;
import mekanism.api.text.ILangEntry;
import mekanism.common.block.prefab.BlockTile;
import mekanism.common.content.blocktype.BlockTypeTile;
import mekanism.common.registration.impl.ContainerTypeRegistryObject;
import mekanism.common.registration.impl.TileEntityTypeRegistryObject;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.function.Supplier;

public class ForensicChamberBlock extends BlockTile<ForensicChamberBlockEntity, BlockTypeTile<ForensicChamberBlockEntity>> implements mekanism.common.block.interfaces.IHasDescription {

    public ForensicChamberBlock(BlockBehaviour.Properties properties) {
        this(properties, () -> ReplicateMekanism.FORENSIC_CHAMBER_TILE, () -> ReplicateMekanism.FORENSIC_CHAMBER_CONTAINER);
    }

    public ForensicChamberBlock(BlockBehaviour.Properties properties,
                                Supplier<TileEntityTypeRegistryObject<ForensicChamberBlockEntity>> tileSupplier,
                                Supplier<ContainerTypeRegistryObject<ForensicChamberMenu>> containerSupplier) {
        super(createBlockType(tileSupplier, containerSupplier), properties);
    }

    private static BlockTypeTile<ForensicChamberBlockEntity> createBlockType(
            Supplier<TileEntityTypeRegistryObject<ForensicChamberBlockEntity>> tileSupplier,
            Supplier<ContainerTypeRegistryObject<ForensicChamberMenu>> containerSupplier) {

        ILangEntry description = new ILangEntry() {
            @Override
            public String getTranslationKey() {
                return "container.replicatemekanism.forensic_chamber";
            }
        };

        BlockTypeTile<ForensicChamberBlockEntity> blockType = new BlockTypeTile<>(tileSupplier, description);
        blockType.add(
                new mekanism.common.block.attribute.AttributeEnergy(() -> FloatingLong.create(50), () -> FloatingLong.create(40_000)),
                new mekanism.common.block.attribute.AttributeGui(containerSupplier::get, description),
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
                return "description.replicatemekanism.forensic_chamber";
            }
        };
    }
}