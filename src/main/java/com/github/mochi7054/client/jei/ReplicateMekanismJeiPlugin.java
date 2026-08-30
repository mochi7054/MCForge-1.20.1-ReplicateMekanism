package com.github.mochi7054.client.jei;

import com.github.mochi7054.ReplicateMekanism;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@JeiPlugin
public class ReplicateMekanismJeiPlugin implements IModPlugin {

    private static final ResourceLocation PLUGIN_UID = new ResourceLocation("replicatemekanism", "jei_plugin");

    @NotNull
    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_UID;
    }

    @Override
    public void registerRecipes(@NotNull IRecipeRegistration registration) {
        // Imaginator info
        List<ItemStack> imaginators = List.of(
            new ItemStack(ReplicateMekanism.IMAGINATOR_BLOCK.getBlock().asItem()),
            new ItemStack(ReplicateMekanism.IMAGINATOR_BASIC_BLOCK.getBlock().asItem()),
            new ItemStack(ReplicateMekanism.IMAGINATOR_ADVANCED_BLOCK.getBlock().asItem()),
            new ItemStack(ReplicateMekanism.IMAGINATOR_ELITE_BLOCK.getBlock().asItem()),
            new ItemStack(ReplicateMekanism.IMAGINATOR_ULTIMATE_BLOCK.getBlock().asItem())
        );
        registration.addIngredientInfo(imaginators, VanillaTypes.ITEM_STACK, Component.translatable("description.replicatemekanism.imaginator"));

        // Collapser info
        List<ItemStack> collapsers = List.of(
            new ItemStack(ReplicateMekanism.COLLAPSER_BLOCK.getBlock().asItem()),
            new ItemStack(ReplicateMekanism.COLLAPSER_BASIC_BLOCK.getBlock().asItem()),
            new ItemStack(ReplicateMekanism.COLLAPSER_ADVANCED_BLOCK.getBlock().asItem()),
            new ItemStack(ReplicateMekanism.COLLAPSER_ELITE_BLOCK.getBlock().asItem()),
            new ItemStack(ReplicateMekanism.COLLAPSER_ULTIMATE_BLOCK.getBlock().asItem())
        );
        registration.addIngredientInfo(collapsers, VanillaTypes.ITEM_STACK, Component.translatable("description.replicatemekanism.collapser"));
    }
}