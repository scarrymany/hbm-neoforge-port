package com.hbm.explosion.vanillant.interfaces;

import com.hbm.explosion.vanillant.ExplosionVNT;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;

/**
 * CE: {@code IPlayerProcessor}. In CE (1.12), player movement is client-authoritative, so CE's own
 * {@code PlayerProcessorStandard} pushes each affected player's knockback vector to their client over
 * a dedicated packet after {@link IEntityProcessor} computes it server-side. See this port's
 * {@code PlayerProcessorStandard} for why that packet push is no longer necessary in 1.21.1 (modern
 * vanilla already resyncs a knocked-back {@code Player}'s velocity via {@code player.hurtMarked}) -
 * the role/interface itself is still ported faithfully since {@link ExplosionVNT} treats it as one of
 * its four required pluggable roles.
 */
public interface IPlayerProcessor {

    void process(ExplosionVNT explosion, Level level, double x, double y, double z, HashMap<Player, Vec3> affectedPlayers);
}
