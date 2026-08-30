package com.github.mochi7054;

import com.github.mochi7054.block.ReplicaTier;
import com.github.mochi7054.collapser.*;
import com.github.mochi7054.chemical.RMChemical;
import com.github.mochi7054.config.Config;
import com.github.mochi7054.forensic.*;
import com.github.mochi7054.imaginator.*;
import com.github.mochi7054.item.ReplicaTierInstallerItem;
import com.github.mochi7054.item.ReplicaUpgradeItem;
import com.github.mochi7054.network.ClearActiveCraftingPacket;
import com.github.mochi7054.network.ToggleAutoSortPacket;
import com.mojang.logging.LogUtils;
import mekanism.api.Upgrade;
import mekanism.common.registration.impl.BlockDeferredRegister;
import mekanism.common.registration.impl.BlockRegistryObject;
import mekanism.common.registration.impl.ContainerTypeDeferredRegister;
import mekanism.common.registration.impl.ContainerTypeRegistryObject;
import mekanism.common.registration.impl.ItemDeferredRegister;
import mekanism.common.registration.impl.ItemRegistryObject;
import mekanism.common.registration.impl.TileEntityTypeDeferredRegister;
import mekanism.common.registration.impl.TileEntityTypeRegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

@Mod(ReplicateMekanism.MODID)
public class ReplicateMekanism {

    public static final String MODID = "replicatemekanism";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final BlockDeferredRegister BLOCKS = new BlockDeferredRegister(MODID);
    public static final ItemDeferredRegister ITEMS = new ItemDeferredRegister(MODID);
    public static final TileEntityTypeDeferredRegister TILE_ENTITY_TYPES = new TileEntityTypeDeferredRegister(MODID);
    public static final ContainerTypeDeferredRegister CONTAINER_TYPES = new ContainerTypeDeferredRegister(MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static Upgrade REPLICA_UPGRADE_TYPE;
    public static final ItemRegistryObject<ReplicaUpgradeItem> REPLICA_UPGRADE_ITEM;

    public static final BlockRegistryObject<CollapserBlock, CollapserBlockItem> COLLAPSER_BLOCK;
    public static final TileEntityTypeRegistryObject<CollapserBlockEntity> COLLAPSER_TILE;

    public static final BlockRegistryObject<CollapserBlock, CollapserBlockItem> COLLAPSER_BASIC_BLOCK;
    public static final TileEntityTypeRegistryObject<CollapserBlockEntity> COLLAPSER_BASIC_TILE;

    public static final BlockRegistryObject<CollapserBlock, CollapserBlockItem> COLLAPSER_ADVANCED_BLOCK;
    public static final TileEntityTypeRegistryObject<CollapserBlockEntity> COLLAPSER_ADVANCED_TILE;

    public static final BlockRegistryObject<CollapserBlock, CollapserBlockItem> COLLAPSER_ELITE_BLOCK;
    public static final TileEntityTypeRegistryObject<CollapserBlockEntity> COLLAPSER_ELITE_TILE;

    public static final BlockRegistryObject<CollapserBlock, CollapserBlockItem> COLLAPSER_ULTIMATE_BLOCK;
    public static final TileEntityTypeRegistryObject<CollapserBlockEntity> COLLAPSER_ULTIMATE_TILE;

    public static final ContainerTypeRegistryObject<CollapserMenu> COLLAPSER_CONTAINER;

    public static final BlockRegistryObject<ImaginatorBlock, ImaginatorBlockItem> IMAGINATOR_BLOCK;
    public static final TileEntityTypeRegistryObject<ImaginatorBlockEntity> IMAGINATOR_TILE;

    public static final BlockRegistryObject<ImaginatorBlock, ImaginatorBlockItem> IMAGINATOR_BASIC_BLOCK;
    public static final TileEntityTypeRegistryObject<ImaginatorBlockEntity> IMAGINATOR_BASIC_TILE;

    public static final BlockRegistryObject<ImaginatorBlock, ImaginatorBlockItem> IMAGINATOR_ADVANCED_BLOCK;
    public static final TileEntityTypeRegistryObject<ImaginatorBlockEntity> IMAGINATOR_ADVANCED_TILE;

    public static final BlockRegistryObject<ImaginatorBlock, ImaginatorBlockItem> IMAGINATOR_ELITE_BLOCK;
    public static final TileEntityTypeRegistryObject<ImaginatorBlockEntity> IMAGINATOR_ELITE_TILE;

    public static final BlockRegistryObject<ImaginatorBlock, ImaginatorBlockItem> IMAGINATOR_ULTIMATE_BLOCK;
    public static final TileEntityTypeRegistryObject<ImaginatorBlockEntity> IMAGINATOR_ULTIMATE_TILE;

    public static final ContainerTypeRegistryObject<ImaginatorMenu> IMAGINATOR_CONTAINER;

    public static final BlockRegistryObject<ForensicChamberBlock, net.minecraft.world.item.BlockItem> FORENSIC_CHAMBER_BLOCK;
    public static final TileEntityTypeRegistryObject<ForensicChamberBlockEntity> FORENSIC_CHAMBER_TILE;
    public static final ContainerTypeRegistryObject<ForensicChamberMenu> FORENSIC_CHAMBER_CONTAINER;

    public static final ItemRegistryObject<ReplicaTierInstallerItem> REPLICA_TIER_INSTALLER;

    public static final RegistryObject<CreativeModeTab> REPLICATEMEKANISM_TAB;

    public static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel PACKET_HANDLER = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    static {
        REPLICA_UPGRADE_ITEM = ITEMS.register("replica_upgrade", ReplicaUpgradeItem::new);

        COLLAPSER_BLOCK = BLOCKS.registerDefaultProperties("collapser",
                () -> new CollapserBlock(ReplicaTier.STANDARD, BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.5F, 16.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()),
                CollapserBlockItem::new);
        COLLAPSER_TILE = TILE_ENTITY_TYPES.register(COLLAPSER_BLOCK, CollapserBlockEntity::new);

        COLLAPSER_BASIC_BLOCK = BLOCKS.registerDefaultProperties("collapser_basic",
                () -> new CollapserBlock(ReplicaTier.BASIC, BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.5F, 16.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()),
                CollapserBlockItem::new);
        COLLAPSER_BASIC_TILE = TILE_ENTITY_TYPES.register(COLLAPSER_BASIC_BLOCK, CollapserBlockEntity::new);

        COLLAPSER_ADVANCED_BLOCK = BLOCKS.registerDefaultProperties("collapser_advanced",
                () -> new CollapserBlock(ReplicaTier.ADVANCED, BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.5F, 16.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()),
                CollapserBlockItem::new);
        COLLAPSER_ADVANCED_TILE = TILE_ENTITY_TYPES.register(COLLAPSER_ADVANCED_BLOCK, CollapserBlockEntity::new);

        COLLAPSER_ELITE_BLOCK = BLOCKS.registerDefaultProperties("collapser_elite",
                () -> new CollapserBlock(ReplicaTier.ELITE, BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.5F, 16.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()),
                CollapserBlockItem::new);
        COLLAPSER_ELITE_TILE = TILE_ENTITY_TYPES.register(COLLAPSER_ELITE_BLOCK, CollapserBlockEntity::new);

        COLLAPSER_ULTIMATE_BLOCK = BLOCKS.registerDefaultProperties("collapser_ultimate",
                () -> new CollapserBlock(ReplicaTier.ULTIMATE, BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.5F, 16.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()),
                CollapserBlockItem::new);
        COLLAPSER_ULTIMATE_TILE = TILE_ENTITY_TYPES.register(COLLAPSER_ULTIMATE_BLOCK, CollapserBlockEntity::new);

        COLLAPSER_CONTAINER = CONTAINER_TYPES.register("collapser", CollapserBlockEntity.class, CollapserMenu::new);

        IMAGINATOR_BLOCK = BLOCKS.registerDefaultProperties("imaginator",
                () -> new ImaginatorBlock(ReplicaTier.STANDARD, BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.5F, 16.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()),
                ImaginatorBlockItem::new);
        IMAGINATOR_TILE = TILE_ENTITY_TYPES.register(IMAGINATOR_BLOCK, ImaginatorBlockEntity::new);

        IMAGINATOR_BASIC_BLOCK = BLOCKS.registerDefaultProperties("imaginator_basic",
                () -> new ImaginatorBlock(ReplicaTier.BASIC, BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.5F, 16.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()),
                ImaginatorBlockItem::new);
        IMAGINATOR_BASIC_TILE = TILE_ENTITY_TYPES.register(IMAGINATOR_BASIC_BLOCK, ImaginatorBlockEntity::new);

        IMAGINATOR_ADVANCED_BLOCK = BLOCKS.registerDefaultProperties("imaginator_advanced",
                () -> new ImaginatorBlock(ReplicaTier.ADVANCED, BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.5F, 16.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()),
                ImaginatorBlockItem::new);
        IMAGINATOR_ADVANCED_TILE = TILE_ENTITY_TYPES.register(IMAGINATOR_ADVANCED_BLOCK, ImaginatorBlockEntity::new);

        IMAGINATOR_ELITE_BLOCK = BLOCKS.registerDefaultProperties("imaginator_elite",
                () -> new ImaginatorBlock(ReplicaTier.ELITE, BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.5F, 16.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()),
                ImaginatorBlockItem::new);
        IMAGINATOR_ELITE_TILE = TILE_ENTITY_TYPES.register(IMAGINATOR_ELITE_BLOCK, ImaginatorBlockEntity::new);

        IMAGINATOR_ULTIMATE_BLOCK = BLOCKS.registerDefaultProperties("imaginator_ultimate",
                () -> new ImaginatorBlock(ReplicaTier.ULTIMATE, BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.5F, 16.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()),
                ImaginatorBlockItem::new);
        IMAGINATOR_ULTIMATE_TILE = TILE_ENTITY_TYPES.register(IMAGINATOR_ULTIMATE_BLOCK, ImaginatorBlockEntity::new);

        IMAGINATOR_CONTAINER = CONTAINER_TYPES.register("imaginator", ImaginatorBlockEntity.class, ImaginatorMenu::new);

        FORENSIC_CHAMBER_BLOCK = BLOCKS.register("forensic_chamber",
                () -> new ForensicChamberBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.5F, 16.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()));
        FORENSIC_CHAMBER_TILE = TILE_ENTITY_TYPES.register(FORENSIC_CHAMBER_BLOCK, ForensicChamberBlockEntity::new);
        FORENSIC_CHAMBER_CONTAINER = CONTAINER_TYPES.register("forensic_chamber", ForensicChamberBlockEntity.class, ForensicChamberMenu::new);

        REPLICA_TIER_INSTALLER = ITEMS.register("replica_tier_installer", ReplicaTierInstallerItem::new);

        REPLICATEMEKANISM_TAB = CREATIVE_MODE_TABS.register("tab", () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.replicatemekanism"))
                .icon(() -> new ItemStack(REPLICA_UPGRADE_ITEM.get()))
                .displayItems((parameters, output) -> {
                    output.accept(REPLICA_UPGRADE_ITEM.get());
                    output.accept(REPLICA_TIER_INSTALLER.get());

                    output.accept(COLLAPSER_BLOCK.getBlock().asItem());
                    output.accept(COLLAPSER_BASIC_BLOCK.getBlock().asItem());
                    output.accept(COLLAPSER_ADVANCED_BLOCK.getBlock().asItem());
                    output.accept(COLLAPSER_ELITE_BLOCK.getBlock().asItem());
                    output.accept(COLLAPSER_ULTIMATE_BLOCK.getBlock().asItem());

                    output.accept(IMAGINATOR_BLOCK.getBlock().asItem());
                    output.accept(IMAGINATOR_BASIC_BLOCK.getBlock().asItem());
                    output.accept(IMAGINATOR_ADVANCED_BLOCK.getBlock().asItem());
                    output.accept(IMAGINATOR_ELITE_BLOCK.getBlock().asItem());
                    output.accept(IMAGINATOR_ULTIMATE_BLOCK.getBlock().asItem());

                    output.accept(FORENSIC_CHAMBER_BLOCK.getBlock().asItem());
                })
                .build()
        );
    }

    public ReplicateMekanism() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        TILE_ENTITY_TYPES.register(modEventBus);
        CONTAINER_TYPES.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        RMChemical.INFUSE_TYPES.register(modEventBus);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        modEventBus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(this);
    }

    public static void checkAndAwardCheatedAdvancement(ServerPlayer player) {
        if (player == null) return;
        var adv = player.server.getAdvancements().getAdvancement(new ResourceLocation(MODID, "cheat"));
        if (adv != null) {
            var progress = player.getAdvancements().getOrStartProgress(adv);
            if (!progress.isDone()) {
                for (String criteria : progress.getRemainingCriteria()) {
                    player.getAdvancements().award(adv, criteria);
                }
            }
        }
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("ReplicateMekanism Common Setup (MC 1.20.1)");

        int id = 0;
        PACKET_HANDLER.registerMessage(id++, ClearActiveCraftingPacket.class,
                ClearActiveCraftingPacket::encode,
                ClearActiveCraftingPacket::decode,
                ClearActiveCraftingPacket::handle);

        PACKET_HANDLER.registerMessage(id++, ToggleAutoSortPacket.class,
                ToggleAutoSortPacket::encode,
                ToggleAutoSortPacket::decode,
                ToggleAutoSortPacket::handle);
    }
}