package com.hbm.world.gen.nbt;

/**
 * Marker for a block whose placed orientation needs adjusting when it is copied out of an NBT
 * structure file under rotation/mirroring - ported from CE's {@code INBTBlockTransformable}.
 * <p>
 * CE's original contract was a single {@code int transformMeta(int meta, int coordBaseMode)}
 * method plus a belt of {@code static} helper functions ({@code transformMetaPillar},
 * {@code transformMetaStairs}, {@code transformMetaDoor}, ...), all expressed in terms of 1.12's
 * integer block metadata. That contract has no meaningful 1.21 equivalent to port line-for-line:
 * modern Minecraft has no block metadata at all (orientation lives in typed
 * {@code BlockState}/{@code Property} values), and structure rotation itself is handled by
 * {@code Block#rotate(BlockState, Rotation)}/{@code #mirror(BlockState, Mirror)} - already
 * implemented natively wherever this port needs it (see e.g. {@link com.hbm.blocks.generic.BlockRailing},
 * {@link com.hbm.blocks.generic.BarbedWire}).
 * <p>
 * The world-gen NBT-structure system this marker belongs to has not been ported yet (Phase 2+, see
 * the blocks_generic port report's cross-cutting note on {@code INBTBlockTransformable}/
 * {@code INBTTileEntityTransformable}). Until that system lands and can define what "correct
 * rotation under a structure copy" actually means against {@code BlockState}, this interface is
 * kept as an empty marker - implementing it costs a class nothing today (no abstract methods to
 * satisfy) and lets that future pass find every block that cares via {@code instanceof
 * INBTBlockTransformable} instead of re-deriving the set from CE from scratch. Only affects
 * correctness under world-gen structure rotation, not ordinary placement/interaction.
 */
public interface INBTBlockTransformable {
}
