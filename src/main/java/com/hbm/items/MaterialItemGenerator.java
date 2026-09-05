package com.hbm.items;

import com.hbm.creativetabs.CreativeTabContents;
import com.hbm.creativetabs.ModCreativeTabs;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.List;

/**
 * Ports CE's {@code ItemAutogen} mechanism: a data-driven registration loop over
 * {@code MaterialShapes x Mats.orderedList}, producing one plain {@link Item} per (material, shape)
 * pair where {@link NTMMaterial#getAutogen()} contains the shape.
 *
 * <p>Only the 17 {@link MaterialShapes} constants CE actually feeds into
 * {@code new ItemAutogen(shape, name)} are covered here (see
 * {@code docs/phase1/moditems_generative.md} section 1) - the remaining shapes with a
 * {@code setAutogen(...)} entry in {@code Mats.java} (NUGGET, DUST, DUSTTINY, BILLET, PLATE, GEM,
 * BLOCK) back CE's irregular hand-coded {@code ingot_}/{@code nugget_}/{@code powder_}/etc. item
 * families instead, which belong to a different registration pass, not this generator.
 *
 * <p>Reading CE's {@code ItemAutogen.java} confirmed these variants carry no hazard/NBT/behavior of
 * their own (pure generic resource items, metadata + rendering only), so every entry here is a plain
 * {@code new Item(new Item.Properties())} with no hazard registration.
 *
 * <p>{@code HEAVY_COMPONENT} is kept in the shape list for structural parity with CE even though no
 * {@code NTMMaterial} currently declares it via {@code setAutogen(HEAVY_COMPONENT)} - it registers
 * zero items today, matching CE's own live behavior.
 */
final class MaterialItemGenerator {

    private static final List<MaterialShapes> AUTOGEN_SHAPES = List.of(
            MaterialShapes.SHELL, MaterialShapes.PIPE, MaterialShapes.INGOT, MaterialShapes.CASTPLATE,
            MaterialShapes.WELDEDPLATE, MaterialShapes.HEAVY_COMPONENT, MaterialShapes.WIRE,
            MaterialShapes.DENSEWIRE, MaterialShapes.BOLT, MaterialShapes.LIGHTBARREL,
            MaterialShapes.HEAVYBARREL, MaterialShapes.LIGHTRECEIVER, MaterialShapes.HEAVYRECEIVER,
            MaterialShapes.MECHANISM, MaterialShapes.STOCK, MaterialShapes.GRIP, MaterialShapes.FRAGMENT,
            MaterialShapes.DUST, // DUST added for MAT_FLUORITE/MAT_SULFUR (CE PowderRecipes.java:68-69)
            MaterialShapes.DUSTTINY); // DUSTTINY added for powder_desh_mix (CE PowderRecipes.java:57)

    private MaterialItemGenerator() {
    }

    /** Registers every (material, shape) variant. Must run before {@code ModItems.ITEMS.register(modEventBus)}. */
    static void registerAll() {
        for (MaterialShapes shape : AUTOGEN_SHAPES) {
            for (NTMMaterial mat : Mats.orderedList) {
                if (!mat.getAutogen().contains(shape)) {
                    continue;
                }

                String registryName = shape.buildRegistryName(mat);
                DeferredItem<Item> item = ModItems.ITEMS.register(registryName, () -> new Item(new Item.Properties()));
                CreativeTabContents.add(ModCreativeTabs.PARTS, item);
            }
        }
    }
}
