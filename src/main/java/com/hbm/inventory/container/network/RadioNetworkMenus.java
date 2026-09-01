package com.hbm.inventory.container.network;

import com.hbm.blockentity.network.RadioTorchBaseBlockEntity;
import com.hbm.blockentity.network.RadioTorchCounterBlockEntity;
import com.hbm.interfaces.IKeypadHandler;
import com.hbm.inventory.container.ModMenuTypes;
import com.hbm.inventory.container.machine.KeypadMenu;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.network.IContainerFactory;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class RadioNetworkMenus {

    public static DeferredHolder<MenuType<?>, MenuType<RadioTorchMenu>> RADIO_TORCH;
    public static DeferredHolder<MenuType<?>, MenuType<RadioTorchCounterMenu>> RADIO_TORCH_COUNTER;
    public static DeferredHolder<MenuType<?>, MenuType<KeypadMenu>> KEYPAD;

    private RadioNetworkMenus() {
    }

    public static void registerAll() {
        RADIO_TORCH = reg("radio_torch", (id, inv, buf) ->
                new RadioTorchMenu(id, inv, (RadioTorchBaseBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        RADIO_TORCH_COUNTER = reg("radio_torch_counter", (id, inv, buf) ->
                new RadioTorchCounterMenu(id, inv, (RadioTorchCounterBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        KEYPAD = reg("keypad", (id, inv, buf) -> {
            BlockEntity be = inv.player.level().getBlockEntity(buf.readBlockPos());
            return new KeypadMenu(id, inv, be instanceof IKeypadHandler handler ? handler : null);
        });
    }

    private static <T extends AbstractContainerMenu> DeferredHolder<MenuType<?>, MenuType<T>> reg(String name, IContainerFactory<T> factory) {
        return ModMenuTypes.MENU_TYPES.register(name, () -> IMenuTypeExtension.create(factory));
    }
}
