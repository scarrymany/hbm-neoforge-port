package com.hbm.entity.mob;

import com.hbm.entity.mob.ai.EntityAIMaskmanCasualApproach;
import com.hbm.entity.mob.ai.EntityAIMaskmanLasergun;
import com.hbm.entity.mob.ai.EntityAIMaskmanMinigun;
import com.hbm.handler.ArmorUtil;
import com.hbm.interfaces.IRadiationImmune;
import com.hbm.items.gear.SpecialArmorItems;
import com.hbm.items.special.SpecialItems;
import com.hbm.main.AdvancementManager;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownEgg;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * Ported from CE's {@code com.hbm.entity.mob.EntityMaskMan} (149 lines, read in full) - see
 * {@code docs/phase4/entities_bosses.md}. Purely ranged boss (1000 HP, 100-block follow range, full
 * knockback resistance, 0.25 movement speed, fire-immune): {@link EntityAIMaskmanCasualApproach}
 * paths to a ~10-block standoff position rather than closing to melee range, and CE's own vanilla
 * attack-on-arrival call is commented out in the real source - this port preserves that faithfully,
 * MaskMan's {@code ATTACK_DAMAGE} attribute is set (for CE parity) but genuinely never invoked by any
 * AI task. Damage output is entirely {@link EntityAIMaskmanLasergun} (beyond 10 blocks) and
 * {@link EntityAIMaskmanMinigun} (5-10 blocks).
 * <p>
 * <b>{@link IRadiationImmune}</b> - automatic radiation immunity via this port's already-real
 * interface check in {@code ContaminationUtil.isRadImmune}; no {@code ContaminationUtil} edit needed
 * (confirmed by the research report and this class's own read of that file).
 * <p>
 * <b>CE bug found and fixed</b>: CE declares {@code public float prevHealth;} with no initializer and
 * never assigns it anywhere except inside the very {@code onUpdate} branch it gates
 * ({@code prevHealth >= getMaxHealth()/2}). A Java {@code float} field defaults to {@code 0.0F}, so
 * that condition ({@code 0 >= 500}, for 1000 max HP) can never be true on its own - real CE 1.12.2's
 * self-detonation phase transition is permanently unreachable dead code. This is confirmed
 * self-contained purely from CE's own 149-line file (no engine-side field-shadowing ambiguity needed
 * to prove it - nothing else in the class ever writes to {@link #prevHealth}). This port initializes
 * {@link #prevHealth} to {@link #getMaxHealth()} at construction so the intended one-time 50%-health
 * phase trigger - the research report's own words, "CE's entire boss phase" - actually fires once,
 * matching the evident design intent rather than preserving the uninitialized-field oversight.
 * <p>
 * <b>Boss-bar API</b> ({@link ServerBossEvent}/{@link BossEvent.BossBarColor}/
 * {@code Entity#startSeenByPlayer}) is well-established, long-stable Mojang-mapping knowledge (the
 * same shape vanilla's own {@code EnderDragon}/{@code WitherBoss} use) but is <b>not</b> verified
 * against a compiled 1.21.1 jar in this sandbox - flagged per the research report's own explicit
 * caveat (no compiled dependency jar available to check against here either).
 */
public class EntityMaskMan extends Monster implements IRadiationImmune {

    private final ServerBossEvent bossEvent =
            new ServerBossEvent(this.getDisplayName(), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS);

    /** See class javadoc "CE bug found and fixed". */
    public float prevHealth;

    public EntityMaskMan(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.prevHealth = this.getMaxHealth();
    }

    /** Matches this port's established {@code Monster.createMonsterAttributes()}-based shape (see
     *  e.g. {@link EntityCreeperNuclear#createAttributes()}), wired via
     *  {@code CommonEvents#onEntityAttributeCreation} (see this task's wiringSnippets). */
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 1000.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.FOLLOW_RANGE, 100.0D)
                // CE sets this for parity but never invokes a melee attack anywhere - see class javadoc.
                .add(Attributes.ATTACK_DAMAGE, 15.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new EntityAIMaskmanCasualApproach(this));
        this.goalSelector.addGoal(2, new EntityAIMaskmanMinigun(this, 3));
        this.goalSelector.addGoal(3, new EntityAIMaskmanLasergun(this));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** CE: {@code isAIDisabled() { return false; }} - AI is never allowed to be disabled on this boss. */
    @Override
    public boolean isNoAi() {
        return false;
    }

    /** CE: {@code canDespawn() { return false; }} */
    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!this.isAlive()) return false;

        // CE: a 1-in-10 instant-kill vulnerability to any EntityEgg-sourced indirect damage - a real,
        // intentional joke weakness, preserved exactly (not a bug to drop). DamageSource#getDirectEntity
        // is CE's getImmediateSource() equivalent (already this port's established idiom, see e.g.
        // EntityDeliveryDrone/EntityRequestDrone).
        if (source.getDirectEntity() instanceof ThrownEgg && this.random.nextInt(10) == 0) {
            this.setHealth(0.0F);
            this.die(source);
            // CE also zeroes this.experienceValue on this path - not reproduced: this port could not
            // confirm a stable 1.21.1 Mob API shape for overriding per-kill XP reward without a
            // compiled jar to verify against. The instant-kill mechanic itself (the part that matters
            // for the joke) is fully intact; only the "and also grants no XP orbs" flourish is skipped.
            return true;
        }

        if (source.is(DamageTypeTags.IS_FIRE)) amount = 0F;
        if (source.is(DamageTypeTags.IS_MAGIC)) amount = 0F;
        if (source.is(DamageTypeTags.IS_PROJECTILE)) amount *= 0.25F;
        if (source.is(DamageTypeTags.IS_EXPLOSION)) amount *= 0.5F;
        if (amount > 50F) amount = 50F + (amount - 50F) * 0.25F;

        return super.hurt(source, amount);
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide) {
            float maxHealth = this.getMaxHealth();
            this.bossEvent.setProgress(maxHealth > 0F ? this.getHealth() / maxHealth : 0F);

            // CE: onUpdate's one-time 50%-health self-detonation phase trigger - see class javadoc
            // "CE bug found and fixed" for why prevHealth is initialized to max health at construction.
            float half = maxHealth / 2F;
            if (this.prevHealth >= half && this.getHealth() < half) {
                this.prevHealth = this.getHealth();
                this.level().explode(this, this.getX(), this.getY() + 4D, this.getZ(), 2.5F, Level.ExplosionInteraction.MOB);
            }
        }
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);

        if (!this.level().isClientSide) {
            for (ServerPlayer player : this.level().getEntitiesOfClass(ServerPlayer.class, this.getBoundingBox().inflate(50))) {
                AdvancementManager.grantAchievement(player, AdvancementManager.bossMaskman);
            }
        }
    }

    /** CE: {@code EntityMaskMan#dropFewItems} - gas_mask_m65 (filter pre-installed), coin_maskman,
     *  v1, and a vanilla skull. */
    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);

        ItemStack mask = new ItemStack(SpecialArmorItems.GAS_MASK_M65.get());
        ArmorUtil.installGasMaskFilter(mask, new ItemStack(SpecialItems.GAS_MASK_FILTER_COMBO.get()));
        this.spawnAtLocation(mask, 0.0F);
        this.spawnAtLocation(new ItemStack(SpecialItems.COIN_MASKMAN.get()), 0.0F);
        this.spawnAtLocation(new ItemStack(SpecialItems.V1.get()), 0.0F);
        this.spawnAtLocation(new ItemStack(Items.SKELETON_SKULL), 0.0F);
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    /**
     * Not a CE port (CE's 1.12 boss bar has no equivalent client-tracked lifecycle to mirror here) -
     * a correctness fix this class needs regardless: without clearing {@link #bossEvent} on removal,
     * the purple boss bar would stay stuck on every tracking player's screen forever after MaskMan
     * dies/unloads, since {@link ServerBossEvent} is not itself tied to this entity's lifecycle.
     * Matches vanilla {@code EnderDragon}/{@code WitherBoss}'s own {@code remove(RemovalReason)}
     * override for exactly this purpose.
     */
    @Override
    public void remove(Entity.RemovalReason reason) {
        super.remove(reason);
        this.bossEvent.removeAllPlayers();
    }
}
