package com.hbm.blockentity.bomb;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.config.BombConfig;
import com.hbm.entity.effect.EntityNukeTorex;
import com.hbm.entity.logic.EntityBalefire;
import com.hbm.entity.logic.NukeEntityTypes;
import com.hbm.inventory.container.bomb.NukeBalefireMenu;
import com.hbm.items.bomb.NukeCasingItems;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.main.ModContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Ported from CE's {@code TileEntityNukeBalefire} (201 lines, read in full) - a countdown-timer
 * casing, closer in shape to a Phase 2 ticking machine than the other 8 flat-check casings (see
 * {@link com.hbm.blocks.bomb.NukeCasingBlockBase}'s own javadoc for why this does not extend
 * {@code NukeCasingBlockEntity}). 2-slot inventory: slot 0 the balefire egg, slot 1 a battery
 * (spark = grade 1, trixite = grade 2 - CE's {@code getBattery()} grade value is read but never
 * actually branched on beyond "present", preserved as-is). {@code started}/{@code timer} count down
 * once armed via the GUI's start button; reaching zero (or the button being pressed at 0) detonates.
 */
public class NukeBalefireBlockEntity extends MachineBaseBlockEntity implements ITickableBE, MenuProvider {

    public static final int BUTTON_START = 0;
    public static final int BUTTON_SET_TIMER = 1;

    public boolean started;
    public int timer = 18000;
    @Nullable
    public UUID placerID;

    public NukeBalefireBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 2, 64, false, false);
    }

    public boolean hasEgg() {
        return inventory.getStackInSlot(0).getItem() == NukeCasingItems.EGG_BALEFIRE.get();
    }

    public int getBattery() {
        ItemStack stack = inventory.getStackInSlot(1);
        if (stack.getItem() == NukeCasingItems.BATTERY_SPARK.get()) return 1;
        if (stack.getItem() == NukeCasingItems.BATTERY_TRIXITE.get()) return 2;
        return 0;
    }

    public boolean hasBattery() {
        return getBattery() > 0;
    }

    /** CE: {@code isLoaded()} override - reuses the name for both "chunk loaded" (inherited) and "has the parts to be armed" checks. */
    @Override
    public boolean isLoaded() {
        return super.isLoaded() && hasEgg() && hasBattery();
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide()) return;

        if (!isLoaded()) started = false;

        if (started) {
            timer--;
            if (timer % 20 == 0) {
                level.playSound(null, worldPosition, HBMSoundHandler.fstbmbPing.get(), SoundSource.BLOCKS, 5.0F, 1.0F);
            }
        }

        if (timer <= 0) explode();

        networkPackNT(250);
    }

    @Override
    public void handleButtonPacket(int value, int meta) {
        if (level == null) return;
        if (meta == BUTTON_START && isLoaded()) {
            level.playSound(null, worldPosition, HBMSoundHandler.fstbmbStart.get(), SoundSource.BLOCKS, 5.0F, 1.0F);
            started = true;
        }
        if (meta == BUTTON_SET_TIMER) {
            timer = value * 20;
        }
    }

    public void explode() {
        clearSlots();
        level.destroyBlock(worldPosition, false);

        EntityBalefire bf = new EntityBalefire(NukeEntityTypes.BALEFIRE.get(), level);
        bf.setPos(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5);
        bf.destructionRange = 250;
        if (ModContext.DETONATOR_CONTEXT.get() == null) {
            bf.detonator = placerID;
        } else {
            bf.setDetonator(ModContext.DETONATOR_CONTEXT.get());
        }
        level.addFreshEntity(bf);

        if (BombConfig.ENABLE_NUKE_CLOUDS.get()) {
            EntityNukeTorex.statFacBale(level, worldPosition.getX() + 0.5, worldPosition.getY() + 5, worldPosition.getZ() + 0.5, 250F);
        }
    }

    public void clearSlots() {
        for (int i = 0; i < inventory.getSlots(); i++) {
            inventory.setStackInSlot(i, ItemStack.EMPTY);
        }
    }

    public String getMinutes() {
        String mins = String.valueOf(timer / 1200);
        return mins.length() == 1 ? "0" + mins : mins;
    }

    public String getSeconds() {
        String secs = String.valueOf((timer / 20) % 60);
        return secs.length() == 1 ? "0" + secs : secs;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.nukeFstbmb");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeInt(timer);
        buf.writeBoolean(isLoaded());
        buf.writeBoolean(started);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        timer = buf.readInt();
        buf.readBoolean(); // CE's "loaded" flag - derived client-side from hasEgg()/hasBattery() instead, not a separate field here
        started = buf.readBoolean();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("started", started);
        tag.putInt("timer", timer);
        if (placerID != null) tag.putUUID("placer", placerID);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        started = tag.getBoolean("started");
        timer = tag.getInt("timer");
        if (tag.hasUUID("placer")) placerID = tag.getUUID("placer");
    }

    @Override
    public @NotNull AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new NukeBalefireMenu(containerId, playerInventory, this);
    }
}
