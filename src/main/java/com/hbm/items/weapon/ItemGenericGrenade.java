package com.hbm.items.weapon;

import com.hbm.entity.grenade.EntityGrenadeBouncyGeneric;
import com.hbm.entity.grenade.EntityGrenadeImpactGeneric;
import com.hbm.items.ItemBase;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Port of CE's {@code com.hbm.items.weapon.ItemGenericGrenade} (62 lines, abstract) - the legacy
 * single-purpose grenade family's shared item behavior. {@code fuse} is stored in seconds exactly as
 * CE has it ({@link #getMaxTimer()} converts to ticks); {@code fuse == -1} means "detonate on first
 * impact" ({@link EntityGrenadeImpactGeneric}) rather than "bounce until the timer expires"
 * ({@link EntityGrenadeBouncyGeneric}) - see {@code docs/phase3/grenades.md} for why the
 * {@code -1} path is currently unreachable by any concrete subclass but kept for parity.
 */
public class ItemGenericGrenade extends ItemBase {

    protected final int fuse;

    public ItemGenericGrenade(int fuse, Properties properties) {
        super(properties.stacksTo(16));
        this.fuse = fuse;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!player.isCreative()) {
            stack.shrink(1);
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS,
                0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));

        if (!level.isClientSide()) {
            if (fuse == -1) {
                level.addFreshEntity(new EntityGrenadeImpactGeneric(level, player, hand).setType(this));
            } else {
                level.addFreshEntity(new EntityGrenadeBouncyGeneric(level, player, hand).setType(this));
            }
        }

        return InteractionResultHolder.success(stack);
    }

    /** Per-subclass detonation payload - CE's own override point, empty by default. */
    public void explode(Entity grenade, LivingEntity thrower, Level level, double x, double y, double z) {
    }

    public int getMaxTimer() {
        return this.fuse * 20;
    }

    public double getBounceMod() {
        return 0.5D;
    }
}
