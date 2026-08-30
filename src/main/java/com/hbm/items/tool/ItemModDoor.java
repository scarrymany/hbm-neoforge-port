package com.hbm.items.tool;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

/**
 * Item wrapper that places a custom door block - the door-item analogue of vanilla
 * {@code DoorItem}. Ported from CE's {@code com.hbm.items.tool.ItemModDoor} ({@code door_metal},
 * {@code door_office}, {@code door_bunker}, {@code door_red}).
 *
 * <p><b>Stubbed pending an accessible registered door block.</b> CE's {@code onItemUse}
 * identity-checks {@code this} against {@code ModItems.door_metal}/{@code door_office}/
 * {@code door_red} to pick a matching {@code ModBlocks.door_*} block and places it via vanilla
 * {@code DoorBlock} two-tall placement logic. A door block <em>class</em> already exists in this
 * port ({@code com.hbm.blocks.generic.BlockModDoor}, a plain {@link net.minecraft.world.level.block.DoorBlock}
 * subclass), but no instance of it is registered anywhere in {@code ModBlocks} yet - only the class
 * itself has landed so far (referenced today only by {@code BlockNTMTrapdoor}'s shared
 * {@code BlockSetType}). Per the port plan's "stub with a documented TODO rather than blocking"
 * rule, this item is registered now (one instance per CE door variant, distinguished by name/texture
 * only) with its block-placing behavior deferred; vanilla's own {@code DoorItem} already provides
 * correct two-tall placement once a matching registered {@code DoorBlock} exists to hand it, so the
 * real follow-up here is likely "register the door blocks and use vanilla {@code DoorItem}
 * directly" rather than porting CE's {@code onItemUse} verbatim.
 */
public class ItemModDoor extends Item {

    public ItemModDoor(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        // TODO(cross-area follow-up): once a matching door block is registered in ModBlocks, either
        // port CE's placement logic here or (preferably) replace this class's registrations with
        // vanilla DoorItem instances pointed at that block.
        return InteractionResult.PASS;
    }
}
