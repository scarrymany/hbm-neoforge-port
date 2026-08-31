package com.hbm.blockentity.machine;

import com.hbm.blockentity.IPersistentNBT;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.blocks.generic.BlockStorageCrate;
import com.hbm.blocks.machine.CrateBlock;
import com.hbm.hazard.HazardSystem;
import com.hbm.util.ContaminationUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Mass storage crate block entity, ported from CE's {@code com.hbm.tileentity.machine.TileEntityCrate}
 * plus its {@code TileEntityCrateBase}/{@code TileEntityLockableBase} ancestors (both read in full -
 * see {@code docs/phase2/machines_storage.md}). CE models each of its five grades
 * ({@code TileEntityCrateIron}/{@code Steel}/{@code Tungsten}/{@code Desh}, {@code TileEntitySafe})
 * as a one-line subclass hard-coding a constructor call; this port collapses that into a single
 * class parameterized by {@link CrateType}, matching this port's own {@code BlockCrate.Type}
 * precedent (see {@code com.hbm.blocks.generic.BlockCrate}) rather than five near-duplicate files.
 *
 * <h2>Deliberately narrowed scope vs. CE - both documented in {@code machines_storage.md}</h2>
 * <ul>
 *   <li><b>No lock/pin mechanism.</b> CE's {@code TileEntityLockableBase} (lock state machine) is
 *   trivial on its own, but every path that actually unlocks a locked crate reads
 *   {@code ItemKeyPin}/{@code ModItems.key_red}/a screwdriver-based pick-attempt - none of which
 *   exist in this port yet (confirmed absent from {@code com.hbm.items} at the time of this pass).
 *   Porting inert lock fields with no way to ever lock or unlock a crate would be dead weight, not a
 *   feature; the research report recommends porting the lock/pin item family in the same pass, but
 *   that is an items/tools package, not a storage-machines one, so it is left as documented follow-up
 *   work rather than guessed at here. Every crate this class produces is therefore always accessible.</li>
 *   <li><b>No loot-table auto-fill.</b> CE's {@code TileEntityCrateBase.ensureFilled}/{@code fillWithLoot}
 *   lazily rolls a vanilla {@code LootTable} into the crate's inventory the first time it's touched,
 *   driven by a {@code ResourceLocation} + seed CE's block placement/world-gen code assigns. No
 *   world-gen or structure-loot-table wiring exists anywhere in this port yet to ever set
 *   {@code lootTable} on a placed crate, so the whole mechanism would be unreachable dead code; the
 *   plain default-empty inventory this class ships with is exactly what a loot-table-less crate
 *   already behaves like in CE. A future world-gen pass can add {@code lootTable}/{@code lootTableSeed}
 *   fields plus the fill-on-first-open hook back in without touching anything else on this class.</li>
 * </ul>
 * Everything else - the {@link ItemStackHandler} inventory (rejecting nested crates the same way CE's
 * {@code ItemBlockStorageCrate.containsCrate} check does), the drop-on-break persistent payload
 * (slot contents, via {@link IPersistentNBT}), and the custom-name/GUI-metadata fields - is a direct
 * port of CE's {@code TileEntityCrate} behavior.
 */
public class CrateBlockEntity extends MachineBaseBlockEntity implements IPersistentNBT {

    /**
     * Per-grade layout/metadata table, replacing CE's five hard-coded subclass constructors (see
     * {@code TileEntityCrateIron}/{@code TileEntityCrateSteel}/{@code TileEntityCrateTungsten}/
     * {@code TileEntityCrateDesh}/{@code TileEntitySafe}, all read in full - the numbers below are
     * copied verbatim from those constructor calls).
     */
    public enum CrateType {
        IRON(36, "container.crateIron", 9, 4, 8, 18, 8, 104, 162, 176, 186, 8, 0x404040, 0x404040, "gui_crate_iron"),
        STEEL(54, "container.crateSteel", 9, 6, 8, 18, 8, 140, 198, 176, 222, 8, 0x1C1C1C, 0x1C1C1C, "gui_crate_steel"),
        TUNGSTEN(27, "container.crateTungsten", 9, 3, 8, 18, 8, 86, 144, 176, 168, 8, 0xA0A0A0, 0xA0A0A0, "gui_crate_tungsten"),
        DESH(104, "container.crateDesh", 13, 8, 8, 18, 44, 174, 232, 248, 256, 44, 0x3F1515, 0x3F1515, "gui_crate_desh"),
        SAFE(15, "container.safe", 5, 3, 44, 18, 8, 86, 144, 176, 168, 8, 0x404040, 0x404040, "gui_safe");

        public final int slots;
        public final String nameKey;
        public final int columns;
        public final int rows;
        public final int crateX;
        public final int crateY;
        public final int playerInventoryX;
        public final int playerInventoryY;
        public final int hotbarY;
        public final int guiWidth;
        public final int guiHeight;
        public final int inventoryLabelX;
        public final int titleColor;
        public final int inventoryLabelColor;
        public final ResourceLocation texture;

        CrateType(int slots, String nameKey, int columns, int rows, int crateX, int crateY,
                  int playerInventoryX, int playerInventoryY, int hotbarY, int guiWidth, int guiHeight,
                  int inventoryLabelX, int titleColor, int inventoryLabelColor, String textureName) {
            this.slots = slots;
            this.nameKey = nameKey;
            this.columns = columns;
            this.rows = rows;
            this.crateX = crateX;
            this.crateY = crateY;
            this.playerInventoryX = playerInventoryX;
            this.playerInventoryY = playerInventoryY;
            this.hotbarY = hotbarY;
            this.guiWidth = guiWidth;
            this.guiHeight = guiHeight;
            this.inventoryLabelX = inventoryLabelX;
            this.titleColor = titleColor;
            this.inventoryLabelColor = inventoryLabelColor;
            this.texture = ResourceLocation.fromNamespaceAndPath("hbm", "textures/gui/storage/" + textureName + ".png");
        }
    }

    private final CrateType type;

    public CrateBlockEntity(BlockEntityType<?> beType, BlockPos pos, BlockState state, CrateType type) {
        super(beType, pos, state, type.slots, false, false);
        this.type = type;
    }

    public CrateType getCrateType() {
        return this.type;
    }

    /**
     * Rejects nested crates, matching CE's {@code TileEntityCrateBase.isItemValidForSlot} ->
     * {@code ItemBlockStorageCrate.containsCrate}: a crate that is itself a {@link CrateBlock}
     * {@code BlockItem} is refused, so players can't nest nested storage indefinitely.
     */
    @Override
    public boolean isItemValidForSlot(int i, ItemStack stack) {
        return !(stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof CrateBlock);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable(type.nameKey);
    }

    /**
     * {@link #setDestroyedByCreativePlayer()}/{@link #isDestroyedByCreativePlayer()} are not
     * overridden here - {@link MachineBaseBlockEntity} already implements both concretely with
     * exactly the flag {@link IPersistentNBT#shouldDrop()}'s default expects, so they satisfy this
     * interface's two abstract methods without a second, shadowing flag on this subclass.
     */

    /**
     * Drop-on-break payload: slot contents. (Phase 6 update: accumulated contained-item radiation -
     * CE's {@code HazardSystem.getTotalRadsFromStack} sum - is no longer dropped here; it is now
     * carried by {@link #writeItemComponents} instead, once {@link BlockStorageCrate#CRATE_RAD_KEY}
     * gave it somewhere real to live. See that method for the port of CE's radiation sum itself.)
     * Slot contents are the load-bearing half of this feature either way: without them, breaking a
     * full crate would silently void its inventory instead of dropping one item that remembers what
     * was inside.
     */
    @Override
    public void writeNBT(CompoundTag nbt) {
        CompoundTag data = new CompoundTag();
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack.isEmpty()) continue;
            data.put("slot" + i, stack.save(this.level.registryAccess(), new CompoundTag()));
        }
        if (!data.isEmpty()) nbt.put(NBT_PERSISTENT_KEY, data);
    }

    @Override
    public void readNBT(CompoundTag nbt) {
        CompoundTag data = nbt.contains(NBT_PERSISTENT_KEY) ? nbt.getCompound(NBT_PERSISTENT_KEY) : nbt;
        for (int i = 0; i < inventory.getSlots(); i++) {
            String key = "slot" + i;
            if (data.contains(key)) {
                inventory.setStackInSlot(i, ItemStack.parseOptional(this.level.registryAccess(), data.getCompound(key)));
            }
        }
    }

    /**
     * Direct port of CE's {@code TileEntityCrateBase.buildDropData} radiation sum, attached to the
     * dropped stack via {@link BlockStorageCrate#CRATE_RAD_KEY} instead of CE's raw {@code cRads} NBT
     * double (see that field's javadoc - the 1.21 data-component replacement for it).
     * <p>
     * CE: {@code radiation += HazardSystem.getTotalRadsFromStack(stack) * stack.getCount();} where
     * {@code getTotalRadsFromStack = getHazardLevelFromStack(RADIATION) + ContaminationUtil.getNeutronRads(stack)}
     * and {@code ContaminationUtil.getNeutronRads} <em>already</em> multiplies by {@code stack.getCount()}
     * internally - so CE's neutron-contamination term is count-multiplied twice per slot. That is
     * preserved verbatim here (this port's {@link HazardSystem#getRawRadsFromStack} and
     * {@link ContaminationUtil#getNeutronRads} match their CE counterparts exactly, including that same
     * internal count multiplication on the neutron term), matching this port's established precedent of
     * carrying CE quirks over rather than silently "fixing" them (see
     * {@code HazardTransformerRadiationContainer}'s own preserved plastic-bag-unreachable-branch quirk).
     */
    @Override
    public void writeItemComponents(ItemStack itemstack) {
        double radiation = 0D;
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack.isEmpty()) continue;
            radiation += (HazardSystem.getRawRadsFromStack(stack) + ContaminationUtil.getNeutronRads(stack)) * stack.getCount();
        }
        if (radiation > 0D) {
            itemstack.set(BlockStorageCrate.CRATE_RAD_KEY.get(), radiation);
        }
    }
}
