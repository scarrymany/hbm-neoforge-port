package com.hbm.entity.item;

import com.hbm.blocks.generic.BlockSupplyCrate;
import com.hbm.blocks.generic.GenericCrateBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Port of CE's {@code com.hbm.entity.item.EntityParachuteCrate} (76 lines, read in full) -
 * {@link com.hbm.entity.logic.EntityC130}'s payload-drop entity, per both
 * {@code docs/phase4/entities_vehicles_aircraft.md} and
 * {@code docs/phase4/entities_orbital_and_beam_payloads.md} (independently converging on this class,
 * no discrepancy per the second report's own check).
 * <p>
 * <b>Terminal-velocity fall, not free-fall</b> - CE's own {@code onUpdate}: {@code motionY} decays
 * toward a {@code -0.2} blocks/tick cap ({@code if (motionY > -0.2) motionY -= 0.02}) rather than
 * accelerating unboundedly, matching a parachute-drag model. Clamped to never fall below world
 * Y=600 on spawn (CE's own safety clamp for very-high-altitude C130 drops - kept verbatim even though
 * it rarely matters at normal play heights).
 * <p>
 * On touching any non-air block, replaces itself with {@link GenericCrateBlocks#crateSupply()}
 * ({@link BlockSupplyCrate}, already real in this port - confirmed zero missing dependency by direct
 * read of {@link BlockSupplyCrate.SupplyCrateBlockEntity}) one block above the impact point, handing
 * off its carried {@link #items} list to that block entity's own {@code items} field.
 * <p>
 * Spawned via plain {@code level.addFreshEntity(...)} (this port's own established substitute for
 * CE's {@code WorldUtil.loadAndSpawnEntityInWorld} chunk-preload loop - the crate itself is short-lived
 * and falls only a short vertical distance from a plane that is already {@link
 * com.hbm.entity.logic.IChunkLoader}-chunk-loaded, so it needs no chunk-loading of its own, matching
 * both reports' confirmed finding). CE's {@code TrackerUtil.setTrackingRange} call is dropped entirely
 * per that same established precedent (a supersede, not a regression - see {@code EntityC130}'s
 * javadoc for the identical reasoning applied to the plane itself).
 */
public class EntityParachuteCrate extends Entity {

    public final List<ItemStack> items = new ArrayList<>();

    public EntityParachuteCrate(EntityType<? extends EntityParachuteCrate> type, Level level) {
        super(type, level);
        this.noCulling = true;
    }

    public EntityParachuteCrate(Level level, double x, double y, double z) {
        this(ParachuteCrateEntityTypes.PARACHUTE_CRATE.get(), level);
        this.setPos(x, Math.min(y, 600), z);
    }

    @Override
    public void tick() {
        Level level = this.level();

        this.tickCount++;

        Vec3 motion = this.getDeltaMovement();
        this.setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);

        if (motion.y > -0.2D) {
            this.setDeltaMovement(motion.x, motion.y - 0.02D, motion.z);
        }
        if (getY() > 600) this.setPos(getX(), 600, getZ());

        if (level.isClientSide()) return;

        BlockPos pos = BlockPos.containing(Math.floor(getX()), Math.floor(getY()), Math.floor(getZ()));
        BlockState state = level.getBlockState(pos);

        if (!state.isAir()) {
            this.discard();

            BlockPos cratePos = pos.above();
            level.setBlockAndUpdate(cratePos, GenericCrateBlocks.crateSupply().get().defaultBlockState());
            if (level.getBlockEntity(cratePos) instanceof BlockSupplyCrate.SupplyCrateBlockEntity crate) {
                crate.items.addAll(this.items);
            }
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        items.clear();
        ListTag list = tag.getList("items", 10);
        for (int i = 0; i < list.size(); i++) {
            items.add(ItemStack.parseOptional(this.registryAccess(), list.getCompound(i)));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        ListTag list = new ListTag();
        for (ItemStack stack : items) {
            list.add(stack.save(this.registryAccess(), new CompoundTag()));
        }
        tag.put("items", list);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true;
    }
}
