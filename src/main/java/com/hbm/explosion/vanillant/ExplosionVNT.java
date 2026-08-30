package com.hbm.explosion.vanillant;

import com.hbm.explosion.vanillant.interfaces.IBlockAllocator;
import com.hbm.explosion.vanillant.interfaces.IBlockProcessor;
import com.hbm.explosion.vanillant.interfaces.IEntityProcessor;
import com.hbm.explosion.vanillant.interfaces.IExplosionSFX;
import com.hbm.explosion.vanillant.interfaces.IPlayerProcessor;
import com.hbm.explosion.vanillant.standard.BlockAllocatorStandard;
import com.hbm.explosion.vanillant.standard.BlockProcessorStandard;
import com.hbm.explosion.vanillant.standard.CustomDamageHandlerAmat;
import com.hbm.explosion.vanillant.standard.EntityProcessorStandard;
import com.hbm.explosion.vanillant.standard.ExplosionEffectAmat;
import com.hbm.explosion.vanillant.standard.ExplosionEffectStandard;
import com.hbm.explosion.vanillant.standard.PlayerProcessorStandard;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * CE: {@code com.hbm.explosion.vanillant.ExplosionVNT} - a 4-role pluggable strategy object for
 * "normal" (non nuke-tier) explosions, CE's own most-consumed explosion primitive (grenades,
 * warheads, non-nuke bombs - 65 consumer files in CE). CE's own class javadoc:
 * "Time to over-engineer this into fucking oblivion so that I never have to write a vanilla-esque
 * explosion class ever again."
 * <p>
 * The four roles, called in a fixed order from {@link #explode()}:
 * <ol>
 *     <li>{@link IBlockAllocator#allocate} - which {@link BlockPos} are affected.</li>
 *     <li>{@link IEntityProcessor#process} - AoE damage/knockback, returns the affected players.</li>
 *     <li>{@link IBlockProcessor#process} - drops/removal/{@code IBlockMutator} hooks for every
 *     affected block.</li>
 *     <li>{@link IPlayerProcessor#process} - historically pushed knockback to affected players over
 *     the network (see that interface's javadoc for why 1.21.1 usually no longer needs to).</li>
 * </ol>
 * plus any number of {@link IExplosionSFX} callbacks fired last (sound/particles/client packets).
 * <p>
 * {@link #compat} is a real vanilla {@link Explosion} kept alive purely as a compatibility adapter so
 * {@code BlockState#canDropFromExplosion}/{@code getExplosionResistance}, {@code Entity#hurt}'s
 * explosion-flavored {@code DamageSource}, and NeoForge's {@code EventHooks#onExplosionDetonate} all
 * still see a real {@link Explosion} instance. This class never calls {@code compat.explode()}/
 * {@code finalizeExplosion()} - every actual world mutation goes through the four roles above, exactly
 * as in CE. Unlike CE's 1.12 original (which reconstructs a second {@code Explosion} instance mid-
 * {@code explode()} purely to flip its {@code isSmoking} flag to {@code true}), modern
 * {@link Explosion#getToBlow()}/{@code getHitPlayers()} are already mutable collections this class
 * owns via the anonymous subclass below, so the single instance built in the constructor is kept and
 * simply filled in as allocation/entity-processing complete - a simplification made possible by the
 * new API shape, not a CE behavior change (this port's {@code compat} still never drives its own
 * block removal, exactly like CE's).
 */
public class ExplosionVNT {

    // explosions only need one of each of these; the unlikely "combine different types" case can be
    // handled with a wrapper implementing one of these interfaces as a chainloader, per CE's own comment.
    private IBlockAllocator blockAllocator;
    private IEntityProcessor entityProcessor;
    private IBlockProcessor blockProcessor;
    private IPlayerProcessor playerProcessor;
    // reduced to the bare minimum per effect (sound, particles, etc. kept separate), so more than one is common.
    private IExplosionSFX[] sfx;

    public Level world;
    public double posX;
    public double posY;
    public double posZ;
    public float size;
    @Nullable
    public Entity exploder;

    private final Map<Player, Vec3> compatPlayers = new HashMap<>();
    public Explosion compat;

    public ExplosionVNT(Level world, double x, double y, double z, float size) {
        this(world, x, y, z, size, null);
    }

    // CE: "I am lazy" - center-of-block convenience constructor.
    public ExplosionVNT(Level world, BlockPos pos, float size) {
        this(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, size, null);
    }

    public ExplosionVNT(Level world, double x, double y, double z, float size, @Nullable Entity exploder) {
        this.world = world;
        this.posX = x;
        this.posY = y;
        this.posZ = z;
        this.size = size;
        this.exploder = exploder;

        // CE: `new Explosion(world, exploder, x, y, z, size, false, false)`. The trailing two 1.12
        // booleans (isFlaming, isSmoking) collapse into one `Explosion.BlockInteraction` in 1.21.1;
        // `null` here (confirmed real, compiling shape via Neo Edition's own ExplosionVNT) means this
        // compat object is never asked to resolve its own block-interaction behavior, which is correct
        // since it never drives block removal itself.
        this.compat = new Explosion(world, exploder, x, y, z, size, false, null) {

            @Override
            public Map<Player, Vec3> getHitPlayers() {
                return ExplosionVNT.this.compatPlayers;
            }

            @Override
            @Nullable
            public Entity getDirectSourceEntity() {
                return ExplosionVNT.this.exploder;
            }

            @Override
            public float radius() {
                return ExplosionVNT.this.size;
            }
        };
    }

    public void explode() {

        boolean processBlocks = blockAllocator != null && blockProcessor != null;
        boolean processEntities = entityProcessor != null && playerProcessor != null;

        HashSet<BlockPos> affectedBlocks = null;
        HashMap<Player, Vec3> affectedPlayers = null;

        // allocation
        if (processBlocks) affectedBlocks = blockAllocator.allocate(this, world, posX, posY, posZ, size);
        if (processEntities) affectedPlayers = entityProcessor.process(this, world, posX, posY, posZ, size);

        // serverside processing
        if (processBlocks) blockProcessor.process(this, world, posX, posY, posZ, affectedBlocks);
        if (processEntities) playerProcessor.process(this, world, posX, posY, posZ, affectedPlayers);

        // compat - see class javadoc for why this doesn't reconstruct a second Explosion like CE does.
        if (processBlocks) this.compat.getToBlow().addAll(affectedBlocks);
        if (processEntities) this.compatPlayers.putAll(affectedPlayers);

        if (sfx != null) {
            for (IExplosionSFX fx : sfx) {
                fx.doEffect(this, world, posX, posY, posZ, size);
            }
        }
    }

    public ExplosionVNT setBlockAllocator(IBlockAllocator blockAllocator) {
        this.blockAllocator = blockAllocator;
        return this;
    }

    public ExplosionVNT setEntityProcessor(IEntityProcessor entityProcessor) {
        this.entityProcessor = entityProcessor;
        return this;
    }

    public ExplosionVNT setBlockProcessor(IBlockProcessor blockProcessor) {
        this.blockProcessor = blockProcessor;
        return this;
    }

    public ExplosionVNT setPlayerProcessor(IPlayerProcessor playerProcessor) {
        this.playerProcessor = playerProcessor;
        return this;
    }

    public ExplosionVNT setSFX(IExplosionSFX... sfx) {
        this.sfx = sfx;
        return this;
    }

    /** CE's "normal explosion" preset: full block destruction, standard AoE damage, standard SFX. */
    public ExplosionVNT makeStandard() {
        this.setBlockAllocator(new BlockAllocatorStandard());
        this.setBlockProcessor(new BlockProcessorStandard());
        this.setEntityProcessor(new EntityProcessorStandard());
        this.setPlayerProcessor(new PlayerProcessorStandard());
        this.setSFX(new ExplosionEffectStandard());
        return this;
    }

    /** CE's antimatter preset: wider allocation resolution at scale, no drops, doubled damage range plus radiation. */
    public ExplosionVNT makeAmat() {
        this.setBlockAllocator(new BlockAllocatorStandard(this.size < 15 ? 16 : 32));
        this.setBlockProcessor(new BlockProcessorStandard()
                .setNoDrop());
        this.setEntityProcessor(new EntityProcessorStandard()
                .withRangeMod(2F)
                .withDamageMod(new CustomDamageHandlerAmat(50F)));
        this.setPlayerProcessor(new PlayerProcessorStandard());
        this.setSFX(new ExplosionEffectAmat());
        return this;
    }
}
