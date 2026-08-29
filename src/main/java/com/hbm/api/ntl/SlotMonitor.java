package com.hbm.api.ntl;

import com.hbm.api.ntl.StackCache.CacheSlot;
import com.hbm.uninos.networkproviders.PneumaticNetwork;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.LinkedHashSet;

/**
 * Slot monitors are the access points of the system to the actual items stacks.
 * Each storage unit needs to provide all its stacks in the form of slot monitors
 * to the storage system. The slot monitor's main functionality is to detect changes
 * in the underlying stack, so that it can notify and update the system's wider
 * data structures.
 * <p>
 * Ported from CE's meta+NBTTagCompound identity model to a components-based one: 1.21 items no
 * longer carry metadata, and NBT compounds are no longer how a stack's non-default data is
 * represented, so type identity is now (Item, DataComponentPatch) instead of (Item, meta, NBT).
 *
 * @author hbm
 */
public class SlotMonitor {

    /** The index of the slot in the inventory this monitor....monitors */
    public final int index;
    /** The inventory */
    public final ISlotMonitorProvider parent;

    /** If this monitor detects a change, all cache slots need to be notified */
    public LinkedHashSet<CacheSlot> viewedBy = new LinkedHashSet<>();

    @Nullable public Item item;
    public long stacksize;
    public DataComponentPatch components = DataComponentPatch.EMPTY;

    protected boolean hasAvailabilityChanged = false;

    public SlotMonitor(int index, ISlotMonitorProvider parent) {
        this.hasAvailabilityChanged = true;
        this.index = index;
        this.parent = parent;
    }

    /** A single-item stack of the monitored type, only used for display purposes. The cache slot tracks the actual amount. */
    public ItemStack toDisplayStack() {
        if (item == null) return ItemStack.EMPTY;
        ItemStack stack = new ItemStack(item);
        stack.applyComponents(components);
        return stack;
    }

    /**
     * Monitor providers need to keep track of whether availability has changed, i.e. compair has run out, compression setting has changed, etc
     * This means that we don't have to check availability every single tick, which potentially saves a fuckton of iterations.
     */
    public void availabilityHasChanged() {
        this.hasAvailabilityChanged = true;
    }

    public void checkUpdate() {

        PneumaticNetwork pneumoNet = this.parent.getRelevantNetwork();

        if (hasAvailabilityChanged) {

            if (pneumoNet != null) {
                for (StackCache cache : pneumoNet.accessors) {
                    if (!cache.hasExpired && parent.isAvailableToCache(cache)) {
                        cache.addToCache(this);
                    }
                }
            }

            // if this monitor is not available to some caches, remove them
            Iterator<CacheSlot> iterator = viewedBy.iterator();
            while (iterator.hasNext()) {
                CacheSlot slot = iterator.next();
                StackCache cache = slot.getStackCache();
                if (cache.hasExpired || !parent.isAvailableToCache(cache)) {
                    slot.removeMonitor(this);
                    iterator.remove();
                }
            }

            hasAvailabilityChanged = false;
        }

        ItemStack stack = parent.getSlotAt(index);
        long amount = parent.getAmountAt(index);

        boolean hasTypeChanged;
        if (stack.isEmpty() || item == null) {
            hasTypeChanged = stack.isEmpty() != (item == null);
        } else {
            hasTypeChanged = item != stack.getItem() || !components.equals(stack.getComponentsPatch());
        }

        if (hasTypeChanged) {

            // remove from all existing monitors
            Iterator<CacheSlot> iterator = viewedBy.iterator();
            while (iterator.hasNext()) {
                CacheSlot slot = iterator.next();
                slot.removeMonitor(this);
                iterator.remove();
            }

            // set updated traits
            if (stack.isEmpty()) {
                this.item = null;
                this.stacksize = 0;
                this.components = DataComponentPatch.EMPTY;
            } else {
                this.item = stack.getItem();
                this.stacksize = amount;
                this.components = stack.getComponentsPatch();
            }

            // find new monitors
            if (pneumoNet != null) {

                for (StackCache cache : pneumoNet.accessors) {
                    if (!cache.hasExpired && parent.isAvailableToCache(cache)) {
                        cache.addToCache(this);
                    }
                }
            }

            return;
        }

        if (stacksize != amount) {
            long delta = amount - stacksize;
            for (CacheSlot slot : viewedBy) slot.changeAmounts(delta);

            this.stacksize = amount;
        }
    }
}
