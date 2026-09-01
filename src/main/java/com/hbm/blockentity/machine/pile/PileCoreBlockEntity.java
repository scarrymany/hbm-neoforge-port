package com.hbm.blockentity.machine.pile;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.LoadedBaseBlockEntity;
import com.hbm.blocks.machine.pile.BlockPile;
import com.hbm.blocks.machine.pile.PileBlocks;
import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.items.machine.ItemPileRodMK2;
import com.hbm.items.machine.MachineDataComponents;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.lib.DirPos;
import com.hbm.particle.HbmEffect;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * CE {@code TileEntityPileCore} (~637 lines). Channels + neutron/heat/vent/meltdown.
 * Vent VFX uses {@link HbmEffect#TOWER} (CE AuxParticlePacketNT / HbmEffectNT.Tower).
 * Debris fire-trail {@code FlameCreator} is cited skip — impact explode 5F still lands.
 */
public class PileCoreBlockEntity extends LoadedBaseBlockEntity implements ITickableBE {

    public PileOrientation orientation = PileOrientation.NEITHER;

    public int height;
    public int width;
    public int depth;

    public final List<PileChannel> fuelChannels = new ArrayList<>();
    public final List<PileChannel> ventilationChannels = new ArrayList<>();
    public final List<PileChannel> controlChannels = new ArrayList<>();

    public int left;
    public int right;
    public int up;

    public PileSegment[] segments = new PileSegment[0];

    public double highestHeat;
    public static final int MAX_HEAT = 800;
    public static boolean meltingDown = false;

    public PileCoreBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
        super.loadAdditional(nbt, registries);
        height = nbt.getInt("height");
        width = nbt.getInt("width");
        depth = nbt.getInt("depth");
        left = nbt.getInt("left");
        right = nbt.getInt("right");
        up = nbt.getInt("up");

        int ori = nbt.getInt("orientation");
        PileOrientation[] values = PileOrientation.VALUES;
        orientation = ori >= 0 && ori < values.length ? values[ori] : PileOrientation.NEITHER;

        segments = new PileSegment[width];
        fuelChannels.clear();
        ventilationChannels.clear();
        controlChannels.clear();

        int fuelCount = nbt.getByte("fc") & 0xFF;
        int ventCount = nbt.getByte("vc") & 0xFF;
        int contCount = nbt.getByte("cc") & 0xFF;

        for (int i = 0; i < fuelCount; i++) fuelChannels.add(readChannelFromNBT(nbt, "f" + i, registries));
        for (int i = 0; i < ventCount; i++) ventilationChannels.add(readChannelFromNBT(nbt, "v" + i, registries));
        for (int i = 0; i < contCount; i++) controlChannels.add(readChannelFromNBT(nbt, "c" + i, registries));

        this.recalculateSegments();
    }

    @Override
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
        super.saveAdditional(nbt, registries);
        nbt.putInt("height", height);
        nbt.putInt("width", width);
        nbt.putInt("depth", depth);
        nbt.putInt("left", left);
        nbt.putInt("right", right);
        nbt.putInt("up", up);
        nbt.putInt("orientation", orientation.ordinal());

        int fuelCount = fuelChannels.size();
        int ventCount = ventilationChannels.size();
        int contCount = controlChannels.size();

        nbt.putByte("fc", (byte) fuelCount);
        nbt.putByte("vc", (byte) ventCount);
        nbt.putByte("cc", (byte) contCount);

        for (int i = 0; i < fuelCount; i++) fuelChannels.get(i).writeChannelToNBT(nbt, "f" + i, registries);
        for (int i = 0; i < ventCount; i++) ventilationChannels.get(i).writeChannelToNBT(nbt, "v" + i, registries);
        for (int i = 0; i < contCount; i++) controlChannels.get(i).writeChannelToNBT(nbt, "c" + i, registries);
    }

    public PileChannel getFuelChannel(int x, int y, int z) { return getChannel(x, y, z, fuelChannels); }
    public PileChannel getVentilationChannel(int x, int y, int z) { return getChannel(x, y, z, ventilationChannels); }
    public PileChannel getControlChannel(int x, int y, int z) { return getChannel(x, y, z, controlChannels); }

    public PileChannel getChannel(int x, int y, int z, List<PileChannel> list) {
        for (PileChannel channel : list) if (channel.entry.compare(x, y, z)) return channel;
        return null;
    }

    public PileCoreBlockEntity setupSize(int up, int down, int l, int r, int d) {
        this.height = up + 1 + down;
        this.width = l + 1 + r;
        this.depth = d;
        this.left = l;
        this.right = r;
        this.up = up;
        this.segments = new PileSegment[width];
        setChanged();
        return this;
    }

    public List<PileChannel> getChannelList(PileChannelType type) {
        if (type == PileChannelType.FUEL) return this.fuelChannels;
        if (type == PileChannelType.VENTILATION) return this.ventilationChannels;
        return this.controlChannels;
    }

    protected int getMeta(int x, int y, int z) {
        BlockState state = level.getBlockState(new BlockPos(x, y, z));
        if (!state.hasProperty(BlockPile.META)) return -1;
        return state.getValue(BlockPile.META);
    }

    protected void setMeta(int x, int y, int z, int meta) {
        BlockPos target = new BlockPos(x, y, z);
        BlockState state = level.getBlockState(target);
        if (state.hasProperty(BlockPile.META)) {
            level.setBlock(target, state.setValue(BlockPile.META, meta), 3);
        }
    }

    public boolean drillChannel(int x, int y, int z, Direction dir, Player player) {
        int startMeta = getMeta(x, y, z);
        PileChannelType type = PileChannelType.getChannelType(dir, orientation);

        int size = type == PileChannelType.CONTROL ? height
                : type == PileChannelType.FUEL ? depth : width;

        List<PileChannel> list = getChannelList(type);

        if (startMeta == BlockPile.META_FUEL_IN || startMeta == BlockPile.META_AIR_IN || startMeta == BlockPile.META_CONTROL) {
            for (int i = 0; i < list.size(); i++) {
                PileChannel chan = list.get(i);
                if (chan.entry.compare(x, y, z) && chan.entry.getDir() == dir) {
                    if (chan.type == PileChannelType.FUEL) chan.ejectAll();
                    list.remove(i);
                    for (int j = 0; j < size; j++) {
                        setMeta(x + dir.getStepX() * j, y + dir.getStepY() * j, z + dir.getStepZ() * j, BlockPile.META_DUMMY);
                    }
                    level.playSound(null, x + 0.5, y + 0.5, z + 0.5, SoundEvents.ITEM_BREAK, SoundSource.BLOCKS, 1F, 0.75F);
                    recalculateSegments();
                    setChanged();
                    dataChanged();
                    return true;
                }
            }
        }

        boolean error = false;
        for (int i = 0; i < size; i++) {
            int iX = x + dir.getStepX() * i;
            int iY = y + dir.getStepY() * i;
            int iZ = z + dir.getStepZ() * i;
            BlockPos iPos = new BlockPos(iX, iY, iZ);
            if (level.getBlockState(iPos).getBlock() != PileBlocks.PILE_BLOCK.get()) {
                BlockPile.sendError(player, "Foreign block in reactor");
                error = true;
                continue;
            }
            int meta = getMeta(iX, iY, iZ);
            if (meta == BlockPile.META_EDGE) {
                BlockPile.sendError(player, "Cannot drill along edge");
                error = true;
            } else if (meta == BlockPile.META_CORE) {
                BlockPile.sendError(player, "Cannot intersect core");
                error = true;
            } else if (meta == BlockPile.META_CHANNEL) {
                BlockPile.sendError(player, "Cannot intersect channel");
                error = true;
            } else if (meta != BlockPile.META_DUMMY) {
                BlockPile.sendError(player, "Cannot intersect channel IO");
                error = true;
            }
        }

        if (error) return false;

        for (int i = 0; i < size; i++) {
            int iX = x + dir.getStepX() * i;
            int iY = y + dir.getStepY() * i;
            int iZ = z + dir.getStepZ() * i;
            if (i == 0) {
                if (type == PileChannelType.FUEL) setMeta(iX, iY, iZ, BlockPile.META_FUEL_IN);
                if (type == PileChannelType.VENTILATION) setMeta(iX, iY, iZ, BlockPile.META_AIR_IN);
                if (type == PileChannelType.CONTROL) setMeta(iX, iY, iZ, BlockPile.META_CONTROL);
            } else if (i == size - 1) {
                if (type == PileChannelType.FUEL) setMeta(iX, iY, iZ, BlockPile.META_FUEL_OUT);
                if (type == PileChannelType.VENTILATION) setMeta(iX, iY, iZ, BlockPile.META_AIR_OUT);
                if (type == PileChannelType.CONTROL) setMeta(iX, iY, iZ, BlockPile.META_CONTROL);
            } else {
                setMeta(iX, iY, iZ, BlockPile.META_CHANNEL);
            }
        }

        list.add(new PileChannel(x, y, z, dir, size, type));

        level.playSound(null, x + 0.5, y + 0.5, z + 0.5, SoundEvents.ITEM_BREAK, SoundSource.BLOCKS, 1F, 1.25F);
        setChanged();
        dataChanged();
        recalculateSegments();
        return true;
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;
        this.runSimulation();
        this.handleVentilation();
        this.handleMeltdown();
        this.networkPackNT(25);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        for (PileChannel chan : this.fuelChannels) chan.ejectAll();
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeDouble(this.highestHeat);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        this.highestHeat = buf.readDouble();
    }

    protected void runSimulation() {
        for (PileChannel chan : this.fuelChannels) {
            if (chan.length <= 0) continue;
            double producedNeutrons = 0;

            for (int i = 0; i < chan.rods.length; i++) {
                ItemStack stack = chan.rods[i];
                if (!stack.isEmpty() && stack.getItem() instanceof ItemPileRodMK2) {
                    double neut = ItemPileRodMK2.getReactivity(stack, chan.incomingNeutrons / chan.length);
                    producedNeutrons += neut;
                    chan.heat += neut * ItemPileRodMK2.getHeatPerNeutron(stack);
                    chan.rods[i] = ItemPileRodMK2.react(stack, neut);
                }
            }
            chan.outgoingNeutrons = producedNeutrons;
            chan.incomingNeutrons = 0;
        }

        for (PileSegment seg : this.segments) {
            if (seg == null || seg.segType != PileChannelType.FUEL) continue;
            double outgoing = 0D;
            for (PileChannel chan : seg.channels) outgoing += chan.outgoingNeutrons;
            for (PileChannel chan : seg.channels) chan.incomingNeutrons += outgoing;
        }

        for (int i = 1; i < this.segments.length - 1; i++) {
            PileSegment seg = this.segments[i];
            if (seg == null || seg.segType != PileChannelType.FUEL) continue;
            double outgoing = 0D;
            for (PileChannel chan : seg.channels) outgoing += chan.outgoingNeutrons;

            double mult = 1D;
            for (int j = i - 1; j >= 1; j--) {
                PileSegment neighbor = this.segments[j];
                if (neighbor == null) continue;
                mult *= neighbor.getNeutronMult(this);
                if (neighbor.segType == PileChannelType.FUEL) {
                    for (PileChannel chan : neighbor.channels) chan.incomingNeutrons += outgoing * mult;
                }
            }

            mult = 1D;
            for (int j = i + 1; j < this.segments.length - 1; j++) {
                PileSegment neighbor = this.segments[j];
                if (neighbor == null) continue;
                mult *= neighbor.getNeutronMult(this);
                if (neighbor.segType == PileChannelType.FUEL) {
                    for (PileChannel chan : neighbor.channels) chan.incomingNeutrons += outgoing * mult;
                }
            }
        }
    }

    protected void handleVentilation() {
        for (PileChannel chan : this.ventilationChannels) {
            if (chan.air <= 0) continue;

            double airCap = (double) chan.air / (double) PileChannel.MAX_AIR;

            for (PileChannel fuel : this.fuelChannels) {
                if (Math.abs(fuel.entry.getPos().getY() - chan.entry.getPos().getY()) <= 1) {
                    fuel.heat *= (1D - airCap * 0.05D);
                }
            }

            int toUse = (int) Math.ceil(airCap * 5D);
            chan.air -= toUse;

            if (level.getGameTime() % 3 != 0) continue;

            double x = chan.entry.getPos().getX() + 0.5 + chan.entry.getDir().getStepX() * (this.width - 0.375);
            double y = chan.entry.getPos().getY() + 0.5;
            double z = chan.entry.getPos().getZ() + 0.5 + chan.entry.getDir().getStepZ() * (this.width - 0.375);

            CompoundTag data = new CompoundTag();
            data.putFloat("lift", 1F);
            data.putFloat("base", (0.125F + level.random.nextFloat() * 0.125F) * (float) airCap);
            data.putFloat("max", 1F * (float) airCap);
            data.putFloat("strafe", 0.0025F);
            data.putBoolean("noWind", true);
            data.putInt("life", 20 + level.random.nextInt(30));
            data.putInt("color", 0xa0a0a0);
            // CE AuxParticlePacketNT(HbmEffectNT.Tower) radius 150. Port TOWER ignores lift/base/max/color.
            HbmEffect.sendPacket(level, HbmEffect.TOWER, x, y, z, 150, data);
        }

        for (PileChannel chan : this.fuelChannels) {
            chan.heat *= 0.999;
            if (chan.heat < 20) chan.heat = 20;
        }
    }

    protected void handleMeltdown() {
        this.highestHeat = 0;
        for (PileChannel chan : this.fuelChannels) {
            if (chan.heat > this.highestHeat) this.highestHeat = chan.heat;
        }

        if (this.highestHeat > MAX_HEAT) {
            this.destroy();
            if (this.fuelChannels.isEmpty()) return;
            double avgX = 0;
            double avgZ = 0;
            for (PileChannel chan : this.fuelChannels) {
                avgX += chan.entry.getPos().getX() + 0.5 + chan.entry.getDir().getStepX() * (chan.length - 1) / 2D;
                avgZ += chan.entry.getPos().getZ() + 0.5 + chan.entry.getDir().getStepZ() * (chan.length - 1) / 2D;
            }
            avgX /= this.fuelChannels.size();
            avgZ /= this.fuelChannels.size();
            meltingDown = true;
            level.explode(null, avgX, worldPosition.getY() + up, avgZ, 15F, true, Level.ExplosionInteraction.TNT);
            meltingDown = false;

            for (int i = 0; i < 15; i++) {
                double mY = level.random.nextDouble() * 0.5 + 1D;
                EntityBulletBaseMK4 fragment = new EntityBulletBaseMK4(level, null, pile_debris, 100F, 0.35F,
                        new Vec3(avgX, worldPosition.getY() + up + 1, avgZ), new Vec3(0, mY, 0));
                level.addFreshEntity(fragment);
            }
        }
    }

    protected void recalculateSegments() {
        this.segments = new PileSegment[width];

        for (PileChannel chan : fuelChannels) {
            int index = getChannelVerticalIndex(chan);
            if (index < 0 || index >= this.segments.length) continue;

            if (this.segments[index] == null) {
                this.segments[index] = new PileSegment(PileChannelType.FUEL).addChan(chan);
            } else {
                if (this.segments[index].segType == PileChannelType.FUEL) this.segments[index].addChan(chan);
            }
        }

        for (PileChannel chan : controlChannels) {
            int index = getChannelVerticalIndex(chan);
            if (index < 0 || index >= this.segments.length) continue;

            if (this.segments[index] == null) {
                this.segments[index] = new PileSegment(PileChannelType.CONTROL).addChan(chan);
            } else {
                if (this.segments[index].segType == PileChannelType.CONTROL) this.segments[index].addChan(chan);
            }
        }
    }

    protected int getChannelVerticalIndex(PileChannel chan) {
        BlockPos entry = chan.entry.getPos();
        Direction right = rotationAroundUp(chan.entry.getDir());
        int deltaX = (entry.getX() - worldPosition.getX()) * right.getStepX();
        int deltaZ = (entry.getZ() - worldPosition.getZ()) * right.getStepZ();
        int abs = deltaX == 0 ? deltaZ : deltaX;
        return abs + this.left;
    }

    /**
     * CE {@code ForgeDirection.getRotation(UP)} via ROTATION_MATRIX[UP].
     * Horizontal dirs = {@link Direction#getClockWise()}; UP/DOWN stay themselves.
     */
    static Direction rotationAroundUp(Direction dir) {
        return switch (dir) {
            case DOWN -> Direction.DOWN;
            case UP -> Direction.UP;
            default -> dir.getClockWise();
        };
    }

    public void destroy() {
        if (level == null) return;
        level.setBlock(worldPosition, PileBlocks.PILE_BRICK.get().defaultBlockState(), 3);
    }

    public enum PileOrientation {
        NORTH_SOUTH,
        EAST_WEST,
        NEITHER;

        public static final PileOrientation[] VALUES = values();

        public static PileOrientation getOrientation(Direction dir) {
            if (dir == Direction.NORTH || dir == Direction.SOUTH) return NORTH_SOUTH;
            if (dir == Direction.EAST || dir == Direction.WEST) return EAST_WEST;
            return NEITHER;
        }
    }

    public class PileChannel {

        public final DirPos entry;
        public final int length;
        public final PileChannelType type;

        public final ItemStack[] rods;
        public double heat = 0D;
        public double outgoingNeutrons = 0D;
        public double incomingNeutrons = 0D;
        public static final int MAX_AIR = 1_000;
        public int air;
        public double control = 1D;

        public PileChannel(int x, int y, int z, Direction dir) {
            this.entry = new DirPos(x, y, z, dir);
            this.type = PileChannelType.getChannelType(dir, orientation);
            this.length = type == PileChannelType.CONTROL ? height
                    : type == PileChannelType.FUEL ? depth : width;
            this.rods = new ItemStack[length];
            for (int i = 0; i < length; i++) this.rods[i] = ItemStack.EMPTY;
        }

        public PileChannel(int x, int y, int z, Direction dir, int length, PileChannelType type) {
            this.entry = new DirPos(x, y, z, dir);
            this.type = type;
            this.length = length;
            this.rods = new ItemStack[length];
            for (int i = 0; i < length; i++) this.rods[i] = ItemStack.EMPTY;
        }

        public void writeChannelToNBT(CompoundTag nbt, String name, HolderLookup.Provider registries) {
            nbt.putInt(name + "_x", entry.getPos().getX());
            nbt.putInt(name + "_y", entry.getPos().getY());
            nbt.putInt(name + "_z", entry.getPos().getZ());
            nbt.putByte(name + "_d", (byte) entry.getDir().get3DDataValue());

            if (type == PileChannelType.FUEL) {
                ListTag list = new ListTag();
                for (int i = 0; i < rods.length; i++) {
                    if (!rods[i].isEmpty()) {
                        CompoundTag nbt1 = (CompoundTag) rods[i].save(registries, new CompoundTag());
                        nbt1.putByte("slot", (byte) i);
                        list.add(nbt1);
                    }
                }
                nbt.put(name + "items", list);
                nbt.putDouble(name + "heat", heat);
                nbt.putDouble(name + "neutrons", incomingNeutrons);
            }

            if (type == PileChannelType.VENTILATION) {
                nbt.putInt(name + "air", air);
            }

            if (type == PileChannelType.CONTROL) {
                nbt.putDouble(name + "control", control);
            }
        }

        public void loadItem(ItemStack stack) {
            if (stack.isEmpty()) return;
            if (rods.length <= 0) {
                dropItem(stack, -1);
                return;
            }

            for (int i = 0; i < rods.length; i++) {
                if (rods[i].isEmpty()) {
                    rods[i] = stack;
                    return;
                } else {
                    ItemStack prev = rods[i];
                    rods[i] = stack;
                    stack = prev;
                }
            }

            dropItem(stack, length);
        }

        public void ejectAll() {
            for (int i = 0; i < this.rods.length; i++) {
                this.dropItem(rods[i], length);
                this.rods[i] = ItemStack.EMPTY;
            }
        }

        public void dropItem(ItemStack stack, int depth) {
            if (stack.isEmpty() || level == null) return;
            int x = entry.getPos().getX() + entry.getDir().getStepX() * depth;
            int y = entry.getPos().getY();
            int z = entry.getPos().getZ() + entry.getDir().getStepZ() * depth;

            stack.remove(MachineDataComponents.PILE_ROD_DEPLETION.get());

            ItemEntity item = new ItemEntity(level, x + 0.5, y + 0.5, z + 0.5, stack);
            level.addFreshEntity(item);
        }
    }

    public PileChannel readChannelFromNBT(CompoundTag nbt, String name, HolderLookup.Provider registries) {
        int x = nbt.getInt(name + "_x");
        int y = nbt.getInt(name + "_y");
        int z = nbt.getInt(name + "_z");
        Direction dir = Direction.from3DDataValue(nbt.getByte(name + "_d"));

        PileChannel chan = new PileChannel(x, y, z, dir);

        if (chan.type == PileChannelType.FUEL) {
            ListTag list = nbt.getList(name + "items", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag nbt1 = list.getCompound(i);
                byte b0 = nbt1.getByte("slot");
                if (b0 >= 0 && b0 < chan.rods.length) {
                    chan.rods[b0] = ItemStack.parseOptional(registries, nbt1);
                }
            }

            chan.heat = nbt.getDouble(name + "heat");
            chan.incomingNeutrons = nbt.getDouble(name + "neutrons");
        }

        if (chan.type == PileChannelType.VENTILATION) {
            chan.air = nbt.getInt(name + "air");
        }

        if (chan.type == PileChannelType.CONTROL) {
            chan.control = nbt.getDouble(name + "control");
        }

        return chan;
    }

    public enum PileChannelType {
        FUEL, VENTILATION, CONTROL;

        public static PileChannelType getChannelType(Direction channelDir, PileOrientation pileOrientation) {
            if (channelDir == Direction.UP || channelDir == Direction.DOWN) {
                return PileChannelType.CONTROL;
            } else if (PileOrientation.getOrientation(channelDir) == pileOrientation) {
                return PileChannelType.FUEL;
            } else {
                return PileChannelType.VENTILATION;
            }
        }
    }

    public static class PileSegment {

        public List<PileChannel> channels = new ArrayList<>();
        public final PileChannelType segType;

        public PileSegment(PileChannelType segType) {
            this.segType = segType;
        }

        public PileSegment addChan(PileChannel chan) {
            this.channels.add(chan);
            return this;
        }

        public double getNeutronMult(PileCoreBlockEntity core) {
            if (this.segType != PileChannelType.CONTROL) return 1D;
            int size = core.depth - 1;
            if (size < 3) return 0D;
            double total = 0D;
            for (PileChannel chan : channels) total += chan.control;
            return Mth.clamp(total / size, 0D, 0.5D);
        }
    }

    public static BulletConfig pile_debris;

    public static final BiConsumer<EntityBulletBaseMK4, HitResult> LAMBDA_STANDARD_EXPLODE = (bullet, mop) -> {
        bullet.level().explode(bullet, bullet.getX(), bullet.getY(), bullet.getZ(), 5F, true, Level.ExplosionInteraction.NONE);
        bullet.discard();
    };

    // TODO(CE: TileEntityPileCore.java:630-632): FlameCreator.META_FIRE trail — ParticleFlameCreator
    // not ported. Debris still explodes 5F on impact.

    static {
        pile_debris = new BulletConfig("pile_debris").setLife(200).setVel(1F).setGrav(0.1F).setOnImpact(LAMBDA_STANDARD_EXPLODE);
    }
}
