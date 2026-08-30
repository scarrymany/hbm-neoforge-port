package com.hbm.entity.missile;

import com.hbm.items.IngotNuggetItems;
import com.hbm.items.weapon.MissileItems;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Ported from CE's {@code com.hbm.entity.missile.EntityMissileStealth} (34 lines, read in full) -
 * a radar-invisible {@link EntityMissileBaseNT} variant ({@link #canBeSeenBy} always {@code false}).
 * CE's debris ({@code ModItems.bolt}, a meta-variant material item) and rare drop
 * ({@code DictFrame.fromOne(ModItems.powder_ash, EnumAshType.MISC)}) both depend on items not
 * registered in this port yet - debris substituted with an already-real material item ({@code
 * ingot_steel}, closest available analogue), rare drop left {@code null} with a TODO.
 */
public class EntityMissileStealth extends EntityMissileBaseNT {

    public EntityMissileStealth(EntityType<? extends EntityMissileStealth> type, Level level) {
        super(type, level);
    }

    @Override
    public List<ItemStack> getDebris() {
        List<ItemStack> list = new ArrayList<>();
        // TODO(ModItems.bolt, a meta-variant material item not yet registered): CE drops 4x steel bolts here.
        list.add(new ItemStack(IngotNuggetItems.INGOT_STEEL.get(), 4));
        return list;
    }

    @Override
    public ItemStack getMissileItemForInfo() {
        return new ItemStack(MissileItems.MISSILE_STEALTH.get());
    }

    @Override
    public boolean canBeSeenBy(Object radar) {
        return false;
    }

    @Override
    public void onMissileImpact(HitResult mop) {
        this.explodeStandard(20F, 24, false);
        // TODO(ExplosionCreator.composeEffectStandard, Phase 5): VFX only.
    }

    @Override
    public ItemStack getDebrisRareDrop() {
        // TODO(ModItems.powder_ash / EnumAshType.MISC, not yet registered in this port).
        return null;
    }
}
