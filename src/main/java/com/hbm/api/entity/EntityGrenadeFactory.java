package com.hbm.api.entity;

import net.minecraft.core.Position;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;

@FunctionalInterface
public interface EntityGrenadeFactory {
    Projectile create(Level world, Position position);
}
