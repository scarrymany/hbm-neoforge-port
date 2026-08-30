package com.hbm.blockentity.bomb;

import com.hbm.blockentity.ITickableBE;
import com.hbm.config.BombConfig;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.NbtComparableStack;
import com.hbm.inventory.container.bomb.NukeCustomMenu;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.IngotNuggetItems;
import com.hbm.items.BilletPowderItems;
import com.hbm.items.bomb.NukeCasingItems;
import com.hbm.items.special.ItemCell;
import com.hbm.items.special.SpecialItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

/**
 * Ported from CE's {@code TileEntityNukeCustom} (388 lines, read in full) - the one casing whose
 * 27-slot inventory is validated by a real crafting-value lookup ({@link ComparableStack}/
 * {@link NbtComparableStack}) rather than a flat per-slot item-identity chain, per {@code
 * docs/phase3/bomb_blocks_and_detonators.md}'s "the one outlier" framing. {@link #updateEntity()}
 * (CE: {@code update()}) recomputes the 8 yield categories every tick by summing/multiplying every
 * recognized item across all 27 slots, then clamping each to its {@code BombConfig.maxCustom*}
 * cap and zeroing out categories whose prerequisite tier didn't reach its own threshold (CE's
 * {@code if(tnt<16) nuke=0} chain, preserved exactly).
 * <p>
 * {@link #entries} is populated once, lazily, by a static initializer (CE calls
 * {@code registerBombItems()} once from {@code MainRegistry}'s init - this port's equivalent
 * shared-file wiring point doesn't need touching since a static initializer runs on first class
 * reference regardless of call site). A handful of CE's original {@code entries.put(...)} lines are
 * commented out below with a named TODO rather than fabricated under the wrong package - see each
 * comment for the exact missing dependency (conventional-explosives items/blocks, fluid-canister
 * metadata, {@code block_solinium}, and the {@code MachineItems} lithium-family loop item, none of
 * which this bomb-casing package owns).
 * <p>
 * <b>Registration keys are {@link NbtComparableStack}, not plain {@link ComparableStack}</b> - this
 * port's real {@code ComparableStack.equals} (confirmed by reading {@code RecipesCommon.java} in
 * full) rejects cross-subclass comparison ({@code getClass() != obj.getClass()}), and
 * {@code NbtComparableStack.equals} additionally requires the compared object to itself be an
 * {@code NbtComparableStack}. Since {@link #updateEntity()}'s lookup key is always an
 * {@code NbtComparableStack} (matching CE's own {@code new NbtComparableStack(stack).makeSingular()}
 * call), a map populated with plain {@code ComparableStack} keys (a literal transcription of CE's own
 * {@code new ComparableStack(...)} registration calls) would never match anything - every lookup
 * would silently return {@code null} and the entire crafting map would be inert. Populating with
 * {@code NbtComparableStack} keys instead (built from a real {@link ItemStack}, as {@link #put}/
 * {@link #putMult} do below) keeps both sides of every comparison the same concrete type, which also
 * happens to be the intentionally more correct choice for the {@code ItemCell.getFullCell(...)}
 * entries (a full-cell {@code ItemStack} carries fluid-fill state in its data components that a plain
 * item-only {@code ComparableStack} can't see - it would otherwise match an empty cell of the same
 * base item too).
 */
public class NukeCustomBlockEntity extends NukeCasingBlockEntity implements ITickableBE {

    public static final HashMap<ComparableStack, CustomNukeEntry> entries = new HashMap<>();

    static {
        registerBombItems();
    }

    public float tnt, nuke, hydro, bale, dirty, schrab, sol, euph;

    public NukeCustomBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 27);
    }

    private static void put(ItemStack stack, EnumBombType type, float value) {
        entries.put(new NbtComparableStack(stack).makeSingular(), new CustomNukeEntry(type, value));
    }

    private static void put(Item item, EnumBombType type, float value) {
        put(new ItemStack(item), type, value);
    }

    private static void putMult(ItemStack stack, EnumBombType type, float value) {
        entries.put(new NbtComparableStack(stack).makeSingular(), new CustomNukeEntry(type, value, EnumEntryType.MULT));
    }

    private static void putMult(Item item, EnumBombType type, float value) {
        putMult(new ItemStack(item), type, value);
    }

    public static void registerBombItems() {
        // TNT
        put(Items.GUNPOWDER, EnumBombType.TNT, 0.8F);
        put(Blocks.TNT.asItem(), EnumBombType.TNT, 4F);
        // TODO(ModBlocks.det_cord, sibling conventional-explosives package): CE also adds 1.5F TNT-equiv here.
        put(NukeCasingItems.CUSTOM_TNT.get(), EnumBombType.TNT, 10F);
        // TODO(ModItems.ball_dynamite/ball_tnt, sibling conventional-explosives package): CE adds 4F/6F TNT-equiv here.
        // TODO(ModBlocks.det_charge, sibling conventional-explosives package): CE adds 15F TNT-equiv here.
        // TODO(ModItems.canister_full + Fluids.DIESEL/KEROSENE metadata, ModItems.canister_napalm): CE's
        // canister entries keyed a fluid-backed item by 1.12 metadata damage value - this port's
        // flattened/data-component canister item(s) have no direct numeric-ID equivalent to key a
        // ComparableStack off; narrow gap, not resolved here.
        // TODO(ModBlocks.red_barrel/pink_barrel, sibling packages): CE also adds 2.5F/4F TNT-equiv here.

        putMult(NukeCasingItems.N2_CHARGE.get(), EnumBombType.TNT, 1.25F);
        putMult(Items.REDSTONE, EnumBombType.TNT, 1.05F);
        putMult(Blocks.REDSTONE_BLOCK.asItem(), EnumBombType.TNT, 1.5F);

        // NUKE
        put(IngotNuggetItems.INGOT_U235.get(), EnumBombType.NUKE, 15F);
        put(IngotNuggetItems.INGOT_PU239.get(), EnumBombType.NUKE, 25F);
        put(IngotNuggetItems.INGOT_NEPTUNIUM.get(), EnumBombType.NUKE, 30F);
        put(IngotNuggetItems.NUGGET_U235.get(), EnumBombType.NUKE, 1.5F);
        put(IngotNuggetItems.NUGGET_PU239.get(), EnumBombType.NUKE, 2.5F);
        put(IngotNuggetItems.NUGGET_NEPTUNIUM.get(), EnumBombType.NUKE, 3.0F);
        put(BilletPowderItems.POWDER_NEPTUNIUM.get(), EnumBombType.NUKE, 30F);
        put(NukeCasingItems.CUSTOM_NUKE.get(), EnumBombType.NUKE, 30F);
        putMult(IngotNuggetItems.INGOT_URANIUM.get(), EnumBombType.NUKE, 1.05F);
        putMult(IngotNuggetItems.INGOT_PLUTONIUM.get(), EnumBombType.NUKE, 1.15F);
        putMult(IngotNuggetItems.INGOT_U238.get(), EnumBombType.NUKE, 1.1F);
        putMult(IngotNuggetItems.INGOT_PU238.get(), EnumBombType.NUKE, 1.15F);
        putMult(IngotNuggetItems.NUGGET_URANIUM.get(), EnumBombType.NUKE, 1.005F);
        putMult(IngotNuggetItems.NUGGET_PLUTONIUM.get(), EnumBombType.NUKE, 1.15F);
        putMult(IngotNuggetItems.NUGGET_U238.get(), EnumBombType.NUKE, 1.01F);
        putMult(IngotNuggetItems.NUGGET_PU238.get(), EnumBombType.NUKE, 1.015F);
        putMult(BilletPowderItems.POWDER_URANIUM.get(), EnumBombType.NUKE, 1.05F);
        putMult(BilletPowderItems.POWDER_PLUTONIUM.get(), EnumBombType.NUKE, 1.15F);

        // SUPER (hydrogen)
        put(ItemCell.getFullCell(SpecialItems.CELL.get(), Fluids.DEUTERIUM), EnumBombType.HYDRO, 20F);
        put(ItemCell.getFullCell(SpecialItems.CELL.get(), Fluids.TRITIUM), EnumBombType.HYDRO, 30F);
        // TODO(MachineItems lithium-family loop item, no discrete field to reference): CE adds 20F HYDRO-equiv here.
        put(NukeCasingItems.TRITIUM_DEUTERIUM_CAKE.get(), EnumBombType.HYDRO, 200F);
        put(NukeCasingItems.CUSTOM_HYDRO.get(), EnumBombType.HYDRO, 30F);

        // ANTIMATTER
        put(ItemCell.getFullCell(SpecialItems.CELL.get(), Fluids.AMAT), EnumBombType.BALE, 5F);
        put(NukeCasingItems.CUSTOM_AMAT.get(), EnumBombType.BALE, 15F);
        put(NukeCasingItems.EGG_BALEFIRE_SHARD.get(), EnumBombType.BALE, 15F);
        put(NukeCasingItems.EGG_BALEFIRE.get(), EnumBombType.BALE, 150F);

        // SALTED
        put(IngotNuggetItems.INGOT_TUNGSTEN.get(), EnumBombType.DIRTY, 1F);
        put(NukeCasingItems.CUSTOM_DIRTY.get(), EnumBombType.DIRTY, 10F);
        putMult(IngotNuggetItems.INGOT_PU240.get(), EnumBombType.DIRTY, 1.05F);
        putMult(NukeCasingItems.NUCLEAR_WASTE.get(), EnumBombType.DIRTY, 1.025F);
        // TODO(ModBlocks.block_waste/yellow_barrel, generic-blocks / sibling packages): CE adds 1.25F/1.2F DIRTY-mult here.

        // ANTISCHRABIDIUM
        put(IngotNuggetItems.INGOT_SCHRABIDIUM.get(), EnumBombType.SCHRAB, 5F);
        put(IngotNuggetItems.NUGGET_SCHRABIDIUM.get(), EnumBombType.SCHRAB, 0.5F);
        put(BilletPowderItems.POWDER_SCHRABIDIUM.get(), EnumBombType.SCHRAB, 5F);
        put(ItemCell.getFullCell(SpecialItems.CELL.get(), Fluids.SAS3), EnumBombType.SCHRAB, 7.5F);
        put(ItemCell.getFullCell(SpecialItems.CELL.get(), Fluids.ASCHRAB), EnumBombType.SCHRAB, 15F);
        put(NukeCasingItems.CUSTOM_SCHRAB.get(), EnumBombType.SCHRAB, 15F);
        // TODO(ModBlocks.block_schrabidium as a distinct 50F entry): MaterialBlockGenerator's block_schrabidium
        // exists but wasn't cross-checked against this pass's constructor shape - left out to avoid
        // guessing its exact registry field name; the ingot/nugget/powder/cell entries above already
        // cover this tier.

        // SOLINIUM
        put(NukeCasingItems.SOLINIUM_CORE.get(), EnumBombType.SOL, 20F);
        put(IngotNuggetItems.NUGGET_SOLINIUM.get(), EnumBombType.SOL, 0.5F);
        put(IngotNuggetItems.INGOT_SOLINIUM.get(), EnumBombType.SOL, 5F);
        put(BilletPowderItems.BILLET_SOLINIUM.get(), EnumBombType.SOL, 3F);
        // TODO(ModBlocks.block_solinium, confirmed absent from this port): CE adds 50F SOL-equiv here.
        put(NukeCasingItems.CUSTOM_SOL.get(), EnumBombType.SOL, 15F);

        // ANTI-MASS
        put(IngotNuggetItems.NUGGET_EUPHEMIUM.get(), EnumBombType.EUPH, 1F);
        put(IngotNuggetItems.INGOT_EUPHEMIUM.get(), EnumBombType.EUPH, 1F);
    }

    @Override
    public void updateEntity() {
        float tnt = 0F, tntMod = 1F;
        float nuke = 0F, nukeMod = 1F;
        float hydro = 0F, hydroMod = 1F;
        float bale = 0F, baleMod = 1F;
        float dirty = 0F, dirtyMod = 1F;
        float schrab = 0F, schrabMod = 1F;
        float sol = 0F, solMod = 1F;
        float euph = 0F;

        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack.isEmpty()) continue;

            ComparableStack comp = new NbtComparableStack(stack).makeSingular();
            CustomNukeEntry ent = entries.get(comp);
            if (ent == null) continue;

            if (ent.entry == EnumEntryType.ADD) {
                switch (ent.type) {
                    case TNT -> tnt += ent.value;
                    case NUKE -> nuke += ent.value;
                    case HYDRO -> hydro += ent.value;
                    case BALE -> bale += ent.value;
                    case DIRTY -> dirty += ent.value;
                    case SCHRAB -> schrab += ent.value;
                    case SOL -> sol += ent.value;
                    case EUPH -> euph += ent.value;
                }
            } else {
                switch (ent.type) {
                    case TNT -> tntMod *= ent.value;
                    case NUKE -> nukeMod *= ent.value;
                    case HYDRO -> hydroMod *= ent.value;
                    case BALE -> baleMod *= ent.value;
                    case DIRTY -> dirtyMod *= ent.value;
                    case SOL -> solMod *= ent.value;
                    case SCHRAB -> schrabMod *= ent.value;
                    default -> {
                    }
                }
            }
        }

        tnt *= tntMod;
        nuke *= nukeMod;
        hydro *= hydroMod;
        bale *= baleMod;
        dirty *= dirtyMod;
        sol *= solMod;
        schrab *= schrabMod;

        if (tnt < 16) nuke = 0;
        if (nuke < 100) hydro = 0;
        if (nuke < 50) bale = 0;
        if (nuke < 50) schrab = 0;
        if (nuke < 25) sol = 0;
        if (schrab < 1 || sol < 1) euph = 0;

        this.tnt = Math.min(tnt, BombConfig.MAX_CUSTOM_TNT_RADIUS.get());
        this.nuke = Math.min(nuke, BombConfig.MAX_CUSTOM_NUKE_RADIUS.get());
        this.hydro = Math.min(hydro, BombConfig.MAX_CUSTOM_HYDRO_RADIUS.get());
        this.bale = Math.min(bale, BombConfig.MAX_CUSTOM_BALE_RADIUS.get());
        this.dirty = Math.min(dirty, BombConfig.MAX_CUSTOM_DIRTY_RADIUS.get());
        this.schrab = Math.min(schrab, BombConfig.MAX_CUSTOM_SCHRAB_RADIUS.get());
        this.sol = Math.min(sol, BombConfig.MAX_CUSTOM_SOL_RADIUS.get());
        this.euph = Math.min(euph, BombConfig.MAX_CUSTOM_EUPH_LVL.get());
    }

    public boolean isFalling() {
        for (int i = 0; i < inventory.getSlots(); i++) {
            if (inventory.getStackInSlot(i).getItem() == NukeCasingItems.CUSTOM_FALL.get()) return true;
        }
        return false;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.nukeCustom");
    }

    @Override
    public @NotNull AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new NukeCustomMenu(containerId, playerInventory, this);
    }

    public enum EnumBombType {
        TNT, NUKE, HYDRO, BALE, DIRTY, SCHRAB, SOL, EUPH
    }

    public enum EnumEntryType {
        ADD, MULT
    }

    public static class CustomNukeEntry {
        public final EnumBombType type;
        public final EnumEntryType entry;
        public final float value;

        public CustomNukeEntry(EnumBombType type, float value) {
            this(type, value, EnumEntryType.ADD);
        }

        public CustomNukeEntry(EnumBombType type, float value, EnumEntryType entry) {
            this.type = type;
            this.entry = entry;
            this.value = value;
        }
    }
}
