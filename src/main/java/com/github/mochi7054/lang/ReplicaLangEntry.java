package com.github.mochi7054.lang;

import mekanism.api.text.ILangEntry;

public class ReplicaLangEntry implements ILangEntry {
    private final String key;

    public ReplicaLangEntry(String key) {
        this.key = key;
    }

    @Override
    public String getTranslationKey() {
        return key;
    }
}