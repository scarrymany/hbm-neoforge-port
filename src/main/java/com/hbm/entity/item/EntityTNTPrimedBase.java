package com.hbm.entity.item;

import com.hbm.api.block.IExploder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * Ported from CE's {@code com.hbm.entity.item.EntityTNTPrimedBase} (165 lines, read in full) - the
 * entity every conventional explosive block (TNT/dynamite/semtex/C4/charges, per
 * {@code docs/phase3/bomb_blocks_and_detonators.md}'s Section A) spawns instead of vanishing
 * silently when caught in another explosion, so it can go on to explode itself a short random
 * "pop fuse" later. This unblocks {@code BlockDetonatable}/{@code BlockTNTBase} and every concrete
 * TNT-family block, and satisfies {@code com.hbm.api.block.IExploder}'s own already-ported
 * {@code explodeEntity(Level, double, double, double, EntityTNTPrimedBase)} overload, which already
 * imported this exact (previously nonexistent) class as a documented forward reference.
 * <p>
 * <b>Block-identity encoding - the report's Open Question, resolved.</b> CE syncs a registry-name
 * {@code String} + a {@code byte} meta via two separate {@code DataParameter}s and resolves them
 * back into a full {@code IBlockState} via {@code getStateFromMeta}. 1.21 {@link BlockState} has no
 * meta ints to round-trip. Reading this class's one real consumer, {@link IExploder#explodeEntity}
 * (confirmed already-ported, this port's own shape) - its signature never takes a {@link BlockState}
 * at all, only a plain {@code Level/x/y/z/entity} tuple; {@link #getBomb()} resolves the
 * {@link IExploder} implementor by casting {@code getBombState().getBlock()}, i.e. the interface is
 * implemented per-{@link Block} (each concrete TNT-family block class implements it directly), never
 * per-{@link BlockState} variant. So only "which {@link Block}" genuinely matters downstream - full
 * {@link BlockState} fidelity is not required. Rather than the report's suggested plain
 * {@code ResourceLocation} string (which would lose any non-default state the caller had in hand for
 * free, e.g. a lit vs. unlit TNT block property, at no extra implementation cost), this stores the
 * caller's exact {@link BlockState} as a single synced {@code int} via {@link Block#getId(BlockState)}/
 * {@link Block#stateById(int)} - the same lightweight int-palette-id round trip Neo Edition's own
 * confirmed-real {@code FallingBlockEntityNT}/{@code TNTPrimedBase} (both cross-checked here for
 * this exact call shape, also reused by this same package's sibling {@link com.hbm.entity.projectile.
 * EntityRubble}) already use for this exact purpose - not the far heavier
 * {@code NbtUtils.writeBlockState}/{@code readBlockState} codec round trip {@code FallingBlockEntityNT}
 * needs for its own full block-restoration duties (this entity never needs to place a block back
 * down, only ask "which Block do I explode as").
 * <p>
 * <b>Faithfully-preserved CE bug</b>: CE's {@code writeEntityToNBT} writes the fuse under the key
 * {@code "fuse"} but {@code readEntityFromNBT} reads it back under {@code "Fuse"} (capital F) - a
 * genuine upstream case-mismatch that always misses on load, silently resetting a reloaded entity's
 * fuse to {@code 0} (instant re-detonation next tick). Preserved exactly below rather than "fixed" -
 * in practice a non-issue, since this entity's ~80-tick (4s) default fuse essentially never survives
 * an actual chunk save/unload cycle.
 * <p>
 * <b>Not ported</b>: CE's {@code getYOffset()}/{@code getEyeHeight()}/{@code canTriggerWalking()}
 * overrides - cosmetic/AI-adjacent 1.12 {@code Entity} hooks with no confirmed-safe 1.21.1 override
 * point found in this port's own codebase or Neo Edition's parallel entities, and no gameplay
 * consequence for a ~4-second-lived thrown-block entity. {@code canBeCollidedWith()} (confirmed real
 * in 1.21.1 by Neo Edition's {@code PlaneBase}) is kept.
 */
public class EntityTNTPrimedBase extends Entity {

    private static final EntityDataAccessor<Integer> FUSE =
            SynchedEntityData.defineId(EntityTNTPrimedBase.class, EntityDataSerializers.INT);
    /** Synced {@link Block#getId(BlockState)} palette id of the priming block's exact state - see class javadoc. */
    private static final EntityDataAccessor<Integer> BLOCK_STATE_ID =
            SynchedEntityData.defineId(EntityTNTPrimedBase.class, EntityDataSerializers.INT);

    public int fuse;
    public boolean detonateOnCollision;

    @Nullable
    private LivingEntity tntPlacedBy;

    public EntityTNTPrimedBase(EntityType<? extends EntityTNTPrimedBase> type, Level level) {
        super(type, level);
        // CE: this.preventEntitySpawning = true - 1.21.1's nearest equivalent spawn-overlap flag.
        this.blocksBuilding = true;
        this.fuse = 80;
        this.detonateOnCollision = false;
    }

    public EntityTNTPrimedBase(Level level, double x, double y, double z, @Nullable LivingEntity placer, BlockState bomb) {
        this(TntPrimedEntityTypes.TNT_PRIMED.get(), level);
        this.setPos(x, y, z);
        this.xo = x;
        this.yo = y;
        this.zo = z;

        double angle = this.random.nextDouble() * Math.PI * 2.0D;
        this.setDeltaMovement(-Math.sin(angle) * 0.02D, 0.2D, -Math.cos(angle) * 0.02D);

        this.tntPlacedBy = placer;
        this.setBombState(bomb);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(FUSE, 80);
        builder.define(BLOCK_STATE_ID, Block.getId(Blocks.TNT.defaultBlockState()));
    }

    @Override
    public boolean canBeCollidedWith() {
        return !this.isRemoved();
    }

    @Override
    public void tick() {
        this.xo = this.getX();
        this.yo = this.getY();
        this.zo = this.getZ();

        this.setDeltaMovement(this.getDeltaMovement().add(0.0D, -0.04D, 0.0D));
        this.move(MoverType.SELF, this.getDeltaMovement());
        this.setDeltaMovement(this.getDeltaMovement().scale(0.98D));

        if (this.onGround()) {
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.7D, -0.5D, 0.7D));
        }

        if (this.fuse-- <= 0 || (this.detonateOnCollision && (this.horizontalCollision || this.verticalCollision))) {
            this.discard();

            if (!this.level().isClientSide()) {
                this.explode();
            }
        } else {
            this.level().addParticle(ParticleTypes.SMOKE,
                    this.getX(), this.getY() + 0.5D, this.getZ(), 0.0D, 0.0D, 0.0D);
        }
    }

    private void explode() {
        this.getBomb().explodeEntity(this.level(), this.getX(), this.getY(), this.getZ(), this);
    }

    public IExploder getBomb() {
        return (IExploder) getBombState().getBlock();
    }

    public BlockState getBombState() {
        return Block.stateById(this.entityData.get(BLOCK_STATE_ID));
    }

    public void setBombState(BlockState state) {
        this.entityData.set(BLOCK_STATE_ID, Block.getId(state));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putShort("fuse", (short) this.fuse);
        tag.putInt("BlockStateId", this.entityData.get(BLOCK_STATE_ID));
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        // See class javadoc "Faithfully-preserved CE bug" - CE itself reads "Fuse" (capital F)
        // against its own "fuse" write key above, so this always misses and resets fuse to 0.
        this.setFuse(tag.getShort("Fuse"));
        this.entityData.set(BLOCK_STATE_ID, tag.getInt("BlockStateId"));
    }

    @Nullable
    public LivingEntity getTntPlacedBy() {
        return this.tntPlacedBy;
    }

    public int getFuse() {
        return this.fuse;
    }

    public void setFuse(int fuseIn) {
        this.fuse = fuseIn;
        this.entityData.set(FUSE, fuseIn);
    }
}
