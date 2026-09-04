package com.hbm.blockentity.machine.rbmk;

import com.hbm.api.rbmk.IRBMKColumn;
import com.hbm.api.rbmk.IRBMKMeltdownHandler;
import com.hbm.api.rbmk.RBMKColumnHeatMath;
import com.hbm.api.rbmk.RBMKDials;
import com.hbm.api.rbmk.RBMKMeltdownTrigger;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.LoadedBaseBlockEntity;
import com.hbm.blocks.machine.rbmk.RBMKBaseBlock;
import com.hbm.handler.neutron.NeutronNodeWorld;
import com.hbm.handler.neutron.RBMKNeutronHandler;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.main.AdvancementManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Shared base for every RBMK reactor column block entity - this work package's own implementation of
 * {@link IRBMKColumn} (the interface the sibling {@code rbmk_core_logic} package's pure flux/heat/
 * meltdown-trigger math operates against generically, per that interface's own javadoc: "the parallel
 * com.hbm.blockentity.machine.rbmk column-blocks package... implements this interface on its column
 * block entities"). Ported from CE's {@code TileEntityRBMKBase} (710 lines) - heat diffusion and the
 * meltdown BFS/debris-conversion state machine are this package's own responsibility per
 * {@link IRBMKMeltdownHandler}'s own javadoc ("the column-blocks package for the immediate
 * block-conversion piece... should supply a real implementation of this interface"), while the pure
 * arithmetic those two lean on ({@link RBMKColumnHeatMath}, {@link RBMKMeltdownTrigger}) comes from
 * the sibling package.
 * <p>
 * Melt Exact CE {@code TileEntityRBMKBase.java:463-597} playable: {@code dropLids} gate,
 * {@code rbmk_explosion} vol 50, {@code achRBMKBoom} ±50 AABB. pribris/EntityRBMKDebris /
 * overpressure pipes / RBMKMush / EntitySpear stay skipped.
 */
public abstract class RBMKBaseBlockEntity extends LoadedBaseBlockEntity implements IRBMKColumn, ITickableBE {

    private static final Direction[] HEAT_DIRS = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

    /** Shared meltdown handler every column dispatches through - see this class's own javadoc. */
    private static final IRBMKMeltdownHandler MELTDOWN_HANDLER = RBMKBaseBlockEntity::runMeltdown;

    public double heat = 20.0D;
    /** CE {@code TileEntityRBMKBase.diag} — skip super NBT when flushing DODD. */
    public boolean diag;

    protected RBMKBaseBlockEntity[] neighborCache = new RBMKBaseBlockEntity[4];

    protected RBMKBaseBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // ==================== IRBMKColumn ====================

    @Override
    public ServerLevel getRbmkLevel() {
        return level instanceof ServerLevel serverLevel ? serverLevel : null;
    }

    @Override
    public BlockPos getRbmkPos() {
        return worldPosition;
    }

    @Override
    public double getHeat() {
        return heat;
    }

    @Override
    public void setHeat(double heat) {
        this.heat = heat;
    }

    /** CE: {@code TileEntityRBMKBase.hasLid()} - overridden by rod/outgasser columns whose block tracks a removable lid state. */
    public boolean hasLid() {
        return true;
    }

    public int trackingRange() {
        return 15;
    }

    /**
     * CE: only {@code TileEntityRBMKRod.update()} ever checks {@code heat > maxHeat()} and fires a
     * meltdown (see {@link RBMKMeltdownTrigger}'s own javadoc) - every other column type (moderator,
     * reflector, absorber, blank, control, boiler, outgasser, heater) inherits/calls this base update
     * for heat diffusion only and never triggers a meltdown from its own accumulated heat in CE.
     * {@link #checkMeltdown} is therefore deliberately NOT called here; it stays a {@code protected}
     * method that only {@link RBMKRodBlockEntity#updateEntity()} calls, matching CE exactly.
     */
    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide || !(level instanceof ServerLevel serverLevel)) return;

        moveHeat();
        networkPackNT(trackingRange());
    }

    /**
     * CE: {@code TileEntityRBMKBase.moveHeat()}, arithmetic delegated to {@link RBMKColumnHeatMath}.
     * Protected (not called from within {@link #updateEntity()} alone) so a column that needs a
     * different per-tick sequence (e.g. {@link RBMKRodBlockEntity}, which must burn fuel and
     * reduce/reset its flux fields around this call rather than purely before/after it) can call it
     * directly instead of overriding {@link #updateEntity()} and duplicating this logic.
     */
    protected void moveHeat() {
        int index = 0;
        double[] heats = new double[5];
        int members = 1;
        heats[0] = this.heat;

        for (Direction dir : HEAT_DIRS) {
            if (neighborCache[index] != null && neighborCache[index].isRemoved()) neighborCache[index] = null;

            if (neighborCache[index] == null) {
                BlockEntity te = level.getBlockEntity(worldPosition.relative(dir));
                if (te instanceof RBMKBaseBlockEntity base) neighborCache[index] = base;
            }

            RBMKBaseBlockEntity neighbor = neighborCache[index];
            if (neighbor != null) {
                heats[members] = neighbor.heat;
                members++;
            }
            index++;
        }

        if (members > 1) {
            double[] group = new double[members];
            System.arraycopy(heats, 0, group, 0, members);
            double stepSize = RBMKDials.getColumnHeatFlow((ServerLevel) level);
            double[] equalized = RBMKColumnHeatMath.equalizeHeat(group, stepSize);

            this.heat = equalized[0];
            int gi = 1;
            for (RBMKBaseBlockEntity neighbor : neighborCache) {
                if (neighbor != null) {
                    neighbor.heat = equalized[gi++];
                    neighbor.setChanged();
                }
            }
        }

        coolPassively(members - 1);
    }

    protected void coolPassively(int neighbors) {
        double inner = RBMKDials.getPassiveCoolingInner((ServerLevel) level);
        double outer = RBMKDials.getPassiveCooling((ServerLevel) level);
        double amount = RBMKColumnHeatMath.passiveCooling(neighbors, inner, outer);
        this.heat = RBMKColumnHeatMath.applyPassiveCooling(this.heat, amount);
    }

    // ==================== Meltdown trigger dispatch (CE: TileEntityRBMKRod.update()'s heat > maxHeat() check) ====================

    protected void checkMeltdown(ServerLevel serverLevel) {
        RBMKMeltdownTrigger.checkAndFire(serverLevel, this, MELTDOWN_HANDLER);
    }

    /**
     * CE: {@code TileEntityRBMKBase.meltdown()} - BFS flood-fill + per-column {@code onMelt} dispatch,
     * this package's own responsibility (see class javadoc). Byproduct blocks/entities are Phase 3
     * forward references (not created this wave) - only the decision logic (which columns get how
     * much reduction) runs; {@link #onMelt} on a column with no real byproduct blocks available is a
     * documented no-op fallback (see {@link #standardMelt}/{@link #spawnDebris}).
     */
    private static void runMeltdown(ServerLevel level, BlockPos originPos, IRBMKColumn origin) {
        RBMKBaseBlock.dropLids = false;
        java.util.Set<RBMKBaseBlockEntity> columns = new java.util.HashSet<>();
        java.util.Deque<BlockPos> queue = new java.util.ArrayDeque<>();
        queue.add(originPos);
        int safety = 50_000;

        while (!queue.isEmpty() && safety-- > 0) {
            BlockPos current = queue.poll();
            if (!level.isLoaded(current)) continue;

            if (level.getBlockEntity(current) instanceof RBMKBaseBlockEntity rbmk && columns.add(rbmk)) {
                queue.add(current.offset(1, 0, 0));
                queue.add(current.offset(-1, 0, 0));
                queue.add(current.offset(0, 0, 1));
                queue.add(current.offset(0, 0, -1));
            }
        }

        if (columns.isEmpty()) {
            RBMKBaseBlock.dropLids = true;
            return;
        }

        int minX = originPos.getX(), maxX = originPos.getX(), minZ = originPos.getZ(), maxZ = originPos.getZ();
        for (RBMKBaseBlockEntity rbmk : columns) {
            minX = Math.min(minX, rbmk.worldPosition.getX());
            maxX = Math.max(maxX, rbmk.worldPosition.getX());
            minZ = Math.min(minZ, rbmk.worldPosition.getZ());
            maxZ = Math.max(maxZ, rbmk.worldPosition.getZ());
        }

        for (RBMKBaseBlockEntity rbmk : columns) {
            int distFromMinX = rbmk.worldPosition.getX() - minX;
            int distFromMaxX = maxX - rbmk.worldPosition.getX();
            int distFromMinZ = rbmk.worldPosition.getZ() - minZ;
            int distFromMaxZ = maxZ - rbmk.worldPosition.getZ();
            int minDist = Math.min(Math.min(distFromMinX, distFromMaxX), Math.min(distFromMinZ, distFromMaxZ));
            rbmk.onMelt(minDist + 1);
        }

        // CE :501-564 pribris/overpressure — pribris + IOverpressurable unregistered, skip invent.
        // CE :570-573 RBMKMush — VFX skip.
        int avgX = minX + (maxX - minX) / 2;
        int avgZ = minZ + (maxZ - minZ) / 2;
        level.playSound(null, avgX + 0.5, originPos.getY() + 1, avgZ + 0.5,
                HBMSoundHandler.rbmk_explosion.get(), SoundSource.BLOCKS, 50.0F, 1.0F);

        AABB box = new AABB(
                originPos.getX() - 50 + 0.5, originPos.getY() - 50 + 0.5, originPos.getZ() - 50 + 0.5,
                originPos.getX() + 50 + 0.5, originPos.getY() + 50 + 0.5, originPos.getZ() + 50 + 0.5);
        for (Player player : level.getEntitiesOfClass(Player.class, box)) {
            if (player instanceof ServerPlayer serverPlayer) {
                AdvancementManager.grantAchievement(serverPlayer, AdvancementManager.achRBMKBoom);
            }
        }
        // CE :583-589 EntitySpear on digamma — EntityLogicTail stub, skip invent.
        RBMKBaseBlock.dropLids = true;
        RBMKBaseBlock.digamma = false;
    }

    /**
     * Convenience for a column TE that wants to trigger a meltdown directly (matching CE's
     * {@code TileEntityRBMKBase.meltdown()} being callable from e.g. a debug tool) - not otherwise
     * called by this class itself, {@link #checkMeltdown} is the normal per-tick path.
     */
    public void meltdown() {
        if (level instanceof ServerLevel serverLevel) {
            runMeltdown(serverLevel, worldPosition, this);
        }
    }

    /**
     * Per-column melt behavior - overridden per column type for debris-type selection (see each
     * concrete class). CE: {@code TileEntityRBMKBase.onMelt(int)}/subclass overrides.
     */
    public void onMelt(int reduce) {
        standardMelt(reduce);
    }

    /**
     * CE: {@code TileEntityRBMKBase.standardMelt(int)} - converts the column to rubble/air based on
     * {@code reduce}. Real byproduct blocks ({@code pribris}/{@code pribris_burning}) don't exist in
     * this port (Phase 3 scope) - this documents the decision (which blocks would convert) without
     * performing it, so the meltdown BFS/reduce-factor math above is still fully exercised and
     * testable even though nothing visibly changes in the world yet.
     */
    protected void standardMelt(int reduce) {
        // forward reference: com.hbm.blocks.ModBlocks.pribris / pribris_burning - Phase 3, not created this wave.
        // CE: for i in [0, columnHeight], blocks below (columnHeight + 1 - reduce) become rubble/burning
        // rubble, blocks above become air. Left as a no-op world mutation until those blocks exist.
    }

    /**
     * CE: {@code TileEntityRBMKBase.spawnDebris(DebrisType)} - {@code EntityRBMKDebris} does not
     * exist in this port (Phase 3, {@code com.hbm.entity} package entirely absent). No-op forward
     * reference, called from every column's {@code onMelt} override exactly where CE calls it, so a
     * future Phase 3 pass only needs to fill this one method in.
     */
    protected void spawnDebris(Object debrisType) {
        // forward reference: com.hbm.entity.projectile.EntityRBMKDebris(.DebrisType) - Phase 3, not created this wave.
    }

    // ==================== Console data (this package's own scope) ====================

    public abstract RBMKColumn.ColumnType getConsoleType();

    public RBMKColumn getConsoleData() {
        RBMKColumn col = RBMKColumn.createForType(getConsoleType());
        col.heat = this.heat;
        col.maxHeat = this.maxHeat();
        col.moderated = this.isModerated();
        return col;
    }

    // ==================== Networking / persistence ====================

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.heat = tag.getDouble("heat");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        if (!diag) {
            super.saveAdditional(tag, registries);
        }
        tag.putDouble("heat", this.heat);
    }

    /** CE {@code TileEntityRBMKBase.getDiagData}. */
    public void getDiagData(CompoundTag nbt, HolderLookup.Provider registries) {
        diag = true;
        saveAdditional(nbt, registries);
        diag = false;
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeDouble(this.heat);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        this.heat = buf.readDouble();
    }

    /** CE: {@code TileEntityRBMKBase.getRBMKType()}, default {@code OTHER} - see {@link IRBMKColumn#getRBMKType()}. */
    public RBMKNeutronHandler.RBMKType getRBMKType() {
        return RBMKNeutronHandler.RBMKType.OTHER;
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level instanceof ServerLevel serverLevel) NeutronNodeWorld.removeNode(serverLevel, worldPosition);
    }
}
