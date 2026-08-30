package com.hbm.handler.neutron;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import java.util.Iterator;

/**
 * A directional flux packet travelling away from a {@link NeutronNode}. CE:
 * {@code com.hbm.handler.neutron.NeutronStream} (79 lines, read in full), ported unchanged except
 * for the type-system adjustments noted below.
 * <p>
 * CE's {@code NeutronType} enum also carries a {@code PILE} value for Chicago-pile content; this
 * port's version drops it (renamed {@link NeutronKind} to avoid confusion with
 * {@code RBMKNeutronHandler.RBMKType}) - {@code com.hbm.handler.neutron.PileNeutronHandler} is
 * explicitly out of scope for this pass (docs/phase2/rbmk_reactor.md's "Sources read in full" list
 * calls it out by name as "Chicago-pile content, not RBMK"). Re-add a {@code PILE} constant here
 * first if that system is ever ported.
 */
public abstract class NeutronStream {

    public enum NeutronKind {
        /** Dummy streams used only to walk a {@link NeutronNode}'s cache lookups, never added to a {@link NeutronNodeWorld.StreamWorld}'s stream list. */
        DUMMY,
        /** RBMK neutron streams. */
        RBMK
    }

    public NeutronNode origin;

    // doubles!!
    public double fluxQuantity;
    // Basically a ratio for slow flux to fast flux.
    // 0 = all slow flux
    // 1 = all fast flux
    public double fluxRatio;

    public NeutronKind kind = NeutronKind.DUMMY;

    // Vector for direction of neutron flow.
    public Vec3 vector;

    protected BlockPos posInstance;

    private int i;

    /** Primarily used as a "dummy stream" for node-cache walks - not added to any stream list. */
    protected NeutronStream(NeutronNode origin, Vec3 vector) {
        this.origin = origin;
        this.vector = vector;
        this.posInstance = origin.getPos();
    }

    protected NeutronStream(NeutronNode origin, Vec3 vector, double flux, double ratio, NeutronKind kind) {
        this.origin = origin;
        this.vector = vector;
        this.posInstance = origin.getPos();
        this.fluxQuantity = flux;
        this.fluxRatio = ratio;
        this.kind = kind;

        NeutronNodeWorld.getOrAddWorld(origin.getColumn().getRbmkLevel()).addStream(this);
    }

    /**
     * Walks an iterator of {@link BlockPos} outward along this stream's vector, one block at a
     * time (rounds {@code 0.5 + vector * i} to an int - this is how a diagonal-looking direction
     * still walks an axis-aligned column of blocks, though in practice RBMK only ever uses the 4
     * cardinal vectors). USES THE CACHE - see {@code RBMKNeutronHandler} for how the returned
     * positions are resolved against {@link NeutronNodeWorld.StreamWorld}'s node cache.
     */
    public Iterator<BlockPos> getBlocks(int range) {
        i = 1;

        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return i <= range;
            }

            @Override
            public BlockPos next() {
                int x = (int) Math.floor(0.5 + vector.x * i);
                int z = (int) Math.floor(0.5 + vector.z * i);

                i++;
                BlockPos originPos = origin.getPos();
                posInstance = new BlockPos(originPos.getX() + x, originPos.getY(), originPos.getZ() + z);
                return posInstance;
            }
        };
    }

    public abstract void runStreamInteraction(ServerLevel level, NeutronNodeWorld.StreamWorld streamWorld);
}
