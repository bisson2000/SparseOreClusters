package com.bisson2000.largepatchgenerator.config;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class LargePatchGeneratorConfig {

    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.ConfigValue<Boolean> AUTO_ORE_SEARCH;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> ALLOW_LISTED_BLOCKS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> DENY_LISTED_BLOCKS;
    private static HashSet<Block> TARGETED_BLOCKS = new HashSet<>();

    static {
        BUILDER.push("Configs for Large Patch Generator");

        AUTO_ORE_SEARCH = BUILDER.comment("Search automatically for ores")
                .define("Auto ore search", true);

        ALLOW_LISTED_BLOCKS = BUILDER.comment("Which blocks are allowed to have their generation modified. Write the block with modid:block_name")
                .defineListAllowEmpty("Allowed blocks", Arrays.asList("minecraft:iron_ore", "minecraft:deepslate_iron_ore"), entry -> entry instanceof String);

        DENY_LISTED_BLOCKS = BUILDER.comment("Which blocks are NOT allowed to have their generation modified. Write the block with modid:block_name")
                .defineListAllowEmpty("Denied blocks", Arrays.asList("minecraft:oak_log", "minecraft:acacia_log"), entry -> entry instanceof String);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    public static void SetTargetedBlocks(HashSet<Block> set) {
        TARGETED_BLOCKS = set;
    }

    public static boolean isTargeted(Block block) {
        return TARGETED_BLOCKS.contains(block);
    }

}
