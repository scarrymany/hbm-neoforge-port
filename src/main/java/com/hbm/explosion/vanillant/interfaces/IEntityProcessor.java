package com.hbm.explosion.vanillant.interfaces;

import com.hbm.explosion.vanillant.ExplosionVNT;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;

/**
 * CE: {@code IEntityProcessor}. Applies AoE damage/knockback to every entity in range and returns the
 * subset of affected {@link Player}s (mapped to their knockback vector) for {@link IPlayerProcessor}
 * to act on. {@code EntityPlayer}/{@code Vec3d} -&gt; {@link Player}/{@link Vec3}, {@code World} -&gt;
 * {@link Level}; otherwise identical to CE's shape.
 */
public interface IEntityProcessor {

    HashMap<Player, Vec3> process(ExplosionVNT explosion, Level level, double x, double y, double z, float size);
}
