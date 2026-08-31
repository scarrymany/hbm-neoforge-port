package com.hbm.entity.mob.glyphid;

import com.hbm.entity.effect.EntityMist;
import com.hbm.entity.mob.glyphid.GlyphidStats.StatBundle;
import com.hbm.entity.projectile.EntityChemical;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.weapon.grenade.GrenadeItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * CE {@code EntityGlyphidBehemoth} (117 lines) —
 * {@code @AutoRegister(name = "entity_glyphid_behemoth")} at line 24.
 * Acid breath: {@code onUpdate}/{@code acidAttack} CE lines 58-101.
 */
public class EntityGlyphidBehemoth extends EntityGlyphid {

    public int timer = 120;
    int breathTime = 0;

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
    public void tick() {
        super.tick();
        LivingEntity e = this.getTarget();
        if (e == null) {
            timer = 120;
            breathTime = 0;
        } else if (breathTime > 0) {
            if (!this.swinging) {
                this.swing(InteractionHand.MAIN_HAND);
            }
            acidAttack();
            this.setYRot(this.yRotO);
            breathTime--;
        } else if (--timer <= 0) {
            breathTime = 120;
            timer = 120;
        }
    }

    /** CE {@code acidAttack()} lines 93-101. */
    public void acidAttack() {
        if (!this.level().isClientSide && this.getTarget() instanceof LivingEntity
                && this.distanceTo(this.getTarget()) < 20) {
            this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 2 * 20, 6));
            EntityChemical chem = new EntityChemical(this.level(), this, 0, 0, 0);
            chem.setFluid(Fluids.SULFURIC_ACID);
            this.level().addFreshEntity(chem);
        }
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
