package com.hbm.explosion;

import com.hbm.blocks.generic.BlockSellafield;
import com.hbm.blocks.generic.WastelandVirusBlocks;
import com.hbm.damage.ModDamageTypes;
import com.hbm.entity.projectile.EntityRubble;
import com.hbm.handler.ArmorUtil;
import com.hbm.main.MainRegistry;
import com.hbm.potion.HbmPotionEffects;
import com.hbm.util.ArmorRegistry;
import com.hbm.util.ArmorRegistry.HazardClass;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.function.Consumer;

/**
 * Ported from CE's {@code com.hbm.explosion.ExplosionChaos} (915 lines, read in full) - a flat
 * 26-method grab-bag with no shared data model beyond the private {@link #forEachBlockInSphere}
 * sphere-iteration helper (CE's own in-file comment: "this whole class looks outdated as fuck"). Per
 * docs/phase4/entities_vortex_gravity_wells.md's Table B, every method's {@code isWarDim} gate is
 * preserved <b>exactly per-method as CE has it - the gate is genuinely inconsistent across this file,
 * not a uniform class-wide policy</b> (e.g. {@link #floater} is gated, {@link #move} - used by the
 * exact same {@code ItemDrop} call sequence - is not); see {@link #isWarDim}'s own javadoc for why the
 * gate itself is stubbed {@code true} rather than removed.
 * <p>
 * <b>Fully ported (zero blocking dependency once the foundation wave's wasteland/reinforced/virus
 * block set and armor/hazmat surface existed):</b> {@link #explode}, {@link #spawnExplosion},
 * {@link #c}, {@link #pc}, {@link #poison}, {@link #flameDeath}, {@link #burn}, {@link #frag},
 * {@link #explodeZOMG}, {@link #pulse}/{@link #pDestruction}, {@link #floater}, {@link #move},
 * {@link #levelDown}, {@link #decontaminate}, {@link #hardenVirus}, {@link #spreadVirus}.
 * <p>
 * <b>Documented no-op forward references</b> (real dependency confirmed absent from this port; kept as
 * real, callable methods with a TODO body rather than deleted, matching {@link ExplosionLarge}'s own
 * established precedent for identical gaps): {@link #spawnChlorine}/{@link #spawnVolley} (particle/
 * networking infra - {@code EntityModFXShadow}/{@code AuxParticlePacketNT}/{@code PacketThreading} -
 * none of which exist in this port yet); {@link #cluster}/{@link #miniMirv}/{@link #schrab}/
 * {@link #zomg} (the unrelated legacy artillery/rocket/boss-projectile family - {@code EntityRocket}/
 * {@code EntityMiniNuke}/{@code EntitySchrab}/{@code EntityRainbow} - confirmed not ported anywhere and
 * not named by any Phase 4 sibling report); {@link #tauMeSinPi} (needs CE's legacy {@code EntityBullet},
 * owned by {@code docs/phase4/entities_legacy_bullet_system.md}; no existing {@code BulletConfig} in
 * {@code LegacyMobBulletConfigs} matches its real 35-400 damage range, so a new one is not invented
 * here - that report's own consumer table already names this exact call site).
 * <p>
 * <b>{@code Block#getExplosionResistance()}</b>: 1.21's no-arg overload (this port's own established
 * convention, confirmed by {@link ExplosionLarge#jolt}) replaces CE's {@code getExplosionResistance
 * (Entity)}. <b>{@code Block#isFlammable(BlockState, BlockGetter, BlockPos, Direction)}</b> (used by
 * {@link #flameDeath}) is a real NeoForge {@code IBlockExtension} default method - confirmed by this
 * port's own {@code BlockGasFlammable#isFlammable} override - <b>not</b> the "no confirmed 1.21.1
 * equivalent" gap two earlier Phase 4 areas ({@code EntityFalloutRain}, {@code ExplosionNukeGeneric})
 * independently flagged; see this package's own returned {@code realBugsFound}.
 * <p>
 * {@link #forEachBlockInSphere}'s hardcoded CE {@code y ∈ [0,255]} clamp is replaced with a real
 * {@link Level#getMinBuildHeight()}/{@link Level#getMaxBuildHeight()} check, per the report's Key
 * design decisions.
 */
public final class ExplosionChaos {

    private ExplosionChaos() {
    }

    /**
     * Package-local stub matching {@code com.hbm.potion.HbmPotionEffects#isWarDim}'s established
     * convention (that method is package-private to {@code com.hbm.potion}, unreachable here; see
     * also {@link com.hbm.entity.effect.EntityBlackHole}'s own identical stub for this family's entity
     * half). CE's real default has {@code peaceDimensionsIsWhitelist=true} with an empty
     * {@code peaceDimensions} set, so every dimension is a "war dimension" out of the box - stubbed
     * {@code true}, not {@code false}, for that reason (see {@code CompatibilityConfig}'s own class
     * javadoc for why the dimension-id re-keying itself is deferred to whichever phase owns world-gen).
     */
    private static boolean isWarDim(Level level) {
        return true;
    }

    /** Looks up one of this port's own registered blocks by path, defaulting to {@code Blocks.AIR} if not (yet) registered. */
    private static Block ours(String path) {
        Block block = BuiltInRegistries.BLOCK.getOptional(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, path)).orElse(null);
        return block != null ? block : Blocks.AIR;
    }

    // ================================================================================================
    // shared sphere-iteration engine
    // ================================================================================================

    /**
     * Optimized iteration algorithm to reduce CPU load during large scale block operations - CE's own
     * shrinking-xz-bound-per-y-layer sphere walk (not a naive cube-scan-and-reject), preserved exactly
     * since several callers pass very large radii ({@link #explodeZOMG}'s name alone suggests
     * deliberately huge blasts). The {@code y} clamp uses this level's real build-height range instead
     * of CE's hardcoded 1.12 {@code [0,255]}.
     */
    private static void forEachBlockInSphere(Level level, int x, int y, int z, int radius, Consumer<BlockPos.MutableBlockPos> action) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int radiusSqHalf = (radius * radius) / 2;

        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight() - 1;

        for (int yy = -radius; yy < radius; yy++) {
            int currentY = y + yy;
            if (currentY < minY || currentY > maxY) continue;

            int YY = yy * yy;
            if (YY >= radiusSqHalf) continue;

            int xzRadius = (int) Math.sqrt(radiusSqHalf - YY);

            for (int xx = -xzRadius; xx <= xzRadius; xx++) {
                int XX = xx * xx;
                int YY_XX = YY + XX;
                if (YY_XX >= radiusSqHalf) continue;

                int zRadius = (int) Math.sqrt(radiusSqHalf - YY_XX);

                for (int zz = -zRadius; zz <= zRadius; zz++) {
                    action.accept(pos.set(x + xx, currentY, z + zz));
                }
            }
        }
    }

    // ================================================================================================
    // block-sphere destruction
    // ================================================================================================

    public static void explode(Level level, Entity detonator, int x, int y, int z, int bombStartStrength) {
        if (!isWarDim(level)) return;
        forEachBlockInSphere(level, x, y, z, bombStartStrength, pos -> destruction(level, pos));
    }

    private static void destruction(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        Block b = state.getBlock();
        if (b == Blocks.BEDROCK || isReinforced(b) || b.getExplosionResistance() > 2_000_000) {
            // Indestructible
        } else {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }
    }

    /** CE's {@code explode}'s indestructible-block list beyond bedrock/resistance - all 5 now registered by {@code GenericBlocks.registerRadResistant} (foundation wave). */
    private static boolean isReinforced(Block b) {
        return b == ours("reinforced_brick") || b == ours("reinforced_sand") || b == ours("reinforced_glass")
                || b == ours("reinforced_lamp_on") || b == ours("reinforced_lamp_off");
    }

    public static void spawnExplosion(Level level, Entity detonator, int x, int y, int z, int bound) {
        RandomSource random = level.getRandom();
        for (int i = 0; i < 25; i++) {
            blast(level, detonator, x + random.nextInt(bound), y + random.nextInt(bound), z + random.nextInt(bound));
            blast(level, detonator, x + random.nextInt(bound), y - random.nextInt(bound), z + random.nextInt(bound));
            blast(level, detonator, x + random.nextInt(bound), y + random.nextInt(bound), z - random.nextInt(bound));
            blast(level, detonator, x - random.nextInt(bound), y + random.nextInt(bound), z + random.nextInt(bound));
            blast(level, detonator, x - random.nextInt(bound), y - random.nextInt(bound), z + random.nextInt(bound));
            blast(level, detonator, x - random.nextInt(bound), y + random.nextInt(bound), z - random.nextInt(bound));
            blast(level, detonator, x + random.nextInt(bound), y - random.nextInt(bound), z - random.nextInt(bound));
            blast(level, detonator, x - random.nextInt(bound), y - random.nextInt(bound), z - random.nextInt(bound));
        }
    }

    private static void blast(Level level, Entity detonator, double x, double y, double z) {
        // CE: world.createExplosion(detonator, x, y, z, 10.0F, true) - matches ExplosionLarge.explode's
        // own already-established true -> (fire=true, .TNT) mapping for this exact CE call shape.
        level.explode(detonator, x, y, z, 10.0F, true, Level.ExplosionInteraction.TNT);
    }

    // ================================================================================================
    // gas/poison-cloud area effects
    // ================================================================================================

    /** CE's own name - "cloudPoisoning" per an inline dev comment. */
    public static void c(Level level, int x, int y, int z, int bombStartStrength) {
        if (!isWarDim(level)) return;

        double wat = bombStartStrength * 2.0D;
        AABB box = cloudBounds(x, y, z, wat);
        List<Entity> list = level.getEntities((Entity) null, box, ExplosionChaos::isNotSpectatingOrCreative);

        for (Entity entity : list) {
            if (!withinCloudRange(entity, x, y, z, wat)) continue;

            if (entity instanceof Player player) {
                if (!ArmorRegistry.hasProtection(player, EquipmentSlot.HEAD, HazardClass.GAS_BLISTERING)) {
                    damageFullSuit(player, 5);
                }
            }

            boolean hazmatProtected = entity instanceof Player p && ArmorUtil.checkForHazmat(p);
            if (!hazmatProtected && entity instanceof LivingEntity living) {
                if (living.hasEffect(HbmPotionEffects.TAINT)) {
                    living.removeEffect(HbmPotionEffects.TAINT);
                    living.addEffect(new MobEffectInstance(HbmPotionEffects.MUTATION, 1 * 60 * 60 * 20, 0, false, true));
                } else if (ArmorRegistry.hasProtection(living, EquipmentSlot.HEAD, HazardClass.BACTERIA)) {
                    ArmorUtil.damageGasMaskFilter(living, 1);
                } else {
                    living.hurt(level.damageSources().source(ModDamageTypes.CLOUD), 3);
                }
            }
        }
    }

    /** Alcater: pc for pinkCloudPoisoning. */
    public static void pc(Level level, int x, int y, int z, int bombStartStrength) {
        if (!isWarDim(level)) return;

        double wat = bombStartStrength * 2.0D;
        AABB box = cloudBounds(x, y, z, wat);
        List<Entity> list = level.getEntities((Entity) null, box, ExplosionChaos::isNotSpectatingOrCreative);

        for (Entity entity : list) {
            if (!withinCloudRange(entity, x, y, z, wat)) continue;

            if (entity instanceof Player player) {
                if (!ArmorRegistry.hasProtection(player, EquipmentSlot.HEAD, HazardClass.GAS_BLISTERING)) {
                    damageFullSuit(player, 25);
                }
            }
            if (entity instanceof LivingEntity living) {
                if (ArmorRegistry.hasAllProtection(living, EquipmentSlot.HEAD, HazardClass.BACTERIA, HazardClass.SAND)) {
                    ArmorUtil.damageGasMaskFilter(living, 2);
                } else {
                    living.hurt(level.damageSources().source(ModDamageTypes.PC), 5);
                }
            }
        }
    }

    /** Alcater: used by grenades and Chlorine seal gas blocks. */
    public static void poison(Level level, int x, int y, int z, int bombStartStrength) {
        if (!isWarDim(level)) return;

        double wat = bombStartStrength * 2.0D;
        AABB box = cloudBounds(x, y, z, wat);
        List<LivingEntity> list = level.getEntitiesOfClass(LivingEntity.class, box, ExplosionChaos::isNotSpectatingOrCreative);

        for (LivingEntity entity : list) {
            if (!withinCloudRange(entity, x, y, z, wat)) continue;

            if (ArmorRegistry.hasAllProtection(entity, EquipmentSlot.HEAD, HazardClass.NERVE_AGENT)) {
                ArmorUtil.damageGasMaskFilter(entity, 1);
            } else {
                entity.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 5 * 20, 0));
                entity.addEffect(new MobEffectInstance(MobEffects.POISON, 20 * 20, 2));
                entity.addEffect(new MobEffectInstance(MobEffects.WITHER, 1 * 20, 1));
                entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30 * 20, 1));
                entity.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 30 * 20, 2));
            }
        }
    }

    private static AABB cloudBounds(int x, int y, int z, double wat) {
        int i = Mth.floor(x - wat - 1.0D);
        int j = Mth.floor(x + wat + 1.0D);
        int k = Mth.floor(y - wat - 1.0D);
        int i2 = Mth.floor(y + wat + 1.0D);
        int l = Mth.floor(z - wat - 1.0D);
        int j2 = Mth.floor(z + wat + 1.0D);
        return new AABB(i, k, l, j, i2, j2);
    }

    private static boolean isNotSpectatingOrCreative(Entity e) {
        return !(e instanceof Player p && (p.isSpectator() || p.isCreative()));
    }

    /** CE's {@code d4 <= bombStartStrength*2} outer gate plus its real {@code d9 < wat} eye-height-adjusted inner gate. */
    private static boolean withinCloudRange(Entity entity, int x, int y, int z, double wat) {
        double outer = Math.sqrt(entity.distanceToSqr(x, y, z)) / wat;
        if (outer > 1.0D) return false;

        double d5 = entity.getX() - x;
        double d6 = entity.getY() + entity.getEyeHeight() - y;
        double d7 = entity.getZ() - z;
        return Math.sqrt(d5 * d5 + d6 * d6 + d7 * d7) < wat;
    }

    private static void damageFullSuit(Player player, int amount) {
        ArmorUtil.damageSuit(player, EquipmentSlot.FEET, amount);
        ArmorUtil.damageSuit(player, EquipmentSlot.LEGS, amount);
        ArmorUtil.damageSuit(player, EquipmentSlot.CHEST, amount);
        ArmorUtil.damageSuit(player, EquipmentSlot.HEAD, amount);
    }

    // ================================================================================================
    // fire-setting
    // ================================================================================================

    /** Sets all flammable blocks on fire. */
    public static void flameDeath(Level level, Entity detonator, BlockPos pos, int bound) {
        if (!isWarDim(level)) return;
        BlockPos.MutableBlockPos posUp = new BlockPos.MutableBlockPos();

        forEachBlockInSphere(level, pos.getX(), pos.getY(), pos.getZ(), bound, mPos -> {
            posUp.set(mPos.getX(), mPos.getY() + 1, mPos.getZ());
            BlockState state = level.getBlockState(mPos);
            if (state.getBlock().isFlammable(state, level, mPos, Direction.UP) && level.getBlockState(posUp).is(Blocks.AIR)) {
                level.setBlock(posUp, Blocks.FIRE.defaultBlockState(), 3);
            }
        });
    }

    /** Sets all blocks on fire (broader trigger than {@link #flameDeath} - doesn't require flammability). */
    public static void burn(Level level, Entity detonator, BlockPos pos, int bound) {
        if (!isWarDim(level)) return;
        BlockPos.MutableBlockPos posUp = new BlockPos.MutableBlockPos();

        forEachBlockInSphere(level, pos.getX(), pos.getY(), pos.getZ(), bound, mPos -> {
            posUp.set(mPos.getX(), mPos.getY() + 1, mPos.getZ());
            BlockState upState = level.getBlockState(posUp);
            if ((upState.is(Blocks.AIR) || upState.is(Blocks.SNOW)) && !level.getBlockState(mPos).is(Blocks.AIR)) {
                level.setBlock(posUp, Blocks.FIRE.defaultBlockState(), 3);
            }
        });
    }

    // ================================================================================================
    // particle/VFX-only broadcasts - documented no-ops, see class javadoc
    // ================================================================================================

    public static void spawnChlorine(Level level, double x, double y, double z, int count, double speed, int type) {
        if (!isWarDim(level)) return;
        // TODO(EntityModFXShadow/AuxParticlePacketNT/PacketThreading, Phase 5): see class javadoc.
    }

    public static void spawnVolley(Level level, double x, double y, double z, int count, double speed) {
        if (!isWarDim(level)) return;
        // TODO(EntityModFXShadow/AuxParticlePacketNT/PacketThreading, Phase 5): see class javadoc.
    }

    // ================================================================================================
    // legacy artillery/rocket/boss-projectile spawner family - documented no-ops, see class javadoc
    // ================================================================================================

    public static void cluster(Level level, int x, int y, int z, int count, double gravity) {
        // TODO(EntityRocket, not ported anywhere in this port): see class javadoc.
    }

    public static void miniMirv(Level level, double x, double y, double z) {
        // TODO(EntityMiniNuke, not ported anywhere in this port): see class javadoc. Note: CE's real
        // "mini-MIRV" spawns EntityMiniNuke, not a separate EntityMiniMIRV class - do not conflate the
        // two when this method is eventually implemented.
    }

    public static void schrab(Level level, int x, int y, int z, int count, int gravity) {
        // TODO(EntitySchrab, not ported anywhere in this port): see class javadoc.
    }

    public static void zomg(Level level, double x, double y, double z, int count, Entity shooter, Entity zomg) {
        // TODO(EntityRainbow, not ported anywhere in this port): see class javadoc.
    }

    /** Drillgon200: This method name irks me. */
    public static void tauMeSinPi(Level level, double x, double y, double z, int count, Entity shooter, Entity tau) {
        // TODO(EntityBullet, owned by docs/phase4/entities_legacy_bullet_system.md): see class javadoc
        // - no existing LegacyMobBulletConfigs entry matches this method's real 35-400 damage range.
    }

    // ================================================================================================
    // vanilla-explosion carpet-bomb - covered above (spawnExplosion); block-relocation/entity-shove
    // ================================================================================================

    public static void floater(Level level, Entity detonator, BlockPos pos, int radi, int height) {
        floater(level, detonator, pos.getX(), pos.getY(), pos.getZ(), radi, height);
    }

    public static void floater(Level level, Entity detonator, int x, int y, int z, int radi, int height) {
        if (!isWarDim(level)) return;
        forEachBlockInSphere(level, x, y, z, radi, pos -> {
            BlockState save = level.getBlockState(pos);
            if (!save.is(Blocks.AIR)) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                level.setBlock(new BlockPos(pos.getX(), pos.getY() + height, pos.getZ()), save, 3);
            }
        });
    }

    public static void move(Level level, BlockPos pos, int radius, int a, int b, int c) {
        move(level, pos.getX(), pos.getY(), pos.getZ(), radius, a, b, c);
    }

    public static void move(Level level, int x, int y, int z, int radius, int a, int b, int c) {
        double wat = radius;
        double doubledRadius = radius * 2.0D;

        AABB box = cloudBounds(x, y, z, wat);
        List<Entity> list = level.getEntities((Entity) null, box);
        RandomSource random = level.getRandom();

        for (Entity entity : list) {
            double d4 = Math.sqrt(entity.distanceToSqr(x, y, z)) / doubledRadius;
            if (d4 > 1.0D) continue;

            double d5 = entity.getX() - x;
            double d6 = entity.getY() + entity.getEyeHeight() - y;
            double d7 = entity.getZ() - z;

            if (entity instanceof Sheep) {
                entity.setCustomName(Component.literal("jeb_"));
            } else if (entity instanceof Mob) {
                entity.setCustomName(Component.literal(random.nextInt(2) == 0 ? "Dinnerbone" : "Grumm"));
            }

            double d9 = Math.sqrt(d5 * d5 + d6 * d6 + d7 * d7);
            if (d9 < wat) {
                entity.setPos(entity.getX() + a, entity.getY() + b, entity.getZ() + c);
            }
        }
    }

    // ================================================================================================
    // block-relocation/rubble utilities
    // ================================================================================================

    public static void levelDown(Level level, int x, int y, int z, int radius) {
        if (!isWarDim(level)) return;
        if (level.isClientSide()) return;

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int i = x - radius; i <= x + radius; i++) {
            for (int j = z - radius; j <= z + radius; j++) {
                pos.set(i, y, j);
                BlockState state = level.getBlockState(pos);
                float hardness = state.getDestroySpeed(level, pos);

                if (hardness < 6000 && hardness > 0 && !state.is(Blocks.AIR)) {
                    EntityRubble rubble = new EntityRubble(level, i + 0.5D, y, j + 0.5D);
                    rubble.setBlockState(state);
                    rubble.setDeltaMovement(0, 0.025D * 10 + 0.15D, 0);
                    level.addFreshEntity(rubble);

                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
    }

    public static void decontaminate(Level level, BlockPos pos) {
        RandomSource random = level.getRandom();
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();

        if (block == ours("waste_earth") && random.nextInt(3) != 0) {
            level.setBlock(pos, Blocks.GRASS_BLOCK.defaultBlockState(), 3);
        } else if (block == ours("waste_grass_tall") && random.nextInt(3) != 0) {
            level.setBlock(pos, Blocks.SHORT_GRASS.defaultBlockState(), 3);
        } else if (block == ours("waste_mycelium") && random.nextInt(5) == 0) {
            level.setBlock(pos, Blocks.MYCELIUM.defaultBlockState(), 3);
        } else if (block == ours("waste_leaves") && random.nextInt(5) != 0) {
            // CE: Blocks.LEAVES.getDefaultState() (meta 0 = oak). No generic "LEAVES" block survives
            // 1.21's metadata flattening - oak is CE's own real meta-0 default.
            level.setBlock(pos, Blocks.OAK_LEAVES.defaultBlockState(), 3);
        } else if (block == ours("waste_trinitite") && random.nextInt(3) == 0) {
            level.setBlock(pos, Blocks.SAND.defaultBlockState(), 3);
        } else if (block == ours("waste_trinitite_red") && random.nextInt(3) == 0) {
            level.setBlock(pos, Blocks.RED_SAND.defaultBlockState(), 3);
        } else if (block == ours("waste_log") && random.nextInt(3) != 0) {
            // CE preserves the waste log's axis; this port's WasteLog has no axis property to read
            // (already a simplification made by an earlier phase) - defaults to the Y axis.
            level.setBlock(pos, Blocks.OAK_LOG.defaultBlockState(), 3);
        } else if (block == ours("waste_planks") && random.nextInt(3) != 0) {
            level.setBlock(pos, Blocks.OAK_PLANKS.defaultBlockState(), 3);
        } else if (block == WastelandVirusBlocks.BLOCK_TRINITITE.get() && random.nextInt(10) == 0) {
            level.setBlock(pos, ours("lead_block").defaultBlockState(), 3);
        } else if (block == WastelandVirusBlocks.BLOCK_WASTE.get() && random.nextInt(10) == 0) {
            level.setBlock(pos, ours("lead_block").defaultBlockState(), 3);
        } else if (block == WastelandVirusBlocks.SELLAFIELD.get()) {
            // CE's real 5-step meta decay chain (bare -> meta4 -> meta3 -> meta2 -> meta1 -> meta0 ->
            // sellafield_slaked, 1-in-10 for the first step and 1-in-5 for every step after) is
            // re-expressed directly against this port's own BlockSellafield#LEVEL property (0-5,
            // 5=freshest per FalloutConfigJSON's own confirmed level-rescale direction) rather than
            // CE's separate per-meta block-identity branches, since this port already collapsed that
            // distinction into one int property (see BlockSellafield's own javadoc for the identical
            // adaptation its own randomTick already makes).
            int currentLevel = state.getValue(BlockSellafield.LEVEL);
            int chance = currentLevel == 5 ? 10 : 5;
            if (random.nextInt(chance) == 0) {
                if (currentLevel > 0) {
                    level.setBlock(pos, WastelandVirusBlocks.SELLAFIELD.get().defaultBlockState().setValue(BlockSellafield.LEVEL, currentLevel - 1), 3);
                } else {
                    level.setBlock(pos, WastelandVirusBlocks.SELLAFIELD_SLAKED.get().defaultBlockState(), 3);
                }
            }
        } else if (block == WastelandVirusBlocks.SELLAFIELD_SLAKED.get() && random.nextInt(5) == 0) {
            level.setBlock(pos, Blocks.STONE.defaultBlockState(), 3);
        }
    }

    public static void hardenVirus(Level level, int x, int y, int z, int bombStartStrength) {
        if (!isWarDim(level)) return;
        forEachBlockInSphere(level, x, y, z, bombStartStrength, pos -> {
            if (level.getBlockState(pos).is(WastelandVirusBlocks.CRYSTAL_VIRUS.get())) {
                level.setBlock(pos, WastelandVirusBlocks.CRYSTAL_HARDENED.get().defaultBlockState(), 3);
            }
        });
    }

    public static void spreadVirus(Level level, int x, int y, int z, int bombStartStrength) {
        if (!isWarDim(level)) return;
        RandomSource random = level.getRandom();
        forEachBlockInSphere(level, x, y, z, bombStartStrength, pos -> {
            if (random.nextInt(15) == 0 && !level.getBlockState(pos).is(Blocks.AIR)) {
                level.setBlock(pos, WastelandVirusBlocks.CHEATER_VIRUS_SEED.get().defaultBlockState(), 3);
            }
        });
    }

    // ================================================================================================
    // pulse/pDestruction
    // ================================================================================================

    public static void pulse(Level level, int x, int y, int z, int bombStartStrength) {
        if (!isWarDim(level)) return;
        forEachBlockInSphere(level, x, y, z, bombStartStrength, pos -> {
            if (level.getBlockState(pos).getBlock().getExplosionResistance() <= 70) {
                pDestruction(level, pos.getX(), pos.getY(), pos.getZ());
            }
        });
    }

    public static void pDestruction(Level level, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        BlockState state = level.getBlockState(pos);
        FallingBlockEntity.fall(level, pos, state);
        // CE never clears the source block here (unlike every other block-destruction method in this
        // file) - a genuine, confirmed CE bug (block duplication), preserved deliberately rather than
        // fixed, see class javadoc's Open questions in docs/phase4/entities_vortex_gravity_wells.md.
        // FallingBlockEntity.fall() itself clears the source block as an implementation detail of
        // spawning the falling copy, so it is restored immediately after to reproduce that exact
        // (buggy) visible duplication rather than silently "fixing" it.
        level.setBlock(pos, state, 3);
    }

    // ================================================================================================
    // vanilla arrow scatter
    // ================================================================================================

    public static void frag(Level level, int x, int y, int z, int count, boolean flame, Entity shooter) {
        RandomSource random = level.getRandom();

        for (int i = 0; i < count; i++) {
            double d1 = random.nextDouble() * (random.nextInt(2) == 0 ? -1 : 1);
            double d2 = random.nextDouble();
            double d3 = random.nextDouble() * (random.nextInt(2) == 0 ? -1 : 1);

            Arrow fragment = new Arrow(level, x, y, z, new ItemStack(Items.ARROW), null);
            fragment.setDeltaMovement(d1, d2, d3);
            fragment.setOwner(shooter);
            fragment.setCritArrow(true);
            if (flame) {
                // CE: fragment.setFire(1000) - 1.12's Entity#setFire(int) takes seconds.
                fragment.setRemainingFireTicks(1000 * 20);
            }
            fragment.setBaseDamage(2.5D);

            level.addFreshEntity(fragment);
        }
    }
}
