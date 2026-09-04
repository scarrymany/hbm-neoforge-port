package com.hbm.items.weapon.sedna.mods;

import com.hbm.items.weapon.sedna.Receiver;
import com.hbm.items.weapon.sedna.content.XFactoryDrill;
import net.minecraft.world.item.ItemStack;

/**
 * Port of CE's {@code WeaponModDrill} (40 lines) - a mining-drill bit upgrade (damage/reach/AoE/pierce/
 * harvest level).
 * <p>
 * Eval keys live on {@link XFactoryDrill} (Exact CE).
 */
public class WeaponModDrill extends WeaponModBase {

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
        if (key.equals(XFactoryDrill.D_REACH)) return cast((Double) base * reach, base);
        if (key.equals(XFactoryDrill.F_DTNEG) && dt >= 0) return cast(dt, base);
        if (key.equals(XFactoryDrill.F_PIERCE) && pierce >= 0) return cast(pierce, base);
        if (key.equals(XFactoryDrill.I_AOE) && aoe >= 0) return cast(aoe, base);
        if (key.equals(XFactoryDrill.I_HARVEST) && harvest >= 0) return cast(harvest, base);
        return base;
    }
}
