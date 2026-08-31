package com.hbm.util;

import com.hbm.api.block.ICrucibleAcceptor;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Ported from CE's {@code com.hbm.util.CrucibleUtil} (167 lines, read in full): hitscan-based
 * pouring dispatch to an {@link ICrucibleAcceptor} below the pour origin, with a "safe" spill
 * fallback when nothing accepts. {@code World.rayTraceBlocks}/{@code RayTraceResult} maps onto
 * {@link Level#clip(ClipContext)}/{@link BlockHitResult} (the same idiom already established
 * elsewhere in this port, e.g. {@code ItemDesignatorRange}/{@code TurretBaseBlockEntity}'s own
 * entity-less {@code ClipContext(..., null)} raytrace); {@code ForgeDirection} maps onto
 * {@link Direction}.
 * <p>
 * <b>Deliberately not ported</b>: CE's {@code MutableVec3d impactPosHolder} out-parameter and the
 * particle-effect/network-packet plumbing built on top of it in
 * {@code TileEntityCrucible.update()} ({@code AuxParticlePacketNT}/{@code HbmEffectNT.Foundry},
 * {@code PacketThreading}) - none of those three classes exist anywhere in this port yet (confirmed
 * by exhaustive grep), and inventing them is out of this task's scope (a separate, not-yet-ported
 * particle-engine system). The pour <i>routing</i> logic (raytrace -&gt; acceptor lookup -&gt;
 * partial-pour negotiation -&gt; spill) is preserved in full; only the cosmetic "Foundry" particle
 * burst on a successful pour is dropped.
 */
public final class CrucibleUtil {

    private CrucibleUtil() {
    }

    /**
     * Standard pouring: hitscans straight down from {@code (x,y,z)} for up to {@code range} blocks,
     * pours up to {@code quanta} of the first eligible material in {@code stacks} into whatever
     * {@link ICrucibleAcceptor} it hits, and returns the material actually removed. The list's
     * entries are mutated in place (matches CE exactly - copy the list first if that's not wanted).
     * Returns {@code null} if the list was already empty.
     */
    @Nullable
    public static Mats.MaterialStack pourFullStack(Level level, double x, double y, double z, double range, boolean safe, List<Mats.MaterialStack> stacks, int quanta) {
        if (stacks.isEmpty()) return null;

        Vec3 start = new Vec3(x, y, z);
        Vec3 end = new Vec3(x, y - range, z);
        BlockHitResult hit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, null));

        ICrucibleAcceptor acceptor = getPouringTarget(level, hit);
        if (acceptor == null) {
            return spill(safe, stacks, quanta);
        }

        for (Mats.MaterialStack stack : stacks) {
            if (stack.material == null) continue;

            int amountToPour = Math.min(stack.amount, quanta);
            Mats.MaterialStack toPour = new Mats.MaterialStack(stack.material, amountToPour);
            Mats.MaterialStack left = tryPourStack(level, acceptor, hit, toPour);

            if (left != null) {
                stack.amount -= (amountToPour - left.amount);
                return new Mats.MaterialStack(stack.material, stack.amount - left.amount);
            }
        }

        return spill(safe, stacks, quanta);
    }

    /**
     * Tries to pour {@code stack} onto {@code acceptor}. Returns whatever is left of the stack when
     * the acceptor was actually consulted (a full accept returns a zero-amount leftover, matching
     * CE), or {@code null} if the acceptor was never asked (material isn't {@code SMELTABLE}) or
     * refused outright.
     */
    @Nullable
    public static Mats.MaterialStack tryPourStack(Level level, ICrucibleAcceptor acceptor, BlockHitResult hit, Mats.MaterialStack stack) {
        if (stack.material.smeltable != NTMMaterial.SmeltingBehavior.SMELTABLE) {
            return null;
        }

        Vec3 hitVec = hit.getLocation();
        Direction side = hit.getDirection();

        if (acceptor.canAcceptPartialPour(level, hit.getBlockPos(), hitVec.x, hitVec.y, hitVec.z, side, stack)) {
            Mats.MaterialStack left = acceptor.pour(level, hit.getBlockPos(), hitVec.x, hitVec.y, hitVec.z, side, stack);
            return left != null ? left : new Mats.MaterialStack(stack.material, 0);
        }

        return null;
    }

    /** Uses a downward hitscan to find the target of the pour. {@code null} on a miss or a non-acceptor block. */
    @Nullable
    public static ICrucibleAcceptor getPouringTarget(Level level, @Nullable BlockHitResult hit) {
        if (hit == null || hit.getType() != HitResult.Type.BLOCK) return null;
        return level.getBlockState(hit.getBlockPos()).getBlock() instanceof ICrucibleAcceptor acceptor ? acceptor : null;
    }

    /** Regular spillage but for a stack list - uses the first available entry, matching CE. Assumes the list is non-empty. */
    @Nullable
    public static Mats.MaterialStack spill(boolean safe, List<Mats.MaterialStack> stacks, int quanta) {
        Mats.MaterialStack top = stacks.get(0);
        Mats.MaterialStack ret = spill(safe, top, quanta);
        stacks.removeIf(s -> s.amount <= 0);
        return ret;
    }

    /** No valid acceptor found: a no-op when {@code safe}, otherwise the material lost to the void. Mutates {@code stack}. */
    @Nullable
    public static Mats.MaterialStack spill(boolean safe, Mats.MaterialStack stack, int quanta) {
        if (safe) {
            return null;
        }

        Mats.MaterialStack toWaste = new Mats.MaterialStack(stack.material, Math.min(stack.amount, quanta));
        stack.amount -= toWaste.amount;
        return toWaste;
    }
}
