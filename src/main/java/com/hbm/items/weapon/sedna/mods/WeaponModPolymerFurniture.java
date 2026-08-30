package com.hbm.items.weapon.sedna.mods;

import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.ItemGunBaseNT.LambdaContext;
import com.hbm.items.weapon.sedna.Receiver;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;
import java.util.function.BiConsumer;

/** Direct port of CE's {@code WeaponModPolymerFurniture} (25 lines) - a G3 stock/handguard swap with slightly randomized recoil. */
public class WeaponModPolymerFurniture extends WeaponModBase {

    public WeaponModPolymerFurniture(String id) {
        super(id, "FURNITURE");
    }

    @Override
    public <T> T eval(T base, ItemStack gun, String key, Object parent) {
        if (Objects.equals(key, Receiver.CON_ONRECOIL)) return cast(LAMBDA_RECOIL_G3, base);
        return base;
    }

    public static final BiConsumer<ItemStack, LambdaContext> LAMBDA_RECOIL_G3 = (stack, ctx) ->
            ItemGunBaseNT.setupRecoil((float) (ctx.getPlayer().getRandom().nextGaussian() * 0.125), (float) (ctx.getPlayer().getRandom().nextGaussian() * 0.125));
}
