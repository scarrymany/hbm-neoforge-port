package com.hbm.world.feature;

import com.hbm.blocks.generic.BlockBedrockOreTE;
import com.hbm.config.WorldConfig;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.BilletPowderItems;
import com.hbm.items.special.BedrockOreItems;
import com.hbm.items.special.BedrockOreType;
import com.hbm.items.special.ItemBedrockOreBase;
import com.hbm.lib.Library;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import javax.annotation.Nullable;

/**
 * The bedrock-ore family, ported from CE's {@code BedrockOre} (194 lines, read in full) and
 * {@code ItemBedrockOreBase.getOreLevel} (already ported in this port at
 * {@link ItemBedrockOreBase#getOreLevel}, reused here directly rather than re-implemented). Bedrock
 * ore is placed <b>only</b> at/adjacent to {@code y=0}, never as an ordinary underground vein:
 * {@code executeOriginalLogic} scans a 3x3 XZ patch at {@code y=0} - the center column always
 * converts if it's {@code Blocks.BEDROCK}, the 8 neighbors each get an independent 50/50 chance -
 * every converted position becomes {@code ore_bedrock_block} with a fresh
 * {@code BedrockOreBlockEntity}, then a {@code 7x7x6} column above ({@code y in [1,7)}) gets swept
 * into a tier-specific "depth rock" cosmetic shell.
 * <p>
 * {@code nether == false} (the overworld/tiered variant) reproduces CE's real, unconditional
 * placement site exactly: CE's {@code WorldConfig.newBedrockOres} + {@code rand.nextInt(10)==0} roll
 * in {@code HbmWorldGen.generateOres} is <b>not</b> nested inside any {@code dimID==0} check - it
 * fires in every dimension - so this feature is registered under all three biome-tag groups except
 * the End (vanilla's End has no naturally-generated bedrock at y=0 for it to ever match, so
 * registering it there would be a pure no-op; skipped as a harmless optimization, not a behavior
 * change). {@code nether == true} (the weighted glowstone/phosphorus/quartz variant) is CE's
 * <em>additional</em>, independent {@code dimID==-1}-only roll - both variants can fire in the same
 * Nether chunk from two separate {@code rand.nextInt(10)} draws, exactly as CE's own source reads.
 * <p>
 * <b>Confirmed, deliberately preserved CE quirk</b>: the overworld tier-selection density scan
 * ({@link ItemBedrockOreBase#getOreLevel}) uses fixed literal seeds ({@code 2114043}/
 * {@code 2082127+ordinal}), not {@code world.getSeed()} - every world, regardless of seed, has
 * bedrock-ore density hotspots at the same X/Z coordinates. The Nether variant, by contrast, uses a
 * genuinely seed-dependent {@link Library#nextIntDeterministic} weighted pick. Both asymmetric
 * behaviors are real CE source, not ported bugs - see the research report's Open questions.
 */
public class BedrockOreFeature extends Feature<NoneFeatureConfiguration> {

    private final boolean nether;

    public BedrockOreFeature(Codec<NoneFeatureConfiguration> codec, boolean nether) {
        super(codec);
        this.nether = nether;
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        if (!WorldConfig.NEW_BEDROCK_ORES.get()) return false;

        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        if (random.nextInt(10) != 0) return false;

        int chunkMinX = OreShapeUtil.chunkOrigin(origin.getX());
        int chunkMinZ = OreShapeUtil.chunkOrigin(origin.getZ());
        int x = chunkMinX + random.nextInt(2) + 8;
        int z = chunkMinZ + random.nextInt(2) + 8;

        Selection selection = nether
                ? pickNetherOre(level, chunkMinX >> 4, chunkMinZ >> 4)
                : pickOverworldTier(x, z);
        if (selection == null) return false;

        return executeOriginalLogic(level, random, x, z, selection);
    }

    private static Selection pickOverworldTier(int x, int z) {
        double total = 0D;
        for (BedrockOreType type : BedrockOreType.VALUES) {
            total += ItemBedrockOreBase.getOreLevel(x, z, type);
        }
        total /= BedrockOreType.VALUES.length;

        int tier = getTier(total);
        FluidStack acid = getBoreFluid(total);
        ItemStack resource = new ItemStack(BedrockOreItems.BEDROCK_ORE_BASE.get());
        return new Selection(resource, acid, 0xD78A16, tier, "stone_depth");
    }

    private static int getTier(double density) {
        if (density > 1.5) return 4;
        if (density > 1) return 3;
        if (density > 0.75) return 2;
        return 1;
    }

    @Nullable
    private static FluidStack getBoreFluid(double density) {
        if (density > 1.5) return new FluidStack(Fluids.SOLVENT, 2_000);
        if (density > 1) return new FluidStack(Fluids.SULFURIC_ACID, 1_000);
        if (density > 0.75) return new FluidStack(Fluids.WATER, 1_000);
        return null;
    }

    private static Selection pickNetherOre(WorldGenLevel level, int chunkX, int chunkZ) {
        int wGlow = WorldConfig.BEDROCK_GLOWSTONE_SPAWN.get();
        int wFire = WorldConfig.BEDROCK_PHOSPHORUS_SPAWN.get();
        int wQuartz = WorldConfig.BEDROCK_QUARTZ_SPAWN.get();
        int total = wGlow + wFire + wQuartz;
        if (total <= 0) return null;

        long seed = OreShapeUtil.seed(level);
        int r = Library.nextIntDeterministic(seed, chunkX, chunkZ, total);
        if (r < wGlow) {
            return new Selection(new ItemStack(Items.GLOWSTONE_DUST, 4), null, 0xF9FF4D, 1, "stone_depth_nether");
        }
        r -= wGlow;
        if (r < wFire) {
            return new Selection(new ItemStack(BilletPowderItems.POWDER_FIRE.get(), 4), null, 0xD7341F, 1, "stone_depth_nether");
        }
        return new Selection(new ItemStack(Items.QUARTZ, 4), null, 0xF0EFDD, 1, "stone_depth_nether");
    }

    /** Ported from CE's {@code BedrockOre.executeOriginalLogic}. */
    private static boolean executeOriginalLogic(WorldGenLevel level, RandomSource random, int x, int z, Selection selection) {
        Block oreBlock = OreShapeUtil.block("ore_bedrock_block");
        Block filler = OreShapeUtil.block(selection.depthRockName());
        if (oreBlock == null || filler == null) return false;
        BlockState oreState = oreBlock.defaultBlockState();
        BlockState fillerState = filler.defaultBlockState();

        boolean placedAny = false;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int ix = x - 1; ix <= x + 1; ix++) {
            for (int iz = z - 1; iz <= z + 1; iz++) {
                pos.set(ix, 0, iz);
                if (!level.getBlockState(pos).is(Blocks.BEDROCK)) continue;
                if (!(ix == x && iz == z) && !random.nextBoolean()) continue;

                level.setBlock(pos, oreState, 3);
                if (level.getBlockEntity(pos) instanceof BlockBedrockOreTE.BedrockOreBlockEntity ore) {
                    ore.resource = selection.resource().copy();
                    ore.color = selection.color();
                    ore.shape = random.nextInt(10);
                    ore.acidRequirement = selection.acid();
                    ore.tier = selection.tier();
                    ore.setChanged();
                }
                placedAny = true;
            }
        }

        for (int ix = x - 3; ix <= x + 3; ix++) {
            for (int iz = z - 3; iz <= z + 3; iz++) {
                for (int iy = 1; iy < 7; iy++) {
                    pos.set(ix, iy, iz);
                    BlockState state = level.getBlockState(pos);
                    boolean isStoneOrBedrock = state.is(Blocks.STONE) || state.is(Blocks.BEDROCK);
                    boolean replace = (iy < 3 && isStoneOrBedrock) || (iy >= 3 && state.is(Blocks.BEDROCK));
                    if (replace) {
                        level.setBlock(pos, fillerState, 2);
                    }
                }
            }
        }
        return placedAny;
    }

    private record Selection(ItemStack resource, @Nullable FluidStack acid, int color, int tier, String depthRockName) {
    }
}
