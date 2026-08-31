package com.hbm.interfaces;

import com.hbm.config.MachineConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntity;

public interface IDoor {

    void open();

    void close();

    DoorState getState();

    void toggle();

    default boolean setTexture(String tex) {
        return false;
    }

    default void setTextureState(byte tex) {
    }

    boolean getRedstoneOnly();

    default Mode getConfiguredMode() {
        String name = BuiltInRegistries.BLOCK.getKey(((BlockEntity) this).getBlockState().getBlock()).toString();
        java.util.Map<String, String> conf = MachineConfig.doorConf();
        String mode = conf.get(name);
        if (mode == null) mode = conf.getOrDefault("ALL", "DEFAULT");
        try {
            return Mode.valueOf(mode);
        } catch (IllegalArgumentException e) {
            return Mode.DEFAULT;
        }
    }

    default boolean isRedstoneOnly() {
        Mode mode = getConfiguredMode();
        if (mode == Mode.REDSTONE) return true;
        if (mode == Mode.DEFAULT) return false;
        return getRedstoneOnly();
    }

    void setRedstoneOnly(boolean redstoneOnly);

    default void toggleRedstoneMode() {
        this.setRedstoneOnly(!this.getRedstoneOnly());
        ((BlockEntity) this).setChanged();
    }

    enum DoorState {
        CLOSED, OPEN, CLOSING, OPENING;

        public static final DoorState[] VALUES = values();

        public boolean isStationaryState() {
            return (this == CLOSED || this == OPEN);
        }

        public boolean isMovingState() {
            return (this == CLOSING || this == OPENING);
        }
    }

    enum Mode {
        DEFAULT,
        TOOLABLE,
        REDSTONE
    }
}
