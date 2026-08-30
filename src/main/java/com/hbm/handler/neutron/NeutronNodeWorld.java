package com.hbm.handler.neutron;

import com.hbm.main.MainRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * One {@link StreamWorld} (stream list + node cache) per {@link ServerLevel}, garbage-collected
 * when empty. CE: {@code com.hbm.handler.neutron.NeutronNodeWorld}, read in full - ported
 * unchanged except for {@code World -> ServerLevel} (RBMK simulation is server-only in CE; see
 * {@code com.hbm.api.rbmk.IRBMKColumn}'s javadoc) and the per-{@link StreamWorld}
 * {@link RBMKNeutronHandler.TickContext} addition described on {@link RBMKNeutronHandler}'s class
 * javadoc.
 */
public final class NeutronNodeWorld {

    private NeutronNodeWorld() {
    }

    /** List of all stream worlds. */
    public static final Map<ServerLevel, StreamWorld> streamWorlds = new HashMap<>();

    @Nullable
    public static NeutronNode getNode(ServerLevel level, BlockPos pos) {
        StreamWorld streamWorld = streamWorlds.get(level);
        return streamWorld != null ? streamWorld.getNode(pos) : null;
    }

    public static void removeNode(ServerLevel level, BlockPos pos) {
        StreamWorld streamWorld = streamWorlds.get(level);
        if (streamWorld == null) return;
        streamWorld.removeNode(pos);
    }

    public static StreamWorld getOrAddWorld(ServerLevel level) {
        return streamWorlds.computeIfAbsent(level, l -> new StreamWorld());
    }

    public static void removeAllWorlds() {
        streamWorlds.clear();
    }

    public static void removeEmptyWorlds() {
        streamWorlds.values().removeIf(streamWorld -> streamWorld.streams.isEmpty());
    }

    public static final class StreamWorld {

        private final List<NeutronStream> streams = new ArrayList<>();
        private final Map<BlockPos, NeutronNode> nodeCache = new HashMap<>();

        /**
         * The per-tick dial snapshot for this world, refreshed once per server tick by
         * {@link NeutronHandler#onServerTick()} before {@link #runStreamInteractions} runs. See
         * {@link RBMKNeutronHandler}'s class javadoc for why this replaces CE's shared mutable
         * static fields.
         */
        private RBMKNeutronHandler.TickContext tickContext = RBMKNeutronHandler.TickContext.DEFAULT;

        public RBMKNeutronHandler.TickContext getTickContext() {
            return tickContext;
        }

        public void setTickContext(RBMKNeutronHandler.TickContext tickContext) {
            this.tickContext = tickContext;
        }

        public void runStreamInteractions(ServerLevel level) {
            for (NeutronStream stream : streams) {
                stream.runStreamInteraction(level, this);
            }
        }

        public void addStream(NeutronStream stream) {
            streams.add(stream);
        }

        public void removeAllStreams() {
            streams.clear();
        }

        public void cleanNodes() {
            List<BlockPos> toRemove = new ArrayList<>();
            for (NeutronNode cachedNode : nodeCache.values()) {
                if (cachedNode.getKind() == NeutronStream.NeutronKind.RBMK) {
                    RBMKNeutronHandler.RBMKNeutronNode node = (RBMKNeutronHandler.RBMKNeutronNode) cachedNode;
                    toRemove.addAll(node.checkNode(this, tickContext));
                }
            }

            for (BlockPos pos : toRemove) {
                nodeCache.remove(pos);
            }
        }

        @Nullable
        public NeutronNode getNode(BlockPos pos) {
            NeutronNode node = nodeCache.get(pos);
            if (node != null && node.getColumn().isRemoved()) {
                MainRegistry.logger.warn("[NeutronNodeWorld] Removed invalid neutron node {} at {}", node, pos);
                nodeCache.remove(pos);
                return null;
            }
            return node;
        }

        public void addNode(NeutronNode node) {
            nodeCache.put(node.getPos(), node);
        }

        public void removeNode(BlockPos pos) {
            nodeCache.remove(pos);
        }

        public void removeAllStreamsOfType(NeutronStream.NeutronKind kind) {
            streams.removeIf(stream -> stream.kind == kind);
        }
    }
}
