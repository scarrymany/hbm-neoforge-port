package com.hbm.entity.missile;

import com.hbm.items.PlateCrystalWasteItems;
import com.hbm.items.weapon.MissileItems;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Ported from CE's {@code com.hbm.entity.missile.EntityMissileShuttle} (82 lines, read in full) -
 * the shuttle-launch payload preset. CE's own impact uses {@code ExplosionNT} (an older, unported
 * explosion family - see {@code EntityMissileTier3}'s javadoc) with both {@code NOSOUND}/{@code
 * NOPARTICLE} attributes set, plus a networked mushroom-cloud particle burst and a custom explosion
 * sound - all VFX/unported-engine concerns. Substituted with a real vanilla explosion of the same
 * declared strength/resolution (20 blocks, matching CE's {@code overrideResolution(64)} call's
 * intent of "a large but soft-edged crater") so impact still has a real destructive effect rather
 * than none at all - documented, not silent.
 */
public class EntityMissileShuttle extends EntityMissileBaseNT {

    public EntityMissileShuttle(EntityType<? extends EntityMissileShuttle> type, Level level) {
        super(type, level);
    }

    @Override
    public void onMissileImpact(HitResult mop) {
        // TODO(ExplosionNT/ExAttrib, networked mushroom-cloud particle burst, custom sound - not
        // ported, see class javadoc): substituted with a real vanilla explosion of equivalent
        // strength so impact keeps a real destructive effect.
        level().explode(this, getX() + 0.5, getY() + 0.5, getZ() + 0.5, 20.0F, false, Level.ExplosionInteraction.TNT);
    }

    @Override
    public List<ItemStack> getDebris() {
        List<ItemStack> list = new ArrayList<>();
        list.add(new ItemStack(PlateCrystalWasteItems.PLATE_STEEL.get(), 8));
        // TODO(ModItems.thruster_medium/canister_empty, not yet registered in this port).
        list.add(new ItemStack(Blocks.GLASS_PANE, 2));
        return list;
    }

    @Override
    public ItemStack getDebrisRareDrop() {
        return new ItemStack(MissileItems.MISSILE_GENERIC.get());
    }

    @Override
    public String getTranslationKey() {
        return "radar.target.shuttle";
    }

    @Override
    public int getBlipLevel() {
        return 3;
    }

    @Override
    public ItemStack getMissileItemForInfo() {
        return new ItemStack(MissileItems.MISSILE_SHUTTLE.get());
    }
}
