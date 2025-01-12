package com.bisson2000.largepatchgenerator.event;

import com.bisson2000.largepatchgenerator.LargePatchGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BlockChargeSavedData extends SavedData {

    private static final String DATA_NAME = LargePatchGenerator.MODID + "_" + "block_charges";
    private final Map<BlockPos, Integer> blockCharges = new HashMap<>();
    private static final int DEFAULT_CHARGES = 2;

    // Create new instance of saved data
    public static BlockChargeSavedData create() {
        return new BlockChargeSavedData();
    }

    // Integrated computeIfAbsent to retrieve or load BlockChargeSavedData
    public static BlockChargeSavedData get(ServerLevel level) {
        // Use computeIfAbsent to retrieve or load BlockChargeSavedData
        return level.getDataStorage().computeIfAbsent(
                new Factory<>(BlockChargeSavedData::new, BlockChargeSavedData::load),
                DATA_NAME
        );
    }

    // Load existing instance of saved data
    public static BlockChargeSavedData load(CompoundTag tag, HolderLookup.Provider lookupProvider) {
        BlockChargeSavedData data = BlockChargeSavedData.create();
        // Load block charge data
        ListTag chargeList = tag.getList("BlockCharges", CompoundTag.TAG_COMPOUND);
        data.blockCharges.clear();
        for (int i = 0; i < chargeList.size(); i++) {
            CompoundTag chargeTag = chargeList.getCompound(i);
            BlockPos pos = new BlockPos(
                    chargeTag.getInt("x"),
                    chargeTag.getInt("y"),
                    chargeTag.getInt("z")
            );
            int charges = chargeTag.getInt("charges");
            data.blockCharges.put(pos, charges);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        // Write block charge data to the tag
        ListTag chargeList = new ListTag();
        blockCharges.forEach((pos, charges) -> {
            CompoundTag chargeTag = new CompoundTag();
            chargeTag.putInt("x", pos.getX());
            chargeTag.putInt("y", pos.getY());
            chargeTag.putInt("z", pos.getZ());
            chargeTag.putInt("charges", charges);
            chargeList.add(chargeTag);
        });
        tag.put("BlockCharges", chargeList);
        return tag;
    }

    // Modify data
    public int reduceCharges(ServerLevel level, Player player, BlockPos pos) {
        int charges = blockCharges.getOrDefault(pos, DEFAULT_CHARGES) - 1;

        if (charges <= 0) {
            blockCharges.remove(pos);
            //level.removeBlock(pos, false); // Remove the block without dropping items
        } else {
            blockCharges.put(pos, charges);
            dropBlockItems(level, player, pos);
        }

        this.setDirty(); // Mark as dirty when data is modified
        return charges;
    }

    // Drop the items of the block
    private static void dropBlockItems(ServerLevel level, Player player, BlockPos pos) {
        if (player.isCreative()) {
            return;
        }

        BlockState blockState = level.getBlockState(pos);
        Block block = blockState.getBlock();
        BlockEntity blockentity = blockState.hasBlockEntity() ? level.getBlockEntity(pos) : null;

        // previously player.hasCorrectToolForDrops(blockstate)
        boolean canHarvestBlock = blockState.canHarvestBlock(level, pos, player); // taken from ServerPlayerGameMode. <
        if (!canHarvestBlock) {
            return;
        }

        Block.dropResources(blockState, level, pos, blockentity, null, player.getMainHandItem());

//        LootParams.Builder lootParamsBuilder = new LootParams.Builder(level);
//        lootParamsBuilder.withParameter(LootContextParams.TOOL, ItemStack.EMPTY);
//        lootParamsBuilder.withParameter(LootContextParams.ORIGIN, pos.getCenter());
//        //lootParamsBuilder.withLuck(player.getLuck()); // Luck
//
//        // Specify a loot context param set here if you want.
//        LootParams lootParams = lootParamsBuilder.create(LootContextParamSets.EMPTY);
//
//        // Get the loot table.
//        LootTable table = level.getServer().reloadableRegistries().getLootTable(block.getLootTable());
//
//        // Use this instead if you are rolling the loot table for container contents, e.g. loot chests.
//        // This method takes care of properly splitting the loot items across the container.
//        //List<ItemStack> containerList = table.fill(container, params, someSeed);
//
//        // Actually roll the loot table.
//        table.getRandomItems(lootParams).forEach(itemStack -> {
//            // Drop the item in the world
//            Block.popResource(level, pos, itemStack);
//        });
    }
}

