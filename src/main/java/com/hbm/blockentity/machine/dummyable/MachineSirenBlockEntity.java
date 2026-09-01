package com.hbm.blockentity.machine.dummyable;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.inventory.container.machine.dummyable.SirenMenu;
import com.hbm.items.machine.ItemCassette;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * CE {@code TileEntityMachineSiren} — 1 cassette slot. Redstone → play track.
 * Control-panel / packet loop skipped (no TESirenPacket client audio bus).
 */
public class MachineSirenBlockEntity extends MachineBaseBlockEntity
        implements ITickableBE, MenuProvider {

    private int cooldown;

    public MachineSirenBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 1, false, false);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.siren");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return stack.getItem() instanceof ItemCassette;
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int amount) {
        return true;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(Direction side) {
        return new int[]{0};
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;
        if (cooldown > 0) {
            cooldown--;
            return;
        }
        if (!level.hasNeighborSignal(worldPosition)) return;
        ItemCassette.TrackType track = ItemCassette.getType(inventory.getStackInSlot(0));
        if (track == ItemCassette.TrackType.NULL) return;
        SoundEvent sound = track.getSoundLocation();
        if (sound == null) return;
        float vol = Math.max(0.25F, Math.min(2.0F, track.getVolume() / 100F));
        level.playSound(null, worldPosition, sound, SoundSource.BLOCKS, vol, 1.0F);
        cooldown = track.getSoundType() == ItemCassette.SoundType.LOOP ? 40 : 80;
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new SirenMenu(id, inv, this);
    }
}
