package com.bisson2000.largepatchgenerator.tag;

import com.bisson2000.largepatchgenerator.LargePatchGenerator;
import com.bisson2000.largepatchgenerator.config.LargePatchGeneratorConfig;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.tags.ITagManager;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber(modid = LargePatchGenerator.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TagEventManager {

    @SubscribeEvent
    public static void onTagsUpdated(TagsUpdatedEvent event) {
        if (event.getUpdateCause() != TagsUpdatedEvent.UpdateCause.SERVER_DATA_LOAD) {
            return;
        }

        final Set<String> whitelist = new HashSet<>(LargePatchGeneratorConfig.ALLOW_LISTED_BLOCKS.get());
        final Set<String> blacklist = new HashSet<>(LargePatchGeneratorConfig.DENY_LISTED_BLOCKS.get());

        HashSet<Block> targetList = new HashSet<>();
        ITagManager<Block> tagManager = ForgeRegistries.BLOCKS.tags();

        // Search by #forge:ore tag
        if (LargePatchGeneratorConfig.AUTO_ORE_SEARCH.get() && tagManager != null) {
            tagManager.getTag(BlockTags.create(new ResourceLocation("forge", "ores"))).forEach(b -> {
                b.defaultBlockState().getBlockHolder().unwrapKey().ifPresent(k -> {
                    String name = k.location().toString();
                    if (!blacklist.contains(name)) {
                        targetList.add(b);
                    }
                });
            });
        }

        // Search through all blocks
        targetList.addAll(
                ForgeRegistries.BLOCKS.getEntries().stream().filter(entry -> {
                    ResourceKey<Block> resourceKey = entry.getKey();
                    String name = resourceKey.location().toString();
                    boolean match = whitelist.contains(name);
                    if (LargePatchGeneratorConfig.AUTO_ORE_SEARCH.get()) {
                        match = match || name.contains("_ore");
                    }
                    match = match && !blacklist.contains(name);

                    return match;
                }).map(Map.Entry::getValue).collect(Collectors.toCollection(HashSet::new))
        );

        // Complete operation
        LargePatchGeneratorConfig.SetTargetedBlocks(targetList);
    }

}
