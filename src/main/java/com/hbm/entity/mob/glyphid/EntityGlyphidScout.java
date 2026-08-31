package com.hbm.entity.mob.glyphid;

import com.hbm.entity.mob.glyphid.GlyphidStats.StatBundle;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;

/**
 * CE: {@code EntityGlyphidScout} (364 lines). Hive construction / {@code EntityWaypoint} /
 * {@code GlyphidHive} skipped — poison melee + scout stats remain playable.
 */
public class EntityGlyphidScout extends EntityGlyphid {

    public EntityGlyphidScout(EntityType<? extends EntityGlyphidScout> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return EntityGlyphidBombardier.MonsterAttrs.of(GlyphidStats.getStats().getScout());
    }

    @Override
    public boolean doHurtTarget(Entity victim) {
        if (super.doHurtTarget(victim) && victim instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.POISON, 10 * 20, 3));
            return true;
        }
        return false;
    }

    @Override
    public ResourceLocation getSkin() {
        return ResourceLocation.fromNamespaceAndPath("hbm", "textures/entity/glyphid_scout.png");
    }

    @Override
    public double getGlyphidScale() {
        return 0.75D;
    }

    @Override
    public StatBundle getStats() {
        return GlyphidStats.getStats().statsScout;
    }

    @Override
    public boolean isArmorBroken(float amount) {
        return this.random.nextInt(100) <= Math.min(Math.pow(amount, 2), 100);
    }
}
