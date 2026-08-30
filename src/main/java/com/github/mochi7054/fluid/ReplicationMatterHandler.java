package com.github.mochi7054.fluid;

import com.buuz135.replication.api.IMatterType;
import com.buuz135.replication.api.matter_fluid.IMatterHandler;
import com.buuz135.replication.api.matter_fluid.MatterStack;
import mekanism.common.tile.prefab.TileEntityConfigurableMachine;
import mekanism.common.util.MekanismUtils;
import net.minecraft.core.Direction;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;

import java.util.List;

public class ReplicationMatterHandler implements IMatterHandler {
    private final TileEntityConfigurableMachine tile;
    private final List<SimpleMatterTank> tanks;
    private final Direction side;

    public ReplicationMatterHandler(TileEntityConfigurableMachine tile, List<SimpleMatterTank> tanks, Direction side) {
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

    private SimpleMatterTank getTankForMatter(IMatterType matterType) {
        for (SimpleMatterTank tank : tanks) {
            if (tank.getMatterType() == matterType) {
                return tank;
            }
        }
        return null;
    }

    @Override
    public int getTanks() {
        return tanks.size();
    }

    @Override
    public MatterStack getMatterInTank(int tankIndex) {
        if (tankIndex < 0 || tankIndex >= tanks.size()) {
            return MatterStack.EMPTY;
        }
        return tanks.get(tankIndex).getMatter();
    }

    @Override
    public int getTankCapacity(int tankIndex) {
        if (tankIndex < 0 || tankIndex >= tanks.size()) {
            return 0;
        }
        return tanks.get(tankIndex).getCapacity();
    }

    @Override
    public boolean isMatterValid(int tankIndex, MatterStack stack) {
        if (stack.isEmpty() || tankIndex < 0 || tankIndex >= tanks.size()) {
            return false;
        }
        return tanks.get(tankIndex).getMatterType() == stack.getMatterType();
    }

    @Override
    public int fill(MatterStack stack, FluidAction action) {
        if (stack.isEmpty() || !canInput()) {
            return 0;
        }
        SimpleMatterTank tank = getTankForMatter(stack.getMatterType());
        if (tank == null) {
            return 0;
        }
        int filled = tank.fill(stack, action);
        if (filled > 0 && action.execute()) {
            tile.markForSave();
        }
        return filled;
    }

    @Override
    public MatterStack drain(MatterStack stack, FluidAction action) {
        if (stack.isEmpty() || !canOutput()) {
            return MatterStack.EMPTY;
        }
        SimpleMatterTank tank = getTankForMatter(stack.getMatterType());
        if (tank == null || tank.isEmpty()) {
            return MatterStack.EMPTY;
        }
        MatterStack drained = tank.drain(stack, action);
        if (!drained.isEmpty() && action.execute()) {
            tile.markForSave();
        }
        return drained;
    }

    @Override
    public MatterStack drain(int amount, FluidAction action) {
        if (amount <= 0 || !canOutput()) {
            return MatterStack.EMPTY;
        }
        for (SimpleMatterTank tank : tanks) {
            if (!tank.isEmpty()) {
                MatterStack drained = tank.drain(amount, action);
                if (!drained.isEmpty() && action.execute()) {
                    tile.markForSave();
                }
                return drained;
            }
        }
        return MatterStack.EMPTY;
    }
}