package com.hbm.blockentity.machine.fusion;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.LoadedBaseBlockEntity;
import com.hbm.inventory.recipes.FusionRecipes;
import com.hbm.uninos.UniNodespace;
import com.hbm.uninos.networkproviders.KlystronNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** CE {@code TileEntityFusionKlystronCreative} — dumps {@code FusionRecipes.maxInput}. */
public class FusionKlystronCreativeBlockEntity extends LoadedBaseBlockEntity implements ITickableBE {

    protected KlystronNetwork.KlystronNode klystronNode;
    public boolean isConnected;

    public FusionKlystronCreativeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;
        this.klystronNode = FusionKlystronBlockEntity.handleKNode(klystronNode, this);
        this.isConnected = FusionKlystronBlockEntity.provideKyU(klystronNode, FusionRecipes.INSTANCE.maxInput);
        networkPackNT(100);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && !level.isClientSide && klystronNode != null) {
            UniNodespace.destroyNode(level, klystronNode);
        }
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeBoolean(isConnected);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        isConnected = buf.readBoolean();
    }
}
