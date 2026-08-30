package com.github.mochi7054.client;

import com.github.mochi7054.ReplicateMekanism;
import com.github.mochi7054.collapser.CollapserScreen;
import com.github.mochi7054.collapser.RenderCollapser;
import com.github.mochi7054.imaginator.ImaginatorScreen;
import com.github.mochi7054.forensic.ForensicChamberScreen;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = ReplicateMekanism.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ReplicateMekanismClient {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ReplicateMekanism.COLLAPSER_CONTAINER.get(), CollapserScreen::new);
            MenuScreens.register(ReplicateMekanism.IMAGINATOR_CONTAINER.get(), ImaginatorScreen::new);
            MenuScreens.register(ReplicateMekanism.FORENSIC_CHAMBER_CONTAINER.get(), ForensicChamberScreen::new);
        });
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ReplicateMekanism.COLLAPSER_TILE.get(), RenderCollapser::new);
        event.registerBlockEntityRenderer(ReplicateMekanism.COLLAPSER_BASIC_TILE.get(), RenderCollapser::new);
        event.registerBlockEntityRenderer(ReplicateMekanism.COLLAPSER_ADVANCED_TILE.get(), RenderCollapser::new);
        event.registerBlockEntityRenderer(ReplicateMekanism.COLLAPSER_ELITE_TILE.get(), RenderCollapser::new);
        event.registerBlockEntityRenderer(ReplicateMekanism.COLLAPSER_ULTIMATE_TILE.get(), RenderCollapser::new);
    }
}