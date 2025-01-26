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

            if (phase == Phase.AFTER_EVERYTHING) {
                Biome targetBiome = biome.get();

                HashMap<Biome, HashSet<Holder<PlacedFeature>>> allowedFeaturesInBiome = new HashMap<>();

                // Classify
                for (GenerationStep.Decoration decoration : decorations) {
                    for (Holder<PlacedFeature> placedFeatureHolder : builder.getGenerationSettings().getFeatures(decoration)) {
                        boolean allowed = false;
                        for (PlacementModifier placementModifier : placedFeatureHolder.value().placement()) {
                            // This placed feature contains the placement we are filtering with, add it to the list
                            if (placementModifier instanceof CenterChunkPlacement) {
                                allowed = true;
                                break;
                            }
                        }

                        if (allowed) {
                            HashSet<Holder<PlacedFeature>> allowedFeatures = allowedFeaturesInBiome.getOrDefault(targetBiome, new HashSet<>());
                            allowedFeatures.add(placedFeatureHolder);
                            allowedFeaturesInBiome.put(targetBiome, allowedFeatures);
                        }
                    }
                }

                // Complete operation
                BiggerOreClustersConfig.addTargetedFeaturesInBiome(allowedFeaturesInBiome);
            }
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
