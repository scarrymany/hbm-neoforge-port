package com.hbm.world.gen.structure;

import com.hbm.main.MainRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * {@link StructureType} + {@link StructurePieceType} for CE NBT POIs. CE's own engine was
 * {@code NBTStructure extends MapGenStructure} ({@code NTMWorldGenerator.java}); 1.21 replacement
 * is a registered type + datapack {@code structure}/{@code structure_set}.
 */
public final class ModStructureTypes {

    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, MainRegistry.MODID);
    public static final DeferredRegister<StructurePieceType> PIECE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_PIECE, MainRegistry.MODID);

    public static final DeferredHolder<StructureType<?>, StructureType<NbtPoiStructure>> NBT_POI =
            STRUCTURE_TYPES.register("nbt_poi", () -> () -> NbtPoiStructure.CODEC);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> NBT_POI_PIECE =
            PIECE_TYPES.register("nbt_poi", () -> (StructurePieceType) NbtPoiPiece::new);

    private ModStructureTypes() {
    }

    public static void register(IEventBus modEventBus) {
        STRUCTURE_TYPES.register(modEventBus);
        PIECE_TYPES.register(modEventBus);
    }
}
