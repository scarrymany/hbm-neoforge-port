
# Research report — mrec-05-purex-misc

Scope: CE's `PUREXRecipes.java` (518 ln), `SolidificationRecipes.java` (195 ln), `DFCRecipes.java`
(126 ln), `FusionRecipesLegacy.java` (80 ln), all under
`upstream/hbm-ce/src/main/java/com/hbm/inventory/recipes/`. All four read in full. This is a
research-only deliverable — no implementation code was written.

## Scope confirmed

| File | Lines | In-CE structure |
|---|---:|---|
| `PUREXRecipes.java` | 518 | `class PUREXRecipes extends GenericRecipes<PUREXRecipe>` (singleton `INSTANCE`). Overrides `inputItemLimit()=3`, `inputFluidLimit()=3`, `outputItemLimit()=6`, `outputFluidLimit()=1`, `getFileName()="hbmPUREX.json"`. All content is one method, `registerDefaults()`, a flat sequence of **58** `this.register((PUREXRecipe) new PUREXRecipe("id").setup(duration,power)...)` builder-chain calls (no loops, no table) grouped by CE's own `//` comments into 10 sub-families (CP-1 pile, ZIRNOX, Plate Fuel, PWR, Molten Salt, Watz, ICF, Vitrification, Schrabidium, plus 3 standalone billet-reprocessing recipes at the top). 2 of the 58 (`watznaqadah`/`watznaqadria`) are inside an `if (!OreDictionary.getOres("nuggetNaquadria").isEmpty())` guard — conditional on a Naquadria-integration mod being present even in CE. |
| `SolidificationRecipes.java` | 195 | `class SolidificationRecipes extends SerializableRecipe`. `registerDefaults()` is a flat sequence: 11 direct `registerRecipe(FluidType, int, Item\|Block\|ItemStack)` calls, 9 more `registerRecipe(...)` calls keyed by `oil_tar`+`EnumTarType` via `OreDictManager.DictFrame.fromOne(...)`, 1 direct `BALEFIRE` call, then **26** single-arg `registerSFAuto(FluidType)` calls (2 more, `GAS`/`BIOGAS`, are commented out in CE's own source — disabled, not real entries) plus **1** three-arg override `registerSFAuto(BALEFIRE, 24_000_000L, solid_fuel_bf)`. `registerSFAuto` is a real formula, not a table: `mB = tuPerSF * 1000 * 1.25 / fluid.getTrait(FT_Flammable.class).getHeatEnergy()`, rounded down to a clean multiple of 10/100/1000 depending on magnitude. Backing store is `HashMap<FluidType, Pair<Integer,ItemStack>>` — **one output per fluid type**, so the 1 duplicate key (`BALEFIRE`, registered both directly at line 85 and via the override at line 115) collapses to 1 entry (the second `put` wins) — **47 distinct FluidType→output entries**, not 48. |
| `DFCRecipes.java` | 126 | Plain utility class, **no `extends` clause at all** — not part of CE's `SerializableRecipe`/JSON-loader framework (no `readRecipe`/`writeRecipe`/`getFileName`). `register()` calls exactly **1** `setRecipe(long requiredFlux, ItemStack in, ItemStack out)`. The rest of the file is a static `LinkedHashMap<ComparableStack, Object[]>` lookup table + a JEI `IRecipeWrapper` inner class (`DFCRecipe`) for display. |
| `FusionRecipesLegacy.java` | 80 | Plain utility class, no `extends`. Four parallel static `HashMap<FluidType, X>` tables (`delays`, `levels`, `byproducts`, `steamprod`), each populated by a `static { }` block with exactly the same **6** `FluidType` keys (`PLASMA_DT`, `PLASMA_DH3`, `PLASMA_HD`, `PLASMA_HT`, `PLASMA_XM`, `PLASMA_BF`) — 6 "recipes" × 4 fields = 24 data points. Plus 4 trivial getter methods and one `getRecipes()` JEI-display helper. |

**Machine correspondence, verified by grep + reading CE source:**

| Recipe class | CE consumer (block/TE) | This port's equivalent | Status |
|---|---|---|---|
| `PUREXRecipes` | `MachinePUREX` (block) / `TileEntityMachinePUREX` (TE), `GUIMachinePUREX`/`ContainerMachinePUREX` | **None found.** `grep -rn "PUREX" src/main/java/com/hbm` returns exactly 1 hit, a javadoc comment in `AssemblerRecipe.java:38` ("...family that actually needs them (chemical plant, PUREX, etc.)...") — not an implementation. **Both the machine and its recipe data are unbuilt.** |
| `SolidificationRecipes` | `MachineSolidifier` (block) / `TileEntityMachineSolidifier` (TE, package `com.hbm.tileentity.machine.oil` — same family as CE's oil-refinery chain) | **None found.** `grep -rn "Solidif" src/main/java/com/hbm` returns 0 hits. This port does have the sibling oil-chain blocks (`OilChainBlocks.java`, `ore_oil_sand` confirmed registered) but no Solidifier. **Both machine and recipe data unbuilt.** |
| `DFCRecipes` | **Not** the `dfc_emitter`/`dfc_injector`/`dfc_receiver`/`dfc_stabilizer`/`dfc_core` casing blocks (those are plain `CoreComponent`/`CoreCore` decorative blocks with no tile entity found referencing `DFCRecipes`). The real consumer is `TileEntityCrateTungsten` (backing block `crate_tungsten`), which implements `ILaserable` and, in `addEnergy(...)`, looks up `DFCRecipes.getRequiredFlux(stack)`/`getOutput(stack)` to transmute an item once enough laser energy has accumulated (and separately charges an `ItemCrucible` melee weapon to 3 charges once `energy > 10_000_000`). | **Partially present.** This port already has `crate_tungsten` (`src/main/java/com/hbm/blocks/machine/StorageMachineBlocks.java:78`, `CrateType.TUNGSTEN`, hardness/resistance `15.0F`/`10000.0F` matching CE's `BlockStorageCrateRadResistant` exactly) and a generic `CrateBlockEntity`/`CrateBlock`, and separately already has `ILaserable` (`src/main/java/com/hbm/interfaces/ILaserable.java`, a clean NeoForge `BlockPos`/`Direction` port of CE's interface) and `ItemCrucible` (`src/main/java/com/hbm/items/weapon/ItemCrucible.java` — the **melee weapon**, a `getCharges`/`charge`/`discharge` data-component API already fully ported). **But `CrateBlockEntity` does not implement `ILaserable` and has no tungsten-specific `addEnergy`/transmutation branch** (confirmed by grep — 0 hits for `ILaserable`/`addEnergy` in `CrateBlockEntity.java`) — the laser-charging/DFC-lookup behavior itself is unbuilt, only its two supporting pieces (the crate block, the interface, the weapon) exist. |
| `FusionRecipesLegacy` | **No consumer found anywhere in CE's current source.** Repo-wide grep (`grep -rn "FusionRecipesLegacy" upstream/hbm-ce`) returns exactly one hit: the class's own declaration line. Neither `TileEntityICF.java` (355 ln) nor `TileEntityWatz.java` (725 ln) — CE's real, live ICF/Watz reactor tile entities — reference `byproduct`/`delay`/`breedingLevel`/`steamprod`/`PLASMA_` anywhere (checked directly, 0 matches each). This class appears to be **dead/orphaned code in CE itself**, superseded by whatever mechanic those two TEs actually use (not read in this pass — out of this task's file list). | This port's `FusionBlocks`/`FusionBlockEntities` (`ICF_REACTOR`, `ICF_CONTROLLER`, `ICF_PRESS`, `WATZ_REACTOR`) is the structural equivalent of CE's ICF/Watz family and already exists (confirmed built, Phase 2). | See **Open questions/risks** — porting `FusionRecipesLegacy`'s data as-is would produce equally-dead code unless further research finds a real consumer CE itself uses (this task's file list did not include `TileEntityICF.java`/`TileEntityWatz.java`, so their real byproduct logic, if any, is unread). |

One naming note worth flagging explicitly: CE has **two unrelated things named "Crucible."** `ModItems.crucible` / `com.hbm.items.weapon.ItemCrucible` (the melee weapon `TileEntityCrateTungsten.addEnergy` charges) is **already ported** in this port (`src/main/java/com/hbm/items/weapon/ItemCrucible.java`). The **ore-melting Crucible** (`com.hbm.blocks.machine.MachineCrucible` / `TileEntityCrucible` / `CrucibleRecipes.java`, Phase 6's audit "root cause #1", ~700+ blocked items) is a completely different class family and remains unported. Do not conflate the two when scoping DFC work.

## Already covered by this port

**None of the four files' recipe data is ported** (confirmed by grep — `PUREX`/`Solidif`/`FusionRecipesLegacy` all return 0 real hits; `DFC` returns 2 hits, both false positives: an unrelated tooltip string `"[DFC Core]"` in `ItemAMSCore.java:45` and an unrelated translation key `trait.dfcFuel` in `ModLanguageProvider.java:911` — coincidental reuse of the "DFC" initials, not the recipe system).

However, **three independent prior-phase files already documented specific items these four files need, and confirm those items are still missing** — cross-referenced here rather than re-discovered blind:
- `src/main/java/com/hbm/datagen/ModRecipeProvider.java:367-369`: "The 7th call CE makes, for `nuclear_waste_vitrified`/`_tiny`, is skipped — neither item was found registered anywhere in this port."
- `src/main/java/com/hbm/inventory/recipes/chem/GasCentrifugeRecipes.java:29-32`: "`dust` (plain rock dust) and `nuclear_waste_tiny` are not yet registered in this port."
- `src/main/java/com/hbm/inventory/recipes/RefineryRecipes.java:39-47`: "CE's `ModItems.sulfur` and `ModItems.oil_tar` (with `ItemEnums.EnumTarType`) are not registered items in this port yet" — directly blocks all 9 `oil_tar`-keyed `SolidificationRecipes` entries.

The port's own `docs/phase2/items_tool_machine_coupling_and_recipe_system.md` (§"Key design/API decisions", read in full for this task) already did design work directly relevant to `PUREXRecipes` specifically: it names `PUREXRecipes` as 1 of the **9** CE recipe classes that build CE's `GenericRecipe`/`GenericRecipes<T>` shape (the others: `AssemblyMachineRecipes`, `ChemicalPlantRecipes`, `FusionRecipes` [note: **not** `FusionRecipesLegacy`, a different, still-live CE class], `PrecAssRecipes`, `PlasmaForgeRecipes`, `BlastFurnaceRecipesNT`, `RockMillRecipes`, `SuperComputerRecipes`), and lays out a complete target 1.21 shape for that whole family (`HbmMachineRecipe implements Recipe<HbmMachineRecipe.Input>`, JSON-backed, sealed `IOutput` interface with a `MapCodec` for `ChanceOutput`/`ChanceOutputMulti`) — see **Recommended implementation shape** below. That doc also ships a compile-time stand-in, `src/main/java/com/hbm/inventory/recipes/loader/GenericRecipe.java`, whose own javadoc states plainly: "this class only carries the slice `ItemBlueprints`/`ItemBlueprintFolder` actually read today... Whoever ports a real 'GenericRecipe-shaped' machine (the 9 named above) should extend or replace this with the real input/output/duration/power fields at that time — this class is intentionally not that." **`HbmMachineRecipe` does not exist anywhere in the port yet** (confirmed by grep) — the real gap for `PUREXRecipes` is this not-yet-built shared infrastructure, not merely "the 58 recipes weren't typed in."

The remaining gap for all four files, precisely: **58 + 47 + 1 + 6 = 112 recipe/data entries, 0 ported**, plus (for PUREX and Solidification) their machine block/block-entity, plus (for DFC) the laser-charging behavior on an already-existing block, plus (for `HbmMachineRecipe`) shared multi-io/chance-output recipe infrastructure that no machine in this port has needed yet.

## Full recipe/entry catalog

### PUREXRecipes.java — all 58 entries

All recipes use `.setIconToFirstIngredient()` unless noted. "K500+N250" = the common input-fluid pair `FluidStack(KEROSENE,500)` + `FluidStack(NITRIC_ACID,250)`, used by 37 of the 58 recipes (all of ZIRNOX/Plate/PWR/Watz plus `schrabzirnox`... note `schrabzirnox`/`schrabpwr`/`schrabmen` instead use `SOLVENT 4000 + SCHRABIDIC 500`, called out separately). Duration is in ticks, power in HE/tick (CE's own units).

| # | id | dur/pow | group | input items | input fluids | output items | output fluid | notes |
|---|---|---:|---|---|---|---|---|---|
| 1 | purex.uzh | 600/1000 | — | billet_uranium_fuel, 3× billet_zirconium (tag) | NITRIC_ACID 1000, HYDROGEN 4000 | 4× billet_uzh | — | |
| 2 | purex.flashgold | 600/1000 | — | 1× billet_au198 (tag), pellet_charged | AMAT 1000 | 2× billet_balefire_gold | — | |
| 3 | purex.flashlead | 600/1000 | — | 1× billet_pb209 (tag), billet_balefire_gold | AMAT 1000 | 1× billet_flashlead | — | |
| 4 | purex.pilepu | 40/100 | autoswitch.pile | pile_rod_plutonium | SULFURIC_ACID 100 | 2× billet_pu_mix, 1× billet_uranium, 2× plate_iron | — | nameWrapper=purex.recycle |
| 5 | purex.pilethorium | 40/100 | autoswitch.pile | pile_rod(mk2)=THORIUM_FUEL | SULFURIC_ACID 100 | 2× billet_thorium_fuel, 1× billet_nuclear_waste | — | nameWrapper=purex.recycle |
| 6 | purex.pilepu239 | 40/100 | autoswitch.pile | pile_rod_pu239 | SULFURIC_ACID 100 | 1× billet_pu239, 1× billet_pu_mix, 1× billet_uranium, 2× plate_iron | — | nameWrapper=purex.recycle |
| 7 | purex.zirnoxnu | 100/1000 | autoswitch.zirnox | waste_natural_uranium | K500+N250 | 1× nugget_u238, 2× nugget_pu_mix, 1× nugget_pu239, 2× nuclear_waste_tiny | — | nameWrapper=purex.recycle |
| 8 | purex.zirnoxmeu | 100/1000 | autoswitch.zirnox | waste_uranium | K500+N250 | 1× nugget_pu_mix, 2× nugget_plutonium, 1× nugget_technetium, 2× nuclear_waste_tiny | — | " |
| 9 | purex.zirnoxthmeu | 100/1000 | autoswitch.zirnox | waste_thorium | K500+N250 | 1× nugget_u238, 1× nugget_th232, 2× nugget_u233, 2× nuclear_waste_tiny | — | " |
| 10 | purex.zirnoxmox | 100/1000 | autoswitch.zirnox | waste_mox | K500+N250 | 1× nugget_pu_mix, 1× nugget_technetium, 1× nugget_u238, 3× nuclear_waste_tiny | — | " |
| 11 | purex.zirnoxmep | 100/1000 | autoswitch.zirnox | waste_plutonium | K500+N250 | 2× nugget_pu_mix, 1× nugget_technetium, 3× nuclear_waste_tiny | — | " |
| 12 | purex.zirnoxheu233 | 100/1000 | autoswitch.zirnox | waste_u233 | K500+N250 | 1× nugget_u235, 1× nugget_neptunium, 1× nugget_technetium, 3× nuclear_waste_tiny | — | " |
| 13 | purex.zirnoxheu235 | 100/1000 | autoswitch.zirnox | waste_u235 | K500+N250 | 1× nugget_pu238, 1× nugget_neptunium, 1× nugget_technetium, 3× nuclear_waste_tiny | — | " |
| 14 | purex.zirnoxles | 100/1000 | autoswitch.zirnox | waste_schrabidium | K500+N250 | 2× nugget_beryllium, 1× nugget_pu239, 1× nuclear_waste_tiny, 2× nuclear_waste_tiny | — | " (note: CE lists `nuclear_waste_tiny` output twice, 1+2 — likely a CE authoring quirk, preserve as-is) |
| 15 | purex.zirnoxzfbmox | 100/1000 | autoswitch.zirnox | waste_zfb_mox | K500+N250 | 3× nugget_zirconium, 1× nugget_technetium, 1× nugget_pu_mix, 1× nuclear_waste_tiny | — | " |
| 16 | purex.platemox | 100/1500 | autoswitch.plate | waste_plate_mox | K500+N250 | 1× powder_sr90_tiny, 3× nugget_pu_mix, 1× powder_cs137_tiny, 4× nuclear_waste_tiny | — | nameWrapper=purex.recycle |
| 17 | purex.platepu238be | 100/1500 | autoswitch.plate | waste_plate_pu238be | K500+N250 | 1× nugget_beryllium, 1× nugget_pu238, 2× powder_coal_tiny, 2× nugget_lead | — | " |
| 18 | purex.platepu239 | 100/1500 | autoswitch.plate | waste_plate_pu239 | K500+N250 | 2× nugget_pu240, 1× nugget_technetium, 1× powder_cs137_tiny, 5× nuclear_waste_tiny | — | " |
| 19 | purex.platera226be | 100/1500 | autoswitch.plate | waste_plate_ra226be | K500+N250 | 2× nugget_beryllium, 2× nugget_polonium, 1× powder_coal_tiny, 1× nugget_lead | — | " |
| 20 | purex.platesa326 | 100/1500 | autoswitch.plate | waste_plate_sa326 | K500+N250 | 1× nugget_solinium, 1× powder_neodymium_tiny, 1× nugget_tantalium, 6× nuclear_waste_tiny | — | " |
| 21 | purex.plateu233 | 100/1500 | autoswitch.plate | waste_plate_u233 | K500+N250 | 1× nugget_u235, 1× powder_i131_tiny, 1× powder_sr90_tiny, 6× nuclear_waste_tiny | — | " |
| 22 | purex.plateu235 | 100/1500 | autoswitch.plate | waste_plate_u235 | K500+N250 | 1× nugget_neptunium, 1× nugget_pu238, 1× nugget_technetium, 6× nuclear_waste_tiny | — | " |
| 23 | purex.pwrmeu | 100/2500 | autoswitch.pwr | pwr_fuel_depleted[MEU] | K500+N250 | 3× nugget_u238, 4× nugget_plutonium, 2× nugget_technetium, 3× nuclear_waste_tiny | — | nameWrapper=purex.recycle |
| 24 | purex.pwrheu233 | 100/2500 | autoswitch.pwr | pwr_fuel_depleted[HEU233] | K500+N250 | 3× nugget_u235, 3× nugget_pu238, 1× nugget_technetium, 5× nuclear_waste_tiny | — | " |
| 25 | purex.pwrheu235 | 100/2500 | autoswitch.pwr | pwr_fuel_depleted[HEU235] | K500+N250 | 3× nugget_neptunium, 3× nugget_pu238, 1× nugget_technetium, 5× nuclear_waste_tiny | — | " |
| 26 | purex.pwrmen | 100/2500 | autoswitch.pwr | pwr_fuel_depleted[MEN] | K500+N250 | 3× nugget_u238, 4× nugget_pu239, 2× nugget_technetium, 3× nuclear_waste_tiny | — | " |
| 27 | purex.pwrhen237 | 100/2500 | autoswitch.pwr | pwr_fuel_depleted[HEN237] | K500+N250 | 2× nugget_pu238, 4× nugget_pu239, 1× nugget_technetium, 5× nuclear_waste_tiny | — | " |
| 28 | purex.pwrmox | 100/2500 | autoswitch.pwr | pwr_fuel_depleted[MOX] | K500+N250 | 3× nugget_u238, 4× nugget_pu240, 2× nugget_technetium, 3× nuclear_waste_tiny | — | " |
| 29 | purex.pwrmep | 100/2500 | autoswitch.pwr | pwr_fuel_depleted[MEP] | K500+N250 | 2× nugget_lead, 4× nugget_pu_mix, 2× nugget_technetium, 3× nuclear_waste_tiny | — | " |
| 30 | purex.pwrhep239 | 100/2500 | autoswitch.pwr | pwr_fuel_depleted[HEP239] | K500+N250 | 2× nugget_pu_mix, 4× nugget_pu240, 1× nugget_technetium, 5× nuclear_waste_tiny | — | " |
| 31 | purex.pwrhep241 | 100/2500 | autoswitch.pwr | pwr_fuel_depleted[HEP241] | K500+N250 | 3× nugget_lead, 2× nugget_zirconium, 1× nugget_technetium, 6× nuclear_waste_tiny | — | " |
| 32 | purex.pwrmea | 100/2500 | autoswitch.pwr | pwr_fuel_depleted[MEA] | K500+N250 | 3× nugget_lead, 2× nugget_zirconium, 1× nugget_technetium, 6× nuclear_waste_tiny | — | " |
| 33 | purex.pwrhea242 | 100/2500 | autoswitch.pwr | pwr_fuel_depleted[HEA242] | K500+N250 | 3× nugget_lead, 2× nugget_zirconium, 1× nugget_technetium, 6× nuclear_waste_tiny | — | " |
| 34 | purex.pwrhes326 | 100/2500 | autoswitch.pwr | pwr_fuel_depleted[HES326] | K500+N250 | 3× nugget_solinium, 2× nugget_lead, 1× nugget_euphemium, 6× nuclear_waste_tiny | — | " |
| 35 | purex.pwrhes327 | 100/2500 | autoswitch.pwr | pwr_fuel_depleted[HES327] | K500+N250 | 4× nugget_australium, 1× nugget_lead, 1× nugget_euphemium, 6× nuclear_waste_tiny | — | " |
| 36 | purex.pwrbfbam | 100/2500 | autoswitch.pwr | pwr_fuel_depleted[BFB_AM_MIX] | K500+N250 | 9× nugget_am_mix, 2× nugget_pu_mix, 6× nugget_bismuth, 1× nuclear_waste_tiny | — | " |
| 37 | purex.pwrbfpu241 | 100/2500 | autoswitch.pwr | pwr_fuel_depleted[BFB_PU241] | K500+N250 | 9× nugget_pu241, 2× nugget_pu_mix, 6× nugget_bismuth, 1× nuclear_waste_tiny | — | " |
| 38 | purex.thoriumsalt | 100/10000 | — | 2× nugget_th232 (tag) | in: THORIUM_SALT_DEPLETED 16000 | 50%: 1× nugget_u233, 25%: 1× nuclear_waste_tiny | out: THORIUM_SALT 16000 | **chance outputs**; icon = fluid_icon(THORIUM_SALT) |
| 39 | purex.watzschrab | 60/10000 | autoswitch.watz | watz_pellet_depleted[SCHRABIDIUM] | K500+N250 | 15× nugget_solinium, 3× nugget_euphemium, 2× nuclear_waste | WATZ 1000 | nameWrapper=purex.recycle |
| 40 | purex.watzhes | 60/10000 | autoswitch.watz | watz_pellet_depleted[HES] | K500+N250 | 17× nugget_solinium, 1× nugget_euphemium, 2× nuclear_waste | WATZ 1000 | " |
| 41 | purex.watzmes | 60/10000 | autoswitch.watz | watz_pellet_depleted[MES] | K500+N250 | 12× nugget_solinium, 6× nugget_tantalium, 2× nuclear_waste | WATZ 1000 | " |
| 42 | purex.watzles | 60/10000 | autoswitch.watz | watz_pellet_depleted[LES] | K500+N250 | 9× nugget_solinium, 9× nugget_tantalium, 2× nuclear_waste | WATZ 1000 | " |
| 43 | purex.watzhen | 60/10000 | autoswitch.watz | watz_pellet_depleted[HEN] | K500+N250 | 12× nugget_pu239, 6× nugget_technetium, 2× nuclear_waste | WATZ 1000 | " |
| 44 | purex.watzmeu | 60/10000 | autoswitch.watz | watz_pellet_depleted[MEU] | K500+N250 | 12× nugget_pu239, 6× nugget_bismuth, 2× nuclear_waste | WATZ 1000 | " |
| 45 | purex.watzmep | 60/10000 | autoswitch.watz | watz_pellet_depleted[MEP] | K500+N250 | 12× nugget_pu241, 6× nugget_bismuth, 2× nuclear_waste | WATZ 1000 | " |
| 46 | purex.watzlead | 60/10000 | autoswitch.watz | watz_pellet_depleted[LEAD] | K500+N250 | 6× nugget_lead, 12× nugget_bismuth, 2× nuclear_waste | WATZ 1000 | " |
| 47 | purex.watzboron | 60/10000 | autoswitch.watz | watz_pellet_depleted[BORON] | K500+N250 | 12× powder_coal_tiny, 6× nugget_co60, 2× nuclear_waste | WATZ 1000 | " |
| 48 | purex.watzdu | 60/10000 | autoswitch.watz | watz_pellet_depleted[DU] | K500+N250 | 12× nugget_polonium, 6× nugget_pu238, 2× nuclear_waste | WATZ 1000 | " |
| 49 | purex.watznaqadah | 60/10000 | autoswitch.watz | watz_pellet_depleted[NQD] | K500+N250 | 12× (ore-dict "nuggetNaquadria" item), 6× nugget_euphemium, 2× nuclear_waste | WATZ 1000 | **conditional** on `OreDictionary.getOres("nuggetNaquadria")` non-empty |
| 50 | purex.watznaqadria | 60/10000 | autoswitch.watz | watz_pellet_depleted[NQR] | K500+N250 | 12× nugget_co60, 6× nugget_euphemium, 2× nuclear_waste | WATZ 1000 | **conditional**, same guard |
| 51 | purex.icf | 300/10000 | — | icf_pellet_depleted | — | 1× icf_pellet_empty, 1× pellet_charged, 1× powder_iron | HELIUM4 1250 | nameWrapper=purex.recycle |
| 52 | purex.vitliquid | 100/1000 | — | ModBlocks.sand_lead | WASTEFLUID 1000 | 1× nuclear_waste_vitrified | — | |
| 53 | purex.vitgaseous | 100/1000 | — | ModBlocks.sand_lead | WASTEGAS 1000 | 1× nuclear_waste_vitrified | — | |
| 54 | purex.vitsolid | 300/1000 | — | ModBlocks.sand_lead, 4× nuclear_waste | — | 4× nuclear_waste_vitrified | — | |
| 55 | purex.schraranium | 200/1000 | — | ingot_schraranium | KEROSENE 2000, NITRIC_ACID 1000 | 3× nugget_schrabidium, 3× nugget_uranium, 2× nugget_neptunium | — | nameWrapper=purex.schrab |
| 56 | purex.schrabzirnox | 200/50000 | autoswitch.schrab | waste_plutonium | SOLVENT 4000, SCHRABIDIC 500 | 1× powder_schrabidium, 3× nugget_technetium, 4× nuclear_waste_tiny | — | " |
| 57 | purex.schrabpwr | 200/50000 | autoswitch.schrab | pwr_fuel_depleted[MEP] | SOLVENT 4000, SCHRABIDIC 500 | 1× powder_schrabidium, 3× nugget_technetium, 4× nuclear_waste_tiny | — | " |
| 58 | purex.schrabmen | 200/50000 | autoswitch.schrab | pwr_fuel_depleted[MEN] | SOLVENT 4000, SCHRABIDIC 500 | 1× powder_schrabidium, 3× nugget_technetium, 4× nuclear_waste_tiny | — | " |

The `autoswitch.*` `group` value is CE's "auto switch" UX feature (`GenericRecipes.autoSwitchGroups` / `setGroup`): the PUREX GUI auto-picks whichever recipe in that named group matches the first solid input item currently loaded, rather than the player selecting a recipe by name. This is real behavior, not decoration — the implement wave should carry the group string through if it reuses the `HbmMachineRecipe` design (§ below), which already accounts for it via the blueprint-pool/localization metadata fields.

### SolidificationRecipes.java — all 47 distinct entries (net of the 1 dedup)

Direct entries (11):

| FluidType | mB | output |
|---|---:|---|
| WATER | 1000 | `Blocks.ICE` (vanilla) |
| LAVA | 1000 | `Blocks.OBSIDIAN` (vanilla) |
| MERCURY | 125 | ingot_mercury |
| BIOGAS | 250 | 4× biomass_compressed |
| SALIENT | 1280 | 8× bio_wafer |
| ENDERJUICE | 100 | `Items.ENDER_PEARL` (vanilla) |
| WATZ | 1000 | ingot_mud |
| REDMUD | 450 | `Items.IRON_INGOT` (vanilla) |
| SODIUM | 100 | powder_sodium |
| LEAD | 100 | ingot_lead |
| SLOP | 250 | `ModBlocks.ore_oil_sand` |

`oil_tar`/`EnumTarType`-keyed entries (9, all use `OreDictManager.DictFrame.fromOne(ModItems.oil_tar, ItemEnums.EnumTarType.X)`):

| FluidType | mB | oil_tar sub-type |
|---|---:|---|
| OIL | 200 | CRUDE |
| CRACKOIL | 200 | CRACK |
| COALOIL | 200 | COAL |
| HEAVYOIL | 150 | CRUDE |
| HEAVYOIL_VACUUM | 150 | CRUDE |
| BITUMEN | 100 | CRUDE |
| COALCREOSOTE | 200 | COAL |
| WOODOIL | 1000 | WOOD |
| LUBRICANT | 100 | PARAFFIN |

`registerSFAuto`-generated entries (26 single-arg + 1 override, all with `tuPerSF=1_440_000L` except BALEFIRE which overrides to `24_000_000L` and `output=solid_fuel_bf` instead of `solid_fuel`): `SMEAR`, `HEATINGOIL`, `HEATINGOIL_VACUUM`, `RECLAIMED`, `PETROIL`, `NAPHTHA`, `NAPHTHA_CRACK`, `DIESEL`, `DIESEL_REFORM`, `DIESEL_CRACK`, `DIESEL_CRACK_REFORM`, `LIGHTOIL`, `LIGHTOIL_CRACK`, `LIGHTOIL_VACUUM`, `KEROSENE`, `KEROSENE_REFORM`, `SOURGAS`, `REFORMGAS`, `SYNGAS`, `PETROLEUM`, `LPG`, `BIOFUEL`, `AROMATICS`, `UNSATURATEDS`, `REFORMATE`, `XYLENE`, and `BALEFIRE` (override). CE's own source has `GAS`/`BIOGAS` `registerSFAuto` calls **commented out** (`//registerSFAuto(GAS);` / `//registerSFAuto(BIOGAS);`) — disabled by CE itself, correctly excluded from the count.

The `mB` for these 27 is **computed, not authored** — `registerSFAuto`'s formula (reproduced verbatim above) reads each fluid's `FT_Flammable.getHeatEnergy()` trait at class-init time. This port's `FT_Flammable` (`src/main/java/com/hbm/inventory/fluid/trait/FT_Flammable.java:23`) already has a `getHeatEnergy()` method matching CE's exactly, so the formula is directly reproducible in Java — **do not hand-transcribe 27 pre-computed mB numbers into JSON**; port the formula as a static Java data class (like `RefineryRecipes`/`ChemPlantRecipes` already do) so it stays correct if a fluid's heat-energy trait value ever changes.

### DFCRecipes.java — the 1 entry

| requiredFlux | input | output |
|---:|---|---|
| 10,000,000 | billet_polonium | billet_yharonite |

### FusionRecipesLegacy.java — all 6 rows × 4 columns

| Plasma | delay (ticks) | breeding level | byproduct | steam production |
|---|---:|---:|---|---:|
| PLASMA_DT | 900 | 1000 | pellet_charged | 30 |
| PLASMA_DH3 | 600 | 2000 | pellet_charged | 50 |
| PLASMA_HD | 1200 | 1000 | pellet_charged | 20 |
| PLASMA_HT | 900 | 1000 | pellet_charged | 25 |
| PLASMA_XM | 1200 | 3000 | powder_chlorophyte | 60 |
| PLASMA_BF | 150 | 4000 | powder_balefire | 160 |

## Item/registry dependency check

Every distinct item/block CE references was individually grepped against `src/main/java/com/hbm` (string-literal id search, not assumed). Summary:

**Confirmed already registered** (spot-checked file cited): all `nugget_*`/`billet_*`/`ingot_*`/`powder_*` material-shape items referenced (`src/main/java/com/hbm/items/IngotNuggetItems.java`, `BilletPowderItems.java`), all `waste_*`/`waste_plate_*` items (`src/main/java/com/hbm/items/PlateCrystalWasteItems.java`), `plate_iron`, `pile_rod_plutonium`/`pile_rod_pu239` (plain `ItemPileRod` loop, `MachineItems.java:362-363`), `pile_rod_mk2_thorium_fuel` (`ItemPileRodMK2.EnumPileRod.THORIUM_FUEL` exists, `ItemPileRodMK2.java:124`, registered `MachineItems.java:361-370`), all 12 `watz_pellet_depleted_<suffix>` variants used (`ItemWatzPellet.EnumWatzType` has all 12 constants, registered `MachineItems.java:582-591`), `icf_pellet_empty` (`IcfPressItems.java`), `nuclear_waste` (plain item, `NukeCasingItems.java:134`), `fluid_icon` (`MachineItems.java`), `crucible` the weapon (`WeaponMeleeItems.java`), `crate_tungsten` (`StorageMachineBlocks.java:78`), `ore_oil_sand` (`OilChainBlocks.java` / world-gen features), `ingot_mercury`/`ingot_mud`/`ingot_lead`/`powder_sodium` (`IngotNuggetItems.java`/`BilletPowderItems.java`), `billet_polonium`/`billet_yharonite` (`BilletPowderItems.java:105,138`), `billet_zirconium`/`billet_au198`/`billet_pb209`/`nugget_th232` (all confirmed — CE's `OreDictManager` isotope `DictFrame`s `ZR`/`AU198`/`PB209`/`TH232` correspond to already-registered materials/items in this port, see below).

**Confirmed missing** (grepped, 0 hits anywhere in `src/main/java/com/hbm`, and independently corroborated by 3 other already-committed port files' own javadocs):
- `pwr_fuel_depleted` (all 15 `EnumPWRFuel` grades) — this port only registers the **fresh** `pwr_fuel_<name>` grade (`MachineItems.java:407-413`); its own comment at line 403-405 states explicitly: "CE also registers `pwr_fuel_hot`/`pwr_fuel_depleted` as plain `ItemEnumMulti` byproduct markers sharing this same enum... not duplicated here." Blocks all 15 PWR-family PUREX recipes + 2 Schrabidium recipes (17 of 58).
- `nuclear_waste_tiny` — corroborated by `ModRecipeProvider.java:367-369` and `GasCentrifugeRecipes.java:29-32`. Blocks 9 ZIRNOX + 7 Plate Fuel + `thoriumsalt` + `schrabzirnox` PUREX recipes (18 of 58).
- `nuclear_waste_vitrified` — corroborated by `ModRecipeProvider.java:367-369`. Blocks all 3 Vitrification PUREX recipes.
- `sand_lead` (block) — CE: `BlockFallingBase(Material.SAND, "sand_lead", ...)` at `upstream/hbm-ce/.../ModBlocks.java:866`; not the same as `ore_oil_sand` (which is ported). Blocks all 3 Vitrification PUREX recipes.
- `pellet_charged` — CE: plain `ItemCustomLore`. Blocks `purex.flashgold`, `purex.icf`, and 4 of 6 `FusionRecipesLegacy` byproduct rows (DT/DH3/HD/HT).
- `icf_pellet_depleted` — distinct from the already-ported `icf_pellet`/`icf_pellet_empty`; this port's `ItemICFPellet` (`src/main/java/com/hbm/items/machine/ItemICFPellet.java`) models fuel selection via data components but has **no "depleted" state** (grepped, 0 hits for `depleted`/`DEPLETED` in that file). Blocks `purex.icf`.
- `biomass_compressed`, `bio_wafer` — named as "registered by other Phase [areas]" in `src/main/java/com/hbm/items/food/FoodItems.java:184`'s own comment, confirming this was a known, deliberate deferral. Blocks 2 Solidification entries (BIOGAS, SALIENT).
- `oil_tar` (+ `EnumTarType`) — corroborated by `RefineryRecipes.java:39-47`'s own TODO. Blocks all 9 `oil_tar`-keyed Solidification entries.
- `solid_fuel`, `solid_fuel_bf` — not found anywhere. Blocks all 27 `registerSFAuto`-generated Solidification entries (26 use `solid_fuel`, 1 override uses `solid_fuel_bf`).
- A port-side equivalent of CE's `OreDictionary.getOres("nuggetNaquadria")` check — this port has **zero** Naquadria material/item anywhere (grepped `aquadria`/`NAQUADRIA`, 0 hits), and per `Mats.java`'s own javadoc the whole CE ore-dictionary system it would query is deliberately not ported. Affects only the 2 conditional Watz PUREX recipes, which are themselves conditional even in CE (only fire if a Naquadria-integration mod is loaded) — **recommend treating as skip/N-A**, not a real blocker, unless the implement wave decides to add a Naquadria material from scratch (out of scope here).

**Ready-to-port tally for PUREXRecipes (item-readiness only, ignoring the missing-machine/missing-`HbmMachineRecipe`-infra blockers that apply to literally all 58):** **16 of 58 fully item-ready today** (`purex.uzh`, `purex.flashlead`, the 3 CP-1 pile recipes, the 10 unconditional Watz recipes, `purex.schraranium`) — the remaining 42 are blocked on exactly one or two of the 8 missing-item causes above (mostly `pwr_fuel_depleted` or `nuclear_waste_tiny`, which between them account for 35 of the 42).

**Ready-to-port tally for SolidificationRecipes:** **9 of 47 fully item-ready today** (the non-`oil_tar`, non-`solid_fuel*`, non-`biomass`/`bio_wafer` direct entries: WATER, LAVA, MERCURY, ENDERJUICE, WATZ, REDMUD, SODIUM, LEAD, SLOP) — the other 38 are blocked on `oil_tar` (9), `solid_fuel`/`solid_fuel_bf` (27), or `biomass_compressed`/`bio_wafer` (2).

**DFCRecipes:** 1 of 1 item-ready (`billet_polonium`→`billet_yharonite` both exist) — blocked only on the machine-behavior gap (§ above), not on items.

**FusionRecipesLegacy:** 4 of 6 rows item-ready (`powder_chlorophyte`/`powder_balefire` both exist for XM/BF; the other 4 rows need `pellet_charged`) — moot regardless, see Open questions/risks.

## Recommended 1.21.1 implementation shape

**PUREXRecipes → custom `RecipeType`/`RecipeSerializer` (`HbmMachineRecipe`), not JSON-as-vanilla-shaped, not a bespoke plain-Java table.** This is not a judgment call this task is making fresh — it is exactly what `docs/phase2/items_tool_machine_coupling_and_recipe_system.md` §"Key design/API decisions" point 2 already specifies, naming `PUREXRecipes` by name as one of the 9 classes this shape is for. Rationale, confirmed by reading the actual file: 3 item inputs + 3 fluid inputs + 6 item outputs (**with per-output chance**, e.g. `thoriumsalt`'s 50%/25% outputs) + 1 fluid output + duration/power + auto-switch group + name-wrapper localization is not expressible as a vanilla shaped/shapeless recipe or a `SingleRecipeInput`-based custom recipe — it needs a genuine multi-slot `RecipeInput` and a sealed `IOutput` (`ChanceOutput`/`ChanceOutputMulti`) codec, which is precisely what that design doc worked out (`MapCodec` per variant, JSON field layout kept close to CE's own `writeRecipe`/`readRecipe` for a clean 1:1 data extraction). **This shared `HbmMachineRecipe` infrastructure does not exist yet** — building it is a prerequisite for PUREX (and, once built, directly reusable for the other 8 classes that design doc names, several of which are separate research tasks' scope). If the implement wave's budget doesn't allow building `HbmMachineRecipe` first, a fallback matching this port's own already-proven "port now, JSON-override later" pattern (`ChemPlantRecipes.java`, `RefineryRecipes.java` — both read in full, both cited above as precedent) — a small static Java class with a `PurexRecipe` POJO (item inputs as `AStack[]`, fluid inputs as `FluidStack[]`, outputs as a small local chance-output wrapper, duration/power longs) — is lower-risk and unblocks the recipe *data* immediately without waiting on the shared infra, at the cost of not being JEI/datagen-friendly until migrated later. Either way, **a `MachinePUREX` block + block entity must also be built** — none exists yet, and per this port's own established convention (`ChemPlantBlockEntity` etc.) recipe *recognition* should be automatic from input contents, not CE's name-wrapper/group-driven GUI dropdown (already a documented, accepted behavior change for this port's other `GenericRecipe`-shaped machines).

**SolidificationRecipes → a plain static Java data class**, following the `RefineryRecipes.java`/`ChemPlantRecipes.java` precedent exactly (both read in full as this file's direct analogues: single-input-fluid-type-keyed, no chance outputs, no multi-input complexity). A `Map<FluidType, SolidificationOutput>` populated once by a `registerDefaults()`-equivalent static method, reproducing `registerSFAuto`'s formula in Java (not pre-baked into JSON, per the reasoning above) is the lowest-risk shape and needs no new shared infrastructure. Needs a new `MachineSolidifier`-equivalent block/block-entity (none exists).

**DFCRecipes → trivial, either shape works given only 1 entry.** Simplest: a one-line static constant/lookup matching CE's own `setRecipe`/`getRequiredFlux`/`getOutput` API shape (no JSON machinery needed for 1 entry). The real work here is **not** the recipe data but wiring `ILaserable`+`DFCRecipes`-lookup+`ItemCrucible`-charging behavior into (or as a `TUNGSTEN`-specific branch of) `CrateBlockEntity`, mirroring CE's `TileEntityCrateTungsten.addEnergy` (read in full above) — the block and the two supporting classes already exist, only this one method's behavior is missing.

**FusionRecipesLegacy → do not port as a live data source pending further research.** See Open questions/risks — this file has zero confirmed consumers in CE's current codebase. If the implement wave (or a follow-up research pass) reads `TileEntityICF.java`/`TileEntityWatz.java` in full and finds they use a *different*, still-live mechanism for plasma byproduct/breeding-level/steam-production data, port that mechanism's real data instead of this file's. If no such mechanism is found, this file is CE's own dead code and porting its 24 data points would only produce equally-dead code in the port — flag as "N/A, CE never wired it up" rather than spending implement-wave budget on it.

## Open questions / risks

1. **`FusionRecipesLegacy`'s true status is the single biggest open question in this assignment.** This task confirmed (grep, both directions) that no other class in CE's current source references it, and that CE's two real, live plasma-reactor tile entities (`TileEntityICF.java`, `TileEntityWatz.java`) contain no byproduct/delay/breeding-level/steam-production logic matching this file's shape at all. Those two files were **not** in this task's read list (out of scope for this assignment), so it remains possible — though this task judges it unlikely given the zero-hit grep — that they pull this same data through an indirection this task's greps didn't catch (e.g. reflection, a different static import alias). **Recommend**: whichever task next touches `TileEntityICF`/`TileEntityWatz` (or this port's already-built `IcfReactorBlockEntity`/`WatzReactorBlockEntity`) should re-check this specifically before either porting or discarding `FusionRecipesLegacy`.
2. **`OreDictManager`'s isotope `DictFrame`s (`ZR`, `AU198`, `PB209`, `TH232`) map cleanly onto this port's `Mats.java` materials/items by name, but the *mechanism* (ore-dict alias matching) does not exist in this port at all** — `Mats.java`'s own javadoc confirms the ore-dict coupling was deliberately dropped. Translating `new OreDictStack(ZR.billet(), 3)` into this port's tag-based ingredient model (`MaterialShapes.java`'s `c:<plural>/<material>` tag convention, e.g. `c:billets/zirconium`) is a mechanical but real translation step the implement wave needs to do per-ingredient, not a copy-paste.
3. **CE authoring quirk in `purex.zirnoxles`** (recipe #14 above): the output list has `nuclear_waste_tiny` listed twice with counts 1 and 2 (`new ItemStack(ModItems.nuclear_waste_tiny, 1), new ItemStack(ModItems.nuclear_waste_tiny, 2)`) rather than one stack of 3 — preserved as-is in the catalog above; worth a deliberate decision (collapse to one stack of 3, or keep as two separate output slots matching CE's exact `outputItemLimit()=6` slot semantics) rather than silently "fixing" it.
4. **`icf_pellet_depleted` may not need a new item id at all** if the implement wave models "depleted" as a new data component on the existing single `icf_pellet` item (matching that class's own already-chosen data-component approach for fuel-type/muon-catalysis state) rather than a CE-style separate registered id — this is a design choice for whoever owns `ItemICFPellet`, not something this research task should preempt.
5. **This port's `Fluids.java` was not individually re-verified against all 26 `registerSFAuto`-referenced fluid names** (`HEATINGOIL_VACUUM`, `DIESEL_CRACK_REFORM`, `XYLENE`, `REFORMATE`, etc.) given this task's time budget — Phase 6's `PARITY_REPORT.md` §3.3 already found this port's fluid-definition parity at ≈97.5% (158/162), so most are very likely present, but a spot-check of the specific 26 names before implementation is cheap insurance.
6. **Naquadria**: recommend explicit confirmation from whoever scopes Phase 7's material work on whether a Naquadria material is planned at all before deciding to skip `watznaqadah`/`watznaqadria` permanently vs. stub them behind a similar "if present" guard against a not-yet-existing port-side material.
