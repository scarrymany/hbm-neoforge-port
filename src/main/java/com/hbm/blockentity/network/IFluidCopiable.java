package com.hbm.blockentity.network;

import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.api.fluidmk2.IFluidUserMK2;
import com.hbm.interfaces.ICopiable;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrench copy/paste-settings contract for fluid-carrying block entities, ported from CE's
 * {@code com.hbm.tileentity.IFluidCopiable}. Retargeted onto this port's own already-committed
 * {@link ICopiable} contract ({@code getSettings(Level, BlockPos)}/
 * {@code pasteSettings(CompoundTag, int, Level, Player, BlockPos)}, collapsing CE's
 * {@code World, int x, int y, int z} into {@code Level, BlockPos}) rather than CE's own signature, and
 * {@link IFluidUserMK2#getAllTanks()}'s {@link List} shape rather than CE's {@code FluidTankNTM[]} -
 * see that interface's own javadoc for why (matches {@code NTMFluidHandlerWrapper}'s already-committed
 * convention).
 *
 * <p>{@code com.hbm.blockentity.network.PipeBaseBlockEntity} does not implement
 * {@link IFluidUserMK2} (a bare duct has no tanks, only a single {@code FluidType} field), so it
 * overrides both {@link #getFluidIDToCopy()} and {@link #getTankToPaste()} directly instead of relying
 * on these tank-based defaults - exactly like CE's own {@code TileEntityPipeBaseNT} does.
 */
public interface IFluidCopiable extends ICopiable {

    /**
     * @return First type for the normal paste, second type for the alt paste,
     *         none if there is no alt paste support
     */
    default int[] getFluidIDToCopy() {
        IFluidUserMK2 tile = (IFluidUserMK2) this;
        List<Integer> types = new ArrayList<>();

        for (FluidTankNTM tank : tile.getAllTanks()) {
            if (!tank.getTankType().hasNoID()) types.add(tank.getTankType().getID());
        }

        // CE routes this through its own BobMathUtil.intCollectionToArray, not ported to this port -
        // inlined here rather than pulling in a whole new util class for one boxed-to-primitive
        // conversion nothing else in this port needs yet.
        int[] ids = new int[types.size()];
        for (int i = 0; i < ids.length; i++) ids[i] = types.get(i);
        return ids;
    }

    default FluidTankNTM getTankToPaste() {
        if (this instanceof IFluidStandardTransceiverMK2 tile) {
            List<FluidTankNTM> receiving = tile.getReceivingTanks();
            return receiving.isEmpty() ? null : receiving.get(0);
        }
        return null;
    }

    @Override
    default CompoundTag getSettings(Level world, BlockPos pos) {
        CompoundTag tag = new CompoundTag();
        int[] ids = getFluidIDToCopy();
        if (ids.length > 0) tag.putIntArray("fluidID", ids);
        return tag;
    }

    @Override
    default void pasteSettings(CompoundTag nbt, int index, Level world, Player player, BlockPos pos) {
        if (getTankToPaste() != null) {
            int[] ids = nbt.getIntArray("fluidID");
            if (ids.length > 0 && index < ids.length) {
                getTankToPaste().setTankType(Fluids.fromID(ids[index]));
            }
        }
    }

    @Override
    default String[] infoForDisplay(Level world, BlockPos pos) {
        int[] ids = getFluidIDToCopy();
        String[] names = new String[ids.length];
        for (int i = 0; i < ids.length; i++) {
            names[i] = Fluids.fromID(ids[i]).getTranslationKey();
        }
        return names;
    }
}
