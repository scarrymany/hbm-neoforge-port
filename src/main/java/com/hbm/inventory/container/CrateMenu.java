package com.hbm.inventory.container;

import com.hbm.blockentity.machine.CrateBlockEntity;
import com.hbm.blockentity.machine.CrateBlockEntity.CrateType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

/**
 * Mass storage crate menu, ported from CE's {@code com.hbm.inventory.container.ContainerCrateBase}
 * (read in full, per {@code docs/phase2/machines_storage.md}) - one crate-grid slot range (dimensions
 * taken straight off the block entity's own {@link CrateType}) plus the standard player inventory,
 * both laid out via {@link MenuBase}'s shared batch helpers. Every {@link CrateType} grade shares this
 * one {@link net.minecraft.world.inventory.AbstractContainerMenu} class - the grid dimensions are
 * data, not a reason for five separate Menu classes, matching {@link CrateBlockEntity} itself.
 *
 * <p>Opened server-side via {@code player.openMenu(new SimpleMenuProvider((id, inv, ply) -> new
 * CrateMenu(id, inv, be), be.getDisplayName()), pos)} (see {@link com.hbm.blocks.machine.CrateBlock#useWithoutItem}) -
 * confirmed-real NeoForge 1.21.1 shape, cross-checked against Neo Edition's own
 * {@code com.hbm.blocks.machine.CrateBlock#useWithoutItem} and {@code MachineFluidTankBlock#useWithoutItem}.
 * {@link #fromNetwork} is the client-side counterpart {@link ModMenuTypes} registers via
 * {@code IMenuTypeExtension.create} - the {@link BlockPos} vanilla's {@code Player.openMenu(MenuProvider, BlockPos)}
 * writes is read back here to find the same block entity on the client.
 */
public class CrateMenu extends MenuBase<CrateBlockEntity> {

    public CrateMenu(int id, Inventory playerInventory, CrateBlockEntity be) {
        super(ModMenuTypes.CRATE.get(), id, be);

        CrateType type = be.getCrateType();
        addSlots(be.inventory, 0, type.crateX, type.crateY, type.rows, type.columns);
        playerInv(playerInventory, type.playerInventoryX, type.playerInventoryY, type.hotbarY);
        // Exact CE TileEntityCrateBase.java:199-201 — server open (CE client IInventory hook).
        if (!playerInventory.player.level().isClientSide()) {
            be.openInventory(playerInventory.player);
        }
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        // Exact CE TileEntityCrateBase.java:207-209 / FileCabinet onContainerClosed.
        if (!player.level().isClientSide()) {
            be.closeInventory(player);
        }
    }

    public static CrateMenu fromNetwork(int id, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        if (playerInventory.player.level().getBlockEntity(pos) instanceof CrateBlockEntity be) {
            return new CrateMenu(id, playerInventory, be);
        }
        throw new IllegalStateException("No CrateBlockEntity at " + pos);
    }
}
