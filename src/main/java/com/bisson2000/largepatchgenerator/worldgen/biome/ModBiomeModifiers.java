package com.bisson2000.largepatchgenerator.worldgen.biome;

import com.bisson2000.largepatchgenerator.LargePatchGenerator;
import com.bisson2000.largepatchgenerator.config.LargePatchGeneratorConfig;
import com.bisson2000.largepatchgenerator.worldgen.placement.CenterChunkPlacement;
import com.bisson2000.largepatchgenerator.worldgen.placement.ModPlacementModifiers;
import com.bisson2000.largepatchgenerator.worldgen.placement.SpreadFilter;
import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
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
                ResourceLocation resourceLocation = new ResourceLocation(LargePatchGenerator.MOD_ID, "none_biome_mod_codec");
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
            List<GenerationStep.Decoration> decorations = List.of(
                    GenerationStep.Decoration.UNDERGROUND_ORES,
                    GenerationStep.Decoration.UNDERGROUND_DECORATION
            );

            if (phase == Phase.MODIFY) {
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
            if (featureHolder.value().feature().value().config() instanceof OreConfiguration oreConfiguration) {
                boolean match = !oreConfiguration.targetStates.isEmpty();
                for (OreConfiguration.TargetBlockState targetState : oreConfiguration.targetStates) {
                    match = match && LargePatchGeneratorConfig.isTargeted(targetState.state.getBlock());
                }
                return match;
            }
            return false;
        }

        private void replaceFeature(ModifiableBiomeInfo.BiomeInfo.Builder builder,  GenerationStep.Decoration decoration, Holder<PlacedFeature> replacedFeature) {
            final int ORES_PER_VEIN = LargePatchGeneratorConfig.ORES_PER_VEIN.get();
            final int VEINS_PER_CHUNK = LargePatchGeneratorConfig.VEINS_PER_CHUNK.get();

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
            newPlacementModifier.add(new SpreadFilter(LargePatchGeneratorConfig.ODDS_OF_ORES_IN_CHUNK.get()));

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
