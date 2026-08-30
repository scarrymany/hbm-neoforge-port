package com.hbm.inventory.container.turret;

import com.hbm.blockentity.turret.TurretBaseBlockEntity;
import com.hbm.inventory.container.ModMenuTypes;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * One shared {@link MenuType} for all 11 in-scope concrete turret block entities, matching CE's own
 * single shared {@code ContainerTurretBase} - see {@link TurretMenu}'s own javadoc. Deliberately a
 * separate class from {@code com.hbm.inventory.container.ModMenuTypes} rather than adding a field
 * there directly, matching the exact zero-shared-file-edit reasoning {@code PowerGenMenus} documents
 * for its own area - see {@code TurretBlocks#registerAll} for the one call that wires this in.
 */
public final class TurretMenus {

    public static DeferredHolder<MenuType<?>, MenuType<TurretMenu>> TURRET;

    private TurretMenus() {
    }

    public static void registerAll() {
        TURRET = ModMenuTypes.MENU_TYPES.register("turret", () -> IMenuTypeExtension.create((id, inv, buf) ->
                new TurretMenu(id, inv, (TurretBaseBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos()))));
    }
}
