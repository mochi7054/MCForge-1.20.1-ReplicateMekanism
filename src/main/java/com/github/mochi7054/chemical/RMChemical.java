package com.github.mochi7054.chemical;

import com.github.mochi7054.ReplicateMekanism;
import mekanism.api.chemical.infuse.InfuseType;
import mekanism.common.registration.impl.InfuseTypeDeferredRegister;
import mekanism.common.registration.impl.InfuseTypeRegistryObject;
import net.minecraft.resources.ResourceLocation;

public class RMChemical {
    public static final InfuseTypeDeferredRegister INFUSE_TYPES = new InfuseTypeDeferredRegister(ReplicateMekanism.MODID);

    public static final InfuseTypeRegistryObject<InfuseType> REPLICA = INFUSE_TYPES.register("replica", new ResourceLocation(ReplicateMekanism.MODID, "infuse_type/replica"), 0x6A8FCC);
    public static final InfuseTypeRegistryObject<InfuseType> EARTH_MATTER = INFUSE_TYPES.register("earth_matter", new ResourceLocation(ReplicateMekanism.MODID, "infuse_type/replica"), 0x48793C);
    public static final InfuseTypeRegistryObject<InfuseType> NETHER_MATTER = INFUSE_TYPES.register("nether_matter", new ResourceLocation(ReplicateMekanism.MODID, "infuse_type/replica"), 0x762525);
    public static final InfuseTypeRegistryObject<InfuseType> ORGANIC_MATTER = INFUSE_TYPES.register("organic_matter", new ResourceLocation(ReplicateMekanism.MODID, "infuse_type/replica"), 0xA97E36);
    public static final InfuseTypeRegistryObject<InfuseType> ENDER_MATTER = INFUSE_TYPES.register("ender_matter", new ResourceLocation(ReplicateMekanism.MODID, "infuse_type/replica"), 0x30938A);
    public static final InfuseTypeRegistryObject<InfuseType> METALLIC_MATTER = INFUSE_TYPES.register("metallic_matter", new ResourceLocation(ReplicateMekanism.MODID, "infuse_type/replica"), 0x8A939F);
    public static final InfuseTypeRegistryObject<InfuseType> PRECIOUS_MATTER = INFUSE_TYPES.register("precious_matter", new ResourceLocation(ReplicateMekanism.MODID, "infuse_type/replica"), 0xCCB94C);
    public static final InfuseTypeRegistryObject<InfuseType> LIVING_MATTER = INFUSE_TYPES.register("living_matter", new ResourceLocation(ReplicateMekanism.MODID, "infuse_type/replica"), 0xCC6E4C);
    public static final InfuseTypeRegistryObject<InfuseType> QUANTUM_MATTER = INFUSE_TYPES.register("quantum_matter", new ResourceLocation(ReplicateMekanism.MODID, "infuse_type/replica"), 0xAE4CCC);
}