# Phase 11 parity census (live, 2026-09-01)

**This document supersedes `docs/phase6/PARITY_REPORT.md`.** Phase 6 (~54% weighted / ~67% unweighted)
is stale. Do not quote Phase 6 as current.

Source: static read of `upstream/hbm-ce` vs this port. Script: `scripts/phase11_parity_census.py`
(item/block ids via Phase 10 `extract_all_ids` — Java `register`/`reg`/`parts`/`parts1` + Mats autogen
+ plant/glyph/bedrock loops, plus flatten extras; **not** lang keys). Recipe JSON counted from
`src/main/resources` + `src/generated`. Quality bar: `docs/CE_PARITY_ADDENDUM.md`.

Verified this wave: `compileJava` 0.
`./gradlew runServer` after push (wiped world, port 25566). No new tag. `v0.0.1-rc2` stays.

## Top line

| | |
|---|---|
| **Weighted** (Σport / ΣCE) | **105.7%** (8213 / 7767) |
| **Unweighted** (mean of category %) | **103.8%** |
| Recipe/loot + machine-table + ItemPools reachability | **62.4%** (1615 / 2590) |
| CE `@AutoRegister` entities still missing | **none** |

Weighted is **above 99%** (need 7689). Gates: `compileJava` 0 + `runServer` Done.
Tag `v0.0.1-rc2`. Existing `v0.0.1-rc1` / `beta-82` / `beta-82.1` stay.

Largest remaining holes: **blocks 161**, **machine 89**, **vanilla 52**.
Weighted **105.7%**. Reachability **62.4%** — still the owner pain. Not a tag.
99%+ tag: https://github.com/scarrymany/hbm-neoforge-port/releases/tag/v0.0.1-rc2
90% playtest (kept): https://github.com/scarrymany/hbm-neoforge-port/releases/tag/v0.0.1-rc1

## Per-category

| Category | CE | Port | % | Method |
|---|---:|---:|---:|---|
| Items (flattened ids) | 1863 | 2590 | **139.0%** | +`scrap`/`scrap_nuclear`/`scrap_oil`/`pipes_steel` (`parts()`). `hidden(scrap_plastic)` + 6 `control(debris_*)` registered, census regex misses those helpers |
| Blocks | 1169 | 1008 | **86.2%** | unchanged |
| Fluids | 162 | 162 | **100%** | `FluidType` fields |
| Entities | 168 | 189 | **112.5%** | CE `@AutoRegister(name=)` under `entity/`. Port extras = spawn eggs + `entity_cloud_solinium` |
| Sounds | 381 | 381 | **100%** | `SoundEvent` / `DeferredHolder` fields |
| Vanilla crafting | ~1950 | 1898 | **97.3%** | CE estimate kept at 1950 |
| Machine recipes | ~2009 | 1920 | **95.6%** | +anvil Java table. ChemPlant unique **72=72**. Crystallizer unique **303/~309**. Shredder unique **201/200**. Cyclotron **42=42**. Anvil unique **67 / 200** (I/O that exists). Centrifuge 75/78 AE2 skip. |
| Advancements | 65 | 65 | **100%** | JSON under `data/hbm/advancement(s)` |

Texture leftover after aliases (Phase 10, do **not** invent art): items **9.3%** (164/1771), blocks
**16.6%** (96/579). See `docs/phase10/LEFTOVER_MISSES.md`.

## What changed this wave (reachability + Anvil GUI)

- Reachability **60.5% → 62.4%** (1567 → 1615 / 2590). Census `loot_outputs()` now
  scans live `ItemPools*` (`addHbm` / `ItemPoolLookups.add`) — those pools already
  fire from structures / satellite miners / vending. Not JSON self-drop padding.
- Sat leftovers whose I/O now exists: `fluorite` 4/4/15, `gravel_diamond` 1/1/3,
  `moon_turf` 48/48/5 + 32/32/7 + 16/16/5. Stale "absent" javadoc fixed.
- **Anvil E2E**: `NTMAnvil.useWithoutItem` → `AnvilMenu` (2+1, no BE) +
  `AnvilScreen` blit CE `gui_anvil.png` + `AnvilCraftPacket` construction from
  player inv. Smithing consume on take. Recipes: upgrades + gunmetal + plates +
  deco + coils/motors + machines/armor/fuel plates with registered I/O.
  TODO(CE: AnvilRecipes.java:75-130) hot/mold/cyanide/rename. `machine_boiler` AIR.
- Liquefaction: `oil_tar_crude` BITUMEN 75, `oil_tar_crack` BITUMEN 100,
  `lignite` COALOIL 150, `lead_block` LEAD 900. coal/wood tar remaps stay.
- Assembler SKIP7 not invented.

## Prior wave (Shredder + Cyclotron start)

- Family: **Shredder**. Leftover CE `registerDefaults` rows → JSON `data/hbm/recipe/shredder/`
  (TE already queries `ProcessingRecipes.SHREDDER_TYPE`). **201** files, **200** unique
  inputs (`quartz.json` + `quartz_item.json` both key `minecraft:quartz` — not triplicated).
- Registered I/O with existing CE png/lang/models: `scrap`, `scrap_nuclear`, `scrap_oil`,
  `scrap_plastic` (CE tab=null → hidden), `pipes_steel`, `debris_{concrete,shrapnel,exchanger,element,metal,graphite}`.
- Fixed `obsidian.json` output to `hbm:gravel_obsidian` (CE `:229`; was vanilla gravel).
- Wood OreDict loops → 3 tag recipes (`minecraft:logs`/`planks`/`saplings`), not per-wood files.
- `schrabidate_block` is Mats BLOCK autogen of CE `block_schrabidate` (not a second id).
- Sellafield flatten: one BlockItem → `scrap_nuclear`×1 (CE `:352` meta 0).
- Cited skips: `TODO(CE: ShredderRecipes.java:119-201)` registerPost,
  `:103-115` miss→scrap fallback (scrap exists; TE still rejects no-match),
  `:246` other `dustLapis`, `:348` old `bedrock_ore`, `:353-357` sellafield LEVEL 1-5,
  `:400-402` bobbleheads, `:412-423` GC/AR (commented in CE).
- Cyclotron: **42** unique CE rows now in `CyclotronRecipes` (was 12). Catalysts
  `part_*` (already registered). Li+gold → `nugget_mercury`. `dustPhosphorus` live
  member = `powder_fire`.

## Prior wave (ChemPlant verify + Crystallizer)

- ChemPlant: **72 unique `chem.*` names in CE = 72 in port**. Census 145 = `this.register` +
  `.register`. No rows added.
- Family: **Crystallizer** leftover live (`MachineCrystallizerBlockEntity.getOutput`). Unique
  **303 / ~309**. Bedrock wash loop 222, dye loop 18, leftover ores/utilities. Registered
  `coal_infernal` (CE fuel 4800, existing png/lang).
- Skips cited: `TODO(CE: CrystallizerRecipes.java:75)` LI.ore, `:79` malachite scrap,
  `:103` mustardwillow, `:199-216` AE2 / P_WHITE.dust / CINNABAR.dust.
- CE `gui_crystallizer_alt.png` blit-wired 176×204. Fluid-id slots stay
  `TODO(CE: ContainerCrystallizer.java:38-42)`.
- Centrifuge 75/78 + AE2 skip unchanged.

## Prior wave (104.2% → 104.3%, 8104 / 7767)

- Family: **Centrifuge** leftover rows now in the existing machine table (BE already calls
  `CentrifugeRecipes.getOutput`). Added `chunk_ore_rare`, `ore_aluminium`, `ore_nether_plutonium`,
  `block_slag`, `powder_ash_coal`, `crystal_aluminium`. Mercury outputs restored on redstone /
  `crystal_gold` / `crystal_redstone` (CE `ingot_mercury` id = `nugget_mercury`).
- Registered `block_slag` with CE texture/model (not `slag_block`, not orphan `block_slag_0`).
  Chunk-ore item models point at existing CE `chunk_ore.*` pngs.
- CE `gui_centrifuge.png` already in jar; screen blit-wired (power 37px + 4 progress columns).
  Menu slots match `ContainerCentrifuge` (44/57 in, 8/57 batt, 70/90/110/130 out, 156 upgrades,
  player 11/107). Gray-box gone.
- AE2 `oreCertusQuartz` skip: `TODO(CE: CentrifugeRecipes.java:243-254)`.
- AmmoPress fluid-slot leftover stays cited: `TODO(CE: AmmoPressRecipes.java:936+)`. CE TE has no tank.
- SuperComputer dropdown still skip — `ModuleMachineBase` class missing.
- SILEX DRX stays cited skip.

## Prior wave (103.7% → 104.2%, 8096 / 7767)

- CE `gui_electrolyser_fluid.png` / `gui_electrolyser_metal.png` already in jar; screens blit CE UVs
  (power / progress / molten tint / power-ok). Gray-box gone.
- Electrolyser fluid-id + canister I/O: `FluidTankNTM.setType`/`loadTank`/`unloadTank` + loaders
  (`FluidLoaderStandard` / `FillableItem` / `Infinite`). Slots 3-10 CE coords.
- Family: **AmmoPress** (next after Electrolyser/Mixer). 28 leftover `registerDefaults` rows
  (NUKE_BALEFIRE skipped — item id collides with bomb BlockItem). Registered `p45_*`,
  `nuke_standard/demo/high/tots/hive`, `assembly_nuke` with existing CE textures/lang. No invent.

## Prior waves (86.8% → 96.4%, 7489 / 7767)

Previous published snapshot: weighted **86.8%** / unweighted **93.3%**, vanilla **1416 / 1950
(72.6%)**, machine **1682 / 83.7%**, items **2056**, blocks **790**. Short of 90% by **249**.

### Extractor: already-registered helpers

`scripts/phase10_remap_v3.py` HELPERS now matches `registerBillet` / `registerPowder` /
`registerFuelPowder` / `registerParts` / `registerWaste` / `registerRtgPellet` /
`registerResource`. Those items were already in `BilletPowderItems` / `PlateCrystalWasteItems` /
etc. Census items **2056 → 2354**. Not dummy registration.

### Vanilla leftover JSON (CE-faithful, no invented rows)

- `scripts/phase11_wave12_minerals.py` — CE `MineralRecipes.java` `add1To9Pair` / `addMineralSet` /
  `addBillet` + explicit 9-pack. Skips unregistered I/O and banned results.
- `scripts/phase11_wave12_rods.py` — leftover empty-rod / RBMK / RTG rows whose ids are visible.
  Pile/PWR fuels already in datagen (`src/generated/.../rod/`); not re-emitted under new filenames.
- `scripts/phase11_wave13_leftover.py` — leftover Tool / Armor / Consumable / Exclusive crafts
  with registered I/O only.

Vanilla **1416 → 1866**. Recipe JSON total **2174 → 2624**. Dropped `gas_mask_filter` (DataComponent id, not an item).

Dropped (not invented): armor sets whose pieces are unregistered (`steel_helmet` / hazmat /
`gas_mask_m65`), `ring_starmetal`, `block_schrabidium`, SEDNA `part_stock_*` / `part_grip_*`,
`insert_*` / `cladding_*` / `pads_*` / `servo_set`, `block_steel` (Mats id is `steel_block`),
foods (`pancake` / `quesadilla` / …), pile/PWR concat ids already owned by datagen.

Banned results still skipped: `powder_sawdust` / `gem_tantalium` / `coil_tungsten`.

### Unchanged this wave

- Machine **1781 → 1809 / 90.0%**. AmmoPress 60→88. CE `registerDefaults` 89; NUKE_BALEFIRE
  skipped (`nuke_balefire` BlockItem collision, no invented id). Census CE 91 includes
  JSON `recipes.add` helper. ElectrolyserMetal 21/23.
  Cited leftovers: SILEX DRX `:417-431` (`undefined`), SuperComputer dropdown.
- Blocks **790 / 67.6%**. No dummy blocks.
- Assembler skip **7**. `SafeMenuScreens.bind` stays. `modId` stays `hbm`.
- Client NPE fix (`CrucibleBlocks.registerAll` + `SafeMenuScreens`) from prior HEAD.

## Exclusion list (only CE-lacks or deliberate skips)

- Double-slab flatten (1.21 has no double-slab block)
- CE `entity_clound_solinium` typo → `entity_cloud_solinium`
- `GlyphidHive` is a structure, not an entity
- Soyuz pad `LAUNCHING` is TBI **in CE itself**
- Projectile tails = fallback renderer
- Assembler expensive-mode `inputItemsEx` legs — dropped (same as prior assembler ports)
- PA recipe `#10` SBD.ingot() — no schrabidate INGOT autogen
- Texture misses with no CE file — documented, no invented art
- ElectrolyserMetal 21/23. CE GUI pngs copied + blit-wired (fluid + metal).
  Fluid-id / canister slots 3-10 live (`setType`/`loadTank`/`unloadTank`).
  `chunk_ore_*` registered; CE has no `chunk_ore*.png` — no invented art.
- PUREX chance-output / ICF / vitrification / naquadria — missing I/O
- Full Albion beam physics — detector runs the recipe table locally
- AmmoPress fluid-slot recipes (FLAME_*) — stored, not consumed (no tank on the press)
- PlasmaForge late-game recipes with 11–12 item stacks — counted, TE still 6 slots
- Assembler leftover skip **7**: `Fluids.X.getDict` ore stacks (hpcondenser / himars TB ×2 / mpw10taint),
  `ass.nitra` ChanceOutput, `ass.digimemer` (commented Mekanism in CE), `ass.50bmgbypass`
  (`black_diamond` is `ItemModHealth`, not a dummy)
- Combination coke/briquette/powder_ash item flatten — put sites present, AIR filtered
- Blast `meteorite_sword_*` — items missing, row dropped after register
- RockMill AE2 module / blueprint cycling — auto-detect instead
- Annihilator 528 gate — table registered unconditionally (same CE lines)
- SILEX DRX pellet — TODO(CE: SILEXRecipes.java:417-431) `ModItems.undefined` not registered
- Press meteorite sword / briquette / page_of — AIR filtered
- Rotary `ModuleBurnTime` heat mods — vanilla burn/2
- WasteDrum RBMK rod heat path — `ItemRBMKRod.updateHeat` not in this port
- Fraction CE chat inspect — replaced with a real tank menu (not a stub)
- Compressor upgrades (`UpgradeManagerNT`) — slots exist, levels unused (manager not ported)
- Compressor compact — still a cube casing (assembler output only)
- Cracker CE overlay — replaced with a real tank menu (not a stub)
- Reformer / hydrotreater canister `loadTank`/`unloadTank` — FluidTankNTM item-canister subsystem
  not ported (same skip as diesel / crystallizer / PWR)
- Coker pollution soot increment — PollutionHandler tick not wired on this TE
- Vacuum distill canister unload — FluidTankNTM item-canister subsystem not ported
- Radiolysis sterilize (`ntmContagion` NBT) — not ported; slots exist
- Flare pollution / particles / entity fire / tilt — skipped
- Flare / EPress `UpgradeManagerNT` — slot scan by `ItemMachineUpgrade` type/tier instead
- PyroOven tar→soot — `powder_ash_*` now registered; row still skipped pending wire-up
- PyroOven / arc furnace pollution / audio / particles / lid animation — skipped
- Arc furnace vanilla furnace autogen — RecipeManager not available at commonSetup
- Arc furnace `sand_quartz` → `glass_quartz` — `sand_quartz` unregistered
- Exposure expensive-mode DEGENERATE_MATTER — cheap path only
- Pyro / arc / exposure `UpgradeManagerNT` — slot scan by `ItemMachineUpgrade` type/tier
- Slopper animation / entity shred — skipped
- Turbofan pollution / particles — skipped
- Radgen depleted/tiny leftovers — items unregistered
- Hephaestus `volcanic_lava_block` — unregistered; CE overlay-only (port has a real menu)
- Wood burner `powder_ash_*` registered; ash emit + `loadTank` canister path not ported
- Wood burner / slopper / turbofan `UpgradeManagerNT` — slot scan by type/tier instead
- Iron / steel furnace pollution + particles; steel ore bonus
- Firebox `ModuleBurnTime` fuel-class mods + ashpit + door anim
- Oilburner `loadTank` / pollution
- Sawmill blade / entity shred / overspeed (`sawblade` unregistered → blade assumed)
- Heaters / steel furnace / sawmill CE crafts are **anvil**, not vanilla
- SuperComputer CE recipe dropdown (`IControlReceiver` / ModuleMachineBase) — auto-match instead
  TODO(CE: TileEntityMachineSuperComputer.java:186-194)
- Autosaw entity shred skipped
- `catalytic_converter` `ANY_BISMOID.ingot()` → `ingot_bismuth` (no `any_bismoid` tag)
- wave11 drops: `powder_fire` / `key` / `gear_large` / `sawblade` / `mold_base` unregistered
- wave13 drops: unregistered armor pieces / SEDNA parts / inserts / claddings / foods / `block_steel`
- fluid-NBT / `OreDictionary.WILDCARD` / LBSM-gated / commented CE crafts skipped

## Recipe-graph reachability (cheap)

**62.4%** (1615 / 2590). JSON loot + Java machine-table **outputs** + live `ItemPools*`
(`addHbm` / `ItemPoolLookups.add`). Inputs not counted. Self-drop block loot not added.

Before this wave: **60.5%** (1567 / 2590). +48 honest (pools that already fire + anvil
outs that exist + sat leftovers `fluorite`/`gravel_diamond`/`moon_turf`).

## Next single gap

**Anvil** unique: CE **200** Mod* outs / 236 add-sites vs port **67** with registered I/O.
GUI is live this wave. Leftover rows need unregistered I/O (hot/mold/shell/pipe/sawblade/
recycling). Do not invent.

Next other family by unique CE vs port (not regex 145-style): Press **41/38**,
Combination ~done, Exposure done, Liquefaction leftovers landed, Solidification live.
Assembler skip 7. `v0.0.1-rc2` stays.
Anvil E2E: smithing consume/produce **yes** (menu slots). Construction **yes**
(`AnvilCraftPacket` + player inv). GUI blit CE `gui_anvil.png`, not gray-box.

## Entities (Phase 9 leftovers)

All leftover classes spawn-wired at prior HEAD. No other CE `@AutoRegister` entity missing.
Cites: `docs/phase9/ENTITY_CENSUS.md`.
