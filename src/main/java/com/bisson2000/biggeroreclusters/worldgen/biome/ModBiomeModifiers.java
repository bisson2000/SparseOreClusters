package com.bisson2000.biggeroreclusters.worldgen.biome;

import com.bisson2000.biggeroreclusters.BiggerOreClusters;
import com.bisson2000.biggeroreclusters.config.BiggerOreClustersConfig;
import com.bisson2000.biggeroreclusters.worldgen.placement.CenterChunkPlacement;
import com.bisson2000.biggeroreclusters.worldgen.placement.ModPlacementModifiers;
import com.bisson2000.biggeroreclusters.worldgen.placement.SpreadFilter;
import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.placement.*;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.common.world.ModifiableBiomeInfo;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class ModBiomeModifiers {


    @Nullable
    private static Codec<BiomeModifierImpl> noneBiomeModCodec = null;

    public static void init(IEventBus modEventBus) {
        modEventBus.<RegisterEvent>addListener(event -> {
            event.register(ForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, registry -> {
                ResourceLocation resourceLocation = new ResourceLocation(BiggerOreClusters.MOD_ID, "none_biome_mod_codec");
                noneBiomeModCodec = Codec.unit(BiomeModifierImpl.INSTANCE);
                registry.register(resourceLocation, noneBiomeModCodec);
            });
        });
    }

    private static class BiomeModifierImpl implements BiomeModifier {
        private static final BiomeModifierImpl INSTANCE = new BiomeModifierImpl();

        @Override
        public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
            // The overworld generates its ore during GenerationStep.Decoration.UNDERGROUND_ORES
            // The nether generates its ores during GenerationStep.Decoration.UNDERGROUND_DECORATION
            // why
            final List<GenerationStep.Decoration> decorations = List.of(
                    GenerationStep.Decoration.UNDERGROUND_ORES,
                    GenerationStep.Decoration.UNDERGROUND_DECORATION
            );

            if (phase == Phase.MODIFY) {
                Biome targetBiome = biome.get();

                HashMap<Biome, HashSet<Block>> allowedBlocksInBiome = new HashMap<>();
                // Classify blocks per biomes:
                // We do this only once, when the world loads
                // This is horrible and slow.
                // Do not use targetBiome.getGenerationSettings().features()
                for (GenerationStep.Decoration decoration : decorations) {
                    for (Holder<PlacedFeature> placedFeatureHolder : builder.getGenerationSettings().getFeatures(decoration)) {
                        if (placedFeatureHolder.get().feature().get().config() instanceof OreConfiguration oreConfiguration) {
                            for (OreConfiguration.TargetBlockState targetBlockState : oreConfiguration.targetStates) {
                                Block block = targetBlockState.state.getBlock();
                                if (BiggerOreClustersConfig.isTargeted(block)) {
                                    HashSet<Block> set = allowedBlocksInBiome.getOrDefault(targetBiome, new HashSet<>());
                                    set.add(block);
                                    allowedBlocksInBiome.put(targetBiome, set);
                                }
                            }
                        }
                    }
                }

                // For debugging: event.getRegistryAccess().registryOrThrow(Registries.BIOME).getKey(allowedBlocksInBiome.keySet().stream().toList().get(7))
                // Complete operation
                BiggerOreClustersConfig.addTargetedBlocksInBiome(allowedBlocksInBiome);

                // Iterate over decorations where ores are placed.
                for (GenerationStep.Decoration decoration : decorations) {
                    modifyPhase(builder, decoration);
                }
            }
        }

        private void modifyPhase(ModifiableBiomeInfo.BiomeInfo.Builder builder, GenerationStep.Decoration decoration) {
            List<Holder<PlacedFeature>> features = builder.getGenerationSettings().getFeatures(decoration);

            final List<Holder<PlacedFeature>> replacedFeatures = features.stream()
                    .filter(BiomeModifierImpl::isPlacedFeatureMatch)
                    .collect(Collectors.toCollection(ArrayList::new));

            for (Holder<PlacedFeature> featureHolder : replacedFeatures) {
                replaceFeature(builder, decoration, featureHolder);
            }
        }

        private static boolean isPlacedFeatureMatch(Holder<PlacedFeature> featureHolder) {
            FeatureConfiguration config = featureHolder.value().feature().value().config();
            if (config instanceof OreConfiguration oreConfiguration) {
                boolean match = !oreConfiguration.targetStates.isEmpty();
                for (OreConfiguration.TargetBlockState targetState : oreConfiguration.targetStates) {
                    match = match && BiggerOreClustersConfig.isTargeted(targetState.state.getBlock());
                }
                return match;
            }
            return false;
        }

        private void replaceFeature(ModifiableBiomeInfo.BiomeInfo.Builder builder,  GenerationStep.Decoration decoration, Holder<PlacedFeature> replacedFeature) {
            final int ORES_PER_VEIN = BiggerOreClustersConfig.ORES_PER_VEIN.get();
            final int VEINS_PER_CHUNK = BiggerOreClustersConfig.VEINS_PER_CHUNK.get();

            if (replacedFeature == null) {
                return;
            }

            if (!(replacedFeature.value().feature().value().config() instanceof OreConfiguration oreConfiguration)) {
                return;
            }

            // Remove modifiers, including the one we will be placing
            Set<PlacementModifierType<?>> replacedPlacements = new HashSet<>(Arrays.asList(
                    PlacementModifierType.COUNT, // Number of veins
                    PlacementModifierType.IN_SQUARE,
                    ModPlacementModifiers.CENTER_CHUNK_PLACEMENT.get(),
                    ModPlacementModifiers.SPREAD_FILTER.get()
            ));

            // Remove placements
            List<PlacementModifier> newPlacementModifier = new ArrayList<>(replacedFeature.value().placement());
            for (int j =  newPlacementModifier.size() - 1; j >= 0; --j) {
                if (replacedPlacements.contains(newPlacementModifier.get(j).type())) {
                    newPlacementModifier.remove(j);
                }
            }
            // Place at the center
            newPlacementModifier.add(CenterChunkPlacement.center());

            // Make it rare
            newPlacementModifier.add(new SpreadFilter(BiggerOreClustersConfig.ODDS_OF_ORES_IN_CHUNK.get()));

            // Get the new placement modifier, with a vein of NUMBER_OF_VEINS
            newPlacementModifier.add(CountPlacement.of(VEINS_PER_CHUNK));


            // make the veins huge
            ConfiguredFeature<?, ?> newConfiguration = null;
            newConfiguration = new ConfiguredFeature<>(Feature.ORE, new OreConfiguration(oreConfiguration.targetStates, ORES_PER_VEIN, oreConfiguration.discardChanceOnAirExposure));
            // TODO: custom ore configuration

            // apply
            PlacedFeature newPlacedFeature = new PlacedFeature(Holder.direct(newConfiguration), newPlacementModifier);
            Holder<PlacedFeature> placedFeatureHolder = Holder.direct(newPlacedFeature);
            builder.getGenerationSettings().getFeatures(decoration).removeIf(s -> s.is(replacedFeature.unwrap().left().get()));
            builder.getGenerationSettings().addFeature(decoration, placedFeatureHolder);
        }

        @Override
        public Codec<? extends BiomeModifier> codec() {
            if (noneBiomeModCodec != null) {
                return noneBiomeModCodec;
            } else {
                return Codec.unit(INSTANCE);
            }
        }
    }
}
