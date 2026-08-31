package com.hbm.world.feature;

import com.hbm.blocks.generic.DecoPoleSatelliteReceiver;
import com.hbm.blocks.generic.DecoSteelPoles;
import com.hbm.blocks.generic.DecoTapeRecorder;
import com.hbm.config.CompatibilityConfig;
import com.hbm.itempool.ItemPool;
import com.hbm.itempool.ItemPoolsLegacy;
import com.hbm.main.MainRegistry;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Port of CE's {@code com.hbm.world.Antenna} (238 lines, read in full) — the 3×3×21 steel radio
 * mast with one {@code POOL_ANTENNA} chest. CE places this via {@code AbstractPhasedStructure} +
 * {@code HbmWorldGen} ({@code CompatibilityConfig.antennaStructure} default {@code "0:750"}, biome
 * {@code temperature >= 0.4F && rainfall <= 0.6F}). This port keeps the literal schematic (AIR
 * placements dropped as no-ops) and the 1-in-N chunk roll, as a {@link Feature} matching
 * {@link MeteoriteFeature}'s already-shipping pipeline.
 * <p>
 * 1.21 substitutions (no {@code biome.getRainfall()}): temperature uses {@link Biome#getBaseTemperature()};
 * the rainfall ≤ 0.6 gate is approximated by excluding {@link BiomeTags#IS_JUNGLE},
 * {@link BiomeTags#IS_OCEAN}, {@link BiomeTags#IS_RIVER} (the biomes that would fail CE's rainfall
 * cap). CE {@code EnumFacing.VALUES[n]} indices are the vanilla 1.12 array
 * (0=DOWN 1=UP 2=NORTH 3=SOUTH 4=WEST 5=EAST).
 */
public class AntennaFeature extends Feature<NoneFeatureConfiguration> {

    public AntennaFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();

        ResourceKey<Level> dimension = OreShapeUtil.dimension(level);
        int rate = CompatibilityConfig.forDimension(CompatibilityConfig.antennaStructure(), dimension);
        if (rate <= 0 || random.nextInt(rate) != 0) return false;

        var biomeHolder = level.getBiome(origin);
        if (biomeHolder.is(BiomeTags.IS_OCEAN) || biomeHolder.is(BiomeTags.IS_RIVER) || biomeHolder.is(BiomeTags.IS_JUNGLE)) {
            return false;
        }
        if (biomeHolder.value().getBaseTemperature() < 0.4F) return false;

        BlockPos ground = origin.below();
        BlockState groundState = level.getBlockState(ground);
        if (groundState.isAir() || !groundState.getFluidState().isEmpty()) return false;

        int x = origin.getX();
        int y = origin.getY();
        int z = origin.getZ();

        Block poles = hbm("steel_poles");
        Block deco = hbm("deco_steel");
        Block recorder = hbm("tape_recorder");
        Block receiver = hbm("pole_satellite_receiver");
        Block top = hbm("pole_top");

        // y0 — CE Antenna.java lines 45-53
        setFacing(level, x + 1, y, z, poles, Direction.NORTH);
        setFacing(level, x, y, z + 1, poles, Direction.WEST);
        set(level, x + 1, y, z + 1, deco);
        setFacing(level, x + 2, y, z + 1, recorder, Direction.EAST);
        setFacing(level, x + 1, y, z + 2, poles, Direction.SOUTH);
        placeAntennaChest(level, new BlockPos(x + 2, y, z + 2), random);

        // y1 — CE lines 55-63
        setFacing(level, x + 1, y + 1, z, poles, Direction.NORTH);
        setFacing(level, x, y + 1, z + 1, poles, Direction.WEST);
        set(level, x + 1, y + 1, z + 1, deco);
        setFacing(level, x + 2, y + 1, z + 1, recorder, Direction.EAST);
        setFacing(level, x + 1, y + 1, z + 2, poles, Direction.SOUTH);

        // y2 platform — CE lines 66-71
        set(level, x + 1, y + 2, z, deco);
        set(level, x, y + 2, z + 1, deco);
        set(level, x + 1, y + 2, z + 1, deco);
        set(level, x + 2, y + 2, z + 1, deco);
        set(level, x + 1, y + 2, z + 2, deco);

        // y3-y12 mast — CE VALUES[4] = WEST
        for (int dy = 3; dy <= 12; dy++) {
            setFacing(level, x + 1, y + dy, z + 1, poles, Direction.WEST);
        }
        // y13 receiver SOUTH — CE line 167 VALUES[3]
        setFacing(level, x + 1, y + 13, z + 1, receiver, Direction.SOUTH);
        for (int dy = 14; dy <= 16; dy++) {
            setFacing(level, x + 1, y + dy, z + 1, poles, Direction.WEST);
        }
        setFacing(level, x + 1, y + 17, z + 1, receiver, Direction.NORTH);
        setFacing(level, x + 1, y + 18, z + 1, receiver, Direction.WEST);
        setFacing(level, x + 1, y + 19, z + 1, poles, Direction.WEST);
        set(level, x + 1, y + 20, z + 1, top);
        return true;
    }

    /** CE: 8 rolls from {@code ItemPoolsLegacy.POOL_ANTENNA} into an EAST-facing chest. */
    private static void placeAntennaChest(WorldGenLevel level, BlockPos pos, RandomSource random) {
        level.setBlock(pos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.EAST), 3);
        if (!(level.getBlockEntity(pos) instanceof RandomizableContainerBlockEntity chest)) return;
        ItemPool pool = ItemPool.getPool(ItemPoolsLegacy.POOL_ANTENNA);
        int slots = chest.getContainerSize();
        for (int i = 0; i < 8; i++) {
            ItemStack stack = ItemPool.getStack(pool, random);
            if (stack.isEmpty() || slots <= 0) continue;
            chest.setItem(random.nextInt(slots), stack);
        }
    }

    private static void set(WorldGenLevel level, int x, int y, int z, Block block) {
        if (block == null || block == Blocks.AIR) return;
        level.setBlock(new BlockPos(x, y, z), block.defaultBlockState(), 3);
    }

    private static void setFacing(WorldGenLevel level, int x, int y, int z, Block block, Direction facing) {
        if (block == null || block == Blocks.AIR) return;
        BlockState state = block.defaultBlockState();
        Property<Direction> prop = facingProperty(block);
        if (prop != null && prop.getPossibleValues().contains(facing)) {
            state = state.setValue(prop, facing);
        }
        level.setBlock(new BlockPos(x, y, z), state, 3);
    }

    private static Property<Direction> facingProperty(Block block) {
        if (block instanceof DecoSteelPoles) return DecoSteelPoles.FACING;
        if (block instanceof DecoPoleSatelliteReceiver) return DecoPoleSatelliteReceiver.FACING;
        if (block instanceof DecoTapeRecorder) return DecoTapeRecorder.FACING;
        return null;
    }

    private static Block hbm(String path) {
        return BuiltInRegistries.BLOCK.getOptional(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, path)).orElse(Blocks.AIR);
    }
}
