package com.hbm.inventory.container;

import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

/**
 * Small parallel {@link AbstractContainerMenu} base for an {@code Entity}-implementing-{@link
 * Container} GUI (the rail/train cargo cars - {@code docs/phase4/entities_vehicles_aircraft.md}'s
 * "Key design/API decisions" section on this exact gap), since this port's existing
 * {@link MenuBase}{@code <T extends com.hbm.blockentity.MachineBaseBlockEntity>} is hard-wired to a
 * block entity and cannot bind to an entity-backed inventory at all - a generic bound (e.g.
 * {@code <T extends Container>}) is not enough either, since {@link #stillValid} needs the entity's
 * own liveness/distance check, not a block entity's.
 * <p>
 * Opened via {@code player.openMenu(MenuProvider, Consumer<RegistryFriendlyByteBuf>)} - the entity
 * itself is the {@link net.minecraft.world.MenuProvider} (it already satisfies
 * {@code getDisplayName()} via vanilla {@link Entity}'s own {@link net.minecraft.world.entity.Nameable}
 * implementation) - writing its network ID as the {@code extraData} payload, read back by each
 * concrete {@code fromNetwork} factory via {@code buf.readVarInt()} +
 * {@code playerInventory.player.level().getEntity(id)}, mirroring this package's existing
 * {@code BlockPos}-based {@code fromNetwork} factories (see {@link LaunchPadMenu#fromNetwork}) one
 * level of indirection over (entity id instead of block pos).
 *
 * @param <T> the concrete rail-car entity this menu is opened against.
 */
public abstract class EntityMenuBase<T extends Entity & Container> extends AbstractContainerMenu {

    /** The entity this menu was opened for. */
    public final T entity;

    protected EntityMenuBase(MenuType<?> menuType, int id, T entity) {
        super(menuType, id);
        this.entity = entity;
    }

    @Override
    public boolean stillValid(Player player) {
        return entity.stillValid(player);
    }
}
