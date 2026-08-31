package com.hbm.items;

import com.hbm.lib.HBMSoundHandler;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Base class for items containing an inventory (crates, containment boxes, the toolbox).
 *
 * CE persisted the slot contents as a hand-rolled NBTTagList written into the stack's own tag
 * compound via ItemStackUtil, plus a checkNBT() safety valve that gzip-compressed the tag,
 * measured its size, and ejected the whole inventory into the world if it exceeded 6000 bytes
 * (a real concern with 1.12's uncapped raw NBT). Persistence is rewritten around vanilla's own
 * DataComponents.CONTAINER / ItemContainerContents component instead of a custom NBT list, per
 * the "components, not raw NBT" rule; ItemStackUtil is no longer needed for this class.
 *
 * The 6000-byte eject-to-world safety valve is dropped rather than reimplemented: it has no
 * direct component-era equivalent (there is no cheap byte-size probe on a component value the
 * way there was on a compressed NBT blob), and whether an overflow safety valve is still needed
 * at all - and what it should look like - is a gameplay decision for whoever ports the concrete
 * container items (crates, containment boxes, toolbox) in a later phase.
 *
 * ItemStackHandler's existence/shape under NeoForge 21.1.228 is inferred from long-standing
 * Forge/NeoForge convention (net.neoforged.neoforge.items.ItemStackHandler), not confirmed via a
 * live usage in the Neo Edition reference tree - flagged for the integration step to double-check.
 */
public class ItemInventory extends ItemStackHandler {

    public final Player player;
    public final ItemStack target;

    public ItemInventory(Player player, ItemStack target, int size) {
        super(size);
        this.player = player;
        this.target = target;

        ItemContainerContents contents = target.get(DataComponents.CONTAINER);
        if (contents != null) {
            NonNullList<ItemStack> loaded = NonNullList.withSize(size, ItemStack.EMPTY);
            contents.copyInto(loaded);
            for (int i = 0; i < size; i++) {
                setStackInSlot(i, loaded.get(i));
            }
        }
    }

    @Override
    protected void onContentsChanged(int slot) {
        // Clean zero-sized stacks just in case
        for (int i = 0; i < getSlots(); ++i) {
            ItemStack stack = getStackInSlot(i);
            if (!stack.isEmpty() && stack.getCount() <= 0) {
                setStackInSlot(i, ItemStack.EMPTY);
            }
        }

        NonNullList<ItemStack> current = NonNullList.withSize(getSlots(), ItemStack.EMPTY);
        for (int i = 0; i < getSlots(); i++) {
            current.set(i, getStackInSlot(i));
        }
        target.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(current));
    }

    public void openInventory() {
        if (player == null) return;
        player.level().playSound(null, player.blockPosition(), HBMSoundHandler.crateOpen.get(), SoundSource.BLOCKS, 1.0F, 0.8F);
    }

    public void closeInventory() {
        if (player == null) return;
        player.level().playSound(null, player.blockPosition(), HBMSoundHandler.crateClose.get(), SoundSource.BLOCKS, 1.0F, 0.8F);
    }
}
