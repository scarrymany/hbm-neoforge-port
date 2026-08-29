package com.hbm.api.tile;

import net.minecraft.network.chat.Component;
import net.minecraft.world.Nameable;

// Why mojang why didn't you make this interface?
public interface IWorldRenameable extends Nameable {
    void setCustomName(Component name);
}
