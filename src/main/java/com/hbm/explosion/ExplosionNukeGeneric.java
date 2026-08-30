package com.hbm.explosion;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.damage.ModDamageTypes;
import com.hbm.main.MainRegistry;
import com.hbm.util.Compat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Ported from CE's {@code com.hbm.explosion.ExplosionNukeGeneric} (563 lines, read in full) - the
 * grab-bag of static block-mutation/AoE-damage/EMP helpers shared by the mk3 column-carving family
 * and the mk5 ray family's own AoE damage tick.
 * <p>
 * <b>Scope actually ported</b> (see {@code docs/phase3/explosion_engine.md}): {@link #dealDamage}
 * (needed directly by {@code EntityNukeExplosionMK5}/{@code MK3}/{@code Balefire}'s per-tick AoE
 * damage), {@link #empBlast}/{@link #emp} (needed by the EMP-bomb family), and {@link #destruction}/
 * {@link #vaporDest}/{@link #waste}/{@link #wasteDest}/{@link #solinium} (needed by {@link
 * ExplosionNukeAdvanced}'s 3 modes and {@link ExplosionSolinium}).
 * <p>
 * <b>Deliberately reduced, not invented</b>: roughly 20 of CE's block-conversion branches across
 * {@code destruction}/{@code vaporDest}/{@code wasteDest}/{@code solinium} reference specific
 * decorative/waste/ore {@code ModBlocks} fields (e.g. {@code waste_earth}, {@code
 * ore_schrabidium}, {@code block_scrap}, {@code sellafield}) that are not registered anywhere in
 * this port yet (confirmed by grep against the committed {@code ModBlocks.java} - Phase 1/4
 * content, not this pass's to add). Each such branch is a real, separately-tracked forward
 * reference, marked with a {@code TODO(ModBlocks.<field>)} comment at its exact original call
 * site, and is skipped (block left untouched) rather than guessed at with an invented substitute
 * block - the vanilla-only conversions in each method (doors, clay, mossy cobblestone → coal ore,
 * coal ore → diamond/emerald ore, grass/mycelium → dirt, obsidian/liquid/weak-block removal, ...)
 * are fully ported and functional. {@code succ} (a black-hole pull effect with no consumer among
 * this pass's required entities) and the {@code wasteNoSchrab}/{@code wasteDestNoSchrab} pair (used
 * only by content outside this pass's scope) are not ported - see this pass's structured output.
 * <p>
 * CE's {@code dealDamage} routes living targets through {@code EntityDamageUtil.attackEntityFromNT}
 * (an armor-piercing/pierce-DT-aware attack helper) and, on kill, {@code ConfettiUtil.decideConfetti}
 * (a gib-VFX helper) - neither exists in this port. Both are replaced with plain vanilla {@code
 * LivingEntity#hurt}, a documented simplification (drops armor-piercing nuance and the kill-gib
 * effect), not a silent behavior change - see {@code docs/phase3/explosion_engine.md}'s "Open
 * questions". CE's {@code Library.isObstructed} line-of-sight gate has no ported equivalent either
 * and is treated as always-unobstructed (documented gap: blast damage currently applies through
 * intervening walls).
 */
public final class ExplosionNukeGeneric {

    private ExplosionNukeGeneric() {
    }

    private static final Random RANDOM = new Random();

    public static Map<Block, Block> soliniumConfig = new HashMap<>();

    // --- EMP -----------------------------------------------------------------------------------

    public static void empBlast(Level level, Entity detonator, int x, int y, int z, int bombStartStrength) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int r = bombStartStrength;
        int r2 = r * r;
        int r22 = r2 / 2;
        for (int xx = -r; xx < r; xx++) {
            int X = xx + x;
            int XX = xx * xx;
            for (int yy = -r; yy < r; yy++) {
                int Y = yy + y;
                int YY = XX + yy * yy;
                for (int zz = -r; zz < r; zz++) {
                    int Z = zz + z;
                    int ZZ = YY + zz * zz;
                    if (ZZ < r22) {
                        pos.set(X, Y, Z);
                        emp(level, pos);
                    }
                }
            }
        }
    }

    public static void emp(Level level, BlockPos pos) {
        if (level.isClientSide()) return;

        BlockEntity be = Compat.getBlockEntityStandard(level, pos);
        if (be == null) return;

        if (be instanceof IEnergyReceiverMK2 receiver) {
            receiver.setPower(0);
            if (RANDOM.nextInt(5) < 1) {
                // TODO(ModBlocks.block_electrical_scrap): CE has a chance to leave scrap behind here; not registered yet.
            }
            return;
        }

        IEnergyStorage handle = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, null);
        if (handle != null) {
            handle.extractEnergy(handle.getEnergyStored(), false);
            if (RANDOM.nextInt(5) <= 1) {
                // TODO(ModBlocks.block_electrical_scrap): see above.
            }
        }

        // CE's third branch (CoFH RedstoneFlux's IEnergyProvider) has no NeoForge-ecosystem
        // equivalent mod to target - dropped, not translated (per docs/phase3/explosion_engine.md).
    }

    // --- AoE damage ------------------------------------------------------------------------------

    public static void dealDamage(Level level, List<Entity> list, double x, double y, double z, double radius) {
        dealDamage(level, list, x, y, z, radius, 250F);
    }

    /** CE's "deprecated, use the version above" convenience overload - builds its own entity list via a cube AABB (matching {@code WorldUtil.getEntitiesInRadius}'s own shape, since that helper doesn't exist in this port). */
    public static void dealDamage(Level level, double x, double y, double z, double radius) {
        dealDamage(level, entitiesInRadius(level, x, y, z, radius), x, y, z, radius, 250F);
    }

    private static List<Entity> entitiesInRadius(Level level, double x, double y, double z, double radius) {
        return level.getEntitiesOfClass(Entity.class,
                new net.minecraft.world.phys.AABB(x - radius, y - radius, z - radius, x + radius, y + radius, z + radius));
    }

    public static void dealDamage(Level level, List<Entity> list, double x, double y, double z, double radius, float maxDamage) {
        for (Entity e : list) {
            double dist = Math.sqrt(e.distanceToSqr(x, y, z));

            if (dist <= radius && !isExplosionExempt(e)) {
                // TODO(Library.isObstructed): CE gates this on a line-of-sight raycast against
                // intervening blocks; that helper doesn't exist in this port yet - treated as
                // always-unobstructed (documented gap, not an invented fix).

                double damage = maxDamage * (radius - dist) / radius;

                if (e instanceof LivingEntity living && living.isAlive()) {
                    // TODO(EntityDamageUtil.attackEntityFromNT, ConfettiUtil): see class javadoc -
                    // substituted with plain vanilla damage application.
                    living.hurt(level.damageSources().source(ModDamageTypes.NUCLEAR_BLAST), (float) damage);
                } else {
                    e.hurt(level.damageSources().source(ModDamageTypes.NUCLEAR_BLAST), (float) damage);
                }

                e.igniteForSeconds(5);

                Vec3 knock = new Vec3(e.getX() - x, e.getY() + e.getEyeHeight() - y, e.getZ() - z);
                if (knock.lengthSqr() > 1.0E-8D) {
                    knock = knock.normalize();
                    e.setDeltaMovement(e.getDeltaMovement().add(knock.x * 0.2D, knock.y * 0.2D, knock.z * 0.2D));
                    e.hasImpulse = true;
                }
            }
        }
    }

    private static boolean isExplosionExempt(Entity e) {
        // CE also exempts EntityExplosiveBeam/EntityBulletBaseNT/EntityBulletBaseMK4/
        // EntityGrenadeUniversal here - none of those projectile/grenade entity classes exist in
        // this port yet (separate weapons packages' scope); only the ocelot exemption (needs no
        // missing dependency) is ported for now.
        return e instanceof Ocelot;
    }

    // --- column-family block-mutation helpers -----------------------------------------------------

    /** Crater-mode block removal. @return a "protection" score {@code breakColumn} uses to decide how many more Y layers to spare below the epicenter. */
    public static int destruction(Level level, BlockPos pos) {
        if (level.isClientSide()) return 0;
        // TODO(CompatDynamicTrees): CE's tree-destruction compat hook is not ported (no consumer mod present).

        BlockState b = level.getBlockState(pos);
        float resistance = b.getBlock().getExplosionResistance();

        if (resistance >= 200f) {
            // TODO(ModBlocks.brick_concrete/brick_light/brick_obsidian/gravel_obsidian/
            // waste_planks/block_scrap): CE has a chance to convert 3 custom decorative blocks and
            // obsidian into gravel/scrap/waste variants here; none of those ModBlocks fields exist
            // in this port yet - the block is left untouched (spared, matching CE's own "most of
            // the time nothing happens" odds) rather than guessed at.
            return (int) (resistance / 300f);
        } else {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }
        return 0;
    }

    /** Vapor-mode block removal - more aggressive than {@link #destruction}, sparing only genuinely tough blocks. */
    public static int vaporDest(Level level, BlockPos pos) {
        if (level.isClientSide()) return 0;
        // TODO(CompatDynamicTrees): see destruction().

        BlockState b = level.getBlockState(pos);
        float resistance = b.getBlock().getExplosionResistance();

        if (resistance < 0.5f || b.is(Blocks.COBWEB) || !b.getFluidState().isEmpty()) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            return 0;
        } else if (resistance <= 3.0f && !b.canOcclude()) {
            if (!b.is(Blocks.CHEST) && !b.is(Blocks.FARMLAND)) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                return 0;
            }
        }
        // TODO(ModBlocks.red_cable): CE also insta-clears its own red_cable block here regardless
        // of resistance; not registered in this port yet.
        // TODO(flammability ignition): CE ignites a flammable block's empty top neighbor here
        // (Block#isFlammable); no simple 1.21.1 equivalent single-call replacement was confirmed -
        // dropped rather than guessed at (minor cosmetic branch, not core destruction logic).

        return (int) (resistance / 300f);
    }

    public static void waste(Level level, int x, int y, int z, int radius) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int r = radius;
        int r2 = r * r;
        int r22 = r2 / 2;
        // mlbv (CE): also used at very low radius (HazardTypeContaminating) - guard against a 0 bound.
        int bound = r22 / 5;
        if (bound == 0) return;
        for (int xx = -r; xx < r; xx++) {
            int X = xx + x;
            int XX = xx * xx;
            for (int yy = -r; yy < r; yy++) {
                int Y = yy + y;
                int YY = XX + yy * yy;
                for (int zz = -r; zz < r; zz++) {
                    int Z = zz + z;
                    int ZZ = YY + zz * zz;
                    if (ZZ < r22 + RANDOM.nextInt(bound)) {
                        pos.set(X, Y, Z);
                        if (!level.getBlockState(pos).is(Blocks.AIR)) wasteDest(level, pos);
                    }
                }
            }
        }
    }

    public static void wasteDest(Level level, BlockPos pos) {
        if (level.isClientSide()) return;

        BlockState bs = level.getBlockState(pos);
        Block b = bs.getBlock();

        if (b == Blocks.AIR) {
            return;
        } else if (b instanceof DoorBlock) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
        } else if (b == Blocks.CLAY) {
            level.setBlock(pos, Blocks.TERRACOTTA.defaultBlockState(), 3);
        } else if (b == Blocks.MOSSY_COBBLESTONE) {
            level.setBlock(pos, Blocks.COAL_ORE.defaultBlockState(), 3);
        } else if (b == Blocks.COAL_ORE) {
            int rand = RANDOM.nextInt(10);
            if (rand == 1 || rand == 2 || rand == 3) {
                level.setBlock(pos, Blocks.DIAMOND_ORE.defaultBlockState(), 3);
            } else if (rand == 9) {
                level.setBlock(pos, Blocks.EMERALD_ORE.defaultBlockState(), 3);
            }
        }
        // TODO(ModBlocks.waste_earth/waste_mycelium/waste_trinitite(_red)/waste_planks/waste_log,
        // ModBlocks.ore_schrabidium/ore_uranium_scorched/ore_nether_{uranium,schrabidium}*/
        // ore_gneiss_{uranium,schrabidium}*, VersatileConfig.getSchrabOreChance()): CE converts
        // grass/mycelium/sand/logs/planks/mushroom blocks/wood-material blocks and 3 uranium-ore
        // variants into radioactive-terrain equivalents here - ~15 branches, none of whose target
        // ModBlocks fields are registered in this port yet (Phase 1/4 content). Each is a real,
        // separately-scoped forward reference; left untouched rather than guessed at.
    }

    // --- solinium --------------------------------------------------------------------------------

    /**
     * Data-driven block-swap table loaded from {@code solinium.cfg} - fully portable regardless of
     * which specific blocks are registered, since it resolves block ids dynamically at runtime via
     * the block registry rather than referencing {@code ModBlocks} fields directly.
     */
    public static void loadSoliniumFromFile() {
        File config = new File(MainRegistry.configHbmDir, "solinium.cfg");
        if (!config.exists()) {
            try {
                //noinspection ResultOfMethodCallIgnored
                config.getParentFile().mkdirs();
                try (FileWriter write = new FileWriter(config)) {
                    write.write("""
                            # Format: modid:blockName|modid:blockName
                            # Left blocks are transformed to right, one per line
                            """);
                }
            } catch (IOException e) {
                MainRegistry.logger.error("ERROR: Could not create config file: {}", config.getAbsolutePath(), e);
                return;
            }
        }

        try (BufferedReader read = new BufferedReader(new FileReader(config))) {
            String currentLine;
            while ((currentLine = read.readLine()) != null) {
                if (currentLine.startsWith("#") || currentLine.isEmpty()) continue;
                String[] blocks = currentLine.trim().split("\\|");
                if (blocks.length != 2) continue;
                String[] modidBlock1 = blocks[0].split(":");
                String[] modidBlock2 = blocks[1].split(":");
                if (modidBlock1.length != 2 || modidBlock2.length != 2) continue;
                Block b1 = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(modidBlock1[0], modidBlock1[1]));
                Block b2 = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(modidBlock2[0], modidBlock2[1]));
                soliniumConfig.put(b1, b2);
            }
        } catch (IOException e) {
            MainRegistry.logger.error("Error reading solinium config!", e);
        }
    }

    /**
     * Vanilla-material-group checks (cactus/coral/leaves/plants/sponge/vine/gourd/wood → air) are
     * an approximation of CE's 1.12 {@code Material}-based check - that class doesn't exist in
     * modern Minecraft, which replaced it with per-block tags/classes. Close enough to preserve
     * "soft plant-like terrain vaporizes" intent, not asserted as 1:1 identical to CE's exact set.
     */
    public static void solinium(Level level, BlockPos pos) {
        if (level.isClientSide()) return;

        BlockState b = level.getBlockState(pos);
        Block block = b.getBlock();

        if (soliniumConfig.containsKey(block)) {
            Block target = soliniumConfig.get(block);
            if (target != null) level.setBlock(pos, target.defaultBlockState(), 3);
            return;
        }

        if (block == Blocks.GRASS_BLOCK || block == Blocks.MYCELIUM) {
            level.setBlock(pos, (RANDOM.nextInt(5) < 2 ? Blocks.COARSE_DIRT : Blocks.DIRT).defaultBlockState(), 3);
            return;
        }

        // TODO(ModBlocks.sellafield/sellafield_slaked/waste_earth/waste_mycelium/waste_trinitite(_red)/
        // taint/stone_gneiss): CE also converts these HBM-specific blocks here; none registered yet.

        // TODO(coral tags): CE's Material.CORAL branch is not reproduced here - this port did not
        // confirm a single reliable "any coral block" tag name in 1.21.1 Mojang mappings and would
        // rather omit the check than guess a tag id that fails to resolve at datapack-load time.
        if (block instanceof net.minecraft.world.level.block.BushBlock
                || block == Blocks.CACTUS || block == Blocks.SPONGE || block == Blocks.WET_SPONGE
                || block == Blocks.VINE || block == Blocks.PUMPKIN || block == Blocks.MELON
                || block == Blocks.CARVED_PUMPKIN || block == Blocks.JACK_O_LANTERN
                || b.is(net.minecraft.tags.BlockTags.LEAVES)) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }
    }
}
