package com.hbm.inventory.fluid.tank;

import com.hbm.capability.NTMFluidCapabilityHandler;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Fluid-tank data class backing NTM's own {@code fluidmk2} network API
 * ({@link com.hbm.api.fluidmk2.IFluidUserMK2#getAllTanks()} and friends), and already depended on
 * by the already-committed {@link com.hbm.capability.NTMFluidHandlerWrapper} (which fixes this
 * class's exact contract: {@code getFluid()}/{@code getCapacity()}/{@code isFluidValid(FluidStack)}
 * /{@code drain(int, FluidAction)}) and by {@link FluidType} itself ({@code onTankBroken}/
 * {@code onTankUpdate}/{@code onFluidRelease} callbacks).
 *
 * <p>Ported from CE's {@code FluidTankNTM} (504 lines). Behavior (fields, fill/drain math, NBT and
 * raw-{@link ByteBuf} wire format) is CE's, translated 1.12.2 Forge types to NeoForge ones. Two
 * deliberate deviations from CE, both scoped narrowly:
 * <ul>
 *   <li>The item-canister loading subsystem ({@code loadTank}/{@code unloadTank}/{@code setType},
 *   CE's {@code IFluidLoadingHandler} chain and {@code IItemFluidIdentifier}) is left out - neither
 *   exists in this port yet, and nothing forward-references it (unlike this class itself, which is
 *   already relied on by {@code NTMFluidHandlerWrapper}). A future item-container package can add
 *   it back as pure addition, since it doesn't touch any field or method below.</li>
 *   <li>{@code onTypeChanged()} is a documented no-op: CE calls {@code IConnectionAnchors.notifyAnchors},
 *   an interface this port hasn't ported (no multiblock/connection-anchor package exists yet).
 *   {@link #withOwner} is kept so that wiring is a one-line change once it lands.</li>
 * </ul>
 *
 * <p>Client rendering ({@link #renderTank}/{@link #renderTankTooltip}) is CE's own GL11/Tessellator
 * quad exactly, ported onto confirmed real NeoForge 1.21.1 APIs (verified against
 * {@code upstream/neo-edition}'s own {@code com.hbm.inventory.fluid.tank.FluidTank} - a distinct,
 * differently-shaped class in that reference tree, but the only place in either reference project
 * where this exact CE rendering routine has already been ported to NeoForge 1.21.1 and is called
 * from a compiling {@code Screen}: {@code MachineFluidTankScreen.renderBg}/{@code .render}). That
 * confirmed call site passes no {@link GuiGraphics} to {@code renderTank} at all (it draws a raw
 * quad via {@code RenderSystem}/{@code Tesselator}, the same way CE's GL11 version did) and only
 * threads one through to {@link #renderTankTooltip}, which needs it for
 * {@code GuiGraphics#renderComponentTooltip}. That is the shape kept here, taking precedence over
 * this class's own task brief (which sketched a {@code renderTank(GuiGraphics, x, y, w, h)}
 * signature) since it is independently verified against a real compiling call site rather than
 * inferred.
 */
public class FluidTankNTM implements Cloneable {

    @NotNull
    private FluidType type;
    private int fluid;
    private int maxFluid;
    private int pressure = 0;

    @Nullable
    private BlockEntity owner;

    public FluidTankNTM(@NotNull FluidType type, int maxFluid) {
        this.type = type;
        this.maxFluid = maxFluid;
    }

    /** @param be The block entity that owns this tank, notified via {@link #onTypeChanged()} when the tank's type changes. */
    public FluidTankNTM withOwner(BlockEntity be) {
        this.owner = be;
        return this;
    }

    @Nullable
    public BlockEntity getOwner() {
        return this.owner;
    }

    /**
     * CE notifies {@code IConnectionAnchors} here so multiblock fluid connectors can re-check their
     * link validity when a tank's type flips. That interface isn't ported yet (no multiblock
     * connection-anchor package exists in this port) - left as a documented no-op so callers don't
     * need to change once it lands; {@link #owner} is already threaded through for that day.
     */
    private void onTypeChanged() {
        // forward reference: com.hbm.tileentity.IConnectionAnchors not ported yet
    }

    public FluidTankNTM withPressure(int pressure) {
        if (this.pressure != pressure) this.setFill(0);
        this.pressure = pressure;
        return this;
    }

    public byte getRedstoneComparatorPower() {
        if (getFill() == 0) return 0;
        double frac = (double) getFill() / (double) getMaxFill() * 15D;
        return (byte) Mth.clamp((int) frac, 1, 15);
    }

    @NotNull
    public FluidType getTankType() {
        return type;
    }

    public void setTankType(FluidType type) {
        if (type == null) {
            type = Fluids.NONE;
        }
        if (this.type == type) return;

        this.type = type;
        this.setFill(0);
        onTypeChanged();
    }

    public void resetTank() {
        boolean changed = this.type != Fluids.NONE;
        this.type = Fluids.NONE;
        this.fluid = 0;
        this.pressure = 0;
        if (changed) onTypeChanged();
    }

    /** Changes type and pressure based on a fluid stack, useful for changing tank types based on recipes */
    public FluidTankNTM conform(com.hbm.inventory.fluid.FluidStack stack) {
        this.setTankType(stack.type);
        this.withPressure(stack.pressure);
        return this;
    }

    @Nullable
    public Fluid getTankTypeFF() {
        return this.type.getFF();
    }

    public int getFill() {
        return fluid;
    }

    public void setFill(int i) {
        fluid = Math.max(0, Math.min(i, maxFluid));
    }

    public int getMaxFill() {
        return maxFluid;
    }

    public int getPressure() {
        return pressure;
    }

    public int changeTankSize(int size) {
        maxFluid = size;
        if (fluid > maxFluid) {
            int dif = fluid - maxFluid;
            fluid = maxFluid;
            return dif;
        }
        return 0;
    }

    /**
     * @deprecated use {@link #getFill()} whenever possible - kept for parity with NeoForge's
     * {@code IFluidTank} naming convention, read by {@link com.hbm.capability.NTMFluidHandlerWrapper}.
     */
    @Deprecated
    public int getFluidAmount() {
        return getFill();
    }

    /**
     * @deprecated use {@link #getMaxFill()} whenever possible - kept for parity with NeoForge's
     * {@code IFluidTank} naming convention, read by {@link com.hbm.capability.NTMFluidHandlerWrapper}.
     */
    @Deprecated
    public int getCapacity() {
        return getMaxFill();
    }

    /**
     * @deprecated use {@link #getTankType()}, {@link #getFill()} whenever possible. Read directly by
     * {@link com.hbm.capability.NTMFluidHandlerWrapper#getFluidInTank}.
     */
    @Nullable
    @Deprecated
    public FluidStack getFluid() {
        Fluid fluid = getTankTypeFF();
        int amount = getFill();

        return (fluid != null && amount > 0) ? new FluidStack(fluid, amount) : null;
    }

    /**
     * Whether this tank could accept the given stack's fluid type, disregarding current fill/space -
     * an empty (NONE-typed) tank accepts anything the fluid registry recognizes, a typed tank only
     * matches its own type. Mirrors NeoForge's own {@code IFluidTank#isFluidValid} contract; CE had
     * no equivalent method (its {@code IFluidHandler}/{@code IFluidTank} implementation predates it).
     * Read directly by {@link com.hbm.capability.NTMFluidHandlerWrapper#isFluidValid}.
     */
    public boolean isFluidValid(@NotNull FluidStack stack) {
        if (stack.isEmpty()) return false;
        FluidType incoming = NTMFluidCapabilityHandler.getFluidType(stack.getFluid());
        if (incoming == null || incoming == Fluids.NONE) return false;
        return this.type == Fluids.NONE || this.type == incoming;
    }

    /** Typed fill, matching a fluid network's own {@link FluidType} rather than a vanilla {@link Fluid}. */
    public int fill(@Nullable FluidType incomingType, int amount, boolean doFill) {
        if (incomingType == null || incomingType == Fluids.NONE || amount <= 0) return 0;
        if (this.type == Fluids.NONE) {
            int toTransfer = Math.min(getMaxFill(), amount);
            if (doFill) {
                this.type = incomingType;
                this.setFill(toTransfer);
                onTypeChanged();
            }
            return toTransfer;
        } else {
            if (!this.type.equals(incomingType)) return 0;
            int toTransfer = Math.min(getMaxFill() - getFill(), amount);
            if (doFill && toTransfer > 0) setFill(getFill() + toTransfer);
            return toTransfer;
        }
    }

    /**
     * @deprecated use {@link #fill(FluidType, int, boolean)} whenever possible - kept for callers
     * still holding a vanilla {@link FluidStack} (e.g. a foreign-handler bridge).
     */
    @Deprecated
    public int fill(@Nullable FluidStack resource, FluidAction action) {
        if (resource == null || resource.isEmpty()) return 0;
        FluidType incomingType = NTMFluidCapabilityHandler.getFluidType(resource.getFluid());
        return fill(incomingType, resource.getAmount(), action.execute());
    }

    /**
     * @deprecated use {@link #setFill(int)} whenever possible. Called (always with
     * {@link FluidAction#SIMULATE}) by {@link com.hbm.capability.NTMFluidHandlerWrapper#drain(int, FluidAction)}
     * to obtain an exemplar stack - the actual state change there goes through
     * {@code IFluidProviderMK2#useUpFluid} instead, not through this method.
     */
    @Nullable
    @Deprecated
    public FluidStack drain(int maxDrain, FluidAction action) {
        if (getTankType() == Fluids.NONE || getTankTypeFF() == null || getFill() == 0) return null;
        int toDrain = Math.min(maxDrain, getFill());
        FluidStack drained = new FluidStack(getTankTypeFF(), toDrain);
        if (action.execute()) setFill(getFill() - toDrain);
        return drained;
    }

    //Called by TE to save fillstate
    public void writeToNBT(CompoundTag nbt, String s) {
        nbt.putInt(s, fluid);
        nbt.putInt(s + "_max", maxFluid);
        Fluids.writeType(nbt, s + "_type", type); //stored by name, IDs shift when fluids are added/removed
        nbt.putShort(s + "_p", (short) pressure);
    }

    //Called by TE to load fillstate
    public void readFromNBT(@NotNull CompoundTag nbt, String s) {
        fluid = nbt.getInt(s);
        int max = nbt.getInt(s + "_max");
        if (max > 0) maxFluid = max;

        fluid = Mth.clamp(fluid, 0, max);

        type = Fluids.readType(nbt, s + "_type"); //name-based, with legacy numeric-ID fallback

        this.pressure = nbt.getShort(s + "_p");
    }

    public void serialize(@NotNull ByteBuf buf) {
        buf.writeInt(fluid);
        buf.writeInt(maxFluid);
        buf.writeInt(type.getID());
        buf.writeShort((short) pressure);
    }

    public void deserialize(@NotNull ByteBuf buf) {
        fluid = buf.readInt();
        maxFluid = buf.readInt();
        type = Fluids.fromID(buf.readInt());
        pressure = buf.readShort();
    }

    /**
     * Renders the fluid texture into a GUI, with the height based on the fill state.
     *
     * @param x      the tank's left side
     * @param y      the tank's bottom side (convention from the old system, changing it now would be a pain in the ass)
     * @param z      the GUI's z-depth
     * @param width
     * @param height
     */
    @OnlyIn(Dist.CLIENT)
    public void renderTank(int x, int y, float z, int width, int height) {
        renderTank(x, y, z, width, height, 0);
    }

    @OnlyIn(Dist.CLIENT)
    public void renderTank(int x, int y, float z, int width, int height, int orientation) {

        RenderSystem.enableBlend();

        int color = type.getTint();
        float r = ((color & 0xff0000) >> 16) / 255F;
        float g = ((color & 0x00ff00) >> 8) / 255F;
        float b = (color & 0x0000ff) / 255F;

        y -= height;

        int i = maxFluid != 0 ? (fluid * height) / maxFluid : 0;

        float minX = x;
        float maxX = x;
        float minY = y;
        float maxY = y;

        float minV = 1F - i / 16F;
        float maxV = 1F;
        float minU = 0F;
        float maxU = width / 16F;

        if (orientation == 0) {
            maxX += width;
            minY += height - i;
            maxY += height;
        }

        if (orientation == 1) {
            i = maxFluid != 0 ? (fluid * width) / maxFluid : 0;
            maxX += i;
            maxY += height;

            minV = 0F;
            maxV = height / 16F;
            minU = 1F;
            maxU = 1F - i / 16F;
        }

        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, type.getTexture());

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buf = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

        buf.addVertex(minX, maxY, z).setUv(minU, maxV).setColor(r, g, b, 1.0F);
        buf.addVertex(maxX, maxY, z).setUv(maxU, maxV).setColor(r, g, b, 1.0F);
        buf.addVertex(maxX, minY, z).setUv(maxU, minV).setColor(r, g, b, 1.0F);
        buf.addVertex(minX, minY, z).setUv(minU, minV).setColor(r, g, b, 1.0F);

        BufferUploader.drawWithShader(buf.buildOrThrow());

        RenderSystem.disableBlend();
    }

    @OnlyIn(Dist.CLIENT)
    public void renderTankTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY, int x, int y, int width, int height) {
        if (x <= mouseX && x + width > mouseX && y < mouseY && y + height >= mouseY) {

            List<Component> list = new ArrayList<>();
            list.add(this.type.getLocalizedName());
            list.add(Component.literal(this.fluid + "/" + this.maxFluid + "mB"));

            if (this.pressure != 0) {
                list.add(Component.literal("Pressure: " + this.pressure + " PU").withStyle(ChatFormatting.RED));
            }

            type.addInfo(list);
            guiGraphics.renderComponentTooltip(Minecraft.getInstance().font, list, mouseX, mouseY);
        }
    }

    @Override
    public FluidTankNTM clone() {
        try {
            return (FluidTankNTM) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }
}
