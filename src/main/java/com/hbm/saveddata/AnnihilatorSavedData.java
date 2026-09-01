package com.hbm.saveddata;

import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.AnnihilatorRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

/**
 * CE {@code AnnihilatorSavedData.java}. Per-world pool counters + milestone payout.
 */
public class AnnihilatorSavedData extends SavedData {

    public static final String KEY = "annihilator";

    public final HashMap<String, AnnihilatorPool> pools = new HashMap<>();

    public static SavedData.Factory<AnnihilatorSavedData> factory() {
        return new SavedData.Factory<>(AnnihilatorSavedData::new, AnnihilatorSavedData::load);
    }

    public static AnnihilatorSavedData getData(Level level) {
        if (!(level instanceof ServerLevel serverLevel)) {
            throw new IllegalStateException("AnnihilatorSavedData is server-only");
        }
        return serverLevel.getDataStorage().computeIfAbsent(factory(), KEY);
    }

    public AnnihilatorPool grabPool(String pool) {
        return pools.computeIfAbsent(pool, k -> new AnnihilatorPool());
    }

    public ItemStack pushToPool(String pool, FluidType type, long amount, boolean alwaysPayOut) {
        ItemStack payout = grabPool(pool).increment(type, amount, alwaysPayOut);
        setDirty();
        return payout;
    }

    public ItemStack pushToPool(String pool, ItemStack stack, boolean alwaysPayOut) {
        AnnihilatorPool poolInstance = grabPool(pool);
        ItemStack itemPayout = poolInstance.increment(stack.getItem(), stack.getCount(), alwaysPayOut);
        ItemStack compPayout = poolInstance.increment(new ComparableStack(stack.getItem()), stack.getCount(), alwaysPayOut);
        setDirty();
        if (compPayout != null && !compPayout.isEmpty()) return compPayout;
        return itemPayout == null ? ItemStack.EMPTY : itemPayout;
    }

    public static AnnihilatorSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        AnnihilatorSavedData data = new AnnihilatorSavedData();
        ListTag pools = tag.getList("pools", Tag.TAG_COMPOUND);
        for (int i = 0; i < pools.size(); i++) {
            CompoundTag poolCompound = pools.getCompound(i);
            AnnihilatorPool pool = new AnnihilatorPool();
            pool.deserialize(poolCompound.getList("pool", Tag.TAG_COMPOUND));
            data.pools.put(poolCompound.getString("poolname"), pool);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag poolsTag = new ListTag();
        for (Map.Entry<String, AnnihilatorPool> entry : pools.entrySet()) {
            CompoundTag pool = new CompoundTag();
            ListTag poolList = new ListTag();
            entry.getValue().serialize(poolList);
            pool.putString("poolname", entry.getKey());
            pool.put("pool", poolList);
            poolsTag.add(pool);
        }
        tag.put("pools", poolsTag);
        return tag;
    }

    public static class AnnihilatorPool {
        public final HashMap<Object, BigInteger> items = new HashMap<>();

        public ItemStack increment(Object type, long amount, boolean alwaysPayOut) {
            BigInteger counter = items.get(type);
            ItemStack payout;
            if (counter == null) {
                counter = BigInteger.valueOf(amount);
                payout = AnnihilatorRecipes.getHighestPayoutFromKey(type, BigInteger.ZERO, counter);
            } else {
                BigInteger prev = counter;
                counter = counter.add(BigInteger.valueOf(amount));
                payout = AnnihilatorRecipes.getHighestPayoutFromKey(type, alwaysPayOut ? null : prev, counter);
            }
            items.put(type, counter);
            return payout == null ? ItemStack.EMPTY : payout;
        }

        public void serialize(ListTag nbt) {
            for (Map.Entry<Object, BigInteger> entry : items.entrySet()) {
                CompoundTag compound = new CompoundTag();
                serializeKey(compound, entry.getKey());
                compound.putByteArray("amount", entry.getValue().toByteArray());
                nbt.add(compound);
            }
        }

        public void deserialize(ListTag nbt) {
            try {
                for (int i = 0; i < nbt.size(); i++) {
                    CompoundTag compound = nbt.getCompound(i);
                    Object key = deserializeKey(compound);
                    if (key != null) items.put(key, new BigInteger(compound.getByteArray("amount")));
                }
            } catch (Throwable ignored) {
            }
        }

        public void serializeKey(CompoundTag nbt, Object key) {
            if (key instanceof Item item) {
                nbt.putByte("key", (byte) 0);
                ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
                nbt.putString("item", id.toString());
            }
            if (key instanceof ComparableStack comp) {
                nbt.putByte("key", (byte) 1);
                ResourceLocation id = BuiltInRegistries.ITEM.getKey(comp.item);
                nbt.putString("item", id.toString());
            }
            if (key instanceof FluidType type) {
                nbt.putByte("key", (byte) 2);
                nbt.putString("fluid", type.getName());
            }
            if (key instanceof String s) {
                nbt.putByte("key", (byte) 3);
                nbt.putString("dict", s);
            }
        }

        public Object deserializeKey(CompoundTag nbt) {
            try {
                byte key = nbt.getByte("key");
                if (key == 0) {
                    return BuiltInRegistries.ITEM.get(ResourceLocation.parse(nbt.getString("item")));
                }
                if (key == 1) {
                    return new ComparableStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(nbt.getString("item"))));
                }
                if (key == 2) {
                    return Fluids.fromName(nbt.getString("fluid"));
                }
                if (key == 3) {
                    return nbt.getString("dict");
                }
            } catch (Throwable ignored) {
            }
            return null;
        }
    }
}
