package com.hbm.creativetabs;

import com.hbm.main.MainRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * NeoForge creative-mode-tab registrations, consolidated from CE's ten separate
 * {@code CreativeTabs} subclasses (creativetabs.PartsTab, ControlTab, TemplateTab,
 * ResourceTab, BlockTab, MachineTab, NukeTab, MissileTab, WeaponTab, ConsumableTab).
 * <p>
 * Phase 0: every tab is registered empty with a placeholder {@link Items#BARRIER} icon
 * purely so the tab exists and shows up in the creative inventory. Phase 1 must:
 * <ul>
 *     <li>swap each placeholder icon for the real CE icon item/block noted per tab below</li>
 *     <li>add a {@code displayItems} callback populating each tab from the ported item/block catalog</li>
 *     <li>reimplement ControlTab's battery full/empty stack splitting (CE's
 *     {@code displayAllRelevantItems} override, driven by {@code IBatteryItem}) as post-processing
 *     inside CONTROL's displayItems callback</li>
 *     <li>reimplement MissileTab's nine curated {@code ItemCustomMissile.buildMissile(...)} showcase
 *     stacks as appended entries inside MISSILE's displayItems callback</li>
 *     <li>restore TemplateTab's search-bar behaviour and item_search.png / nuke.png background
 *     textures once the corresponding textures are ported (see {@link CreativeModeTab.Builder#backgroundTexture})</li>
 * </ul>
 */
public final class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MainRegistry.MODID);

    private static final String ID_PARTS = "tab_parts";
    private static final String ID_CONTROL = "tab_control";
    private static final String ID_TEMPLATE = "tab_template";
    private static final String ID_RESOURCE = "tab_resource";
    private static final String ID_BLOCKS = "tab_blocks";
    private static final String ID_MACHINE = "tab_machine";
    private static final String ID_NUKE = "tab_nuke";
    private static final String ID_MISSILE = "tab_missile";
    private static final String ID_WEAPON = "tab_weapon";
    private static final String ID_CONSUMABLE = "tab_consumable";

    // CE icon: ModItems.ingot_uranium (ingots/nuggets/wires/machine parts tab)
    public static final Supplier<CreativeModeTab> PARTS = CREATIVE_MODE_TABS.register(
            ID_PARTS,
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(Items.BARRIER))
                    .title(Component.translatable(translationKey(ID_PARTS)))
                    .build());

    // CE icon: ModItems.pellet_rtg (fuels/batteries/upgrades tab)
    public static final Supplier<CreativeModeTab> CONTROL = CREATIVE_MODE_TABS.register(
            ID_CONTROL,
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(Items.BARRIER))
                    .title(Component.translatable(translationKey(ID_CONTROL)))
                    .withTabsBefore(tabId(ID_PARTS))
                    .build());

    // CE icon: ModItems.blueprints (templates/blueprints/siren tracks tab)
    public static final Supplier<CreativeModeTab> TEMPLATE = CREATIVE_MODE_TABS.register(
            ID_TEMPLATE,
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(Items.BARRIER))
                    .title(Component.translatable(translationKey(ID_TEMPLATE)))
                    .withTabsBefore(tabId(ID_CONTROL))
                    .build());

    // CE icon: ModBlocks.ore_uranium (ore/mineral blocks tab)
    public static final Supplier<CreativeModeTab> RESOURCE = CREATIVE_MODE_TABS.register(
            ID_RESOURCE,
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(Items.BARRIER))
                    .title(Component.translatable(translationKey(ID_RESOURCE)))
                    .withTabsBefore(tabId(ID_TEMPLATE))
                    .build());

    // CE icon: ModBlocks.brick_concrete (construction blocks tab)
    public static final Supplier<CreativeModeTab> BLOCKS = CREATIVE_MODE_TABS.register(
            ID_BLOCKS,
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(Items.BARRIER))
                    .title(Component.translatable(translationKey(ID_BLOCKS)))
                    .withTabsBefore(tabId(ID_RESOURCE))
                    .build());

    // CE icon: ModBlocks.pwr_controller (machines/structure parts tab)
    public static final Supplier<CreativeModeTab> MACHINE = CREATIVE_MODE_TABS.register(
            ID_MACHINE,
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(Items.BARRIER))
                    .title(Component.translatable(translationKey(ID_MACHINE)))
                    .withTabsBefore(tabId(ID_BLOCKS))
                    .build());

    // CE icon: ModBlocks.nuke_man (nuke/bomb tab)
    public static final Supplier<CreativeModeTab> NUKE = CREATIVE_MODE_TABS.register(
            ID_NUKE,
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(Items.BARRIER))
                    .title(Component.translatable(translationKey(ID_NUKE)))
                    .withTabsBefore(tabId(ID_MACHINE))
                    .build());

    // CE icon: ModItems.missile_nuclear (missiles/satellites tab)
    public static final Supplier<CreativeModeTab> MISSILE = CREATIVE_MODE_TABS.register(
            ID_MISSILE,
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(Items.BARRIER))
                    .title(Component.translatable(translationKey(ID_MISSILE)))
                    .withTabsBefore(tabId(ID_NUKE))
                    .build());

    // CE icon: ModItems.gun_vortex (turrets/weapons/ammo tab)
    public static final Supplier<CreativeModeTab> WEAPON = CREATIVE_MODE_TABS.register(
            ID_WEAPON,
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(Items.BARRIER))
                    .title(Component.translatable(translationKey(ID_WEAPON)))
                    .withTabsBefore(tabId(ID_MISSILE))
                    .build());

    // CE icon: ModItems.bottle_nuka (drinks/kits/tools tab)
    public static final Supplier<CreativeModeTab> CONSUMABLE = CREATIVE_MODE_TABS.register(
            ID_CONSUMABLE,
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(Items.BARRIER))
                    .title(Component.translatable(translationKey(ID_CONSUMABLE)))
                    .withTabsBefore(tabId(ID_WEAPON))
                    .build());

    private ModCreativeTabs() {
    }

    private static ResourceLocation tabId(String path) {
        return ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, path);
    }

    private static String translationKey(String path) {
        return "itemGroup." + MainRegistry.MODID + "." + path;
    }

    public static void register(IEventBus modEventBus) {
        CREATIVE_MODE_TABS.register(modEventBus);
    }
}
