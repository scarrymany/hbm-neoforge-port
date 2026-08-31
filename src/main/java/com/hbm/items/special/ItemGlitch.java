package com.hbm.items.special;

import com.hbm.entity.effect.EntityVortex;
import com.hbm.explosion.ExplosionChaos;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Port of CE's {@code ItemGlitch} ({@code glitch}) - registration only, per
 * docs/phase1/items_special.md's explicit guidance. CE's {@code onItemRightClick} rolls a 31-case
 * random-effect table referencing dozens of items/blocks/entities spanning missiles, nukes, treasure
 * blocks and potions across every future phase; implementing it faithfully needs those dependencies
 * to exist first.
 * <p>
 * Two of those 31 cases are wired here, per docs/phase4/entities_vortex_gravity_wells.md's Headline
 * finding 5 (the report's own "2 real call sites into this package's own scope"): CE's case 14
 * ({@link ExplosionChaos#burn}, a 5-block-radius burn centered on the player) and case 27 (an
 * {@link EntityVortex} of size {@code 2.5F} spawned 15 blocks below the player). CE's exact
 * {@code itemRand.nextInt(31)} roll range/case numbering is preserved so these two fire at the real
 * 1-in-31 odds each; the other 29 cases (chat messages, meteor-treasure/kit/ammo drops,
 * {@code EntityBoxcar}/{@code EntityMeteor} spawns, RESISTANCE/STRENGTH/SLOWNESS potion effects,
 * {@code ExplosionLarge.spawnBurst}) all reference dependencies this port has not built yet and are
 * intentionally left as a no-op default branch - incremental completion of the remaining table is
 * explicitly out of this package's own scope (gravity wells + {@code ExplosionChaos} only), matching
 * Phase 1's "implement/port the effect table incrementally... spanning every future phase" guidance.
 */
public class ItemGlitch extends Item {

    public ItemGlitch(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // CE: stack.damageItem(5, player), unconditionally on every use regardless of which case
        // rolls. SpecialItems already registers this item at durability(1) (CE's own
        // setMaxDamage(1)), so any positive damage amount breaks it after a single use.
        stack.hurtAndBreak(5, player, LivingEntity.getSlotForHand(hand));

        if (!level.isClientSide()) {
            switch (level.getRandom().nextInt(31)) {
                case 14 -> ExplosionChaos.burn(level, null,
                        new BlockPos((int) player.getX(), (int) player.getY(), (int) player.getZ()), 5);
                case 27 -> {
                    EntityVortex vortex = new EntityVortex(level, 2.5F);
                    vortex.setPos(player.getX(), player.getY() - 15, player.getZ());
                    level.addFreshEntity(vortex);
                }
                default -> {
                    // TODO(docs/phase1/items_special.md): CE's other 29 cases - chat messages;
                    // meteor-treasure/nuke-kit/ammo drops; EntityBoxcar/EntityMeteor spawns;
                    // RESISTANCE/STRENGTH/SLOWNESS potion effects; ExplosionLarge.spawnBurst - are
                    // left unimplemented here, out of this package's own scope. Complete
                    // incrementally as their own dependencies land in later phases.
                }
            }
        }

        return InteractionResultHolder.pass(stack);
    }
}
