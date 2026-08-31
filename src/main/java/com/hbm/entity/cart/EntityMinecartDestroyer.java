package com.hbm.entity.cart;

import com.hbm.inventory.container.MinecartDestroyerMenu;
import com.hbm.items.tool.CartItems;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Port of CE's {@code com.hbm.entity.cart.EntityMinecartDestroyer} (225 lines) - an 18-slot rolling
 * item-filter/voider cart: every 5 ticks, scans a fixed 5x3.5x5 AABB around itself for {@link
 * ItemEntity} drops matching either slots 0-8 (exact item+data match) or 9-17 (item-only match) and
 * despawns matches on contact - functioning as a rolling filter/voider along a conveyor-adjacent rail
 * line. {@link #canPlaceItem} always returns {@code false} - its 18 slots are a read-only filter
 * template, not a real inventory a hopper could insert into (CE's own {@code isItemValidForSlot}
 * comment, preserved exactly).
 * <p>
 * {@code ItemStack.getItemDamage()} (1.12 metadata) has no 1:1 successor; the closest faithful modern
 * equivalent for "exact same item variant" is {@link ItemStack#isSameItemSameComponents}, used for the
 * strict slots 0-8 (matches this port's own established convention - confirmed already used elsewhere
 * in this codebase for the identical "same item + same data components" comparison).
 */
public class EntityMinecartDestroyer extends EntityMinecartContainerBase implements MenuProvider {

    public static final int SIZE = 18;

    public EntityMinecartDestroyer(EntityType<? extends EntityMinecartDestroyer> type, Level level) {
        super(type, level);
    }

    public EntityMinecartDestroyer(Level level) {
        this(CartEntityTypes.CART_DESTROYER.get(), level);
    }

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return false;
    }

    @Override
    public Item getCartItem() {
        return CartItems.CART_DESTROYER.get();
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (!this.level().isClientSide()) {
            player.openMenu(this, buf -> buf.writeVarInt(this.getId()));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new MinecartDestroyerMenu(id, playerInventory, this);
    }

    @Override
    public void tick() {
        super.tick();
        Level level = this.level();

        if (!level.isClientSide() && this.tickCount % 5 == 0) {
            List<ItemEntity> nearby = level.getEntitiesOfClass(ItemEntity.class, new AABB(
                    getX() - 2.5, getY() - 1.5, getZ() - 2.5,
                    getX() + 2.5, getY() + 2, getZ() + 2.5));

            boolean sound = false;

            outer:
            for (ItemEntity itemEntity : nearby) {
                ItemStack stack = itemEntity.getItem();

                for (int i = 0; i < 9; i++) {
                    ItemStack match = this.items.get(i);
                    if (!match.isEmpty() && ItemStack.isSameItemSameComponents(match, stack)) {
                        itemEntity.discard();
                        sound = true;
                        continue outer;
                    }
                }

                for (int i = 9; i < 18; i++) {
                    ItemStack match = this.items.get(i);
                    if (!match.isEmpty() && match.getItem() == stack.getItem()) {
                        itemEntity.discard();
                        sound = true;
                        continue outer;
                    }
                }
            }

            if (sound) {
                level.playSound(null, getX(), getY(), getZ(), SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, SoundSource.BLOCKS, 0.5F, 0.5F + level.getRandom().nextFloat() * 0.2F);
            }
        }
    }
}
