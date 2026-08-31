package com.hbm.entity.cart;

import com.hbm.items.tool.CartItems;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * Port of CE's {@code com.hbm.entity.cart.EntityMinecartSemtex} (40 lines) - confirmed <b>purely
 * cosmetic</b> by {@code docs/phase4/entities_vehicles_aircraft.md}'s Headline finding #3 (a real
 * correction to {@code docs/phase1/items_tool.md}, which had flagged this as "an explosive cart"
 * needing explosives content): no fuze, no explosion, no unique behavior of any kind beyond a
 * different item source and a different (Phase 5, client-only) render texture. Do <b>not</b> add
 * explosion/fuze logic here - {@link EntityMinecartCrate}'s NBT-size safety valve is the only cart in
 * this family that actually explodes, and that is a data-safety mechanic, not a themed one.
 */
public class EntityMinecartSemtex extends EntityMinecartNTM {

    public EntityMinecartSemtex(EntityType<? extends EntityMinecartSemtex> type, Level level) {
        super(type, level);
    }

    public EntityMinecartSemtex(Level level) {
        this(CartEntityTypes.CART_SEMTEX.get(), level);
    }

    @Override
    public Item getCartItem() {
        return CartItems.CART_SEMTEX.get();
    }
}
