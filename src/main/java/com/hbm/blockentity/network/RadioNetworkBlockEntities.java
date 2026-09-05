package com.hbm.blockentity.network;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.network.RadioNetworkBlocks;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Supplier;

/** CE TileMappings {@code tileentity_rtty_*}. */
public final class RadioNetworkBlockEntities {

    public static Supplier<BlockEntityType<RadioTorchSenderBlockEntity>> SENDER;
    public static Supplier<BlockEntityType<RadioTorchReceiverBlockEntity>> RECEIVER;
    public static Supplier<BlockEntityType<RadioTorchCounterBlockEntity>> COUNTER;
    public static Supplier<BlockEntityType<RadioTorchLogicBlockEntity>> LOGIC;
    public static Supplier<BlockEntityType<RadioTorchReaderBlockEntity>> READER;
    public static Supplier<BlockEntityType<RadioTorchControllerBlockEntity>> CONTROLLER;

    private RadioNetworkBlockEntities() {
    }

    public static void registerAll() {
        SENDER = ModBlocks.BLOCK_ENTITY_TYPES.register("tileentity_rtty_sender", () ->
                BlockEntityType.Builder.of(
                        (pos, state) -> new RadioTorchSenderBlockEntity(SENDER.get(), pos, state),
                        RadioNetworkBlocks.RADIO_TORCH_SENDER.get()).build(null));
        RECEIVER = ModBlocks.BLOCK_ENTITY_TYPES.register("tileentity_rtty_rec", () ->
                BlockEntityType.Builder.of(
                        (pos, state) -> new RadioTorchReceiverBlockEntity(RECEIVER.get(), pos, state),
                        RadioNetworkBlocks.RADIO_TORCH_RECEIVER.get()).build(null));
        COUNTER = ModBlocks.BLOCK_ENTITY_TYPES.register("tileentity_rtty_counter", () ->
                BlockEntityType.Builder.of(
                        (pos, state) -> new RadioTorchCounterBlockEntity(COUNTER.get(), pos, state),
                        RadioNetworkBlocks.RADIO_TORCH_COUNTER.get()).build(null));
        LOGIC = ModBlocks.BLOCK_ENTITY_TYPES.register("tileentity_rtty_logic", () ->
                BlockEntityType.Builder.of(
                        (pos, state) -> new RadioTorchLogicBlockEntity(LOGIC.get(), pos, state),
                        RadioNetworkBlocks.RADIO_TORCH_LOGIC.get()).build(null));
        READER = ModBlocks.BLOCK_ENTITY_TYPES.register("tileentity_rtty_reader", () ->
                BlockEntityType.Builder.of(
                        (pos, state) -> new RadioTorchReaderBlockEntity(READER.get(), pos, state),
                        RadioNetworkBlocks.RADIO_TORCH_READER.get()).build(null));
        CONTROLLER = ModBlocks.BLOCK_ENTITY_TYPES.register("tileentity_rtty_controller", () ->
                BlockEntityType.Builder.of(
                        (pos, state) -> new RadioTorchControllerBlockEntity(CONTROLLER.get(), pos, state),
                        RadioNetworkBlocks.RADIO_TORCH_CONTROLLER.get()).build(null));
    }
}
