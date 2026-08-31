package com.hbm.items.special;

import com.hbm.config.WeaponConfig;
import com.hbm.entity.effect.EntityBlackHole;
import com.hbm.entity.effect.EntityRagingVortex;
import com.hbm.entity.effect.EntityVortex;
import com.hbm.explosion.ExplosionChaos;
import com.hbm.explosion.ExplosionLarge;
import com.hbm.items.ItemBase;
import com.hbm.util.i18n.I18nUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Ported from CE's {@code com.hbm.items.special.ItemDrop} (244 lines, read in full) - <b>only</b> the
 * singularity/xen/antimatter half (see {@link DropEffect}), per
 * docs/phase4/entities_vortex_gravity_wells.md's Headline finding 1. The {@code detonator_deadman}/
 * {@code detonator_de} half of CE's monolithic class already has its own dedicated classes
 * ({@link ItemDeadmanDetonator}/{@link ItemDeadMansExplosive}, per {@code docs/phase3/
 * scattered_military_items.md}'s explicit split) and CE's {@code beta} flavor item's silent-vanish
 * branch belongs wherever {@code ModItems.beta} is otherwise registered - neither is reproduced here.
 * <p>
 * All 8 real branches trigger only when {@code entityItem.onGround() || entityItem.isOnFire()} (a
 * thrown/dropped item resting or burning, not the instant it's dropped), and the entity always
 * discards itself afterward - matching CE's real "one-shot on landing" semantics.
 * <p>
 * {@code capsule_xen}/{@code crystal_xen} call {@link ExplosionChaos#floater}/{@link ExplosionChaos#move}
 * directly with no entity spawn at all (CE passes the dropped item's thrower as the {@code detonator}/
 * shooter argument via a server player-list username lookup that has no confirmed 1.21 equivalent -
 * the same documented gap already accepted by {@link ItemDeadmanDetonator}/{@code HazardTypeUnstable}
 * for an identical case; {@code null} is passed instead, which both methods' own bodies never actually
 * read anyway).
 */
public class ItemDrop extends ItemBase {

    public enum DropEffect {
        SINGULARITY(List.of(
                "You may be asking:",
                "\"But HBM, a manifold with an undefined",
                "state of spacetime? How is this possible?\"",
                "Long answer short:",
                "\"I have no idea!\"")),
        SINGULARITY_COUNTER_RESONANT(List.of(
                "Nullifies resonance of objects in",
                "non-euclidean space, creates variable",
                "gravity well. Spontaneously spawns",
                "tesseracts. If a tesseract happens to",
                "appear near you, do not look directly",
                "at it.")),
        SINGULARITY_SUPER_HEATED(List.of(
                "Continuously heats up matter by",
                "resonating every planck second.",
                "Tends to catch fire or to create",
                "small plamsa arcs. Not edible.")),
        SINGULARITY_SPARK(List.of()),
        BLACK_HOLE(List.of(
                "Contains a regular singularity",
                "in the center. Large enough to",
                "stay stable. It's not the end",
                "of the world as we know it,",
                "and I don't feel fine.")),
        CAPSULE_XEN(List.of()),
        CRYSTAL_XEN(List.of()),
        PELLET_ANTIMATTER(List.of(
                "Very heavy antimatter cluster.",
                "Gets rid of black holes."));

        private final List<String> flavorText;

        DropEffect(List<String> flavorText) {
            this.flavorText = flavorText;
        }
    }

    private final DropEffect effect;

    public ItemDrop(Properties properties, DropEffect effect) {
        super(properties);
        this.effect = effect;
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entityItem) {
        if (!(entityItem.onGround() || entityItem.isOnFire())) return false;

        Level level = entityItem.level();
        if (!level.isClientSide()) {
            double x = entityItem.getX();
            double y = entityItem.getY();
            double z = entityItem.getZ();

            switch (effect) {
                case SINGULARITY -> {
                    if (WeaponConfig.DROP_SINGULARITY.get()) spawnVortex(level, x, y, z, 1.5F);
                }
                case SINGULARITY_COUNTER_RESONANT -> {
                    if (WeaponConfig.DROP_SINGULARITY.get()) spawnVortex(level, x, y, z, 2.5F);
                }
                case SINGULARITY_SUPER_HEATED -> {
                    // CE gives this item the exact same drop effect as singularity_counter_resonant
                    // (identical size) despite different tooltips/flavor text - not a typo, matched
                    // per the research report's Headline finding 1.
                    if (WeaponConfig.DROP_SINGULARITY.get()) spawnVortex(level, x, y, z, 2.5F);
                }
                case BLACK_HOLE -> {
                    if (WeaponConfig.DROP_SINGULARITY.get()) {
                        EntityBlackHole bh = new EntityBlackHole(level, 1.5F);
                        bh.setPos(x, y, z);
                        level.addFreshEntity(bh);
                    }
                }
                case SINGULARITY_SPARK -> {
                    if (WeaponConfig.DROP_SINGULARITY.get()) {
                        EntityRagingVortex vortex = new EntityRagingVortex(level, 3.5F);
                        vortex.setPos(x, y, z);
                        level.addFreshEntity(vortex);
                    }
                }
                case CAPSULE_XEN -> {
                    if (WeaponConfig.DROP_CRYSTAL.get()) {
                        ExplosionChaos.floater(level, null, (int) x, (int) y, (int) z, 3, 8);
                        ExplosionChaos.move(level, (int) x, (int) y, (int) z, 3, 0, 8, 0);
                    }
                }
                case CRYSTAL_XEN -> {
                    if (WeaponConfig.DROP_CRYSTAL.get()) {
                        ExplosionChaos.floater(level, null, (int) x, (int) y, (int) z, 25, 75);
                        ExplosionChaos.move(level, (int) x, (int) y, (int) z, 25, 0, 75, 0);
                    }
                }
                case PELLET_ANTIMATTER -> {
                    if (WeaponConfig.DROP_CELL.get()) {
                        ExplosionLarge.explodeFire(level, null, x, y, z, 100, true, true, true);
                    }
                }
            }
        }

        entityItem.discard();
        return true;
    }

    private static void spawnVortex(Level level, double x, double y, double z, float size) {
        EntityVortex vortex = new EntityVortex(level, size);
        vortex.setPos(x, y, z);
        level.addFreshEntity(vortex);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        for (String line : effect.flavorText) {
            tooltip.add(Component.literal(line));
        }
        tooltip.add(Component.literal("[" + I18nUtil.resolveKey("trait.drop") + "]").withStyle(ChatFormatting.RED));
    }
}
