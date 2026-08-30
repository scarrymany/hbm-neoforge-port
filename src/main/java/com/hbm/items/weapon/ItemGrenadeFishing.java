package com.hbm.items.weapon;

import com.hbm.explosion.vanillant.ExplosionVNT;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/**
 * Port of CE's {@code com.hbm.items.weapon.ItemGrenadeFishing} ({@code stick_dynamite_fishing}, 76
 * lines). See {@link ItemGrenadeDynamite}'s javadoc for why the blast itself is routed through
 * {@link ExplosionVNT} rather than a raw vanilla explosion call; the {@code null} exploder passed
 * here (vs. {@code grenade} for plain dynamite) is a real CE asymmetry, preserved faithfully rather
 * than "fixed".
 * <p>
 * <b>Blocking dependency, stubbed (not silently dropped):</b> CE's loot-scatter payload
 * ({@code LootTableList.GAMEPLAY_FISHING} / {@code LootContext.Builder} / {@code EntityItemBuoyant})
 * uses 1.12-era loot-table and buoyant-item-entity APIs with no confirmed 1.21.1 replacement in this
 * pass - {@code docs/phase3/grenades.md} explicitly deferred resolving the modern
 * {@code ResourceKey<LootTable>}/{@code LootParams.Builder} call shape to whichever pass already
 * touches loot tables generally, and {@code EntityItemBuoyant} is not itself ported anywhere in this
 * tree (grepped). The real, non-block-destroying part of this item's behavior (the explosion) is
 * fully wired below.
 */
public class ItemGrenadeFishing extends ItemGenericGrenade {

    public ItemGrenadeFishing(int fuse, Properties properties) {
        super(fuse, properties);
    }

    @Override
    public void explode(Entity grenade, LivingEntity thrower, Level level, double x, double y, double z) {
        new ExplosionVNT(level, x, y + 0.25D, z, 3F, null).makeStandard().explode();

        // forward reference: scatter up to 15 items (LootTableList.GAMEPLAY_FISHING roll) into water
        // blocks within a 15x15x15 volume around the blast, floated via EntityItemBuoyant - see class
        // javadoc. No loot is scattered until that dependency chain lands.
    }

    @Override
    public int getMaxTimer() {
        return 60;
    }

    @Override
    public double getBounceMod() {
        return 0.5D;
    }
}
