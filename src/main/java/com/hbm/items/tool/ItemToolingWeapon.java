package com.hbm.items.tool;

import com.hbm.api.block.IToolable.ToolType;

/**
 * The one melee-capable {@code ItemTooling} variant ({@code wrench_archineer}), ported from CE's
 * {@code com.hbm.items.tool.ItemToolingWeapon}: same {@link IToolable}-dispatching {@code useOn} as
 * {@link ItemTooling} (CE: {@code extends ItemTooling}), plus an attack-damage bonus applied through
 * this item's {@code Item.Properties#attributes(...)} at registration time (CE: an
 * {@code ATTACK_DAMAGE} entry added to {@code getAttributeModifiers}'s multimap) - see
 * {@code CouplingToolItems#toolingWeapon} for the exact {@code ItemAttributeModifiers} builder call,
 * following this port's own {@code GearItems#weaponAttributesWithSlow} precedent for the idiom.
 * <p>
 * <b>Not ported</b>: CE's {@code ItemToolingWeapon.hitEntity} knockback-on-hit special case is
 * guarded by {@code this == ModItems.wrench} - i.e. it only ever fires for the plain
 * {@link ItemWrench} instance, never for an actual {@code ItemToolingWeapon} instance (dead code in
 * CE's own class, since {@code ModItems.wrench} is never an {@code ItemToolingWeapon}) - correctly
 * omitted here rather than mistranslated into a real behavior this class never had.
 */
public class ItemToolingWeapon extends ItemTooling {

    public ItemToolingWeapon(ToolType type, Properties properties) {
        super(type, properties);
    }
}
