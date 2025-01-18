package com.bisson2000.largepatchgenerator.worldgen.biome;

import com.bisson2000.largepatchgenerator.LargePatchGenerator;
import com.bisson2000.largepatchgenerator.config.LargePatchGeneratorConfig;
import com.bisson2000.largepatchgenerator.worldgen.placement.CenterChunkPlacement;
import com.bisson2000.largepatchgenerator.worldgen.placement.ModPlacementModifiers;
import com.bisson2000.largepatchgenerator.worldgen.placement.SpreadFilter;
import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.data.worldgen.features.OreFeatures;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
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
            // Never called
            //event.register(NeoForgeRegistries.Keys.BIOME_MODIFIERS, registry -> {
            //    ResourceLocation resourceLocation = ResourceLocation.fromNamespaceAndPath(LargePatchGenerator.MODID, "impl");
            //    registry.register(resourceLocation, BiomeModifierImpl.INSTANCE);
            //});
        });
    }

    private static class BiomeModifierImpl implements BiomeModifier {
        // cry about it
        private static final BiomeModifierImpl INSTANCE = new BiomeModifierImpl();

        @Override
        public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
            //List<Pair<Predicate<BiomeContext>, BiConsumer<BiomeContext, BiomeProperties.Mutable>>> list = switch (phase) {
            //    case ADD -> ADDITIONS;
            //    case REMOVE -> REMOVALS;
            //    case MODIFY -> REPLACEMENTS;
            //    case AFTER_EVERYTHING -> POST_PROCESSING;
            //    default -> null;
            //};

            //if (list == null) return;
            //BiomeContext biomeContext = wrapSelectionContext(biome.unwrapKey(), builder);
            //BiomeProperties.Mutable mutableBiome = new MutableBiomeWrapped(builder);

            //for (var pair : list) {
            //    if (pair.getLeft().test(biomeContext)) {
            //        pair.getRight().accept(biomeContext, mutableBiome);
            //    }
            //}
            GenerationStep.Decoration decoration = GenerationStep.Decoration.UNDERGROUND_ORES;

            switch (phase) {
                case MODIFY:
                    modifyPhase(builder, decoration);
                    break;
            }

            //List<PlacementModifier> l = features.get(30).value().placement();

            //features.stream().findFirst(placedFeatureHolder -> placedFeatureHolder.getKey().location().getPath() == );
            //features.stream().findFirst()

            //Holder<PlacedFeature> replacedFeature = features.stream().filter(featureHolder -> featureHolder.is(new ResourceLocation("minecraft", "ore_iron_upper"))).findFirst().orElse(null)
        }

//        private void removePhase(ModifiableBiomeInfo.BiomeInfo.Builder builder, GenerationStep.Decoration decoration) {
//            final int KEPT_ORES_PER_CHUNK = 2;
//
//            List<Holder<PlacedFeature>> features = builder.getGenerationSettings().getFeatures(decoration);
//
//            List<Integer> matches = new ArrayList<>();
//
//            // find matching features indexes
//            for (int i = features.size() - 1; i >= 0; --i) {
//                if (isPlacedFeatureMatch(features.get(i))) {
//                    matches.add(i); // sorted in descending order
//                }
//            }
//
//            // Nothing to remove
//            if (matches.size() < KEPT_ORES_PER_CHUNK) {
//                return;
//            }
//
//            // remove random indexes
//            Random randomSource = new Random();
//            for (int i = 0; i < KEPT_ORES_PER_CHUNK; ++i) {
//                int randomIndex = randomSource.nextInt(matches.size());
//                matches.remove(randomIndex);
//            }
//
//            // Remove features
//            // Important for matches to be sorted in descending order
//            for (int matchIndex : matches) {
//                features.remove(matchIndex);
//            }
//        }

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
            final int VEIN_SIZE = 64;
            final int NUMBER_OF_VEINS = 1;

            if (replacedFeature == null) {
                return;
            }

            if (!(replacedFeature.value().feature().value().config() instanceof OreConfiguration oreConfiguration)) {
                return;
            }

            //for(OreConfiguration.TargetBlockState targetBlockState : oreConfiguration.targetStates) {
            //    var blockHolder = targetBlockState.state.getBlockHolder();
            //    if (!(blockHolder instanceof Holder.Reference<Block> ref)) {
            //        return false;
            //    }
            //}

            //replacedFeature.unwrapKey().map(ResourceKey::location);

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
            newPlacementModifier.add(new SpreadFilter(0.5f));

            // Get the new placement modifier, with a vein of NUMBER_OF_VEINS
            newPlacementModifier.add(CountPlacement.of(NUMBER_OF_VEINS));


            // make the veins huge
            ConfiguredFeature<?, ?> newConfiguration = null;
            // replacedFeature.value().feature().value().feature() == Feature.ORE
            newConfiguration = new ConfiguredFeature<>(Feature.ORE, new OreConfiguration(oreConfiguration.targetStates, VEIN_SIZE, oreConfiguration.discardChanceOnAirExposure));
            // TODO: custom ore configuration
            //newConfiguration = new ConfiguredFeature<>(ModOreFeatures.CENTER_ORE_FEATURE.get(), new OreConfiguration(oreConfiguration.targetStates, 4, oreConfiguration.discardChanceOnAirExposure));

            // apply
            PlacedFeature newPlacedFeature = new PlacedFeature(Holder.direct(newConfiguration), newPlacementModifier);
            Holder<PlacedFeature> placedFeatureHolder = Holder.direct(newPlacedFeature);
            builder.getGenerationSettings().getFeatures(decoration).removeIf(s -> s.is(replacedFeature.unwrap().left().get()));
            builder.getGenerationSettings().addFeature(decoration, placedFeatureHolder);

            //features.get(30).value().placement().toArray()



            //MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            //if (server != null) {
            //    Optional<? extends Registry<PlacedFeature>> registry = server.registryAccess().registry(Registries.PLACED_FEATURE);
            //    if (registry.isPresent()) {
            //        Optional<Holder.Reference<PlacedFeature>> holder = registry.get().getHolder(feature);
            //        if (holder.isPresent()) {
            //            addFeature(decoration, holder.get());
            //        } else {
            //            throw new IllegalArgumentException("Unknown feature: " + feature);
            //        }
            //    }
            //}

            //MutableGenerationSettingsBuilderWrapped mutable = new MutableGenerationSettingsBuilderWrapped(builder.getGenerationSettings());


            //for (int i = features.size() - 1; i >= 0; --i) {
            //    Holder<PlacedFeature> feature = features.get(i);
            //    for (int j = 0; j < feature.value().placement().size(); ++j) {
            //        if (feature.value().placement().get(j).type() == PlacementModifierType.COUNT) { // Number of veins
            //            //feature.value().placement() feature.value().placement.set(j, CountPlacement.of(1000))
            //            //builder.getGenerationSettings().addFeature()
            //        }
            //    }
            //    builder.getGenerationSettings().getFeatures(decoration).removeIf(s -> s.is(feature));
            //    LogUtils.getLogger().warn("Removing feature %s from generation step %s in biome %s".formatted(feature.unwrapKey(), decoration.name().toLowerCase(), biome.getKey()));
            //}


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
