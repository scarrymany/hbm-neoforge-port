package com.hbm.blockentity.machine.pile;

import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.blocks.machine.pile.BlockPile;
import com.hbm.blocks.machine.pile.PileBlocks;
import com.hbm.items.machine.ItemPileRodMK2;
import com.hbm.lib.HBMSoundHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;

/**
 * CE {@code TileEntityPileLoader}. Slot 1, insert-only. CE {@code == ModItems.pile_rod}
 * flattens to any {@link ItemPileRodMK2}.
 * Field {@code insertLevel} is CE {@code level} — renamed so it does not shadow {@code BlockEntity.level}.
 * TODO(CE: TileEntityPileLoader.java:293-345): OpenComputers callbacks.
 */
public class PileLoaderBlockEntity extends PileDeviceBaseBlockEntity implements IRORValueProvider {

    public double syncLevel;
    public double insertLevel;
    public double lastLevel;
    public int turnProgress;

    public static final double SPEED = 1D / 7D;

    public boolean loading = false;
    public int delay = 0;
    public ItemStack syncStack = ItemStack.EMPTY;
    public ItemStack stack = ItemStack.EMPTY;
    public boolean wasRedstone;

    public ItemStack channelStack = ItemStack.EMPTY;
    public double channelDepletion;
    public double channelTemp;

    public final IItemHandler itemHandler = new LoaderItemHandler();

    public PileLoaderBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void updateEntity() {
        if (level == null) return;

        if (!level.isClientSide) {
            Direction dir = getOrientation();
            PileCoreBlockEntity.PileChannel fuelChan = null;
            this.channelStack = ItemStack.EMPTY;
            this.channelDepletion = 0D;
            this.channelTemp = 0D;

            BlockPos port = worldPosition.offset(-dir.getStepX(), 0, -dir.getStepZ());
            BlockState portState = level.getBlockState(port);

            if (portState.getBlock() == PileBlocks.PILE_BLOCK.get()
                    && portState.getValue(BlockPile.META) == BlockPile.META_FUEL_IN) {
                BlockEntity tile = level.getBlockEntity(port);
                if (tile instanceof PileBaseBlockEntity pile) {
                    PileCoreBlockEntity core = pile.getCore();
                    if (core != null) {
                        fuelChan = core.getFuelChannel(port.getX(), port.getY(), port.getZ());
                        if (fuelChan != null) {
                            this.chanNum = core.fuelChannels.indexOf(fuelChan);
                            this.channelStack = fuelChan.rods.length == 0 ? ItemStack.EMPTY
                                    : fuelChan.rods[fuelChan.rods.length - 1];
                            this.channelDepletion = ItemPileRodMK2.getDepletionPercent(channelStack);
                            this.channelTemp = fuelChan.heat;
                        }
                    }
                }
            }

            boolean redstone = level.getSignal(worldPosition.relative(dir), dir.getOpposite()) > 0;
            if (redstone && !wasRedstone && this.delay <= 0 && this.insertLevel <= 0) this.loading = true;
            this.wasRedstone = redstone;

            if (this.delay > 0) {
                this.delay--;
            } else if (loading) {
                if (this.insertLevel == 0) {
                    level.playSound(null, worldPosition, HBMSoundHandler.boltOpen.get(), SoundSource.BLOCKS, this.getVolume(1F), 1F);
                }
                this.insertLevel += SPEED;
                if (this.insertLevel >= 1D) {
                    this.insertLevel = 1D;
                    this.loading = false;
                    this.delay = 5;
                }
            } else {
                if (this.insertLevel == 1) {
                    level.playSound(null, worldPosition, HBMSoundHandler.boltOpen.get(), SoundSource.BLOCKS, this.getVolume(1F), 0.75F);
                    if (fuelChan != null) {
                        fuelChan.loadItem(stack);
                        this.stack = ItemStack.EMPTY;
                    }
                }
                if (this.insertLevel > 0D) {
                    this.insertLevel -= SPEED;
                    if (this.insertLevel < 0D) this.insertLevel = 0D;
                }
            }

            setChanged();
            this.networkPackNT(35);
        } else {
            this.lastLevel = this.insertLevel;
            if (this.turnProgress > 0) {
                this.insertLevel = this.insertLevel + ((this.syncLevel - this.insertLevel) / (double) this.turnProgress);
                --this.turnProgress;
            } else {
                this.insertLevel = this.syncLevel;
            }
        }
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeDouble(this.insertLevel);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, this.stack);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, this.channelStack);
        buf.writeDouble(this.channelDepletion);
        buf.writeDouble(this.channelTemp);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        double lastSync = this.syncLevel;
        this.syncLevel = buf.readDouble();
        this.syncStack = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
        this.channelStack = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
        this.channelDepletion = buf.readDouble();
        this.channelTemp = buf.readDouble();
        if (this.syncLevel != lastSync) this.turnProgress = 2;
    }

    @Override
    protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
        super.loadAdditional(nbt, registries);
        this.loading = nbt.getBoolean("loading");
        this.insertLevel = nbt.getDouble("level");
        this.delay = nbt.getInt("delay");
        this.wasRedstone = nbt.getBoolean("redstone");
        if (nbt.contains("stack")) {
            this.stack = ItemStack.parseOptional(registries, nbt.getCompound("stack"));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
        super.saveAdditional(nbt, registries);
        nbt.putBoolean("loading", loading);
        nbt.putDouble("level", insertLevel);
        nbt.putInt("delay", delay);
        nbt.putBoolean("wasRedstone", wasRedstone);
        if (!this.stack.isEmpty()) {
            nbt.put("stack", this.stack.save(registries, new CompoundTag()));
        }
    }

    public static boolean isItemLoadable(ItemStack stack) {
        return stack.getItem() instanceof ItemPileRodMK2;
    }

    @Override
    public String[] getFunctionInfo() {
        return new String[]{
                PREFIX_VALUE + "meta",
                PREFIX_VALUE + "depletion",
                PREFIX_VALUE + "deppercent",
                PREFIX_VALUE + "lifetime",
                PREFIX_VALUE + "temp",
        };
    }

    @Override
    public String provideRORValue(String name) {
        if (name.equals(PREFIX_VALUE + "meta")) {
            if (this.channelStack.isEmpty() || !(this.channelStack.getItem() instanceof ItemPileRodMK2 rod)) return "-1";
            return "" + rod.getType().ordinal();
        }
        if (name.equals(PREFIX_VALUE + "deppercent")) {
            return "" + (int) Math.round(this.channelDepletion);
        }
        if (name.equals(PREFIX_VALUE + "depletion")) {
            if (this.channelStack.isEmpty()) return "0";
            return "" + (int) Math.round(ItemPileRodMK2.getDepletionPercent(this.channelStack));
        }
        if (name.equals(PREFIX_VALUE + "lifetime")) {
            if (this.channelStack.isEmpty() || !(this.channelStack.getItem() instanceof ItemPileRodMK2 rod)) return "0";
            return "" + (int) Math.round(rod.getType().life);
        }
        if (name.equals(PREFIX_VALUE + "temp")) {
            return "" + (int) Math.round(this.channelTemp);
        }
        return null;
    }

    private class LoaderItemHandler implements IItemHandler {
        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            return stack;
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack incoming, boolean simulate) {
            if (slot != 0 || incoming.isEmpty() || !isItemLoadable(incoming) || !stack.isEmpty()) return incoming;
            if (!simulate) {
                stack = incoming.copyWithCount(1);
                setChanged();
                dataChanged();
            }
            ItemStack leftover = incoming.copy();
            leftover.shrink(1);
            return leftover;
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack incoming) {
            return isItemLoadable(incoming);
        }
    }
}
