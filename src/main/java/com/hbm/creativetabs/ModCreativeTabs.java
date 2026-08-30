package com.hbm.creativetabs;

import com.hbm.main.MainRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * NeoForge creative-mode-tab registrations, consolidated from CE's ten separate
 * {@code CreativeTabs} subclasses (creativetabs.PartsTab, ControlTab, TemplateTab,
 * ResourceTab, BlockTab, MachineTab, NukeTab, MissileTab, WeaponTab, ConsumableTab).
 * <p>
 * Tab membership is not declared here: CE baked {@code setCreativeTab} into every item/block
 * constructor, but 1.21 has no per-item tab flag, so each tab's {@code displayItems} callback
 * below flushes {@link CreativeTabContents}, the shared accumulator that other Phase 1 areas
 * populate via {@code CreativeTabContents.add(ModCreativeTabs.XXX, supplier)} as they register
 * their own items/blocks (see docs/phase1/creative_tabs_plan.md).
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

    /**
     * Registry keys other areas pass as the first argument to
     * {@link CreativeTabContents#add(ResourceKey, java.util.function.Supplier)} when adding their
     * own items/blocks to a tab.
     */
    public static final ResourceKey<CreativeModeTab> PARTS = tabKey(ID_PARTS);
    public static final ResourceKey<CreativeModeTab> CONTROL = tabKey(ID_CONTROL);
    public static final ResourceKey<CreativeModeTab> TEMPLATE = tabKey(ID_TEMPLATE);
    public static final ResourceKey<CreativeModeTab> RESOURCE = tabKey(ID_RESOURCE);
    public static final ResourceKey<CreativeModeTab> BLOCKS = tabKey(ID_BLOCKS);
    public static final ResourceKey<CreativeModeTab> MACHINE = tabKey(ID_MACHINE);
    public static final ResourceKey<CreativeModeTab> NUKE = tabKey(ID_NUKE);
    public static final ResourceKey<CreativeModeTab> MISSILE = tabKey(ID_MISSILE);
    public static final ResourceKey<CreativeModeTab> WEAPON = tabKey(ID_WEAPON);
    public static final ResourceKey<CreativeModeTab> CONSUMABLE = tabKey(ID_CONSUMABLE);

    static {
        // CE icon: ModItems.ingot_uranium (ingots/nuggets/wires/machine parts tab)
        CREATIVE_MODE_TABS.register(ID_PARTS, () -> CreativeModeTab.builder()
                .icon(() -> new ItemStack(Items.BARRIER))
                .title(Component.translatable(translationKey(ID_PARTS)))
                .displayItems((parameters, output) -> CreativeTabContents.flush(PARTS, output))
                .build());

        // CE icon: ModItems.pellet_rtg (fuels/batteries/upgrades tab)
        CREATIVE_MODE_TABS.register(ID_CONTROL, () -> CreativeModeTab.builder()
                .icon(() -> new ItemStack(Items.BARRIER))
                .title(Component.translatable(translationKey(ID_CONTROL)))
                .withTabsBefore(tabId(ID_PARTS))
                .displayItems((parameters, output) -> {
                    CreativeTabContents.flush(CONTROL, output);
                    // CE's ControlTab.displayAllRelevantItems (upstream hbm-ce
                    // com.hbm.creativetabs.ControlTab) post-processes the flushed list: every
                    // stack whose item is IBatteryItem is replaced with an explicit full-charge
                    // copy plus (only when battery.getChargeRate(stack) > 0, i.e. not an SU-only
                    // battery) a preceding empty-charge copy, via battery.setCharge(stack, n).
                    // Not implemented here: no battery item type or charge data component has
                    // landed in the port yet (owned by a different Phase 1 area) - there is
                    // nothing to detect or split. Once a battery item and its charge component
                    // exist, replace this comment with logic that, for each battery supplier
                    // registered into CONTROL, accepts a full-charge ItemStack and (charge rate
                    // > 0) an empty-charge ItemStack built via ItemStack.set(chargeComponent, n)
                    // instead of relying on the plain flush above for those entries.
                })
                .build());

        // CE icon: ModItems.blueprints (templates/blueprints/siren tracks tab)
        CREATIVE_MODE_TABS.register(ID_TEMPLATE, () -> CreativeModeTab.builder()
                .icon(() -> new ItemStack(Items.BARRIER))
                .title(Component.translatable(translationKey(ID_TEMPLATE)))
                .withTabsBefore(tabId(ID_CONTROL))
                .withSearchBar()
                // Follow-up, not an omission: CE's TemplateTab.getBackgroundImageName() points at
                // item_search.png. .backgroundTexture(ResourceLocation) is the confirmed-real
                // NeoForge equivalent (see docs/phase1/creative_tabs_plan.md section 5) but is
                // left unset until a ported item_search.png texture exists under this mod's
                // namespace; withSearchBar() alone already defaults to vanilla's own
                // item_search texture, so the tab is fully functional without it meanwhile.
                .displayItems((parameters, output) -> CreativeTabContents.flush(TEMPLATE, output))
                .build());

        // CE icon: ModBlocks.ore_uranium (ore/mineral blocks tab)
        CREATIVE_MODE_TABS.register(ID_RESOURCE, () -> CreativeModeTab.builder()
                .icon(() -> new ItemStack(Items.BARRIER))
                .title(Component.translatable(translationKey(ID_RESOURCE)))
                .withTabsBefore(tabId(ID_TEMPLATE))
                .displayItems((parameters, output) -> CreativeTabContents.flush(RESOURCE, output))
                .build());

        // CE icon: ModBlocks.brick_concrete (construction blocks tab)
        CREATIVE_MODE_TABS.register(ID_BLOCKS, () -> CreativeModeTab.builder()
                .icon(() -> new ItemStack(Items.BARRIER))
                .title(Component.translatable(translationKey(ID_BLOCKS)))
                .withTabsBefore(tabId(ID_RESOURCE))
                .displayItems((parameters, output) -> CreativeTabContents.flush(BLOCKS, output))
                .build());

        // CE icon: ModBlocks.pwr_controller (machines/structure parts tab)
        CREATIVE_MODE_TABS.register(ID_MACHINE, () -> CreativeModeTab.builder()
                .icon(() -> new ItemStack(Items.BARRIER))
                .title(Component.translatable(translationKey(ID_MACHINE)))
                .withTabsBefore(tabId(ID_BLOCKS))
                .displayItems((parameters, output) -> CreativeTabContents.flush(MACHINE, output))
                .build());

        // CE icon: ModBlocks.nuke_man (nuke/bomb tab)
        CREATIVE_MODE_TABS.register(ID_NUKE, () -> CreativeModeTab.builder()
                .icon(() -> new ItemStack(Items.BARRIER))
                .title(Component.translatable(translationKey(ID_NUKE)))
                .withTabsBefore(tabId(ID_MACHINE))
                // Follow-up, not an omission: CE's NukeTab.getBackgroundImageName() points at
                // nuke.png. .backgroundTexture(ResourceLocation) is confirmed real (see
                // docs/phase1/creative_tabs_plan.md section 5) but left unset until a ported
                // nuke.png texture exists under this mod's namespace.
                .displayItems((parameters, output) -> CreativeTabContents.flush(NUKE, output))
                .build());

        // CE icon: ModItems.missile_nuclear (missiles/satellites tab)
        CREATIVE_MODE_TABS.register(ID_MISSILE, () -> CreativeModeTab.builder()
                .icon(() -> new ItemStack(Items.BARRIER))
                .title(Component.translatable(translationKey(ID_MISSILE)))
                .withTabsBefore(tabId(ID_NUKE))
                .displayItems((parameters, output) -> {
                    CreativeTabContents.flush(MISSILE, output);
                    // CE's MissileTab.displayAllRelevantItems (upstream hbm-ce
                    // com.hbm.creativetabs.MissileTab) appends nine hand-built
                    // ItemCustomMissile.buildMissile(chip, warhead, fuselage, stability,
                    // thruster) showcase stacks, each given a custom colored display name, after
                    // its normal population:
                    //   mp_chip_3 + mp_warhead_10_he + mp_fuselage_10_kerosene +
                    //     mp_stability_10_flat + mp_thruster_10_kerosene -> "Lil Bub"
                    //   mp_chip_3 + mp_warhead_10_incendiary + mp_fuselage_10_long_solid +
                    //     mp_stability_10_space + mp_thruster_10_solid -> "Long Boy"
                    //   mp_chip_3 + mp_warhead_10_nuclear + mp_fuselage_10_15_kerosene +
                    //     mp_stability_15_flat + mp_thruster_15_kerosene -> "Uncle Kim"
                    //   mp_chip_3 + mp_warhead_10_nuclear_large + mp_fuselage_10_15_balefire +
                    //     mp_stability_15_flat + mp_thruster_15_balefire_large ->
                    //     "Trotty's Toy Rocket"
                    //   mp_chip_3 + mp_warhead_15_nuclear_shark + mp_fuselage_15_kerosene_camo +
                    //     mp_stability_15_thin + mp_thruster_15_kerosene_triple ->
                    //     "Stealthy Shark"
                    //   mp_chip_3 + mp_warhead_15_he + mp_fuselage_15_kerosene_polite +
                    //     mp_stability_15_thin + mp_thruster_15_kerosene_dual -> "Polite Lad"
                    //   mp_chip_3 + mp_warhead_15_n2 + mp_fuselage_15_solid_desh +
                    //     mp_stability_15_thin + mp_thruster_15_solid_hexdecuple ->
                    //     "NERV's Leftover Missile"
                    //   mp_chip_5 + mp_warhead_15_mirv + mp_fuselage_15_kerosene_lambda +
                    //     mp_stability_15_soyuz + mp_thruster_15_kerosene ->
                    //     "7 For 1 Package Deal"
                    //   mp_chip_4 + mp_warhead_15_balefire +
                    //     mp_fuselage_15_20_kerosene_magnusson + (no stability chip) +
                    //     mp_thruster_20_kerosene -> "Hightower Missile"
                    // Not implemented here: none of the mp_chip_*/mp_warhead_*/mp_fuselage_*/
                    // mp_stability_*/mp_thruster_* missile part items, nor a ported
                    // ItemCustomMissile.buildMissile(...) equivalent, exist yet (owned by a
                    // different Phase 1 area). Once they land, add a private
                    // addShowcaseMissile(output, chip, warhead, fuselage, stability, thruster,
                    // name, color) helper here and call it for the nine combinations above.
                })
                .build());

        // CE icon: ModItems.gun_vortex (turrets/weapons/ammo tab)
        CREATIVE_MODE_TABS.register(ID_WEAPON, () -> CreativeModeTab.builder()
                .icon(() -> new ItemStack(Items.BARRIER))
                .title(Component.translatable(translationKey(ID_WEAPON)))
                .withTabsBefore(tabId(ID_MISSILE))
                .displayItems((parameters, output) -> CreativeTabContents.flush(WEAPON, output))
                .build());

        // CE icon: ModItems.bottle_nuka (drinks/kits/tools tab)
        CREATIVE_MODE_TABS.register(ID_CONSUMABLE, () -> CreativeModeTab.builder()
                .icon(() -> new ItemStack(Items.BARRIER))
                .title(Component.translatable(translationKey(ID_CONSUMABLE)))
                .withTabsBefore(tabId(ID_WEAPON))
                .displayItems((parameters, output) -> CreativeTabContents.flush(CONSUMABLE, output))
                .build());
    }

    private ModCreativeTabs() {
    }

    private static ResourceKey<CreativeModeTab> tabKey(String path) {
        return ResourceKey.create(Registries.CREATIVE_MODE_TAB, tabId(path));
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
