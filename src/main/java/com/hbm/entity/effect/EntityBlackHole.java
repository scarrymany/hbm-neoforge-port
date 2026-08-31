package com.hbm.entity.effect;

import com.hbm.damage.ModDamageTypes;
import com.hbm.entity.projectile.EntityRubble;
import com.hbm.items.special.ScatteredMilitaryItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Ported from CE's {@code com.hbm.entity.effect.EntityBlackHole} (220 lines, read in full) - the
 * gravity-well family's base class and the only one carrying real per-tick logic; {@link EntityVortex}/
 * {@link EntityRagingVortex}/{@link EntityQuasar} all extend this and add only a shrink/pulse curve on
 * top (see each class's own javadoc). Full behavior per docs/phase4/entities_vortex_gravity_wells.md:
 * <ol>
 *   <li>Bail (and discard) if {@link #isWarDim} is false - CE-faithful stub, see {@link #isWarDim}'s
 *   own javadoc.</li>
 *   <li>If {@link #breaksBlocks} (default {@code true}; {@link #noBreak()} exists but per the report
 *   no real CE call site ever calls it) and server-side: fire {@code size*2} rays from a genuine
 *   uniform-sphere direction sample (spherical coordinates, not a naive per-axis random - preserved
 *   exactly), walk up to {@code ceil(size*15)} blocks per ray, convert the first liquid hit to air and
 *   keep walking, then on the first solid block spawn an {@link EntityRubble} there and stop that ray
 *   (each ray destroys at most one block).</li>
 *   <li>Scan a {@code size*15*2}-wide cube around itself (excluding itself - {@code Level#getEntities
 *   (Entity, AABB)}'s self-excluding overload, the more literal translation of CE's own
 *   {@code getEntitiesWithinAABBExcludingEntity(this, aabb)} than Neo Edition's null-plus-manual-filter
 *   substitute, per the report's Key design decisions): skip creative players outright; convert any
 *   mid-air {@link FallingBlockEntity} older than 1 tick into an {@link EntityRubble} carrying its
 *   block identity and velocity (this conversion runs unconditionally for every entity in the box,
 *   with no early {@code continue} afterward, exactly matching CE's real control flow); for every
 *   entity within the (spherical, not just box) {@code range}, rotate the pull vector 15 degrees around
 *   the yaw axis unless it's an {@link ItemEntity} (vanilla {@link Vec3#yRot(float)}, confirmed real
 *   and mathematically identical to CE's own {@code MutableVec3d.rotateYawSelf}, per the report), then
 *   add the pull to the entity's motion every tick (this pull is applied even to other
 *   {@code EntityBlackHole}s - only the damage/consumption branch below skips them); within the
 *   tighter {@code size*1.5} consumption radius, deal CE's real {@code 1000.0F}
 *   {@link ModDamageTypes#BLACK_HOLE} damage (already tagged {@code BYPASSES_ARMOR} + this port's
 *   {@code ABSOLUTE} tag, matching CE's {@code setDamageIsAbsolute().setDamageBypassesArmor()} -
 *   confirmed against {@code ModDamageTypeTagsProvider}), outright discard anything that isn't a
 *   {@link LivingEntity}, and - only for a consumed {@code pellet_antimatter} item (CE also checks
 *   {@code flame_pony}; that flavor item is not registered anywhere in this port yet, see
 *   {@link #isAntimatterStack}'s own javadoc) - self-destruct into a real
 *   {@code level.explode(null, x, y, z, 5F, Level.ExplosionInteraction.BLOCK)} (CE's real
 *   {@code 5.0F}/{@code true}, <b>not</b> Neo Edition's {@code Float.MAX_VALUE} damage substitution -
 *   that only affects the unrelated {@code 1000.0F} hurt call above, never copied here).</li>
 *   <li>Move by its own (usually-zero) accumulated motion, damped x0.99/tick.</li>
 * </ol>
 * {@link #ignoreExplosion(Explosion)} is placed on this base class (CE's real placement - all 3
 * subclasses inherit it), <b>not</b> only on {@code EntityRagingVortex} the way Neo Edition's parallel
 * port mistakenly does - confirmed a real gap in that reference, not a CE behavior to reproduce.
 */
public class EntityBlackHole extends Entity {

    protected static final EntityDataAccessor<Float> SIZE =
            SynchedEntityData.defineId(EntityBlackHole.class, EntityDataSerializers.FLOAT);

    protected boolean breaksBlocks = true;

    public EntityBlackHole(EntityType<? extends EntityBlackHole> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.noCulling = true;
    }

    public EntityBlackHole(Level level, float size) {
        this(GravityWellEntityTypes.BLACK_HOLE.get(), level);
        this.entityData.set(SIZE, size);
    }

    public EntityBlackHole noBreak() {
        this.breaksBlocks = false;
        return this;
    }

    public float getSize() {
        return this.entityData.get(SIZE);
    }

    /**
     * Package-local stub matching {@code com.hbm.potion.HbmPotionEffects#isWarDim}'s established
     * convention (that method is package-private to {@code com.hbm.potion}, unreachable here) - CE's
     * real default has {@code peaceDimensionsIsWhitelist=true} with an empty {@code peaceDimensions}
     * set, so every dimension is a "war dimension" out of the box; stubbed {@code true}, not
     * {@code false}, per that same reasoning (see {@code CompatibilityConfig}'s own class javadoc for
     * why the dimension-id re-keying itself is deferred to whichever phase owns world-gen).
     */
    static boolean isWarDim(Level level) {
        return true;
    }

    @Override
    public boolean ignoreExplosion(Explosion explosion) {
        return true;
    }

    @Override
    public void tick() {
        super.tick();

        Level level = this.level();
        if (!isWarDim(level)) {
            this.discard();
            return;
        }

        final float size = this.entityData.get(SIZE);

        if (!level.isClientSide() && breaksBlocks) {
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            for (int k = 0; k < size * 2; k++) {
                double phi = this.random.nextDouble() * (Math.PI * 2);
                double costheta = this.random.nextDouble() * 2 - 1;
                double theta = Math.acos(costheta);
                double dx = Math.sin(theta) * Math.cos(phi);
                double dy = Math.sin(theta) * Math.sin(phi);
                double dz = Math.cos(theta);

                int length = (int) Math.ceil(size * 15);

                for (int i = 0; i < length; i++) {
                    int x0 = (int) (this.getX() + (dx * i));
                    int y0 = (int) (this.getY() + (dy * i));
                    int z0 = (int) (this.getZ() + (dz * i));

                    pos.set(x0, y0, z0);
                    BlockState state = level.getBlockState(pos);

                    if (!state.getFluidState().isEmpty()) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                        state = level.getBlockState(pos);
                    }

                    if (!state.is(Blocks.AIR)) {
                        EntityRubble rubble = new EntityRubble(level, x0 + 0.5D, y0, z0 + 0.5D);
                        rubble.setBlockState(state);
                        level.addFreshEntity(rubble);
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                        break;
                    }
                }
            }
        }

        final double range = size * 15;

        AABB box = new AABB(
                this.getX() - range, this.getY() - range, this.getZ() - range,
                this.getX() + range, this.getY() + range, this.getZ() + range
        );
        List<Entity> entities = level.getEntities(this, box);

        for (Entity e : entities) {

            if (e instanceof Player player && player.isCreative()) continue;

            if (e instanceof FallingBlockEntity fbe && !level.isClientSide() && fbe.tickCount > 1) {
                double fx = fbe.getX();
                double fy = fbe.getY();
                double fz = fbe.getZ();
                BlockState fallingState = fbe.getBlockState();
                Vec3 fallingMotion = fbe.getDeltaMovement();

                fbe.discard();

                EntityRubble rubble = new EntityRubble(level, fx, fy, fz);
                rubble.setBlockState(fallingState);
                rubble.setDeltaMovement(fallingMotion);
                level.addFreshEntity(rubble);
            }

            Vec3 vec = new Vec3(this.getX() - e.getX(), this.getY() - e.getY(), this.getZ() - e.getZ());
            double dist = vec.length();
            if (dist > range) continue;

            vec = vec.normalize();
            if (!(e instanceof ItemEntity)) {
                vec = vec.yRot((float) Math.toRadians(15));
            }

            double speed = 0.1D;
            e.setDeltaMovement(e.getDeltaMovement().add(vec.x * speed, vec.y * speed * 2, vec.z * speed));

            if (e instanceof EntityBlackHole) continue;

            if (dist < size * 1.5F) {
                e.hurt(level.damageSources().source(ModDamageTypes.BLACK_HOLE), 1000.0F);

                if (!(e instanceof LivingEntity)) {
                    e.discard();
                }

                if (!level.isClientSide() && e instanceof ItemEntity itemEntity) {
                    ItemStack stack = itemEntity.getItem();
                    if (isAntimatterStack(stack)) {
                        this.discard();
                        level.explode(null, this.getX(), this.getY(), this.getZ(), 5.0F, Level.ExplosionInteraction.BLOCK);
                        return;
                    }
                }
            }
        }

        this.setPos(this.getX() + this.getDeltaMovement().x, this.getY() + this.getDeltaMovement().y, this.getZ() + this.getDeltaMovement().z);
        this.setDeltaMovement(this.getDeltaMovement().scale(0.99D));
    }

    /**
     * CE checks {@code stack.getItem() == ModItems.pellet_antimatter || stack.getItem() ==
     * ModItems.flame_pony}. {@code flame_pony} ({@code ItemCustomLore}, a flavor/joke item unrelated
     * to gravity wells) is not registered anywhere in this port yet (confirmed by repo-wide grep) -
     * out of this package's scope to add. Only the {@code pellet_antimatter} half - the item this
     * family's own tooltip ("Gets rid of black holes") actually documents - is wired here.
     */
    private static boolean isAntimatterStack(ItemStack stack) {
        return stack.is(ScatteredMilitaryItems.PELLET_ANTIMATTER.get());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(SIZE, 0.5F);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.entityData.set(SIZE, tag.getFloat("size"));
        this.breaksBlocks = !tag.contains("breaksBlocks") || tag.getBoolean("breaksBlocks");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("size", this.entityData.get(SIZE));
        tag.putBoolean("breaksBlocks", this.breaksBlocks);
    }
}
