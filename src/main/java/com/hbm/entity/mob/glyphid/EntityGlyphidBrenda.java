package com.hbm.entity.mob.glyphid;

import com.hbm.entity.effect.EntityMist;
import com.hbm.entity.mob.glyphid.GlyphidStats.StatBundle;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.weapon.grenade.GrenadeItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** CE: {@code EntityGlyphidBrenda} (78 lines) — pheromone mist + 12 grunt spawn on death. */
public class EntityGlyphidBrenda extends EntityGlyphid {

    public EntityGlyphidBrenda(EntityType<? extends EntityGlyphidBrenda> type, Level level) {
        super(type, level);
    }

    public static net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder createAttributes() {
        return EntityGlyphidBombardier.MonsterAttrs.of(GlyphidStats.getStats().getBrenda());
    }

    @Override
    public ResourceLocation getSkin() {
        return ResourceLocation.fromNamespaceAndPath("hbm", "textures/entity/glyphid_brenda.png");
    }

    @Override
    public double getGlyphidScale() {
        return 2D;
    }

    @Override
    public StatBundle getStats() {
        return GlyphidStats.getStats().statsBrenda;
    }

    @Override
    public boolean isArmorBroken(float amount) {
        return this.random.nextInt(100) <= Math.min(Math.pow(amount * 0.12, 2), 100);
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!this.level().isClientSide) {
            EntityMist.spawn(this.level(), this.getX(), this.getY(), this.getZ(),
                    Fluids.PHEROMONE, 14, 6, 80);
            for (int i = 0; i < 12; ++i) {
                EntityGlyphid glyphid = new EntityGlyphid(GlyphidEntityTypes.GLYPHID.get(), this.level());
                glyphid.moveTo(this.getX(), this.getY() + 0.5D, this.getZ(), this.random.nextFloat() * 360.0F, 0.0F);
                this.level().addFreshEntity(glyphid);
                glyphid.move(MoverType.SELF, new net.minecraft.world.phys.Vec3(this.random.nextGaussian(), 0, this.random.nextGaussian()));
            }
        }
    }

    @Override
    protected void dropFromLootTable(DamageSource damageSource, boolean attackedRecently) {
        super.dropFromLootTable(damageSource, attackedRecently);
        if (this.random.nextInt(3) == 0) {
            this.spawnAtLocation(new ItemStack(GrenadeItems.GLYPHID_GLAND.get()));
        }
    }
}
