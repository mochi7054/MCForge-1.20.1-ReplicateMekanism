package com.github.mochi7054.block;

import mekanism.api.math.FloatingLong;
import mekanism.api.tier.BaseTier;
import mekanism.api.tier.ITier;
import net.minecraft.network.chat.TextColor;

public enum ReplicaTier implements ITier {
    STANDARD("standard", null,          1, 10000, 1000,   FloatingLong.createConst(100000L), TextColor.fromRgb(0xFFFFFF)),
    BASIC   ("basic",    BaseTier.BASIC, 3, 20000, 2000,   FloatingLong.createConst(200000L), TextColor.fromRgb(0x5FFFB8)),
    ADVANCED("advanced", BaseTier.ADVANCED, 5, 40000, 4000, FloatingLong.createConst(400000L), TextColor.fromRgb(0xFF806A)),
    ELITE   ("elite",    BaseTier.ELITE, 7, 80000, 8000,   FloatingLong.createConst(800000L),  TextColor.fromRgb(0x4BF8FF)),
    ULTIMATE("ultimate", BaseTier.ULTIMATE, 9, 160000, 16000, FloatingLong.createConst(1600000L), TextColor.fromRgb(0xF787FF));

    private final String name;
    private final BaseTier baseTier;
    private final int slotCount;
    private final int tankCapacity;
    private final int imaginatorTankCapacity;
    private final FloatingLong energyCapacity;
    private final TextColor textColor;

    ReplicaTier(String name, BaseTier baseTier, int slotCount, int tankCapacity, int imaginatorTankCapacity, FloatingLong energyCapacity, TextColor textColor) {
        this.name = name;
        this.baseTier = baseTier;
        this.slotCount = slotCount;
        this.tankCapacity = tankCapacity;
        this.imaginatorTankCapacity = imaginatorTankCapacity;
        this.energyCapacity = energyCapacity;
        this.textColor = textColor;
    }

    public String getName() {
        return name;
    }

    @Override
    public BaseTier getBaseTier() {
        return baseTier;
    }

    public int getSlots() { return slotCount; }

    public int getSlotCount() {
        return slotCount;
    }

    public int getTankCapacity() {
        return tankCapacity;
    }

    public int getImaginatorTankCapacity() {
        return imaginatorTankCapacity;
    }

    public FloatingLong getEnergyCapacity() {
        return energyCapacity;
    }

    public TextColor getTextColor() {
        return textColor;
    }
}