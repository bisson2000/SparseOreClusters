package com.bisson2000.largepatchgenerator.config;

import com.bisson2000.largepatchgenerator.utils.ModRandomExtension;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.ForgeConfigSpec;
import org.jetbrains.annotations.NotNull;

import java.util.*;


public class LargePatchGeneratorConfig {

    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    // Configs
    public static final ForgeConfigSpec.ConfigValue<Integer> VARIETY_PER_CHUNK;
    public static final ForgeConfigSpec.ConfigValue<Integer> VEINS_PER_CHUNK;
    public static final ForgeConfigSpec.ConfigValue<Integer> ORES_PER_VEIN;
    public static final ForgeConfigSpec.ConfigValue<Double> ODDS_OF_ORES_IN_CHUNK;
    public static final ForgeConfigSpec.ConfigValue<Boolean> AUTO_ORE_SEARCH;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> ALLOW_LISTED_BLOCKS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> DENY_LISTED_BLOCKS;
    private static final ForgeConfigSpec.ConfigValue<List<? extends List<?>>> WEIGHT_LIST;
    private static HashMap<Block, ResourceLocation> TARGETED_BLOCKS = new HashMap<>();
    private static HashMap<Biome, HashSet<Block>> TARGETED_BLOCKS_IN_BIOME = new HashMap<>();

    // default values
    private static final List<? extends List<?>> DEFAULT_WEIGHT_LIST = List.of(
            List.of("minecraft:diamond_ore", 0.7),
            List.of("minecraft:deepslate_diamond_ore", 0.7),
            List.of("minecraft:ancient_debris", 0.2)
    );

    // definitions
    static {
        BUILDER.push(" Configs for Large Patch Generator");

        AUTO_ORE_SEARCH = BUILDER.comment(" Search automatically for ores")
                .define("Auto ore search", true);

        VARIETY_PER_CHUNK = BUILDER.comment(" How many different ores can spawn in a chunk. Must be greater or equal than 0")
                .defineInRange("Variety per chunk", 2, 0, Integer.MAX_VALUE);

        VEINS_PER_CHUNK = BUILDER.comment(" How many veins can each ore have within a chunk. Must be greater or equal than 0")
                .defineInRange("Veins per chunk", 1, 0, Integer.MAX_VALUE);

        ORES_PER_VEIN = BUILDER.comment(" How many ores can a vein have. Must be greater or equal than 0")
                .defineInRange("Ores per vein", 64, 0, Integer.MAX_VALUE);

        ODDS_OF_ORES_IN_CHUNK = BUILDER.comment(" The odds of a chunk containing ores. Value should be between 0 and 1.\n" +
                        " 0 means there won't be ores at all, while 1 means there will be ores in every chunks")
                .defineInRange("Odds of ores in chunk", 0.2, 0, 2.0);

        ALLOW_LISTED_BLOCKS = BUILDER.comment(" Which blocks are allowed to have their generation modified. Write the block with modid:block_name")
                .defineListAllowEmpty("Allowed blocks", Arrays.asList("minecraft:iron_ore", "minecraft:deepslate_iron_ore"), entry -> entry instanceof String);

        DENY_LISTED_BLOCKS = BUILDER.comment(" Which blocks are NOT allowed to have their generation modified. Write the block with modid:block_name")
                .defineListAllowEmpty("Denied blocks", Arrays.asList("minecraft:oak_log", "minecraft:acacia_log"), entry -> entry instanceof String);

        WEIGHT_LIST = BUILDER.comment(" A list of weights each ore has. The higher the weight, the more likely it is for the ore to appear.\n" +
                        " Takes in pairs of values (modid:block_name, weight). The weight should be above 0. By default, all ores have a weight of 1.\n " +
                        "Make sure to write the weight as a floation point number. E.g.: writing \"1\n will not work, but \"1.0\" will.")
                .defineList("Weight list", DEFAULT_WEIGHT_LIST, obj -> {
                    if (!(obj instanceof List<?> list) || list.size() != 2) return false;
                    if (!(list.get(1) instanceof Double weight)) return false;
                    return list.get(0) instanceof String && weight > 0.0;
                });

        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    public static void setTargetedBlocks(HashMap<Block, ResourceLocation> set) {
        TARGETED_BLOCKS = set;
    }

    public static void setTargetedBlocksInBiome(HashMap<Biome, HashSet<Block>> set) {
        TARGETED_BLOCKS_IN_BIOME = set;
    }

    public static void addTargetedBlocksInBiome(HashMap<Biome, HashSet<Block>> set) {
        TARGETED_BLOCKS_IN_BIOME.putAll(set);
    }

    public static boolean isTargeted(Block block) {
        return TARGETED_BLOCKS.containsKey(block);
    }

    public static boolean isTargeted(Biome biome) {
        return TARGETED_BLOCKS_IN_BIOME.containsKey(biome);
    }

    public static List<Block> getKRandomTargetedBlocks(@NotNull RandomSource randomSource, int k, Biome biome) {
        if (!TARGETED_BLOCKS_IN_BIOME.containsKey(biome)) return new ArrayList<>();

        final Double DEFAULT_WEIGHT = 1.0;
        final HashMap<String, Double> configuredWeights = getWeightList();
        List<Block> blockList = new ArrayList<>(TARGETED_BLOCKS_IN_BIOME.get(biome));
        k = Math.max(0, Math.min(k, blockList.size()));

        // Set up weight list
        ArrayList<Double> weightList = new ArrayList<>(blockList.size());
        for (Block block : blockList) {
            String searchedLocation = TARGETED_BLOCKS.get(block).toString();
            weightList.add(configuredWeights.getOrDefault(searchedLocation, DEFAULT_WEIGHT));
        }

        // shuffle
        blockList = ModRandomExtension.weightedShuffle(randomSource, blockList, weightList);
        return blockList.subList(0, k);
    }

    private static HashMap<String, Double> getWeightList() {
        HashMap<String, Double> res = new HashMap<>();
        WEIGHT_LIST.get().stream()
                .filter(obj -> obj != null && ((List<?>) obj).size() == 2) // Validate structure
                .forEach(obj -> {
                    List<?> list = (List<?>) obj;
                    res.put((String) list.get(0), (Double) list.get(1)); // Map
                });
        return res;
    }

}
