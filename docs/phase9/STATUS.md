# Phase 9 — remaining CE mobs / vehicles

Stay on `cursor/compile-fix-wave-v2-4af2`. CE is content truth; neo-edition = API only.

## Ported this slice (cite CE)

| CE class | lines | `@AutoRegister` | Port |
|---|---:|---|---|
| `EntityGlowingOne` | 113 | `entity_glowing_one` | `entity/mob/EntityGlowingOne.java` |
| `EntityGhost` | 59 | `entity_ntm_ghost` | `EntityGhost.java` |
| `EntityFBI` | 196 | `entity_ntm_fbi` | `EntityFBI.java` (AIBreaking/MLPF not ported) |
| `EntityFBIDrone` | 73 | `entity_ntm_fbi_drone` | `EntityFBIDrone.java` + `EntityUFOBase.java` (207) |
| `EntityUndeadSoldier` | 125 | `entity_ntm_undead_soldier` | `EntityUndeadSoldier.java` |
| `EntityPigeon` | 238 | `entity_pigeon` | `EntityPigeon.java` (flying AI inlined) |
| `EntityPlasticBag` | 147 | `entity_plastic_bag` | `EntityPlasticBag.java` |
| `EntityParasiteMaggot` | 66 | `entity_parasite_maggot` | `EntityParasiteMaggot.java` |
| `EntityBlockSpider` | 58 | `entity_taintcrawler` | `EntityBlockSpider.java` |
| `EntityDummy` | 63 | `entity_ntm_test_dummy` | `EntityDummy.java` |
| `EntityBoatRubber` + `ItemBoatRubber` | 26+106 | `entity_rubber_boat` | `entity/item/EntityBoatRubber.java` |
| Glyphid family | 674+variants | `entity_glyphid*` | `entity/mob/glyphid/*` |

Hive waypoint / `EntityWaypoint` / `GlyphidHive` / `EntityAcidBomb` / `EntityChemical` / flying-AI classes **not** ported. Combat + summon + loot + nuclear death explosion are.

## Still missing vs CE (projectiles / FX / vehicles, not this slice)

`EntitySoyuz`, `EntitySoyuzCapsule`, `EntityDuchessGambit`, leftover projectiles (`EntityShrapnel`, `EntityBoxcar`, beams, …).
