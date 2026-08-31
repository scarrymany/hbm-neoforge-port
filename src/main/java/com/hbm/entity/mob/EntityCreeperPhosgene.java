package com.hbm.entity.mob;

import com.hbm.damage.tags.ModDamageTypeTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * Direct port of CE's {@code com.hbm.entity.mob.EntityCreeperPhosgene} (48 lines, read in full) - see
 * {@code docs/phase4/entities_creeper_variants.md}. Stats unchanged except {@code fuseTime = 20}
 * (vanilla's default is 30 - a faster fuse); no AI override.
 * <p>
 * <b>Damage reduction</b> ({@link #hurt}): CE's
 * {@code if(!source.isDamageAbsolute() && !source.isUnblockable()) amount -= 4F;} - flat 4-damage
 * reduction against any non-absolute/non-unblockable source. The research report flagged this exact
 * 1.12.2-to-1.21.1 {@code DamageSource} mapping as unverifiable without a real jar; it resolves
 * cleanly against this port's own already-established tag scheme (confirmed via
 * {@code ModDamageTypeTagsProvider}'s own javadoc: "Ports every builder flag from CE's ...
 * ModDamageSource (setExplosion, setDamageBypassesArmor, setDamageIsAbsolute, ...)"):
 * {@code isDamageAbsolute()} -&gt; {@link ModDamageTypeTags#ABSOLUTE}, {@code isUnblockable()}
 * (CE: set by {@code setDamageBypassesArmor()}) -&gt; {@link DamageTypeTags#BYPASSES_ARMOR}.
 * <p>
 * <b>Explosion</b> ({@link #explodeCreeper()}): CE's is the simplest of the 5 -
 * {@code world.createExplosion(this, x, y+height/2, z, 2F, false)} (plain, portable now) plus spawning
 * an {@code EntityMist} (type {@code Fluids.PHOSGENE}, 10x5 area, 150-tick duration). {@code EntityMist}
 * does not exist in this port yet (confirmed absent from {@code src/} as of this task -
 * {@code docs/phase3/grenades.md} owns it, per the research report's Deferred scope #1) - the mist
 * spawn is a documented one-line forward reference, not reproduced.
 * <p>
 * <b>Drops</b>: CE has no drop overrides at all - it inherits vanilla {@link Creeper}'s own loot table
 * (plain gunpowder, including the skeleton-arrow-&gt;music-disc mechanic, unmodified). Since this is a
 * brand-new {@code EntityType} with no matching datapack {@code loot_table/entities/} json, relying on
 * automatic loot-table-by-registry-name resolution is unverified in this sandbox; the well-known,
 * stable modern vanilla creeper drop shape (0-2 gunpowder) is reproduced directly instead via
 * {@link #dropCustomDeathLoot} for a deterministic result. <b>Known simplification:</b> the
 * skeleton-kill music-disc mechanic and Looting-enchantment count scaling are not reproduced (niche,
 * cosmetic, and not verifiable against decompiled 1.21.1 source in this sandbox).
 */
public class EntityCreeperPhosgene extends Creeper {

    public EntityCreeperPhosgene(EntityType<? extends Creeper> type, Level level) {
        super(type, level);
        CreeperVariantSupport.setFuseTime(this, 20);
    }

    /** See {@link EntityCreeperGold#createAttributes()} for why this reimplements from
     *  {@link Monster#createMonsterAttributes()} rather than calling {@code Creeper.createAttributes()}. */
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.25D);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!source.is(ModDamageTypeTags.ABSOLUTE) && !source.is(DamageTypeTags.BYPASSES_ARMOR)) {
            amount -= 4F;
        }
        if (amount < 0) return false;
        return super.hurt(source, amount);
    }

    @Override
    protected void explodeCreeper() {
        if (this.level().isClientSide) return;

        this.level().explode(this, this.getX(), this.getY() + this.getBbHeight() / 2, this.getZ(),
                2F, false, Level.ExplosionInteraction.TNT);

        // CE also spawns an EntityMist (Fluids.PHOSGENE, 10x5 area, 150-tick duration) here -
        // com.hbm.entity.effect.EntityMist does not exist in this port yet (docs/phase3/grenades.md
        // owns it; docs/phase4/entities_creeper_variants.md Deferred scope #1). One-line forward
        // reference once it lands, not reproduced here.

        this.discard();
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        int count = this.random.nextInt(3);
        if (count > 0) {
            this.spawnAtLocation(new ItemStack(Items.GUNPOWDER, count), 0.0F);
        }
    }
}
