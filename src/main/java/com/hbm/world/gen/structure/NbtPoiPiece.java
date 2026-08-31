package com.hbm.world.gen.structure;

import com.hbm.world.gen.nbt.NbtTemplatePlacer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * Single-template piece. Bounding box is a conservative 48³ until the template is loaded at
 * {@link #postProcess} (CE pieces are small; aircraft_carrier/lighthouse still fit).
 */
public class NbtPoiPiece extends StructurePiece {

    private final String template;
    private final Rotation rotation;

    public NbtPoiPiece(String template, BlockPos origin, Rotation rotation) {
        super(ModStructureTypes.NBT_POI_PIECE.get(), 0, boxAround(origin));
        this.template = template;
        this.rotation = rotation;
    }

    public NbtPoiPiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(ModStructureTypes.NBT_POI_PIECE.get(), tag);
        this.template = tag.getString("Template");
        this.rotation = Rotation.valueOf(tag.getString("Rot"));
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putString("Template", template);
        tag.putString("Rot", rotation.name());
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        NbtTemplatePlacer.place(level, template, new BlockPos(boundingBox.minX(), boundingBox.minY(), boundingBox.minZ()), rotation, box);
    }

    private static BoundingBox boxAround(BlockPos origin) {
        return new BoundingBox(origin.getX(), origin.getY(), origin.getZ(),
                origin.getX() + 64, origin.getY() + 48, origin.getZ() + 64);
    }
}
