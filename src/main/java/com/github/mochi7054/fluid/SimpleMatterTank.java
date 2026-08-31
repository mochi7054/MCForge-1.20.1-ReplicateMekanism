package com.github.mochi7054.fluid;

import com.buuz135.replication.api.IMatterType;
import com.buuz135.replication.api.matter_fluid.IMatterTank;
import com.buuz135.replication.api.matter_fluid.MatterStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;

public class SimpleMatterTank implements IMatterTank {
    private final IMatterType matterType;
    private final double capacity;
    private double stored;

    public SimpleMatterTank(IMatterType matterType, double capacity) {
        this.matterType = matterType;
        this.capacity = capacity;
        this.stored = 0;
    }

    public IMatterType getMatterType() {
        return matterType;
    }

    @Override
    public int getCapacity() {
        return (int) Math.round(capacity);
    }

    public double getCapacityDouble() {
        return capacity;
    }

    public double getStored() {
        return stored;
    }

    @Override
    public int getMatterAmount() {
        return (int) Math.round(stored);
    }

    public void setStored(double stored) {
        this.stored = Math.max(0, Math.min(capacity, stored));
    }

    public double getNeeded() {
        return Math.max(0, capacity - stored);
    }

    public boolean isEmpty() {
        return stored <= 0.00001;
    }

    public boolean isFull() {
        return stored >= capacity - 0.00001;
    }

    @Override
    public MatterStack getMatter() {
        if (isEmpty()) {
            return MatterStack.EMPTY;
        }
        return new MatterStack(matterType, (int) Math.round(stored));
    }

    public net.minecraftforge.fluids.FluidStack getFluidStack() {
        if (isEmpty() || matterType == null) {
            return net.minecraftforge.fluids.FluidStack.EMPTY;
        }
        var fluid = ReplicationFluidHandler.getFluidFromMatter(matterType);
        if (fluid != null) {
            return new net.minecraftforge.fluids.FluidStack(fluid, (int) Math.round(stored));
        }
        return net.minecraftforge.fluids.FluidStack.EMPTY;
    }

    @Override
    public boolean isMatterValid(MatterStack stack) {
        return stack != null && stack.getMatterType() == this.matterType;
    }

    @Override
    public int fill(MatterStack stack, FluidAction action) {
        if (stack == null || stack.isEmpty() || stack.getMatterType() != this.matterType) {
            return 0;
        }
        double toAdd = Math.min((double) stack.getAmount(), getNeeded());
        if (action.execute()) {
            this.stored += toAdd;
        }
        return (int) Math.round(toAdd);
    }

    public double fillDouble(double amount, FluidAction action) {
        if (amount <= 0) return 0;
        double toAdd = Math.min(amount, getNeeded());
        if (action.execute()) {
            this.stored += toAdd;
        }
        return toAdd;
    }

    @Override
    public MatterStack drain(int maxDrain, FluidAction action) {
        if (isEmpty() || maxDrain <= 0) {
            return MatterStack.EMPTY;
        }
        double drained = Math.min(this.stored, (double) maxDrain);
        if (action.execute()) {
            this.stored -= drained;
        }
        return new MatterStack(matterType, (int) Math.round(drained));
    }

    @Override
    public MatterStack drain(MatterStack stack, FluidAction action) {
        if (stack == null || stack.isEmpty() || stack.getMatterType() != this.matterType) {
            return MatterStack.EMPTY;
        }
        return drain(stack.getAmount(), action);
    }

    public double drainDouble(double toDrainAmount, FluidAction action) {
        if (isEmpty() || toDrainAmount <= 0) {
            return 0;
        }
        double drained = Math.min(this.stored, toDrainAmount);
        if (action.execute()) {
            this.stored -= drained;
        }
        return drained;
    }

    public CompoundTag writeToNBT(CompoundTag tag) {
        tag.putDouble("Stored", stored);
        return tag;
    }

    public void readFromNBT(CompoundTag tag) {
        if (tag.contains("Stored")) {
            this.stored = tag.getDouble("Stored");
        }
    }
}