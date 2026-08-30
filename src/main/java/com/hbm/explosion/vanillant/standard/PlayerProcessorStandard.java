package com.hbm.explosion.vanillant.standard;

import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.interfaces.IPlayerProcessor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;

/**
 * CE: {@code PlayerProcessorStandard} - in 1.12, player movement is client-authoritative and ordinary
 * {@code motionX/Y/Z} changes made server-side (as {@code EntityProcessorStandard}/
 * {@code EntityProcessorCross} do) are <em>not</em> automatically pushed back to that player's own
 * client, so CE sends each affected player's knockback vector explicitly over a dedicated
 * {@code ExplosionKnockbackPacket} for the client to apply locally.
 * <p>
 * 1.21.1 no longer needs this: this port's {@code EntityProcessorStandard}/{@code EntityProcessorCross}
 * already set {@code player.hurtMarked = true} whenever they change a player's
 * {@code Entity#setDeltaMovement}, and modern vanilla's own per-tick entity tracking
 * ({@code ServerEntity#sendChanges()}) already checks that exact flag to push a
 * {@code ClientboundSetEntityMotionPacket} to that player and clear it again - the same job CE's own
 * packet did, now already handled by the engine. Sending an additional packet here that also nudges
 * the player's velocity would double-apply the knockback (once via vanilla's automatic resync, once
 * via a hand-rolled push), which would be a new bug, not a faithful port - so this class is
 * intentionally a documented no-op. The interface and this implementation are still ported (rather
 * than dropped, unlike Neo Edition's own {@code ExplosionVNT}, which removes the role entirely) purely
 * so {@link ExplosionVNT}'s four-role architecture and its {@code makeStandard()}/{@code makeAmat()}
 * presets stay structurally faithful to CE's shape, and so a future consumer that needs to add its own
 * player-side effect here has a real, already-wired extension point to override.
 */
public class PlayerProcessorStandard implements IPlayerProcessor {

    @Override
    public void process(ExplosionVNT explosion, Level level, double x, double y, double z, HashMap<Player, Vec3> affectedPlayers) {
        // Intentionally empty - see class javadoc.
    }
}
