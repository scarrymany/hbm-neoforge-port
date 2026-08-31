package com.hbm.entity.mob.glyphid;

import com.hbm.entity.mob.glyphid.GlyphidStats.StatBundle;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/**
 * CE: {@code EntityGlyphidBombardier} (122 lines). Acid-bomb projectile is not in this port —
 * melee + bombardier stats until {@code EntityAcidBomb} lands.
 */
public class EntityGlyphidBombardier extends EntityGlyphid {

    public EntityGlyphidBombardier(EntityType<? extends EntityGlyphidBombardier> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        StatBundle s = GlyphidStats.getStats().getBombardier();
        return MonsterAttrs.of(s);
    }

    @Override
    public ResourceLocation getSkin() {
        return ResourceLocation.fromNamespaceAndPath("hbm", "textures/entity/glyphid_bombardier.png");
    }

    @Override
    public StatBundle getStats() {
        return GlyphidStats.getStats().statsBombardier;
    }

    static final class MonsterAttrs {
        static AttributeSupplier.Builder of(StatBundle s) {
            return net.minecraft.world.entity.monster.Monster.createMonsterAttributes()
                    .add(Attributes.MAX_HEALTH, s.health())
                    .add(Attributes.MOVEMENT_SPEED, s.speed())
                    .add(Attributes.ATTACK_DAMAGE, s.damage())
                    .add(Attributes.FOLLOW_RANGE, 16.0D);
        }
    }
}
