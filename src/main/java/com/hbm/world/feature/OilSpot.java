package com.hbm.world.feature;

import com.hbm.blocks.PlantEnums.EnumDeadPlantType;
import com.hbm.blocks.generic.PlantBlocks;
import com.hbm.main.MainRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Ported from CE's {@code com.hbm.world.feature.OilSpot} (118 lines, read in full) - the fracking
 * tower's surface-decoration side effect, called from {@code onSuck} on every successful fracking
 * extraction. Per {@code docs/phase2/oil_production_chain.md}'s explicit Phase-2-safe-scope finding,
 * this is <b>not</b> the deferred world-gen boundary: it's a plain per-block
 * {@code Level#setBlock(BlockPos, BlockState, int)} loop scanning {@code count} randomly-offset
 * columns within Gaussian {@code width} of a center point, staining up to 4 blocks below the surface -
 * "nothing chunk/section-API-shaped to redesign here", confirmed by direct CE source reading, and
 * fully in this task's stated scope ("fracking's block-manipulation mechanic is fully in-scope").
 *
 * <h2>Deliberate simplifications vs. CE</h2>
 * <ul>
 *   <li><b>Plant-killing collapses to one variant.</b> CE distinguishes FERN/GRASS/FLOWER/BIG_FLOWER
 *   dead-plant replacements by exact vanilla plant class ({@code BlockTallGrass}/{@code BlockFlower}/
 *   {@code BlockDoublePlant}/{@code IPlantable}, none of which exist under those names in modern
 *   Minecraft). This port instead checks {@link BushBlock} (the real, confirmed vanilla 1.21.1
 *   superclass already used by this port's own {@code BlockDeadPlant}/{@code WasteGrassTall} for the
 *   same "small standalone plant" category) and always replaces with
 *   {@link EnumDeadPlantType#GENERIC} ({@code plant_dead_generic}, already registered by
 *   {@code PlantBlocks}) - a cosmetic-only precision loss, not a mechanic change.</li>
 *   <li><b>{@code addWillows} is accepted but not implemented</b> - CE's own only call site
 *   ({@code TileEntityMachineFrackingTower.onSuck}) always passes {@code false}, so the willow-planting
 *   branch is dead code for every in-repo caller; left as a documented no-op rather than porting
 *   CE's {@code MUSTARD_WILLOW_0}/{@code EnumFlowerPlantType} placement logic for a branch nothing
 *   reaches.</li>
 *   <li><b>{@code ore_oil_sand}/{@code sand_dirty}/{@code sand_dirty_red}/{@code stone_cracked}</b>
 *   (CE's sand/stone staining targets, confirmed absent from this port per the research report's
 *   Deferred scope #7) are registered by this same pass's
 *   {@link com.hbm.blocks.machine.OilChainBlocks} as plain decorative blocks - cheap, no special
 *   behavior needed beyond what {@code dirt_oily}/{@code dirt_dead} already do.</li>
 *   <li>CE's {@code BlockPlantEnumMeta} willow-stem protection check is dropped along with
 *   {@code addWillows} (nothing plants willows here for it to protect).</li>
 *   <li><b>World-gen callers must pass {@link WorldGenLevel}, not {@link Level}.</b> CE's
 *   {@code world.getHeight(rX, rZ)} on 1.12 {@code World} is fine; Neo 1.21
 *   {@link Level#getHeight} loads neighboring chunks via {@code ServerChunkCache.getChunk} and
 *   deadlocks when invoked from {@code Feature#place} (OilBubble / BedrockOil). The
 *   {@link LevelAccessor} overload skips columns whose chunk is not already present and uses
 *   {@link Heightmap.Types#WORLD_SURFACE_WG} on a {@link WorldGenLevel}.</li>
 * </ul>
 */
public final class OilSpot {

    private static Block sandDirtyCache;
    private static Block sandDirtyRedCache;
    private static Block stoneCrackedCache;
    private static Block oreOilSandCache;

    private OilSpot() {
    }

    private static Block resolve(String path) {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, path));
    }

    private static Block sandDirty() {
        if (sandDirtyCache == null) sandDirtyCache = resolve("sand_dirty");
        return sandDirtyCache;
    }

    private static Block sandDirtyRed() {
        if (sandDirtyRedCache == null) sandDirtyRedCache = resolve("sand_dirty_red");
        return sandDirtyRedCache;
    }

    private static Block stoneCracked() {
        if (stoneCrackedCache == null) stoneCrackedCache = resolve("stone_cracked");
        return stoneCrackedCache;
    }

    private static Block oreOilSand() {
        if (oreOilSandCache == null) oreOilSandCache = resolve("ore_oil_sand");
        return oreOilSandCache;
    }

    /**
     * Live-world overload (fracking tower). {@link Level#getHeight} is safe here - the chunk
     * is already loaded.
     *
     * @param addWillows accepted for CE API-signature parity, not implemented (see class javadoc) -
     *                   every in-repo caller passes {@code false}.
     */
    public static void generateOilSpot(Level level, int x, int z, int width, int count, boolean addWillows) {
        generateOilSpot((LevelAccessor) level, x, z, width, count, addWillows);
    }

    /**
     * World-gen-safe entry. Feature callers must pass the {@link WorldGenLevel} from
     * {@code Feature#place}, not {@code WorldGenLevel#getLevel()} / {@code ServerLevel}.
     */
    public static void generateOilSpot(LevelAccessor level, int x, int z, int width, int count, boolean addWillows) {
        if (level == null) return;
        if (level instanceof Level live && live.isClientSide) return;

        RandomSource random = level.getRandom();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int minY = level.getMinBuildHeight();
        Heightmap.Types heightmap = level instanceof WorldGenLevel
                ? Heightmap.Types.WORLD_SURFACE_WG
                : Heightmap.Types.WORLD_SURFACE;

        for (int i = 0; i < count; i++) {
            int rX = x + (int) (random.nextGaussian() * width);
            int rZ = z + (int) (random.nextGaussian() * width);
            if (!level.hasChunk(rX >> 4, rZ >> 4)) continue;
            int rY = level.getHeight(heightmap, rX, rZ);

            for (int y = rY; y > rY - 4 && y > minY; y--) {
                pos.set(rX, y - 1, rZ);
                BlockState belowState = level.getBlockState(pos);

                pos.set(rX, y, rZ);
                BlockState groundState = level.getBlockState(pos);
                Block ground = groundState.getBlock();

                if (ground instanceof BushBlock && belowState.isFaceSturdy(level, pos.below(), Direction.UP)) {
                    if (random.nextInt(10) == 0) {
                        level.setBlock(pos, PlantBlocks.deadPlant(EnumDeadPlantType.GENERIC).defaultBlockState(), 2 | 16);
                    } else {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2 | 16);
                    }
                }

                if (ground == Blocks.GRASS_BLOCK || ground == Blocks.DIRT) {
                    BlockState dirtState = random.nextInt(10) == 0
                            ? PlantBlocks.DIRT_OILY.get().defaultBlockState()
                            : PlantBlocks.DIRT_DEAD.get().defaultBlockState();
                    level.setBlock(pos, dirtState, 2 | 16);
                    break;
                } else if (ground == Blocks.RED_SAND) {
                    level.setBlock(pos, sandDirtyRed().defaultBlockState(), 2 | 16);
                    break;
                } else if (ground == Blocks.SAND || ground == oreOilSand()) {
                    level.setBlock(pos, sandDirty().defaultBlockState(), 2 | 16);
                    break;
                } else if (ground == Blocks.STONE) {
                    level.setBlock(pos, stoneCracked().defaultBlockState(), 2 | 16);
                    break;
                } else if (groundState.is(BlockTags.LEAVES)) {
                    // Th3_Sl1ze (CE): flag 3 may cause cascading lag, but otherwise snow layers are
                    // left floating - kept unchanged from CE's own comment/flag choice.
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    break;
                }
            }
        }
    }
}
