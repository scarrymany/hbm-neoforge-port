package com.hbm.entity.mob;

import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.BlockAllocatorBulkie;
import com.hbm.explosion.vanillant.standard.BlockMutatorBulkie;
import com.hbm.explosion.vanillant.standard.BlockProcessorStandard;
import com.hbm.explosion.vanillant.standard.EntityProcessorStandard;
import com.hbm.explosion.vanillant.standard.ExplosionEffectStandard;
import com.hbm.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm.interfaces.IRadiationImmune;
import com.hbm.items.PlateCrystalWasteItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

/**
 * Direct port of CE's {@code com.hbm.entity.mob.EntityCreeperGold} (60 lines, read in full) - see
 * {@code docs/phase4/entities_creeper_variants.md}. Stats unchanged from vanilla {@link Creeper}
 * (20 HP / 0.25 speed / 30-tick fuse - see {@link #createAttributes()} for how that's reproduced).
 * No AI override, matching CE (inherits vanilla's {@code GoalSelector} wholesale).
 * <p>
 * <b>Explosion</b> ({@link #explodeCreeper()}): CE's {@code ExplosionVNT} r=14(powered)/7 with
 * {@code BlockAllocatorBulkie(60, 32/16)} + {@code BlockProcessorStandard} +
 * {@code BlockMutatorBulkie(Blocks.GOLD_ORE)} (mutates stone into gold ore rather than destroying it)
 * + {@code EntityProcessorStandard(rangeMod 0.5)} + {@code PlayerProcessorStandard} +
 * {@code ExplosionEffectStandard} - all 6 classes already exist in this port and are reused as-is,
 * with the exact same constructor arguments CE uses.
 * <p>
 * <b>Drops</b> ({@link #dropCustomDeathLoot}): CE's {@code dropFewItems} fully replaces vanilla's own
 * drop path (player-killed: {@code 5 + rand(6 + 2*looting)} x {@code crystal_gold}; otherwise flat 3),
 * making its {@code getDropItem() -> Items.GUNPOWDER} override dead code (never reached, since
 * {@code dropFewItems} never calls {@code super}/uses it) - not reproduced here for the same reason.
 * {@code crystal_gold} is already registered in this port as
 * {@link PlateCrystalWasteItems#CRYSTAL_GOLD}. <b>Known simplification:</b> the {@code +2*looting}
 * scaling term is dropped (this port has no established 1.21.1 idiom yet for reading a killer's
 * Looting enchantment level from inside {@code dropCustomDeathLoot}, and the base count already
 * dominates); the flat/player-killed split itself is preserved exactly.
 * <p>
 * <b>Natural spawning</b>: CE's real source has {@code getCanSpawnHere()} restrict this variant to
 * {@code posY<=40 && dimension==0} (Overworld) - not reproduced (see this package's
 * {@code CreeperVariantBiomeModifiers}/{@code CreeperVariantEntityTypes} javadoc for why the
 * placement-restriction half of natural spawning is a documented {@code knownGap} rather than a
 * guessed {@code SpawnPlacements} call).
 */
public class EntityCreeperGold extends Creeper implements IRadiationImmune {

    public EntityCreeperGold(EntityType<? extends Creeper> type, Level level) {
        super(type, level);
    }

    /**
     * Reimplemented from {@link Monster#createMonsterAttributes()} + vanilla {@link Creeper}'s own
     * well-known 0.25 movement speed, rather than calling {@code Creeper.createAttributes()} directly
     * - Neo Edition's own real, compiling {@code CreeperNuclear.createAttributes()} does the same
     * (reimplements from {@code Monster.createMonsterAttributes()} rather than calling
     * {@code Creeper.createAttributes()}), the closest signal this sandbox has, with no compiled jar
     * available, to whether that vanilla method is even public. {@code MAX_HEALTH} needs no explicit
     * override: {@code Monster}'s own attribute base already defaults it to vanilla's 20.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.25D);
    }

    @Override
    protected void explodeCreeper() {
        if (this.level().isClientSide) return;

        ExplosionVNT vnt = new ExplosionVNT(this.level(), this.getX(), this.getY(), this.getZ(),
                this.isPowered() ? 14 : 7, this);
        vnt.setBlockAllocator(new BlockAllocatorBulkie(60, this.isPowered() ? 32 : 16));
        vnt.setBlockProcessor(new BlockProcessorStandard().withBlockEffect(new BlockMutatorBulkie(Blocks.GOLD_ORE)));
        vnt.setEntityProcessor(new EntityProcessorStandard().withRangeMod(0.5F));
        vnt.setPlayerProcessor(new PlayerProcessorStandard());
        vnt.setSFX(new ExplosionEffectStandard());
        vnt.explode();

        this.discard();
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        int amount = recentlyHit ? 5 + this.random.nextInt(6) : 3;
        this.spawnAtLocation(new ItemStack(PlateCrystalWasteItems.CRYSTAL_GOLD.get(), amount), 0.0F);
    }
}
