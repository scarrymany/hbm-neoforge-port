package com.hbm.inventory.material;

import com.hbm.main.MainRegistry;
import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Encapsulates a material that used to be a Forge ore-dict alias set (a CE "DictFrame") - now the
 * central description of one entry in {@link Mats}: its identity, the physical shapes it
 * auto-generates items for, its metal/nonmetal trait, crucible smelting behavior and conversion
 * ratio, and its solid/molten render colors.
 *
 * <p>CE's {@code make(Item, int)} / {@code make(Item)} helpers are intentionally dropped - they
 * built an {@code ItemStack} carrying this material's numeric id as legacy metadata, which no
 * longer exists in 1.21. Once Phase 1 registers one distinct {@code Item} per (material, shape)
 * pair (see {@link MaterialShapes#buildRegistryName(NTMMaterial)}), stack construction is just
 * {@code new ItemStack(item, amount)} at the call site - there is nothing left for this class to
 * bake into a stack.
 */
public class NTMMaterial {

    public final int id;
    /** Alias names, index 0 is canonical (used for the registry name and translation key). */
    public final String[] names;
    public final Set<MaterialShapes> autogen = new HashSet<>();
    public final Set<MatTraits> traits = new HashSet<>();
    public SmeltingBehavior smeltable = SmeltingBehavior.NOT_SMELTABLE;
    public int solidColorLight = 0xFF4A00;
    public int solidColorDark = 0x802000;
    public int moltenColor = 0xFF4A00;

    public NTMMaterial smeltsInto;
    public int convIn;
    public int convOut;

    public NTMMaterial(int id, String... names) {
        this.id = id;
        this.names = names;

        this.smeltsInto = this;
        this.convIn = 1;
        this.convOut = 1;

        for (String name : names) {
            Mats.matByName.put(name, this);
        }

        Mats.orderedList.add(this);
        Mats.matById.put(id, this);
    }

    /** Lowercase, registry-legal token derived from the canonical name, e.g. "Uranium235" -> "uranium235". */
    public String getRegistryName() {
        return this.names[0].toLowerCase(Locale.US);
    }

    public String getDescriptionId() {
        return "hbmmat." + getRegistryName();
    }

    public Component getName() {
        return Component.translatable(getDescriptionId());
    }

    public NTMMaterial setConversion(NTMMaterial mat, int in, int out) {
        this.smeltsInto = mat;
        this.convIn = in;
        this.convOut = out;
        return this;
    }

    /** Shapes for autogen */
    public NTMMaterial setAutogen(MaterialShapes... shapes) {
        for (MaterialShapes shape : shapes) {
            if (shape != null) {
                this.autogen.add(shape);
            } else {
                MainRegistry.logger.warn("Warning: Null MaterialShape passed to setAutogen for " + this.names[0]);
            }
        }
        return this;
    }

    public Set<MaterialShapes> getAutogen() {
        return this.autogen;
    }

    /** Traits for recipe detection */
    public NTMMaterial setTraits(MatTraits... traits) {
        this.traits.addAll(Arrays.asList(traits));
        return this;
    }

    public NTMMaterial m() { this.traits.add(MatTraits.METAL); return this; }
    public NTMMaterial n() { this.traits.add(MatTraits.NONMETAL); return this; }

    /** Defines smelting behavior */
    public NTMMaterial smeltable(SmeltingBehavior behavior) {
        this.smeltable = behavior;
        return this;
    }

    public NTMMaterial setSolidColor(int colorLight, int colorDark) {
        this.solidColorLight = colorLight;
        this.solidColorDark = colorDark;
        return this;
    }

    public NTMMaterial setMoltenColor(int color) {
        this.moltenColor = color;
        return this;
    }

    public enum SmeltingBehavior {
        NOT_SMELTABLE,  //anything that can't be smelted or otherwise doesn't belong in a smelter, like diamond. may also include things that are smeltable but turn into a different type
        VAPORIZES,      //can't be smelted because the material would skadoodle
        BREAKS,         //can't be smelted because the material doesn't survive the temperatures
        SMELTABLE,      //mostly metal
        ADDITIVE        //stuff like coal which isn't smeltable but can be put in a crucible anyway
    }

    public enum MatTraits {
        METAL,      //metal(like), smeltable by arc furnaces
        NONMETAL    //non-metal(like), for gems, non-alloy compounds and similar
    }
}
