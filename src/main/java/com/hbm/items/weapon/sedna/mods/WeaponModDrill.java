package com.hbm.items.weapon.sedna.mods;

import com.hbm.items.weapon.sedna.Receiver;
import net.minecraft.world.item.ItemStack;

/**
 * Port of CE's {@code WeaponModDrill} (40 lines) - a mining-drill bit upgrade (damage/reach/AoE/pierce/
 * harvest level).
 * <p>
 * <b>Forward reference.</b> CE's {@code D_REACH}/{@code F_DTNEG}/{@code F_PIERCE}/{@code I_AOE}/
 * {@code I_HARVEST} mod-eval keys are declared on {@code com.hbm.items.weapon.sedna.factory.XFactoryDrill}
 * (CE's {@code gun_drill} content class, which is not a {@code GunConfig}/{@code Receiver}-driven
 * state machine gun at all - {@code XFactoryDrill} calls {@code XWeaponModManager.eval(base, stack,
 * KEY, ModItems.gun_drill, 0)} directly with the gun item itself as the eval "parent"). That content
 * class does not exist in this port yet (Package D, blocked on the same {@code gun_drill}/mining-tool
 * item not being registered) - the 5 key constants are declared locally here instead of on that
 * not-yet-existing class so this mod class compiles independently of it. Move these constants to
 * {@code XFactoryDrill} (and delete the copies here) once that content class lands - they must stay
 * byte-for-byte identical strings since {@link XWeaponModManager#eval} matches on string identity.
 */
public class WeaponModDrill extends WeaponModBase {

    /** Forward-reference key constants - see class javadoc. */
    public static final String D_REACH = "D_REACH";
    public static final String F_DTNEG = "F_DTNEG";
    public static final String F_PIERCE = "F_PIERCE";
    public static final String I_AOE = "I_AOE";
    public static final String I_HARVEST = "I_HARVEST";

    protected float damage = 1;
    protected double reach = 1;
    protected float dt = -1;
    protected float pierce = -1;
    protected int aoe = -1;
    protected int harvest = -1;

    public WeaponModDrill(String id) {
        super(id, "DRILL");
        this.setPriority(PRIORITY_SET);
    }

    public WeaponModDrill damage(float damage) { this.damage = damage; return this; }
    public WeaponModDrill reach(double reach) { this.reach = reach; return this; }
    public WeaponModDrill dt(float dt) { this.dt = dt; return this; }
    public WeaponModDrill pierce(float pierce) { this.pierce = pierce; return this; }
    public WeaponModDrill aoe(int aoe) { this.aoe = aoe; return this; }
    public WeaponModDrill harvest(int harvest) { this.harvest = harvest; return this; }

    @Override
    public <T> T eval(T base, ItemStack gun, String key, Object parent) {
        if (key.equals(Receiver.F_BASEDAMAGE)) return cast((Float) base * damage, base);
        if (key.equals(D_REACH)) return cast((Double) base * reach, base);
        if (key.equals(F_DTNEG) && dt >= 0) return cast(dt, base);
        if (key.equals(F_PIERCE) && pierce >= 0) return cast(pierce, base);
        if (key.equals(I_AOE) && aoe >= 0) return cast(aoe, base);
        if (key.equals(I_HARVEST) && harvest >= 0) return cast(harvest, base);
        return base;
    }
}
