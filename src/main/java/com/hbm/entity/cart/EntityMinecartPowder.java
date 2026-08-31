package com.hbm.entity.cart;

import com.hbm.items.tool.CartItems;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * Port of CE's {@code com.hbm.entity.cart.EntityMinecartPowder} (38 lines) - only {@link
 * #getCartItem()} and (client-only, Phase 5) a texture swap differ from {@link EntityMinecartNTM}.
 */
public class EntityMinecartPowder extends EntityMinecartNTM {

    public EntityMinecartPowder(EntityType<? extends EntityMinecartPowder> type, Level level) {
        super(type, level);
    }

    public EntityMinecartPowder(Level level) {
        this(CartEntityTypes.CART_POWDER.get(), level);
    }

    @Override
    public Item getCartItem() {
        return CartItems.CART_POWDER.get();
    }
}
