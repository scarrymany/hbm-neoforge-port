package com.hbm.entity.mob.glyphid;

import com.hbm.entity.effect.EntityMist;
import com.hbm.entity.mob.glyphid.GlyphidStats.StatBundle;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.weapon.grenade.GrenadeItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** CE: {@code EntityGlyphidBehemoth} (117 lines). Acid breath skipped (no EntityChemical). */
public class EntityGlyphidBehemoth extends EntityGlyphid {

    public EntityGlyphidBehemoth(EntityType<? extends EntityGlyphidBehemoth> type, Level level) {
        super(type, level);
    }

    public static net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder createAttributes() {
        return EntityGlyphidBombardier.MonsterAttrs.of(GlyphidStats.getStats().getBehemoth());
    }

    @Override
    public ResourceLocation getSkin() {
        return ResourceLocation.fromNamespaceAndPath("hbm", "textures/entity/glyphid_behemoth.png");
    }

    @Override
    public double getGlyphidScale() {
        return 1.5D;
    }

    @Override
    public StatBundle getStats() {
        return GlyphidStats.getStats().statsBehemoth;
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!this.level().isClientSide) {
            EntityMist.spawn(this.level(), this.getX(), this.getY(), this.getZ(),
                    Fluids.SULFURIC_ACID, 10, 4, 120);
        }
    }

    @Override
    protected void dropFromLootTable(DamageSource damageSource, boolean attackedRecently) {
        this.spawnAtLocation(new ItemStack(GrenadeItems.GLYPHID_GLAND.get()));
        super.dropFromLootTable(damageSource, attackedRecently);
    }

    @Override
    public boolean isArmorBroken(float amount) {
        return this.random.nextInt(100) <= Math.min(Math.pow(amount * 0.15, 2), 100);
    }
}
