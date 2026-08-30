package com.github.mochi7054.item;

import com.github.mochi7054.ReplicateMekanism;
import com.buuz135.replication.block.ReplicatorBlock;
import com.buuz135.replication.block.DisintegratorBlock;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraft.world.Containers;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ReplicaTierInstallerItem extends Item {

    private static final TextColor REPLICA_COLOR = TextColor.fromRgb(0x75FF89);

    public ReplicaTierInstallerItem(Properties properties) {
        super(properties);
    }

    public ReplicaTierInstallerItem(Properties properties, TextColor nameColor) {
        super(properties);
    }

    @Override
    public MutableComponent getName(ItemStack stack) {
        return super.getName(stack).copy().withStyle(style -> style.withColor(REPLICA_COLOR));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);

        boolean isReplicator = state.getBlock() instanceof ReplicatorBlock;
        boolean isDisintegrator = state.getBlock() instanceof DisintegratorBlock;
        boolean isIdentificationChamber = state.getBlock() instanceof com.buuz135.replication.block.IdentificationChamberBlock;

        if (!isReplicator && !isDisintegrator && !isIdentificationChamber) {
            return InteractionResult.FAIL;
        }

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        Player player = context.getPlayer();

        if (player != null && player instanceof net.minecraft.server.level.ServerPlayer) {
            if (!level.mayInteract(player, pos)) {
                return InteractionResult.FAIL;
            }
        }

        BlockEntity oldTile = level.getBlockEntity(pos);
        List<ItemStack> savedItems = new ArrayList<>();
        List<FluidStack> savedFluids = new ArrayList<>();

        if (oldTile != null) {
            oldTile.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(itemHandler -> {
                for (int i = 0; i < itemHandler.getSlots(); i++) {
                    ItemStack st = itemHandler.getStackInSlot(i);
                    if (!st.isEmpty()) {
                        savedItems.add(st.copy());
                    }
                }
            });

            oldTile.getCapability(ForgeCapabilities.FLUID_HANDLER).ifPresent(fluidHandler -> {
                for (int i = 0; i < fluidHandler.getTanks(); i++) {
                    FluidStack fl = fluidHandler.getFluidInTank(i);
                    if (!fl.isEmpty()) {
                        savedFluids.add(fl.copy());
                    }
                }
            });
        }

        Direction facing = Direction.NORTH;
        for (net.minecraft.world.level.block.state.properties.Property<?> property : state.getProperties()) {
            if (property instanceof net.minecraft.world.level.block.state.properties.DirectionProperty dirProp) {
                facing = state.getValue(dirProp);
                break;
            }
        }

        Block targetBlock;
        if (isIdentificationChamber) {
            targetBlock = ReplicateMekanism.FORENSIC_CHAMBER_BLOCK.getBlock();
        } else if (isReplicator) {
            targetBlock = ReplicateMekanism.IMAGINATOR_BLOCK.getBlock();
        } else {
            targetBlock = ReplicateMekanism.COLLAPSER_BLOCK.getBlock();
        }

        BlockState newState = targetBlock.defaultBlockState();
        for (net.minecraft.world.level.block.state.properties.Property<?> property : newState.getProperties()) {
            if (property instanceof net.minecraft.world.level.block.state.properties.DirectionProperty dirProp) {
                if (dirProp.getPossibleValues().contains(facing)) {
                    newState = newState.setValue(dirProp, facing);
                } else if (facing.getAxis().isHorizontal() && dirProp.getPossibleValues().contains(facing)) {
                    newState = newState.setValue(dirProp, facing);
                }
            }
        }

        level.setBlockAndUpdate(pos, newState);

        BlockEntity newTile = level.getBlockEntity(pos);
        if (newTile instanceof mekanism.common.tile.base.TileEntityMekanism mekTile) {
            mekTile.setFacing(facing);
        }
        if (newTile != null) {
            final List<ItemStack> uninsertedItems = new ArrayList<>();
            newTile.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(newHandler -> {
                for (ItemStack st : savedItems) {
                    ItemStack remainder = ItemHandlerHelper.insertItemStacked(newHandler, st, false);
                    if (!remainder.isEmpty()) {
                        uninsertedItems.add(remainder);
                    }
                }
            });

            for (ItemStack rem : uninsertedItems) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), rem);
            }

            newTile.getCapability(ForgeCapabilities.FLUID_HANDLER).ifPresent(newFluidHandler -> {
                for (FluidStack fl : savedFluids) {
                    newFluidHandler.fill(fl, net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
                }
            });

            newTile.setChanged();
        }

        if (player != null && !player.isCreative()) {
            context.getItemInHand().shrink(1);
        }

        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            var adv = serverPlayer.server.getAdvancements().getAdvancement(new net.minecraft.resources.ResourceLocation(ReplicateMekanism.MODID, "upgrade_machine"));
            if (adv != null) {
                var progress = serverPlayer.getAdvancements().getOrStartProgress(adv);
                if (!progress.isDone()) {
                    for (String criteria : progress.getRemainingCriteria()) {
                        serverPlayer.getAdvancements().award(adv, criteria);
                    }
                }
            }
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, level, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable("description.replicatemekanism.replica_tier_installer"));
    }
}