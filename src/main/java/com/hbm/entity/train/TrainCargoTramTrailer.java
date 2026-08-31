package com.hbm.entity.train;

import com.hbm.blocks.rail.IRailNTM.TrackGauge;
import com.hbm.inventory.container.TrainCargoTramTrailerMenu;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Port of CE's {@code com.hbm.entity.train.TrainCargoTramTrailer} (158 lines) - the non-powered half
 * of CE's one concrete rail-car set: {@code getCurrentSpeed()} hardcoded to 0 (never drives itself,
 * only ever towed via coupling to a {@link TrainCargoTram}), 45-slot cargo inventory, opened via
 * ordinary right-click (unlike the rideable {@link TrainCargoTram}, which per that class's own
 * javadoc needs a sneak-modifier to reach its cargo GUI at all since a plain right-click mounts it).
 * <p>
 * Minor deliberate deviation from CE: CE's own {@code processInitialInteract} returns {@code false}
 * (not {@code true}) after a successful coupling-tool action before falling through to open the GUI
 * check - a likely CE quirk (returning "not handled" despite having just handled it) with no clear
 * intentional effect, since the coupling itself already fully happened via the super call's side
 * effects either way. This port simply propagates whatever {@link EntityRailCarBase#interact}
 * returned when it was not {@code InteractionResult.PASS}, which is the unambiguously correct shape.
 */
public class TrainCargoTramTrailer extends EntityRailCarCargo implements MenuProvider {

    public TrainCargoTramTrailer(EntityType<? extends TrainCargoTramTrailer> type, Level level) {
        super(type, level);
    }

    public TrainCargoTramTrailer(Level level) {
        this(TrainEntityTypes.CARGO_TRAM_TRAILER.get(), level);
    }

    @Override public double getMaxRailSpeed() { return 1; }
    @Override public TrackGauge getGauge() { return TrackGauge.STANDARD; }
    @Override public double getLengthSpan() { return 1.5; }
    @Override public double getCollisionSpan() { return 2.5; }
    @Override public int getContainerSize() { return 45; }
    @Override public double getCouplingDist(TrainCoupling coupling) { return coupling != null ? 2.75 : 0; }
    @Override public double getCurrentSpeed() { return 0; }

    @Override
    public Component getTypeName() {
        return Component.translatable("container.trainTramTrailer");
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
    public InteractionResult interact(Player player, InteractionHand hand) {
        InteractionResult superResult = super.interact(player, hand);
        if (superResult != InteractionResult.PASS) return superResult;

        if (!this.level().isClientSide) {
            player.openMenu(this, buf -> buf.writeVarInt(this.getId()));
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new TrainCargoTramTrailerMenu(id, playerInventory, this);
    }
}
