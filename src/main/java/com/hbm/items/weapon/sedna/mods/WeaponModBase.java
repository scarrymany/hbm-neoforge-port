package com.hbm.items.weapon.sedna.mods;

import com.hbm.main.MainRegistry;
import net.minecraft.resources.ResourceLocation;

/**
 * Port of CE's {@code com.hbm.items.weapon.sedna.mods.WeaponModBase} (29 lines) - shared base for
 * every concrete {@code WeaponMod*} effect class. See {@code docs/phase3/gun_framework.md}'s Package
 * C section, read in full.
 * <p>
 * The constructor takes a plain {@code String} id (namespaced under {@code hbm} exactly like
 * {@code BulletConfig(String path)}) instead of CE's raw {@code int} - see {@link IWeaponMod}'s
 * javadoc for the full id-scheme rationale. Self-registers into {@link XWeaponModManager}'s registry
 * at construction time, matching CE's own {@code WeaponModBase(int id, ...)} constructor doing the
 * same into {@code XWeaponModManager.idToMod}.
 */
public abstract class WeaponModBase implements IWeaponMod {

    public static final int PRIORITY_SET = 1_000_000;
    public static final int PRIORITY_MULTIPLICATIVE = 1_000;
    public static final int PRIORITY_ADDITIVE = 500;
    public static final int PRIORITY_MULT_FINAL = -1;

    public final ResourceLocation id;
    public String[] slots;
    public int priority = 0;

    public WeaponModBase(String id, String... slots) {
        this(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, id), slots);
    }

    public WeaponModBase(ResourceLocation id, String... slots) {
        this.id = id;
        this.slots = slots;
        XWeaponModManager.register(id, this);
    }

    public WeaponModBase setPriority(int priority) {
        this.priority = priority;
        return this;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public int getModPriority() {
        return priority;
    }

    @Override
    public String[] getSlots() {
        return slots;
    }

    /**
     * Java generics are cool and all but once you actually get to use them, they suck ass.
     * This piece of shit only exists to prevent double cast, casting from int to {@code <T>} would
     * require {@code (T) (Integer) int}, which makes me want to vomit. Using this method here
     * implicitly casts the int (or whatever it is) to Object, and Object can be cast to {@code <T>}.
     */
    @SuppressWarnings("unchecked")
    public <T> T cast(Object arg, T castTo) {
        return (T) arg;
    }
}
