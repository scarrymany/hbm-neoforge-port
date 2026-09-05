package com.hbm.inventory.container.bomb;

import com.hbm.blockentity.bomb.NukeBalefireBlockEntity;
import com.hbm.blockentity.bomb.NukeBoyBlockEntity;
import com.hbm.blockentity.bomb.NukeCustomBlockEntity;
import com.hbm.blockentity.bomb.NukeFleijaBlockEntity;
import com.hbm.blockentity.bomb.NukeGadgetBlockEntity;
import com.hbm.blockentity.bomb.NukeManBlockEntity;
import com.hbm.blockentity.bomb.NukeMikeBlockEntity;
import com.hbm.blockentity.bomb.NukeN2BlockEntity;
import com.hbm.blockentity.bomb.NukePrototypeBlockEntity;
import com.hbm.blockentity.bomb.NukeSoliniumBlockEntity;
import com.hbm.blockentity.bomb.NukeTsarBlockEntity;
import com.hbm.inventory.container.ModMenuTypes;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.network.IContainerFactory;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * {@link MenuType} registrations for the 9 concrete nuke casings + {@code NukeCustom} (10 GUIs -
 * {@code CrashedBomb} has none, per {@code docs/phase3/bomb_blocks_and_detonators.md} Section B).
 * Deliberately a separate class from {@code com.hbm.inventory.container.ModMenuTypes}, calling that
 * file's already-public {@link ModMenuTypes#MENU_TYPES} field directly - see {@code PowerGenMenus}'
 * own javadoc for why (many Phase 3 areas landing in the same wave, avoiding a shared-file race).
 */
public final class NukeCasingMenus {

    public static DeferredHolder<MenuType<?>, MenuType<NukeBoyMenu>> NUKE_BOY;
    public static DeferredHolder<MenuType<?>, MenuType<NukeGadgetMenu>> NUKE_GADGET;
    public static DeferredHolder<MenuType<?>, MenuType<NukeManMenu>> NUKE_MAN;
    public static DeferredHolder<MenuType<?>, MenuType<NukeMikeMenu>> NUKE_MIKE;
    public static DeferredHolder<MenuType<?>, MenuType<NukeTsarMenu>> NUKE_TSAR;
    public static DeferredHolder<MenuType<?>, MenuType<NukeN2Menu>> NUKE_N2;
    public static DeferredHolder<MenuType<?>, MenuType<NukePrototypeMenu>> NUKE_PROTOTYPE;
    public static DeferredHolder<MenuType<?>, MenuType<NukeFleijaMenu>> NUKE_FLEIJA;
    public static DeferredHolder<MenuType<?>, MenuType<NukeBalefireMenu>> NUKE_BALEFIRE;
    public static DeferredHolder<MenuType<?>, MenuType<NukeSoliniumMenu>> NUKE_SOLINIUM;
    public static DeferredHolder<MenuType<?>, MenuType<NukeCustomMenu>> NUKE_CUSTOM;

    private NukeCasingMenus() {
    }

    public static void registerAll() {
        NUKE_BOY = reg("nuke_boy", (id, inv, buf) ->
                new NukeBoyMenu(id, inv, (NukeBoyBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        NUKE_GADGET = reg("nuke_gadget", (id, inv, buf) ->
                new NukeGadgetMenu(id, inv, (NukeGadgetBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        NUKE_MAN = reg("nuke_man", (id, inv, buf) ->
                new NukeManMenu(id, inv, (NukeManBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        NUKE_MIKE = reg("nuke_mike", (id, inv, buf) ->
                new NukeMikeMenu(id, inv, (NukeMikeBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        NUKE_TSAR = reg("nuke_tsar", (id, inv, buf) ->
                new NukeTsarMenu(id, inv, (NukeTsarBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        NUKE_N2 = reg("nuke_n2", (id, inv, buf) ->
                new NukeN2Menu(id, inv, (NukeN2BlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        NUKE_PROTOTYPE = reg("nuke_prototype", (id, inv, buf) ->
                new NukePrototypeMenu(id, inv, (NukePrototypeBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        NUKE_FLEIJA = reg("nuke_fleija", (id, inv, buf) ->
                new NukeFleijaMenu(id, inv, (NukeFleijaBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        NUKE_BALEFIRE = reg("nuke_balefire", (id, inv, buf) ->
                new NukeBalefireMenu(id, inv, (NukeBalefireBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        NUKE_SOLINIUM = reg("nuke_solinium", (id, inv, buf) ->
                new NukeSoliniumMenu(id, inv, (NukeSoliniumBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        NUKE_CUSTOM = reg("nuke_custom", (id, inv, buf) ->
                new NukeCustomMenu(id, inv, (NukeCustomBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
    }

    private static <T extends AbstractContainerMenu> DeferredHolder<MenuType<?>, MenuType<T>> reg(String name, IContainerFactory<T> factory) {
        return ModMenuTypes.MENU_TYPES.register(name, () -> IMenuTypeExtension.create(factory));
    }
}
