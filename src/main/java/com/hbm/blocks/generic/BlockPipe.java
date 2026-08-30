package com.hbm.blocks.generic;

import com.hbm.world.gen.nbt.INBTBlockTransformable;
import net.minecraft.world.level.block.RotatedPillarBlock;

/**
 * Decorative pipe segment, ported from CE's {@code BlockPipe}. CE hand-rolled its own
 * {@code PropertyEnum<EnumFacing.Axis> AXIS}, set from the placer's clicked face exactly like
 * vanilla's own pillar blocks already do - this collapses to a thin {@link RotatedPillarBlock}
 * subclass, the same shape already used for {@link BlockRotatablePillar}/{@link BlockPinkLog}.
 * CE's non-opaque/cutout-rendering flags ({@code isOpaqueCube}/{@code isFullCube}/
 * {@code isNormalCube}, {@code BlockRenderLayer.CUTOUT_MIPPED}) are datagen/model concerns under
 * this port's ground rule (a {@code Properties.noOcclusion()} at the registration call site plus a
 * cutout-render-type datagen entry reproduce them) rather than Java overrides. CE's
 * {@code getBlockFaceShape} special case (the six {@code deco_pipe_framed*} variants render a solid
 * top face so machine casings can attach to them) has no port equivalent yet - flagged here rather
 * than guessed at, since {@code getBlockSupportShape} would need per-instance wiring this class does
 * not currently carry; every pipe variant registers and places identically in the meantime.
 * {@link INBTBlockTransformable} is implemented as the empty marker described on that interface -
 * CE's meta-int {@code transformMeta}/{@code transformMetaPillar} rotation math has no
 * blockstate-based equivalent to port yet (world-gen NBT structures are not ported, per the port
 * report's cross-cutting note); only affects correctness under structure-file rotation.
 */
public class BlockPipe extends RotatedPillarBlock implements INBTBlockTransformable {

    public BlockPipe(Properties properties) {
        super(properties);
    }
}
