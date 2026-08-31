package com.hbm.entity.item;

import com.hbm.entity.logic.IChunkLoader;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.tool.CouplingToolItems;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Ported from CE's {@code com.hbm.entity.item.EntityDeliveryDrone} (266 lines, read in full) - the
 * "patrol"/logistics-hauler variant of {@link EntityDroneBase}, per
 * {@code docs/phase4/entities_vehicles_aircraft.md}'s "Logistics-drone entity family" section.
 * <p>
 * <b>{@code Container} instead of an {@code ItemStackHandler}</b>: CE backs its 18-slot inventory with
 * a Forge {@code ItemStackHandler}; this port instead implements vanilla's own {@link Container}
 * directly over a plain {@code NonNullList<ItemStack>} - the exact same substitution this port's own
 * sibling packages already made for the identical CE "{@code IInventory} on an entity" shape
 * ({@code com.hbm.entity.cart.EntityMinecartContainerBase}, {@code com.hbm.entity.train.
 * EntityRailCarCargo} - both landed this same wave), rather than introducing a third, redundant
 * pattern for what the Phase 4 report itself flags as boilerplate worth collapsing, not repeating.
 * <p>
 * <b>Dropped-item encoding - "Data Components" superseded by this port's own real, already-committed
 * convention.</b> CE encodes the express/chunk-loading state of the dropped {@code drone} item into
 * two metadata bits (0-3, {@code ItemDrone.EnumDroneType}). This port's own already-committed
 * {@code com.hbm.items.tool.ItemDrone}/{@code CouplingToolItems} (Phase 2) already flattened CE's
 * 5-variant metadata item into 5 separate registered items ({@code DRONE_PATROL},
 * {@code DRONE_PATROL_CHUNKLOADING}, {@code DRONE_PATROL_EXPRESS},
 * {@code DRONE_PATROL_EXPRESS_CHUNKLOADING}, {@code DRONE_REQUEST}) - which is itself already the
 * "metadata-equivalent" encoding this class needs, in the exact convention this port's post-
 * flattening item registrations already use everywhere else. Picking the matching one of those 4
 * {@code DRONE_PATROL*} items on death (see {@link #createDroppedDroneItem()}) is therefore more
 * faithful to this port's own established conventions than introducing a bespoke
 * {@code DataComponentType} for a single item family that already has a real, compiling, non-
 * component encoding.
 * <p>
 * <b>Chunk-loading footprint - CE's own more-aggressive-than-usual shape, preserved exactly.</b> CE's
 * {@code loadNeighboringChunks} force-loads a full 3x3 chunk neighborhood around the drone's *current*
 * chunk, plus (only while moving with meaningful horizontal speed) a second 3x3 neighborhood around a
 * heading-projected lookahead chunk - more aggressive than every other chunk-loading entity in this
 * survey (which force-load only their own current chunk), per the Phase 4 report's own Open questions
 * section, which explicitly asks for a deliberate preserve-vs-simplify call rather than a silent
 * default. This class preserves CE's exact footprint (both 3x3 neighborhoods, same lookahead formula:
 * {@code pos + motion * 16} blocks, floor-divided into a chunk coordinate). The only change from CE's
 * own literal implementation is *how* the footprint is applied: rather than CE's own "unforce every
 * currently-held chunk, then reforce the whole new footprint" every single tick (an already-flagged
 * CE inefficiency, not part of the footprint shape itself), this diffs the new footprint against the
 * previous tick's {@link #forcedChunks} set and only calls {@link ServerLevel#setChunkForced} for
 * chunks that actually entered or left the footprint - same steady-state chunks forced, fewer redundant
 * calls, and (per this port's own {@link IChunkLoader} javadoc) chunks are force-added before any old
 * chunk is force-removed, so there is never a tick where a chunk that should stay loaded is briefly
 * unforced.
 * <p>
 * <b>{@link IChunkLoader} implemented, but not via its single-chunk default methods.</b> This port's
 * real {@link IChunkLoader} interface's {@code onAddedToLevel}/{@code onRemovedFromLevel}/
 * {@code updateChunkTicket} default methods all operate on exactly one {@link ChunkPos} - insufficient
 * for this class's real multi-chunk footprint above. {@link #setLoadedChunkPos}/{@link
 * #getLoadedChunkPos} are still implemented (tracking just the drone's own current-position chunk) so
 * the interface contract is satisfied and any future generic {@code IChunkLoader}-typed caller still
 * gets a sensible single-chunk answer; the real multi-chunk force-load bookkeeping lives entirely in
 * {@link #forcedChunks}, managed directly by this class's own {@link #loadNeighboringChunks()} override
 * and released via {@link #clearChunkLoader()}.
 */
public class EntityDeliveryDrone extends EntityDroneBase implements Container, IChunkLoader {

    private static final EntityDataAccessor<Boolean> IS_EXPRESS =
            SynchedEntityData.defineId(EntityDeliveryDrone.class, EntityDataSerializers.BOOLEAN);

    private NonNullList<ItemStack> items = NonNullList.withSize(18, ItemStack.EMPTY);
    @Nullable
    public FluidStack fluid;

    private boolean chunkLoading = false;
    private final Set<ChunkPos> forcedChunks = new LinkedHashSet<>();
    @Nullable
    private ChunkPos loadedChunkPos;

    public EntityDeliveryDrone(EntityType<? extends EntityDeliveryDrone> type, Level level) {
        super(type, level);
    }

    /**
     * CE: {@code hitByEntity(Entity)} always {@code setDead()}s on any player hit, dropping every
     * non-empty cargo slot plus a {@code drone} item encoding express/chunk-loading state (see class
     * javadoc). No 1.21.1 equivalent of the old pre-damage veto hook exists, so this is re-expressed
     * as a {@link #hurt} override - same substitution this package's own {@code EntityMovingItem}
     * already established for an equivalent CE shape. CE's carried {@link #fluid} is *not* dropped
     * (CE's own {@code hitByEntity} never touches it either - faithfully preserved, not an oversight
     * fixed here).
     * <p>
     * Checks {@link DamageSource#getDirectEntity()}, not {@link DamageSource#getEntity()} - CE's own
     * 1.12 {@code hitByEntity(Entity)} was only ever invoked for a player's direct melee punch, not
     * for e.g. an arrow the player fired (a narrower trigger than "any damage a player is ultimately
     * responsible for"). {@code getDirectEntity()} (the immediate physical cause) preserves that same
     * melee-only scope; {@code getEntity()} (the "true" attacker) would incorrectly also destroy the
     * drone from a player-shot arrow, which real CE does not.
     */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.isRemoved()) return false;

        if (!this.level().isClientSide() && source.getDirectEntity() instanceof Player) {
            for (int i = 0; i < this.items.size(); i++) {
                ItemStack stack = this.items.get(i);
                if (!stack.isEmpty()) {
                    this.spawnAtLocation(stack, 1.0F);
                }
            }
            this.spawnAtLocation(this.createDroppedDroneItem(), 1.0F);
            this.discard();
        }

        return true;
    }

    private ItemStack createDroppedDroneItem() {
        boolean express = this.entityData.get(IS_EXPRESS);

        if (express && this.chunkLoading) return new ItemStack(CouplingToolItems.DRONE_PATROL_EXPRESS_CHUNKLOADING.get());
        if (express) return new ItemStack(CouplingToolItems.DRONE_PATROL_EXPRESS.get());
        if (this.chunkLoading) return new ItemStack(CouplingToolItems.DRONE_PATROL_CHUNKLOADING.get());
        return new ItemStack(CouplingToolItems.DRONE_PATROL.get());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(IS_EXPRESS, false);
    }

    public boolean isExpress() {
        return this.entityData.get(IS_EXPRESS);
    }

    public void setExpress(boolean express) {
        this.entityData.set(IS_EXPRESS, express);
    }

    /**
     * CE: lazily requests its chunk-loading ticket the first time this is called, guarded by
     * {@code loaderTicket == null}. 1.21.1's {@code setChunkForced} needs no persistent ticket object
     * (see {@link IChunkLoader}'s own javadoc), so flipping this flag is sufficient - the next tick's
     * {@link #loadNeighboringChunks()} call starts (idempotently) force-loading the footprint.
     */
    public void setChunkLoading() {
        this.chunkLoading = true;
    }

    @Override
    public double getSpeed() {
        return this.entityData.get(IS_EXPRESS) ? 0.375D * 3 : 0.375D;
    }

    // --- Container (CE's IInventory) --------------------------------------------------------------

    @Override
    public int getContainerSize() {
        return 18;
    }

    /** CE: {@code isEmpty() { return false; }} - a literal CE quirk, preserved rather than "fixed"; see class javadoc. */
    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return ContainerHelper.removeItem(this.items, slot, amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(this.items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (!stack.isEmpty() && stack.getCount() > this.getMaxStackSize()) {
            stack.setCount(this.getMaxStackSize());
        }
        this.items.set(slot, stack);
    }

    /** CE: {@code markDirty() { }} - a no-op stub in CE too. */
    @Override
    public void setChanged() {
    }

    /** CE: {@code isUsableByPlayer(EntityPlayer) { return false; }} - no GUI at this entity layer. */
    @Override
    public boolean stillValid(Player player) {
        return false;
    }

    /** CE: {@code isItemValidForSlot(int, ItemStack) { return false; }} - internal logistics only, not player-insertable. */
    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return false;
    }

    @Override
    public void clearContent() {
        this.items.clear();
    }

    // --- Chunk loading -----------------------------------------------------------------------------

    @Override
    protected void loadNeighboringChunks() {
        if (!this.chunkLoading) return;
        if (!(this.level() instanceof ServerLevel serverLevel)) return;

        Set<ChunkPos> newFootprint = new LinkedHashSet<>();
        int chunkX = Mth.floor(this.getX() / 16.0D);
        int chunkZ = Mth.floor(this.getZ() / 16.0D);
        addChunkArea(newFootprint, chunkX, chunkZ);
        this.setLoadedChunkPos(new ChunkPos(chunkX, chunkZ));

        Vec3 motion = this.getDeltaMovement();
        if (motion.x * motion.x + motion.z * motion.z > 0.0001D) {
            int lookAheadChunkX = Mth.floor((this.getX() + motion.x * 16.0D) / 16.0D);
            int lookAheadChunkZ = Mth.floor((this.getZ() + motion.z * 16.0D) / 16.0D);
            addChunkArea(newFootprint, lookAheadChunkX, lookAheadChunkZ);
        }

        // Force new chunks first, then release stale ones - never a tick with neither forced.
        for (ChunkPos pos : newFootprint) {
            if (this.forcedChunks.add(pos)) {
                serverLevel.setChunkForced(pos.x, pos.z, true);
            }
        }

        Iterator<ChunkPos> it = this.forcedChunks.iterator();
        while (it.hasNext()) {
            ChunkPos pos = it.next();
            if (!newFootprint.contains(pos)) {
                serverLevel.setChunkForced(pos.x, pos.z, false);
                it.remove();
            }
        }
    }

    private static void addChunkArea(Set<ChunkPos> chunks, int centerChunkX, int centerChunkZ) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                chunks.add(new ChunkPos(centerChunkX + dx, centerChunkZ + dz));
            }
        }
    }

    public void clearChunkLoader() {
        if (this.level() instanceof ServerLevel serverLevel) {
            for (ChunkPos pos : this.forcedChunks) {
                serverLevel.setChunkForced(pos.x, pos.z, false);
            }
        }
        this.forcedChunks.clear();
    }

    @Override
    public void onRemovedFromLevel() {
        super.onRemovedFromLevel();
        this.clearChunkLoader();
    }

    // --- IChunkLoader (single-chunk bookkeeping only - see class javadoc) -------------------------

    @Override
    public void setLoadedChunkPos(ChunkPos pos) {
        this.loadedChunkPos = pos;
    }

    @Override
    @Nullable
    public ChunkPos getLoadedChunkPos() {
        return this.loadedChunkPos;
    }

    // --- NBT -----------------------------------------------------------------------------------

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);

        ContainerHelper.saveAllItems(tag, this.items, this.registryAccess());

        if (this.fluid != null) {
            Fluids.writeType(tag, "fluidType", this.fluid.type);
            tag.putInt("fluidAmount", this.fluid.fill);
        }

        tag.putBoolean("load", this.entityData.get(IS_EXPRESS));
        tag.putBoolean("chunkLoading", this.chunkLoading);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);

        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, this.items, this.registryAccess());

        if (tag.contains("fluidType")) {
            FluidType type = Fluids.readType(tag, "fluidType");
            this.fluid = new FluidStack(type, tag.getInt("fluidAmount"));
        }

        this.entityData.set(IS_EXPRESS, tag.getBoolean("load"));
        this.chunkLoading = tag.getBoolean("chunkLoading");
    }
}
