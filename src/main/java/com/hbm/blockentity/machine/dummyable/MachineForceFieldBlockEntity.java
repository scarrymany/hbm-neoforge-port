package com.hbm.blockentity.machine.dummyable;

import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.inventory.container.machine.dummyable.ForceFieldMenu;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.lib.Library;
import com.hbm.main.MainRegistry;
import com.hbm.tileentity.IConfigurableMachine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * CE {@code TileEntityForceField} (460 lines). Live bounce field.
 * {@link IConfigurableMachine} Exact CE {@code TileEntityForceField.java:71}/{@code :436-458}
 * ({@code forcefield}). TODO(CE: RenderMachineForceField.java:20): TESR sphere. Do not invent.
 */
public class MachineForceFieldBlockEntity extends MachineBaseBlockEntity
        implements IEnergyReceiverMK2, ITickableBE, MenuProvider, IConfigurableMachine {

    public static int baseCon = 1000;
    public static int radCon = 500;
    public static int shCon = 250;
    public static long maxPower = 1_000_000L;
    public static int baseRadius = 16;
    public static int radUpgrade = 16;
    public static int shUpgrade = 50;
    public static double cooldownModif = 1;
    public static double healthRegenModif = 1;

    public int health = 100;
    public int maxHealth = 100;
    public long power;
    public int powerCons;
    public int cooldown;
    public int blink;
    public float radius = 16;
    public boolean isOn;
    public int color = 0x0000FF;
    private final List<Entity> outside = new ArrayList<>();
    private final List<Entity> inside = new ArrayList<>();

    public MachineForceFieldBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 3, false, true);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.forceField");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == 0) return Library.isBattery(stack);
        if (slot == 1) return stack.is(item("upgrade_radius"));
        if (slot == 2) return stack.is(item("upgrade_health"));
        return false;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(Direction side) {
        return new int[]{0, 1, 2};
    }

    public int getHealthScaled(int i) {
        return (health * i) / Math.max(1, maxHealth);
    }

    public long getPowerScaled(long i) {
        return (power * i) / Math.max(1, maxPower);
    }

    public void toggleOn() {
        isOn = !isOn;
        setChanged();
    }

    @Override
    public void updateEntity() {
        if (level == null) return;

        if (!level.isClientSide) {
            updateConnections();
            int rStack = 0;
            int hStack = 0;
            radius = baseRadius;
            maxHealth = 100;

            ItemStack rUp = inventory.getStackInSlot(1);
            if (rUp.is(item("upgrade_radius"))) {
                rStack = rUp.getCount();
                radius += rStack * radUpgrade;
            }
            ItemStack hUp = inventory.getStackInSlot(2);
            if (hUp.is(item("upgrade_health"))) {
                hStack = hUp.getCount();
                maxHealth += hStack * shUpgrade;
            }

            this.powerCons = baseCon + rStack * radCon + hStack * shCon;
            power = Library.chargeTEFromItems(inventory, 0, power, maxPower);

            if (blink > 0) {
                blink--;
                color = 0xFF0000;
            } else {
                color = 0x00FF00;
            }
        }

        if (cooldown > 0) {
            cooldown--;
        } else {
            if (health < maxHealth)
                health += (int) (((double) maxHealth / 100) * healthRegenModif);
            if (health > maxHealth) health = maxHealth;
        }

        if (isOn && cooldown == 0 && health > 0 && power >= powerCons) {
            doField(radius);
            if (!level.isClientSide) power -= powerCons;
        } else {
            outside.clear();
            inside.clear();
        }

        if (!level.isClientSide) {
            if (power < powerCons) power = 0;
            dataChanged();
            networkPackMK2(100);
        }
    }

    private void updateConnections() {
        trySubscribe(level, worldPosition.offset(1, 0, 0), Direction.EAST);
        trySubscribe(level, worldPosition.offset(-1, 0, 0), Direction.WEST);
        trySubscribe(level, worldPosition.offset(0, 0, 1), Direction.SOUTH);
        trySubscribe(level, worldPosition.offset(0, 0, -1), Direction.NORTH);
        trySubscribe(level, worldPosition.offset(0, -1, 0), Direction.DOWN);
    }

    private int impact(Entity e) {
        double mass = e.getBbHeight() * e.getBbWidth() * e.getBbWidth();
        return (int) (mass * getMotionWithFallback(e) * 50);
    }

    private void damage(int ouch) {
        health -= ouch;
        if (ouch >= (this.maxHealth / 250)) blink = 5;
        if (health <= 0) {
            health = 0;
            cooldown = (int) (100 + radius);
        }
    }

    private void doField(float rad) {
        List<Entity> oLegacy = new ArrayList<>(outside);
        List<Entity> iLegacy = new ArrayList<>(inside);
        outside.clear();
        inside.clear();

        double cx = worldPosition.getX() + 0.5;
        double cy = worldPosition.getY() + 0.5;
        double cz = worldPosition.getZ() + 0.5;
        List<Entity> list = level.getEntities(null, new AABB(
                cx - (rad + 25), cy - (rad + 25), cz - (rad + 25),
                cx + (rad + 25), cy + (rad + 25), cz + (rad + 25)));

        for (Entity entity : list) {
            if (entity instanceof Player || entity instanceof ItemEntity) continue;

            double dist = Math.sqrt(
                    Math.pow(cx - entity.getX(), 2)
                            + Math.pow(cy - entity.getY(), 2)
                            + Math.pow(cz - entity.getZ(), 2));
            boolean out = dist > rad;

            if (!oLegacy.contains(entity) && !iLegacy.contains(entity)) {
                if (out) outside.add(entity);
                else inside.add(entity);
            } else if (oLegacy.contains(entity) && !out) {
                bounce(entity, rad + 1, true);
                outside.add(entity);
                if (!level.isClientSide) damage(impact(entity));
            } else if (iLegacy.contains(entity) && out) {
                bounce(entity, rad - 1, false);
                inside.add(entity);
                if (!level.isClientSide) damage(impact(entity));
            } else if (out) {
                outside.add(entity);
            } else {
                inside.add(entity);
            }
        }
    }

    private void bounce(Entity entity, double push, boolean inward) {
        double cx = worldPosition.getX() + 0.5;
        double cy = worldPosition.getY() + 0.5;
        double cz = worldPosition.getZ() + 0.5;
        Vec3 vec = new Vec3(cx - entity.getX(), cy - entity.getY(), cz - entity.getZ()).normalize();
        entity.teleportTo(cx + (-vec.x * push), cy + (-vec.y * push), cz + (-vec.z * push));
        double mo = entity.getDeltaMovement().length();
        double sign = inward ? -1 : 1;
        entity.setDeltaMovement(vec.x * sign * mo, vec.y * sign * mo, vec.z * sign * mo);
        Vec3 mot = entity.getDeltaMovement();
        entity.setPos(entity.getX() - mot.x, entity.getY() - mot.y, entity.getZ() - mot.z);
        if (!this.muffled) {
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                    HBMSoundHandler.sparkShoot.get(), SoundSource.BLOCKS, 2.5F, 1.0F);
        }
    }

    /** CE copies {@code posX - prevPosY} as-is (TileEntityForceField.java:365). */
    private double getMotionWithFallback(Entity e) {
        Vec3 v1 = e.getDeltaMovement();
        Vec3 v2 = new Vec3(e.getX() - e.yo, e.getY() - e.yo, e.getZ() - e.zo);
        double s1 = v1.length();
        double s2 = v2.length();
        if (s1 == 0) return s2;
        if (s2 == 0) return s1;
        return Math.min(s1, s2);
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, id));
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long i) {
        power = i;
    }

    @Override
    public long getMaxPower() {
        return maxPower;
    }

    public AABB getRenderBoundingBox() {
        double r = radius;
        return new AABB(worldPosition.getX() - r, worldPosition.getY() - r, worldPosition.getZ() - r,
                worldPosition.getX() + 1 + r, worldPosition.getY() + 1 + r, worldPosition.getZ() + 1 + r);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("powerTime", power);
        tag.putInt("health", health);
        tag.putInt("maxHealth", maxHealth);
        tag.putInt("cooldown", cooldown);
        tag.putInt("blink", blink);
        tag.putFloat("radius", radius);
        tag.putBoolean("isOn", isOn);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        power = tag.getLong("powerTime");
        health = tag.getInt("health");
        maxHealth = tag.getInt("maxHealth");
        cooldown = tag.getInt("cooldown");
        blink = tag.getInt("blink");
        radius = tag.getFloat("radius");
        isOn = tag.getBoolean("isOn");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        buf.writeInt(health);
        buf.writeInt(maxHealth);
        buf.writeInt(cooldown);
        buf.writeInt(blink);
        buf.writeFloat(radius);
        buf.writeBoolean(isOn);
        buf.writeInt(color);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        power = buf.readLong();
        health = buf.readInt();
        maxHealth = buf.readInt();
        cooldown = buf.readInt();
        blink = buf.readInt();
        radius = buf.readFloat();
        isOn = buf.readBoolean();
        color = buf.readInt();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new ForceFieldMenu(id, inv, this);
    }

    @Override
    public String getConfigName() {
        return "forcefield";
    }

    @Override
    public void readIfPresent(JsonObject obj) {
        readConfig(obj);
    }

    @Override
    public void writeConfig(JsonWriter writer) throws IOException {
        writeConfigStatic(writer);
    }

    static void readConfig(JsonObject obj) {
        // CE TileEntityForceField.java:437-445
        maxPower = IConfigurableMachine.grab(obj, "L:powerCap", maxPower);
        baseCon = IConfigurableMachine.grab(obj, "I:baseConsumption", baseCon);
        radCon = IConfigurableMachine.grab(obj, "I:radiusConsumption", radCon);
        shCon = IConfigurableMachine.grab(obj, "I:shieldConsumption", shCon);
        baseRadius = IConfigurableMachine.grab(obj, "I:baseRadius", baseRadius);
        radUpgrade = IConfigurableMachine.grab(obj, "I:radiusUpgrade", radUpgrade);
        shUpgrade = IConfigurableMachine.grab(obj, "I:shieldUpgrade", shUpgrade);
        cooldownModif = IConfigurableMachine.grab(obj, "D:cooldownModifier", cooldownModif);
        healthRegenModif = IConfigurableMachine.grab(obj, "D:healthRegenModifier", healthRegenModif);
    }

    static void writeConfigStatic(JsonWriter writer) throws IOException {
        // CE TileEntityForceField.java:450-458
        writer.name("L:powerCap").value(maxPower);
        writer.name("I:baseConsumption").value(baseCon);
        writer.name("I:radiusConsumption").value(radCon);
        writer.name("I:shieldConsumption").value(shCon);
        writer.name("I:baseRadius").value(baseRadius);
        writer.name("I:radiusUpgrade").value(radUpgrade);
        writer.name("I:shieldUpgrade").value(shUpgrade);
        writer.name("D:cooldownModifier").value(cooldownModif);
        writer.name("D:healthRegenModifier").value(healthRegenModif);
    }

    public static final class ConfigDummy implements IConfigurableMachine {
        @Override
        public String getConfigName() {
            return "forcefield";
        }

        @Override
        public void readIfPresent(JsonObject obj) {
            readConfig(obj);
        }

        @Override
        public void writeConfig(JsonWriter writer) throws IOException {
            writeConfigStatic(writer);
        }
    }
}
