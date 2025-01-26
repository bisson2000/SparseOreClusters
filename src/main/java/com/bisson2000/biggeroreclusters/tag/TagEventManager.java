package com.bisson2000.biggeroreclusters.tag;

import com.bisson2000.biggeroreclusters.BiggerOreClusters;
import com.bisson2000.biggeroreclusters.config.BiggerOreClustersConfig;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.tags.ITagManager;

import java.util.*;

@Mod.EventBusSubscriber(modid = BiggerOreClusters.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TagEventManager {

    @SubscribeEvent
    public static void onTagsUpdated(TagsUpdatedEvent event) {
        if (event.getUpdateCause() != TagsUpdatedEvent.UpdateCause.SERVER_DATA_LOAD) {
            return;
        }

        final Set<String> whitelist = new HashSet<>(BiggerOreClustersConfig.ALLOW_LISTED_BLOCKS.get());
        final Set<String> blacklist = new HashSet<>(BiggerOreClustersConfig.DENY_LISTED_BLOCKS.get());

        HashMap<Block, ResourceLocation> targetList = new HashMap<>();
        ITagManager<Block> tagManager = ForgeRegistries.BLOCKS.tags();

        // Search by #forge:ore tag
        if (BiggerOreClustersConfig.AUTO_ORE_SEARCH.get() && tagManager != null) {
            tagManager.getTag(BlockTags.create(new ResourceLocation("forge", "ores"))).forEach(b -> {
                b.defaultBlockState().getBlockHolder().unwrapKey().ifPresent(k -> {
                    String name = k.location().toString();
                    if (!blacklist.contains(name)) {
                        targetList.put(b, k.location());
                    }
                });
            });
        }

        // Search through all blocks
        ForgeRegistries.BLOCKS.getEntries().stream().filter(entry -> {
            ResourceKey<Block> resourceKey = entry.getKey();
            String name = resourceKey.location().toString();
            boolean match = whitelist.contains(name);
            if (BiggerOreClustersConfig.AUTO_ORE_SEARCH.get()) {
                match = match || name.contains("_ore");
            }
            match = match && !blacklist.contains(name);

            return match;
        }).forEach(entry -> {
            targetList.put(entry.getValue(), entry.getKey().location());
        });


        HashMap<Biome, HashSet<Block>> allowedBlocksInBiome = new HashMap<>();
        // Classify blocks per biomes:
        // We do this only once, when the world loads
        // This is horrible and slow.
        //Set<Map.Entry<ResourceKey<Biome>, Biome>> biomeEntries = event.getRegistryAccess().registryOrThrow(Registries.BIOME).entrySet();
        //for (Map.Entry<ResourceKey<Biome>, Biome> biomeEntry : biomeEntries) {
        //    Biome biome = biomeEntry.getValue();
        //    for (HolderSet<PlacedFeature> featureSet : biome.getGenerationSettings().features()) {
        //        for (Holder<PlacedFeature> placedFeatureHolder : featureSet) {
        //            if (placedFeatureHolder.get().feature().get().config() instanceof OreConfiguration oreConfiguration) {
        //                for (OreConfiguration.TargetBlockState targetBlockState : oreConfiguration.targetStates) {
        //                    Block block = targetBlockState.state.getBlock();
        //                    if (targetList.containsKey(block)) {
        //                        HashSet<Block> set = allowedBlocksInBiome.getOrDefault(biome, new HashSet<>());
        //                        set.add(block);
        //                        allowedBlocksInBiome.put(biome, set);
        //                    }
        //                }
        //            }
        //        }
        //    }
        //}

        // For debugging: event.getRegistryAccess().registryOrThrow(Registries.BIOME).getKey(allowedBlocksInBiome.keySet().stream().toList().get(7))
        // Complete operation
        // NOTE: THIS CONTAINS ONLY VANILLA BIOMES
        BiggerOreClustersConfig.setTargetedBlocksInBiome(allowedBlocksInBiome);

        // This contains all blocks
        BiggerOreClustersConfig.setTargetedBlocks(targetList);
    }

}
