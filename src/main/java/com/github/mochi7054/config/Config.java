package com.github.mochi7054.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.IntValue REPLICA_UPGRADE_MAX_STACK = BUILDER
            .comment("The maximum stack size for the replica upgrade (1-8)")
            .translation("replicatemekanism.configuration.replicaUpgradeMaxStack")
            .defineInRange("replicaUpgradeMaxStack", 8, 1, 8);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    public static int getReplicaUpgradeMaxStack() {
        try {
            return REPLICA_UPGRADE_MAX_STACK.get();
        } catch (Exception e) {
            return 8;
        }
    }
}