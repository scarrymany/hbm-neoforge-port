package com.hbm.entity.train;

import com.hbm.blocks.rail.IRailNTM.TrackGauge;
import com.hbm.inventory.container.TrainCargoTramMenu;
import com.hbm.items.tool.ToolItems;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Port of CE's {@code com.hbm.entity.train.TrainCargoTram} (188 lines) - CE's only concrete
 * rideable/powered rail car (confirmed by {@code ItemTrain.EnumTrainType}'s 2-constant enum - CE
 * ships exactly 2 rail-car types total). 5x2 hitbox, standard gauge, 2 passenger seats plus the
 * driver's own seat, a 29-slot cargo inventory (slot 28 reserved for a battery/charge item).
 * {@code getMaxRailSpeed()} (1) exceeding {@code getMaxPoweredSpeed()} (0.5) is CE's own real design
 * (coasting/gravity-assist can, in principle, exceed motor-only speed) - not a typo, preserved as-is.
 * <p>
 * <b>Real CE oddity found and fixed here (see this package's {@code realBugsFound}):</b> a full grep
 * of CE's own source found {@code TrainCargoTram} never overrides {@code processInitialInteract}, so
 * every right-click on it resolves to {@code EntityRailCarRidable}'s mount/seat-picking logic only -
 * CE's own {@code IGUIProvider}/{@code provideContainer}/{@code provideGUI} pair on this class is
 * <b>unreachable through normal play</b> (no sneak-modifier branch, no separate GUI-opening item found
 * anywhere in CE that targets this specific class). Preserving that dead end verbatim would make this
 * package's own newly-built {@link TrainCargoTramMenu}/{@link com.hbm.inventory.container.EntityMenuBase}
 * infrastructure permanently unreachable too - not a faithful port so much as a faithful copy of an
 * oversight. This port adds one small, documented behavior: <b>sneak + right-click opens the cargo
 * menu</b> instead of mounting/seat-picking (a plain right-click keeps CE's exact ride behavior,
 * and the coupling tool still takes priority over both, matching {@link EntityRailCarBase}'s own
 * always-first coupling check).
 */
public class TrainCargoTram extends EntityRailCarElectric implements MenuProvider {

    public TrainCargoTram(EntityType<? extends TrainCargoTram> type, Level level) {
        super(type, level);
    }

    public TrainCargoTram(Level level) {
        this(TrainEntityTypes.CARGO_TRAM.get(), level);
    }

    @Override public double getPoweredAcceleration() { return 0.01; }
    @Override public double getPassivBrake() { return 0.95; }
    @Override public boolean shouldUseEngineBrake(Player player) { return Math.abs(this.engineSpeed) < 0.1; }
    @Override public double getMaxPoweredSpeed() { return 0.5; }
    @Override public double getMaxRailSpeed() { return 1; }

    @Override public TrackGauge getGauge() { return TrackGauge.STANDARD; }
    @Override public double getLengthSpan() { return 1.5; }
    @Override public double getCollisionSpan() { return 2.5; }
    @Override public Vec3 getRiderSeatPosition() { return new Vec3(0.375, 2.375, 0.5); }
    @Override public int getContainerSize() { return 29; }
    @Override public double getCouplingDist(TrainCoupling coupling) { return coupling != null ? 2.75 : 0; }

    @Override public int getMaxPower() { return this.getPowerConsumption() * 100; }
    @Override public int getPowerConsumption() { return 10; }
    @Override public boolean hasChargeSlot() { return true; }
    @Override public int getChargeSlot() { return 28; }

    @Override
    public Component getTypeName() {
        return Component.translatable("container.trainTram");
    }

    @Override
    public DummyConfig[] getDummies() {
        return new DummyConfig[] {
                new DummyConfig(2F, 1F, new Vec3(0, 0, 1.5)),
                new DummyConfig(2F, 1F, new Vec3(0, 0, 0)),
                new DummyConfig(2F, 1F, new Vec3(0, 0, -1.5))
        };
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!this.level().isClientSide && this.isAlive()) {
            this.discard();
        }
        return true;
    }

    @Override
    public Vec3[] getPassengerSeats() {
        return new Vec3[] {
                new Vec3(0.5, 1.75, -1.5),
                new Vec3(-0.5, 1.75, -1.5)
        };
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        boolean isCouplingTool = !held.isEmpty() && held.getItem() == ToolItems.COUPLING_TOOL.get();

        if (!isCouplingTool && player.isShiftKeyDown()) {
            if (!this.level().isClientSide) {
                player.openMenu(this, buf -> buf.writeVarInt(this.getId()));
            }
            return InteractionResult.SUCCESS;
        }

        return super.interact(player, hand);
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new TrainCargoTramMenu(id, playerInventory, this);
    }
}
