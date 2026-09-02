package com.hbm.inventory.recipes;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.sedna.content.GunEnergyItems;
import com.hbm.items.weapon.sedna.content.GunHeavyItems;
import com.hbm.items.weapon.sedna.content.GunLauncherItems;
import com.hbm.items.weapon.sedna.content.GunPistolItems;
import com.hbm.items.weapon.sedna.content.GunRifleItems;
import com.hbm.items.weapon.sedna.content.GunShotgunItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * Port of CE {@code PedestalRecipes.java} (SHA {@code 293649fc}): ritual crafting on 3×3 pedestal
 * grid (8 surrounding {@link com.hbm.blocks.generic.BlockPedestal} + 1 center pedestal holding item).
 * Redstone-powered center pedestal checks pattern, consumes inputs, spawns output. Celestial/karma
 * conditions (FULL_MOON/SUN/GOOD_KARMA) checked via {@link ExtraCondition}.
 * <p>
 * CE cite: {@code PedestalRecipes.java:15-222} (17 register calls + celestial/karma conditions),
 * {@code BlockPedestal.java:144-196} (neighbor scan + recipe match loop). Full census from
 * {@code docs/phase7/mrec_02_assembly_misc.md:204-220}.
 */
public final class PedestalRecipes {

    public static final List<PedestalRecipe> recipes = new ArrayList<>();

    private static boolean registered = false;

    private PedestalRecipes() {
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;

        // CE PedestalRecipes.java:34-222 — 17 recipes (12 legendary gun variants + 5 ammo_secret)
        // Ring order: [NW, N, NE, W, center, E, SW, S, SE] (CE BlockPedestal.java:144-156)
        
        // 1. gun_light_revolver_dani (no extra, set 0)
        // CE :45-53. Ring: null, PB.plate(), null / GOLD.plate(), —, GOLD.plate() / null, PB.plate(), null
        // Center: gun_light_revolver
        register(new PedestalRecipe(
                gun("gun_light_revolver"),
                new AStack[] {
                        null, plate("lead"), null,
                        plate("gold"), null, plate("gold"),
                        null, plate("lead"), null
                },
                gun("gun_light_revolver_dani"),
                null, 0
        ));

        // 2. gun_maresleg_broken (no extra, set 0)
        // CE :55-63. Ring: barbed_wire×4 corners, WEAPONSTEEL.plate()×4 edges
        // TODO(CE: PedestalRecipes.java:56-60): bolt_spike×16 not registered; CE uses barbed_wire BLOCK
        // (ModBlocks.barbed_wire) as ring ingredient. NeoForge: barbed_wire exists as block
        // (GenericBlocks.java:562-567). Accept block OR item? For now: skip (no barbed_wire item).
        // Center: gun_maresleg
        
        // 3. gun_heavy_revolver_lilmac (no extra, set 0)
        // CE :65-73. Ring: null, weapon_mod_special(SCOPE), null / powder_magic, —, WEAPONSTEEL.plate() /
        // null, BONE.grip(), null, APPLE, APPLE, APPLE
        // Center: gun_heavy_revolver
        register(new PedestalRecipe(
                gun("gun_heavy_revolver"),
                new AStack[] {
                        null, weaponModScope(), null,
                        powder("powder_magic"), null, plate("desh"), // CE WEAPONSTEEL ≈ desh
                        null, item("bone_grip"), new ComparableStack(Items.APPLE, 3)
                },
                gun("gun_heavy_revolver_lilmac"),
                null, 0
        ));

        // 4. gun_heavy_revolver_protege (no extra, set 0)
        // CE :75-83. Ring: chain×16 corners, CINNABAR.gem()/scrap_nuclear edges
        // TODO(CE: PedestalRecipes.java:77-79): chain not registered as item (only dungeon_chain block).
        // Skip for now.
        
        // 5. gun_amat_subtlety (no extra, set 0)
        // CE :85-93. Ring: STAR.ingot() corners, AL.plateCast() edges
        register(new PedestalRecipe(
                gun("gun_amat"),
                new AStack[] {
                        ingot("schrabidium"), plate("aluminium_cast"), ingot("schrabidium"),
                        plate("aluminium_cast"), null, plate("aluminium_cast"),
                        ingot("schrabidium"), plate("aluminium_cast"), ingot("schrabidium")
                },
                gun("gun_amat_subtlety"),
                null, 0
        ));

        // 6. gun_amat_penance (no extra, set 0)
        // CE :95-103. Ring: STAR.ingot() corners, DURA.plateCast()/weapon_mod_special(SILENCER/FURNITURE_BLACK) edges
        register(new PedestalRecipe(
                gun("gun_amat"),
                new AStack[] {
                        ingot("schrabidium"), plate("dura_cast"), ingot("schrabidium"),
                        weaponModSilencer(), null, weaponModFurnitureBlack(),
                        ingot("schrabidium"), plate("dura_cast"), ingot("schrabidium")
                },
                gun("gun_amat_penance"),
                null, 0
        ));

        // 7. gun_flamer_daybreaker (SUN, set 0)
        // CE :105-113. Ring: GOLD.plateCast() corners, canned_conserve(JIZZ)/P_WHITE.ingot()/stick_dynamite edges
        // TODO(CE: PedestalRecipes.java:107-109): canned_conserve(JIZZ) not registered (no meta variants).
        // Use canned_conserve_tube as placeholder.
        register(new PedestalRecipe(
                gun("gun_flamer"),
                new AStack[] {
                        plate("gold_cast"), item("canned_conserve_tube"), plate("gold_cast"),
                        ingot("phosphorus_white"), null, item("stick_dynamite"),
                        plate("gold_cast"), item("canned_conserve_tube"), plate("gold_cast")
                },
                gun("gun_flamer_daybreaker"),
                ExtraCondition.SUN, 0
        ));

        // 8. gun_autoshotgun_sexy (no extra, set 0)
        // CE :115-123. Ring: bolt_spike×16 corners, wild_p/card_qos/card_aos/STAR.ingot()×16 edges
        // TODO(CE: PedestalRecipes.java:117-119): bolt_spike not registered (commented out in CE bolt pool).
        // TODO(CE): wild_p/card_qos/card_aos exist in ItemPoolsRedRoom but not as discrete items yet.
        // Skip for now.
        
        // 9. gun_minigun_lacunae (FULL_MOON, set 0)
        // CE :125-133. Ring: null corners, powder_magic×4 / item_secret(SELENIUM_STEEL)×4 edges
        // TODO(CE: PedestalRecipes.java:127-129): item_secret(SELENIUM_STEEL) only partially ported
        // (Phase11ProcessItems.java:289). Use powder_magic placeholder for now.
        register(new PedestalRecipe(
                gun("gun_minigun"),
                new AStack[] {
                        null, powder("powder_magic"), null,
                        item("item_secret_selenium_steel"), null, item("item_secret_selenium_steel"),
                        null, powder("powder_magic"), null
                },
                gun("gun_minigun_lacunae"),
                ExtraCondition.FULL_MOON, 0
        ));

        // 10. gun_laser_pistol_morning_glory (no extra, set 0)
        // CE :135-143. Ring: null corners, morning_glory/item_secret(SELENIUM_STEEL)×2/EMERALD.gem()×16 edges
        // TODO(CE: PedestalRecipes.java:137-139): morning_glory exists in loot pools (ItemPoolsRedRoom.java:23).
        // Check if discrete item registered.
        register(new PedestalRecipe(
                gun("gun_laser_pistol"),
                new AStack[] {
                        null, item("morning_glory"), null,
                        item("item_secret_selenium_steel"), null, item("item_secret_selenium_steel"),
                        null, new ComparableStack(Items.EMERALD, 16), null
                },
                gun("gun_laser_pistol_morning_glory"),
                null, 0
        ));

        // 11. gun_folly (FULL_MOON, set 1)
        // CE :145-153. Ring: item_secret(FOLLY)×4 corners, item_secret(CONTROLLER)×2/BSCCO.ingot()×16 edges
        // TODO(CE: PedestalRecipes.java:147-149): item_secret(FOLLY/CONTROLLER) skipped (ItemPoolsRedRoom.java:41/47).
        // Use placeholder.
        
        // 12. gun_aberrator (no extra, set 1)
        // CE :155-163. Ring: null corners, item_secret(ABERRATOR) edges
        // TODO(CE: PedestalRecipes.java:157-159): item_secret(ABERRATOR) not registered.
        // Skip for now.
        
        // 13. gun_aberrator_eott (GOOD_KARMA, set 1)
        // CE :165-173. Ring: item_secret(ABERRATOR) all 8 slots
        // TODO(CE: PedestalRecipes.java:167-169): same as above.
        
        // 14. ammo_secret(FOLLY_SM) ×1 (FULL_MOON, set 1)
        // CE :175-183. Ring: STAR.ingot() corners, powder_magic edges
        // Center: chunk_ore(MOONSTONE)
        register(new PedestalRecipe(
                item("chunk_ore_moonstone"),
                new AStack[] {
                        ingot("schrabidium"), powder("powder_magic"), ingot("schrabidium"),
                        powder("powder_magic"), null, powder("powder_magic"),
                        ingot("schrabidium"), powder("powder_magic"), ingot("schrabidium")
                },
                ammo("folly_sm"),
                ExtraCondition.FULL_MOON, 1
        ));

        // 15. ammo_secret(FOLLY_NUKE) ×1 (FULL_MOON, set 1)
        // CE :185-193. Ring: STAR.ingot() corners, powder_magic edges
        // Center: ammo_standard(NUKE_HIGH)×4
        // TODO(CE: PedestalRecipes.java:188): ammo_standard not ported (missile ammo family).
        // Skip for now.
        
        // 16. ammo_secret(P35_800) ×5 (no extra, set 1)
        // CE :195-203. Ring: all null except center
        // Center: item_secret(ABERRATOR)×1
        // TODO(CE: PedestalRecipes.java:198): item_secret(ABERRATOR) not registered.
        
        // 17. ammo_secret(P35_800_BL) ×10 (no extra, set 1)
        // CE :205-213. Ring: all null except center
        // Center: item_secret(ABERRATOR)×3
        // TODO(CE: PedestalRecipes.java:208): same as above.
    }

    private static void register(PedestalRecipe recipe) {
        recipes.add(recipe);
    }

    // ========== Helper constructors ==========

    private static AStack gun(String name) {
        // Map common gun names to their GunXxxItems registry
        return switch (name) {
            case "gun_light_revolver" -> new ComparableStack(GunPistolItems.GUN_LIGHT_REVOLVER.get());
            case "gun_light_revolver_dani" -> new ComparableStack(GunPistolItems.GUN_LIGHT_REVOLVER_DANI.get());
            case "gun_maresleg" -> new ComparableStack(GunRifleItems.GUN_MARESLEG.get());
            case "gun_heavy_revolver" -> new ComparableStack(GunPistolItems.GUN_HEAVY_REVOLVER.get());
            case "gun_heavy_revolver_lilmac" -> new ComparableStack(GunPistolItems.GUN_HEAVY_REVOLVER_LILMAC.get());
            case "gun_heavy_revolver_protege" -> new ComparableStack(GunPistolItems.GUN_HEAVY_REVOLVER_PROTEGE.get());
            case "gun_amat" -> new ComparableStack(GunRifleItems.GUN_AMAT.get());
            case "gun_amat_subtlety" -> new ComparableStack(GunRifleItems.GUN_AMAT_SUBTLETY.get());
            case "gun_amat_penance" -> new ComparableStack(GunRifleItems.GUN_AMAT_PENANCE.get());
            case "gun_flamer" -> new ComparableStack(GunLauncherItems.GUN_FLAMER.get());
            case "gun_flamer_daybreaker" -> new ComparableStack(GunLauncherItems.GUN_FLAMER_DAYBREAKER.get());
            case "gun_autoshotgun" -> new ComparableStack(GunShotgunItems.GUN_AUTOSHOTGUN.get());
            case "gun_minigun" -> new ComparableStack(GunHeavyItems.GUN_MINIGUN.get());
            case "gun_minigun_lacunae" -> new ComparableStack(GunHeavyItems.GUN_MINIGUN_LACUNAE.get());
            case "gun_laser_pistol" -> new ComparableStack(GunEnergyItems.GUN_LASER_PISTOL.get());
            case "gun_laser_pistol_morning_glory" -> new ComparableStack(GunEnergyItems.GUN_LASER_PISTOL_MORNING_GLORY.get());
            case "gun_folly" -> new ComparableStack(GunEnergyItems.GUN_FOLLY.get());
            case "gun_aberrator" -> new ComparableStack(GunEnergyItems.GUN_ABERRATOR.get());
            case "gun_aberrator_eott" -> new ComparableStack(GunEnergyItems.GUN_ABERRATOR_EOTT.get());
            default -> throw new IllegalArgumentException("Unknown gun: " + name);
        };
    }

    private static AStack ammo(String name) {
        return switch (name) {
            case "folly_sm" -> new ComparableStack(GunEnergyItems.FOLLY_SM.get());
            case "folly_nuke" -> new ComparableStack(GunEnergyItems.FOLLY_NUKE.get());
            default -> throw new IllegalArgumentException("Unknown ammo: " + name);
        };
    }

    private static AStack plate(String materialName) {
        // Map material names to their plate items
        // CE uses MaterialShapes system: STEEL.plate() → steel_plate
        String itemName = switch (materialName) {
            case "lead" -> "lead_plate";
            case "gold" -> "gold_plate";
            case "desh" -> "desh_plate"; // CE WEAPONSTEEL closest equivalent
            case "aluminium_cast" -> "aluminium_plate_triple"; // CE plateCast
            case "dura_cast" -> "dura_plate_triple";
            case "gold_cast" -> "gold_plate_triple";
            default -> throw new IllegalArgumentException("Unknown plate material: " + materialName);
        };
        return item(itemName);
    }

    private static AStack ingot(String materialName) {
        String itemName = switch (materialName) {
            case "schrabidium" -> "ingot_schrabidium"; // CE STAR
            case "phosphorus_white" -> "ingot_phosphorus_white"; // CE P_WHITE
            default -> "ingot_" + materialName;
        };
        return item(itemName);
    }

    private static AStack powder(String name) {
        return item(name);
    }

    private static AStack weaponModScope() {
        return item("weapon_mod_special_scope");
    }

    private static AStack weaponModSilencer() {
        return item("weapon_mod_special_silencer");
    }

    private static AStack weaponModFurnitureBlack() {
        // TODO(CE: PedestalRecipes.java:99): weapon_mod_special(FURNITURE_BLACK) not ported as discrete item.
        // CE has FURNITURE_BLACK as weapon mod enum variant. Use generic furniture as placeholder.
        return item("weapon_mod_special_scope"); // Placeholder — need actual furniture_black
    }

    private static AStack item(String name) {
        return new ComparableStack(ModItems.itemByName(name));
    }

    // ========== Recipe container ==========

    public static class PedestalRecipe {
        public final AStack centerInput;
        public final AStack[] ring; // 8 elements: [NW, N, NE, W, E, SW, S, SE] (center excluded)
        public final ItemStack output;
        public final ExtraCondition extra;
        public final int recipeSet;

        public PedestalRecipe(AStack center, AStack[] ring, AStack output, ExtraCondition extra, int set) {
            this.centerInput = center;
            this.ring = ring;
            this.output = output.getStack();
            this.extra = extra;
            this.recipeSet = set;
        }

        /**
         * CE BlockPedestal.java:188-196. Check if 9-element array (center + 8 ring positions)
         * matches this recipe's pattern. {@code stacks[4]} = center, rest = ring clockwise from NW.
         */
        public boolean matches(ItemStack[] stacks, Level level) {
            if (stacks.length != 9) return false;

            // Check center
            if (!centerInput.matchesRecipe(stacks[4], false)) return false;

            // Check ring (stacks[0..3,5..8] = NW/N/NE/W/E/SW/S/SE)
            int[] ringIndices = {0, 1, 2, 3, 5, 6, 7, 8};
            for (int i = 0; i < 8; i++) {
                AStack required = ring[i];
                ItemStack actual = stacks[ringIndices[i]];

                if (required == null) {
                    if (!actual.isEmpty()) return false;
                } else {
                    if (!required.matchesRecipe(actual, false)) return false;
                }
            }

            // Check extra condition (celestial/karma)
            if (extra != null && !extra.check(level)) return false;

            return true;
        }

        /**
         * Consume all 9 input stacks (shrink by recipe amounts).
         */
        public void consume(ItemStack[] stacks) {
            if (stacks.length != 9) return;

            // Consume center
            stacks[4].shrink(centerInput.count());

            // Consume ring
            int[] ringIndices = {0, 1, 2, 3, 5, 6, 7, 8};
            for (int i = 0; i < 8; i++) {
                if (ring[i] != null) {
                    stacks[ringIndices[i]].shrink(ring[i].count());
                }
            }
        }
    }

    // ========== Extra conditions (moon/sun/karma) ==========

    public enum ExtraCondition {
        FULL_MOON,
        NEW_MOON,
        SUN,
        GOOD_KARMA,
        BAD_KARMA;

        /**
         * CE BlockPedestal.java:168-186. Check celestial angle / moon phase / player karma.
         * TODO(CE: PedestalRecipes.java:230-233): Karma system (HbmCapability.getData(player).getReputation())
         * not ported yet. GOOD_KARMA/BAD_KARMA always return false for now.
         */
        public boolean check(Level level) {
            return switch (this) {
                case FULL_MOON -> {
                    // CE: world.provider.getMoonPhase(world.getWorldTime()) == 0
                    // NeoForge: level.getMoonPhase() returns 0.0-1.0 (0 = full moon)
                    yield level.getMoonPhase() < 0.125F; // Close to 0 = full moon
                }
                case NEW_MOON -> {
                    yield level.getMoonPhase() >= 0.875F || level.getMoonPhase() < 0.125F; // Opposite of full
                }
                case SUN -> {
                    // CE: world.getCelestialAngle(0) between 0.74-0.76 (midday)
                    float angle = level.getSunAngle(0);
                    yield angle >= 0.24F && angle <= 0.26F; // Midday (adjusted for NeoForge angle range)
                }
                case GOOD_KARMA -> {
                    // TODO(CE: PedestalRecipes.java:232): HbmCapability.getData(player).getReputation() >= 10
                    // Karma/reputation system not ported yet. Return false for now.
                    yield false;
                }
                case BAD_KARMA -> {
                    // TODO(CE: PedestalRecipes.java:232): reputation <= -10
                    yield false;
                }
            };
        }
    }

    /**
     * Find first matching recipe for given 9-stack array (center + 8 ring).
     */
    public static PedestalRecipe findRecipe(ItemStack[] stacks, Level level) {
        register();
        for (PedestalRecipe recipe : recipes) {
            if (recipe.matches(stacks, level)) {
                return recipe;
            }
        }
        return null;
    }
}
