package com.hbm.items.tool;

/**
 * Universal "master key" variant of {@link ItemKeyPin} (CE registry name {@code key_red}), ported
 * from CE's {@code com.hbm.items.tool.ItemKey} (a trivial one-line subclass in CE too). Any lock
 * dispatch code that special-cases "holding a master key" (see
 * {@link com.hbm.api.block.ILockable#canAccess}'s {@code universalKey} parameter) should check
 * {@code stack.getItem() instanceof ItemKey} rather than pin equality, matching CE's own
 * {@code stack.getItem() == ModItems.key_red} identity check.
 */
public class ItemKey extends ItemKeyPin {

    public ItemKey(Properties properties) {
        super(properties);
    }
}
