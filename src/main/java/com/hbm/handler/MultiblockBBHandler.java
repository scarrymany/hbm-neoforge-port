package com.hbm.handler;

import com.hbm.main.MainRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

/**
 * Ported from CE's {@code com.hbm.handler.MultiblockBBHandler}: loads the binary {@code .mbb}
 * bounding-box dumps used by non-rectangular multiblocks (currently only {@code MachineFENSU}) and
 * keeps a {@code Block -> MultiblockBounds} registry that {@link com.hbm.blocks.BlockDummyableMBB}
 * reads from.
 * <p>
 * CE's {@code REGISTRY.put(ModBlocks.machine_fensu, FENSU_BOUNDS)} wiring is intentionally NOT
 * reproduced here: {@code MachineFENSU} is one of the 150 concrete {@code BlockDummyable} subclasses
 * this Phase 2 package explicitly defers (see {@code docs/phase2/multiblock_framework.md}), and
 * {@code ModBlocks} has no such field yet. {@link #FENSU_BOUNDS} is exposed publicly so the future
 * wave that ports {@code MachineFENSU} can register it with one line
 * ({@code MultiblockBBHandler.REGISTRY.put(ModBlocks.machine_fensu, MultiblockBBHandler.FENSU_BOUNDS)})
 * instead of this class re-deciding the wiring.
 */
public class MultiblockBBHandler {

    public static final MultiblockBounds FENSU_BOUNDS = load(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "multiblock_bounds/bb_fensu0.mbb"));

    public static final Map<Block, MultiblockBounds> REGISTRY = new HashMap<>();

    public static MultiblockBounds load(ResourceLocation loc) {
        try (InputStream s = MultiblockBBHandler.class.getResourceAsStream("/assets/" + loc.getNamespace() + "/" + loc.getPath())) {
            if (s == null) {
                MainRegistry.logger.warn("MultiblockBBHandler: missing resource {}", loc);
                return null;
            }
            return parse(ByteBuffer.wrap(s.readAllBytes()));
        } catch (Exception e) {
            MainRegistry.logger.warn("MultiblockBBHandler: failed to load {}", loc, e);
        }
        return null;
    }

    private static MultiblockBounds parse(ByteBuffer buf) {
        buf.order(ByteOrder.LITTLE_ENDIAN);
        int version = buf.getInt();
        int offsetX = buf.getInt();
        int offsetY = buf.getInt();
        int offsetZ = buf.getInt();
        AABB[] boundingBoxes = new AABB[buf.getInt()];
        int numBlocks = buf.getInt();

        Map<BlockPos, AABB[]> blocks = new HashMap<>();

        for (int i = 0; i < boundingBoxes.length; i++) {
            boundingBoxes[i] = new AABB(buf.getFloat(), buf.getFloat(), buf.getFloat(), buf.getFloat(), buf.getFloat(), buf.getFloat());
        }
        for (int i = 0; i < numBlocks; i++) {
            BlockPos pos = BlockPos.containing(buf.getFloat(), buf.getFloat(), buf.getFloat());
            AABB[] blockBoxes = new AABB[buf.getInt()];
            for (int j = 0; j < blockBoxes.length; j++) {
                blockBoxes[j] = new AABB(buf.getFloat(), buf.getFloat(), buf.getFloat(), buf.getFloat(), buf.getFloat(), buf.getFloat());
            }
            blocks.put(pos, blockBoxes);
        }

        return new MultiblockBounds(boundingBoxes, blocks);
    }

    /**
     * CE's {@code MultiblockBounds} carries both a flat {@code boxes} array and a precomputed
     * per-position {@code blocks} map; the port's own {@code BlockDummyableMBB.rasterizeFootprint}
     * only ever reads {@code boxes} and recomputes the rasterization itself, but both are parsed
     * here regardless since the {@code .mbb} binary format always contains both.
     */
    public static class MultiblockBounds {
        public AABB[] boxes;
        public Map<BlockPos, AABB[]> blocks;

        public MultiblockBounds(AABB[] boxes, Map<BlockPos, AABB[]> blocks) {
            this.boxes = boxes;
            this.blocks = blocks;
        }
    }
}
