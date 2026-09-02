# CE gun animation leftover

Sedna bus engine, 12 CE JSON clips, and 68 weapon OBJs were already in-tree.
This wave wired CE `ItemRender*` + `LAMBDA_*_ANIMS` onto registered guns.
No invented keyframes.

## Gained CE anims (renderer + lambda)

Already wired before this wave: `gun_spas12`, `gun_uzi`, `gun_am180`.

Newly bound this wave (CE renderer + CE factory lambda):

- pistols: `gun_uzi_akimbo`, `gun_pepperbox`, `gun_light_revolver`(+atlas/dani), `gun_henry`(+lincoln), `gun_heavy_revolver`(+lilmac/protege), `gun_hangman`, `gun_greasegun`, `gun_lag`, `gun_star_f`(+akimbo)
- shotguns: `gun_maresleg`(+akimbo/broken), `gun_liberator`, `gun_autoshotgun`(+shredder/sexy/heretic), `gun_double_barrel`(+sacred_dragon)
- rifles: `gun_g3`(+zebra), `gun_stg77`, `gun_carbine`, `gun_minigun`(+lacunae/dual), `gun_mas36`, `gun_amat`(+subtlety/penance), `gun_m2`
- launchers: `gun_flaregun`, `gun_congolake`, `gun_mk108`, `gun_bolter`
- heavy: `gun_panzerschreck`, `gun_stinger`, `gun_quadro`, `gun_missile_launcher`, `gun_charge_thrower`, `gun_flamer`(+topaz/daybreaker), `gun_chemthrower`, `gun_drill`, `gun_pa_melee`, `gun_debug`
- energy: `gun_tau`, `gun_coilgun`, `gun_n_i_4_n_i`, `gun_tesla_cannon`, `gun_laser_pistol`(+pew_pew/morning_glory), `gun_lasrifle`, `gun_fatman`, `gun_folly`, `gun_aberrator`(+eott)

## Still static / incomplete

- `gun_fireext` — CE uses legacy `ItemRenderFireExt` (`TEISRBase`), not Sedna `ItemRenderWeaponBase`. TODO(CE:ItemRenderFireExt.java:20)
- `gun_pa_ranged` — CE `ItemGunPA`, no dedicated Sedna FP renderer. TODO(CE:XFactoryPA.java:36)
- `gun_b92` — legacy `ItemRenderGunAnim`, not Sedna bus. TODO(CE:ClientProxy.java:323)
- Orphan CE JSON never referenced in CE Java either: `python.json`, `cursed.json`, `novac.json`, `ks23.json`, `supershotty.json`, `benelli.json`. TODO(CE:ResourceManager.java:537-543)

## True blockers (not invented around)

- smokeNodes trail: TODO(CE:ItemGunBaseNT.java:329)
- Custom FP projection `setPerspectiveAndRender`: TODO(CE:ItemRenderWeaponBase.java:116). Port uses BEWLR + `applyForgeHandTransform` + `ViewportEvent.ComputeFov`.
- CYCLE recoil callback: TODO(CE:GunAnimationPacketSedna.java:96-102)
- Trenchmaster reload speed / equip-progress reset: TODO(CE:GunAnimationPacketSedna.java:116-119)
- Spent-casing tint: TODO(CE:ItemRenderSPAS12.java:64-73) / TODO(CE:ItemRenderCongoLake.java:86)
- Tesla yomi plushie: TODO(CE:ItemRenderTeslaCannon.java:106) (`RenderPlushie` not ported)
- Stinger “Not accurate” font: TODO(CE:ItemRenderStinger.java:81-100)
- Folly/Bolter/Shredder/Quadro/MissileLauncher ADS FontRenderer overlays: 1.12 immediate-mode
- Aberrator golden-sword orbit quads: TODO(CE:ItemRenderAberrator.java:135)
- FatMan balefire glint: TODO(CE:ItemRenderFatMan.java:143) (`RenderMiscEffects`)
- NI4NI dye/coin NBT: TODO(CE:ItemGunNI4NI)
- PA melee IPAWeaponsProvider: TODO(CE:XFactoryPA.java:36) — only CE NCR-arm OBJ is drawn
- MAS36 clip-plane: TODO(CE:ItemRenderMAS36.java:49)
