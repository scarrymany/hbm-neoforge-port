package com.hbm.items.weapon;

import com.hbm.entity.grenade.EntityDisperserCanister;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.machine.ItemFluidTank;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Port of CE's {@code com.hbm.items.weapon.ItemDisperser} (67 lines) - two registered instances,
 * {@code disperser_canister} (2000 mb) and {@code glyphid_gland} (4000 mb), differing only in
 * capacity/creative-tab fluid filtering (see {@link com.hbm.items.weapon.grenade.GrenadeItems} for
 * the two concrete registrations). Extends this port's own {@link ItemFluidTank} (data-component
 * fluid/amount state) rather than CE's damage-value-keyed one; throwing reads whichever
 * {@link FluidType} the tank stack currently carries.
 * <p>
 * CE's creative-tab {@code getSubItems} (one stack per compatible {@link FluidType}) is not ported -
 * {@code com.hbm.creativetabs.CreativeTabContents} only supports one default-state stack per
 * registered item today (no multi-variant enumeration hook), and extending that shared helper is out
 * of this package's scope; {@link com.hbm.items.weapon.grenade.GrenadeItems} registers one plain,
 * unfilled stack of each disperser item to the WEAPON tab instead. Filling a tank stack with a
 * specific fluid still works via {@link ItemFluidTank#fill}, just not as a creative-tab shortcut.
 */
public class ItemDisperser extends ItemFluidTank {

    public ItemDisperser(int capacity, Properties properties) {
        super(capacity, properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!player.isCreative()) {
            stack.shrink(1);
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SNOWBALL_THROW, SoundSource.NEUTRAL,
                0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));

        if (!level.isClientSide()) {
            FluidType fluid = ItemFluidTank.getFluidType(stack);
            if (fluid == null) fluid = Fluids.NONE;

            EntityDisperserCanister canister = new EntityDisperserCanister(level, player, hand);
            canister.setType(this);
            canister.setFluid(fluid);
            level.addFreshEntity(canister);
        }

        return InteractionResultHolder.success(stack);
    }
}
