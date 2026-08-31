package com.hbm.world.gen.structure;

import com.hbm.config.StructureConfig;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

import java.util.Optional;

/**
 * Single-NBT POI structure. CE registration: {@code NTMWorldGenerator} {@code JigsawPiece(name, nbt,
 * heightOffset)} with {@code SpawnCondition.structure} (single piece, not a jigsaw graph).
 */
public class NbtPoiStructure extends Structure {

    public static final MapCodec<NbtPoiStructure> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    settingsCodec(instance),
                    Codec.STRING.fieldOf("template").forGetter(s -> s.template),
                    Codec.INT.optionalFieldOf("height_offset", 0).forGetter(s -> s.heightOffset),
                    Codec.BOOL.optionalFieldOf("surface", true).forGetter(s -> s.surface),
                    Codec.INT.optionalFieldOf("fixed_y", 32).forGetter(s -> s.fixedY),
                    Codec.STRING.optionalFieldOf("biome_gate", "land").forGetter(s -> s.biomeGate),
                    Codec.STRING.optionalFieldOf("config_gate", "none").forGetter(s -> s.configGate)
            ).apply(instance, NbtPoiStructure::new));

    private final String template;
    private final int heightOffset;
    private final boolean surface;
    private final int fixedY;
    private final String biomeGate;
    private final String configGate;

    public NbtPoiStructure(StructureSettings settings, String template, int heightOffset, boolean surface,
                           int fixedY, String biomeGate, String configGate) {
        super(settings);
        this.template = template;
        this.heightOffset = heightOffset;
        this.surface = surface;
        this.fixedY = fixedY;
        this.biomeGate = biomeGate;
        this.configGate = configGate;
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        if (StructureConfig.enableStructuresFlag() == 0) return Optional.empty();
        if ("ocean".equals(configGate) && !StructureConfig.ENABLE_OCEAN_STRUCTURES.get()) return Optional.empty();
        if ("ruins".equals(configGate) && !StructureConfig.ENABLE_RUINS.get()) return Optional.empty();

        int x = context.chunkPos().getMiddleBlockX();
        int z = context.chunkPos().getMiddleBlockZ();
        int y = surface
                ? context.chunkGenerator().getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG,
                context.heightAccessor(), context.randomState()) + heightOffset
                : fixedY + heightOffset;
        BlockPos pos = new BlockPos(x, y, z);
        Holder<Biome> biome = context.chunkGenerator().getBiomeSource().getNoiseBiome(
                x >> 2, y >> 2, z >> 2, context.randomState().sampler());
        if (!biomeAllowed(biome)) return Optional.empty();

        return Optional.of(new GenerationStub(pos, (StructurePiecesBuilder builder) ->
                builder.addPiece(new NbtPoiPiece(template, pos, Rotation.getRandom(context.random())))));
    }

    private boolean biomeAllowed(Holder<Biome> biome) {
        return switch (biomeGate) {
            case "sandy" -> biome.is(BiomeTags.IS_BADLANDS) || biome.is(BiomeTags.IS_BEACH)
                    || biome.is(net.minecraft.world.level.biome.Biomes.DESERT);
            case "flat" -> biome.is(net.minecraft.world.level.biome.Biomes.PLAINS)
                    || biome.is(net.minecraft.world.level.biome.Biomes.DESERT)
                    || biome.is(net.minecraft.world.level.biome.Biomes.SNOWY_PLAINS)
                    || biome.is(net.minecraft.world.level.biome.Biomes.SUNFLOWER_PLAINS);
            case "plains" -> biome.is(net.minecraft.world.level.biome.Biomes.PLAINS)
                    || biome.is(net.minecraft.world.level.biome.Biomes.SUNFLOWER_PLAINS);
            case "beach" -> biome.is(BiomeTags.IS_BEACH);
            case "ocean" -> biome.is(BiomeTags.IS_OCEAN);
            case "ocean_beach" -> biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_BEACH);
            case "rain" -> !biome.is(BiomeTags.IS_OCEAN) && !biome.is(BiomeTags.IS_RIVER)
                    && biome.value().hasPrecipitation();
            case "low" -> !biome.is(BiomeTags.IS_OCEAN) && !biome.is(BiomeTags.IS_RIVER)
                    && !biome.is(BiomeTags.IS_MOUNTAIN);
            default -> !biome.is(BiomeTags.IS_OCEAN) && !biome.is(BiomeTags.IS_RIVER);
        };
    }

    @Override
    public StructureType<?> type() {
        return ModStructureTypes.NBT_POI.get();
    }
}
