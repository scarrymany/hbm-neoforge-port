package com.hbm.entity.mob.glyphid;

import com.hbm.entity.mob.glyphid.GlyphidStats.StatBundle;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;

/** CE: {@code EntityGlyphidBlaster} (64 lines). */
public class EntityGlyphidBlaster extends EntityGlyphidBombardier {

    public EntityGlyphidBlaster(EntityType<? extends EntityGlyphidBlaster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return EntityGlyphidBombardier.MonsterAttrs.of(GlyphidStats.getStats().getBlaster());
    }

    @Override
    public ResourceLocation getSkin() {
        return ResourceLocation.fromNamespaceAndPath("hbm", "textures/entity/glyphid_blaster.png");
    }

    @Override
    public double getGlyphidScale() {
        return 1.25D;
    }

    @Override
    public StatBundle getStats() {
        return GlyphidStats.getStats().statsBlaster;
    }

    @Override
    public boolean isArmorBroken(float amount) {
        return this.random.nextInt(100) <= Math.min(Math.pow(amount * 0.25, 2), 100);
    }

    /** CE {@code EntityGlyphidBlaster} lines 46-62. */
    @Override
    public float getBombDamage() {
        return 15F;
    }

    @Override
    public int getBombCount() {
        return 10;
    }

    @Override
    public float getSpreadMult() {
        return 0.5F;
    }

    @Override
    public double getV0() {
        return 1.25D;
    }
}
