package com.hbm.blocks;

import net.minecraft.util.StringRepresentable;

import java.util.Locale;

public enum BlockControlPanelType implements StringRepresentable {
    CUSTOM_PANEL,
    FRONT_PANEL;

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
