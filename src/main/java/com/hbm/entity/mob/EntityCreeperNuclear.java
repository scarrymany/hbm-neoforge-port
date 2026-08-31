package com.hbm.entity.mob;

import com.hbm.damage.ModDamageTypes;
import com.hbm.entity.logic.EntityNukeExplosionMK5;
import com.hbm.explosion.ExplosionNukeGeneric;
import com.hbm.explosion.ExplosionNukeSmall;
import com.hbm.interfaces.IRadiationImmune;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.main.AdvancementManager;
import com.hbm.particle.HbmEffect;
import com.hbm.util.ContaminationUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Direct port of CE's {@code com.hbm.entity.mob.EntityCreeperNuclear} (140 lines, read in full) - the
 * deepest of the 5 creeper variants, see {@code docs/phase4/entities_creeper_variants.md}. 50 HP
 * (2.5x vanilla), 0.3 speed (1.2x), {@code fuseTime = 75} (2.5x vanilla's 30 - a deliberately long
 * fuse to let players flee a nuke-tier blast). No AI override.
 * <p>
 * <b>{@code implements IRadiationImmune}</b> - CE hardcodes this class into
 * {@code ContaminationUtil}'s {@code immuneEntities} array instead of using the interface both
 * {@code EntityCreeperGold}/{@code EntityCreeperTainted} already implement; the research report
 * explicitly recommends the interface here instead as a deliberate, behavior-preserving departure
 * (this port's {@code IRadiationImmune} exists exactly to avoid re-growing that hardcoded list) -
 * {@code isRadImmune} already checks {@code instanceof IRadiationImmune} generically, so no change to
 * {@code ContaminationUtil} was needed beyond the doc-comment cleanup this package also makes there.
 * <p>
 * <b>{@link #hurt}</b>: immune to and <em>healed by</em> {@link ModDamageTypes#RADIATION}/
 * {@link ModDamageTypes#MUD_POISONING} damage specifically (both already registered in this port),
 * matching CE's {@code source == ModDamageSource.radiation || source == ModDamageSource.mudPoisoning}
 * check via the 1.21.1 {@code DamageSource.is(ResourceKey)} idiom. Also guards re-entrant damage after
 * death via {@code this.dead} (CE's own comment: prevents the death animation replaying) - confirmed
 * as a real, directly-accessible protected {@code LivingEntity} field via Neo Edition's own compiling
 * {@code CreeperNuclear.hurt} override, consulted for this exact API-shape point only.
 * <p>
 * <b>{@link #tick()}</b>: every server tick, contaminates every other living entity within a 5-block
 * AABB grow with {@code HazardType.RADIATION}/{@code ContaminationType.CREATIVE} at 0.25 (a walking
 * radiation source, reusing the already-real {@link ContaminationUtil#contaminate}); also regenerates
 * 1 HP every 10 ticks while below max HP (same pattern as {@link EntityCreeperTainted}).
 * <p>
 * <b>{@link #setPowered}</b>: CE's own method writes directly to the protected {@code POWERED}
 * {@code DataParameter} it inherits from {@code EntityCreeper}, since {@code TileEntityTesla}'s zap
 * loop needs to charge a Nuclear Creeper without a real lightning strike. Vanilla 1.21.1's equivalent
 * {@code DATA_IS_POWERED} accessor is a <em>private static</em> field on {@code Creeper} with no
 * public setter, and this port has no access-transformer infrastructure to widen it (no
 * {@code [[accessTransformers]]} block anywhere in {@code neoforge.mods.toml}/
 * {@code accesstransformer.cfg}). Rather than reflect into an unverifiable private field name (as
 * {@link CreeperVariantSupport} already accepts that risk for elsewhere, for the much lower-stakes
 * fuse-length tuning), this specific, externally-callable state flip round-trips through the
 * inherited <em>protected</em> {@code readAdditionalSaveData(CompoundTag)} override instead - the
 * exact same code path vanilla's own {@code /summon minecraft:creeper ~ ~ ~ {powered:1b}} NBT syntax
 * uses internally, and a guaranteed-accessible override point (Java requires an override to keep its
 * superclass's access level or wider, so {@code Creeper}'s override cannot be narrower than
 * {@code Entity}'s own {@code protected} declaration) - no reflection, no new build-system risk.
 * <p>
 * <b>{@link #explodeCreeper()}</b>: branches on {@code isPowered()} (vanilla's lightning-charged
 * state, inherited unmodified) x the {@code mobGriefing} gamerule, exactly matching CE's 4-way
 * branch: powered+griefing spawns a real 50-yield {@link EntityNukeExplosionMK5} (already-ported
 * Phase 3 class); powered+no-griefing calls {@link ExplosionNukeGeneric#dealDamage} (damage-only, no
 * terrain destruction); unpowered uses {@link ExplosionNukeSmall#explode} with
 * {@code PARAMS_MEDIUM}/{@code PARAMS_SAFE}. CE's unconditional VFX (a client-broadcast
 * {@code AuxParticlePacketNT(HbmEffectNT.Muke,...)} + {@code HBMSoundHandler.mukeExplosion}, both
 * before the powered/unpowered branch, {@code upstream/hbm-ce/.../EntityCreeperNuclear.java:117-118})
 * is now wired via {@link com.hbm.particle.HbmEffect#MUKE} - see
 * {@code docs/phase5/particle_engine_and_generic_vfx.md}.
 * <p>
 * <b>{@link #die}</b>: grants {@link AdvancementManager#bossCreeper} (now real, foundation wave) to
 * every player within 50 blocks. CE's bonus "Nuke Standard ammo on skeleton/dispenser-arrow kill" drop
 * needs {@code OreDictManager.DictFrame}, which does not exist in this port yet (Deferred scope #7) -
 * a cosmetic-only gap, documented and skipped.
 * <p>
 * <b>Drops</b> ({@link #dropCustomDeathLoot}): CE overrides {@code getDropItem() -> Items.TNT}
 * without overriding {@code dropFewItems} itself (it calls {@code super.dropFewItems} then adds a
 * separate bonus roll), so - like {@link EntityCreeperTainted}/{@link EntityCreeperPhosgene} - the
 * base count uses vanilla {@code EntityCreeper}'s own algorithm, approximated here the same way (0-2
 * items). The CE bonus 1-in-3 {@code ModItems.coin_creeper} drop needs an unregistered
 * {@code ItemCustomLore}-based item (Deferred scope #8, cosmetic-only) and is not reproduced.
 * <p>
 * <b>Not reproduced</b> (both explicitly out of this package's scope per the research report):
 * {@code EntityCyberCrab}'s targeting-exclusion of this class (that boss/mob's own report owns
 * preserving the cross-reference) and {@code DamageResistanceHandler}'s 5-armor/0.35-multiplier
 * explosion-resistance bonus (this port's {@code DamageResistanceHandler} is confirmed to be only a
 * minimal stub, not the full entity-resistance-table system CE's bonus needs a home in).
 */
public class EntityCreeperNuclear extends Creeper implements IRadiationImmune {

    public EntityCreeperNuclear(EntityType<? extends Creeper> type, Level level) {
        super(type, level);
        CreeperVariantSupport.setFuseTime(this, 75);
    }

    /** Matches Neo Edition's own real, compiling {@code CreeperNuclear.createAttributes()} exactly
     *  (reimplemented from {@link Monster#createMonsterAttributes()}; see
     *  {@link EntityCreeperGold#createAttributes()} for why this port follows that shape port-wide
     *  rather than calling {@code Creeper.createAttributes()} directly). */
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 50.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.dead) return false;

        if (source.is(ModDamageTypes.RADIATION) || source.is(ModDamageTypes.MUD_POISONING)) {
            if (this.isAlive()) this.heal(amount);
            return false;
        }

        return super.hurt(source, amount);
    }

    /**
     * {@code Creeper#explodeCreeper()} is {@code private} in real 1.21.1 - not a legal override point
     * (see {@link CreeperVariantSupport}'s class javadoc). The explosion-interception check must run
     * <em>before</em> {@code super.tick()} (calling this directly and returning instead of delegating,
     * on the tick vanilla's own private countdown would have fired) - the contamination-aura/regen tick
     * merges into the same override rather than a second {@code tick()} method.
     */
    @Override
    public void tick() {
        if (!this.level().isClientSide() && this.isAlive() && CreeperVariantSupport.isAboutToExplode(this)) {
            explodeCreeper();
            return;
        }
        super.tick();

        if (!this.level().isClientSide) {
            List<Entity> nearby = this.level().getEntitiesOfClass(Entity.class, this.getBoundingBox().inflate(5));
            for (Entity e : nearby) {
                if (e != this && e instanceof LivingEntity living) {
                    ContaminationUtil.contaminate(living, ContaminationUtil.HazardType.RADIATION,
                            ContaminationUtil.ContaminationType.CREATIVE, 0.25D);
                }
            }

            if (this.isAlive() && this.getHealth() < this.getMaxHealth() && this.tickCount % 10 == 0) {
                this.heal(1.0F);
            }
        }
    }

    /** See class javadoc for why this round-trips through {@code readAdditionalSaveData} instead of reflection. */
    public void setPowered(boolean powered) {
        if (this.isPowered() == powered) return;
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("powered", powered);
        this.readAdditionalSaveData(tag);
    }

    protected void explodeCreeper() {
        if (this.level().isClientSide) return;

        // Matches vanilla's own private explodeCreeper()'s `this.dead = true;` placement (before any
        // blast that could otherwise hurt this entity again mid-explosion) - also what makes this
        // class's own hurt() `if (this.dead) return false;` guard (see class javadoc) actually live.
        this.dead = true;

        Level level = this.level();
        boolean mobGriefing = level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);

        // CE plays this sound+particle unconditionally, before the powered/unpowered branch below
        // (upstream/hbm-ce/.../EntityCreeperNuclear.java:117-118) - preserved 1:1, including CE's own
        // double-sound quirk in the unpowered branch (ExplosionNukeSmall.explode also plays
        // mukeExplosion), rather than "fixed" - CE is the sole source of truth for behavior here.
        HbmEffect.sendPacket(level, HbmEffect.MUKE, this.getX(), this.getY() + 0.5, this.getZ(), 250, null);
        level.playSound(null, this.getX(), this.getY() + 0.5, this.getZ(), HBMSoundHandler.mukeExplosion.get(), SoundSource.HOSTILE, 15.0F, 1.0F);

        if (this.isPowered()) {
            if (mobGriefing) {
                level.addFreshEntity(EntityNukeExplosionMK5.statFac(level, 50, this.getX(), this.getY(), this.getZ())
                        .setDetonator(this));
            } else {
                ExplosionNukeGeneric.dealDamage(level, this.getX(), this.getY() + 0.5, this.getZ(), 100);
            }
        } else {
            if (mobGriefing) {
                ExplosionNukeSmall.explode(level, this.getX(), this.getY() + 0.5, this.getZ(), ExplosionNukeSmall.PARAMS_MEDIUM);
            } else {
                ExplosionNukeSmall.explode(level, this.getX(), this.getY() + 0.5, this.getZ(), ExplosionNukeSmall.PARAMS_SAFE);
            }
        }

        this.discard();
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);

        if (!this.level().isClientSide) {
            List<ServerPlayer> players = this.level().getEntitiesOfClass(ServerPlayer.class, this.getBoundingBox().inflate(50));
            for (ServerPlayer player : players) {
                AdvancementManager.grantAchievement(player, AdvancementManager.bossCreeper);
            }
        }

        // CE also drops one OreDictManager.DictFrame.fromOne(ModItems.ammo_standard,
        // GunFactory.EnumAmmo.NUKE_STANDARD) when killed by a Skeleton or a no-owner (dispenser-fired)
        // Arrow - OreDictManager/DictFrame does not exist in this port yet (Deferred scope #7).
        // Cosmetic-only, not reproduced.
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        int count = this.random.nextInt(3);
        if (count > 0) {
            this.spawnAtLocation(new ItemStack(Items.TNT, count), 0.0F);
        }
        // CE's 1-in-3 bonus ModItems.coin_creeper drop is not reproduced - see class javadoc.
    }
}
