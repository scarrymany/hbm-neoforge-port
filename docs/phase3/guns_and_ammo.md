# Guns and ammo (concrete content) — Phase 3 research

Sources read in full:
- `upstream/hbm-ce/src/main/java/com/hbm/items/weapon/*.java` (21 files — the legacy/old-HBM gun
  package: `ItemGunBase`, `ItemGunShotty`, `ItemGunVortex`, `GunB93`, `GunB92Cell`,
  `WeaponizedCell`, `IMetaItemTesr`, `ItemAmmo`, `ItemAmmoArty`, `ItemAmmoContainer`,
  `ItemAmmoHIMARS`, `ItemCrucible`, `ItemCustomMissile`, `ItemDisperser`, `ItemGenericGrenade`,
  `ItemGrenadeDynamite`, `ItemGrenadeFishing`, `ItemMissile`, `ItemMissileStandard`,
  `ItemSwordCutter`, `GrenadeDispenserRegistry`)
- `upstream/hbm-ce/src/main/java/com/hbm/items/special/weapon/GunB92.java` (full — `gun_b92`
  actually lives here, misfiled out of `items/weapon`, already flagged by Phase 1's
  `DIGEST_REMAINDER.md`)
- `upstream/hbm-ce/src/main/java/com/hbm/items/weapon/sedna/**/*.java` — the *current*, actively
  maintained gun framework CE ships today: `GunConfig`, `Receiver`, `BulletConfig`,
  `ItemGunBaseNT`, `ItemGunBaseSedna` (headers, for field semantics only — the state-machine
  internals are the parallel gun-framework-core report's job) — and every one of the 24
  `factory/XFactory*.java` + `GunFactory.java` files in full or via targeted per-gun grep once the
  `GunConfig`/`Receiver`/`BulletConfig` field vocabulary was established (`.dmg()`, `.delay()`,
  `.dura()`, `.mag(new Magazine...)`, `.setDefaultAmmo()`, `.setItem()`, `.setCasing()` — all
  confirmed by full reads of `GunConfig.java`, `Receiver.java`, `BulletConfig.java`, and 8 of the 24
  factories read in full before switching to grep for the rest)
- `upstream/hbm-ce/src/main/java/com/hbm/items/weapon/sedna/mags/*.java` (all 7 — `IMagazine`,
  `MagazineBelt`, `MagazineEnergy`, `MagazineFluid`, `MagazineFullReload`, `MagazineInfinite`,
  `MagazineSingleReload`, `MagazineSingleTypeBase` — to confirm what "magazine size" means per
  reload style)
- `upstream/hbm-ce/src/main/java/com/hbm/items/ItemEnums.java` (`EnumCasingType`, 7 values, already
  ported), `com/hbm/handler/BulletConfigSyncingUtil.java` (full — the legacy bullet-config registry
  the old `ItemGunBase` system pulls from), `com/hbm/handler/guncfg/{Gun12GaugeFactory,
  GunEnergyFactory}.java` (full — the only two legacy `GunConfiguration` factories still
  instantiated)
- `com/hbm/items/ModItems.java` — every `gun_*`/`ammo_*` field declaration and initializer (grepped,
  not read start-to-end; ~2,900 lines) to get an authoritative census of what actually exists
- This port's own `com.hbm.damage.ModDamageTypes`, `com.hbm.packet.HbmNetwork`,
  `com.hbm.capability.HbmLivingAttachment`, `com.hbm.inventory.container.MenuBase` /
  `com.hbm.inventory.gui.GuiInfoContainer` (headers, to confirm the exact hooks Phase 3 gun content
  will plug into) plus `docs/phase1/{items_tool.md, items_special.md, items_food_gear.md,
  moditems_generative.md, DIGEST_REMAINDER.md, STATUS.md}` for what earlier research already named

## Headline finding: CE ships **two** unrelated gun frameworks, and the older one is dead code

Grepping `ModItems.java` for `gun_` fields returns **74** hits, not the "~8" Phase 1's
`moditems_generative.md` noted (that count only picked up the fields whose *declaration and
initializer* are the same line inside `ModItems.java` itself — `gun_b92`, `gun_b93`,
`gun_supershotgun`, `gun_vortex`, plus a couple of non-gun `gun_kit_*`/`gun_moist_nugget` items that
share the prefix by naming coincidence, not the 70 `gun_*` fields that are *declared* in
`ModItems.java` but *initialized* inside `com.hbm.items.weapon.sedna.factory.XFactory*.init()`
methods called from `GunFactory.init()`). The real content census is:

1. **Legacy system** (`com.hbm.items.weapon.ItemGunBase` + `GunConfiguration` +
   `BulletConfigSyncingUtil`, 1.7.10-vintage) — **4 items**: `gun_b92`, `gun_b93` (both bespoke
   charge-weapons, bypass the bullet-config system entirely), `gun_supershotgun`
   (`ItemGunShotty`), `gun_vortex` (`ItemGunVortex`). Confirmed by reading
   `BulletConfigSyncingUtil.java` in full: `gun_supershotgun`'s `Gun12GaugeFactory.getShottyConfig()`
   references `BulletConfigSyncingUtil.G12_NORMAL/G12_INCENDIARY/G12_SHRAPNEL/G12_DU/G12_AM/
   G12_SLEEK`, and `gun_vortex`'s `GunEnergyFactory.getVortexConfig()` references `R556_STAR` — **none
   of these six int constants are ever passed to `configSet.put(...)`** anywhere in the class's static
   initializer. `ItemGunBase.fire()`/`reload2()` call `BulletConfigSyncingUtil.pullConfig(...)`, get
   `null`, and early-return (`if (config == null) return;`). **These two guns are non-functional in
   current CE**: `gun_supershotgun` can be held, aimed, and its meathook grapple (right-click) works
   because that's independent logic, but left-click fire is a silent no-op; `gun_vortex` is fully
   silent (no ammo consumption possible either, `RELOAD_FULL` pulls the same dead config). This is a
   confirmed upstream bug/dead-code state, not a misreading — see "Key design/API decisions" for the
   porting recommendation.
2. **Sedna system** (`com.hbm.items.weapon.sedna.*`, actively maintained) — **70 items**, every one
   a `ItemGunBaseNT` (or a small subclass: `ItemGunPA`, `ItemGunDrill`, `ItemGunStinger`,
   `ItemGunChemthrower`, `ItemGunChargeThrower`, `ItemGunNI4NI`) built from a `GunConfig` + one or
   two `Receiver`s + a `BulletConfig` list. This is CE's real, current gun content and where nearly
   all of Phase 3's gun-content porting effort belongs.

Ammo is **not** one item per caliber. Both eras use a metadata/NBT-variant model:
- Legacy: no single ammo mega-item — individual `Item`/`ItemAmmo<E>` classes per family
  (`ammo_arty` 12 meta-variants, `ammo_shell` (240mm turret) meta-variants, etc).
- Sedna: **one item, `ammo_standard`** (`ItemEnumMulti<GunFactory.EnumAmmo>`, **101** enum constants
  — every caliber/loading in the game, see full list below) plus **one item, `ammo_secret`**
  (`ItemEnumMulti<GunFactory.EnumAmmoSecret>`, 8 easter-egg/joke rounds, hidden from the creative
  tab). A `BulletConfig` binds one `EnumAmmo`/`EnumAmmoSecret` constant (occasionally a raw
  `ItemStack`, e.g. energy weapons consume `ingot_polymer` as "capacitor" ammo, flamers consume
  fluid, the drill consumes `Fluids.GASOLINE`/`COALGAS`) to ballistics + a spent-casing visual +
  (for cased ammo) an `ItemEnums.EnumCasingType` + count that the Ammo Press (Phase 2 recipe system
  territory) consumes to produce it.

## Phase-3-safe scope

### The Sedna gun roster — 70 items, grouped by caliber/ammo family (`XFactory*` = the ammo family owner)

Every gun below is `new ItemGunBaseNT(WeaponQuality, "gun_xxx", GunConfig...)` unless the "Class"
column names a subclass. **Damage** is `Receiver.dmg()` (`baseDamage_DNA`) — the framework then
multiplies this by the fired `BulletConfig.damageMult` (mostly 1.0; per-round modifiers are listed
in the ammo tables) and by `1/projectilesMax` for multi-pellet rounds, so shotgun "damage per
pellet" numbers below are already the per-pellet share baked into the ammo config, not the
receiver's raw `dmg()`. **Fire rate** is `Receiver.delay()`/`.dry()` in ticks between shots (not
RPM — divide 1200 by the value for RPM at 20 TPS); `.rounds()` is shots-per-trigger-pull when >1
(shotguns firing multiple pellet-groups per squeeze, e.g. SPAS `rounds(1)` default vs `gun_liberator`
`rounds(4)`, matches the source snippets below verbatim). **Magazine** is the `Magazine*(index,
capacity)` argument; `MagazineBelt` has no fixed capacity (draws directly from any matching
inventory stack) so it is listed as "belt". **Ammo type consumed** lists the `BulletConfig` field
names registered in that gun's magazine (`.addConfigs(...)`) — these are the exact `EnumAmmo`
constants the gun accepts, in the order CE lists them (first = default reload pick when the mag is
empty, per `ItemGunBase`/`ItemGunBaseNT`'s "pick the first ammo type the player is carrying"
behavior carried over from the legacy system).

**`XFactoryBlackPowder` — flintlock (`STONE`/`STONE_AP`/`STONE_IRON`/`STONE_SHOT`, no casing item, black-powder rounds burn on fire not eject):**

| Item | Quality | Dura | Fire delay | Mag | Ammo accepted |
|---|---|---|---|---|---|
| `gun_pepperbox` | A_SIDE | 300 | 27 | 6 (full reload) | stone, flint(AP), iron(pen, ricochet x5), shot(6-pellet) |

**`XFactory357` (.357, `SMALL` casing, no steel-AP tier this caliber, revolver family):**

| Item | Quality | Dura | Dmg | Delay | Mag | Default ammo |
|---|---|---|---|---|---|---|
| `gun_light_revolver` | A_SIDE | 300 | 7.5 | 16 | 6 (full) | M357_SP x12 |
| `gun_light_revolver_atlas` | B_SIDE | 300 | 12.5 | 16 | 6 (full) | M357_JHP x12 |
| `gun_light_revolver_dani` | LEGENDARY (akimbo, dual `GunConfig`) | 30,000 | 15 | 11 | 6+6 (full, 2 receivers) | M357_EXPRESS x24 |

Ammo: `m357_bp` (0.75x dmg, black powder), `m357_sp` (baseline), `m357_fmj` (0.8x, +2 threshold
negation, 0.1 AP%), `m357_jhp` (1.5x dmg, 1.5x headshot, -0.25 AP%), `m357_ap` (1.5x dmg,
penetrates, no falloff, +5 threshold, 0.15 AP%), `m357_express` (1.5x dmg, penetrates, +2
threshold, 1.5x wear).

**`XFactory44` (.44, `SMALL` casing, lever-action + heavy revolver family):**

| Item | Quality | Dura | Dmg | Delay | Reload | Mag | Default ammo |
|---|---|---|---|---|---|---|---|
| `gun_henry` | A_SIDE | 300 | 10 | 20 | 25/11/14/8 (sequential single-load) | 14 (single reload) | M44_SP x14 |
| `gun_henry_lincoln` | B_SIDE | 300 | 20 | 20 | same | 14 | M44_JHP x14 |
| `gun_heavy_revolver` | A_SIDE | 600 | 15 | 14 | 46 (full) | 6 (full) | M44_SP x12 |
| `gun_heavy_revolver_lilmac` | LEGENDARY | 31,000 | 30 | 14 | 46 | 6 (adds secret `m44_equestrian_pip` round) | M44_JHP x12 |
| `gun_heavy_revolver_protege` | LEGENDARY | 31,000 | 30 | 14 | 46 | 6 (adds secret `m44_equestrian_mn7` round) | M44_JHP x12 |
| `gun_hangman` | LEGENDARY | 600 | 25 | 10 | 46 | 8 (full) | M44_FMJ x16 |

Ammo: `m44_bp`, `m44_sp`, `m44_fmj` (0.8x, +3 threshold, 0.1 AP%), `m44_jhp` (1.5x, 1.5x headshot,
-0.25 AP%), `m44_ap` (1.5x, penetrates, +7.5 threshold, 0.15 AP%), `m44_express` (1.5x, penetrates,
+3 threshold, 0.1 AP%, 1.5x wear), plus two zero-damage secret novelty rounds
(`m44_equestrian_pip`/`_mn7`) that spawn `EntityBoxcar`/`EntityTorpedo` on impact instead of dealing
damage (an "MLP" easter egg per the source comments — literal joke content, keep as-is).

**`XFactory9mm` (`SMALL`/`SMALL_STEEL` casing, SMG/pistol family):**

| Item | Quality | Dura | Dmg | Delay/Dry | Auto | Mag | Default ammo |
|---|---|---|---|---|---|---|---|
| `gun_greasegun` | A_SIDE | 3,000 | 3 | 4/40 | yes | 30 (full) | P9_SP x30 |
| `gun_lag` | A_SIDE | 1,700 | 25 | 4/10 | no | 17 (full) | P9_JHP x17 |
| `gun_uzi` | A_SIDE | 3,000 | 3 | 2/25 | yes | 30 (full) | P9_SP x30 |
| `gun_uzi_akimbo` | B_SIDE (dual `GunConfig`) | 3,000 | 3 | 2/25 | yes | 30+30 | P9_SP x60 |

Ammo: `p9_sp`, `p9_fmj` (0.8x, +2 threshold, 0.1 AP%), `p9_jhp` (1.5x, 1.5x headshot, -0.25 AP%),
`p9_ap` (`SMALL_STEEL` casing, 1.5x, penetrates, +5 threshold, 0.15 AP%).

**`XFactory45` (ammo-only file — no gun currently consumes P45_* in the read source; `gun_liberator`
uses the shared `all[]` 12ga array, not this caliber, despite the historical "Liberator = .45"
naming — confirm at implementation time whether a 1911-style pistol was cut or is simply unassigned
to this ammo elsewhere):** `p45_sp`, `p45_fmj` (0.8x/+2/0.1), `p45_jhp` (1.5x/1.5x hs/-0.25 AP%),
`p45_ap` (`SMALL_STEEL`, 1.25x, penetrates, +5 threshold, 0.15 AP%), `p45_du` (`SMALL_STEEL`, 2.5x,
penetrates, +15 threshold, 0.25 AP%).

**`XFactory22lr` (`SMALL`/`SMALL_STEEL`, zero knockback on every round, SMG/pistol):**

| Item | Quality | Dura | Dmg | Delay/Dry | Auto | Mag | Default ammo |
|---|---|---|---|---|---|---|---|
| `gun_am180` | A_SIDE | 4,425 | 2 | 1/10 | yes | 177 (full) | P22_SP x35 |
| `gun_star_f` | A_SIDE | 375 | 12.5 | 5/17 | no | 15 (full) | P22_SP x15 |
| `gun_star_f_akimbo` | B_SIDE (dual) | 375 | 12.5 | 5/17 | no | 15+15 | P22_SP x30 |

Ammo: `p22_sp`, `p22_fmj` (0.8x/+1/0.1), `p22_jhp` (1.5x/1.5x hs/-0.25), `p22_ap`
(`SMALL_STEEL`, 1.5x, penetrates, +2.5 threshold, 0.15 AP%).

**`XFactory556mm` (5.56×45, `SMALL`/`SMALL_STEEL`, assault-rifle family):**

| Item | Quality | Dura | Dmg | Delay/Dry | Auto | Mag | Default ammo |
|---|---|---|---|---|---|---|---|
| `gun_g3` | A_SIDE | 3,000 | 5 | 2/15 | yes | 30 (full) | R556_SP x30 |
| `gun_g3_zebra` | B_SIDE (scoped) | 6,000 | 7.5 | 2/15 | yes | 30 (**incendiary** ammo variant set `r556_inc_*`, see note below) | R556_JHP x30 |
| `gun_stg77` | A_SIDE (scoped) | 3,000 | 10 | 2/15 | yes | 30 (full) | R556_FMJ x30 |

Ammo: `r556_sp`, `r556_fmj` (0.8x/+4/0.1), `r556_jhp` (1.5x/1.5x hs/-0.25), `r556_ap`
(`SMALL_STEEL`, 1.5x, penetrates, +10 threshold, 0.15 AP%). *`gun_g3_zebra`'s magazine references
`r556_inc_sp/fmj/jhp/ap` — an incendiary parallel set — that were not visible in the grep window
read for this caliber; confirm their exact modifiers (likely `+setOnImpact` ignite, same damage
curve) by reading `XFactory556mm.java` lines ~30-45 directly before implementing this one gun.*

**`XFactory762mm` (7.62×54/51, `SMALL`/`SMALL_STEEL`, battle-rifle/minigun/laser-minigun family; this
file also defines the three `CAPACITOR*`-consuming "lacunae" energy configs reused by
`gun_minigun_lacunae`):**

| Item | Quality | Dura | Dmg | Delay | Auto | Mag | Default ammo |
|---|---|---|---|---|---|---|---|
| `gun_carbine` | A_SIDE | 3,000 | 15 | 5/15 | no | 14 (full, sequential) | R762_SP x14 |
| `gun_minigun` | A_SIDE | 50,000 | 6 | 1/15 | yes | belt | R762_FMJ x30 |
| `gun_minigun_lacunae` | LEGENDARY | 50,000 | 12 | 1/15 | yes | 200 (full, energy_lacunae\*) | CAPACITOR x15 |
| `gun_minigun_dual` | DEBUG (dual belt, twin `GunConfig`) | 50,000 | 6 | 1/15 | yes | belt+belt | R762_SP x50 |
| `gun_mas36` | LEGENDARY | 5,000 | 30 | 25/25 | no | 7 (full) | R762_AP x14 |

Ammo: `r762_sp`, `r762_fmj` (0.8x/+5/0.1), `r762_jhp` (1.5x/1.5x hs/-0.25), `r762_ap`
(`SMALL_STEEL`, 1.5x, penetrates, +12.5 threshold, 0.15 AP%), `r762_du` (`SMALL_STEEL`, 2.5x,
penetrates, +15 threshold, 0.25 AP%), `r762_he` (`SMALL_STEEL`, 2x, 3x wear, explodes on impact —
`LAMBDA_TINY_EXPLODE`). Lacunae energy rounds (`energy_lacunae`, `_overcharge`, `_ir`) are beam-type,
consume `ingot_polymer` (2 per charge, ×40 reload count) as "capacitor" ammo, not a cased round.

**`XFactory50` (.50 BMG, `LARGE`/`LARGE_STEEL`, anti-materiel rifle + M2 family):**

| Item | Quality | Dura | Dmg | Delay | Mag | Default ammo |
|---|---|---|---|---|---|---|
| `gun_amat` | A_SIDE | 350 | 30 | 25/25 | 7 (full) | BMG50_SP x7 |
| `gun_amat_subtlety` | LEGENDARY | 1,000 | 50 | 25/25 | 7 (adds secret `bmg50_equestrian`) | BMG50_JHP x7 |
| `gun_amat_penance` | LEGENDARY (thermal sights) | 5,000 | 45 | 25/25 | 7 (adds secret `bmg50_black`) | BMG50_JHP x7 |
| `gun_m2` | A_SIDE | 3,000 | 7.5 | 2/10, auto | belt | BMG50_FMJ x25 |

Ammo: `bmg50_sp`, `bmg50_fmj` (0.8x/+7/0.1), `bmg50_jhp` (1.5x/1.5x hs/-0.25), `bmg50_ap`
(`LARGE_STEEL`, 1.5x, penetrates, +17.5 threshold, 0.15 AP%), `bmg50_du` (`LARGE_STEEL`, 2.5x,
penetrates, +21 threshold, 0.25 AP%), `bmg50_he` (`LARGE_STEEL`, explodes, 3x wear, 1.75x dmg,
penetrates), `bmg50_sm` (`LARGE_STEEL`, 6-count casing, 10x wear, 2.5x dmg, +30 threshold, 0.35
AP%), plus secret `bmg50_black` (spectral, penetrates, 1.5x dmg, 3x headshot, +30/0.35 AP%) and
`bmg50_equestrian` (0-dmg joke round, triggers `LAMBDA_BUILDING` — spawns a structure).

**`XFactory12ga` (12ga, `SHOTSHELL`/`BUCKSHOT`/`BUCKSHOT_ADVANCED`, the largest single ammo family —
also builds a parallel "shredder" (laser-beam pellet-burst) submunition variant of every round for
`gun_autoshotgun_shredder`):**

| Item | Quality | Dura | Dmg | Rounds/cyc | Delay | Mag | Default ammo |
|---|---|---|---|---|---|---|---|
| `gun_maresleg` | A_SIDE | 600 | 16 | 1 | 20 | 6 (single reload, lever action) | G12 x12 |
| `gun_maresleg_akimbo` | B_SIDE (dual) | 600 | 16 | 1 | 20 | 6+6 | G12 x24 |
| `gun_maresleg_broken` | LEGENDARY (0 dura, no-wear fire) | 0 | 32 | 1 | 20 | 6 (single, adds secret `g12_equestrian_tkr`) | G12_MAGNUM x24 |
| `gun_liberator` | A_SIDE | 200 | 16 | **4** | 20 | 4 (single, sequential) | G12 x12 |
| `gun_spas12` | A_SIDE | 600 | 32 | 1 | 20 | 8 (single, sequential, reload-changes-type) | G12 x16 |
| `gun_autoshotgun` | A_SIDE | 2,000 | 48 | 1 | 10, auto | 20 (full) | G12 x20 |
| `gun_autoshotgun_shredder` | B_SIDE | 2,000 | 50 | 1 | 10, auto | belt (shredder beam ammo) | G12 x20 |
| `gun_autoshotgun_sexy` | LEGENDARY | 5,000 | 64 | 1 | 4, auto | 100 (full, adds secret `g12_equestrian_bj`) | G12_MAGNUM x50 |
| `gun_autoshotgun_heretic` | DEBUG | (0/uncapped) | 100 | 1 | 3, auto, no-wear | 250 (full) | G10 x50 |

Ammo (12ga): `g12_bp`/`g12_bp_magnum`/`g12_bp_slug` (`SHOTSHELL` casing, black powder, 8/4/1
pellets), `g12` (`BUCKSHOT` casing, 8 pellets, +2 threshold), `g12_slug` (`BUCKSHOT`, single
projectile, 1.5x headshot, +4 threshold, 0.15 AP%), `g12_flechette` (`BUCKSHOT`, 8 pellets, +3
threshold, 0.2 AP%), `g12_magnum` (`BUCKSHOT_ADVANCED`, 4 pellets 2x dmg each, +4 threshold),
`g12_explosive` (`BUCKSHOT_ADVANCED`, 2.5x, explodes on impact), `g12_phosphorus`
(`BUCKSHOT_ADVANCED`, 8 pellets, sets `HbmLivingCapability` phosphorus stack to 300 on hit — already
portable per `HbmLivingAttachment`'s existing `phosphorus` field), plus secret 0-dmg
`g12_equestrian_bj`/`_tkr` joke rounds (spawn `EntityDuchessGambit` boat). Every non-black-powder
12ga round also has a `_shredder`/`_sub` derivative pair (laser-beam that on impact spawns a burst of
the matching submunition bullets) built by `makeShredderConfig`/`makeShredderSubmunition` for
`gun_autoshotgun_shredder`'s belt.

**`XFactory10ga` (10ga, `BUCKSHOT_ADVANCED` casing exclusively, double-barrel family):**

| Item | Quality | Dura | Dmg | Rounds/cyc | Delay | Mag | Default ammo |
|---|---|---|---|---|---|---|---|
| `gun_double_barrel` | SPECIAL | 1,000 | 30 | 2 | 10 | 2 (full, reload-on-empty) | G10 x6 |
| `gun_double_barrel_sacred_dragon` | B_SIDE | 6,000 | 45 | 2 | 10 | 2 | G10_DU x6 |
| `gun_autoshotgun_heretic` | (listed above, 10ga default ammo) | | | | | | |

Ammo: `g10` (10-pellet, +5 threshold), `g10_shrapnel` (10-pellet, ricochet angle 90°/count 15 —
designed to bounce), `g10_du` (10-pellet, 0.25x dmg-share, penetrates, +10 threshold, 0.2 AP%),
`g10_slug` (single projectile, penetrates, +10 threshold, 0.1 AP%), `g10_explosive` (10-pellet, 3x
wear, explodes — `LAMBDA_TINY_EXPLODE`, shared with `r762_he`).

**`XFactory40mm` (40mm grenade/flare launcher, `LARGE` casing):**

| Item | Quality | Dura | Dmg | Reload | Mag | Default ammo |
|---|---|---|---|---|---|---|
| `gun_flaregun` | A_SIDE | 100 | 15 | 28 | 1 (single) | G26_FLARE x3 |
| `gun_congolake` | A_SIDE | 400 | 20 | 16/16/16 (sequential, reload-changes-type) | 4 (single) | G40_HE x8 |
| `gun_mk108` | A_SIDE | 5,000 | 25 | 135, auto | 30 (full) | G40_HE x50 |

Ammo: `g26_flare` (illumination, no damage; `_supply`/`_weapon` variants spawn C130 airdrop
crates on impact — `LAMBDA_SPAWN_C130_*`), `g40_he`/`g40_heat`/`g40_demo`/`g40_inc`/`g40_phosphorus`
(all clones of a shared `g40_base`, each swapping `.onImpact` for a different explosion/effect
variant and 0.5-0.75x damage share).

**`XFactory75Bolt` (7.5mm bolt-action, no casing item registered — black-powder-style, single gun):**

| Item | Quality | Dura | Dmg | Delay | Auto | Mag | Default ammo |
|---|---|---|---|---|---|---|---|
| `gun_bolter` | SPECIAL | 3,000 | 15 | 2, auto | yes | 30 (full) | B75 x15 |

Ammo: `b75`, `b75_inc` (0.8x, +0.1 AP%, incendiary), `b75_exp` (1.5x, -0.25 AP%).

**`XFactoryAccelerator` (particle-accelerator energy weapons; no shared caliber — each gun is its
own bespoke beam ammo):**

| Item | Quality | Dura | Dmg | Delay | Mag | Ammo |
|---|---|---|---|---|---|---|
| `gun_tau` | A_SIDE | 6,400 | 25 | 4, auto | belt | `tau_uranium` (SUBATOMIC beam, penetrates, no falloff) |
| `gun_coilgun` | SPECIAL | 400 | 35 | 5 | 1 (single) | `coil_tungsten`/`coil_ferrouranium` (spectral, penetrates) |
| `gun_n_i_4_n_i` | SPECIAL (`ItemGunNI4NI`, 0 dura) | 0 | 35 | 10 | infinite (`ni4ni_arc`) | bespoke, no consumable ammo item |

**`XFactoryEnergy` (Tesla/laser sidearms; energy configs consume `ingot_polymer` as "capacitor" ammo
via `CAPACITOR`/`CAPACITOR_OVERCHARGE`/`CAPACITOR_IR` `EnumAmmo` constants):**

| Item | Quality | Dura | Dmg | Rounds | Delay | Mag | Default ammo |
|---|---|---|---|---|---|---|---|
| `gun_tesla_cannon` | A_SIDE | 2,000 | 35 | 1 | 20 | belt | CAPACITOR x15 |
| `gun_laser_pistol` | A_SIDE | 500 | 25 | 1 | 5 | 30 (full) | CAPACITOR x15 |
| `gun_laser_pistol_pew_pew` | B_SIDE | 500 | 30 | **5** | 10 | 10 (full) | CAPACITOR_OVERCHARGE x10 |
| `gun_laser_pistol_morning_glory` | LEGENDARY | 1,500 | 20 | 1 | 7 | 20 (full, `energy_emerald*`) | CAPACITOR_OVERCHARGE x20 |
| `gun_lasrifle` | A_SIDE | 2,000 | 50 | 1 | 8 | 24 (full, sequential) | CAPACITOR x24 |
| `gun_fatman` | A_SIDE | 300 | 100 | 1 | 10 | 1 (single, reload-changes-type) | NUKE_STANDARD x1 (`setDefaultAmmoExpensive`) |

Energy ammo: `energy_tesla`/`_overcharge`/`_ir` (ELECTRIC class beam), `energy_las`/`_overcharge`/
`_ir` (LASER/FIRE class beam), each consuming 4x `ingot_polymer`. `gun_fatman`'s mini-nuke rounds
(`nuke_standard`/`_demo`/`_high`/`_tots`/`_hive`/`_balefire`) are `NUKE_*` `EnumAmmo` constants with
no cased-ammo item at all — each `.setOnImpact` triggers a distinct explosion tier; `NUKE_STANDARD`
is flagged `setDefaultAmmoExpensive` (the item container / makeshift-container system halves normal
ammo grants but *never* grants expensive default ammo, per `ItemAmmoContainer`'s `isDefaultExpensive`
check read above).

**`XFactoryFolly` (SECRET/joke tier, subatomic beam + chunk-loading fake-nuke round):**

| Item | Quality | Dura | Dmg | Delay | Mag | Ammo |
|---|---|---|---|---|---|---|
| `gun_folly` | SECRET (0 dura, no dry-fire, crosshair NONE) | 0 | 1,000 | 26 | 1 (single) | `folly_sm` (SUBATOMIC beam, spectral, penetrates), `folly_nuke` (chunk-loading projectile type — the only gun in the roster using `BulletConfig.ProjectileType.BULLET_CHUNKLOADING`) |

**`XFactory35800` (SECRET tier, ultra-high-damage beam sidearm):**

| Item | Quality | Dura | Dmg | Delay/Dry | Mag | Ammo |
|---|---|---|---|---|---|---|
| `gun_aberrator` | SECRET | 2,000 | 100 | 13/21 | 5 (full) | `p35800` (SUBATOMIC-tier beam, 0.5 AP threshold negation, +50 threshold negation) |
| `gun_aberrator_eott` | SECRET (dual `GunConfig`, akimbo) | 2,000 | 100 | 13/21 | 5+5 | same |

**`XFactoryRocket` (rocket launchers, `LARGE` casing on the base 40mm-style shell reused for RPG rounds):**

| Item | Quality | Dura | Dmg | Delay | Mag | Default ammo |
|---|---|---|---|---|---|---|
| `gun_panzerschreck` | A_SIDE | 300 | 25 | 5 | 1 (single) | ROCKET_HE x3 |
| `gun_stinger` | A_SIDE (`ItemGunStinger` — lock-on) | 300 | 35 | 5 | 1 (single) | ROCKET_HEAT x3 |
| `gun_quadro` | A_SIDE | 400 | 40 | 10 | 4 (full) | ROCKET_HE x4 |
| `gun_missile_launcher` | A_SIDE | 500 | 50 | 5 | 1 (single) | ROCKET_HEAT x5 |

**`XFactoryTool` (utility weapons — fire extinguisher, incendiary charge-thrower; both `MagazineFluid`/mixed):**

| Item | Quality | Dura | Dmg | Delay | Auto | Mag | Ammo |
|---|---|---|---|---|---|---|---|
| `gun_fireext` | UTILITY | 5,000 | 0 (utility, no damage) | 1, auto | yes | 300 (full) | `fext_water`/`_foam`/`_sand` (all `ammo_fireext` meta variants) |
| `gun_charge_thrower` | UTILITY (`ItemGunChargeThrower`) | 3,000 | 10 | 4/10, auto | yes | 1 (full) | `ct_hook`/`ct_mortar`/`ct_mortar_charge` |

**`XFactoryFlamer` (flamethrowers, fluid-style ammo via `flame_*` diesel/gas/napalm/balefire tiers):**

| Item | Quality | Dura | Dmg | Delay | Mag | Default ammo |
|---|---|---|---|---|---|---|
| `gun_flamer` | A_SIDE | 20,000 | 1, auto | 1 | 300 (full) | FLAME_DIESEL x1 |
| `gun_flamer_topaz` | B_SIDE | 20,000 | 1.5, auto | 1 | 500 (full) | FLAME_DIESEL x1 |
| `gun_flamer_daybreaker` | LEGENDARY | 20,000 | 25, auto | 10 | 50 (full) | FLAME_DIESEL x1 |
| `gun_chemthrower` | A_SIDE (`ItemGunChemthrower`) | 90,000 | fluid-based, auto | 1 | 3,000 (`MagazineFluid`) | any accepted chemical fluid, not a discrete ammo item |

**`XFactoryDrill` (mining "weapon", fluid-fuelled):**

| Item | Quality | Dura | Dmg | Delay | Auto | Mag | Fuel |
|---|---|---|---|---|---|---|---|
| `gun_drill` | UTILITY (`ItemGunDrill`) | 3,000 | 10 | 20 | yes | 4,000 (`MagazineFluid`) | Gasoline/GasolineLeaded/Coalgas/CoalgasLeaded |

**`XFactoryPA` (powered-armor weapon slots — melee/ranged delegate to an `IPAMelee`/`IPARanged`
component provided by whatever chestplate is worn; no intrinsic `Receiver`/damage/ammo of their own,
these two items are pure dispatch shells):** `gun_pa_melee`, `gun_pa_ranged` (both UTILITY).

**`GunFactory` itself:** `gun_debug` (DEBUG quality, `ammo_debug` — a single `ItemBakedBase`
non-enum item, not part of `ammo_standard`).

**`XFactoryTurret`** does *not* define a handheld gun — it registers `dgk_normal`/`shell_normal`/
`shell_explosive`/`shell_ap`/`shell_du`/`shell_w9` `BulletConfig`s consumed by a 240mm coastal-
turret block entity (`ammo_dgk`, `ammo_shell` items), tied to `TileEntityTurretFritz`. This is
turret/multiblock content, not gun content — flag for whichever Phase 3 sub-area covers turrets, not
this report.

### The full `EnumAmmo` roster (101 constants — `ammo_standard`, one `ItemEnumMulti`)

```
STONE, STONE_AP, STONE_IRON, STONE_SHOT,
M357_BP, M357_SP, M357_FMJ, M357_JHP, M357_AP, M357_EXPRESS,
M44_BP, M44_SP, M44_FMJ, M44_JHP, M44_AP, M44_EXPRESS,
P22_SP, P22_FMJ, P22_JHP, P22_AP,
P9_SP, P9_FMJ, P9_JHP, P9_AP,
R556_SP, R556_FMJ, R556_JHP, R556_AP,
R762_SP, R762_FMJ, R762_JHP, R762_AP, R762_DU,
BMG50_SP, BMG50_FMJ, BMG50_JHP, BMG50_AP, BMG50_DU,
B75, B75_INC, B75_EXP,
G12_BP, G12_BP_MAGNUM, G12_BP_SLUG, G12, G12_SLUG, G12_FLECHETTE, G12_MAGNUM, G12_EXPLOSIVE, G12_PHOSPHORUS,
G26_FLARE, G26_FLARE_SUPPLY, G26_FLARE_WEAPON,
G40_HE, G40_HEAT, G40_DEMO, G40_INC, G40_PHOSPHORUS,
ROCKET_HE, ROCKET_HEAT, ROCKET_DEMO, ROCKET_INC, ROCKET_PHOSPHORUS,
FLAME_DIESEL, FLAME_GAS, FLAME_NAPALM, FLAME_BALEFIRE,
CAPACITOR, CAPACITOR_OVERCHARGE, CAPACITOR_IR,
TAU_URANIUM,
COIL_TUNGSTEN, COIL_FERROURANIUM,
NUKE_STANDARD, NUKE_DEMO, NUKE_HIGH, NUKE_TOTS, NUKE_HIVE,
G10, G10_SHRAPNEL, G10_DU, G10_SLUG,
R762_HE, BMG50_HE, G10_EXPLOSIVE,
P45_SP, P45_FMJ, P45_JHP, P45_AP, P45_DU,
CT_HOOK, CT_MORTAR, CT_MORTAR_CHARGE,
NUKE_BALEFIRE, BMG50_SM
```
(Ordinal order matters: `setDefaultAmmo(EnumAmmo, count)` builds `new ItemStack(ammo_standard,
count, ammo.ordinal())`, i.e. metadata = ordinal. **Never reorder this enum** when porting — treat it
as a fixed id table, append-only, exactly as CE's own "ONLY ADD NEW ENTRIES AT THE BOTTOM" comment
in `GunFactory.java` demands. In the 1.21.1 port this becomes either the Data Component's stored
integer/string id or — better — 101 distinct `DeferredItem`s if the port is flattening
metadata-multi items elsewhere (confirm against whatever convention Phase 1's `ItemEnumMulti`
successor already settled on for other `ItemEnumMulti` families, e.g. `EnumHoloImage`,
`ScrapType` per `items_special.md` finding 1).

`EnumAmmoSecret` (8 constants, `ammo_secret`, hidden creative tab = `null`): `FOLLY_SM, FOLLY_NUKE,
M44_EQUESTRIAN, G12_EQUESTRIAN, BMG50_EQUESTRIAN, P35_800, BMG50_BLACK, P35_800_BL`.

### Casing types (`ItemEnums.EnumCasingType`, already ported, 7 values) — which ammo families use which

| Casing | Used by |
|---|---|
| `SMALL` | 9mm, .357, .44, .45, .22LR, 5.56mm (all base/FMJ/JHP tiers) |
| `SMALL_STEEL` | AP/DU/HE tiers of every `SMALL`-casing caliber above, plus 7.62mm's full family |
| `LARGE` | .50 BMG base tiers, 40mm grenades, 40mm flares |
| `LARGE_STEEL` | .50 BMG AP/DU/HE/SM tiers |
| `SHOTSHELL` | 12ga black-powder-loaded shells only (`g12_bp*`) |
| `BUCKSHOT` | 12ga standard buckshot/slug/flechette |
| `BUCKSHOT_ADVANCED` | 12ga magnum/explosive/phosphorus tiers, **all** 10ga rounds |

Some ammo has **no** `EnumCasingType` at all and is not craftable via the Ammo Press: beam/energy
rounds (consume raw material stacks like `ingot_polymer`, `plate_lead`), fluid-based rounds
(flamethrower/drill/chemthrower/fire-extinguisher — consume tank fluid, not an item), and several
one-off ammo types (`ammo_dgk`, artillery/HIMARS shells, secret/joke rounds). Each `BulletConfig`'s
`.setCasing(...)` call (or its absence) is the single source of truth per round — there is no
blanket rule beyond the table above.

### Legacy system — port as a compatibility shim, not as active content (see below)

`gun_b92` (`items/special/weapon/GunB92`) and `gun_b93` (`items/weapon/GunB93`) are legendary
charge-weapons that bypass `GunConfiguration`/bullet-configs entirely: hold right-click (sneaking)
to accumulate up to 10 charges (`onUpdate` ticks an animation counter, +1 charge every 15 ticks of
the 30-tick cycle), release to fire `getPower(stack)` copies of `EntityExplosiveBeam`
(`gun_b92`)/one `EntityModBeam` (`gun_b93`, `mode = getPower - 1`) that explode on impact
(`dmgMin`/`dmgMax` = 16/28, `+3.5` flat attack-damage attribute). Reaching charge 11 instead
self-detonates the wielder with `EntityNukeExplosionMK3` (`destructionRange = 50`) plus a
`EntityCloudFleijaRainbow`. `gun_b92_ammo` (`GunB92Cell`) is a passive inventory item that siphons
charge from a `gun_b92` in the same inventory (1 charge/tick, capped at 25) purely so players can
build a bigger detonation — `getFullCell()` static factory pre-charges one to 25. These three items
are self-contained (no `BulletConfigSyncingUtil` dependency) and **are** portable as-is once
`EntityExplosiveBeam`/`EntityModBeam`/`EntityNukeExplosionMK3`/`EntityCloudFleijaRainbow` exist
(entity-side, a different Phase 3 sub-area's concern) — their item-side logic is plain NBT counters
(`animation`, `energy`) that map to two int Data Components each.

## Deferred scope

**Legacy `ItemGunBase`/`GunConfiguration`/`BulletConfigSyncingUtil` gun-config framework itself**
(needed only by `gun_supershotgun`/`gun_vortex`) — recommend **not porting the framework at all**.
Both items are confirmed non-functional in current CE (see Headline finding); the honest port is
either (a) register the two items as decorative/holdable-only shells matching upstream's actual
broken behavior (meathook grapple on `gun_supershotgun` still needs `ItemGunShotty`'s
`rayTraceEntitiesInCone`/hooked-entity swing logic, which *is* independent and does work), or (b)
skip both and note the gap in the parity audit. Either way, do not build out
`GunConfiguration`/`BulletConfigSyncingUtil` as a second parallel gun engine next to Sedna — that
would be reproducing ~285 lines of dead-end legacy plumbing (confirmed: `BulletConfigSyncingUtil`'s
`configSet` map has ~120 populated entries total, almost all consumed only by NPC/turret bullet
factories in `com.hbm.handler.guncfg.GunNPCFactory`/`*RocketFactory`, which are their own
Phase-3/Phase-4 concern, not gun content).

**The Sedna gun-framework's own machinery** (`ItemGunBaseNT`'s `GunState` state machine,
`XWeaponModManager` attachment-mod evaluation, `GunStateDecider`/`Lego` prefab lambdas, animation
bus system `BusAnimationSedna`, HUD components) — explicitly out of scope per this report's brief;
covered by the parallel gun-framework-core research task. This report only consumed those types'
*public field/method shapes* (`GunConfig.dura/draw/rec`, `Receiver.dmg/delay/mag/rounds`,
`BulletConfig.setItem/setCasing/setDamage/...`) to read off concrete numbers correctly.

**~30 `WeaponMod*` attachment items** (`weapon_mod_generic`, `weapon_mod_special`,
`weapon_mod_caliber`, `weapon_mod_test` — all `ItemEnumMulti`, registered in `GunFactory.init()`
right after the gun roster) — scopes, silencers, chokes, sawed-off/bayonet mods, speedloaders,
caliber-swap kits, and debug fire-rate/damage-multiplier test mods, each implementing `IWeaponMod`
and read/evaluated through `XWeaponModManager.eval(...)` (the same dispatcher every `GunConfig`/
`Receiver` getter routes through). These directly determine a gun's *effective* stats at runtime
(e.g. `ID_SAWED_OFF` on `gun_maresleg` shortens it and changes its name via `LAMBDA_NAME_MARESLEG`)
but are a distinct item family with their own NBT-attachment-slot mechanic — recommend a dedicated
follow-up pass once the base 70-gun roster and `XWeaponModManager`'s eval mechanism (framework-core
report) both exist. Not counted in this report's 70-gun figure.

**Artillery/rocket/turret ammo not fired from a handheld gun**: `ammo_arty` (`ItemAmmoArty`, 12
shell types — HE/nuke/mini-nuke/phosphorus/chemical-warfare/cargo, each with bespoke
`ExplosionVNT`/`EntityNukeExplosionMK5` impact behavior, fired from an artillery block entity, not
covered here), `ammo_himars` (`ItemAmmoHIMARS`, HIMARS rocket-truck ammo, same shape), `ammo_dgk` +
`ammo_shell` (240mm coastal-defense turret shells, `XFactoryTurret`, see above), `ammo_container`
(`ItemAmmoContainer` — a "resupply crate" utility item that grants a random held gun's default ammo
stack, not ammo itself; its logic is fully read above and is Phase-3-safe today, just noting it's
not "ammo" in the caliber sense). All of these belong with whichever Phase 3/4 sub-area owns
artillery/turret block entities and vehicles, not the handheld-gun report.

**Grenades, missiles, melee, and fluid-thrower weapons in the same `items/weapon` package** — read
in full per this report's brief, but each is its own content family deserving a dedicated research
pass, not a subsection here:
- `ItemGenericGrenade` + subclasses `ItemGrenadeDynamite` (vanilla TNT-style, 3F explosion),
  `ItemGrenadeFishing` (spawns underwater loot via `LootTableList`) — throwable-grenade family,
  fuse-based (`fuse` field, `-1` = impact-detonate).
- `ItemMissile` (base class: `PartType`/`PartSize`/`Rarity`/`health`/`mass` fields — missile *part*
  items for a modular missile-building system) and `ItemCustomMissile`/`ItemMissileStandard`
  (`buildMissile(chip, warhead, fuselage, stability, thruster)` — NBT-composed custom missiles,
  `MissileFormFactor`/`MissileTier`/`MissileFuel` enums) — a large modular-missile crafting system,
  entity-side in `com.hbm.entity.missile`, explicitly its own Phase 3 sub-area (PORT_SPEC calls out
  "weapons & destruction" broadly; missiles are destruction-tier content on the scale of the RBMK
  survey, not an ammo-list item).
- `ItemCrucible` (melee weapon with a charge-up "blender" attack, `SharedMonsterAttributes`
  modifiers, its own `HbmAnimations.BlenderAnimation`), `ItemSwordCutter` (extends
  `items/tool/ItemSwordAbility`, already named in Phase 1's `items_tool.md` bucket (b) as Phase 3
  melee) — melee weapon family, out of scope here.
- `ItemDisperser` (extends `items/machine/ItemFluidTank`, throws `EntityDisperserCanister` — a
  fluid-effect grenade, e.g. chemical/gas weapons) — fluid-weapon family, couples to Phase 0's
  `FluidType`/`Fluids` (already ported) but is its own item, not ammo.
- `GrenadeDispenserRegistry` — despite the name, only registers a vanilla-dispenser behavior for
  `powder_fertilizer` (`ItemFertilizer`); no grenade dispensing logic exists in this class today
  (`registerDispenserBehaviors()` is an empty stub). Misfiled/dead, trivial to port (or drop) whenever
  `ItemFertilizer` is handled.
- `WeaponizedCell` — not a gun or ammo; a "rigged star" trap item that self-detonates
  (`EntityNukeExplosionMK3`) if left on the ground too long (`BombConfig.riggedStarTicks`). Bomb/trap
  content, belongs with whichever Phase 3 sub-area covers `com.hbm.items.special` bomb items
  (`ItemUnstable` etc., already named Phase 3 by `DIGEST_REMAINDER.md`).

**Ammo Press / casing crafting loop** (which machine consumes `EnumCasingType` casing items + powder
to produce `ammo_standard` metadata stacks) is Phase 2 recipe-system territory
(`inventory/recipes/AmmoPressRecipes.java`) — not re-derived here; Phase 3 gun content only needs to
know the casing type + count each `BulletConfig` declares (captured above) so that recipe system has
its inputs when it's implemented/ported.

## Key design/API decisions

- **Ammo is metadata-multi, not one item per caliber.** Port `ammo_standard`/`ammo_secret` following
  whatever convention this port has already settled on for other large `ItemEnumMulti` families
  (Phase 1's `items_special.md` finding 1 lists `ItemHolotapeImage` (18), `ItemPlasticScrap` (21) as
  precedent — confirm with whoever owns that convention before choosing between "101 distinct
  `DeferredItem`s" and "one item + a Data Component holding the ammo-type id" for `ammo_standard`).
  Either way, **the `EnumAmmo` ordinal order must be preserved exactly** as a fixed id table (CE's
  own source comment demands append-only) if any saved-game/NBT compatibility with existing CE
  worlds matters to this port; if it doesn't, this is the one place a flattening pass can freely
  rename/reorder as long as internal `BulletConfig` references and default-ammo grants are updated
  together.
- **Damage sources need new `DamageType` entries.** `com.hbm.damage.ModDamageTypes` (Phase 0, read
  in full) currently defines entries mirroring CE's old `ModDamageSource` singleton fields — it has
  **no** entries for Sedna's `DamageResistanceHandler.DamageClass` values (`PHYSICAL`, `FIRE`,
  `EXPLOSIVE`, `ELECTRIC`, `LASER`, `SUBATOMIC`), which `BulletConfig.getDamage()` turns into a named
  `DamageSourceSednaWithAttacker`/`NoAttacker` via `dmgClass.name()` (lowercased, used as the
  `DamageSource`'s translation key). `PHYSICAL`→`setProjectile()`, `FIRE`→`setFireDamage()`,
  `EXPLOSIVE`→`setExplosion()` map onto existing vanilla/`DamageType` flag equivalents, but
  `ELECTRIC`/`LASER`/`SUBATOMIC` have no flags set at all in CE (a `switch` with no default case for
  those three) — meaning three brand-new `DamageType` datapack entries need names/exhaustion/message
  keys decided (e.g. `hbm:electric`, `hbm:laser`, `hbm:subatomic`), following the exact pattern
  `ModDamageTypes.java`'s own file header lays out (`key("...")`, flags via
  `ModDamageTypeTags`/`ModDamageTypeTagsProvider`, not inline). This is new work, not already covered
  by Phase 0's existing 40-ish entries.
- **NBT -> Data Components.** Every stat CE stores as flat `ItemStack` NBT ints on the *gun* item
  (`ItemGunBase`: `isReloading`, `isMouseDown`, `isAltDown`, `dlay`, `wear`, `cycle`, `reload`,
  `magazine`, `magazineType`; `ItemGunBaseNT`/`Receiver`/magazine classes use the same style keyed
  per-receiver-index, e.g. `MagazineFullReload`'s `magcount0`/`magtype0`/`magprev0`/`magafter0`) is a
  small closed set of ints/booleans per gun instance — a natural single custom Data Component (one
  record holding mag count(s), mag type(s), wear, cooldown/reload-cycle timers, mouse-button-held
  flags) rather than one component per field, mirroring how Phase 1/2 already collapsed other
  multi-field NBT blobs. The *legendary/secret* ammo flags and per-gun `defaultAmmo` ItemStack
  (`ItemGunBaseNT.defaultAmmo`, `isDefaultExpensive`) are item-definition data, not per-stack state —
  those belong on the registered `Item`/its properties, not a Data Component.
- **Networking.** Confirmed via `com.hbm.packet.HbmNetwork` (Phase 2, one real payload registered:
  `BufPacket`) that every new packet is one `CustomPacketPayload` record + `StreamCodec` +
  `registrar.playToClient/playToServer(...)` line appended to `HbmNetwork.registerPackets`. Gun
  content needs at minimum: a button/click-state packet (client tells server LMB/RMB/reload
  pressed/released — CE's `GunButtonPacket`), a fire-FX packet (muzzle flash/sound broadcast to
  nearby players — CE's `GunFXPacket`), and an animation-trigger packet (CE's
  `GunAnimationPacket`/`GunAnimationPacketSedna`, `type.ordinal()` + receiver index). All three are
  small, fixed-shape payloads and fit the `HbmNetwork` pattern directly; exact field layout is the
  framework-core report's call since it owns `ItemGunBaseNT`'s click/state handling.
- **GUI/Menu coupling is minimal for guns themselves** — no gun or ammo item opens a
  `MenuBase`/`GuiInfoContainer` screen (confirmed: none of the 70 Sedna guns or 4 legacy guns
  implement any GUI-provider interface). The one GUI-adjacent item is `ItemAmmoBag`
  (`com.hbm.items.tool`, out of this package but load-bearing for `MagazineBelt`/
  `MagazineSingleTypeBase`'s overflow-ammo lookup, `IMagazine.handleAmmoBag`) — Phase 1's
  `items_tool.md`/`STATUS.md` already shipped it as a registered shell with no menu interaction
  pending the Menu/Screen framework; that framework now exists (Phase 2), so completing
  `ItemAmmoBag`'s container GUI is a reasonable small prerequisite to pull into Phase 3 alongside
  belt-fed guns (`gun_minigun`, `gun_m2`, `gun_tesla_cannon`, `gun_tau`, `gun_autoshotgun_shredder`,
  `gun_minigun_dual`) since those are the only guns that ever call into `ItemAmmoBag`'s inventory.
- **Explosion performance is a real Phase 3 concern for several ammo types**, not just the
  bomb/reactor packages: `g12_explosive`/`g10_explosive`/`r762_he`/`bmg50_he`/every `g40_*` grenade
  round/every `ROCKET_*` warhead calls into the same `Lego.standardExplode`/`ExplosionVNT`
  block-removal machinery PORT_SPEC flags for "batched LevelChunk section writes + deferred
  lighting." A naive per-block `Level#setBlock` loop triggered by, say, `gun_mk108`'s 25-round
  belt-fed 40mm auto-grenade-launcher firing `G40_HE` at its 4-tick cyclic rate would call the
  explosion path far more often and in tighter succession than any single reactor/bomb detonation —
  this is exactly the workload the batching requirement exists for, and gun-fired explosive rounds
  should be tested against it, not assumed safe because "it's just a small explosion."

## Open questions / risks

- **`XFactory556mm`'s `r556_inc_*` incendiary round set** (used exclusively by `gun_g3_zebra`) was
  not captured by this report's grep pass (its `BulletConfig` field declarations sit just above the
  matched lines) — read `XFactory556mm.java` lines ~20-45 directly before implementing
  `gun_g3_zebra` to confirm those four configs' exact damage/threshold/on-impact values; every other
  gun's ammo set in this report was captured in full.
- **`gun_liberator`'s ammo family is genuinely 12ga (`all[]`), not .45** despite the "Liberator"
  pistol name strongly implying the historical FP-45 Liberator (.45 ACP single-shot pistol) — this
  reads as intentional CE naming-vs-mechanics divergence (it's a 4-round break-action shotgun in
  CE's implementation) rather than a research-pass error, but confirm against any CE changelog/wiki
  if exact fidelity to the "real" Liberator matters, since `XFactory45`'s `p45_*` configs are
  registered but never consumed by any gun in the entire source tree searched.
- **Legacy `gun_supershotgun`/`gun_vortex` non-functionality** — confirmed by static analysis
  (`BulletConfigSyncingUtil.configSet` never receives `G12_NORMAL`/`R556_STAR` etc.) but not
  confirmed by actually running CE, since this sandbox cannot run Gradle. If this port's parity
  target is "bug-for-bug identical to a specific CE build," this needs a runtime check against real
  CE before deciding whether to reproduce the break or quietly fix it; the recommendation above
  (register as decorative/keep meathook-only) assumes reproducing it is acceptable, which is a
  product decision, not a technical one.
- **`WeaponMod*` attachment items' interaction with the 70-gun roster** is only sketched here
  (Deferred scope) — `XWeaponModManager.eval(...)` is the single dispatch point every `GunConfig`/
  `Receiver` getter in this report routes every stat through in real CE (this report read the raw
  `_DNA` field defaults, which is correct for "what does the gun do with no mods attached" but is
  *not* the full runtime picture). Anyone implementing off this report alone will get correct
  unmodified stats but will need the framework-core + weapon-mod follow-up before mod-affected stats
  (silencer sound/spread, scope zoom, sawed-off length/name change, caliber-swap kits) are complete.
- **`gun_pa_melee`/`gun_pa_ranged`** have no intrinsic stats at all — their entire behavior is
  delegated to whatever `IPAMelee`/`IPARanged` component the worn power-armor chestplate provides
  (`IPAWeaponsProvider.getMeleeComponentCommon`/`getRangedComponentClient`). This report cannot give
  them damage/fire-rate/ammo numbers because CE itself doesn't hardcode any — that data lives in the
  (unread, out-of-package) `items/armor` power-armor implementation. Flag for whoever's Phase 3 area
  covers power armor.
- **Ammo Press recipe inputs weren't independently re-verified against `AmmoPressRecipes.java`** —
  this report captured casing type + count from each `BulletConfig.setCasing(...)` call (the
  consumer side, i.e. what a gun's magazine expects), which should match the Ammo Press's *output*
  recipe for that ammo id, but the recipe file itself (crafting inputs: powder type, casing source)
  was not read. Low risk (the two are populated from the same `EnumAmmo` id space) but worth a
  cross-check when the recipe-porting pass reaches ammo.
