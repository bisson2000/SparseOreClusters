package com.bisson2000.sparseoreclusters.config;

import com.bisson2000.sparseoreclusters.utils.ModRandomExtension;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraftforge.common.ForgeConfigSpec;
import org.jetbrains.annotations.NotNull;

import java.util.*;


public class SparseOreClustersConfig {

    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    // Configs
    public static final ForgeConfigSpec.ConfigValue<Integer> VARIETY_PER_CHUNK;
    public static final ForgeConfigSpec.ConfigValue<Double> ODDS_OF_FEATURES_IN_CHUNK;
    private static final ForgeConfigSpec.ConfigValue<List<? extends List<?>>> WEIGHT_LIST;
    public static HashMap<Biome, HashSet<Holder<PlacedFeature>>> TARGETED_FEATURES_IN_BIOME = new HashMap<>();

    // default values
    private static final List<? extends List<?>> DEFAULT_WEIGHT_LIST = List.of(
            List.of("minecraft:ore_diamond", 0.5),
            List.of("minecraft:ore_diamond_large", 0.5),
            List.of("minecraft:ore_diamond_buried", 0.5),
            List.of("minecraft:ore_debris_small", 0.2)
    );

    // definitions
    static {
        BUILDER.push("Configs for Sparse Ore Clusters");

        VARIETY_PER_CHUNK = BUILDER.comment(" How many different features can spawn in a chunk")
                .defineInRange("Variety per chunk", 1, 0, Integer.MAX_VALUE);

        ODDS_OF_FEATURES_IN_CHUNK = BUILDER.comment(" The odds of a chunk containing features. Value should be between 0 and 1.\n" +
                        " 0 means there won't be any features at all, while 1 means there will be a feature in every chunk")
                .defineInRange("Odds of features in chunk", 1.0, 0.0, 1.0);

        WEIGHT_LIST = BUILDER.comment(" A list of weights each feature has. The higher the weight, the more likely it is for the feature to appear.\n" +
                        " Takes in pairs of values (modid:feature, weight). The weight should be above 0. By default, all features have a weight of 1.\n" +
                        " Make sure to write the weight as a floating point number. E.g.: writing \"1\n will not work, but \"1.0\" will.")
                .defineList("Weight list", DEFAULT_WEIGHT_LIST, obj -> {
                    if (!(obj instanceof List<?> list) || list.size() != 2) return false;
                    if (!(list.get(1) instanceof Double weight)) return false;
                    return list.get(0) instanceof String && weight > 0.0;
                });

        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    public static void addTargetedFeaturesInBiome(HashMap<Biome, HashSet<Holder<PlacedFeature>>> set) {
        TARGETED_FEATURES_IN_BIOME.putAll(set);
    }

    public static List<Holder<PlacedFeature>> getKRandomTargets(@NotNull RandomSource randomSource, int k, Biome biome) {
        if (!TARGETED_FEATURES_IN_BIOME.containsKey(biome)) return new ArrayList<>();

        final Double DEFAULT_WEIGHT = 1.0;
        final HashMap<String, Double> configuredWeights = getWeightList();
        List<Holder<PlacedFeature>> placedFeatureList = new ArrayList<>(TARGETED_FEATURES_IN_BIOME.get(biome));

        // The set is not ordered by default. Without this sort, the generation would be different across generations with the same seed.
        placedFeatureList.sort(Comparator.comparing((o1) -> {
            if (o1.unwrapKey().isEmpty()) {
                return "";
            }
            return o1.unwrapKey().get().location().toString();
        }));
        k = Math.max(0, Math.min(k, placedFeatureList.size()));

        // Set up weight list
        // The weight list is initialized
        ArrayList<Double> weightList = new ArrayList<>(placedFeatureList.size());
        for (Holder<PlacedFeature> placedFeature : placedFeatureList) {
            double newWeight = DEFAULT_WEIGHT;
            if (placedFeature.unwrapKey().isPresent()) {
                String searchedLocation =  placedFeature.unwrapKey().get().location().toString();
                newWeight = configuredWeights.getOrDefault(searchedLocation, DEFAULT_WEIGHT);
            }
            weightList.add(-1.0 * Math.pow(randomSource.nextDouble(), 1.0 / newWeight));
        }

        // shuffle
        placedFeatureList = ModRandomExtension.weightedShuffle(placedFeatureList, weightList);
        return placedFeatureList.subList(0, k);
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
