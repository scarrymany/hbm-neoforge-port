package com.hbm.entity.cart;

import com.hbm.items.tool.CartItems;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * Port of CE's {@code com.hbm.entity.cart.EntityMinecartOre} (27 lines) - the plain "empty" cart
 * skin. Trivial: only {@link #getCartItem()} differs from {@link EntityMinecartNTM}.
 */
public class EntityMinecartOre extends EntityMinecartNTM {

    public EntityMinecartOre(EntityType<? extends EntityMinecartOre> type, Level level) {
        super(type, level);
    }

    public EntityMinecartOre(Level level) {
        this(CartEntityTypes.CART_ORE.get(), level);
    }

    @Override
    public Item getCartItem() {
        return CartItems.CART_ORE.get();
    }
}
