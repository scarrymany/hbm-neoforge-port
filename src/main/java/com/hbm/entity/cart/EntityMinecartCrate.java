package com.hbm.entity.cart;

import com.hbm.inventory.container.MinecartCrateMenu;
import com.hbm.items.tool.CartItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Port of CE's {@code com.hbm.entity.cart.EntityMinecartCrate} (202 lines) - a 54-slot crate cart.
 * <p>
 * <b>This is the actual "explosive minecart" in this family</b> (the report's Headline finding #3,
 * correcting {@code docs/phase1/items_tool.md}'s guess that {@link EntityMinecartSemtex} was the
 * explosive one): {@link #killMinecart}'s NBT-size safety valve triggers a small block-damaging blast
 * plus an extra empty-crate-item drop if this cart's serialized inventory NBT exceeds 6000 compressed
 * bytes (measured the same way CE measures it - a gzip-compressed NBT byte count via {@link NbtIo}) -
 * a data-safety mechanic against a pathologically large save payload, not a themed "explosive cart".
 * <p>
 * <b>Cargo-preservation adaptation, documented.</b> CE's item form round-trips this cart's full
 * inventory through the dropped item's own NBT tag (so picking the item back up and re-placing it
 * restores the cargo) via {@code ItemModMinecart}'s constructor/item-NBT machinery - real 1.12
 * {@code items/tool} content this package does not claim (see this package's own {@code knownGaps}).
 * Since that item-side round-trip isn't available yet, this port instead spills the cart's actual
 * inventory contents onto the ground directly on death (via {@link Containers#dropItemStack}) so the
 * cargo itself is never silently lost, while still dropping a plain (empty) {@link #getCartItem()}
 * for the cart shell itself - preserving CE's real intent ("don't lose the cargo") through the
 * mechanism this port's current scope supports.
 */
public class EntityMinecartCrate extends EntityMinecartContainerBase implements MenuProvider {

    public static final int SIZE = 9 * 6;

    public EntityMinecartCrate(EntityType<? extends EntityMinecartCrate> type, Level level) {
        super(type, level);
    }

    public EntityMinecartCrate(Level level) {
        this(CartEntityTypes.CART_CRATE.get(), level);
    }

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    public Item getCartItem() {
        return CartItems.CART_CRATE.get();
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (!this.level().isClientSide()) {
            player.openMenu(this, buf -> buf.writeVarInt(this.getId()));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new MinecartCrateMenu(id, playerInventory, this);
    }

    @Override
    protected void killMinecart(DamageSource source) {
        Level level = this.level();

        for (ItemStack stack : this.items) {
            if (!stack.isEmpty()) Containers.dropItemStack(level, getX(), getY(), getZ(), stack);
        }

        CompoundTag nbt = new CompoundTag();
        ContainerHelper.saveAllItems(nbt, this.items, this.registryAccess());

        if (estimateCompressedSize(nbt) > 6000) {
            level.explode(this, getX(), getY(), getZ(), 2F, true, Level.ExplosionInteraction.BLOCK);
            this.spawnAtLocation(new ItemStack(getCartItem()), 0F);
        }

        super.killMinecart(source);
    }

    /** CE: measures the item's serialized NBT via {@code CompressedStreamTools.writeCompressed} into a
     *  byte buffer and checks {@code abyte.length > 6000} - {@link NbtIo} is 1.21.1's confirmed direct
     *  rename of that same class/method (well-established Mojang-mapping knowledge). */
    private static int estimateCompressedSize(CompoundTag nbt) {
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            NbtIo.writeCompressed(nbt, buffer);
            return buffer.size();
        } catch (IOException e) {
            return 0;
        }
    }
}
