package com.hbm.blockentity.bomb;

import com.hbm.api.item.IDesignatorItem;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.entity.missile.EntityMissileTier4;
import com.hbm.entity.missile.MissileEntityTypes;
import com.hbm.interfaces.IBomb;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.inventory.container.LaunchPadRustedMenu;
import com.hbm.items.tool.LaunchInfraItems;
import com.hbm.items.weapon.MissileItems;
import com.hbm.lib.HBMSoundHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

/**
 * Ported from CE's {@code com.hbm.tileentity.bomb.TileEntityLaunchPadRusted} (235 lines, read in
 * full) - <b>not</b> a {@link LaunchPadBaseBlockEntity} subclass, per the research report's own
 * explicit warning (headline finding #2): a standalone {@code TileEntityMachineBase}-equivalent with
 * a completely different unlock condition ({@code launch_code}/{@code launch_key} items physically
 * present, not power/fuel) and a single hardcoded missile type
 * ({@code EntityMissileTier4.EntityMissileDoomsdayRusted}), not consulting {@link
 * LaunchPadBaseBlockEntity#MISSILES}.
 * <p>
 * 4-slot inventory: slot 0 = launch result (populated externally, e.g. by world-gen loot delivery -
 * see {@link #receiveControl}'s {@code "release"} key), slot 1 = {@code launch_code}, slot 2 =
 * {@code launch_key}, slot 3 = designator.
 */
public class LaunchPadRustedBlockEntity extends MachineBaseBlockEntity implements ITickableBE, IControlReceiver, MenuProvider {

    public int prevRedstonePower;
    public int redstonePower;
    public final Set<BlockPos> activatedBlocks = new HashSet<>(4);

    public boolean missileLoaded;

    public LaunchPadRustedBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 4);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.launchPadRusted");
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        if (this.redstonePower > 0 && this.prevRedstonePower <= 0) {
            this.launch();
        }

        this.prevRedstonePower = this.redstonePower;
        networkPackNT(250);
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeBoolean(this.missileLoaded);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        this.missileLoaded = buf.readBoolean();
    }

    public IBomb.BombReturnCode launch() {
        if (level == null) return IBomb.BombReturnCode.ERROR_MISSING_COMPONENT;

        ItemStack codeStack = inventory.getStackInSlot(1);
        ItemStack keyStack = inventory.getStackInSlot(2);
        ItemStack designatorStack = inventory.getStackInSlot(3);

        if (codeStack.isEmpty() || keyStack.isEmpty() || designatorStack.isEmpty() || !this.missileLoaded) {
            return IBomb.BombReturnCode.ERROR_MISSING_COMPONENT;
        }
        if (codeStack.getItem() != LaunchInfraItems.LAUNCH_CODE.get() || keyStack.getItem() != LaunchInfraItems.LAUNCH_KEY.get()) {
            return IBomb.BombReturnCode.ERROR_MISSING_COMPONENT;
        }
        if (!(designatorStack.getItem() instanceof IDesignatorItem designator)) {
            return IBomb.BombReturnCode.ERROR_MISSING_COMPONENT;
        }
        if (!designator.isReady(level, designatorStack, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ())) {
            return IBomb.BombReturnCode.ERROR_MISSING_COMPONENT;
        }

        Vec3 coords = designator.getCoords(level, designatorStack, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ());
        int targetX = (int) Math.floor(coords.x);
        int targetZ = (int) Math.floor(coords.z);

        EntityMissileTier4.EntityMissileDoomsdayRusted missile =
                new EntityMissileTier4.EntityMissileDoomsdayRusted(MissileEntityTypes.DOOMSDAY_RUSTED.get(), level);
        missile.initTrajectory(worldPosition.getX() + 0.5F, worldPosition.getY() + 1F, worldPosition.getZ() + 0.5F, targetX, targetZ);
        level.addFreshEntity(missile);
        level.playSound(null, worldPosition.getX() + 0.5, worldPosition.getY(), worldPosition.getZ() + 0.5,
                HBMSoundHandler.missileTakeoff.get(), SoundSource.BLOCKS, 2.0F, 1.0F);

        this.missileLoaded = false;
        inventory.extractItem(1, 1, false);
        this.setChanged();

        return IBomb.BombReturnCode.LAUNCHED;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.missileLoaded = tag.getBoolean("missileLoaded");

        this.redstonePower = tag.getInt("redstonePower");
        this.prevRedstonePower = tag.getInt("prevRedstonePower");
        CompoundTag activated = tag.getCompound("activatedBlocks");
        this.activatedBlocks.clear();
        int count = activated.getInt("count");
        for (int i = 0; i < count; i++) {
            this.activatedBlocks.add(new BlockPos(activated.getInt("x" + i), activated.getInt("y" + i), activated.getInt("z" + i)));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("missileLoaded", missileLoaded);

        tag.putInt("redstonePower", redstonePower);
        tag.putInt("prevRedstonePower", prevRedstonePower);
        CompoundTag activated = new CompoundTag();
        int i = 0;
        for (BlockPos p : this.activatedBlocks) {
            activated.putInt("x" + i, p.getX());
            activated.putInt("y" + i, p.getY());
            activated.putInt("z" + i, p.getZ());
            i++;
        }
        activated.putInt("count", i);
        tag.put("activatedBlocks", activated);
    }

    public void updateRedstonePower(BlockPos pos) {
        if (level == null) return;
        boolean powered = level.hasNeighborSignal(pos);
        boolean contained = activatedBlocks.contains(pos);
        if (!contained && powered) {
            activatedBlocks.add(pos);
            if (redstonePower == -1) redstonePower = 0;
            redstonePower++;
        } else if (contained && !powered) {
            activatedBlocks.remove(pos);
            redstonePower--;
            if (redstonePower == 0) redstonePower = -1;
        }
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return switch (slot) {
            case 1 -> stack.getItem() == LaunchInfraItems.LAUNCH_CODE.get();
            case 2 -> stack.getItem() == LaunchInfraItems.LAUNCH_KEY.get();
            case 3 -> stack.getItem() instanceof IDesignatorItem;
            default -> true;
        };
    }

    @Override
    public int[] getAccessibleSlotsFromSide(Direction side) {
        return new int[]{0};
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new LaunchPadRustedMenu(containerId, playerInventory, this);
    }

    @Override
    public boolean hasPermission(Player player) {
        return this.isUseableByPlayer(player);
    }

    /**
     * CE: {@code receiveControl(NBTTagCompound)}, {@code "release"} key - externally triggers
     * spawning the result missile item into slot 0 (tied to whatever placed
     * {@code missile_doomsday_rusted} there in the first place - likely {@code SiloComponent}
     * world-gen loot delivery, a Phase 4 world-gen concern per this package's own Deferred scope).
     */
    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("release")) {
            if (this.missileLoaded && inventory.getStackInSlot(0).isEmpty()) {
                this.missileLoaded = false;
                inventory.setStackInSlot(0, new ItemStack(MissileItems.MISSILE_DOOMSDAY_RUSTED.get()));
                this.setChanged();
            }
        }
    }

    @Override
    public void receiveControl(ServerPlayer player, CompoundTag data) {
        receiveControl(data);
    }
}
