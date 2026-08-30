package com.github.mochi7054.mixin;

import mekanism.api.Upgrade;
import mekanism.api.text.APILang;
import mekanism.api.text.EnumColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = Upgrade.class, remap = false)
public interface UpgradeInvoker {
    @Invoker("<init>")
    static Upgrade createUpgrade(String enumName, int ordinal, String name, APILang langKey, APILang descLangKey, int maxStack, EnumColor color) {
        throw new UnsupportedOperationException();
    }
}