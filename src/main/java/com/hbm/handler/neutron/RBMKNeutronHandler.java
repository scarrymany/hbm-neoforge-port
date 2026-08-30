package com.hbm.handler.neutron;

import com.hbm.api.rbmk.IRBMKColumn;
import com.hbm.api.rbmk.IRBMKControlColumn;
import com.hbm.api.rbmk.IRBMKFluxReceiver;
import com.hbm.api.rbmk.RBMKDials;
import com.hbm.handler.neutron.NeutronNodeWorld.StreamWorld;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Per-column-type neutron flux interaction dispatch - the actual reactor physics. CE:
 * {@code com.hbm.handler.neutron.RBMKNeutronHandler} (421 lines, read in full).
 * <p>
 * Ported against this package's own {@code com.hbm.api.rbmk} contracts
 * ({@link IRBMKColumn}/{@link IRBMKFluxReceiver}/{@link IRBMKControlColumn}/{@link RBMKType})
 * instead of CE's concrete {@code TileEntityRBMKRod}/{@code TileEntityRBMKControl}/etc. tile
 * entities, which belong to the parallel column-blocks package
 * ({@code com.hbm.blockentity.machine.rbmk}, this wave's sibling work package) - see
 * {@link #columnAt}. Resolving a {@link BlockPos} to a column only ever goes through the generic
 * {@code ServerLevel#getBlockEntity(BlockPos)} plus an {@code instanceof IRBMKColumn} check, so
 * this class has zero compile-time dependency on the sibling package's classes.
 * <p>
 * <b>Deliberate deviation from CE:</b> CE holds the per-tick dial snapshot
 * ({@code moderatorEfficiency}/{@code reflectorEfficiency}/{@code absorberEfficiency}/
 * {@code columnHeight}/{@code fluxRange}) as shared mutable {@code static} fields on this class,
 * refreshed once per tick in {@code NeutronHandler.onServerTick()} - CE's own in-file comment flags
 * this as "TODO: per-world parallelism" and explicitly not thread/multi-world-safe (see
 * docs/phase2/rbmk_reactor.md's Open Questions, which calls out this exact tension and asks for an
 * explicit decision). This port threads the same snapshot through as an explicit, immutable
 * {@link TickContext} instead, stored per-{@link StreamWorld} (i.e. already per-{@link ServerLevel}
 * - see {@link StreamWorld#setTickContext}/{@link StreamWorld#getTickContext}). This fixes the bug
 * for free while making every method below directly unit-testable with a hand-built
 * {@link TickContext} and zero {@link ServerLevel}/config dependency.
 */
public final class RBMKNeutronHandler {

    private RBMKNeutronHandler() {
    }

    /**
     * The neutron-interaction role of one RBMK reactor column. CE: nested enum
     * {@code RBMKNeutronHandler.RBMKType} - kept nested here (rather than promoted to a top-level
     * type in {@code com.hbm.api.rbmk}) specifically because the parallel column-blocks package
     * (this wave's sibling work package, {@code com.hbm.blockentity.machine.rbmk}) already landed
     * ~20 files in this shared working tree that call {@code RBMKNeutronHandler.RBMKType.XXX}
     * directly (confirmed by reading that package's own committed code, not a guess) - nesting it
     * here exactly matches both CE's real shape and every one of those already-written call sites,
     * at zero design cost. {@link com.hbm.api.rbmk.IRBMKColumn#getRBMKType()} references this type
     * directly (a deliberate, CE-faithful cross-package reference: CE's own
     * {@code TileEntityRBMKBase.getRBMKType()} likewise returns {@code RBMKNeutronHandler.RBMKType}
     * from the tile-entity package).
     */
    public enum RBMKType {
        ROD,
        MODERATOR,
        CONTROL_ROD,
        REFLECTOR,
        ABSORBER,
        OUTGASSER,
        /** CE comment, preserved verbatim: "don't bother with neutron calculations on this, it can't change anything". */
        OTHER
    }

    /**
     * Immutable per-tick snapshot of the RBMK dials the flux engine consults during one stream
     * interaction pass. See this class's javadoc for why this replaces CE's shared static fields.
     */
    public record TickContext(double moderatorEfficiency, double reflectorEfficiency,
                               double absorberEfficiency, int columnHeight, int fluxRange) {

        /** CE's declared dial defaults ({@code RBMKKeys}), for tests and as the fallback before the first real tick. */
        public static final TickContext DEFAULT = forLevel(null);

        public static TickContext forLevel(@Nullable ServerLevel level) {
            return new TickContext(
                    RBMKDials.getModeratorEfficiency(level),
                    RBMKDials.getReflectorEfficiency(level),
                    RBMKDials.getAbsorberEfficiency(level),
                    // CE comment, preserved verbatim: "IT'S NOT THE TOTAL HEIGHT IT'S THE AMOUNT
                    // OF BLOCKS ABOVE" - this is getColumnHeight()+1, not the raw height dial.
                    RBMKDials.getColumnHeight(level) + 1,
                    RBMKDials.getFluxRange(level)
            );
        }
    }

    /** The four cardinal flux directions every ordinary (non-ReaSim) fuel rod/outgasser spreads along. CE: {@code TileEntityRBMKRod.fluxDirs} / the private {@code NEUTRON_VECTORS} constant. Exposed here so the column-blocks package's rod tile entity can reuse it instead of redeclaring the same four vectors. */
    public static final Vec3[] CARDINAL_DIRECTIONS = {
            new Vec3(0, 0, -1), // NORTH
            new Vec3(1, 0, 0),  // EAST
            new Vec3(0, 0, 1),  // SOUTH
            new Vec3(-1, 0, 0)  // WEST
    };

    /** Resolves a {@link BlockPos} to an {@link IRBMKColumn} via a generic block-entity lookup - see this class's javadoc. */
    @Nullable
    private static IRBMKColumn columnAt(ServerLevel level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        return be instanceof IRBMKColumn column ? column : null;
    }

    public static RBMKNeutronNode makeNode(StreamWorld streamWorld, IRBMKColumn column) {
        NeutronNode existing = streamWorld.getNode(column.getRbmkPos());
        return existing instanceof RBMKNeutronNode node ? node : new RBMKNeutronNode(column, column.getRBMKType(), column.hasLid());
    }

    /**
     * CE: {@code TileEntityRBMKRod#spreadFlux(double, double)}. Spawns the four cardinal-direction
     * neutron streams a rod/outgasser column emits after producing {@code flux} output this tick,
     * or uncaches the column's node if it produced none. The column-blocks package's rod/outgasser
     * tile entities should call this from their own tick loop after computing their output flux
     * (CE: {@code ItemRBMKRod#burn}'s return value - see this package's {@code ItemRBMKRod}).
     */
    public static void spreadFlux(IRBMKFluxReceiver receiver, double flux, double ratio) {
        ServerLevel level = receiver.getRbmkLevel();
        BlockPos pos = receiver.getRbmkPos();

        if (flux <= 0) {
            NeutronNodeWorld.removeNode(level, pos);
            return;
        }

        StreamWorld streamWorld = NeutronNodeWorld.getOrAddWorld(level);
        RBMKNeutronNode node = makeNode(streamWorld, receiver);
        if (streamWorld.getNode(pos) == null) streamWorld.addNode(node);

        for (Vec3 vector : CARDINAL_DIRECTIONS) {
            new RBMKNeutronStream(node, vector, flux, ratio);
        }
    }

    /**
     * CE: {@code TileEntityRBMKRodReaSim#spreadFlux(double, double)}. Emits 8 streams, 45 degrees
     * apart, starting from a random cardinal-aligned offset, each carrying 75% of the produced
     * flux - CE's own hardcoded {@code flux * 0.75}, NOT currently backed by
     * {@link RBMKDials#getReaSimCount}/{@link RBMKDials#getReaSimOutputMod} despite those dials
     * existing (confirmed by reading the real, current CE source rather than assuming the dials'
     * existence implies they're wired up - preserved exactly as CE's real, currently-live
     * behavior). CE's {@code Vec3NT#rotateAroundYDeg} helper is replaced with direct trigonometry
     * below (not part of this package's scope to port), matching CE's real
     * {@code Vec3NT#rotateAroundYRad} clockwise rotation-sign convention exactly so the resulting
     * 8-direction set is bit-for-bit identical to CE's under a fixed RNG seed, not merely a
     * mirror-image set of the same 8 evenly-spaced directions.
     *
     * @param nodePos a caller-owned cache key for this ReaSim column (CE:
     *                {@code TileEntityRBMKRodReaSim#posReasimRod}, a lazily-allocated copy of the
     *                tile's own position kept deliberately distinct from
     *                {@code receiver.getRbmkPos()} - callers may simply pass
     *                {@code receiver.getRbmkPos()} unless they need that exact CE aliasing)
     */
    public static void spreadFluxReaSim(IRBMKFluxReceiver receiver, BlockPos nodePos, double flux, double ratio, RandomSource random) {
        ServerLevel level = receiver.getRbmkLevel();

        if (flux == 0) {
            NeutronNodeWorld.removeNode(level, nodePos);
            return;
        }

        StreamWorld streamWorld = NeutronNodeWorld.getOrAddWorld(level);
        NeutronNode existing = streamWorld.getNode(nodePos);
        RBMKNeutronNode node = existing instanceof RBMKNeutronNode rn ? rn : makeNode(streamWorld, receiver);
        if (existing == null) streamWorld.addNode(node);

        // CE's Vec3NT.rotateAroundYRad(a) is nx = x*cos(a) + z*sin(a); nz = -x*sin(a) + z*cos(a) -
        // a clockwise rotation in the (x,z) plane. Both the initial-angle vector below (starting
        // from CE's (1,0,0)) and the per-step rotation apply that exact formula so this matches
        // CE's real, live TileEntityRBMKRodReaSim#spreadFlux direction set bit-for-bit under a
        // fixed RNG seed, not just as a statistically-equivalent mirror image.
        double angle = Math.toRadians(random.nextInt(4) * 9D);
        double vx = Math.cos(angle);
        double vz = -Math.sin(angle);
        double step = Math.toRadians(45D);
        double cosStep = Math.cos(step);
        double sinStep = Math.sin(step);

        for (int i = 0; i < 8; i++) {
            new RBMKNeutronStream(node, new Vec3(vx, 0, vz), flux * 0.75D, ratio);
            double nvx = vx * cosStep + vz * sinStep;
            double nvz = -vx * sinStep + vz * cosStep;
            vx = nvx;
            vz = nvz;
        }
    }

    /**
     * Wraps one {@link IRBMKColumn} plus a snapshot of its {@link RBMKType}/lid state, taken at
     * node-creation time. CE: {@code RBMKNeutronHandler.RBMKNeutronNode} - CE stores the same two
     * values in a stringly-typed {@code Map<String,Object>} (shared machinery with the Pile
     * handler); this port uses plain typed fields instead since Pile content is out of this pass's
     * scope (see {@link NeutronNode}'s javadoc).
     */
    public static final class RBMKNeutronNode extends NeutronNode {

        private boolean hasLid;
        private final RBMKType type;

        public RBMKNeutronNode(IRBMKColumn column, RBMKType type, boolean hasLid) {
            super(column, NeutronStream.NeutronKind.RBMK);
            this.type = type;
            this.hasLid = hasLid;
        }

        public boolean hasLid() {
            return hasLid;
        }

        /** Called by the column-blocks package when a lid is installed on an already-cached column, without waiting for a full node rebuild. CE: {@code RBMKNeutronNode#addLid()}. */
        public void addLid() {
            this.hasLid = true;
        }

        /** CE: {@code RBMKNeutronNode#removeLid()}. */
        public void removeLid() {
            this.hasLid = false;
        }

        public RBMKType getRBMKType() {
            return type;
        }

        /**
         * CE: {@code RBMKNeutronNode#getReaSimNodes()}. Iterates every {@link BlockPos} within a
         * circular diamond of radius {@code ctx.fluxRange()} around this node, used by
         * {@link #checkNode} to test "is there any live fuel rod anywhere nearby" for cache
         * eviction purposes.
         * <p>
         * <b>CE quirk preserved verbatim:</b> the in-circle test uses the PRE-increment
         * {@code (x,z)}, but the position returned when in-circle uses the POST-increment
         * {@code (x,z)} - i.e. one grid cell "ahead" of the cell that was actually tested. This is
         * CE's real, uncorrected behavior; it is only ever consumed by the cache-eviction
         * heuristic below, never by the flux physics itself, so preserving it exactly means "the
         * cache diamond is very slightly mis-shapen," not an actual gameplay/physics bug.
         */
        public Iterator<BlockPos> getReaSimNodes(TickContext ctx) {
            int range = ctx.fluxRange();
            BlockPos origin = this.getPos();

            return new Iterator<>() {
                int x = -range;
                int z = -range;

                @Override
                public boolean hasNext() {
                    return (range + x) * (range * 2 + 1) + z + range + 1 < (range * 2 + 1) * (range * 2 + 1);
                }

                @Override
                @Nullable
                public BlockPos next() {
                    boolean inCircle = Math.pow(x, 2) + Math.pow(z, 2) <= (double) range * range;
                    z++;
                    if (z > range) {
                        z = -range;
                        x++;
                    }
                    return inCircle ? origin.offset(x, 0, z) : null;
                }
            };
        }

        /**
         * Decides whether this cached node (and, in some branches, a wider set of related cached
         * nodes) can be safely evicted from the 20-tick cache-refresh pass. CE:
         * {@code RBMKNeutronNode#checkNode(StreamWorld)}, read in full and ported structurally
         * unchanged against this package's generalized {@link IRBMKFluxReceiver} contract (see
         * that interface's {@code canReceiveFlux}/{@code getLastFluxQuantity}/{@code isReaSimVariant}
         * javadoc for exactly which CE {@code instanceof TileEntityRBMKRod(ReaSim)} check each one
         * replaces).
         *
         * @return positions to evict from the node cache (may be empty - an empty, non-null result means "keep this node cached")
         */
        public List<BlockPos> checkNode(StreamWorld streamWorld, TickContext ctx) {
            List<BlockPos> list = new ArrayList<>();
            BlockPos pos = this.getPos();

            RBMKNeutronStream[] streams = new RBMKNeutronStream[CARDINAL_DIRECTIONS.length];
            for (int i = 0; i < CARDINAL_DIRECTIONS.length; i++) {
                streams[i] = new RBMKNeutronStream(this, CARDINAL_DIRECTIONS[i]);
            }

            IRBMKFluxReceiver receiver = column instanceof IRBMKFluxReceiver r ? r : null;
            boolean isRod = type == RBMKType.ROD && receiver != null;

            // Check if this rod (non-ReaSim) should uncache its 4-cardinal-direction nodes.
            if (isRod && !receiver.isReaSimVariant()) {
                if (!receiver.canReceiveFlux() || receiver.getLastFluxQuantity() == 0) {
                    for (RBMKNeutronStream stream : streams) {
                        for (NeutronNode n : stream.getNodes(streamWorld, ctx, false)) {
                            if (n != null) list.add(n.getPos());
                        }
                    }
                    return list;
                }
            }

            // Check if this ReaSim rod should uncache its whole diamond region.
            if (isRod && receiver.isReaSimVariant()) {
                if (!receiver.canReceiveFlux() || receiver.getLastFluxQuantity() == 0) {
                    getReaSimNodes(ctx).forEachRemaining(p -> {
                        if (p != null) list.add(p);
                    });
                    return list;
                }
            }

            // Check if THIS node (not necessarily a rod itself) should be uncached because there is
            // no live fuel rod anywhere within the diamond around it.
            {
                boolean hasRodNearby = false;
                Iterator<BlockPos> reaSimNodes = getReaSimNodes(ctx);
                while (reaSimNodes.hasNext()) {
                    BlockPos nodePos = reaSimNodes.next();
                    if (nodePos == null) continue;

                    NeutronNode node = streamWorld.getNode(nodePos);
                    if (node instanceof RBMKNeutronNode rn && rn.getRBMKType() == RBMKType.ROD
                            && rn.column instanceof IRBMKFluxReceiver r2
                            && r2.canReceiveFlux() && r2.getLastFluxQuantity() > 0) {
                        hasRodNearby = true;
                        break;
                    }
                }

                if (!hasRodNearby) {
                    list.add(pos);
                    return list;
                }
            }

            // Check if this (non-rod) node should be uncached because no rod was found specifically
            // along the 4 cardinal streams' cached node lists.
            for (RBMKNeutronStream stream : streams) {
                for (NeutronNode n : stream.getNodes(streamWorld, ctx, false)) {
                    if (n instanceof RBMKNeutronNode rn && rn.getRBMKType() == RBMKType.ROD) {
                        return list; // empty - keep cached
                    }
                }
            }

            // No rods were found along this stream's path - safe to uncache.
            list.add(pos);
            return list;
        }
    }

    /**
     * The actual per-tick physics dispatch. CE: {@code RBMKNeutronHandler.RBMKNeutronStream}, read
     * in full.
     */
    public static final class RBMKNeutronStream extends NeutronStream {

        public RBMKNeutronStream(NeutronNode origin, Vec3 vector) {
            super(origin, vector);
        }

        public RBMKNeutronStream(NeutronNode origin, Vec3 vector, double flux, double ratio) {
            super(origin, vector, flux, ratio, NeutronKind.RBMK);
        }

        /** Does NOT include the origin node. USES THE CACHE. CE: {@code RBMKNeutronStream#getNodes}. */
        public NeutronNode[] getNodes(StreamWorld streamWorld, TickContext ctx, boolean addNode) {
            NeutronNode[] positions = new RBMKNeutronNode[ctx.fluxRange()];

            BlockPos originPos = origin.getPos();
            ServerLevel level = origin.getColumn().getRbmkLevel();

            for (int i = 1; i <= ctx.fluxRange(); i++) {
                int x = (int) Math.floor(0.5 + vector.x * i);
                int z = (int) Math.floor(0.5 + vector.z * i);
                BlockPos pos = new BlockPos(originPos.getX() + x, originPos.getY(), originPos.getZ() + z);

                NeutronNode node = streamWorld.getNode(pos);
                if (node instanceof RBMKNeutronNode rbmkNode) {
                    positions[i - 1] = rbmkNode;
                } else {
                    IRBMKColumn column = columnAt(level, pos);
                    if (column != null) {
                        RBMKNeutronNode rbmkNode = makeNode(streamWorld, column);
                        positions[i - 1] = rbmkNode;
                        if (addNode) streamWorld.addNode(rbmkNode);
                    }
                }
            }
            return positions;
        }

        /** The main per-tick physics dispatch for one stream. CE: {@code RBMKNeutronStream#runStreamInteraction}. */
        @Override
        public void runStreamInteraction(ServerLevel level, StreamWorld streamWorld) {
            TickContext ctx = streamWorld.getTickContext();

            // do nothing if there's nothing to do
            if (fluxQuantity == 0D) return;

            BlockPos originPos = origin.getPos();

            IRBMKColumn originColumn;
            NeutronNode node = streamWorld.getNode(originPos);
            if (node != null) {
                originColumn = node.getColumn();
            } else {
                originColumn = columnAt(level, originPos);
                if (originColumn == null) return; // doesn't exist anymore

                streamWorld.addNode(new RBMKNeutronNode(originColumn, originColumn.getRBMKType(), originColumn.hasLid()));
            }

            int moderatedCount = 0;

            Iterator<BlockPos> iterator = getBlocks(ctx.fluxRange());

            while (iterator.hasNext()) {
                BlockPos targetPos = iterator.next();

                if (fluxQuantity == 0D) return; // used it all up

                NeutronNode targetNode = streamWorld.getNode(targetPos);
                if (targetNode == null) {
                    IRBMKColumn column = columnAt(level, targetPos);
                    if (column != null) {
                        targetNode = makeNode(streamWorld, column);
                        streamWorld.addNode(targetNode);
                    } else {
                        int hits = getHits(level, ctx, targetPos);
                        if (hits == ctx.columnHeight()) {
                            return; // stream fully blocked
                        } else if (hits > 0) {
                            irradiateFromFlux(level, ctx, originPos, hits);
                            fluxQuantity *= 1 - ((double) hits / ctx.columnHeight());
                            continue;
                        } else {
                            irradiateFromFlux(level, ctx, originPos, 0);
                            continue;
                        }
                    }
                }

                RBMKNeutronNode rbmkTarget = (RBMKNeutronNode) targetNode;
                RBMKType type = rbmkTarget.getRBMKType();

                if (type == RBMKType.OTHER) continue; // pass right on by

                IRBMKColumn columnEntry = targetNode.getColumn();

                // Phase 4 forward reference, not callable from this wave (com.hbm.handler.radiation
                // does not exist in this port): CE's RBMKNeutronStream#runStreamInteraction calls
                // ChunkRadiationManager.proxy.incrementRad(level, targetPos, this.fluxQuantity * 0.05D)
                // here when !rbmkTarget.hasLid() - see RBMKRodBlockEntity's matching forward reference.

                if (type == RBMKType.MODERATOR || columnEntry.isModerated()) {
                    moderatedCount++;
                    moderateStream(ctx);
                }

                if ((type == RBMKType.ROD || type == RBMKType.OUTGASSER) && columnEntry instanceof IRBMKFluxReceiver receiver) {
                    if (receiver.canReceiveFlux()) {
                        receiver.receiveFlux(this);
                        return;
                    }
                } else if (type == RBMKType.CONTROL_ROD && columnEntry instanceof IRBMKControlColumn control) {
                    if (control.getLevel() > 0.0D) {
                        this.fluxQuantity *= control.getMult();
                        continue;
                    }
                    return;
                } else if (type == RBMKType.REFLECTOR) {
                    if (origin.getColumn().isModerated()) moderatedCount++;

                    if (this.fluxRatio > 0 && moderatedCount > 0) {
                        for (int i = 0; i < moderatedCount; i++) moderateStream(ctx);
                    }

                    if (ctx.reflectorEfficiency() != 1.0D) {
                        this.fluxQuantity *= ctx.reflectorEfficiency();
                        continue;
                    }

                    // CE dispatches to the freshly-resolved `originTE` here, not `this.origin.tile`
                    // (used just above for the isModerated() check) - preserved exactly, see
                    // `originColumn` above.
                    if (originColumn instanceof IRBMKFluxReceiver originReceiver) {
                        originReceiver.receiveFlux(this);
                    }
                    return;
                } else if (type == RBMKType.ABSORBER) {
                    columnEntry.addHeat(RBMKDials.getAbsorberHeatConversion(level) * this.fluxQuantity);

                    if (ctx.absorberEfficiency() == 1) return;

                    this.fluxQuantity *= ctx.absorberEfficiency();
                }
            }

            NeutronNode[] nodes = getNodes(streamWorld, ctx, true);
            NeutronNode lastNode = nodes.length > 0 ? nodes[nodes.length - 1] : null;

            if (lastNode == null) {
                // No good way to figure out exactly where it should irradiate - irradiate one step past the origin.
                irradiateFromFlux(level, ctx, originPos.offset((int) this.vector.x, 0, (int) this.vector.z));
                return;
            }

            RBMKType lastNodeType = ((RBMKNeutronNode) lastNode).getRBMKType();

            if (lastNodeType == RBMKType.CONTROL_ROD && lastNode.getColumn() instanceof IRBMKControlColumn control) {
                if (control.getMult() > 0.0D) {
                    this.fluxQuantity *= control.getMult();
                    BlockPos posAfter = lastNode.getPos().offset((int) this.vector.x, 0, (int) this.vector.z);

                    // Resolves CE GitHub issue #1933: check if the block after the control rod is
                    // actually another RBMK column before falling back to a bare world irradiation.
                    if (NeutronNodeWorld.getNode(level, originPos) == null) {
                        IRBMKColumn columnAfter = columnAt(level, posAfter);
                        if (columnAfter != null) {
                            streamWorld.addNode(makeNode(streamWorld, columnAfter));
                        } else {
                            irradiateFromFlux(level, ctx, posAfter);
                        }
                    }
                }
            }
        }

        /**
         * Counts opaque-cube blocks stacked through the column's full height at {@code pos}, used
         * to partially attenuate a stream that hits a non-RBMK obstruction instead of a proper
         * column. CE: {@code RBMKNeutronStream#getHits(BlockPos)} - CE's own comment, preserved
         * verbatim: "total count of bugs fixed attributed to this function: 14". {@code isOpaqueCube()}
         * (Forge 1.12) is replaced with {@code BlockState#canOcclude()}, this port's confirmed
         * NeoForge 1.21.1 equivalent (already used the same way in
         * {@code com.hbm.blocks.generic.WasteMycelium}).
         */
        public static int getHits(ServerLevel level, TickContext ctx, BlockPos pos) {
            int hits = 0;
            for (int h = 0; h < ctx.columnHeight(); h++) {
                BlockState state = level.getBlockState(pos.above(h));
                if (state.canOcclude()) hits++;
            }
            return hits;
        }

        public void irradiateFromFlux(ServerLevel level, TickContext ctx, BlockPos pos) {
            // Phase 4 forward reference, not callable from this wave (com.hbm.handler.radiation
            // does not exist in this port): CE's RBMKNeutronStream#irradiateFromFlux(BlockPos) calls
            // ChunkRadiationManager.proxy.incrementRad(level, pos, fluxQuantity * 0.05D *
            // (1 - (double) getHits(level, ctx, pos) / ctx.columnHeight())) here - see
            // RBMKRodBlockEntity's matching forward reference.
        }

        public void irradiateFromFlux(ServerLevel level, TickContext ctx, BlockPos pos, int hits) {
            // Phase 4 forward reference, not callable from this wave (com.hbm.handler.radiation
            // does not exist in this port): CE's RBMKNeutronStream#irradiateFromFlux(BlockPos, int)
            // calls ChunkRadiationManager.proxy.incrementRad(level, pos, fluxQuantity * 0.05D *
            // (1 - (double) hits / ctx.columnHeight())) here - see RBMKRodBlockEntity's matching
            // forward reference.
        }

        public void moderateStream(TickContext ctx) {
            fluxRatio *= (1 - ctx.moderatorEfficiency());
        }
    }
}
