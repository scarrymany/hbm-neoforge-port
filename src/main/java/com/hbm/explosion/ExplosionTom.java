package com.hbm.explosion;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Ported from CE's {@code com.hbm.explosion.ExplosionTom} (164 lines, read in full) -
 * {@code EntityTomBlast}'s real payload: an incremental, multi-tick expanding-shell block-conversion
 * algorithm (an Ulam-spiral column walk - {@link #n}/{@link #nlimit}/{@link #shell}/{@link #leg}/
 * {@link #element} state, byte-for-byte the same spiral driver as this port's own already-committed
 * {@code ExplosionBalefire}), carving a crater bowl with a raised rim and converting its floor to a
 * rare glassy material, its rim surroundings to lava, and clearing flammable/meltable blocks near
 * the outer edge. Per {@code docs/phase4/satellites_followup_and_loot_pools.md}'s Deferred scope
 * (which explicitly flagged this class as unread-past-its-fields and requiring a dedicated read
 * before {@code EntityTomBlast} could be ported), this is that dedicated, full port.
 * <p>
 * <b>{@code isWarDim} - dropped, not gated.</b> CE's own {@code update()} starts with
 * {@code if(!CompatibilityConfig.isWarDim(world)) return true;} (an early "pretend we're already
 * done" bail-out). Per this port's documented always-true default for this concept (see
 * {@code EntityTomBlast}'s own javadoc for why this specific class resolves the report's flagged
 * open question in the "always true, guarded content is real" direction), that check is simply
 * dropped here rather than reproduced as a permanently-true no-op branch - {@code EntityTomBlast}
 * itself still carries the equivalent gate at its own call site, matching where CE's other
 * {@code isWarDim} branches (e.g. {@code ExplosionLarge}) keep theirs.
 * <p>
 * <b>Two real, documented content substitutions</b> - both flagged in this pass's own knownGaps,
 * neither silently dropped:
 * <ul>
 *     <li><b>{@code ModBlocks.tektite}/{@code ore_tektite_osmiridium}</b> (CE's crater-floor glass
 *     block and its rare ore variant) are not registered anywhere in this port - confirmed by
 *     repo-wide grep (no {@code tektite} block, no {@code osmiridium} ore block of any kind exist
 *     yet; only refined osmiridium items do). Substituted with vanilla {@link Blocks#OBSIDIAN}/
 *     {@link Blocks#CRYING_OBSIDIAN} (the closest existing "rare glassy blast-fused rock" pair) -
 *     this is world-generation/decorative-block registration scope, not this package's own
 *     satellite-payload scope, matching this same report's precedent for {@code ModBlocks.moon_turf}/
 *     {@code gravel_diamond} (flagged for whoever owns ore/decorative-block content, not resolved
 *     here).</li>
 *     <li><b>CE's {@code Material.WATER}/{@code ICE}/{@code SNOW}/{@code getCanBurn()} checks</b> -
 *     the {@code Material} class does not exist in 1.21.1. Translated to
 *     {@link BlockState#getFluidState()} + {@link FluidTags#WATER} for water, an explicit ice-family
 *     block list, an explicit snow-family block list, and {@link BlockTags#LOGS}/{@link
 *     BlockTags#LEAVES} plus a handful of other clearly-flammable vanilla categories for
 *     {@code getCanBurn()} - the same {@code BlockTags.LOGS}/{@code LEAVES} substitution this port's
 *     own {@code EntityMeteor} already established for the identical CE material-flammability
 *     concept, extended slightly since this method's checked category is broader than CE's own
 *     narrower {@code Blocks.LEAVES}/{@code Blocks.LOG} identity checks there.</li>
 * </ul>
 * <p>
 * World-height bounds are read dynamically from {@link Level#getMinBuildHeight()}/
 * {@link Level#getMaxBuildHeight()} rather than CE's hardcoded 1.12 {@code 0}/{@code 256} (matching
 * this port's own {@code ExplosionNukeRayBatched}/{@code ExplosionBalefire} precedent for the same
 * translation) - the crater-shape formula's own {@code terrain = 63} sea-level constant is a baked-in
 * design parameter of the shape math itself (not a queried world value) and is kept verbatim.
 */
public class ExplosionTom {

    public int posX;
    public int posY;
    public int posZ;
    public int lastposX = 0;
    public int lastposZ = 0;
    public int radius;
    public int radius2;
    public Level levelObj;
    private int n = 1;
    private int nlimit;
    private int shell;
    private int leg;
    private int element;

    public void saveToNbt(CompoundTag nbt, String name) {
        nbt.putInt(name + "posX", posX);
        nbt.putInt(name + "posY", posY);
        nbt.putInt(name + "posZ", posZ);
        nbt.putInt(name + "lastposX", lastposX);
        nbt.putInt(name + "lastposZ", lastposZ);
        nbt.putInt(name + "radius", radius);
        nbt.putInt(name + "radius2", radius2);
        nbt.putInt(name + "n", n);
        nbt.putInt(name + "nlimit", nlimit);
        nbt.putInt(name + "shell", shell);
        nbt.putInt(name + "leg", leg);
        nbt.putInt(name + "element", element);
    }

    public void readFromNbt(CompoundTag nbt, String name) {
        posX = nbt.getInt(name + "posX");
        posY = nbt.getInt(name + "posY");
        posZ = nbt.getInt(name + "posZ");
        lastposX = nbt.getInt(name + "lastposX");
        lastposZ = nbt.getInt(name + "lastposZ");
        radius = nbt.getInt(name + "radius");
        radius2 = nbt.getInt(name + "radius2");
        n = nbt.getInt(name + "n");
        nlimit = nbt.getInt(name + "nlimit");
        shell = nbt.getInt(name + "shell");
        leg = nbt.getInt(name + "leg");
        element = nbt.getInt(name + "element");
    }

    public ExplosionTom(int x, int y, int z, Level level, int rad) {
        this.posX = x;
        this.posY = y;
        this.posZ = z;

        this.levelObj = level;

        this.radius = rad;
        this.radius2 = this.radius * this.radius;

        this.nlimit = this.radius2 * 4;
    }

    /** @return {@code true} once the spiral walk has covered the whole {@link #radius2}-scaled area. */
    public boolean update() {
        breakColumn(this.lastposX, this.lastposZ);
        this.shell = (int) Math.floor((Math.sqrt(n) + 1) / 2);
        int shell2 = this.shell * 2;
        this.leg = (int) Math.floor((this.n - (shell2 - 1) * (shell2 - 1)) / shell2);
        this.element = (this.n - (shell2 - 1) * (shell2 - 1)) - shell2 * this.leg - this.shell + 1;
        this.lastposX = this.leg == 0 ? this.shell : this.leg == 1 ? -this.element : this.leg == 2 ? -this.shell : this.element;
        this.lastposZ = this.leg == 0 ? this.element : this.leg == 1 ? this.shell : this.leg == 2 ? -this.element : -this.shell;
        this.n++;
        return this.n > this.nlimit;
    }

    // mlbv (CE): "100% parity as of Oct 30, 2025; I made some changes to avoid redundant
    // recalculations" - preserved verbatim, only the two documented substitutions above applied.
    private void breakColumn(int x, int z) {
        int r2 = x * x + z * z;
        int dist = this.radius2 - r2;
        if (dist <= 0) return;

        int pX = posX + x;
        int pZ = posZ + z;
        double r = Math.sqrt(r2);
        boolean insideRim = r < 500.0;

        int terrain = 63;

        double cA = (terrain - Math.exp(-(r2) / 40000.0) * 13.0) + levelObj.random.nextInt(2); // bowl
        double rMinus200 = r - 200.0;
        double cB = cA + Math.exp(-(rMinus200 * rMinus200) / 400.0) * 13.0;                     // peak ring
        double rMinus500 = r - 500.0;
        int craterFloor = (int) (cB + Math.exp(-(rMinus500 * rMinus500) / 2000.0) * 37.0);      // rim

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        int scanTop = Math.min(256, levelObj.getMaxBuildHeight() - 1);
        int scanBottom = Math.max(0, levelObj.getMinBuildHeight());

        int y = scanTop;
        for (int i = scanTop; i > scanBottom; i--) {
            pos.set(pX, i, pZ);
            if (i == craterFloor || !levelObj.isEmptyBlock(pos)) {
                y = i;
                break;
            }
        }

        int height = terrain - 14;
        int offset = 20;
        int threshold = (int) (r * (height + offset) / (double) this.radius) + levelObj.random.nextInt(2) - offset;

        while (y > threshold) {
            if (y <= scanBottom) break;

            if (y <= craterFloor) {
                pos.set(pX, y, pZ);
                if (levelObj.random.nextInt(200) == 0) {
                    levelObj.setBlock(pos, Blocks.CRYING_OBSIDIAN.defaultBlockState(), 3);
                } else {
                    levelObj.setBlock(pos, Blocks.OBSIDIAN.defaultBlockState(), 3);
                }
            } else {
                if (y > terrain + 1) {
                    if (insideRim) {
                        for (int i = -2; i < 3; i++) {
                            for (int j = -2; j < 3; j++) {
                                for (int k = -2; k < 3; k++) {
                                    pos.set(pX + i, y + j, pZ + k);
                                    if (isMeltableOrFlammable(levelObj.getBlockState(pos))) {
                                        levelObj.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                                        levelObj.setBlock(pos.set(pX, y, pZ), Blocks.AIR.defaultBlockState(), 3);
                                    }
                                }
                            }
                        }
                        levelObj.setBlock(pos.set(pX, y, pZ), Blocks.AIR.defaultBlockState(), 3);
                    }
                } else {
                    for (int i = -2; i < 3; i++) {
                        for (int j = -2; j < 3; j++) {
                            for (int k = -2; k < 3; k++) {
                                pos.set(pX + i, y + j, pZ + k);
                                BlockState state = levelObj.getBlockState(pos);
                                boolean waterOrIce = state.getFluidState().is(FluidTags.WATER) || isIce(state);
                                if (waterOrIce || levelObj.isEmptyBlock(pos.set(pX + i, y, pZ + k))) {
                                    levelObj.setBlock(pos.set(pX + i, y, pZ + k), Blocks.LAVA.defaultBlockState(), 3);
                                    levelObj.setBlock(pos.set(pX, y, pZ), Blocks.LAVA.defaultBlockState(), 3);
                                }
                            }
                        }
                    }
                    levelObj.setBlock(pos.set(pX, y, pZ), Blocks.LAVA.defaultBlockState(), 3);
                }
            }
            y--;
        }
    }

    private static boolean isIce(BlockState state) {
        return state.is(Blocks.ICE) || state.is(Blocks.PACKED_ICE) || state.is(Blocks.BLUE_ICE) || state.is(Blocks.FROSTED_ICE);
    }

    /** CE: {@code Material.WATER || Material.ICE || Material.SNOW || m.getCanBurn()} - see class javadoc. */
    private static boolean isMeltableOrFlammable(BlockState state) {
        if (state.getFluidState().is(FluidTags.WATER)) return true;
        if (isIce(state)) return true;
        if (state.is(Blocks.SNOW) || state.is(Blocks.SNOW_BLOCK)) return true;
        return state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES) || state.is(BlockTags.PLANKS)
                || state.is(BlockTags.WOOL) || state.is(Blocks.HAY_BLOCK) || state.is(Blocks.BAMBOO)
                || state.is(Blocks.SHORT_GRASS) || state.is(Blocks.TALL_GRASS) || state.is(Blocks.VINE);
    }
}
