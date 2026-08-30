package com.github.mochi7054.fluid;

import mekanism.common.tile.prefab.TileEntityConfigurableMachine;
import mekanism.common.util.MekanismUtils;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import java.util.List;

public class ReplicationFluidHandler implements IFluidHandler {
    private final TileEntityConfigurableMachine tile;
    private final List<SimpleMatterTank> tanks;
    private final Direction side;

    public ReplicationFluidHandler(TileEntityConfigurableMachine tile, List<SimpleMatterTank> tanks, Direction side) {
        this.tile = tile;
        this.tanks = tanks;
        this.side = side;
    }

    private boolean canInput() {
        if (side == null) return true;
        if (tile.getConfig() == null) return true;
        var sideConfig = tile.getConfig().getConfig(mekanism.common.lib.transmitter.TransmissionType.FLUID);
        if (sideConfig == null) return true;
        var dataType = sideConfig.getDataType(mekanism.api.RelativeSide.fromDirections(tile.getDirection(), side));
        return dataType != null && dataType != mekanism.common.tile.component.config.DataType.NONE && !dataType.canOutput();
    }

    private boolean canOutput() {
        if (side == null) return true;
        if (tile.getConfig() == null) return true;
        var sideConfig = tile.getConfig().getConfig(mekanism.common.lib.transmitter.TransmissionType.FLUID);
        if (sideConfig == null) return true;
        var dataType = sideConfig.getDataType(mekanism.api.RelativeSide.fromDirections(tile.getDirection(), side));
        return dataType != null && dataType.canOutput();
    }

    private SimpleMatterTank getTankForFluid(Fluid fluid) {
        ResourceLocation id = BuiltInRegistries.FLUID.getKey(fluid);
        if (id == null || !"replication".equals(id.getNamespace())) {
            return null;
        }
        String path = id.getPath();
        for (SimpleMatterTank tank : tanks) {
            if (path.equals("matter_" + tank.getMatterType().getName())) {
                return tank;
            }
        }
        return null;
    }

    public static Fluid getFluidFromMatter(com.buuz135.replication.api.IMatterType matterType) {
        if (matterType == null) return null;
        ResourceLocation id = new ResourceLocation("replication", "matter_" + matterType.getName());
        return BuiltInRegistries.FLUID.get(id);
    }

    private Fluid getFluidForTank(SimpleMatterTank tank) {
        return getFluidFromMatter(tank.getMatterType());
    }

    @Override
    public int getTanks() {
        return tanks.size();
    }

    @Override
    public FluidStack getFluidInTank(int tankIndex) {
        if (tankIndex < 0 || tankIndex >= tanks.size()) {
            return FluidStack.EMPTY;
        }
        SimpleMatterTank tank = tanks.get(tankIndex);
        if (tank.isEmpty()) {
            return FluidStack.EMPTY;
        }
        Fluid fluid = getFluidForTank(tank);
        if (fluid == null) {
            return FluidStack.EMPTY;
        }
        return new FluidStack(fluid, (int) Math.round(tank.getStored()));
    }

    @Override
    public int getTankCapacity(int tankIndex) {
        if (tankIndex < 0 || tankIndex >= tanks.size()) {
            return 0;
        }
        return (int) Math.round(tanks.get(tankIndex).getCapacityDouble());
    }

    @Override
    public boolean isFluidValid(int tankIndex, FluidStack stack) {
        if (stack.isEmpty() || tankIndex < 0 || tankIndex >= tanks.size()) {
            return false;
        }
        SimpleMatterTank tank = tanks.get(tankIndex);
        return getTankForFluid(stack.getFluid()) == tank;
    }

    @Override
    public int fill(FluidStack stack, FluidAction action) {
        if (stack.isEmpty() || !canInput()) {
            return 0;
        }
        SimpleMatterTank tank = getTankForFluid(stack.getFluid());
        if (tank == null) {
            return 0;
        }
        com.buuz135.replication.api.matter_fluid.MatterStack matterStack =
                new com.buuz135.replication.api.matter_fluid.MatterStack(tank.getMatterType(), stack.getAmount());
        int filled = tank.fill(matterStack, action);
        if (filled > 0 && action.execute()) {
            tile.markForSave();
        }
        return filled;
    }

    @Override
    public FluidStack drain(FluidStack stack, FluidAction action) {
        if (stack.isEmpty() || !canOutput()) {
            return FluidStack.EMPTY;
        }
        SimpleMatterTank tank = getTankForFluid(stack.getFluid());
        if (tank == null || tank.isEmpty()) {
            return FluidStack.EMPTY;
        }
        int drained = tank.drain(stack.getAmount(), action).getAmount();
        if (drained > 0 && action.execute()) {
            tile.markForSave();
        }
        Fluid fluid = getFluidForTank(tank);
        return new FluidStack(fluid, drained);
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        if (maxDrain <= 0 || !canOutput()) {
            return FluidStack.EMPTY;
        }
        for (SimpleMatterTank tank : tanks) {
            if (!tank.isEmpty()) {
                int drained = tank.drain(maxDrain, action).getAmount();
                if (drained > 0 && action.execute()) {
                    tile.markForSave();
                }
                Fluid fluid = getFluidForTank(tank);
                return new FluidStack(fluid, drained);
            }
        }
        return FluidStack.EMPTY;
    }
}