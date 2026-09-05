package com.hbm.client.render.item.weapon;

import com.hbm.config.ClientConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.render.anim.sedna.BusAnimationKeyframeSedna.IType;
import com.hbm.render.anim.sedna.BusAnimationSedna;
import com.hbm.render.anim.sedna.BusAnimationSequenceSedna;
import com.hbm.weapon.anim.GunAnimationType;
import com.hbm.weapon.anim.HbmAnimationType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

import java.util.function.BiFunction;

/** CE XFactory LAMBDA_*_ANIMS, copied verbatim. wrap() adapts GunAnimationType to HbmAnimationType. */
public final class GunAnims {
    private GunAnims() {}

    public static BiFunction<ItemStack, HbmAnimationType, BusAnimationSedna> wrap(
            BiFunction<ItemStack, GunAnimationType, BusAnimationSedna> inner) {
        return (stack, raw) -> raw instanceof GunAnimationType type ? inner.apply(stack, type) : null;
    }

    // CE: XFactory10ga.java
    public static final BiFunction<ItemStack, GunAnimationType, BusAnimationSedna> LAMBDA_DOUBLE_BARREL_ANIMS = (stack, type) -> switch (type) {
        case EQUIP -> new BusAnimationSedna()
                .addBus("EQUIP", new BusAnimationSequenceSedna().addPos(-60, 0, 0, 0).addPos(0, 0, -3, 500, IType.SIN_DOWN));
        case CYCLE -> new BusAnimationSedna()
                .addBus("RECOIL", new BusAnimationSequenceSedna().addPos(0, 0, -1, 50).addPos(0, 0, 0, 250))
                .addBus("BUCKLE", new BusAnimationSequenceSedna().addPos(0, -60, 0, 50).addPos(0, 0, 0, 250));
        case RELOAD -> new BusAnimationSedna()
                .addBus("TURN", new BusAnimationSequenceSedna()
                        .addPos(0, 30, 0, 350, IType.SIN_FULL)
                        .addPos(0, 30, 0, 1150)
                        .addPos(0, 0, 0, 350, IType.SIN_FULL))
                .addBus("LEVER", new BusAnimationSequenceSedna()
                        .addPos(0, 0, 0, 250)
                        .addPos(0, 0, -90, 100, IType.SIN_FULL)
                        .addPos(0, 0, -90, 1300)
                        .addPos(0, 0, 0, 100, IType.SIN_FULL))
                .addBus("BARREL", new BusAnimationSequenceSedna()
                        .addPos(0, 0, 0, 300)
                        .addPos(60, 0, 0, 150, IType.SIN_UP)
                        .addPos(60, 0, 0, 1150)
                        .addPos(0, 0, 0, 150, IType.SIN_UP))
                .addBus("LIFT", new BusAnimationSequenceSedna()
                        .addPos(0, 0, 0, 350)
                        .addPos(-5, 0, 0, 150, IType.SIN_FULL)
                        .addPos(0, 0, 0, 100, IType.SIN_FULL)
                        .addPos(0, 0, 0, 700)
                        .addPos(-5, 0, 0, 100, IType.SIN_FULL)
                        .addPos(0, 0, 0, 100, IType.SIN_UP) //1500
                        .addPos(45, 0, 0, 150)
                        .addPos(45, 0, 0, 150)
                        .addPos(-5, 0, 0, 150, IType.SIN_DOWN)
                        .addPos(0, 0, 0, 100, IType.SIN_FULL)) //2050
                .addBus("SHELLS", new BusAnimationSequenceSedna()
                        .addPos(0, 0, 0, 450)
                        .addPos(0, 0, -2.5, 100)
                        .addPos(0, -5, -5, 350, IType.SIN_DOWN)
                        .addPos(0, -3, -2, 0)
                        .addPos(0, 0, -2, 250)
                        .addPos(0, 0, 0, 150, IType.SIN_UP)) //1300
                .addBus("SHELL_FLIP", new BusAnimationSequenceSedna().addPos(0, 0, 0, 450).addPos(-360, 0, 0, 450).addPos(0, 0, 0, 0));
        case INSPECT -> new BusAnimationSedna()
                .addBus("LEVER", new BusAnimationSequenceSedna()
                        .addPos(0, 0, 0, 250)
                        .addPos(0, 0, -90, 100, IType.SIN_FULL)
                        .addPos(0, 0, -90, 800)
                        .addPos(0, 0, 0, 100, IType.SIN_FULL))
                .addBus("BARREL", new BusAnimationSequenceSedna()
                        .addPos(0, 0, 0, 300)
                        .addPos(60, 0, 0, 150, IType.SIN_UP)
                        .addPos(60, 0, 0, 650)
                        .addPos(0, 0, 0, 150, IType.SIN_UP))
                .addBus("LIFT", new BusAnimationSequenceSedna()
                        .addPos(0, 0, 0, 350)
                        .addPos(-5, 0, 0, 150, IType.SIN_FULL)
                        .addPos(0, 0, 0, 100, IType.SIN_FULL)
                        .addPos(0, 0, 0, 200)
                        .addPos(-5, 0, 0, 100, IType.SIN_FULL)
                        .addPos(0, 0, 0, 100, IType.SIN_UP) //1500
                        .addPos(45, 0, 0, 150)
                        .addPos(45, 0, 0, 150)
                        .addPos(-5, 0, 0, 150, IType.SIN_DOWN)
                        .addPos(0, 0, 0, 100, IType.SIN_FULL));
        default -> null;
    };

    // CE: XFactoryTool.java
    public static final BiFunction<ItemStack, GunAnimationType, BusAnimationSedna> LAMBDA_CT_ANIMS = (stack, type) -> switch (type) {
        case EQUIP -> new BusAnimationSedna()
                .addBus("EQUIP", new BusAnimationSequenceSedna().addPos(-45, 0, 0, 0).addPos(0, 0, 0, 500, IType.SIN_DOWN));
        case CYCLE -> new BusAnimationSedna()
                .addBus("RECOIL", new BusAnimationSequenceSedna().addPos(0, 0, -1, 100, IType.SIN_DOWN).addPos(0, 0, 0, 250, IType.SIN_FULL));
        case RELOAD -> new BusAnimationSedna()
                .addBus("RAISE", new BusAnimationSequenceSedna().addPos(-45, 0, 0, 500, IType.SIN_FULL).hold(2000).addPos(0, 0, 0, 500, IType.SIN_FULL))
                .addBus("AMMO", new BusAnimationSequenceSedna().setPos(0, -10, -5).hold(500).addPos(0, 0, 5, 750, IType.SIN_FULL).addPos(0, 0, 0, 500, IType.SIN_UP).hold(4000))
                .addBus("TWIST", new BusAnimationSequenceSedna().setPos(0, 0, 25).hold(2000).addPos(0, 0, 0, 150));
        case INSPECT -> new BusAnimationSedna()
                .addBus("TURN", new BusAnimationSequenceSedna().addPos(0, 60, 0, 500, IType.SIN_FULL).hold(1750).addPos(0, 0, 0, 500, IType.SIN_FULL))
                .addBus("ROLL", new BusAnimationSequenceSedna().hold(750).addPos(0, 0, -90, 500, IType.SIN_FULL).hold(1000).addPos(0, 0, 0, 500, IType.SIN_FULL));
        default -> null;
    };

    // CE: XFactory50.java
    public static final BiFunction<ItemStack, GunAnimationType, BusAnimationSedna> LAMBDA_AMAT_ANIMS = (stack, type) -> {
        double turn = -60;
        double pullAmount = -2.5;
        double side = 4;
        double down = -2;
        double detach = 0.5;
        double apex = 7;

        return switch (type) {
            case EQUIP -> new BusAnimationSedna()
                    .addBus("EQUIP", new BusAnimationSequenceSedna().addPos(45, 0, 0, 0).addPos(0, 0, 0, 500, IType.SIN_FULL))
                    .addBus("BIPOD", new BusAnimationSequenceSedna().hold(500).addPos(80, 0, 0, 350).addPos(80, 25, 0, 150));
            case CYCLE -> new BusAnimationSedna()
                    .addBus("RECOIL", new BusAnimationSequenceSedna().addPos(0, 0, -0.5, 50, IType.SIN_DOWN).addPos(0, 0, 0, 100, IType.SIN_FULL))
                    .addBus("BOLT_TURN", new BusAnimationSequenceSedna().hold(250).addPos(0, 0, turn, 150).hold(700).addPos(0, 0, 0, 150))
                    .addBus("BOLT_PULL", new BusAnimationSequenceSedna().hold(350).addPos(0, 0, pullAmount, 250, IType.SIN_UP).hold(250).addPos(0, 0, 0, 200, IType.LINEAR))
                    .addBus("LIFT", new BusAnimationSequenceSedna().hold(600).addPos(-3, 0, 0, 150, IType.SIN_DOWN).hold(300).addPos(0, 0, 0, 250, IType.SIN_FULL));
            case CYCLE_DRY -> new BusAnimationSedna()
                    .addBus("BOLT_TURN", new BusAnimationSequenceSedna().hold(250).addPos(0, 0, turn, 150).hold(700).addPos(0, 0, 0, 150))
                    .addBus("BOLT_PULL", new BusAnimationSequenceSedna().hold(350).addPos(0, 0, pullAmount, 250, IType.SIN_UP).hold(250).addPos(0, 0, 0, 200, IType.LINEAR))
                    .addBus("LIFT", new BusAnimationSequenceSedna().hold(600).addPos(-3, 0, 0, 150, IType.SIN_DOWN).hold(300).addPos(0, 0, 0, 250, IType.SIN_FULL));
            case RELOAD -> new BusAnimationSedna()
                    .addBus("MAG", new BusAnimationSequenceSedna().addPos(0, -10, 0, 350, IType.SIN_UP).addPos(0, 0, 0, 650, IType.SIN_UP))
                    .addBus("LIFT", new BusAnimationSequenceSedna().hold(1000).addPos(-2, 0, 0, 150, IType.SIN_DOWN).addPos(0, 0, 0, 250, IType.SIN_FULL).hold(450).addPos(-3, 0, 0, 150, IType.SIN_DOWN).hold(300).addPos(0, 0, 0, 250, IType.SIN_FULL))
                    .addBus("BOLT_TURN", new BusAnimationSequenceSedna().hold(1500).addPos(0, 0, turn, 150).hold(700).addPos(0, 0, 0, 150))
                    .addBus("BOLT_PULL", new BusAnimationSequenceSedna().hold(1600).addPos(0, 0, pullAmount, 250, IType.SIN_UP).hold(250).addPos(0, 0, 0, 200, IType.LINEAR));
            case JAMMED -> new BusAnimationSedna()
                    .addBus("LIFT", new BusAnimationSequenceSedna().hold(250).addPos(-15, 0, 0, 500, IType.SIN_FULL).holdUntil(1650).addPos(0, 0, 0, 500, IType.SIN_FULL))
                    .addBus("BOLT_TURN", new BusAnimationSequenceSedna().hold(250).addPos(0, 0, turn, 150).holdUntil(1250).addPos(0, 0, 0, 150))
                    .addBus("BOLT_PULL", new BusAnimationSequenceSedna().hold(350).addPos(0, 0, pullAmount, 250, IType.SIN_UP).addPos(0, 0, 0, 200, IType.LINEAR).addPos(0, 0, pullAmount, 250, IType.SIN_UP).addPos(0, 0, 0, 200, IType.LINEAR));
            case INSPECT -> new BusAnimationSedna()
                    .addBus("SCOPE_THROW", new BusAnimationSequenceSedna().addPos(0, detach, 0, 100, IType.SIN_FULL).addPos(side, down, 0, 500, IType.SIN_FULL).addPos(side, down - 0.5, 0, 100).addPos(side, apex, 0, 350, IType.SIN_FULL).addPos(side, down - 0.5, 0, 350, IType.SIN_DOWN).addPos(side, down, 0, 100).hold(250).addPos(0, detach, 0, 500, IType.SIN_FULL).addPos(0, 0, 0, 250, IType.SIN_FULL))
                    .addBus("SCOPE_SPIN", new BusAnimationSequenceSedna().hold(700).addPos(-360, 0, 0, 700));
            default -> null;
        };

    };

    // CE: XFactory50.java
    public static final BiFunction<ItemStack, GunAnimationType, BusAnimationSedna> LAMBDA_M2_ANIMS = (stack, type) -> switch (type) {
        case EQUIP -> new BusAnimationSedna()
                .addBus("EQUIP", new BusAnimationSequenceSedna().addPos(80, 0, 0, 0).addPos(0, 0, 0, 500, IType.SIN_FULL));
        case CYCLE -> new BusAnimationSedna()
                .addBus("RECOIL", new BusAnimationSequenceSedna().addPos(0, 0, -0.25, 25).addPos(0, 0, 0, 75));
        default -> null;
    };

    // CE: XFactoryBlackPowder.java
    public static final BiFunction<ItemStack, GunAnimationType, BusAnimationSedna> LAMBDA_PEPPERBOX_ANIMS = (stack, type) -> switch (type) {
        case CYCLE -> new BusAnimationSedna()
                .addBus("ROTATE", new BusAnimationSequenceSedna().addPos(0, 0, 0, 1025).addPos(60, 0, 0, 250))
                .addBus("RECOIL", new BusAnimationSequenceSedna().addPos(0, 0, 0, 50).addPos(45, 0, 0, 150, IType.SIN_DOWN).addPos(45, 0, 0, 50).addPos(0, 0, 0, 500, IType.SIN_FULL))
                .addBus("HAMMER", new BusAnimationSequenceSedna().addPos(80, 0, 0, 25).addPos(80, 0, 0, 1000).addPos(0, 0, 0, 250))
                .addBus("TRIGGER", new BusAnimationSequenceSedna().addPos(1, 0, 0, 25).addPos(1, 0, 0, 250).addPos(0, 0, 0, 100));
        case CYCLE_DRY -> new BusAnimationSedna()
                .addBus("ROTATE", new BusAnimationSequenceSedna().addPos(0, 0, 0, 525).addPos(60, 0, 0, 250))
                .addBus("HAMMER", new BusAnimationSequenceSedna().addPos(80, 0, 0, 25).addPos(80, 0, 0, 500).addPos(0, 0, 0, 250))
                .addBus("TRIGGER", new BusAnimationSequenceSedna().addPos(1, 0, 0, 25).addPos(1, 0, 0, 250).addPos(0, 0, 0, 100));
        case EQUIP -> new BusAnimationSedna()
                .addBus("RECOIL", new BusAnimationSequenceSedna().addPos(-45, 0, 0, 0).addPos(0, 0, 0, 200, IType.SIN_DOWN));
        case RELOAD -> new BusAnimationSedna()
                .addBus("RECOIL", new BusAnimationSequenceSedna().addPos(90, 0, 0, 500, IType.SIN_FULL).addPos(90, 0, 0, 1600).addPos(0, 0, 0, 500, IType.SIN_FULL).addPos(-5, 0, 0, 200, IType.SIN_UP).addPos(0, 0, 0, 200, IType.SIN_DOWN))
                .addBus("TRANSLATE", new BusAnimationSequenceSedna().addPos(0, -12, 5, 500, IType.SIN_FULL).addPos(0, -12, 5, 700).addPos(0, -13, 5, 200).addPos(0, -12, 5, 200).addPos(0, -12, 5, 500).addPos(0, 0, 0, 500, IType.SIN_FULL))
                .addBus("LOADER", new BusAnimationSequenceSedna().addPos(0, 0, 0, 500).addPos(0, 5, -5, 0).addPos(0, 0, -0.1, 500, IType.SIN_FULL).addPos(0, 0, -1, 200).addPos(0, 0, -1, 200).addPos(0, 0, -0.1, 200).addPos(0, 5, -5, 500, IType.SIN_FULL).addPos(0, 0, 0, 0))
                .addBus("ROTATE", new BusAnimationSequenceSedna().addPos(0, 0, 0, 2600).addPos(-360, 0, 0, 750, IType.SIN_FULL))
                .addBus("SHOT", new BusAnimationSequenceSedna().addPos(1, 0, 0, 1400).addPos(0, 0, 0, 0));
        case INSPECT -> new BusAnimationSedna()
                .addBus("ROTATE", new BusAnimationSequenceSedna().addPos(-360, 0, 0, 750, IType.SIN_FULL))
                .addBus("RECOIL", new BusAnimationSequenceSedna().addPos(-5, 0, 0, 200, IType.SIN_UP).addPos(0, 0, 0, 200, IType.SIN_DOWN));
        case JAMMED -> new BusAnimationSedna()
                .addBus("ROTATE", new BusAnimationSequenceSedna().addPos(0, 0, 0, 1300).addPos(60, 0, 0, 500, IType.SIN_FULL).addPos(60, 0, 0, 400).addPos(0, 0, 0, 500, IType.SIN_FULL))
                .addBus("TRANSLATE", new BusAnimationSequenceSedna().addPos(0, 0, 0, 500).addPos(0, -6, 0, 400, IType.SIN_FULL).addPos(0, -6, 0, 2000).addPos(0, 0, 0, 400, IType.SIN_FULL))
                .addBus("RECOIL", new BusAnimationSequenceSedna().addPos(0, 0, 0, 500).addPos(45, 0, 0, 400, IType.SIN_FULL).addPos(45, 0, 0, 2000).addPos(0, 0, 0, 400, IType.SIN_FULL));
        default -> null;
    };

    // CE: XFactoryCatapult.java
    public static final BiFunction<ItemStack, GunAnimationType, BusAnimationSedna> LAMBDA_FATMAN_ANIMS = (stack, type) -> {
        switch (type) {
            case EQUIP -> {
                return new BusAnimationSedna()
                        .addBus("EQUIP", new BusAnimationSequenceSedna().addPos(60, 0, 0, 0).addPos(0, 0, 0, 1000, IType.SIN_DOWN));
            }
            case CYCLE -> {
                java.util.Random rand = new java.util.Random();
                return new BusAnimationSedna()
                        .addBus("GAUGE", new BusAnimationSequenceSedna().addPos(0, 0, 135 + rand.nextInt(136), 100, IType.SIN_DOWN).addPos(0, 0, 0, 500, IType.SIN_DOWN))
                        .addBus("PISTON", new BusAnimationSequenceSedna().addPos(0, 0, 3, 100, IType.SIN_UP))
                        .addBus("NUKE", new BusAnimationSequenceSedna().addPos(0, 0, 3, 100, IType.SIN_UP).addPos(0, 0, 0, 0));
            }
            case RELOAD -> {
                return new BusAnimationSedna()
                        .addBus("LID", new BusAnimationSequenceSedna().addPos(0, 0, 0, 250).addPos(0, 0, -45, 250, IType.SIN_UP).addPos(0, 0, -45, 1200).addPos(0, 0, 0, 250, IType.SIN_UP))
                        .addBus("HANDLE", new BusAnimationSequenceSedna().addPos(0, 0, -2, 500, IType.SIN_FULL).addPos(0, 0, -2, 1700).addPos(0, 0, 0, 750, IType.SIN_FULL))
                        .addBus("NUKE", new BusAnimationSequenceSedna().addPos(5, -4, 3, 0).addPos(5, -4, 3, 750).addPos(2, 0.5, 3, 500, IType.SIN_UP).addPos(1, 0.5, 3, 100).addPos(0, 0, 3, 100).addPos(0, 0, 3, 750).addPos(0, 0, 0, 750, IType.SIN_FULL))
                        .addBus("PISTON", new BusAnimationSequenceSedna().addPos(0, 0, 3, 0).addPos(0, 0, 3, 2200).addPos(0, 0, 0, 750, IType.SIN_FULL))
                        .addBus("EQUIP", new BusAnimationSequenceSedna().addPos(5, 0, 0, 500, IType.SIN_FULL).addPos(0, 0, 0, 500, IType.SIN_FULL).addPos(0, 0, 0, 450).addPos(3, 0, 0, 100, IType.SIN_DOWN).addPos(0, 0, 0, 100, IType.SIN_FULL).addPos(0, 0, 0, 500).addPos(-10, 0, 0, 375, IType.SIN_DOWN).addPos(0, 0, 0, 375, IType.SIN_UP));
            }
            case JAMMED -> {
                return new BusAnimationSedna()
                        .addBus("HANDLE", new BusAnimationSequenceSedna().addPos(0, 0, 0, 750).addPos(0, 0, -2, 250, IType.SIN_FULL).addPos(0, 0, 0, 250, IType.SIN_FULL).addPos(0, 0, -2, 250, IType.SIN_FULL).addPos(0, 0, 0, 250, IType.SIN_FULL))
                        .addBus("EQUIP", new BusAnimationSequenceSedna().addPos(0, 0, 0, 500).addPos(-15, 0, 0, 250, IType.SIN_FULL).addPos(-15, 0, 0, 1000).addPos(0, 0, 0, 250, IType.SIN_FULL));
            }
            case INSPECT -> {
                return new BusAnimationSedna()
                        .addBus("HANDLE", new BusAnimationSequenceSedna().addPos(0, 0, 0, 250).addPos(0, 0, -2, 250, IType.SIN_FULL).addPos(0, 0, 0, 250, IType.SIN_FULL).addPos(0, 0, -2, 250, IType.SIN_FULL).addPos(0, 0, 0, 250, IType.SIN_FULL))
                        .addBus("EQUIP", new BusAnimationSequenceSedna().addPos(-15, 0, 0, 250, IType.SIN_FULL).addPos(-15, 0, 0, 1000).addPos(0, 0, 0, 250, IType.SIN_FULL));
            }
        }
        return null;
    };

    // CE: XFactoryRocket.java
    public static final BiFunction<ItemStack, GunAnimationType, BusAnimationSedna> LAMBDA_PANZERSCHRECK_ANIMS = (stack, type) -> {
        boolean empty = ((ItemGunBaseNT) stack.getItem()).getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack).getAmount(stack, Minecraft.getInstance().player == null ? null : Minecraft.getInstance().player.getInventory()) <= 0;
        switch(type) {
            case EQUIP: return new BusAnimationSedna()
                    .addBus("EQUIP", new BusAnimationSequenceSedna().addPos(60, 0, 0, 0).addPos(0, 0, 0, 500, IType.SIN_DOWN));
            case RELOAD: return new BusAnimationSedna()
                    .addBus("RELOAD", new BusAnimationSequenceSedna().addPos(90, 0, 0, 750, IType.SIN_FULL).addPos(90, 0, 0, 1000).addPos(0, 0, 0, 750, IType.SIN_FULL))
                    .addBus("ROCKET", new BusAnimationSequenceSedna().addPos(0, -3, -6, 0).addPos(0, -3, -6, 750).addPos(0, 0, -6.5, 500, IType.SIN_DOWN).addPos(0, 0, 0, 350, IType.SIN_UP));
            case JAMMED: empty = false;
            case INSPECT:
                return new BusAnimationSedna()
                        .addBus("RELOAD", new BusAnimationSequenceSedna().addPos(90, 0, 0, 750, IType.SIN_FULL).addPos(90, 0, 0, 500).addPos(0, 0, 0, 750, IType.SIN_FULL))
                        .addBus("ROCKET", new BusAnimationSequenceSedna().addPos(0, empty ? -3 : 0, 0, 0));
        }
        return null;
    };

    // CE: XFactoryRocket.java
    public static final BiFunction<ItemStack, GunAnimationType, BusAnimationSedna> LAMBDA_QUADRO_ANIMS = (stack, type) -> switch (type) {
        case EQUIP -> new BusAnimationSedna()
                .addBus("EQUIP", new BusAnimationSequenceSedna().addPos(60, 0, 0, 0).addPos(0, 0, 0, 500, IType.SIN_DOWN));
        case CYCLE -> new BusAnimationSedna()
                .addBus("RECOIL", new BusAnimationSequenceSedna().addPos(0, 0, -0.5, 50).addPos(0, 0, 0, 50));
        case RELOAD -> new BusAnimationSedna()
                .addBus("RELOAD_ROTATE", new BusAnimationSequenceSedna().addPos(0, 0, 60, 500, IType.SIN_FULL).addPos(0, 0, 60, 1500).addPos(0, 0, 0, 750, IType.SIN_FULL))
                .addBus("RELOAD_PUSH", new BusAnimationSequenceSedna().addPos(-1, -1, 0, 0).addPos(-1, -1, 0, 500).addPos(-1, 0, 0, 350).addPos(0, 0, 0, 1000));
        case JAMMED, INSPECT -> new BusAnimationSedna()
                .addBus("RELOAD_ROTATE", new BusAnimationSequenceSedna().addPos(0, 0, 60, 750, IType.SIN_FULL).addPos(0, 0, 60, 500).addPos(0, 0, 0, 750, IType.SIN_FULL));
        default -> null;
    };

    // CE: XFactoryRocket.java
    public static final BiFunction<ItemStack, GunAnimationType, BusAnimationSedna> LAMBDA_MISSILE_LAUNCHER_ANIMS = (stack, type) -> switch (type) {
        case EQUIP -> new BusAnimationSedna()
                .addBus("EQUIP", new BusAnimationSequenceSedna().addPos(60, 0, 0, 0).addPos(0, 0, 0, 1000, IType.SIN_DOWN));
        case RELOAD -> new BusAnimationSedna()
                .addBus("BARREL", new BusAnimationSequenceSedna().addPos(0, 0, 1.5, 150).addPos(0, 0, 1.5, 2100).addPos(0, 0, 0, 150))
                .addBus("OPEN", new BusAnimationSequenceSedna().addPos(0, 0, 0, 250).addPos(90, 0, 0, 500, IType.SIN_FULL).addPos(90, 0, 0, 1000).addPos(0, 0, 0, 500, IType.SIN_FULL))
                .addBus("EQUIP", new BusAnimationSequenceSedna().addPos(0, 0, 0, 2250).addPos(-1, 0, 0, 150, IType.SIN_DOWN).addPos(0, 0, 0, 150, IType.SIN_UP))
                .addBus("MISSILE", new BusAnimationSequenceSedna().addPos(-10, 0, 0, 0).addPos(-10, 0, 0, 750).addPos(3, 0, 2, 0).addPos(0, 0, -6, 350, IType.SIN_FULL).addPos(0, 0, 0, 350, IType.SIN_UP));
        case JAMMED, INSPECT -> new BusAnimationSedna()
                .addBus("BARREL", new BusAnimationSequenceSedna().addPos(0, 0, 1.5, 150).addPos(0, 0, 1.5, 1350).addPos(0, 0, 0, 150))
                .addBus("OPEN", new BusAnimationSequenceSedna().addPos(0, 0, 0, 250).addPos(90, 0, 0, 500, IType.SIN_FULL).addPos(90, 0, 0, 250).addPos(0, 0, 0, 500, IType.SIN_FULL))
                .addBus("EQUIP", new BusAnimationSequenceSedna().addPos(0, 0, 0, 1500).addPos(-1, 0, 0, 150, IType.SIN_DOWN).addPos(0, 0, 0, 150, IType.SIN_UP));
        default -> null;
    };

    // CE: XFactory75Bolt.java
    public static final BiFunction<ItemStack, GunAnimationType, BusAnimationSedna> LAMBDA_BOLTER_ANIMS = (stack, type) -> switch (type) {
        case CYCLE -> new BusAnimationSedna()
                .addBus("RECOIL", new BusAnimationSequenceSedna().addPos(1, 0, 0, 25).addPos(0, 0, 0, 75));
        case RELOAD -> new BusAnimationSedna()
                .addBus("TILT", new BusAnimationSequenceSedna().addPos(1, 0, 0, 250).addPos(1, 0, 0, 1500).addPos(0, 0, 0, 250))
                .addBus("MAG", new BusAnimationSequenceSedna().addPos(0, 0, 1, 500).addPos(1, 0, 1, 500).addPos(0, 0, 0, 500));
        case JAMMED -> new BusAnimationSedna()
                .addBus("TILT", new BusAnimationSequenceSedna().addPos(0, 0, 0, 500).addPos(1, 0, 0, 250).addPos(1, 0, 0, 700).addPos(0, 0, 0, 250))
                .addBus("MAG", new BusAnimationSequenceSedna().addPos(0, 0, 0, 750).addPos(0.6, 0, 0, 250).addPos(0, 0, 0, 250));
        default -> null;
    };

    // CE: XFactory40mm.java
    public static final BiFunction<ItemStack, GunAnimationType, BusAnimationSedna> LAMBDA_FLAREGUN_ANIMS = (stack, type) -> switch (type) {
        case EQUIP -> new BusAnimationSedna()
                .addBus("EQUIP", new BusAnimationSequenceSedna().addPos(-90, 0, 0, 0).addPos(0, 0, 0, 350, IType.SIN_DOWN));
        case CYCLE -> new BusAnimationSedna()
                .addBus("RECOIL", new BusAnimationSequenceSedna().addPos(0, 0, 0, 50).addPos(0, 0, -3, 50).addPos(0, 0, 0, 250))
                .addBus("HAMMER", new BusAnimationSequenceSedna().addPos(15, 0, 0, 50).addPos(15, 0, 0, 550).addPos(0, 0, 0, 100));
        case CYCLE_DRY -> new BusAnimationSedna()
                .addBus("HAMMER", new BusAnimationSequenceSedna().addPos(15, 0, 0, 50).addPos(15, 0, 0, 550).addPos(0, 0, 0, 100));
        case RELOAD -> new BusAnimationSedna()
                .addBus("OPEN", new BusAnimationSequenceSedna().addPos(45, 0, 0, 200, IType.SIN_FULL).addPos(45, 0, 0, 750).addPos(0, 0, 0, 200, IType.SIN_UP))
                .addBus("SHELL", new BusAnimationSequenceSedna().addPos(4, -8, -4, 0).addPos(4, -8, -4, 200).addPos(0, 0, -5, 500, IType.SIN_DOWN).addPos(0, 0, 0, 200, IType.SIN_UP))
                .addBus("FLIP", new BusAnimationSequenceSedna().addPos(0, 0, 0, 200).addPos(25, 0, 0, 200, IType.SIN_DOWN).addPos(25, 0, 0, 800).addPos(0, 0, 0, 200, IType.SIN_DOWN));
        case JAMMED -> new BusAnimationSedna()
                .addBus("OPEN", new BusAnimationSequenceSedna().addPos(0, 0, 0, 500).addPos(45, 0, 0, 200, IType.SIN_FULL).addPos(45, 0, 0, 500).addPos(0, 0, 0, 200, IType.SIN_UP))
                .addBus("FLIP", new BusAnimationSequenceSedna().addPos(0, 0, 0, 500).addPos(0, 0, 0, 200).addPos(25, 0, 0, 200, IType.SIN_DOWN).addPos(25, 0, 0, 550).addPos(0, 0, 0, 200, IType.SIN_DOWN));
        case INSPECT -> new BusAnimationSedna()
                .addBus("FLIP", new BusAnimationSequenceSedna().addPos(-360 * 3, 0, 0, 1500, IType.SIN_FULL));
        default -> null;
    };

    // CE: XFactory40mm.java
    public static final BiFunction<ItemStack, GunAnimationType, BusAnimationSedna> LAMBDA_CONGOLAKE_ANIMS = (stack, type) -> {
        int ammo = ((ItemGunBaseNT) stack.getItem()).getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack).getAmount(stack, Minecraft.getInstance().player == null ? null : Minecraft.getInstance().player.getInventory());
        return switch (type) {
            case EQUIP -> GunModels.json("congolake_anim").get("Equip");
            case CYCLE -> GunModels.json("congolake_anim").get(ammo <= 1 ? "FireEmpty" : "Fire");
            case RELOAD -> GunModels.json("congolake_anim").get(ammo == 0 ? "ReloadEmpty" : "ReloadStart");
            case RELOAD_CYCLE -> GunModels.json("congolake_anim").get("Reload");
            case RELOAD_END -> GunModels.json("congolake_anim").get("ReloadEnd");
            case JAMMED -> GunModels.json("congolake_anim").get("Jammed");
            case INSPECT -> GunModels.json("congolake_anim").get("Inspect");
            default -> null;
        };

    };

    // CE: XFactory40mm.java
    public static final BiFunction<ItemStack, GunAnimationType, BusAnimationSedna> LAMBDA_MK108_ANIMS = (stack, type) -> {
        switch (type) {
            case EQUIP: return new BusAnimationSedna()
                    .addBus("EQUIP", new BusAnimationSequenceSedna().setPos(45, 0, 0).addPos(0, 0, 0, 1000, IType.SIN_DOWN));
            case CYCLE:
                int amount = ((ItemGunBaseNT) stack.getItem()).getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack).getAmount(stack, null);
                return new BusAnimationSedna()
                        .addBus("RECOIL", new BusAnimationSequenceSedna().hold(50).addPos(0, 0, -0.25, 100, IType.SIN_DOWN).addPos(0, 0, 0, 150, IType.SIN_FULL))
                        .addBus("BARREL", new BusAnimationSequenceSedna().addPos(0, 0, -1, 100, IType.SIN_DOWN).addPos(0, 0, 0, 250, IType.SIN_FULL))
                        .addBus("CYCLE", new BusAnimationSequenceSedna().addPos(0, 0, 0, 100).addPos(1, 0, 0, 150))
                        .addBus("SHELLS", new BusAnimationSequenceSedna().setPos(amount - 1, 0, 0));
            case CYCLE_DRY: return new BusAnimationSedna()
                    .addBus("HAMMER", new BusAnimationSequenceSedna().addPos(15, 0, 0, 50).addPos(15, 0, 0, 550).addPos(0, 0, 0, 100));
            case RELOAD: return new BusAnimationSedna()
                    .addBus("LIFT", new BusAnimationSequenceSedna().addPos(10, 0, 0, 500, IType.SIN_FULL).holdUntil(1250).addPos(-50, 0, 0, 750, IType.SIN_FULL).holdUntil(5500).addPos(0, 0, 0, 500, IType.SIN_FULL).hold(500).addPos(1, 0, 0, 100, IType.SIN_UP).addPos(0, 0, 0, 150, IType.SIN_FULL))
                    .addBus("LID", new BusAnimationSequenceSedna().addPos(60, 0, 0, 500, IType.SIN_FULL).holdUntil(6000).addPos(0, 0, 0, 500, IType.SIN_UP))
                    .addBus("BELT", new BusAnimationSequenceSedna().setPos(1, 0, 0).hold(500).addPos(0, 0, 0, 750, IType.SIN_UP).holdUntil(4500).addPos(1, 0, 0, 750, IType.SIN_UP))
                    .addBus("DRUM", new BusAnimationSequenceSedna().hold(2000).addPos(2.5, 0, 0, 500, IType.SIN_DOWN).addPos(2.5, -2, -8, 500, IType.SIN_UP).setPos(4, -3, -8).addPos(2.5, 0, 0, 1000, IType.SIN_FULL).addPos(0, 0, 0, 500, IType.SIN_UP));
            case JAMMED: return new BusAnimationSedna()
                    .addBus("LID", new BusAnimationSequenceSedna().hold(250).addPos(45, 0, 0, 500, IType.SIN_FULL).addPos(0, 0, 0, 250, IType.SIN_UP))
                    .addBus("LIFT", new BusAnimationSequenceSedna().hold(1000).addPos(1, 0, 0, 100, IType.SIN_UP).addPos(0, 0, 0, 150, IType.SIN_FULL));
            case INSPECT:
                int yeetHorizontal = 750;
                int untilImpact = yeetHorizontal * 9 / 15;
                int delay = 250;
                int height = 6;
                int arcUp = untilImpact * 5 / 8;
                int arcDown = untilImpact * 3 / 8;
                return new BusAnimationSedna()
                        .addBus("LIFT", new BusAnimationSequenceSedna().hold(untilImpact).addPos(1, 0, 0, 50, IType.SIN_UP).addPos(0, 0, 0, 100, IType.SIN_FULL).hold(delay - 150).addPos(1, 0, 0, 50, IType.SIN_UP).addPos(0, 0, 0, 100, IType.SIN_FULL).hold(delay - 150).addPos(1, 0, 0, 50, IType.SIN_UP).addPos(0, 0, 0, 100, IType.SIN_FULL))
                        .addBus("GRENH1", new BusAnimationSequenceSedna().setPos(9, 0, 0).addPos(-6, 0, 0, yeetHorizontal))
                        .addBus("GRENV1", new BusAnimationSequenceSedna().setPos(0, -2, 0).addPos(0, height, 0, arcUp, IType.SIN_DOWN).addPos(0, 2, 0, arcDown, IType.SIN_UP).addPos(0, 3, 0, yeetHorizontal - untilImpact, IType.SIN_DOWN))
                        .addBus("GRENS1", new BusAnimationSequenceSedna().addPos(360 * 2, 0, 0, untilImpact).setPos(0, 0, 0).addPos(360 * 1, 0, 0, yeetHorizontal - untilImpact))
                        .addBus("GRENH2", new BusAnimationSequenceSedna().setPos(9, 0, 0).hold(delay).addPos(-6, 0, 0, yeetHorizontal))
                        .addBus("GRENV2", new BusAnimationSequenceSedna().setPos(0, -2, 0).hold(delay).addPos(0, height, 0, arcUp, IType.SIN_DOWN).addPos(0, 2, 0, arcDown, IType.SIN_UP).addPos(0, 3, 0, yeetHorizontal - untilImpact, IType.SIN_DOWN))
                        .addBus("GRENS2", new BusAnimationSequenceSedna().hold(delay).addPos(360 * 2, 0, 0, untilImpact).setPos(0, 0, 0).addPos(360 * 1, 0, 0, yeetHorizontal - untilImpact))
                        .addBus("GRENH3", new BusAnimationSequenceSedna().setPos(9, 0, 0).hold(delay * 2).addPos(-6, 0, 0, yeetHorizontal))
                        .addBus("GRENV3", new BusAnimationSequenceSedna().setPos(0, -2, 0).hold(delay * 2).addPos(0, height, 0, arcUp, IType.SIN_DOWN).addPos(0, 2, 0, arcDown, IType.SIN_UP).addPos(0, 3, 0, yeetHorizontal - untilImpact, IType.SIN_DOWN))
                        .addBus("GRENS3", new BusAnimationSequenceSedna().hold(delay * 2).addPos(360 * 2, 0, 0, untilImpact).setPos(0, 0, 0).addPos(360 * 1, 0, 0, yeetHorizontal - untilImpact));
        }
        return null;
    };

    // CE: XFactory357.java
    public static final BiFunction<ItemStack, GunAnimationType, BusAnimationSedna> LAMBDA_ATLAS_ANIMS = (stack, type) -> switch (type) {
        case EQUIP -> new BusAnimationSedna()
                .addBus("EQUIP", new BusAnimationSequenceSedna().addPos(-90, 0, 0, 0).addPos(0, 0, 0, 350, IType.SIN_DOWN));
        case CYCLE -> new BusAnimationSedna()
                .addBus("RECOIL", new BusAnimationSequenceSedna().addPos(0, 0, 0, 50).addPos(0, 0, -3, 50).addPos(0, 0, 0, 250))
                .addBus("HAMMER", new BusAnimationSequenceSedna().addPos(0, 0, 1, 50).addPos(0, 0, 1, 550).addPos(0, 0, 0, 200))
                .addBus("DRUM", new BusAnimationSequenceSedna().addPos(0, 0, 0, 600).addPos(0, 0, 1, 200));
        case CYCLE_DRY -> new BusAnimationSedna()
                .addBus("HAMMER", new BusAnimationSequenceSedna().addPos(0, 0, 1, 50).addPos(0, 0, 1, 550).addPos(0, 0, 0, 200))
                .addBus("DRUM", new BusAnimationSequenceSedna().addPos(0, 0, 0, 600).addPos(0, 0, 1, 200));
        case RELOAD -> new BusAnimationSedna()
                .addBus("LATCH", new BusAnimationSequenceSedna().addPos(0, 0, 90, 300).addPos(0, 0, 90, 2000).addPos(0, 0, 0, 150))
                .addBus("FRONT", new BusAnimationSequenceSedna().addPos(0, 0, 0, 200).addPos(0, 0, 45, 150).addPos(0, 0, 45, 2000).addPos(0, 0, 0, 75))
                .addBus("RELOAD_ROT", new BusAnimationSequenceSedna().addPos(0, 0, 0, 300).addPos(60, 0, 0, 500).addPos(60, 0, 0, 500).addPos(0, -90, -90, 0).addPos(0, -90, -90, 600).addPos(0, 0, 0, 300).addPos(0, 0, 0, 100).addPos(-45, 0, 0, 50).addPos(-45, 0, 0, 100).addPos(0, 0, 0, 300))
                .addBus("RELOAD_MOVE", new BusAnimationSequenceSedna().addPos(0, 0, 0, 300).addPos(0, -15, 0, 1000).addPos(0, 0, 0, 450))
                .addBus("DRUM_PUSH", new BusAnimationSequenceSedna().addPos(0, 0, 0, 1600).addPos(0, 0, -5, 0).addPos(0, 0, 0, 300));
        case INSPECT -> new BusAnimationSedna()
                .addBus("LATCH", new BusAnimationSequenceSedna().addPos(0, 0, 90, 300).addPos(0, 0, 90, 1000).addPos(0, 0, 0, 150))
                .addBus("FRONT", new BusAnimationSequenceSedna().addPos(0, 0, 0, 200).addPos(0, 0, 45, 150).addPos(0, 0, 45, 1000).addPos(0, 0, 0, 75))
                .addBus("RELOAD_ROT", new BusAnimationSequenceSedna().addPos(0, 0, 0, 300).addPos(45, 0, 0, 500, IType.SIN_FULL).addPos(45, 0, 0, 500).addPos(-45, 0, 0, 50).addPos(-45, 0, 0, 100).addPos(0, 0, 0, 300))
                .addBus("RELOAD_MOVE", new BusAnimationSequenceSedna().addPos(0, 0, 0, 300).addPos(0, -2.5, 0, 500, IType.SIN_FULL).addPos(0, -2.5, 0, 500).addPos(0, 0, 0, 350));
        case JAMMED -> new BusAnimationSedna()
                .addBus("LATCH", new BusAnimationSequenceSedna().addPos(0, 0, 0, 500).addPos(0, 0, 90, 300).addPos(0, 0, 90, 1000).addPos(0, 0, 0, 150))
                .addBus("FRONT", new BusAnimationSequenceSedna().addPos(0, 0, 0, 500).addPos(0, 0, 0, 200).addPos(0, 0, 45, 150).addPos(0, 0, 45, 1000).addPos(0, 0, 0, 75))
                .addBus("RELOAD_ROT", new BusAnimationSequenceSedna().addPos(0, 0, 0, 500).addPos(0, 0, 0, 300).addPos(45, 0, 0, 500, IType.SIN_FULL).addPos(45, 0, 0, 500).addPos(-45, 0, 0, 50).addPos(-45, 0, 0, 100).addPos(0, 0, 0, 300))
                .addBus("RELOAD_MOVE", new BusAnimationSequenceSedna().addPos(0, 0, 0, 500).addPos(0, 0, 0, 300).addPos(0, -2.5, 0, 500, IType.SIN_FULL).addPos(0, -2.5, 0, 500).addPos(0, 0, 0, 350));
        default -> null;
    };

    // CE: XFactory357.java
    public static final BiFunction<ItemStack, GunAnimationType, BusAnimationSedna> LAMBDA_DANI_ANIMS = (stack, type) -> switch (type) {
        case EQUIP ->
                new BusAnimationSedna().addBus("EQUIP", new BusAnimationSequenceSedna().addPos(360 * 3, 0, 0, 1000, IType.SIN_DOWN));
        case CYCLE -> new BusAnimationSedna()
                .addBus("RECOIL", new BusAnimationSequenceSedna().addPos(0, 0, 0, 50).addPos(0, 0, -3, 50).addPos(0, 0, 0, 250))
                .addBus("HAMMER", new BusAnimationSequenceSedna().addPos(0, 0, 1, 50).addPos(0, 0, 1, 300).addPos(0, 0, 0, 200))
                .addBus("DRUM", new BusAnimationSequenceSedna().addPos(0, 0, 0, 350).addPos(0, 0, 1, 200));
        case CYCLE_DRY -> new BusAnimationSedna()
                .addBus("HAMMER", new BusAnimationSequenceSedna().addPos(0, 0, 1, 50).addPos(0, 0, 1, 200).addPos(0, 0, 0, 200))
                .addBus("DRUM", new BusAnimationSequenceSedna().addPos(0, 0, 0, 350).addPos(0, 0, 1, 200));
        default -> LAMBDA_ATLAS_ANIMS.apply(stack, type);
    };

    // CE: XFactoryFolly.java
    public static final BiFunction<ItemStack, GunAnimationType, BusAnimationSedna> LAMBDA_FOLLY_ANIMS = (stack, type) -> switch (type) {
        case EQUIP -> new BusAnimationSedna()
                .addBus("EQUIP", new BusAnimationSequenceSedna().addPos(-60, 0, 0, 0).addPos(5, 0, 0, 1500, IType.SIN_DOWN).addPos(0, 0, 0, 500, IType.SIN_FULL));
        case CYCLE -> new BusAnimationSedna()
                .addBus("RECOIL", new BusAnimationSequenceSedna().addPos(0, 0, -4.5, 50).addPos(0, 0, -4.5, 500).addPos(0, 0, 0, 500, IType.SIN_UP))
                .addBus("LOAD", new BusAnimationSequenceSedna().addPos(0, 0, 0, 50).addPos(-25, 0, 0, 250, IType.SIN_DOWN).addPos(0, 0, 0, 1000, IType.SIN_FULL));
        case RELOAD -> new BusAnimationSedna()
                .addBus("LOAD", new BusAnimationSequenceSedna().addPos(60, 0, 0, 1000, IType.SIN_FULL).addPos(60, 0, 0, 6000).addPos(0, 0, 0, 1000, IType.SIN_FULL))
                .addBus("SCREW", new BusAnimationSequenceSedna().addPos(0, 0, 0, 1000).addPos(0, 0, -135, 1000, IType.SIN_FULL).addPos(0, 0, -135, 4000).addPos(0, 0, 0, 1000, IType.SIN_FULL))
                .addBus("BREECH", new BusAnimationSequenceSedna().addPos(0, 0, 0, 1000).addPos(0, 0, -0.5, 1000, IType.SIN_FULL).addPos(0, -4, -0.5, 1000, IType.SIN_FULL).addPos(0, -4, -0.5, 2000).addPos(0, 0, -0.5, 1000, IType.SIN_FULL).addPos(0, 0, 0, 1000, IType.SIN_FULL))
                .addBus("SHELL", new BusAnimationSequenceSedna().addPos(0, -4, -4.5, 0).addPos(0, -4, -4.5, 3000).addPos(0, 0, -4.5, 1000, IType.SIN_FULL).addPos(0, 0, 0, 500, IType.SIN_UP));
        default -> null;
    };

    // CE: XFactory762mm.java
    public static final BiFunction<ItemStack, GunAnimationType, BusAnimationSedna> LAMBDA_CARBINE_ANIMS = (stack, type) -> {
        boolean empty = ((ItemGunBaseNT) stack.getItem()).getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack).getAmount(stack, Minecraft.getInstance().player == null ? null : Minecraft.getInstance().player.getInventory()) <= 0;
        switch(type) {
            case EQUIP: return new BusAnimationSedna()
                    .addBus("EQUIP", new BusAnimationSequenceSedna().addPos(45, 0, 0, 0).addPos(0, 0, 0, 500, IType.SIN_FULL));
            case CYCLE: return new BusAnimationSedna()
                    .addBus("RECOIL", new BusAnimationSequenceSedna().addPos(0, 0, ItemGunBaseNT.getIsAiming(stack) ? -0.25 : -0.5, 50, IType.SIN_DOWN).addPos(0, 0, 0, 100, IType.SIN_FULL))
                    .addBus("SLIDE", new BusAnimationSequenceSedna().addPos(0, 0, -1, 50, IType.SIN_DOWN).addPos(0, 0, 0, 100, IType.SIN_UP))
                    .addBus(empty ? "NULL" : "REL", new BusAnimationSequenceSedna().addPos(0, 0, 0.25, 50).addPos(0, 0.125, 1.25, 100, IType.SIN_UP));
            case CYCLE_DRY: return new BusAnimationSedna()
                    .addBus("SLIDE", new BusAnimationSequenceSedna().addPos(0, 0, 0, 500).addPos(0, 0, -1, 100, IType.SIN_DOWN).addPos(0, 0, -1, 50).addPos(0, 0, 0, 100, IType.SIN_UP));
            case RELOAD: return new BusAnimationSedna()
                    .addBus("MAG", new BusAnimationSequenceSedna().addPos(0, -4, 0, 250, IType.SIN_UP).addPos(0, -4, 0, 750).addPos(0, 0, 0, 500, IType.SIN_DOWN))
                    .addBus("LIFT", new BusAnimationSequenceSedna().addPos(0, 0, 0, 500).addPos(-25, 0, 0, 250, IType.SIN_FULL).addPos(-25, 0, 0, 1000))
                    .addBus("BULLET", new BusAnimationSequenceSedna().addPos(empty ? 1 : 0, 0, 0, 0).addPos(0, 0, 0, 1000));
            case RELOAD_END: return new BusAnimationSedna()
                    .addBus("LIFT", new BusAnimationSequenceSedna().addPos(-25, 0, 0, 0).addPos(-25, 0, 0, 750).addPos(0, 0, 0, 500, IType.SIN_FULL))
                    .addBus("SLIDE", new BusAnimationSequenceSedna().addPos(0, 0, 0, 250).addPos(0, 0, -1, 100, IType.SIN_DOWN).addPos(0, 0, -1, 50).addPos(0, 0, 0, 100, IType.SIN_UP))
                    .addBus("REL", new BusAnimationSequenceSedna().addPos(0, 0, 0, 250).addPos(0, 0, 0.25, 150).addPos(0, 0.125, 1.25, 100, IType.SIN_UP));
            case JAMMED: return new BusAnimationSedna()
                    .addBus("LIFT", new BusAnimationSequenceSedna().addPos(-25, 0, 0, 0).addPos(-25, 0, 0, 750).addPos(0, 0, 0, 500, IType.SIN_FULL).addPos(0, 0, 0, 250).addPos(-25, 0, 0, 250, IType.SIN_FULL).addPos(-25, 0, 0, 750).addPos(0, 0, 0, 500, IType.SIN_FULL))
                    .addBus("SLIDE", new BusAnimationSequenceSedna().addPos(0, 0, 0, 250).addPos(0, 0, -1, 100, IType.SIN_DOWN).addPos(0, 0, -1, 50).addPos(0, 0, -0.25, 100, IType.SIN_UP).addPos(0, 0, -0.25, 1250).addPos(0, 0, -1, 100, IType.SIN_DOWN).addPos(0, 0, -1, 50).addPos(0, 0, 0, 100, IType.SIN_UP))
                    .addBus("REL", new BusAnimationSequenceSedna().addPos(0, 0, 0, 250).addPos(0, 0, 0.25, 150).addPos(0, 0.125, 1, 100, IType.SIN_UP).addPos(0, 0.125, 1, 1250).addPos(0, 0.125, 0.25, 100, IType.SIN_DOWN).addPos(0, 0.125, 1, 100, IType.SIN_UP));
            case INSPECT: return new BusAnimationSedna()
                    .addBus("LIFT", new BusAnimationSequenceSedna().addPos(-25, 0, 0, 250, IType.SIN_FULL).addPos(-25, 0, 0, 1500).addPos(0, 0, 0, 500, IType.SIN_FULL))
                    .addBus("SLIDE", new BusAnimationSequenceSedna().addPos(0, 0, 0, 500).addPos(0, 0, -0.75, 150, IType.SIN_DOWN).addPos(0, 0, -0.75, 1000).addPos(0, 0, 0, 100, IType.SIN_UP))
                    .addBus(empty ? "NULL" : "REL", new BusAnimationSequenceSedna().addPos(0, 0.125, 1.25, 0).addPos(0, 0.125, 1.25, 500).addPos(0, 0.125, 0.5, 150, IType.SIN_DOWN).addPos(0, 0.125, 0.5, 1000).addPos(0, 0.125, 1.25, 100, IType.SIN_UP));
        }

        return null;
    };

    // CE: XFactory762mm.java
    public static final BiFunction<ItemStack, GunAnimationType, BusAnimationSedna> LAMBDA_MINIGUN_ANIMS = (stack, type) -> {
        switch(type) {
            case EQUIP: return new BusAnimationSedna()
                    .addBus("EQUIP", new BusAnimationSequenceSedna().addPos(45, 0, 0, 0).addPos(0, 0, 0, 1000, IType.SIN_FULL));
            case CYCLE: return new BusAnimationSedna()
                    .addBus("RECOIL", new BusAnimationSequenceSedna().addPos(0, 0, ItemGunBaseNT.getIsAiming(stack) ? -0.25 : -0.5, 0).addPos(0, 0, ItemGunBaseNT.getIsAiming(stack) ? -0.25 : -0.5, 100).addPos(0, 0, 0, 150, IType.SIN_FULL))
                    .addBus("ROTATE", new BusAnimationSequenceSedna().addPos(0, 0, 60, 50).addPos(0, 0, 720, 1000, IType.SIN_DOWN));
            case CYCLE_DRY: return new BusAnimationSedna()
                    .addBus("ROTATE", new BusAnimationSequenceSedna().addPos(0, 0, 60, 50).addPos(0, 0, 720, 1000, IType.SIN_DOWN));
            case RELOAD: return new BusAnimationSedna()
                    .addBus("EQUIP", new BusAnimationSequenceSedna().addPos(-15, 0, 0, 250, IType.SIN_DOWN).addPos(0, 0, 0, 500, IType.SIN_FULL))
                    .addBus("ROTATE", new BusAnimationSequenceSedna().addPos(0, 0, 60, 50).addPos(0, 0, 720, 1000, IType.SIN_DOWN));
            case INSPECT: return new BusAnimationSedna()
                    .addBus("EQUIP", new BusAnimationSequenceSedna().addPos(3, 0, 0, 150, IType.SIN_DOWN).addPos(0, 0, 0, 100, IType.SIN_FULL))
                    .addBus("ROTATE", new BusAnimationSequenceSedna().addPos(0, 0, -720, 1000, IType.SIN_DOWN));
        }

        return null;
    };

    // CE: XFactory762mm.java
    public static final BiFunction<ItemStack, GunAnimationType, BusAnimationSedna> LAMBDA_MAS36_ANIMS = (stack, type) -> {
        int mag = ((ItemGunBaseNT) stack.getItem()).getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack).getAmount(stack, Minecraft.getInstance().player == null ? null : Minecraft.getInstance().player.getInventory());
        double turn = -90;
        double pullAmount = ItemGunBaseNT.getIsAiming(stack) ? -1F : -1.5D;
        switch(type) {
            case EQUIP: return new BusAnimationSedna()
                    .addBus("STOCK", new BusAnimationSequenceSedna().setPos(-158, 0, 0).hold(500).addPos(0, 0, 0, 500, IType.SIN_FULL))
                    .addBus("EQUIP", new BusAnimationSequenceSedna().setPos(45, 0, 0).addPos(0, 0, 0, 500, IType.SIN_FULL).hold(500).addPos(1, 0, 0, 100, IType.SIN_DOWN).addPos(0, 0, 0, 100, IType.SIN_FULL));
            case CYCLE: return new BusAnimationSedna()
                    .addBus("RECOIL", new BusAnimationSequenceSedna().addPos(0, 0, -0.5, 50, IType.SIN_DOWN).addPos(0, 0, 0, 100, IType.SIN_FULL))
                    .addBus("BOLT_TURN", new BusAnimationSequenceSedna().hold(250).addPos(0, 0, turn, 150).hold(700).addPos(0, 0, 0, 150))
                    .addBus("BOLT_PULL", new BusAnimationSequenceSedna().hold(350).addPos(0, 0, pullAmount, 250, IType.SIN_UP).hold(250).addPos(0, 0, 0, 200, IType.LINEAR))
                    .addBus("LIFT", new BusAnimationSequenceSedna().hold(600).addPos(-3, 0, 0, 150, IType.SIN_DOWN).hold(300).addPos(0, 0, 0, 250, IType.SIN_FULL))
                    .addBus("BULLET", mag <= 1 ? new BusAnimationSequenceSedna().setPos(-100, 0, 0) : new BusAnimationSequenceSedna().hold(850).addPos(0, 0.1875, 1.5, 200, IType.LINEAR));
            case CYCLE_DRY: return new BusAnimationSedna()
                    .addBus("BOLT_TURN", new BusAnimationSequenceSedna().hold(250).addPos(0, 0, turn, 150).hold(700).addPos(0, 0, 0, 150))
                    .addBus("BOLT_PULL", new BusAnimationSequenceSedna().hold(350).addPos(0, 0, pullAmount, 250, IType.SIN_UP).hold(250).addPos(0, 0, 0, 200, IType.LINEAR))
                    .addBus("LIFT", new BusAnimationSequenceSedna().hold(600).addPos(-3, 0, 0, 150, IType.SIN_DOWN).hold(300).addPos(0, 0, 0, 250, IType.SIN_FULL))
                    .addBus("BULLET", new BusAnimationSequenceSedna().setPos(-100, 0, 0));
            case RELOAD: return new BusAnimationSedna()
                    .addBus("BOLT_TURN", new BusAnimationSequenceSedna().addPos(0, 0, turn, 150).holdUntil(2000).addPos(0, 0, 0, 150))
                    .addBus("BOLT_PULL", new BusAnimationSequenceSedna().hold(100).addPos(0, 0, -1.5D, 250, IType.SIN_UP).holdUntil(1800).addPos(0, 0, 0, 200, IType.LINEAR))
                    .addBus("BULLET", new BusAnimationSequenceSedna().setPos(-100, 0, 0).holdUntil(1200).setPos(0, 0, 0).hold(600).addPos(0, 0.1875, 1.5, 200, IType.LINEAR))
                    .addBus("LIFT", new BusAnimationSequenceSedna().hold(200).addPos(30, 0, 0, 500, IType.SIN_FULL).holdUntil(1200).addPos(0, 0, 0, 500, IType.SIN_FULL))
                    .addBus("SHOW_CLIP", new BusAnimationSequenceSedna().setPos(1, 1, 1))
                    .addBus("CLIP", new BusAnimationSequenceSedna().setPos(2, -3, 0).hold(250).addPos(0.5, 1, 0, 500, IType.SIN_DOWN).addPos(0, 0, 0, 250, IType.SIN_FULL).hold(400).addPos(-0.5, 0.5, 0, 150).addPos(-3, -3, 0, 250, IType.SIN_UP))
                    .addBus("BULLETS", new BusAnimationSequenceSedna().setPos(2, -4, 0).hold(250).addPos(0.5, 1, 0, 500, IType.SIN_DOWN).addPos(0, 0, 0, 250, IType.SIN_FULL).hold(150).addPos(0, -1.5, 0, 250, IType.SIN_DOWN));
            case JAMMED: return new BusAnimationSedna()
                    .addBus("LIFT", new BusAnimationSequenceSedna().hold(250).addPos(-15, 0, 0, 500, IType.SIN_FULL).holdUntil(1650).addPos(0, 0, 0, 500, IType.SIN_FULL))
                    .addBus("BOLT_TURN", new BusAnimationSequenceSedna().hold(250).addPos(0, 0, turn, 150).holdUntil(1250).addPos(0, 0, 0, 150))
                    .addBus("BOLT_PULL", new BusAnimationSequenceSedna().hold(350).addPos(0, 0, pullAmount, 250, IType.SIN_UP).addPos(0, 0, 0, 200, IType.LINEAR).addPos(0, 0, pullAmount, 250, IType.SIN_UP).addPos(0, 0, 0, 200, IType.LINEAR));
            case INSPECT: return new BusAnimationSedna()
                    .addBus("LIFT", new BusAnimationSequenceSedna().hold(350).addPos(-3, 0, 0, 150, IType.SIN_DOWN).holdUntil(1050).addPos(0, 0, 0, 250, IType.SIN_FULL))
                    .addBus("BOLT_TURN", new BusAnimationSequenceSedna().addPos(0, 0, turn, 150).holdUntil(1050).addPos(0, 0, 0, 150))
                    .addBus("BOLT_PULL", new BusAnimationSequenceSedna().hold(100).addPos(0, 0, -1D, 250, IType.SIN_UP).hold(500).addPos(0, 0, 0, 200, IType.LINEAR))
                    .addBus("BULLET", mag == 0 ? new BusAnimationSequenceSedna().setPos(-100, 0, 0) : new BusAnimationSequenceSedna().setPos(0, 0.1875, 1.5).hold(100).addPos(0, 0.125, 0.5, 250, IType.SIN_UP).hold(500).addPos(0, 0.1875, 1.5, 200, IType.LINEAR));
        }

        return null;
    };

    // CE: XFactory9mm.java
    public static final BiFunction<ItemStack, GunAnimationType, BusAnimationSedna> LAMBDA_GREASEGUN_ANIMS = (stack, type) -> {
        switch (type) {
            case EQUIP -> {
                return new BusAnimationSedna()
                        .addBus("EQUIP", new BusAnimationSequenceSedna().addPos(80, 0, 0, 0).addPos(80, 0, 0, 500).addPos(0, 0, 0, 500, IType.SIN_FULL))
                        .addBus("STOCK", new BusAnimationSequenceSedna().addPos(0, 0, -4, 0).addPos(0, 0, -4, 200).addPos(0, 0, 0, 300, IType.SIN_FULL));
            }
            case CYCLE -> {
                return new BusAnimationSedna()
                        .addBus("RECOIL", new BusAnimationSequenceSedna().addPos(0, 0, ItemGunBaseNT.getIsAiming(stack) ? -0.25 : -0.5, 50, IType.SIN_DOWN).addPos(0, 0, 0, 100, IType.SIN_FULL))
                        .addBus("FLAP", new BusAnimationSequenceSedna().addPos(0, 0, 15, 100, IType.SIN_DOWN).addPos(0, 0, -5, 100, IType.SIN_FULL).addPos(0, 0, 0, 50, IType.SIN_FULL));
            }
            case CYCLE_DRY -> {
                return new BusAnimationSedna()
                        .addBus("LIFT", new BusAnimationSequenceSedna().addPos(0, 0, 0, 500).addPos(-25, 0, 0, 250, IType.SIN_FULL).addPos(-25, 0, 0, 750).addPos(0, 0, 0, 500, IType.SIN_FULL))
                        .addBus("TURN", new BusAnimationSequenceSedna().addPos(0, 0, 0, 500).addPos(0, 0, -45, 250, IType.SIN_FULL).addPos(0, 0, -45, 750).addPos(0, 0, 0, 500, IType.SIN_FULL))
                        .addBus("HANDLE", new BusAnimationSequenceSedna().addPos(0, 0, 0, 500).addPos(0, 0, 0, 250).addPos(-90, 0, 0, 250, IType.SIN_FULL).addPos(0, 0, 0, 250, IType.SIN_FULL));
            }
            case RELOAD -> {
                boolean empty = ((ItemGunBaseNT) stack.getItem()).getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack).getAmount(stack, Minecraft.getInstance().player == null ? null : Minecraft.getInstance().player.getInventory()) <= 0;
                return new BusAnimationSedna()
                        .addBus("MAG", new BusAnimationSequenceSedna().addPos(0, -8, 0, 250, IType.SIN_UP).addPos(0, -8, 0, 750).addPos(0, 0, 0, 500, IType.SIN_DOWN))
                        .addBus("LIFT", new BusAnimationSequenceSedna().addPos(0, 0, 0, 500).addPos(-25, 0, 0, 250, IType.SIN_FULL).addPos(-25, 0, 0, 1750).addPos(0, 0, 0, 500, IType.SIN_FULL))
                        .addBus("TURN", new BusAnimationSequenceSedna().addPos(0, 0, 0, 1750).addPos(0, 0, -45, 250, IType.SIN_FULL).addPos(0, 0, -45, 500).addPos(0, 0, 0, 500, IType.SIN_FULL))
                        .addBus("HANDLE", new BusAnimationSequenceSedna().addPos(0, 0, 0, 2000).addPos(-90, 0, 0, 250, IType.SIN_FULL).addPos(0, 0, 0, 250, IType.SIN_FULL))
                        .addBus("BULLET", new BusAnimationSequenceSedna().addPos(empty ? 1 : 0, 0, 0, 0).addPos(0, 0, 0, 1000));
            }
            case JAMMED -> {
                return new BusAnimationSedna()
                        .addBus("LIFT", new BusAnimationSequenceSedna().addPos(0, 0, 0, 500).addPos(-25, 0, 0, 250, IType.SIN_FULL).addPos(-25, 0, 0, 1500).addPos(0, 0, 0, 500, IType.SIN_FULL))
                        .addBus("TURN", new BusAnimationSequenceSedna().addPos(0, 0, 0, 500).addPos(0, 0, -45, 250, IType.SIN_FULL).addPos(0, 0, -45, 1500).addPos(0, 0, 0, 500, IType.SIN_FULL))
                        .addBus("HANDLE", new BusAnimationSequenceSedna().addPos(0, 0, 0, 500).addPos(0, 0, 0, 250).addPos(-90, 0, 0, 250, IType.SIN_FULL).addPos(0, 0, 0, 250, IType.SIN_FULL).addPos(0, 0, 0, 250).addPos(-90, 0, 0, 250, IType.SIN_FULL).addPos(0, 0, 0, 250, IType.SIN_FULL));
            }
            case INSPECT -> {
                return new BusAnimationSedna()
                        .addBus("TURN", new BusAnimationSequenceSedna().addPos(0, 0, -45, 150).addPos(0, 0, 45, 150).addPos(0, 0, 45, 50).addPos(0, 0, 0, 250).addPos(0, 0, 0, 500).addPos(0, 0, 45, 150).addPos(0, 0, -45, 150).addPos(0, 0, 0, 150))
                        .addBus("FLAP", new BusAnimationSequenceSedna().addPos(0, 0, 0, 300).addPos(0, 0, 180, 150).addPos(0, 0, 180, 850).addPos(0, 0, 0, 150));
            }
        }

        return null;
    };

    // CE: XFactory9mm.java
    public static final BiFunction<ItemStack, GunAnimationType, BusAnimationSedna> LAMBDA_LAG_ANIMS = (stack, type) -> switch (type) {
        case EQUIP -> new BusAnimationSedna()
                .addBus("EQUIP", new BusAnimationSequenceSedna().addPos(-90, 0, 0, 0).addPos(0, 0, 0, 350, IType.SIN_DOWN));
        case CYCLE -> GunModels.json("lag_anim").get("Firing");
        //.addBus("HAMMER", new BusAnimationSequenceSedna().addPos(0, 0, 25, 50).addPos(0, 0, 25, 50).addPos(0, 0, 0, 100, IType.SIN_DOWN));
        case CYCLE_DRY -> GunModels.json("lag_anim").get("Dryfire");
        case RELOAD -> GunModels.json("lag_anim").get("Reload");
        case JAMMED -> GunModels.json("lag_anim").get("Jam");
        case INSPECT -> GunModels.json("lag_anim").get("Inspect")
                .addBus("ADD_TRANS", new BusAnimationSequenceSedna().addPos(-4, 0, -3, 500).addPos(-4, 0, -3, 2000).addPos(0, 0, 0, 500))
                .addBus("ADD_ROT", new BusAnimationSequenceSedna().addPos(0, -2, 5, 500).addPos(0, -2, 5, 2000).addPos(0, 0, 0, 500));
        default -> null;
    };

    // CE: XFactory9mm.java
    public static final BiFunction<ItemStack, GunAnimationType, BusAnimationSedna> LAMBDA_UZI_ANIMS = (stack, type) -> {
        switch (type) {
            case EQUIP -> {
                return new BusAnimationSedna()
                        .addBus("EQUIP", new BusAnimationSequenceSedna().addPos(80, 0, 0, 0).addPos(80, 0, 0, 500).addPos(0, 0, 0, 500, IType.SIN_FULL))
                        .addBus("STOCKBACK", new BusAnimationSequenceSedna().addPos(-200, 0, 0, 0).addPos(0, 0, 0, 500, IType.SIN_FULL))
                        .addBus("STOCKFRONT", new BusAnimationSequenceSedna().addPos(180, 0, 0, 0).addPos(0, 0, 0, 500, IType.SIN_FULL));
            }
            case CYCLE -> {
                return new BusAnimationSedna()
                        .addBus("RECOIL", new BusAnimationSequenceSedna().addPos(0, 0, ItemGunBaseNT.getIsAiming(stack) ? -0.5 : -0.75, 25, IType.SIN_DOWN).addPos(0, 0, 0, 75, IType.SIN_FULL));
            }
            case CYCLE_DRY -> {
                return new BusAnimationSedna()
                        .addBus("LIFT", new BusAnimationSequenceSedna().addPos(0, 0, 0, 250).addPos(-25, 0, 0, 250, IType.SIN_FULL).addPos(-25, 0, 0, 500).addPos(0, 0, 0, 250, IType.SIN_FULL))
                        .addBus("SLIDE", new BusAnimationSequenceSedna().addPos(0, 0, 0, 500).addPos(0, 0, -2, 150, IType.SIN_FULL).addPos(0, 0, 0, 50, IType.SIN_UP));
            }
            case RELOAD -> {
                boolean empty = ((ItemGunBaseNT) stack.getItem()).getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack).getAmount(stack, Minecraft.getInstance().player == null ? null : Minecraft.getInstance().player.getInventory()) <= 0;
                return new BusAnimationSedna()
                        .addBus("MAG", new BusAnimationSequenceSedna().addPos(0, 0, 0, 250).addPos(0, -10, 0, 250, IType.SIN_UP).addPos(0, -10, 0, 750).addPos(0, 0, 0, 500, IType.SIN_DOWN))
                        .addBus("LIFT", new BusAnimationSequenceSedna().addPos(-25, 0, 0, 250, IType.SIN_FULL).addPos(-25, 0, 0, 2000).addPos(0, 0, 0, 500, IType.SIN_FULL))
                        .addBus("SLIDE", new BusAnimationSequenceSedna().addPos(0, 0, 0, 2000).addPos(0, 0, -2, 150, IType.SIN_FULL).addPos(0, 0, 0, 50, IType.SIN_UP))
                        .addBus("BULLET", new BusAnimationSequenceSedna().addPos(empty ? 0 : 1, 0, 0, 0).addPos(empty ? 0 : 1, 0, 0, 500).addPos(1, 0, 0, 0));
            }
            case JAMMED -> {
                return new BusAnimationSedna()
                        .addBus("LIFT", new BusAnimationSequenceSedna().addPos(0, 0, 0, 500).addPos(-25, 0, 0, 250, IType.SIN_FULL).addPos(-25, 0, 0, 1250).addPos(0, 0, 0, 500, IType.SIN_FULL))
                        .addBus("SLIDE", new BusAnimationSequenceSedna().addPos(0, 0, 0, 1000).addPos(0, 0, -2, 150, IType.SIN_FULL).addPos(0, 0, 0, 50, IType.SIN_UP).addPos(0, 0, 0, 500).addPos(0, 0, -2, 150, IType.SIN_FULL).addPos(0, 0, 0, 50, IType.SIN_UP));
            }
            case INSPECT -> {
                return new BusAnimationSedna()
                        .addBus("YEET", new BusAnimationSequenceSedna().addPos(0, -1, 0, 100).addPos(0, 0, 0, 100, IType.SIN_UP).addPos(0, 12, 0, 350, IType.SIN_DOWN).addPos(0, 0, 0, 350, IType.SIN_UP).addPos(0, -1, 0, 50, IType.SIN_DOWN).addPos(0, 0, 0, 100, IType.SIN_FULL))
                        .addBus("SPEEN", new BusAnimationSequenceSedna().addPos(0, 0, 0, 250).addPos(-360, 0, 0, 600));
            }
        }
        return null;
    };

    // CE: XFactory556mm.java
    public static final BiFunction<ItemStack, GunAnimationType, BusAnimationSedna> LAMBDA_G3_ANIMS = (stack, type) -> {
        boolean empty = ((ItemGunBaseNT) stack.getItem()).getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack).getAmount(stack, Minecraft.getInstance().player == null ? null : Minecraft.getInstance().player.getInventory()) <= 0;
        return switch (type) {
            case EQUIP -> new BusAnimationSedna()
                    .addBus("EQUIP", new BusAnimationSequenceSedna().addPos(45, 0, 0, 0).addPos(0, 0, 0, 500, IType.SIN_FULL));
            case CYCLE -> new BusAnimationSedna()
                    .addBus("RECOIL", new BusAnimationSequenceSedna().addPos(0, 0, ItemGunBaseNT.getIsAiming(stack) ? -0.5 : -0.75, 25, IType.SIN_DOWN).addPos(0, 0, 0, 75, IType.SIN_FULL));
            case CYCLE_DRY -> new BusAnimationSedna()
                    .addBus("BOLT", new BusAnimationSequenceSedna().addPos(0, 0, 0, 250).addPos(0, 0, -3.25, 150).addPos(0, 0, 0, 100))
                    .addBus("LIFT", new BusAnimationSequenceSedna().addPos(0, 0, 0, 400).addPos(-1, 0, 0, 100, IType.SIN_DOWN).addPos(0, 0, 0, 100, IType.SIN_FULL));
            case RELOAD -> new BusAnimationSedna()
                    .addBus("MAG", new BusAnimationSequenceSedna()
                            .addPos(0, -8, 0, 250, IType.SIN_UP)    //250
                            .addPos(0, -8, 0, 1000)                    //1250
                            .addPos(0, 0, 0, 300))                    //1550
                    .addBus("BOLT", new BusAnimationSequenceSedna()
                            .addPos(0, 0, 0, 250)                    //250
                            .addPos(0, 0, -3.25, 150)                //400
                            .addPos(0, 0, -3.25, 1250)                //1750
                            .addPos(0, 0, 0, 100))                    //1850
                    .addBus("HANDLE", new BusAnimationSequenceSedna()
                            .addPos(0, 0, 0, 500)                    //500
                            .addPos(0, 0, 45, 50)                    //550
                            .addPos(0, 0, 45, 1150)                    //1700
                            .addPos(0, 0, 0, 50))                    //1750
                    .addBus("LIFT", new BusAnimationSequenceSedna()
                            .addPos(0, 0, 0, 750)                    //750
                            .addPos(-25, 0, 0, 500, IType.SIN_FULL)    //1250
                            .addPos(-25, 0, 0, 750)                    //2000
                            .addPos(0, 0, 0, 500, IType.SIN_FULL))    //3500
                    .addBus("BULLET", new BusAnimationSequenceSedna().addPos(empty ? 1 : 0, 0, 0, 0).addPos(0, 0, 0, 1000));
            case INSPECT -> new BusAnimationSedna()
                    .addBus("MAG", new BusAnimationSequenceSedna()
                            .addPos(0, -1, 0, 150)                    //150
                            .addPos(2, -1, 0, 150)                    //300
                            .addPos(2, 8, 0, 350, IType.SIN_DOWN)    //650
                            .addPos(2, -2, 0, 350, IType.SIN_UP)    //1000
                            .addPos(2, -1, 0, 50)                    //1050
                            .addPos(2, -1, 0, 100)                    //1150
                            .addPos(0, -1, 0, 150, IType.SIN_FULL)    //1300
                            .addPos(0, 0, 0, 150, IType.SIN_UP))    //1450
                    .addBus("SPEEN", new BusAnimationSequenceSedna().addPos(0, 0, 0, 300).addPos(0, 360, 360, 700))
                    .addBus("LIFT", new BusAnimationSequenceSedna().addPos(0, 0, 0, 1450).addPos(-2, 0, 0, 100, IType.SIN_DOWN).addPos(0, 0, 0, 100, IType.SIN_FULL))
                    .addBus("BULLET", new BusAnimationSequenceSedna().addPos(empty ? 1 : 0, 0, 0, 0));
            case JAMMED -> new BusAnimationSedna()
                    .addBus("LIFT", new BusAnimationSequenceSedna().addPos(0, 0, 0, 500).addPos(-25, 0, 0, 250, IType.SIN_FULL).addPos(-25, 0, 0, 1250).addPos(0, 0, 0, 350, IType.SIN_FULL))
                    .addBus("BOLT", new BusAnimationSequenceSedna().addPos(0, 0, 0, 1000).addPos(0, 0, -3.25, 150).addPos(0, 0, 0, 100).addPos(0, 0, 0, 250).addPos(0, 0, -3.25, 150).addPos(0, 0, 0, 100));
            default -> null;
        };

    };

    // CE: XFactory556mm.java
    public static final BiFunction<ItemStack, GunAnimationType, BusAnimationSedna> LAMBDA_STG77_ANIMS = (stack, type) -> {
        if(ClientConfig.GUN_ANIMS_LEGACY.get()) {
            switch (type) {
                case EQUIP -> {
                    return new BusAnimationSedna()
                            .addBus("EQUIP", new BusAnimationSequenceSedna().addPos(45, 0, 0, 0).addPos(0, 0, 0, 500, IType.SIN_FULL));
                }
                case CYCLE -> {
                    return new BusAnimationSedna()
                            .addBus("RECOIL", new BusAnimationSequenceSedna().addPos(0, 0, ItemGunBaseNT.getIsAiming(stack) ? -0.125 : -0.375, 25, IType.SIN_DOWN).addPos(0, 0, 0, 75, IType.SIN_FULL))
                            .addBus("SAFETY", new BusAnimationSequenceSedna().addPos(0.25, 0, 0, 0).addPos(0.25, 0, 0, 2000).addPos(0, 0, 0, 50));
                }
                case CYCLE_DRY -> {
                    return new BusAnimationSedna()
                            .addBus("BOLT", new BusAnimationSequenceSedna().addPos(0, 0, 0, 250).addPos(0, 0, -2, 150).addPos(0, 0, 0, 100, IType.SIN_UP))
                            .addBus("SAFETY", new BusAnimationSequenceSedna().addPos(0.25, 0, 0, 0).addPos(0.25, 0, 0, 2000).addPos(0, 0, 0, 50));
                }
                case RELOAD -> {
                    return new BusAnimationSedna()
                            .addBus("BOLT", new BusAnimationSequenceSedna().addPos(0, 0, -2, 150).addPos(0, 0, -2, 1600).addPos(0, 0, 0, 100, IType.SIN_UP))
                            .addBus("HANDLE", new BusAnimationSequenceSedna().addPos(0, 0, 0, 150).addPos(0, 0, 20, 50).addPos(0, 0, 20, 1500).addPos(0, 0, 0, 50))
                            .addBus("LIFT", new BusAnimationSequenceSedna().addPos(0, 0, 0, 200).addPos(-2, 0, 0, 100, IType.SIN_DOWN).addPos(0, 0, 0, 100, IType.SIN_FULL));
                }
                case INSPECT -> {
                    return new BusAnimationSedna()
                            .addBus("BOLT", new BusAnimationSequenceSedna().addPos(0, 0, -2, 150).addPos(0, 0, -2, 6100).addPos(0, 0, 0, 100, IType.SIN_UP))
                            .addBus("HANDLE", new BusAnimationSequenceSedna().addPos(0, 0, 0, 150).addPos(0, 0, 20, 50).addPos(0, 0, 20, 6000).addPos(0, 0, 0, 50))
                            .addBus("INSPECT_LEVER", new BusAnimationSequenceSedna().addPos(0, 0, 0, 500).addPos(0, 0, -10, 100).addPos(0, 0, -10, 100).addPos(0, 0, 0, 100))
                            .addBus("INSPECT_BARREL", new BusAnimationSequenceSedna().addPos(0, 0, 0, 600).addPos(0, 0, 20, 150).addPos(0, 0, 0, 400).addPos(0, 0, 0, 500).addPos(15, 0, 0, 500).addPos(15, 0, 0, 2000).addPos(0, 0, 0, 500).addPos(0, 0, 0, 500).addPos(0, 0, 20, 200).addPos(0, 0, 20, 400).addPos(0, 0, 0, 150))
                            .addBus("INSPECT_MOVE", new BusAnimationSequenceSedna().addPos(0, 0, 0, 750).addPos(0, 0, 6, 1000).addPos(2, 0, 3, 500, IType.SIN_FULL).addPos(2, 0.75, 0, 500, IType.SIN_FULL).addPos(2, 0.75, 0, 1000).addPos(2, 0, 3, 500, IType.SIN_FULL).addPos(0, 0, 6, 500).addPos(0, 0, 0, 1000))
                            .addBus("INSPECT_GUN", new BusAnimationSequenceSedna().addPos(0, 0, 0, 1750).addPos(15, 0, -70, 500, IType.SIN_FULL).addPos(15, 0, -70, 1500).addPos(0, 0, 0, 500, IType.SIN_FULL));
                }
            }
        } else {
            switch (type) {
                case EQUIP -> {
                    return new BusAnimationSedna()
                            .addBus("EQUIP", new BusAnimationSequenceSedna().addPos(45, 0, 0, 0).addPos(0, 0, 0, 500, IType.SIN_FULL));
                }
                case CYCLE -> {
                    return new BusAnimationSedna()
                            .addBus("RECOIL", new BusAnimationSequenceSedna().addPos(0, 0, ItemGunBaseNT.getIsAiming(stack) ? -0.125 : -0.375, 25, IType.SIN_DOWN).addPos(0, 0, 0, 75, IType.SIN_FULL))
                            .addBus("SAFETY", new BusAnimationSequenceSedna().addPos(0.25, 0, 0, 0).addPos(0.25, 0, 0, 2000).addPos(0, 0, 0, 50));
                }
                case CYCLE_DRY -> {
                    return GunModels.json("stg77_anim").get("FireDry");
                }
                case RELOAD -> {
                    return GunModels.json("stg77_anim").get("Reload");
                }
                case INSPECT -> {
                    return GunModels.json("stg77_anim").get("Inspect");
                }
            }
        }


        return null;
    };

    // CE: XFactoryEnergy.java
    public static final BiFunction<ItemStack, GunAnimationType, BusAnimationSedna> LAMBDA_TESLA_ANIMS = (stack, type) -> {
        int amount = ((ItemGunBaseNT) stack.getItem()).getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack).getAmount(stack, Minecraft.getInstance().player == null ? null : Minecraft.getInstance().player.getInventory());
        return switch (type) {
            case EQUIP -> new BusAnimationSedna()
                    .addBus("EQUIP", new BusAnimationSequenceSedna().addPos(60, 0, 0, 0).addPos(0, 0, 0, 1000, IType.SIN_DOWN));
            case CYCLE -> new BusAnimationSedna()
                    .addBus("RECOIL", new BusAnimationSequenceSedna().addPos(0, 0, ItemGunBaseNT.getIsAiming(stack) ? -0.5 : -1, 100, IType.SIN_DOWN).addPos(0, 0, 0, 250, IType.SIN_FULL))
                    .addBus("CYCLE", new BusAnimationSequenceSedna().addPos(0, 0, 0, 150).addPos(0, 0, 22.5, 350))
                    .addBus("COUNT", new BusAnimationSequenceSedna().addPos(amount, 0, 0, 0));
            case CYCLE_DRY -> new BusAnimationSedna()
                    .addBus("CYCLE", new BusAnimationSequenceSedna().addPos(0, 0, 0, 150).addPos(0, 0, 22.5, 350));
            case INSPECT -> new BusAnimationSedna()
                    .addBus("YOMI", new BusAnimationSequenceSedna().addPos(8, -4, 0, 0).addPos(4, -1, 0, 500, IType.SIN_DOWN).addPos(4, -1, 0, 1000).addPos(6, -6, 0, 500, IType.SIN_UP))
                    .addBus("SQUEEZE", new BusAnimationSequenceSedna().addPos(1, 1, 1, 0).addPos(1, 1, 1, 750).addPos(1, 1, 0.5, 125).addPos(1, 1, 1, 125));
            default -> null;
        };

    };

    // CE: XFactoryEnergy.java
    public static final BiFunction<ItemStack, GunAnimationType, BusAnimationSedna> LAMBDA_LASER_PISTOL = (stack, type) -> {
        switch(type) {
            case EQUIP: return new BusAnimationSedna()
                    .addBus("EQUIP", new BusAnimationSequenceSedna().addPos(60, 0, 0, 0).addPos(0, 0, 0, 500, IType.SIN_DOWN));
            case CYCLE: return new BusAnimationSedna()
                    .addBus("RECOIL", new BusAnimationSequenceSedna().addPos(0, 0, -0.5, 50, IType.SIN_DOWN).addPos(0, 0, 0, 150, IType.SIN_FULL));
            case RELOAD: return new BusAnimationSedna()
                    .addBus("LATCH", new BusAnimationSequenceSedna().addPos(0, -20, 0, 100).hold(1900).addPos(0, 0, 0, 100))
                    .addBus("LIFT", new BusAnimationSequenceSedna().hold(100).addPos(-45, 0, 0, 250, IType.SIN_FULL).hold(500).addPos(0, 0, 0, 500, IType.SIN_FULL))
                    .addBus("JOLT", new BusAnimationSequenceSedna().hold(350).addPos(0, 0, 0.5, 100, IType.SIN_FULL).addPos(0, 0, -1.5, 100, IType.SIN_UP).addPos(0, 0, 0, 150, IType.SIN_FULL).holdUntil(2100).addPos(-0.0625, 0, 0, 50, IType.SIN_UP).addPos(0, 0, 0, 100, IType.SIN_FULL))
                    .addBus("BATTERY", new BusAnimationSequenceSedna().hold(550).addPos(0, 0, 5, 250).hold(550).setPos(0, -2, -2).addPos(0, 0, -2, 250, IType.SIN_FULL).addPos(0, 0, 0, 250, IType.SIN_UP));
            case JAMMED: return new BusAnimationSedna()
                    .addBus("LATCH", new BusAnimationSequenceSedna().hold(500).addPos(0, -20, 0, 100).hold(250).addPos(0, 0, 0, 100))
                    .addBus("JOLT", new BusAnimationSequenceSedna().hold(950).addPos(-0.0625, 0, 0, 50, IType.SIN_UP).addPos(0, 0, 0, 100, IType.SIN_FULL))
                    .addBus("EQUIP", new BusAnimationSequenceSedna().hold(1500).addPos(7.5, 0, 0, 100, IType.SIN_DOWN).addPos(0, 0, 0, 250, IType.SIN_FULL));
            case INSPECT: return new BusAnimationSedna()
                    .addBus("SWIRL", new BusAnimationSequenceSedna().addPos(-720, 0, 0, 750, IType.SIN_FULL).hold(500).addPos(0, 0, 0, 750, IType.SIN_FULL));
        }
        return null;
    };

    // CE: XFactoryEnergy.java
    public static final BiFunction<ItemStack, GunAnimationType, BusAnimationSedna> LAMBDA_LASRIFLE = (stack, type) -> {
        int amount = ((ItemGunBaseNT) stack.getItem()).getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack).getAmount(stack, Minecraft.getInstance().player == null ? null : Minecraft.getInstance().player.getInventory());
        return switch (type) {
            case EQUIP -> new BusAnimationSedna()
                    .addBus("EQUIP", new BusAnimationSequenceSedna().addPos(60, 0, 0, 0).addPos(0, 0, 0, 500, IType.SIN_DOWN));
            case CYCLE -> new BusAnimationSedna()
                    .addBus("RECOIL", new BusAnimationSequenceSedna().addPos(0, 0, -0.5, 50, IType.SIN_DOWN).addPos(0, 0, 0, 150, IType.SIN_FULL))
                    .addBus("CYCLE", new BusAnimationSequenceSedna().addPos(0, 0, 0, 150).addPos(0, 0, 22.5, 350))
                    .addBus("COUNT", new BusAnimationSequenceSedna().addPos(amount, 0, 0, 0));
            case RELOAD -> new BusAnimationSedna()
                    .addBus("LEVER", new BusAnimationSequenceSedna().addPos(-90, 0, 0, 350, IType.SIN_UP).addPos(-90, 0, 0, 1500).addPos(0, 0, 0, 350, IType.SIN_UP))
                    .addBus("MAG", new BusAnimationSequenceSedna().addPos(0, 0, 0, 350).addPos(0, -5, 0, 350, IType.SIN_UP).addPos(0, -5, 0, 500).addPos(0, -0.25, 0, 500, IType.SIN_FULL).addPos(0, -0.25, 0, 150).addPos(0, 0, 0, 350))
                    .addBus("EQUIP", new BusAnimationSequenceSedna().addPos(0, 0, 0, 1700).addPos(-2, 0, 0, 100, IType.SIN_DOWN).addPos(0, 0, 0, 100, IType.SIN_FULL));
            case JAMMED -> new BusAnimationSedna()
                    .addBus("LEVER", new BusAnimationSequenceSedna().addPos(0, 0, 0, 500).addPos(-90, 0, 0, 350, IType.SIN_UP).addPos(-90, 0, 0, 600).addPos(0, 0, 0, 350, IType.SIN_UP))
                    .addBus("MAG", new BusAnimationSequenceSedna().addPos(0, 0, 0, 500).addPos(0, 0, 0, 350).addPos(0, -2, 0, 200, IType.SIN_UP).addPos(0, -0.25, 0, 250, IType.SIN_FULL).addPos(0, -0.25, 0, 150).addPos(0, 0, 0, 350))
                    .addBus("EQUIP", new BusAnimationSequenceSedna().addPos(0, 0, 0, 500).addPos(0, 0, 0, 800).addPos(-2, 0, 0, 100, IType.SIN_DOWN).addPos(0, 0, 0, 100, IType.SIN_FULL));
            case INSPECT -> new BusAnimationSedna()
                    .addBus("LEVER", new BusAnimationSequenceSedna().addPos(-90, 0, 0, 350, IType.SIN_UP).addPos(-90, 0, 0, 600).addPos(0, 0, 0, 350, IType.SIN_UP))
                    .addBus("MAG", new BusAnimationSequenceSedna().addPos(0, 0, 0, 350).addPos(0, -2, 0, 200, IType.SIN_UP).addPos(0, -0.25, 0, 250, IType.SIN_FULL).addPos(0, -0.25, 0, 150).addPos(0, 0, 0, 350))
                    .addBus("EQUIP", new BusAnimationSequenceSedna().addPos(0, 0, 0, 800).addPos(-2, 0, 0, 100, IType.SIN_DOWN).addPos(0, 0, 0, 100, IType.SIN_FULL));
            default -> null;
        };

    };

    // CE: XFactoryPA.java
    public static final BiFunction<ItemStack, GunAnimationType, BusAnimationSedna> LAMBDA_MELEE_ANIMS = (stack, type) -> {
        // TODO(CE:XFactoryPA.java:36) IPAWeaponsProvider / IPAMelee not ported.
        return switch (type) {
            case EQUIP -> new BusAnimationSedna()
                    .addBus("EQUIP", new BusAnimationSequenceSedna().addPos(60, 0, 0, 0).addPos(0, 0, 0, 500, IType.SIN_DOWN));
            default -> null;
        };
    };

    // CE: XFactory35800.java
    public static final BiFunction<ItemStack, GunAnimationType, BusAnimationSedna> LAMBDA_ABERRATOR = (stack, type) -> {
        boolean aim = ItemGunBaseNT.getIsAiming(stack);
        int ammo = ((ItemGunBaseNT) stack.getItem()).getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack).getAmount(stack, null);
        return switch (type) {
            case EQUIP -> new BusAnimationSedna()
                    .addBus("EQUIP", new BusAnimationSequenceSedna().addPos(360, 0, 0, 0).addPos(0, 0, 0, 500, IType.SIN_FULL))
                    .addBus("RISE", new BusAnimationSequenceSedna().addPos(0, -3, 0, 0).addPos(0, 0, 0, 500, IType.SIN_FULL));
            case CYCLE -> new BusAnimationSedna()
                    .addBus("RECOIL", new BusAnimationSequenceSedna().addPos(0, 0, 0, 50).addPos(aim ? -15 : -25, 0, 0, 100, IType.SIN_DOWN).addPos(0, 0, 0, 500, IType.SIN_FULL))
                    .addBus("SIGHT", new BusAnimationSequenceSedna().addPos(0, 0, 0, 50).addPos(aim ? 5 : 15, 0, 0, 100, IType.SIN_DOWN).addPos(0, 0, 0, 250, IType.SIN_FULL))
                    .addBus("SLIDE", new BusAnimationSequenceSedna().addPos(0, 0, 0, 50).addPos(0, 0, -1.125, 50, IType.SIN_DOWN).addPos(0, 0, -1.125, 50).addPos(0, 0, 0, 150, IType.SIN_UP))
                    .addBus(ammo <= 1 ? "NULL" : "BULLET", new BusAnimationSequenceSedna().addPos(0, 0, 0, 150).addPos(0, 0.375, 1.125, 150, IType.SIN_UP))
                    .addBus("HAMMER", new BusAnimationSequenceSedna().addPos(45, 0, 0, 50).addPos(-45, 0, -1.125, 50, IType.SIN_DOWN).addPos(-20, 0, -1.125, 50).addPos(0, 0, 0, 150, IType.SIN_UP));
            case CYCLE_DRY -> new BusAnimationSedna()
                    .addBus("RECOIL", new BusAnimationSequenceSedna().addPos(0, 0, 0, 700).addPos(-5, 0, 0, 100, IType.SIN_FULL).addPos(0, 0, 0, 250, IType.SIN_FULL))
                    .addBus("SLIDE", new BusAnimationSequenceSedna().addPos(0, 0, 0, 550).addPos(0, 0, -1.125, 150, IType.SIN_FULL).addPos(0, 0, -1.125, 50).addPos(0, 0, 0, 150, IType.SIN_UP))
                    .addBus("HAMMER", new BusAnimationSequenceSedna().addPos(45, 0, 0, 50).addPos(45, 0, 0, 500).addPos(-45, 0, -1.125, 150, IType.SIN_FULL).addPos(-20, 0, -1.125, 50).addPos(0, 0, 0, 150, IType.SIN_UP));
            case RELOAD -> new BusAnimationSedna()
                    .addBus("ROLL", new BusAnimationSequenceSedna().addPos(0, 0, 20, 150, IType.SIN_FULL).addPos(0, 0, 20, 50).addPos(0, 0, -45, 150, IType.SIN_UP).addPos(0, 0, 0, 150, IType.SIN_FULL))
                    .addBus("MAG", new BusAnimationSequenceSedna().addPos(0, 0, 0, 350).addPos(0, -2, 0, 0).addPos(-15, -5, 0, 350).addPos(-15, 0, 0, 0).addPos(-15, 0, 0, 700).addPos(3, 3, 0, 0).addPos(0, -2, 0, 250, IType.SIN_DOWN).addPos(0, -2, 0, 50).addPos(0, 0, 0, 150, IType.SIN_DOWN))
                    .addBus("MAGROLL", new BusAnimationSequenceSedna().addPos(0, 0, 0, 350).addPos(0, 0, -180, 250).addPos(0, 0, 0, 0))
                    .addBus("EQUIP", new BusAnimationSequenceSedna().addPos(0, 0, 0, 750).addPos(5, 0, 0, 150, IType.SIN_FULL).addPos(-190, 0, 0, 500, IType.SIN_FULL).addPos(-190, 0, 0, 450).addPos(-360, 0, 0, 350, IType.SIN_DOWN).addPos(0, 0, 0, 0))
                    .addBus("RECOIL", new BusAnimationSequenceSedna().addPos(0, 0, 0, 2350).addPos(-5, 0, 0, 100, IType.SIN_FULL).addPos(0, 0, 0, 250, IType.SIN_FULL))
                    .addBus("SLIDE", new BusAnimationSequenceSedna().addPos(0, 0, 0, 2200).addPos(0, 0, -1.125, 150, IType.SIN_FULL).addPos(0, 0, -1.125, 50).addPos(0, 0, 0, 150, IType.SIN_UP))
                    .addBus("HAMMER", new BusAnimationSequenceSedna().addPos(0, 0, 0, 2250).addPos(-45, 0, -1.125, 100, IType.SIN_FULL).addPos(-20, 0, -1.125, 50).addPos(0, 0, 0, 150, IType.SIN_UP))
                    .addBus("BULLET", new BusAnimationSequenceSedna().addPos(ammo > 0 ? 0 : -100, 0, 0, 0).addPos(ammo > 0 ? 0 : -100, 0, 0, 2400).addPos(0, 0, 0, 0).addPos(0, 0.375, 1.125, 150, IType.SIN_UP));
            case INSPECT -> new BusAnimationSedna()
                    .addBus("EQUIP", new BusAnimationSequenceSedna().addPos(0, 0, 0, 0).addPos(-720, 0, 0, 1000, IType.SIN_FULL).addPos(-720, 0, 0, 250).addPos(0, 0, 0, 1000, IType.SIN_FULL));
            default -> null;
        };

    };

    // CE: XFactoryAccelerator.java
    public static final BiFunction<ItemStack, GunAnimationType, BusAnimationSedna> LAMBDA_TAU_ANIMS = (stack, type) -> switch (type) {
        case EQUIP -> new BusAnimationSedna()
                .addBus("EQUIP", new BusAnimationSequenceSedna().addPos(45, 0, 0, 0).addPos(0, 0, 0, 500, IType.SIN_FULL));
        case CYCLE -> new BusAnimationSedna()
                .addBus("RECOIL", new BusAnimationSequenceSedna().addPos(0, 0, -0.5, 50).addPos(0, 0, 0, 150, IType.SIN_FULL))
                .addBus("ROTATE", new BusAnimationSequenceSedna().addPos(0, 0, -5, 50, IType.SIN_DOWN).addPos(0, 0, 5, 100, IType.SIN_FULL).addPos(0, 0, 0, 50, IType.SIN_UP));
        case ALT_CYCLE -> new BusAnimationSedna()
                .addBus("RECOIL", new BusAnimationSequenceSedna().addPos(0, 0, -3, 100, IType.SIN_DOWN).addPos(0, 0, 0, 250, IType.SIN_FULL))
                .addBus("ROTATE", new BusAnimationSequenceSedna().addPos(0, 0, -5, 50, IType.SIN_DOWN).addPos(0, 0, 5, 100, IType.SIN_FULL).addPos(0, 0, 0, 50, IType.SIN_UP));
        case CYCLE_DRY -> new BusAnimationSedna();
        case INSPECT -> new BusAnimationSedna()
                .addBus("EQUIP", new BusAnimationSequenceSedna().addPos(2, 0, 0, 150, IType.SIN_DOWN).addPos(0, 0, 0, 100, IType.SIN_FULL))
                .addBus("ROTATE", new BusAnimationSequenceSedna().addPos(0, 0, -360 * 3, 500 * 3, IType.SIN_DOWN));
        case SPINUP -> new BusAnimationSedna()
                .addBus("ROTATE", new BusAnimationSequenceSedna().addPos(0, 0, 360 * 6, 3000, IType.SIN_UP).addPos(0, 0, 0, 0).addPos(0, 0, 360 * 40, 500 * 20));
        default -> null;
    };

    // CE: XFactoryAccelerator.java
    public static final BiFunction<ItemStack, GunAnimationType, BusAnimationSedna> LAMBDA_COILGUN_ANIMS = (stack, type) -> {
        if(type == GunAnimationType.EQUIP) return new BusAnimationSedna().addBus("RELOAD", new BusAnimationSequenceSedna().addPos(1, 0, 0, 0).addPos(0, 0, 0, 250));
        if(type == GunAnimationType.CYCLE) return new BusAnimationSedna().addBus("RECOIL", new BusAnimationSequenceSedna().addPos(ItemGunBaseNT.getIsAiming(stack) ? 0.5 : 1, 0, 0, 100).addPos(0, 0, 0, 200));
        if(type == GunAnimationType.RELOAD) return new BusAnimationSedna().addBus("RELOAD", new BusAnimationSequenceSedna().addPos(1, 0, 0, 250).addPos(1, 0, 0, 500).addPos(0, 0, 0, 250));
        return null;
    };

    // CE: XFactoryAccelerator.java
    public static final BiFunction<ItemStack, GunAnimationType, BusAnimationSedna> LAMBDA_NI4NI_ANIMS = (stack, type) -> switch (type) {
        case EQUIP -> new BusAnimationSedna()
                .addBus("EQUIP", new BusAnimationSequenceSedna().addPos(-360 * 2, 0, 0, 500));
        case CYCLE -> {
            boolean aiming = ItemGunBaseNT.getIsAiming(stack);
            yield new BusAnimationSedna()
                    .addBus("RECOIL", new BusAnimationSequenceSedna().addPos(aiming ? -5 : -30, 0, 0, 100, IType.SIN_DOWN).addPos(0, 0, 0, 150, IType.SIN_FULL))
                    .addBus("DRUM", new BusAnimationSequenceSedna().hold(50).addPos(0, 0, 120, 300, IType.SIN_FULL));
        }
        case INSPECT -> new BusAnimationSedna()
                .addBus("EQUIP", new BusAnimationSequenceSedna().addPos(-360 * 3, 0, 0, 750).hold(100).addPos(0, 0, 0, 750));
        default -> null;
    };

    // CE: XFactory12ga.java
    public static final BiFunction<ItemStack, GunAnimationType, BusAnimationSedna> LAMBDA_MARESLEG_ANIMS = (stack, type) -> {
        switch (type) {
            case EQUIP -> {
                return new BusAnimationSedna()
                        .addBus("EQUIP", new BusAnimationSequenceSedna().addPos(-60, 0, 0, 0).addPos(0, 0, -3, 500, IType.SIN_DOWN));
            }
            case CYCLE -> {
                return new BusAnimationSedna()
                        .addBus("RECOIL", new BusAnimationSequenceSedna().addPos(0, 0, 0, 50).addPos(0, 0, -1, 50).addPos(0, 0, 0, 250))
                        .addBus("SIGHT", new BusAnimationSequenceSedna().addPos(35, 0, 0, 100, IType.SIN_DOWN).addPos(0, 0, 0, 100, IType.SIN_FULL))
                        .addBus("LEVER", new BusAnimationSequenceSedna().addPos(0, 0, 0, 600).addPos(-85, 0, 0, 200).addPos(0, 0, 0, 200))
                        .addBus("TURN", new BusAnimationSequenceSedna().addPos(0, 0, 0, 600).addPos(0, 0, 45, 200, IType.SIN_DOWN).addPos(0, 0, 0, 200, IType.SIN_UP))
                        .addBus("HAMMER", new BusAnimationSequenceSedna().addPos(30, 0, 0, 50).addPos(30, 0, 0, 550).addPos(0, 0, 0, 200));
            }
            case CYCLE_DRY -> {
                return new BusAnimationSedna()
                        .addBus("LEVER", new BusAnimationSequenceSedna().addPos(0, 0, 0, 600).addPos(-90, 0, 0, 200).addPos(0, 0, 0, 200))
                        .addBus("TURN", new BusAnimationSequenceSedna().addPos(0, 0, 0, 600).addPos(0, 0, 45, 200, IType.SIN_DOWN).addPos(0, 0, 0, 200, IType.SIN_UP))
                        .addBus("HAMMER", new BusAnimationSequenceSedna().addPos(30, 0, 0, 50).addPos(30, 0, 0, 550).addPos(0, 0, 0, 200));
            }
            case RELOAD -> {
                boolean empty = ((ItemGunBaseNT) stack.getItem()).getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack).getAmount(stack, Minecraft.getInstance().player == null ? null : Minecraft.getInstance().player.getInventory()) <= 0;
                return new BusAnimationSedna()
                        .addBus("LIFT", new BusAnimationSequenceSedna().addPos(30, 0, 0, 400, IType.SIN_FULL))
                        .addBus("LEVER", new BusAnimationSequenceSedna().addPos(0, 0, 0, 400).addPos(-85, 0, 0, 200))
                        .addBus("SHELL", new BusAnimationSequenceSedna().addPos(0, 0, 0, 600).addPos(0, 0.25, -3, 0).addPos(0, empty ? 0.25 : 0.125, -1.5, 150, IType.SIN_UP).addPos(0, empty ? 0.25 : -0.25, 0, 150, IType.SIN_DOWN))
                        .addBus("FLAG", new BusAnimationSequenceSedna().addPos(0, 0, 0, empty ? 900 : 0).addPos(1, 1, 1, 0));
            }
            case RELOAD_CYCLE -> {
                return new BusAnimationSedna()
                        .addBus("LIFT", new BusAnimationSequenceSedna().addPos(30, 0, 0, 0))
                        .addBus("LEVER", new BusAnimationSequenceSedna().addPos(-85, 0, 0, 0))
                        .addBus("SHELL", new BusAnimationSequenceSedna().addPos(0, 0.25, -3, 0).addPos(0, 0.125, -1.5, 150, IType.SIN_UP).addPos(0, -0.125, 0, 150, IType.SIN_DOWN))
                        .addBus("FLAG", new BusAnimationSequenceSedna().addPos(1, 1, 1, 0));
            }
            case RELOAD_END -> {
                return new BusAnimationSedna()
                        .addBus("LIFT", new BusAnimationSequenceSedna().addPos(30, 0, 0, 0).addPos(30, 0, 0, 250).addPos(0, 0, 0, 400, IType.SIN_FULL))
                        .addBus("LEVER", new BusAnimationSequenceSedna().addPos(-85, 0, 0, 0).addPos(0, 0, 0, 200))
                        .addBus("FLAG", new BusAnimationSequenceSedna().addPos(1, 1, 1, 0));
            }
            case JAMMED -> {
                return new BusAnimationSedna()
                        .addBus("LIFT", new BusAnimationSequenceSedna().addPos(30, 0, 0, 0).addPos(30, 0, 0, 250).addPos(0, 0, 0, 400, IType.SIN_FULL))
                        .addBus("LEVER", new BusAnimationSequenceSedna().addPos(-85, 0, 0, 0).addPos(-15, 0, 0, 200).addPos(-15, 0, 0, 650).addPos(-85, 0, 0, 200).addPos(-15, 0, 0, 200).addPos(-15, 0, 0, 200).addPos(-85, 0, 0, 200).addPos(0, 0, 0, 200))
                        .addBus("TURN", new BusAnimationSequenceSedna().addPos(0, 0, 0, 850).addPos(0, 0, 45, 200, IType.SIN_DOWN).addPos(0, 0, 45, 800).addPos(0, 0, 0, 200, IType.SIN_UP))
                        .addBus("FLAG", new BusAnimationSequenceSedna().addPos(1, 1, 1, 0));
            }
            case INSPECT -> {
                return new BusAnimationSedna()
                        .addBus("LIFT", new BusAnimationSequenceSedna().addPos(-35, 0, 0, 300, IType.SIN_FULL).addPos(-35, 0, 0, 1150).addPos(0, 0, 0, 500, IType.SIN_FULL))
                        .addBus("TURN", new BusAnimationSequenceSedna().addPos(0, 0, 0, 450).addPos(0, 0, -90, 500, IType.SIN_FULL).addPos(0, 0, -90, 500).addPos(0, 0, 0, 500, IType.SIN_FULL));
            }
        }

        return null;
    };

    // CE: XFactory12ga.java
    public static final BiFunction<ItemStack, GunAnimationType, BusAnimationSedna> LAMBDA_MARESLEG_SHORT_ANIMS = (stack, type) -> switch (type) {
        case EQUIP -> new BusAnimationSedna()
                .addBus("EQUIP", new BusAnimationSequenceSedna().addPos(-60, 0, 0, 0).addPos(0, 0, -3, 250, IType.SIN_DOWN));
        case CYCLE -> new BusAnimationSedna()
                .addBus("RECOIL", new BusAnimationSequenceSedna().addPos(0, 0, 0, 50).addPos(0, 0, -1, 50).addPos(0, 0, 0, 250))
                .addBus("SIGHT", new BusAnimationSequenceSedna().addPos(35, 0, 0, 100, IType.SIN_DOWN).addPos(0, 0, 0, 100, IType.SIN_FULL))
                .addBus("LEVER", new BusAnimationSequenceSedna().addPos(0, 0, 0, 600).addPos(-85, 0, 0, 200).addPos(0, 0, 0, 200))
                .addBus("HAMMER", new BusAnimationSequenceSedna().addPos(30, 0, 0, 50).addPos(30, 0, 0, 550).addPos(0, 0, 0, 200))
                .addBus("FLIP", new BusAnimationSequenceSedna().addPos(0, 0, 0, 600).addPos(360, 0, 0, 400))
                .addBus("SHELL", new BusAnimationSequenceSedna().addPos(-20, 0, 0, 0)); //gets rid of the shell in the barrel during cycling
        case CYCLE_DRY -> new BusAnimationSedna()
                .addBus("LEVER", new BusAnimationSequenceSedna().addPos(0, 0, 0, 600).addPos(-90, 0, 0, 200).addPos(0, 0, 0, 200))
                .addBus("HAMMER", new BusAnimationSequenceSedna().addPos(30, 0, 0, 50).addPos(30, 0, 0, 550).addPos(0, 0, 0, 200))
                .addBus("FLIP", new BusAnimationSequenceSedna().addPos(0, 0, 0, 600).addPos(360, 0, 0, 400))
                .addBus("SHELL", new BusAnimationSequenceSedna().addPos(-20, 0, 0, 0));
        case JAMMED -> new BusAnimationSedna()
                .addBus("LIFT", new BusAnimationSequenceSedna().addPos(30, 0, 0, 0).addPos(30, 0, 0, 250).addPos(0, 0, 0, 400, IType.SIN_FULL))
                .addBus("LEVER", new BusAnimationSequenceSedna().addPos(-85, 0, 0, 0).addPos(-15, 0, 0, 200).addPos(-15, 0, 0, 650).addPos(-85, 0, 0, 200).addPos(-15, 0, 0, 200).addPos(-15, 0, 0, 200).addPos(-85, 0, 0, 200).addPos(0, 0, 0, 200))
                .addBus("FLAG", new BusAnimationSequenceSedna().addPos(1, 1, 1, 0));
        default -> LAMBDA_MARESLEG_ANIMS.apply(stack, type);
    };

    // CE: XFactory12ga.java
    public static final BiFunction<ItemStack, GunAnimationType, BusAnimationSedna> LAMBDA_LIBERATOR_ANIMS = (stack, type) -> {
        int ammo = ((ItemGunBaseNT) stack.getItem()).getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack).getAmount(stack, Minecraft.getInstance().player == null ? null : Minecraft.getInstance().player.getInventory());
        switch(type) {
            case EQUIP: return new BusAnimationSedna()
                    .addBus("EQUIP", new BusAnimationSequenceSedna().addPos(60, 0, 0, 0).addPos(0, 0, 0, 500, IType.SIN_DOWN));
            case CYCLE: return new BusAnimationSedna()
                    .addBus("RECOIL", new BusAnimationSequenceSedna().addPos(0, 0, -2.5, 50, IType.SIN_DOWN).addPos(0, 0, 0, 350, IType.SIN_FULL));
            case CYCLE_DRY: return new BusAnimationSedna();
            case RELOAD: if(ammo == 0) return new BusAnimationSedna()
                    .addBus("LATCH", new BusAnimationSequenceSedna().addPos(15, 0, 0, 100))
                    .addBus("BREAK", new BusAnimationSequenceSedna().addPos(0, 0, 0, 100).addPos(60, 0, 0, 350, IType.SIN_DOWN))
                    .addBus("SHELL1", new BusAnimationSequenceSedna().addPos(2, -4, -2, 0).addPos(2, -4, -2, 400).addPos(0, 0, -2, 450, IType.SIN_FULL).addPos(0, 0, 0, 50, IType.SIN_UP))
                    .addBus("SHELL2", new BusAnimationSequenceSedna().addPos(2, -4, -2, 0))
                    .addBus("SHELL3", new BusAnimationSequenceSedna().addPos(2, -4, -2, 0))
                    .addBus("SHELL4", new BusAnimationSequenceSedna().addPos(2, -4, -2, 0));
                if(ammo == 1) return new BusAnimationSedna()
                        .addBus("LATCH", new BusAnimationSequenceSedna().addPos(15, 0, 0, 100))
                        .addBus("BREAK", new BusAnimationSequenceSedna().addPos(0, 0, 0, 100).addPos(60, 0, 0, 350, IType.SIN_DOWN))
                        .addBus("SHELL1", new BusAnimationSequenceSedna().addPos(0, 0, 0, 0))
                        .addBus("SHELL2", new BusAnimationSequenceSedna().addPos(2, -4, -2, 0).addPos(2, -4, -2, 400).addPos(0, 0, -2, 450, IType.SIN_FULL).addPos(0, 0, 0, 50, IType.SIN_UP))
                        .addBus("SHELL3", new BusAnimationSequenceSedna().addPos(2, -4, -2, 0))
                        .addBus("SHELL4", new BusAnimationSequenceSedna().addPos(2, -4, -2, 0));
                if(ammo == 2) return new BusAnimationSedna()
                        .addBus("LATCH", new BusAnimationSequenceSedna().addPos(15, 0, 0, 100))
                        .addBus("BREAK", new BusAnimationSequenceSedna().addPos(0, 0, 0, 100).addPos(60, 0, 0, 350, IType.SIN_DOWN))
                        .addBus("SHELL1", new BusAnimationSequenceSedna().addPos(0, 0, 0, 0))
                        .addBus("SHELL2", new BusAnimationSequenceSedna().addPos(0, 0, 0, 0))
                        .addBus("SHELL3", new BusAnimationSequenceSedna().addPos(2, -4, -2, 0).addPos(2, -4, -2, 400).addPos(0, 0, -2, 450, IType.SIN_FULL).addPos(0, 0, 0, 50, IType.SIN_UP))
                        .addBus("SHELL4", new BusAnimationSequenceSedna().addPos(2, -4, -2, 0));
                if(ammo == 3) return new BusAnimationSedna()
                        .addBus("LATCH", new BusAnimationSequenceSedna().addPos(15, 0, 0, 100))
                        .addBus("BREAK", new BusAnimationSequenceSedna().addPos(0, 0, 0, 100).addPos(60, 0, 0, 350, IType.SIN_DOWN))
                        .addBus("SHELL1", new BusAnimationSequenceSedna().addPos(0, 0, 0, 0))
                        .addBus("SHELL2", new BusAnimationSequenceSedna().addPos(0, 0, 0, 0))
                        .addBus("SHELL3", new BusAnimationSequenceSedna().addPos(0, 0, 0, 0))
                        .addBus("SHELL4", new BusAnimationSequenceSedna().addPos(2, -4, -2, 0).addPos(2, -4, -2, 400).addPos(0, 0, -2, 450, IType.SIN_FULL).addPos(0, 0, 0, 50, IType.SIN_UP));
            case RELOAD_CYCLE:
                if(ammo == 0) return new BusAnimationSedna()
                        .addBus("LATCH", new BusAnimationSequenceSedna().addPos(15, 0, 0, 0))
                        .addBus("BREAK", new BusAnimationSequenceSedna().addPos(60, 0, 0, 0))
                        .addBus("SHELL1", new BusAnimationSequenceSedna().addPos(0, 0, 0, 0))
                        .addBus("SHELL2", new BusAnimationSequenceSedna().addPos(2, -4, -2, 0).addPos(0, 0, -2, 450, IType.SIN_FULL).addPos(0, 0, 0, 50, IType.SIN_UP))
                        .addBus("SHELL3", new BusAnimationSequenceSedna().addPos(2, -4, -2, 0))
                        .addBus("SHELL4", new BusAnimationSequenceSedna().addPos(2, -4, -2, 0));
                if(ammo == 1) return new BusAnimationSedna()
                        .addBus("LATCH", new BusAnimationSequenceSedna().addPos(15, 0, 0, 0))
                        .addBus("BREAK", new BusAnimationSequenceSedna().addPos(60, 0, 0, 0))
                        .addBus("SHELL1", new BusAnimationSequenceSedna().addPos(0, 0, 0, 0))
                        .addBus("SHELL2", new BusAnimationSequenceSedna().addPos(0, 0, 0, 0))
                        .addBus("SHELL3", new BusAnimationSequenceSedna().addPos(2, -4, -2, 0).addPos(0, 0, -2, 450, IType.SIN_FULL).addPos(0, 0, 0, 50, IType.SIN_UP))
                        .addBus("SHELL4", new BusAnimationSequenceSedna().addPos(2, -4, -2, 0));
                if(ammo == 2) return new BusAnimationSedna()
                        .addBus("LATCH", new BusAnimationSequenceSedna().addPos(15, 0, 0, 0))
                        .addBus("BREAK", new BusAnimationSequenceSedna().addPos(60, 0, 0, 0))
                        .addBus("SHELL1", new BusAnimationSequenceSedna().addPos(0, 0, 0, 0))
                        .addBus("SHELL2", new BusAnimationSequenceSedna().addPos(0, 0, 0, 0))
                        .addBus("SHELL3", new BusAnimationSequenceSedna().addPos(0, 0, 0, 0))
                        .addBus("SHELL4", new BusAnimationSequenceSedna().addPos(2, -4, -2, 0).addPos(0, 0, -2, 450, IType.SIN_FULL).addPos(0, 0, 0, 50, IType.SIN_UP));
                return null;
            case RELOAD_END: return new BusAnimationSedna()
                    .addBus("LATCH", new BusAnimationSequenceSedna().addPos(15, 0, 0, 0).addPos(15, 0, 0, 250).addPos(0, 0, 0, 50))
                    .addBus("BREAK", new BusAnimationSequenceSedna().addPos(60, 0, 0, 0).addPos(0, 0, 0, 250, IType.SIN_UP))
                    .addBus(ammo >= 0 ? "SHELL1" : "NULL", new BusAnimationSequenceSedna().addPos(0, 0, 0, 0))
                    .addBus(ammo >= 1 ? "SHELL2" : "NULL", new BusAnimationSequenceSedna().addPos(0, 0, 0, 0))
                    .addBus(ammo >= 2 ? "SHELL3" : "NULL", new BusAnimationSequenceSedna().addPos(0, 0, 0, 0))
                    .addBus(ammo >= 3 ? "SHELL4" : "NULL", new BusAnimationSequenceSedna().addPos(0, 0, 0, 0))
                    .addBus(ammo < 0 ? "SHELL1" : "NULL", new BusAnimationSequenceSedna().addPos(2, -8, -2, 0))
                    .addBus(ammo < 1 ? "SHELL2" : "NULL", new BusAnimationSequenceSedna().addPos(2, -8, -2, 0))
                    .addBus(ammo < 2 ? "SHELL3" : "NULL", new BusAnimationSequenceSedna().addPos(2, -8, -2, 0))
                    .addBus(ammo < 3 ? "SHELL4" : "NULL", new BusAnimationSequenceSedna().addPos(2, -8, -2, 0));
            case JAMMED: return new BusAnimationSedna()
                    .addBus("LATCH", new BusAnimationSequenceSedna().addPos(15, 0, 0, 0).addPos(15, 0, 0, 250).addPos(0, 0, 0, 50).addPos(0, 0, 0, 550).addPos(15, 0, 0, 100).addPos(15, 0, 0, 600).addPos(0, 0, 0, 50))
                    .addBus("BREAK", new BusAnimationSequenceSedna().addPos(60, 0, 0, 0).addPos(0, 0, 0, 250, IType.SIN_UP).addPos(0, 0, 0, 600).addPos(45, 0, 0, 250, IType.SIN_DOWN).addPos(45, 0, 0, 300).addPos(0, 0, 0, 150, IType.SIN_UP))
                    .addBus(ammo >= 0 ? "SHELL1" : "NULL", new BusAnimationSequenceSedna().addPos(0, 0, 0, 0))
                    .addBus(ammo >= 1 ? "SHELL2" : "NULL", new BusAnimationSequenceSedna().addPos(0, 0, 0, 0))
                    .addBus(ammo >= 2 ? "SHELL3" : "NULL", new BusAnimationSequenceSedna().addPos(0, 0, 0, 0))
                    .addBus(ammo >= 3 ? "SHELL4" : "NULL", new BusAnimationSequenceSedna().addPos(0, 0, 0, 0))
                    .addBus(ammo < 0 ? "SHELL1" : "NULL", new BusAnimationSequenceSedna().addPos(2, -8, -2, 0))
                    .addBus(ammo < 1 ? "SHELL2" : "NULL", new BusAnimationSequenceSedna().addPos(2, -8, -2, 0))
                    .addBus(ammo < 2 ? "SHELL3" : "NULL", new BusAnimationSequenceSedna().addPos(2, -8, -2, 0))
                    .addBus(ammo < 3 ? "SHELL4" : "NULL", new BusAnimationSequenceSedna().addPos(2, -8, -2, 0));
            case INSPECT: return new BusAnimationSedna()
                    .addBus("LATCH", new BusAnimationSequenceSedna().addPos(15, 0, 0, 100).addPos(15, 0, 0, 1100).addPos(0, 0, 0, 50))
                    .addBus("BREAK", new BusAnimationSequenceSedna().addPos(0, 0, 0, 100).addPos(60, 0, 0, 350, IType.SIN_DOWN).addPos(60, 0, 0, 500).addPos(0, 0, 0, 250, IType.SIN_UP))
                    .addBus(ammo > 0 ? "SHELL1" : "NULL", new BusAnimationSequenceSedna().addPos(0, 0, 0, 0))
                    .addBus(ammo > 1 ? "SHELL2" : "NULL", new BusAnimationSequenceSedna().addPos(0, 0, 0, 0))
                    .addBus(ammo > 2 ? "SHELL3" : "NULL", new BusAnimationSequenceSedna().addPos(0, 0, 0, 0))
                    .addBus(ammo > 3 ? "SHELL4" : "NULL", new BusAnimationSequenceSedna().addPos(0, 0, 0, 0))
                    .addBus(ammo < 1 ? "SHELL1" : "NULL", new BusAnimationSequenceSedna().addPos(2, -8, -2, 0))
                    .addBus(ammo < 2 ? "SHELL2" : "NULL", new BusAnimationSequenceSedna().addPos(2, -8, -2, 0))
                    .addBus(ammo < 3 ? "SHELL3" : "NULL", new BusAnimationSequenceSedna().addPos(2, -8, -2, 0))
                    .addBus(ammo < 4 ? "SHELL4" : "NULL", new BusAnimationSequenceSedna().addPos(2, -8, -2, 0));
        }

        return null;
    };

    // CE: XFactory12ga.java
    public static final BiFunction<ItemStack, GunAnimationType, BusAnimationSedna> LAMBDA_SPAS_ANIMS = (stack, type) -> {
        switch (type) {
            case EQUIP -> {
                return new BusAnimationSedna()
                        .addBus("EQUIP", new BusAnimationSequenceSedna().addPos(-60, 0, 0, 0).addPos(0, 0, -3, 500, IType.SIN_DOWN));
            }
            case CYCLE -> {
                return GunModels.json("spas_12_anim").get("Fire");
            }
            case CYCLE_DRY -> {
                return GunModels.json("spas_12_anim").get("FireDry");
            }
            case ALT_CYCLE -> {
                return GunModels.json("spas_12_anim").get("FireAlt");
            }
            case RELOAD -> {
                boolean empty = ((ItemGunBaseNT) stack.getItem()).getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack).getAmount(stack, Minecraft.getInstance().player == null ? null : Minecraft.getInstance().player.getInventory()) <= 0;
                return GunModels.json("spas_12_anim").get(empty ? "ReloadEmptyStart" : "ReloadStart");
            }
            case RELOAD_CYCLE -> {
                return GunModels.json("spas_12_anim").get("Reload");
            }
            case RELOAD_END -> {
                return GunModels.json("spas_12_anim").get("ReloadEnd");
            }
            case JAMMED -> {
                return GunModels.json("spas_12_anim").get("Jammed");
            }
            case INSPECT -> {
                return GunModels.json("spas_12_anim").get("Inspect");
            }
        }

        return null;
    };

    // CE: XFactory12ga.java
    public static final BiFunction<ItemStack, GunAnimationType, BusAnimationSedna> LAMBDA_SHREDDER_ANIMS = (stack, type) -> switch (type) {
        case EQUIP -> new BusAnimationSedna()
                .addBus("EQUIP", new BusAnimationSequenceSedna().addPos(60, 0, 0, 0).addPos(0, 0, 0, 500, IType.SIN_DOWN));
        case CYCLE -> new BusAnimationSedna()
                .addBus("RECOIL", new BusAnimationSequenceSedna().addPos(0, 0, -1, 50, IType.SIN_DOWN).addPos(0, 0, 0, 150, IType.SIN_FULL))
                .addBus("CYCLE", new BusAnimationSequenceSedna().addPos(0, 0, 0, 150).addPos(0, 0, 18, 100));
        case CYCLE_DRY -> new BusAnimationSedna()
                .addBus("CYCLE", new BusAnimationSequenceSedna().addPos(0, 0, 0, 150).addPos(0, 0, 18, 100));
        case RELOAD -> new BusAnimationSedna()
                .addBus("MAG", new BusAnimationSequenceSedna().addPos(0, -8, 0, 250, IType.SIN_UP).addPos(0, -8, 0, 1000).addPos(0, 0, 0, 300))
                .addBus("LIFT", new BusAnimationSequenceSedna().addPos(0, 0, 0, 750).addPos(-25, 0, 0, 300, IType.SIN_FULL).addPos(-25, 0, 0, 500).addPos(-27, 0, 0, 100, IType.SIN_DOWN).addPos(-25, 0, 0, 100, IType.SIN_FULL).addPos(-25, 0, 0, 150).addPos(0, 0, 0, 300, IType.SIN_FULL));
        case JAMMED -> new BusAnimationSedna()
                .addBus("MAG", new BusAnimationSequenceSedna().addPos(0, 0, 0, 500).addPos(0, -2, 0, 150, IType.SIN_UP).addPos(0, 0, 0, 100))
                .addBus("LIFT", new BusAnimationSequenceSedna().addPos(0, 0, 0, 750).addPos(-2, 0, 0, 100, IType.SIN_DOWN).addPos(0, 0, 0, 100, IType.SIN_FULL));
        case INSPECT -> new BusAnimationSedna()
                .addBus("MAG", new BusAnimationSequenceSedna()
                        .addPos(0, -1, 0, 150).addPos(6, -1, 0, 150).addPos(6, 12, 0, 350, IType.SIN_DOWN).addPos(6, -2, 0, 350, IType.SIN_UP).addPos(6, -1, 0, 50)
                        .addPos(6, -1, 0, 100).addPos(0, -1, 0, 150, IType.SIN_FULL).addPos(0, 0, 0, 150, IType.SIN_UP))
                .addBus("SPEEN", new BusAnimationSequenceSedna().addPos(0, 0, 0, 300).addPos(360, 0, 0, 700))
                .addBus("LIFT", new BusAnimationSequenceSedna().addPos(0, 0, 0, 1450).addPos(-2, 0, 0, 100, IType.SIN_DOWN).addPos(0, 0, 0, 100, IType.SIN_FULL));
        default -> null;
    };

    // CE: XFactory12ga.java
    public static final BiFunction<ItemStack, GunAnimationType, BusAnimationSedna> LAMBDA_SEXY_ANIMS = (stack, type) -> {
        switch (type) {
            case EQUIP -> {
                return new BusAnimationSedna()
                        .addBus("EQUIP", new BusAnimationSequenceSedna().addPos(45, 0, 0, 0).addPos(0, 0, 0, 1000, IType.SIN_DOWN));
            }
            case CYCLE -> {
                int amount = ((ItemGunBaseNT) stack.getItem()).getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack).getAmount(stack, null);
                return new BusAnimationSedna()
                        .addBus("RECOIL", new BusAnimationSequenceSedna().hold(50).addPos(0, 0, -0.25, 50, IType.SIN_DOWN).addPos(0, 0, 0, 100, IType.SIN_FULL))
                        .addBus("BARREL", new BusAnimationSequenceSedna().addPos(0, 0, -1, 50, IType.SIN_DOWN).addPos(0, 0, 0, 150))
                        .addBus("CYCLE", new BusAnimationSequenceSedna().addPos(1, 0, 0, 150))
                        .addBus("HOOD", new BusAnimationSequenceSedna().hold(50).addPos(3, 0, 0, 50, IType.SIN_DOWN).addPos(0, 0, 0, 50, IType.SIN_UP))
                        .addBus("SHELLS", new BusAnimationSequenceSedna().setPos(amount - 1, 0, 0));
            }
            case CYCLE_DRY -> {
                return new BusAnimationSedna()
                        .addBus("CYCLE", new BusAnimationSequenceSedna().addPos(0, 0, 18, 50));
            }
            case RELOAD -> {
                return new BusAnimationSedna()
                        .addBus("LOWER", new BusAnimationSequenceSedna().addPos(15, 0, 0, 500, IType.SIN_FULL).hold(2750).addPos(12, 0, 0, 100, IType.SIN_DOWN).addPos(15, 0, 0, 100, IType.SIN_FULL).hold(1050).addPos(18, 0, 0, 100, IType.SIN_DOWN).addPos(15, 0, 0, 100, IType.SIN_FULL).hold(300).addPos(0, 0, 0, 500, IType.SIN_FULL))
                        .addBus("LEVER", new BusAnimationSequenceSedna().addPos(0, 0, 1, 150).hold(4700).addPos(0, 0, 0, 150))
                        .addBus("HOOD", new BusAnimationSequenceSedna().hold(250).addPos(60, 0, 0, 500, IType.SIN_FULL).hold(3250).addPos(0, 0, 0, 500, IType.SIN_UP))
                        .addBus("BELT", new BusAnimationSequenceSedna().setPos(1, 0, 0).hold(750).addPos(0, 0, 0, 500, IType.SIN_UP).hold(2000).addPos(1, 0, 0, 500, IType.SIN_UP))
                        .addBus("MAG", new BusAnimationSequenceSedna().hold(1500).addPos(0, -1, 0, 250, IType.SIN_UP).addPos(2, -1, 0, 500, IType.SIN_UP).addPos(7, 1, 0, 250, IType.SIN_UP).addPos(15, 2, 0, 250).setPos(0, -2, 0).addPos(0, 0, 0, 500, IType.SIN_UP))
                        .addBus("MAGROT", new BusAnimationSequenceSedna().hold(2250).addPos(0, 0, -180, 500, IType.SIN_FULL).setPos(0, 0, 0));
            }
            case INSPECT -> {
                return new BusAnimationSedna()
                        .addBus("BOTTLE", new BusAnimationSequenceSedna().setPos(8, -8, -2).addPos(6, -4, -2, 500, IType.SIN_DOWN).addPos(3, -3, -5, 500, IType.SIN_FULL).addPos(3, -2, -5, 1000).addPos(4, -6, -2, 750, IType.SIN_FULL).addPos(6, -8, -2, 500, IType.SIN_UP))
                        .addBus("SIP", new BusAnimationSequenceSedna().setPos(25, 0, 0).hold(500).addPos(-90, 0, 0, 500, IType.SIN_FULL).addPos(-110, 0, 0, 1000).addPos(25, 0, 0, 750, IType.SIN_FULL));
            }
        }

		return null;
	};

    // CE: XFactory44.java
    public static final BiFunction<ItemStack, GunAnimationType, BusAnimationSedna> LAMBDA_HENRY_ANIMS = (stack, type) -> {
        switch (type) {
            case EQUIP -> {
                return new BusAnimationSedna()
                        .addBus("EQUIP", new BusAnimationSequenceSedna().addPos(-90, 0, 0, 0).addPos(0, 0, -3, 350, IType.SIN_DOWN))
                        .addBus("SIGHT", new BusAnimationSequenceSedna().addPos(80, 0, 0, 0).addPos(80, 0, 0, 500).addPos(0, 0, -3, 250, IType.SIN_DOWN));
            }
            case CYCLE -> {
                return new BusAnimationSedna()
                        .addBus("RECOIL", new BusAnimationSequenceSedna().addPos(0, 0, 0, 50).addPos(0, 0, -1, 50).addPos(0, 0, 0, 250))
                        .addBus("SIGHT", new BusAnimationSequenceSedna().addPos(35, 0, 0, 100, IType.SIN_DOWN).addPos(0, 0, 0, 100, IType.SIN_FULL))
                        .addBus("LEVER", new BusAnimationSequenceSedna().addPos(0, 0, 0, 600).addPos(-90, 0, 0, 200).addPos(0, 0, 0, 200))
                        .addBus("TURN", new BusAnimationSequenceSedna().addPos(0, 0, 0, 600).addPos(0, 0, 45, 200, IType.SIN_DOWN).addPos(0, 0, 0, 200, IType.SIN_UP))
                        .addBus("HAMMER", new BusAnimationSequenceSedna().addPos(30, 0, 0, 50).addPos(30, 0, 0, 550).addPos(0, 0, 0, 200));
            }
            case CYCLE_DRY -> {
                return new BusAnimationSedna()
                        .addBus("LEVER", new BusAnimationSequenceSedna().addPos(0, 0, 0, 600).addPos(-90, 0, 0, 200).addPos(0, 0, 0, 200))
                        .addBus("TURN", new BusAnimationSequenceSedna().addPos(0, 0, 0, 600).addPos(0, 0, 45, 200, IType.SIN_DOWN).addPos(0, 0, 0, 200, IType.SIN_UP))
                        .addBus("HAMMER", new BusAnimationSequenceSedna().addPos(30, 0, 0, 50).addPos(30, 0, 0, 550).addPos(0, 0, 0, 200));
            }
            case RELOAD -> {
                return new BusAnimationSedna()
                        .addBus("LIFT", new BusAnimationSequenceSedna().addPos(-60, 0, 0, 400, IType.SIN_FULL))
                        .addBus("TWIST", new BusAnimationSequenceSedna().addPos(0, 0, 0, 500).addPos(0, 0, -90, 200, IType.SIN_FULL))
                        .addBus("BULLET", new BusAnimationSequenceSedna().addPos(0, 0, 0, 700).addPos(3, 0, -6, 0).addPos(0, 0, 1, 300, IType.SIN_FULL).addPos(0, 0, 0, 250, IType.SIN_FULL));
            }
            case RELOAD_CYCLE -> {
                return new BusAnimationSedna()
                        .addBus("LIFT", new BusAnimationSequenceSedna().addPos(-60, 0, 0, 0))
                        .addBus("TWIST", new BusAnimationSequenceSedna().addPos(0, 0, -90, 0))
                        .addBus("BULLET", new BusAnimationSequenceSedna().addPos(3, 0, -6, 0).addPos(0, 0, 1, 300, IType.SIN_FULL).addPos(0, 0, 0, 250, IType.SIN_FULL));
            }
            case RELOAD_END -> {
                boolean empty = ((ItemGunBaseNT) stack.getItem()).getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack).getAmountBeforeReload(stack) <= 0;
                return new BusAnimationSedna()
                        .addBus("LIFT", new BusAnimationSequenceSedna().addPos(-60, 0, 0, 0).addPos(-60, 0, 0, 300).addPos(0, 0, 0, 400, IType.SIN_FULL))
                        .addBus("TWIST", new BusAnimationSequenceSedna().addPos(0, 0, -90, 0).addPos(0, 0, 0, 200, IType.SIN_FULL))
                        .addBus("LEVER", new BusAnimationSequenceSedna().addPos(0, 0, 0, 700).addPos(empty ? -90 : 0, 0, 0, 200).addPos(0, 0, 0, 200))
                        .addBus("TURN", new BusAnimationSequenceSedna().addPos(0, 0, 0, 700).addPos(0, 0, empty ? 45 : 0, 200, IType.SIN_DOWN).addPos(0, 0, 0, 200, IType.SIN_UP));
            }
            case JAMMED -> {
                return new BusAnimationSedna()
                        .addBus("LIFT", new BusAnimationSequenceSedna().addPos(-60, 0, 0, 0).addPos(-60, 0, 0, 300).addPos(0, 0, 0, 400, IType.SIN_FULL))
                        .addBus("TWIST", new BusAnimationSequenceSedna().addPos(0, 0, -90, 0).addPos(0, 0, 0, 200, IType.SIN_FULL))
                        .addBus("LEVER", new BusAnimationSequenceSedna().addPos(0, 0, 0, 700).addPos(-90, 0, 0, 200).addPos(0, 0, 0, 200).addPos(0, 0, 0, 500).addPos(-90, 0, 0, 200).addPos(0, 0, 0, 200).addPos(0, 0, 0, 200).addPos(-90, 0, 0, 200).addPos(0, 0, 0, 200))
                        .addBus("TURN", new BusAnimationSequenceSedna().addPos(0, 0, 0, 700).addPos(0, 0, 45, 200, IType.SIN_DOWN).addPos(0, 0, 0, 200, IType.SIN_UP).addPos(0, 0, 0, 500).addPos(0, 0, 45, 200, IType.SIN_FULL).addPos(0, 0, 45, 600).addPos(0, 0, 0, 200, IType.SIN_FULL));
            }
            case INSPECT -> {
                return new BusAnimationSedna()
                        .addBus("YEET", new BusAnimationSequenceSedna().addPos(0, 2, 0, 200, IType.SIN_DOWN).addPos(0, 0, 0, 200, IType.SIN_UP))
                        .addBus("ROLL", new BusAnimationSequenceSedna().addPos(0, 0, 360, 400));
            }
        }

        return null;
    };

    // CE: XFactory44.java
    public static final BiFunction<ItemStack, GunAnimationType, BusAnimationSedna> LAMBDA_NOPIP_ANIMS = (stack, type) -> switch (type) {
        case CYCLE -> new BusAnimationSedna()
                .addBus("RECOIL", new BusAnimationSequenceSedna().addPos(0, 0, 0, 50).addPos(0, 0, -3, 50).addPos(0, 0, 0, 250))
                .addBus("HAMMER", new BusAnimationSequenceSedna().addPos(0, 0, 1, 50).addPos(0, 0, 1, 400).addPos(0, 0, 0, 200))
                .addBus("DRUM", new BusAnimationSequenceSedna().addPos(0, 0, 0, 450).addPos(0, 0, 1, 200));
        case CYCLE_DRY -> new BusAnimationSedna()
                .addBus("HAMMER", new BusAnimationSequenceSedna().addPos(0, 0, 1, 50).addPos(0, 0, 1, 300 + 100).addPos(0, 0, 0, 200))
                .addBus("DRUM", new BusAnimationSequenceSedna().addPos(0, 0, 0, 450).addPos(0, 0, 1, 200));
        case EQUIP ->
                new BusAnimationSedna().addBus("ROTATE", new BusAnimationSequenceSedna().addPos(90, 0, 0, 0).addPos(0, 0, 0, 500, IType.SIN_DOWN));
        case RELOAD -> new BusAnimationSedna()
                .addBus("RELAOD_TILT", new BusAnimationSequenceSedna().addPos(-15, 0, 0, 100).addPos(65, 0, 0, 100).addPos(45, 0, 0, 50).addPos(0, 0, 0, 200).addPos(0, 0, 0, 1450).addPos(-80, 0, 0, 100).addPos(-80, 0, 0, 100).addPos(0, 0, 0, 200))
                .addBus("RELOAD_CYLINDER", new BusAnimationSequenceSedna().addPos(0, 0, 0, 200).addPos(90, 0, 0, 100).addPos(90, 0, 0, 1700).addPos(0, 0, 0, 70))
                .addBus("RELOAD_LIFT", new BusAnimationSequenceSedna().addPos(0, 0, 0, 350).addPos(-45, 0, 0, 250).addPos(-45, 0, 0, 350).addPos(-15, 0, 0, 200).addPos(-15, 0, 0, 1050).addPos(0, 0, 0, 100))
                .addBus("RELOAD_JOLT", new BusAnimationSequenceSedna().addPos(0, 0, 0, 600).addPos(2, 0, 0, 50).addPos(0, 0, 0, 100))
                .addBus("RELOAD_BULLETS", new BusAnimationSequenceSedna().addPos(0, 0, 0, 650).addPos(10, 0, 0, 300).addPos(10, 0, 0, 200).addPos(0, 0, 0, 700))
                .addBus("RELOAD_BULLETS_CON", new BusAnimationSequenceSedna().addPos(1, 0, 0, 0).addPos(1, 0, 0, 950).addPos(0, 0, 0, 1));
        case INSPECT, JAMMED -> new BusAnimationSedna()
                .addBus("RELAOD_TILT", new BusAnimationSequenceSedna().addPos(-15, 0, 0, 100).addPos(65, 0, 0, 100).addPos(45, 0, 0, 50).addPos(0, 0, 0, 200).addPos(0, 0, 0, 200).addPos(-80, 0, 0, 100).addPos(-80, 0, 0, 100).addPos(0, 0, 0, 200))
                .addBus("RELOAD_CYLINDER", new BusAnimationSequenceSedna().addPos(0, 0, 0, 200).addPos(90, 0, 0, 100).addPos(90, 0, 0, 450).addPos(0, 0, 0, 70));
        default -> null;
    };

    // CE: XFactory44.java
    public static final BiFunction<ItemStack, GunAnimationType, BusAnimationSedna> LAMBDA_LILMAC_ANIMS = (stack, type) -> switch (type) {
        case EQUIP ->
                new BusAnimationSedna().addBus("SPIN", new BusAnimationSequenceSedna().addPos(-360, 0, 0, 350));
        default -> LAMBDA_NOPIP_ANIMS.apply(stack, type);
    };

    // CE: XFactory44.java
    public static final BiFunction<ItemStack, GunAnimationType, BusAnimationSedna> LAMBDA_HANGMAN_ANIMS = (stack, type) -> switch (type) {
        case EQUIP ->
                new BusAnimationSedna().addBus("EQUIP", new BusAnimationSequenceSedna().addPos(60, 0, 0, 0).addPos(0, 0, 0, 500, IType.SIN_DOWN));
        case CYCLE -> new BusAnimationSedna()
                .addBus("RECOIL", new BusAnimationSequenceSedna().addPos(0, 0, 0, 50).addPos(0, 0, -3, 50).addPos(0, 0, 0, 250));
        case RELOAD -> new BusAnimationSedna()
                .addBus("LID", new BusAnimationSequenceSedna().addPos(0, 0, -90, 250).addPos(0, 0, -90, 1500).addPos(0, 0, 0, 250))
                .addBus("MAG", new BusAnimationSequenceSedna().addPos(0, 0, 0, 250).addPos(0, -10, 0, 250, IType.SIN_UP).addPos(0, -10, 0, 500).addPos(0, 0, 0, 350, IType.SIN_FULL))
                .addBus("BULLETS", new BusAnimationSequenceSedna().addPos(1, 1, 1, 0).addPos(0, 0, 0, 500))
                .addBus("EQUIP", new BusAnimationSequenceSedna().addPos(-15, 0, 0, 500, IType.SIN_FULL).addPos(-15, 0, 0, 850).addPos(-25, 0, 0, 100, IType.SIN_DOWN).addPos(0, 0, 0, 350, IType.SIN_FULL))
                .addBus("ROLL", new BusAnimationSequenceSedna().addPos(0, 0, 0, 500).addPos(0, 0, 25, 250, IType.SIN_FULL).addPos(0, 0, 25, 1000).addPos(0, 0, 0, 250, IType.SIN_FULL));
        case INSPECT -> new BusAnimationSedna()
                .addBus("TURN", new BusAnimationSequenceSedna().addPos(0, 170, 0, 500, IType.SIN_UP).addPos(0, 170, 0, 550).addPos(0, 0, 0, 500, IType.SIN_FULL))
                .addBus("ROLL", new BusAnimationSequenceSedna().addPos(0, 0, 110, 500, IType.SIN_FULL).addPos(0, 0, 110, 550).addPos(0, 0, 0, 500, IType.SIN_FULL))
                .addBus("SMACK", new BusAnimationSequenceSedna().addPos(0, 0, 0, 500).addPos(0, 0, 1, 150, IType.SIN_DOWN).addPos(0, 0, -3, 150, IType.SIN_UP).addPos(0, 0, 0, 350, IType.SIN_FULL));
        case JAMMED -> new BusAnimationSedna()
                .addBus("LID", new BusAnimationSequenceSedna().addPos(0, 0, 0, 500).addPos(0, 0, -90, 250).addPos(0, 0, -90, 300).addPos(0, 0, 0, 250))
                .addBus("MAG", new BusAnimationSequenceSedna().addPos(0, 0, 0, 500).addPos(0, 0, 0, 250).addPos(0, -3, 0, 150, IType.SIN_UP).addPos(0, 0, 0, 150, IType.SIN_FULL))
                .addBus("EQUIP", new BusAnimationSequenceSedna().addPos(0, 0, 0, 1000).addPos(-10, 0, 0, 100, IType.SIN_DOWN).addPos(0, 0, 0, 350, IType.SIN_FULL))
                .addBus("ROLL", new BusAnimationSequenceSedna().addPos(0, 0, 0, 500).addPos(0, 0, 25, 250, IType.SIN_FULL).addPos(0, 0, 25, 300).addPos(0, 0, 0, 250, IType.SIN_FULL));
        default -> null;
    };

    // CE: XFactoryFlamer.java
    public static final BiFunction<ItemStack, GunAnimationType, BusAnimationSedna> LAMBDA_FLAMER_ANIMS = (stack, type) -> {
        switch(type) {
            case EQUIP: return new BusAnimationSedna()
                    .addBus("EQUIP", new BusAnimationSequenceSedna().addPos(-45, 0, 0, 0).addPos(0, 0, 0, 500, IType.SIN_DOWN));
            case RELOAD: return GunModels.json("flamethrower_anim").get("Reload");
            case INSPECT:
            case JAMMED: return new BusAnimationSedna()
                    .addBus("ROTATE", new BusAnimationSequenceSedna().addPos(0, 0, 45, 250, IType.SIN_FULL).addPos(0, 0, 45, 350).addPos(0, 0, -15, 150, IType.SIN_FULL).addPos(0, 0, 0, 100, IType.SIN_FULL));
        }

        return null;
    };

    // CE: XFactoryFlamer.java
    public static final BiFunction<ItemStack, GunAnimationType, BusAnimationSedna> LAMBDA_CHEMTHROWER_ANIMS = (stack, type) -> {
        switch(type) {
            case EQUIP: return new BusAnimationSedna()
                    .addBus("EQUIP", new BusAnimationSequenceSedna().addPos(-45, 0, 0, 0).addPos(0, 0, 0, 500, IType.SIN_DOWN));
        }

        return null;
    };

    // CE: XFactory22lr.java
    public static final BiFunction<ItemStack, GunAnimationType, BusAnimationSedna> LAMBDA_STAR_F_ANIMS = (stack, type) -> {
        int ammo = ((ItemGunBaseNT) stack.getItem()).getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack).getAmount(stack, Minecraft.getInstance().player == null ? null : Minecraft.getInstance().player.getInventory());
        switch (type) {
            case EQUIP -> {
                return new BusAnimationSedna()
                        .addBus("EQUIP", new BusAnimationSequenceSedna().addPos(45, 0, 0, 0).addPos(0, 0, 0, 500, IType.SIN_DOWN));
            }
            case CYCLE -> {
                return new BusAnimationSedna()
                        .addBus("RECOIL", new BusAnimationSequenceSedna().addPos(0, 0, 0, 50).addPos(0, 0, ItemGunBaseNT.getIsAiming(stack) ? -0.125 : -0.5, 15, IType.SIN_DOWN).addPos(0, 0, 0, 35, IType.SIN_FULL))
                        .addBus("SLIDE", new BusAnimationSequenceSedna().addPos(0, 0, 0, 50).addPos(0, 0, ItemGunBaseNT.getIsAiming(stack) ? -0.5 : -1, 25, IType.SIN_DOWN).addPos(0, 0, 0, 75, IType.SIN_UP))
                        .addBus("HAMMER", new BusAnimationSequenceSedna().addPos(1, 0, 0, 50, IType.SIN_UP).addPos(0, 0, 0, 50, IType.SIN_DOWN))
                        .addBus("BULLET", ammo <= 1 ? new BusAnimationSequenceSedna().setPos(100, 0, 0) : new BusAnimationSequenceSedna().addPos(0, 0, 0, 90).addPos(0, 0.5, 2.25, 50));
            }
            case CYCLE_DRY -> {
                return new BusAnimationSedna()
                        .addBus("HAMMER", new BusAnimationSequenceSedna().addPos(1, 0, 0, 50, IType.SIN_UP).hold(450).addPos(0, 0, 0, 50, IType.SIN_DOWN))
                        .addBus("SLIDE", new BusAnimationSequenceSedna().addPos(0, 0, 0, 500).addPos(0, 0, ItemGunBaseNT.getIsAiming(stack) ? -0.5 : -1, 100, IType.SIN_FULL).hold(100).addPos(0, 0, 0, 75, IType.SIN_UP))
                        .addBus("EQUIP", new BusAnimationSequenceSedna().addPos(0, 0, 0, 600).addPos(-3, 0, 0, 175, IType.SIN_DOWN).addPos(0, 0, 0, 100, IType.SIN_FULL))
                        .addBus("BULLET", new BusAnimationSequenceSedna().setPos(100, 0, 0));
            }
            case RELOAD -> {
                return new BusAnimationSedna()
                        .addBus("TILT", new BusAnimationSequenceSedna().addPos(-30, 0, 0, 250, IType.SIN_FULL).hold(1500).addPos(0, 0, 0, 250, IType.SIN_FULL))
                        .addBus("SLIDE", new BusAnimationSequenceSedna().addPos(0, 0, 0, 250).addPos(0, 0, -1, 100, IType.SIN_FULL).hold(1125).addPos(0, 0, 0, 100, IType.SIN_UP))
                        .addBus("MAG", new BusAnimationSequenceSedna().addPos(0, 0, 0, 250).addPos(0, -7, -1.5, 300, IType.SIN_UP).hold(400).addPos(0, 0, 0, 300, IType.SIN_UP))
                        .addBus("EQUIP", new BusAnimationSequenceSedna().addPos(0, 0, 0, 500).addPos(3, 0, 0, 750, IType.SIN_FULL).addPos(-3, 0, 0, 50, IType.SIN_DOWN).addPos(0, 0, 0, 100, IType.SIN_FULL))
                        .addBus("TURN", new BusAnimationSequenceSedna().addPos(0, 0, 0, 200).addPos(0, 0, 15, 300, IType.SIN_FULL).hold(900).addPos(0, 0, 0, 150, IType.SIN_FULL))
                        .addBus("BULLET", new BusAnimationSequenceSedna().setPos(ammo <= 1 ? 100 : 0, 0, 0).hold(750).setPos(0, 0, 0).hold(750).addPos(0, 0.5, 2.25, 50));
            }
            case JAMMED -> {
                return new BusAnimationSedna()
                        .addBus("TILT", new BusAnimationSequenceSedna().addPos(0, 0, 0, 500).addPos(-30, 0, 0, 150, IType.SIN_FULL).hold(800).addPos(0, 0, 0, 150, IType.SIN_FULL))
                        .addBus("TURN", new BusAnimationSequenceSedna().addPos(0, 0, 0, 500).addPos(0, 0, 25, 150, IType.SIN_FULL).hold(800).addPos(0, 0, 0, 150, IType.SIN_FULL))
                        .addBus("SLIDE", new BusAnimationSequenceSedna().addPos(0, 0, 0, 750).addPos(0, 0, -0.5, 100, IType.SIN_FULL).hold(100).addPos(0, 0, 0, 100, IType.SIN_UP).hold(100).addPos(0, 0, -0.5, 100, IType.SIN_FULL).hold(100).addPos(0, 0, 0, 100, IType.SIN_UP))
                        .addBus("BULLET", new BusAnimationSequenceSedna().setPos(0, 0.5, 2.25).hold(750).addPos(0, 0.5, 1.25, 100, IType.SIN_FULL).hold(100).addPos(0, 0.5, 2.25, 100, IType.SIN_UP).hold(100).addPos(0, 0.5, 1.25, 100, IType.SIN_FULL).hold(100).addPos(0, 0.5, 2.25, 100, IType.SIN_UP));
            }
            case INSPECT -> {
                return new BusAnimationSedna()
                        .addBus("TILT", new BusAnimationSequenceSedna().addPos(-30, 0, 0, 250, IType.SIN_FULL).hold(1500).addPos(0, 0, 0, 250, IType.SIN_FULL))
                        .addBus("TURN", new BusAnimationSequenceSedna().addPos(0, 0, 25, 250, IType.SIN_FULL).hold(1500).addPos(0, 0, 0, 250, IType.SIN_FULL))
                        .addBus("SLIDE", new BusAnimationSequenceSedna().addPos(0, 0, 0, 350).addPos(0, 0, -0.5, 100, IType.SIN_FULL).hold(1125).addPos(0, 0, 0, 100, IType.SIN_UP))
                        .addBus("BULLET", ammo <= 1 ? new BusAnimationSequenceSedna().setPos(100, 0, 0) : new BusAnimationSequenceSedna().setPos(0, 0.5, 2.25).hold(350).addPos(0, 0.5, 1.25, 100, IType.SIN_FULL).hold(1125).addPos(0, 0.5, 2.25, 100, IType.SIN_UP));
            }
        }
        return null;
    };

    // CE: XFactory22lr.java
    public static final BiFunction<ItemStack, GunAnimationType, BusAnimationSedna> LAMBDA_AM180_ANIMS = (stack, type) -> {
        if(ClientConfig.GUN_ANIMS_LEGACY.get()) {
            switch (type) {
                case EQUIP -> {
                    return new BusAnimationSedna()
                            .addBus("EQUIP", new BusAnimationSequenceSedna().addPos(45, 0, 0, 0).addPos(0, 0, 0, 500, IType.SIN_FULL));
                }
                case CYCLE -> {
                    return new BusAnimationSedna()
                            .addBus("RECOIL", new BusAnimationSequenceSedna().addPos(0, 0, ItemGunBaseNT.getIsAiming(stack) ? -0.125 : -0.25, 15, IType.SIN_DOWN).addPos(0, 0, 0, 35, IType.SIN_FULL));
                }
                case CYCLE_DRY -> {
                    return new BusAnimationSedna()
                            .addBus("BOLT", new BusAnimationSequenceSedna().addPos(0, 0, 0, 550).addPos(0, 0, -1.5, 100, IType.SIN_UP).addPos(0, 0, 0, 100, IType.SIN_UP))
                            .addBus("TURN", new BusAnimationSequenceSedna().addPos(0, 0, 0, 300).addPos(0, 0, 15, 250, IType.SIN_FULL).addPos(0, 0, 15, 400).addPos(0, 0, 0, 250, IType.SIN_FULL));
                }
                case RELOAD -> {
                    return new BusAnimationSedna()
                            .addBus("MAGTURN", new BusAnimationSequenceSedna().addPos(15, 0, 0, 250, IType.SIN_FULL).addPos(15, 0, 0, 250).addPos(15, 0, 70, 300, IType.SIN_FULL).addPos(15, 0, 0, 0).addPos(15, 0, 0, 750).addPos(0, 0, 0, 250, IType.SIN_FULL))
                            .addBus("MAG", new BusAnimationSequenceSedna().addPos(0, 0, 0, 250).addPos(2, 0, -4, 250, IType.SIN_FULL).addPos(-10, 2, -4, 300, IType.SIN_UP).addPos(3, -6, -4, 0).addPos(2, 0, -4, 500, IType.SIN_FULL).addPos(0, 0, 0, 250, IType.SIN_FULL))
                            .addBus("BOLT", new BusAnimationSequenceSedna().addPos(0, 0, 0, 2250).addPos(0, 0, -1.5, 100, IType.SIN_UP).addPos(0, 0, 0, 100, IType.SIN_UP))
                            .addBus("TURN", new BusAnimationSequenceSedna().addPos(0, 0, 0, 2000).addPos(0, 0, 15, 250, IType.SIN_FULL).addPos(0, 0, 15, 400).addPos(0, 0, 0, 250, IType.SIN_FULL));
                }
                case JAMMED -> {
                    return new BusAnimationSedna()
                            .addBus("BOLT", new BusAnimationSequenceSedna().addPos(0, 0, 0, 750).addPos(0, 0, -1.5, 100, IType.SIN_UP).addPos(0, 0, 0, 100, IType.SIN_UP))
                            .addBus("TURN", new BusAnimationSequenceSedna().addPos(0, 0, 0, 500).addPos(0, 0, 45, 250, IType.SIN_FULL).addPos(0, 0, 45, 400).addPos(0, 0, 0, 250, IType.SIN_FULL));
                }
                case INSPECT -> {
                    return new BusAnimationSedna()
                            .addBus("MAGTURN", new BusAnimationSequenceSedna().addPos(15, 0, 0, 250, IType.SIN_FULL).addPos(15, 0, 0, 1400).addPos(0, 0, 0, 250, IType.SIN_FULL))
                            .addBus("MAG", new BusAnimationSequenceSedna().addPos(0, 0, 0, 200).addPos(4, -1, -4, 200, IType.SIN_FULL).addPos(4, -1.5, -4, 50).addPos(4, 0, -4, 100).addPos(4, 6, -4, 250, IType.SIN_DOWN).addPos(4, 0, -4, 150, IType.SIN_UP).addPos(4, -1, -4, 100, IType.SIN_DOWN).addPos(4, -1, -4, 250).addPos(0, 0, 0, 250, IType.SIN_FULL))
                            .addBus("MAGSPIN", new BusAnimationSequenceSedna().addPos(0, 0, 0, 600).addPos(-400, 0, 0, 500, IType.SIN_FULL).addPos(-400, 0, 0, 250).addPos(-360, 0, 0, 250));
                }
            }
        } else {
            switch (type) {
                case EQUIP -> {
                    return new BusAnimationSedna()
                            .addBus("EQUIP", new BusAnimationSequenceSedna().addPos(45, 0, 0, 0).addPos(0, 0, 0, 500, IType.SIN_FULL));
                }
                case CYCLE -> {
                    return GunModels.json("am180_anim").get("Fire");
                }
                case CYCLE_DRY -> {
                    return GunModels.json("am180_anim").get("FireDry");
                }
                case RELOAD -> {
                    return GunModels.json("am180_anim").get("Reload");
                }
                case JAMMED -> {
                    return GunModels.json("am180_anim").get("Jammed");
                }
                case INSPECT -> {
                    return GunModels.json("am180_anim").get("Inspect");
                }
            }
        }

        return null;
    };

    // CE: XFactoryDrill.java
    public static final BiFunction<ItemStack, GunAnimationType, BusAnimationSedna> LAMBDA_DRILL_ANIMS = (stack, type) -> {
        switch (type) {

            case EQUIP:
                return new BusAnimationSedna()
                        .addBus("EQUIP", new BusAnimationSequenceSedna()
                                .setPos(-1, 0, 0)
                                .addPos(0, 0, 0, 750, IType.SIN_DOWN)
                        );

            case CYCLE: {
                double deploy = GunAnimationClientState.getRelevantTransformation("DEPLOY")[0];
                double spin = GunAnimationClientState.getRelevantTransformation("SPIN")[2] % 360;
                double speed = GunAnimationClientState.getRelevantTransformation("SPEED")[0];

                return new BusAnimationSedna()
                        .addBus("DEPLOY", new BusAnimationSequenceSedna()
                                .setPos(deploy, 0, 0)
                                .addPos(1, 0, 0, (int) (500 * (1 - deploy)), IType.SIN_FULL)
                                .hold(1000)
                                .addPos(0, 0, 0, 500, IType.SIN_FULL)
                        )
                        .addBus("SPIN", new BusAnimationSequenceSedna()
                                .setPos(spin, 0, 0)
                                .addPos(spin + 360 * 1.5, 0, 0, 1500)
                                .addPos(spin + 360 * 2, 0, 0, 750, IType.SIN_DOWN)
                        )
                        .addBus("SPEED", new BusAnimationSequenceSedna()
                                .setPos(speed, 0, 0)
                                .addPos(1, 0, 0, 500)
                                .hold(1000)
                                .addPos(0, 0, 0, 750 + (int) (1000 * (1D - spin / 360D)), IType.SIN_DOWN)
                        );
            }

            case CYCLE_DRY:
                return new BusAnimationSedna()
                        .addBus("DEPLOY", new BusAnimationSequenceSedna()
                                .addPos(0.25, 0, 0, 250, IType.SIN_FULL)
                                .addPos(0, 0, 0, 250, IType.SIN_FULL)
                        )
                        .addBus("SPIN", new BusAnimationSequenceSedna()
                                .addPos(360, 0, 0, 1500, IType.SIN_DOWN)
                        )
                        .addBus("SPEED", new BusAnimationSequenceSedna()
                            .addPos(0.75, 0, 0, 250)
                            .addPos(0, 0, 0, 1000, IType.SIN_DOWN)
                        );


            case INSPECT:
                return new BusAnimationSedna()
                        .addBus("LIFT", new BusAnimationSequenceSedna()
                                .addPos(-45, 0, 0, 500, IType.SIN_FULL)
                                .hold(1000)
                                .addPos(0, 0, 0, 500, IType.SIN_DOWN)
                        );

            default:
                return null;
        }
    };

    // CE: Lego.java
    public static final BiFunction<ItemStack, GunAnimationType, BusAnimationSedna> LAMBDA_DEBUG_ANIMS = (stack, type) -> switch (type) {
        case CYCLE -> new BusAnimationSedna()
                .addBus("RECOIL", new BusAnimationSequenceSedna().addPos(0, 0, 0, 50).addPos(0, 0, -3, 50).addPos(0, 0, 0, 250))
                .addBus("HAMMER", new BusAnimationSequenceSedna().addPos(0, 0, 1, 50).addPos(0, 0, 1, 400).addPos(0, 0, 0, 200))
                .addBus("DRUM", new BusAnimationSequenceSedna().addPos(0, 0, 0, 450).addPos(0, 0, 1, 200));
        case CYCLE_DRY -> new BusAnimationSedna()
                .addBus("HAMMER", new BusAnimationSequenceSedna().addPos(0, 0, 1, 50).addPos(0, 0, 1, 300 + 100).addPos(0, 0, 0, 200))
                .addBus("DRUM", new BusAnimationSequenceSedna().addPos(0, 0, 0, 450).addPos(0, 0, 1, 200));
        case EQUIP ->
                new BusAnimationSedna().addBus("ROTATE", new BusAnimationSequenceSedna().addPos(-360, 0, 0, 350));
        case RELOAD -> new BusAnimationSedna()
                .addBus("RELAOD_TILT", new BusAnimationSequenceSedna().addPos(-15, 0, 0, 100).addPos(65, 0, 0, 100).addPos(45, 0, 0, 50).addPos(0, 0, 0, 200).addPos(0, 0, 0, 1450).addPos(-80, 0, 0, 100).addPos(-80, 0, 0, 100).addPos(0, 0, 0, 200))
                .addBus("RELOAD_CYLINDER", new BusAnimationSequenceSedna().addPos(0, 0, 0, 200).addPos(90, 0, 0, 100).addPos(90, 0, 0, 1700).addPos(0, 0, 0, 70))
                .addBus("RELOAD_LIFT", new BusAnimationSequenceSedna().addPos(0, 0, 0, 350).addPos(-45, 0, 0, 250).addPos(-45, 0, 0, 350).addPos(-15, 0, 0, 200).addPos(-15, 0, 0, 1050).addPos(0, 0, 0, 100))
                .addBus("RELOAD_JOLT", new BusAnimationSequenceSedna().addPos(0, 0, 0, 600).addPos(2, 0, 0, 50).addPos(0, 0, 0, 100))
                .addBus("RELOAD_BULLETS", new BusAnimationSequenceSedna().addPos(0, 0, 0, 650).addPos(10, 0, 0, 300).addPos(10, 0, 0, 200).addPos(0, 0, 0, 700))
                .addBus("RELOAD_BULLETS_CON", new BusAnimationSequenceSedna().addPos(1, 0, 0, 0).addPos(1, 0, 0, 950).addPos(0, 0, 0, 1));
        case INSPECT, JAMMED -> new BusAnimationSedna()
                .addBus("RELAOD_TILT", new BusAnimationSequenceSedna().addPos(-15, 0, 0, 100).addPos(65, 0, 0, 100).addPos(45, 0, 0, 50).addPos(0, 0, 0, 200).addPos(0, 0, 0, 200).addPos(-80, 0, 0, 100).addPos(-80, 0, 0, 100).addPos(0, 0, 0, 200))
                .addBus("RELOAD_CYLINDER", new BusAnimationSequenceSedna().addPos(0, 0, 0, 200).addPos(90, 0, 0, 100).addPos(90, 0, 0, 450).addPos(0, 0, 0, 70));
        default -> null;
    };
}
