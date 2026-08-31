package com.hbm.entity.mob;

import com.hbm.damage.ModDamageTypes;
import com.hbm.handler.radiation.ChunkRadiationManager;
import com.hbm.interfaces.IRadiationImmune;
import com.hbm.items.PlateCrystalWasteItems;
import com.hbm.items.machine.ItemZirnoxRodDepleted.EnumZirnoxTypeDepleted;
import com.hbm.items.machine.MachineItems;
import com.hbm.items.special.SpecialItems;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.main.AdvancementManager;
import com.hbm.util.ContaminationUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Ported from CE's {@code com.hbm.entity.mob.EntityRADBeast} (239 lines, read in full) - see
 * {@code docs/phase4/entities_bosses.md}'s "RAD Beast - boss-adjacent, explicitly excluded from the
 * 'boss' list" section. 120 HP base ("swarm" spawn), 360 HP + holds/drops {@link SpecialItems#COIN_RADIATION}
 * "leader" variant ({@link #makeLeader()}) whose death alone grants {@link AdvancementManager#bossMeltdown}
 * (both now real, foundation wave). No boss bar - confirmed by direct read of the CE class (no
 * {@code BossInfo} field anywhere in it), matching the research report's explicit call-out.
 * <p>
 * <b>Real CE bug found (AI tasks) - a deliberate, documented departure, not a silent fix:</b> CE's
 * {@code EntityRADBeast extends net.minecraft.entity.monster.EntityMob} (vanilla 1.12's bare hostile-mob
 * base, itself adding zero default AI) and its 239-line file has <b>no {@code applyEntityAI}/
 * {@code tasks.addTask}/{@code targetTasks.addTask} override anywhere</b> - confirmed by a full read,
 * unlike e.g. {@link EntityMaskMan}'s own constructor, which explicitly registers all 8 of its tasks.
 * Taken completely literally, real CE's RAD Beast would never acquire {@code getAttackTarget()} (used by
 * both its hover-toward-target logic and its {@code attackEntityAsMob} distance branches) through any
 * AI-driven path at all - it would stand inert forever unless something else (no such call exists in CE)
 * sets its target directly. Since this makes the mob's own most-detailed, most-explicitly-requested
 * mechanic (the melee/ranged {@code attackEntityAsMob} split this package's own task brief names) dead
 * code as literally transcribed, this port adds the minimal vanilla AI goals needed to make it a
 * functioning hostile elite (targeting + wander + look, see {@link #registerGoals()}) - preserving every
 * CE-specified *number* (HP, damage, ranges, cooldowns) exactly, while fixing the CE oversight that would
 * otherwise make them all unreachable. Flagged explicitly per this task's own ground rules for a
 * departure from a literal transcription.
 * <p>
 * <b>The melee/ranged attack split</b> (CE's {@code attackEntityAsMob}, reproduced inside the small
 * inner {@code RadiationBreathGoal} rather than overriding {@code Mob#doHurtTarget} - the exact 1.21.1
 * signature for that override could not be confirmed without a compiled jar in this sandbox, and a
 * plain {@link Goal} is both lower-risk and sufficient to reproduce CE's real branching): CE's
 * {@code dist = getDistanceSq(target)} is a <b>squared</b> distance in CE's own 1.12 naming convention
 * (not blocks) - preserved exactly as {@code distanceToSqr(target) < 4.0}/{@code < 30.0} (i.e. real
 * melee range ~2 blocks, real "ranged" branch range ~5.48 blocks - a short-range radiation breath, not a
 * long-range ability, despite reading like "30" at a glance). Melee branch deals
 * {@code Attributes.ATTACK_DAMAGE} via a plain {@code mobAttack} damage source (vanilla's own
 * enchantment/knockback nuance on melee hits is not reproduced, matching this port's established
 * "flavor extras dropped, numeric core preserved" convention); ranged branch deals a flat 16
 * {@link ModDamageTypes#RADIATION} hit plus {@link ChunkRadiationManager#proxy}{@code .incrementRad}
 * (150, capped at 1000) at the RAD Beast's <em>own</em> position - exactly matching the task brief's
 * "melee-range ChunkRadiationManager.proxy.incrementRad call on attackEntityAsMob." CE's cosmetic
 * {@code swingArm}/{@code playLivingSound} calls on the ranged branch are dropped (animation/audio
 * flavor only).
 * <p>
 * <b>Passive area radiation pulse</b> ({@link #tick()}): {@code ContaminationUtil.radiate(level, x, y,
 * z, 32, 500)} every server tick, confirmed real signature (already-real Phase 0/3 API). Also
 * reproduces CE's water-punishment ({@code isInWater() -> hurt(drown, 1)}), the periodic randomized
 * hover-offset ({@code heightOffsetUpdateTime}), and the anti-gravity float-toward-target-above logic
 * (raw {@code deltaMovement.y} nudge) - all read from CE's real {@code onLivingUpdate} verbatim. CE's
 * client-only particle flavor (town-aura/flame/lava puffs) is Phase 5 "Client & UX" VFX, not reproduced
 * here, matching this port's established convention for every other boss/elite mob ported so far.
 * <p>
 * <b>{@code getUnfortunateSoul()}/{@code TARGET_ID}</b> (CE's synced current-target int, used only by
 * render code to draw a beam/highlight) is not reproduced - purely a client-rendering hook with zero
 * gameplay effect, Phase 5 territory like the particle flavor above.
 * <p>
 * <b>Drops</b> ({@link #dropCustomDeathLoot}): CE's {@code dropFewItems}
 * (rod_zirnox_depleted/URANIUM_FUEL, {@code random.nextInt(3) [+1 if recentlyHit]}) plus
 * {@code dropLoot}'s separate 1-3-count uranium/mox/plutonium loop (waste if {@code isWet()}, depleted
 * rod otherwise) are both reproduced with real, already-registered items
 * ({@link MachineItems#ZIRNOX_RODS_DEPLETED}, {@link PlateCrystalWasteItems#WASTE_URANIUM}/
 * {@code WASTE_MOX}/{@code WASTE_PLUTONIUM}). CE's looting-enchantment-scaled {@code nugget_polonium}
 * bonus term is dropped - matching this port's established "no confirmed 1.21.1 idiom yet for reading a
 * killer's Looting level from {@code dropCustomDeathLoot}" convention (see {@code EntityCreeperGold}'s
 * own javadoc for the same, earlier-established simplification).
 */
public class EntityRADBeast extends Monster implements IRadiationImmune {

    private float heightOffset = 0.5F;
    private int heightOffsetUpdateTime;

    public EntityRADBeast(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.noCulling = true;
        // CE: this.isImmuneToFire = true - handled at EntityType registration (.fireImmune()) instead,
        // see RadBeastEntityTypes.
    }

    /** CE: {@code applyEntityAttributes} - 120 HP / 16 attack damage base, plus
     *  {@code getTotalArmorValue() = 8} (the modern {@code Attributes.ARMOR} equivalent). CE never sets
     *  {@code FOLLOW_RANGE}; this port bumps it to 32 (matching the 32-block passive radiation pulse
     *  radius) since the AI-completion goals this port adds (see class javadoc) need a real detection
     *  range to function at all - a necessary AI-completion choice, not a CE-specified number. */
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 120.0D)
                .add(Attributes.ATTACK_DAMAGE, 16.0D)
                .add(Attributes.ARMOR, 8.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    /** See class javadoc "Real CE bug found (AI tasks)". */
    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(1, new RadiationBreathGoal(this));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /**
     * CE: {@code makeLeader()} - 360 HP (healed to full), holds and (via vanilla's standard
     * equipment-drop-chance pass) drops {@link SpecialItems#COIN_RADIATION}. Only the leader variant's
     * death grants {@link AdvancementManager#bossMeltdown} - see {@link #die}.
     */
    public EntityRADBeast makeLeader() {
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(SpecialItems.COIN_RADIATION.get()));
        this.setDropChance(EquipmentSlot.MAINHAND, 1.0F);

        AttributeInstance maxHealth = this.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(360.0D);
        }
        this.setHealth(this.getMaxHealth());
        return this;
    }

    /** CE: {@code canDespawn() { return false; }} */
    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    /** CE: {@code fall(float, float) {}} - no fall damage. */
    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        SoundEvent[] geiger = HBMSoundHandler.geigerSounds();
        return geiger[this.random.nextInt(geiger.length)];
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.BLAZE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return HBMSoundHandler.metalStep.get();
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide) return;

        if (this.isInWater()) {
            this.hurt(this.damageSources().drown(), 1.0F);
        }

        if (--this.heightOffsetUpdateTime <= 0) {
            this.heightOffsetUpdateTime = 100;
            this.heightOffset = 0.5F + (float) this.random.nextGaussian() * 3.0F;
        }

        LivingEntity target = this.getTarget();
        if (target != null && target.getY() + target.getEyeHeight() > this.getY() + this.getEyeHeight() + this.heightOffset) {
            Vec3 motion = this.getDeltaMovement();
            this.setDeltaMovement(motion.x, motion.y + (0.30000001192092896D - motion.y) * 0.30000001192092896D, motion.z);
        }

        if (!this.onGround() && this.getDeltaMovement().y < 0.0D) {
            Vec3 motion = this.getDeltaMovement();
            this.setDeltaMovement(motion.x, motion.y * 0.6D, motion.z);
        }

        // CE's client-only particle flavor (TOWN_AURA/FLAME/LAVA puffs) is Phase 5 VFX - not reproduced.

        ContaminationUtil.radiate(this.level(), this.getX(), this.getY(), this.getZ(), 32, 500);
    }

    @Override
    public void die(DamageSource source) {
        // CE: onDeath's `if(this.getMaxHealth() > 150)` leader check, run before super.onDeath.
        if (this.getMaxHealth() > 150.0D && !this.level().isClientSide) {
            for (ServerPlayer player : this.level().getEntitiesOfClass(ServerPlayer.class, this.getBoundingBox().inflate(50))) {
                AdvancementManager.grantAchievement(player, AdvancementManager.bossMeltdown);
            }
        }
        super.die(source);
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);

        int quantity = this.random.nextInt(3) + (recentlyHit ? 1 : 0);
        if (quantity > 0) {
            this.spawnAtLocation(zirnoxDepleted(EnumZirnoxTypeDepleted.URANIUM_FUEL, quantity), 0.0F);
        }

        int count = this.random.nextInt(3) + 1;
        boolean wet = this.isInWater();
        for (int i = 0; i < count; i++) {
            int r = this.random.nextInt(3);
            if (r == 0) {
                this.spawnAtLocation(wet ? new ItemStack(PlateCrystalWasteItems.WASTE_URANIUM.get())
                        : zirnoxDepleted(EnumZirnoxTypeDepleted.URANIUM_FUEL, 1), 0.0F);
            } else if (r == 1) {
                this.spawnAtLocation(wet ? new ItemStack(PlateCrystalWasteItems.WASTE_MOX.get())
                        : zirnoxDepleted(EnumZirnoxTypeDepleted.MOX_FUEL, 1), 0.0F);
            } else {
                this.spawnAtLocation(wet ? new ItemStack(PlateCrystalWasteItems.WASTE_PLUTONIUM.get())
                        : zirnoxDepleted(EnumZirnoxTypeDepleted.PLUTONIUM_FUEL, 1), 0.0F);
            }
        }
    }

    private static ItemStack zirnoxDepleted(EnumZirnoxTypeDepleted type, int count) {
        return new ItemStack(MachineItems.ZIRNOX_RODS_DEPLETED.get(type).get(), count);
    }

    /**
     * CE: {@code attackEntityAsMob(Entity)}. See class javadoc for why this lives in a small inner
     * {@link Goal} (ticked every AI tick while a target exists) rather than an override of {@code Mob}'s
     * modern melee-trigger method.
     */
    private static final class RadiationBreathGoal extends Goal {

        private final EntityRADBeast beast;
        private int cooldown;

        RadiationBreathGoal(EntityRADBeast beast) {
            this.beast = beast;
            // This goal doubles as the mob's only chase-target movement - see class javadoc "Real CE
            // bug found (AI tasks)". Real CE's attackEntityAsMob has no independent chase logic of its
            // own either (its "ranged" branch only fires within ~5.48 blocks, so something has to close
            // that gap first).
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = this.beast.getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            return this.canUse();
        }

        @Override
        public void tick() {
            LivingEntity target = this.beast.getTarget();
            if (target == null) return;

            this.beast.getLookControl().setLookAt(target, 30.0F, 30.0F);

            // CE's `dist` is getDistanceSq(target) - a SQUARED distance despite comparing against 4.0/
            // 30.0 - see class javadoc. Real ranges: ~2 blocks (melee), ~5.48 blocks (radiation breath).
            double distSq = this.beast.distanceToSqr(target);

            if (distSq > 30.0D) {
                this.beast.getNavigation().moveTo(target, 1.0D);
            } else {
                this.beast.getNavigation().stop();
            }

            if (this.cooldown > 0) {
                this.cooldown--;
                return;
            }

            boolean yOverlap = target.getBoundingBox().maxY > this.beast.getBoundingBox().minY
                    && target.getBoundingBox().minY < this.beast.getBoundingBox().maxY;

            if (distSq < 4.0D && yOverlap) {
                target.hurt(this.beast.damageSources().mobAttack(this.beast),
                        (float) this.beast.getAttributeValue(Attributes.ATTACK_DAMAGE));
                this.cooldown = 20;
            } else if (distSq < 30.0D) {
                BlockPos pos = this.beast.blockPosition();
                ChunkRadiationManager.proxy.incrementRad(this.beast.level(), pos, 150D, 1000D);
                target.hurt(this.beast.damageSources().source(ModDamageTypes.RADIATION), 16.0F);
                this.cooldown = 20;
            }
        }
    }
}
