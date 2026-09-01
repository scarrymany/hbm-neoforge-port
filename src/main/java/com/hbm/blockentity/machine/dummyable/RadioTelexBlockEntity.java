package com.hbm.blockentity.machine.dummyable;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.blockentity.network.RTTYSystem;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.inventory.container.machine.dummyable.RadioTelexMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/** CE {@code TileEntityRadioTelex} — char TX/RX on RTTY, {@code IControlReceiver} cmds. */
public class RadioTelexBlockEntity extends MachineBaseBlockEntity
        implements ITickableBE, MenuProvider, IControlReceiver {

    public static final int lineWidth = 33;
    public String txChannel = "";
    public String rxChannel = "";
    public final String[] txBuffer = {"", "", "", "", ""};
    public final String[] rxBuffer = {"", "", "", "", ""};
    public int sendingLine = 0;
    public int sendingIndex = 0;
    public boolean isSending = false;
    public int sendingWait = 0;
    public int writingLine = 0;
    public boolean printAfterRx = false;
    public boolean deleteOnReceive = true;
    public char sendingChar = ' ';

    public static final char eol = '\n';
    public static final char eot = '\u0004';
    public static final char bell = '\u0007';
    public static final char print = '\u000c';
    public static final char pause = '\u0016';
    public static final char clear = '\u007f';

    public RadioTelexBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 1, false, false);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.radioTelex");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot == 0;
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        this.sendingChar = ' ';

        if (this.isSending && this.txChannel.isEmpty()) this.isSending = false;

        if (this.isSending) {
            if (sendingWait > 0) {
                sendingWait--;
            } else {
                String line = txBuffer[sendingLine];
                if (line.length() > sendingIndex) {
                    char c = line.charAt(sendingIndex);
                    sendingIndex++;
                    if (c == pause) {
                        sendingWait = 20;
                    } else {
                        RTTYSystem.broadcast(level, this.txChannel, c);
                        this.sendingChar = c;
                    }
                } else {
                    if (sendingLine >= 4) {
                        this.isSending = false;
                        RTTYSystem.broadcast(level, this.txChannel, eot);
                        this.sendingLine = 0;
                    } else {
                        RTTYSystem.broadcast(level, this.txChannel, eol);
                        this.sendingLine++;
                    }
                    this.sendingIndex = 0;
                }
            }
        }

        if (!this.rxChannel.isEmpty()) {
            RTTYSystem.RTTYChannel chan = RTTYSystem.listen(level, this.rxChannel);
            if (chan != null && chan.signal instanceof Character
                    && (chan.timeStamp > level.getGameTime() - 2 && chan.timeStamp != -1)) {
                char c = (char) chan.signal;

                if (this.deleteOnReceive) {
                    this.deleteOnReceive = false;
                    for (int i = 0; i < 5; i++) this.rxBuffer[i] = "";
                    this.writingLine = 0;
                }

                if (c == eot) {
                    if (this.printAfterRx) {
                        this.printAfterRx = false;
                        this.print();
                    }
                    this.deleteOnReceive = true;
                } else if (c == eol) {
                    if (this.writingLine < 4) this.writingLine++;
                    setChanged();
                } else if (c == bell) {
                    level.playSound(null, worldPosition, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 2.0F, 0.5F);
                } else if (c == print) {
                    this.printAfterRx = true;
                } else if (c == clear) {
                    for (int i = 0; i < 5; i++) this.rxBuffer[i] = "";
                    this.writingLine = 0;
                } else {
                    this.rxBuffer[this.writingLine] += c;
                    setChanged();
                }
            }
        }

        networkPackNT(16);
    }

    public void print() {
        if (level == null || level.isClientSide) return;
        ItemStack stack = new ItemStack(Items.PAPER);
        List<Component> text = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            if (!rxBuffer[i].isEmpty()) text.add(Component.literal(rxBuffer[i]));
        }
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("Message"));
        if (!text.isEmpty()) stack.set(DataComponents.LORE, new ItemLore(text));
        level.addFreshEntity(new ItemEntity(level,
                worldPosition.getX() + 0.5, worldPosition.getY() + 1, worldPosition.getZ() + 0.5, stack));
    }

    @Override
    public boolean hasPermission(Player player) {
        return player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) < 16 * 16;
    }

    @Override
    public void receiveControl(CompoundTag data) {
        for (int i = 0; i < 5; i++) {
            if (data.contains("tx" + i)) this.txBuffer[i] = data.getString("tx" + i);
        }
        String cmd = data.getString("cmd");
        if ("snd".equals(cmd) && !this.isSending) {
            this.isSending = true;
            this.sendingLine = 0;
            this.sendingIndex = 0;
        }
        if ("rxprt".equals(cmd)) print();
        if ("rxcls".equals(cmd)) {
            for (int i = 0; i < 5; i++) this.rxBuffer[i] = "";
            this.writingLine = 0;
        }
        if ("sve".equals(cmd)) {
            this.txChannel = data.getString("txChan");
            this.rxChannel = data.getString("rxChan");
            setChanged();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        for (int i = 0; i < 5; i++) {
            tag.putString("tx" + i, txBuffer[i]);
            tag.putString("rx" + i, rxBuffer[i]);
        }
        tag.putString("txChan", txChannel);
        tag.putString("rxChan", rxChannel);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        for (int i = 0; i < 5; i++) {
            txBuffer[i] = tag.getString("tx" + i);
            rxBuffer[i] = tag.getString("rx" + i);
        }
        txChannel = tag.contains("txChan") ? tag.getString("txChan") : tag.getString("tx");
        rxChannel = tag.contains("rxChan") ? tag.getString("rxChan") : tag.getString("rx");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        for (int i = 0; i < 5; i++) {
            buf.writeUtf(txBuffer[i]);
            buf.writeUtf(rxBuffer[i]);
        }
        buf.writeUtf(txChannel);
        buf.writeUtf(rxChannel);
        buf.writeChar(sendingChar);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        for (int i = 0; i < 5; i++) {
            txBuffer[i] = buf.readUtf();
            rxBuffer[i] = buf.readUtf();
        }
        txChannel = buf.readUtf();
        rxChannel = buf.readUtf();
        sendingChar = buf.readChar();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new RadioTelexMenu(id, inv, this);
    }
}
