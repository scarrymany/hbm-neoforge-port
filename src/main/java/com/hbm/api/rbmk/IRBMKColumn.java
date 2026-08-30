package com.hbm.api.rbmk;

import com.hbm.handler.neutron.RBMKNeutronHandler.RBMKType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * Generalized contract for one RBMK reactor column (fuel rod, moderator, absorber, reflector,
 * control rod, outgasser, or any inert filler/peripheral column) that this package's pure
 * neutron-flux/heat/meltdown-trigger simulation needs to interact with.
 * <p>
 * CE's real column tile entity is {@code com.hbm.tileentity.machine.rbmk.TileEntityRBMKBase}
 * (710 lines, read in full - see {@code docs/phase2/rbmk_reactor.md}). This interface is this
 * port's own addition (no direct CE equivalent - CE's neutron engine references the concrete tile
 * entity class directly). It exists purely so this package's flux/heat math and neutron engine
 * (see {@code com.hbm.handler.neutron}) never reference a concrete {@code BlockEntity} subclass:
 * those belong to the parallel {@code com.hbm.blockentity.machine.rbmk} column-blocks package
 * (this wave's sibling work package), which implements this interface on its column block
 * entities. A {@code BlockEntity} is looked up generically via
 * {@code ServerLevel#getBlockEntity(BlockPos)} and checked with {@code instanceof IRBMKColumn} -
 * see {@code RBMKNeutronHandler}'s private {@code columnAt} helper - so no compile-time dependency
 * on the sibling package's classes exists in either direction.
 */
public interface IRBMKColumn {

    /** The column's world. CE: {@code TileEntity#getWorld()}. RBMK simulation is server-only in CE (every call site is guarded by {@code !world.isRemote}), so this port types it as {@link ServerLevel} directly rather than the more permissive {@code Level}. */
    ServerLevel getRbmkLevel();

    /** The column's position. CE: {@code TileEntity#getPos()}. */
    BlockPos getRbmkPos();

    /** CE: {@code TileEntityRBMKBase#getRBMKType()}, default {@link RBMKType#OTHER}. */
    default RBMKType getRBMKType() {
        return RBMKType.OTHER;
    }

    /**
     * Whether this column currently has a lid installed. An un-lidded column irradiates the open
     * chunk above it every time a flux stream passes through or terminates on it (see
     * {@code RBMKNeutronHandler}). CE: {@code TileEntityRBMKBase#hasLid()} - always {@code true}
     * for column types that can't remove their lid at all (e.g. control rods,
     * {@code isLidRemovable() == false}), otherwise driven by the block's {@code DIR_NO_LID}
     * facing state.
     */
    boolean hasLid();

    /**
     * Whether this column moderates (fast-to-slow) any flux stream passing through it in addition
     * to whatever its {@link #getRBMKType()} already implies - lets a fuel/control column be built
     * with a "moderated" block variant on top of its normal role. CE: {@code TileEntityRBMKBase#isModerated()}, default {@code false}.
     */
    default boolean isModerated() {
        return false;
    }

    /** Current column heat in degrees Celsius. CE: {@code TileEntityRBMKBase#heat} (a public field there; exposed as an accessor pair here). */
    double getHeat();

    /** CE: direct write to {@code TileEntityRBMKBase#heat}. */
    void setHeat(double heat);

    /** Convenience for the common "add heat to this column" pattern (CE: e.g. {@code nodeTE.heat += x} in the absorber flux-interaction branch). */
    default void addHeat(double amount) {
        setHeat(getHeat() + amount);
    }

    /**
     * The heat threshold that triggers a meltdown - see {@link RBMKMeltdownTrigger}. CE:
     * {@code TileEntityRBMKBase#maxHeat()}, hardcoded {@code 1500D} for every column type (CE's own
     * javadoc: "approx melting point of steel... won't be used because fuel tends to melt much
     * earlier than that" - see {@link RBMKMeltdownTrigger}'s javadoc for the other, independent,
     * lower fuel-item threshold).
     */
    default double maxHeat() {
        return 1500D;
    }

    /**
     * Whether this column has been removed from the world (block broken, chunk unloaded, etc).
     * CE: {@code TileEntity#isInvalid()} (1.12's name for what 1.21's {@code BlockEntity} calls
     * {@code isRemoved()}). Used by the neutron node cache to evict entries for columns that no
     * longer exist.
     */
    boolean isRemoved();
}
