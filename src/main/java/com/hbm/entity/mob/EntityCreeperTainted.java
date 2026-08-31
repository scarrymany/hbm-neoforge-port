package com.hbm.entity.mob;

import com.hbm.interfaces.IRadiationImmune;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;

/**
 * Direct port of CE's {@code com.hbm.entity.mob.EntityCreeperTainted} (96 lines, read in full) - see
 * {@code docs/phase4/entities_creeper_variants.md}. 15 HP / 0.35 speed (both lower/faster than
 * vanilla's 20 HP / 0.25), regenerates 1 HP every 10 ticks while alive and below max HP; no AI
 * override. {@code implements IRadiationImmune} exactly like CE.
 * <p>
 * <b>Independent taint-potion immunity axis</b>: CE's {@code HbmPotion.taint} effect additionally
 * exempts this exact class from its own 1-in-80-per-tick self-damage roll - a <em>separate</em>
 * immunity mechanism from {@code isRadImmune}/{@link IRadiationImmune}. Wired directly into this
 * port's already-real {@link com.hbm.potion.TaintEffect} as part of this same package (see that
 * file's own updated javadoc).
 * <p>
 * <b>Natural spawn path - none.</b> Per the research report's Headline finding #2 (confirmed by
 * repo-wide grep of CE source): {@code EntityCreeperTainted} is never placed by world-gen or any
 * spawn list. Its only CE creation path is {@code BlockTaint#onEntityCollision} instantly replacing
 * any vanilla {@code EntityCreeper} that touches a taint block - {@code BlockTaint}/
 * {@code ModBlocks.taint} does not exist in this port yet (Deferred scope #2), so that mutation path
 * has no home yet either. This mob is fully summonable/spawn-egg-obtainable today with correct
 * combat/regen/drop behavior; only its natural creation trigger is a documented forward reference.
 * <p>
 * <b>Explosion</b> ({@link #explodeCreeper()}): CE's {@code world.newExplosion(this, x, y, z, 5F,
 * false, mobGriefing)} (portable now) plus a taint-terrain-conversion sweep (255 samples in a
 * 15x15x15 cube at taintage 5-7 when powered, 85 samples in a 7x7x7 cube at taintage 10-15 when
 * unpowered, halved-ish under {@code ServerConfig.TAINT_TRAILS}) gated on the mobGriefing gamerule -
 * the conversion sweep needs {@code ModBlocks.taint}/{@code BlockTaint.TAINTAGE}, neither of which
 * exists yet (same Deferred scope #2 gap as the spawn path above), so it is a documented no-op here.
 * <p>
 * <b>Drops</b>: CE overrides only {@code getDropItem() -> Item.getItemFromBlock(Blocks.TNT)}; it does
 * <em>not</em> override {@code dropFewItems}, so vanilla {@code EntityCreeper}'s own randomized
 * drop-count algorithm applies to whatever {@code getDropItem()} returns. This sandbox has no
 * decompiled 1.21.1 vanilla source to read that exact count formula from, so
 * {@link #dropCustomDeathLoot} approximates it using vanilla's well-documented modern creeper loot
 * shape (0-2 base items) with TNT substituted for gunpowder; Looting-enchantment scaling is not
 * reproduced (same documented simplification as {@link EntityCreeperPhosgene}).
 */
public class EntityCreeperTainted extends Creeper implements IRadiationImmune {

    public EntityCreeperTainted(EntityType<? extends Creeper> type, Level level) {
        super(type, level);
    }

    /** See {@link EntityCreeperGold#createAttributes()} for why this reimplements from
     *  {@link Monster#createMonsterAttributes()} rather than calling {@code Creeper.createAttributes()}. */
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 15.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.35D);
    }

    /**
     * {@code Creeper#explodeCreeper()} is {@code private} in real 1.21.1 - not a legal override point
     * (see {@link CreeperVariantSupport}'s class javadoc). The explosion-interception check must run
     * <em>before</em> {@code super.tick()} (calling this directly and returning instead of delegating,
     * on the tick vanilla's own private countdown would have fired) - the regen tick merges into the
     * same override rather than a second {@code tick()} method.
     */
    @Override
    public void tick() {
        if (!this.level().isClientSide() && this.isAlive() && CreeperVariantSupport.isAboutToExplode(this)) {
            explodeCreeper();
            return;
        }
        super.tick();
        if (!this.level().isClientSide && this.isAlive() && this.getHealth() < this.getMaxHealth() && this.tickCount % 10 == 0) {
            this.heal(1.0F);
        }
    }

    protected void explodeCreeper() {
        if (this.level().isClientSide) return;

        // Matches vanilla's own private explodeCreeper()'s `this.dead = true;` placement (before any
        // blast that could otherwise hurt this entity again mid-explosion) - see CreeperVariantSupport.
        this.dead = true;

        boolean mobGriefing = this.level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
        // CE: world.newExplosion(this, x, y, z, 5.0F, false, griefing) - the "isSmoking" arg (here
        // `griefing`) is what actually gates block destruction in 1.12.2's Explosion (isSmoking=false
        // -> clearAffectedBlockPositions(), no block damage at all). Ported explosion interaction must
        // key off the same mobGriefing read, not be hardcoded on - matching vanilla Creeper's own
        // explodeCreeper() TNT/NONE-by-griefing pattern.
        this.level().explode(this, this.getX(), this.getY(), this.getZ(), 5.0F, false,
                mobGriefing ? Level.ExplosionInteraction.TNT : Level.ExplosionInteraction.NONE);

        if (mobGriefing) {
            // CE's taint-terrain-conversion sweep (255 samples/15^3 cube at taintage 5-7 when powered,
            // 85 samples/7^3 cube at taintage 10-15 when unpowered) needs ModBlocks.taint/
            // BlockTaint.TAINTAGE - neither exists in this port yet, see class javadoc Deferred scope
            // #2. Documented no-op.
        }

        this.discard();
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        int count = this.random.nextInt(3);
        if (count > 0) {
            this.spawnAtLocation(new ItemStack(Items.TNT, count), 0.0F);
        }
    }
}
