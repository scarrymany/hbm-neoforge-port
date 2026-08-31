package com.hbm.entity.mob;

import com.hbm.blocks.MaterialBlockGenerator;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.BlockAllocatorBulkie;
import com.hbm.explosion.vanillant.standard.BlockMutatorBulkie;
import com.hbm.explosion.vanillant.standard.BlockProcessorStandard;
import com.hbm.explosion.vanillant.standard.EntityProcessorStandard;
import com.hbm.explosion.vanillant.standard.ExplosionEffectStandard;
import com.hbm.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm.inventory.material.Mats;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * Direct port of CE's {@code com.hbm.entity.mob.EntityCreeperVolatile} (59 lines, read in full) - see
 * {@code docs/phase4/entities_creeper_variants.md}. Stats unchanged from vanilla {@link Creeper} (no
 * {@code applyEntityAttributes} override in CE); no AI override.
 * <p>
 * <b>Explosion</b>: identical shape to {@link EntityCreeperGold}'s (same {@code ExplosionVNT}
 * r=14/7 + {@code BlockAllocatorBulkie(60, 32/16)} + {@code EntityProcessorStandard(0.5)} +
 * {@code PlayerProcessorStandard} + {@code ExplosionEffectStandard}), except the block-mutation
 * target is CE's {@code ModBlocks.block_slag} rather than gold ore (turns stone into slag).
 * <p>
 * <b>{@code block_slag} resolution (a real, resolved forward reference):</b> CE's call passes a
 * metadata index ({@code new BlockMutatorBulkie(ModBlocks.block_slag, 1)}), which the already-ported
 * {@link BlockMutatorBulkie} has no {@code (Block, int)} overload for - flagged as a real dependency
 * gap by the research report and by several already-committed comments elsewhere in this port
 * ({@code XFactoryTool}, {@code MaterialBlockGenerator}'s own class javadoc). Those comments predate
 * this port's Mats/{@link MaterialBlockGenerator} generative pass actually running: {@link Mats#MAT_SLAG}
 * is tagged for {@code MaterialShapes.BLOCK} autogen, so a slag storage block <em>is</em> already
 * registered by {@link MaterialBlockGenerator#registerAll()} - just under the generated name
 * {@code slag_block} (a plain single-state {@code BlockBase}, not CE's multi-variant "broken"-substate
 * {@code BlockMeta}) rather than CE's legacy {@code block_slag} id. Since it is single-state, CE's
 * metadata index is moot and {@link BlockMutatorBulkie}'s existing {@code (Block)} constructor applies
 * directly - no new block registration was needed here after all.
 * <p>
 * <b>Drops</b>: CE's {@code dropFewItems} drops {@code ModItems.sulfur x(2+rand(3))} +
 * {@code ModItems.stick_tnt x(1+rand(2))}, no looting scaling, no player-kill distinction. Neither item
 * is registered anywhere in this port yet - {@code sulfur} (the raw dust item, distinct from the
 * already-real {@code crystal_sulfur}) is a pre-existing, differently-cited gap (see
 * {@code SILEXRecipes.java}/{@code MixerRecipes.java}/{@code CrystallizerRecipes.java}), and
 * {@code stick_tnt} is named as an unclaimed small registration by this area's own research report's
 * Deferred scope #8 - both out of this package's scope (creeper mobs, not the base material/item
 * catalog). This variant therefore drops nothing on death until either item lands; a documented
 * {@code knownGap}, not a silent omission.
 */
public class EntityCreeperVolatile extends Creeper {

    public EntityCreeperVolatile(EntityType<? extends Creeper> type, Level level) {
        super(type, level);
    }

    /** See {@link EntityCreeperGold#createAttributes()} for why this reimplements from
     *  {@link Monster#createMonsterAttributes()} rather than calling {@code Creeper.createAttributes()}. */
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.25D);
    }

    @Override
    protected void explodeCreeper() {
        if (this.level().isClientSide) return;

        Block slag = MaterialBlockGenerator.get(Mats.MAT_SLAG).get();

        ExplosionVNT vnt = new ExplosionVNT(this.level(), this.getX(), this.getY(), this.getZ(),
                this.isPowered() ? 14 : 7, this);
        vnt.setBlockAllocator(new BlockAllocatorBulkie(60, this.isPowered() ? 32 : 16));
        vnt.setBlockProcessor(new BlockProcessorStandard().withBlockEffect(new BlockMutatorBulkie(slag)));
        vnt.setEntityProcessor(new EntityProcessorStandard().withRangeMod(0.5F));
        vnt.setPlayerProcessor(new PlayerProcessorStandard());
        vnt.setSFX(new ExplosionEffectStandard());
        vnt.explode();

        this.discard();
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        // CE: ModItems.sulfur x(2+rand(3)) + ModItems.stick_tnt x(1+rand(2)) - neither item is
        // registered anywhere in this port yet, see class javadoc. Documented no-op, not silently
        // dropped: revisit once either item lands.
    }
}
