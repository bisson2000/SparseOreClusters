package com.bisson2000.largepatchgenerator.worldgen;

import com.bisson2000.largepatchgenerator.LargePatchGenerator;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.placement.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.world.BiomeGenerationSettingsBuilder;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.BiomeModifiers;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ModBiomeModifiers {
    public static final ResourceKey<BiomeModifier> ADD_BISMUTH_ORE = registerKey("add_bismuth_ore");
    public static final ResourceKey<BiomeModifier> ADD_NETHER_BISMUTH_ORE = registerKey("add_nether_bismuth_ore");
    public static final ResourceKey<BiomeModifier> ADD_END_BISMUTH_ORE = registerKey("add_end_bismuth_ore");

    public static void bootstrap(BootstrapContext<BiomeModifier> context) {
        // CF -> PF -> BM
        var placedFeatures = context.lookup(Registries.PLACED_FEATURE);
        var biomes = context.lookup(Registries.BIOME);

        context.register(ADD_BISMUTH_ORE, new BiomeModifiers.AddFeaturesBiomeModifier(
                biomes.getOrThrow(BiomeTags.IS_OVERWORLD),
                HolderSet.direct(placedFeatures.getOrThrow(ModPlacedFeatures.BISMUTH_ORE_PLACED_KEY)),
                GenerationStep.Decoration.UNDERGROUND_ORES
        ));
        context.register(ADD_NETHER_BISMUTH_ORE, new BiomeModifiers.AddFeaturesBiomeModifier(
                biomes.getOrThrow(BiomeTags.IS_NETHER),
                HolderSet.direct(placedFeatures.getOrThrow(ModPlacedFeatures.NETHER_BISMUTH_ORE_PLACED_KEY)),
                GenerationStep.Decoration.UNDERGROUND_ORES
        ));
        context.register(ADD_END_BISMUTH_ORE, new BiomeModifiers.AddFeaturesBiomeModifier(
                biomes.getOrThrow(BiomeTags.IS_END),
                HolderSet.direct(placedFeatures.getOrThrow(ModPlacedFeatures.END_BISMUTH_ORE_PLACED_KEY)),
                GenerationStep.Decoration.UNDERGROUND_ORES
        ));

    }

    private static ResourceKey<BiomeModifier> registerKey(String name) {
        return ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS, ResourceLocation.fromNamespaceAndPath(LargePatchGenerator.MODID, name));
    }

    @Nullable
    private static MapCodec<BiomeModifierImpl> noneBiomeModCodec = null;

    public static void init(IEventBus modEventBus) {
        modEventBus.<RegisterEvent>addListener(event -> {
            event.register(NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, registry -> {
                ResourceLocation resourceLocation = ResourceLocation.fromNamespaceAndPath(LargePatchGenerator.MODID, "none_biome_mod_codec");
                noneBiomeModCodec = MapCodec.unit(BiomeModifierImpl.INSTANCE);
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

            if (phase != Phase.MODIFY) {
                return;
            }

            GenerationStep.Decoration decoration = GenerationStep.Decoration.UNDERGROUND_ORES;
            List<Holder<PlacedFeature>> features = builder.getGenerationSettings().getFeatures(decoration);

            //List<PlacementModifier> l = features.get(30).value().placement();

            //features.stream().findFirst(placedFeatureHolder -> placedFeatureHolder.getKey().location().getPath() == );
            //features.stream().findFirst()

            Holder<PlacedFeature> replacedFeature = features.stream().filter(featureHolder -> featureHolder.getKey().location().getPath().equals("bismuth_ore_placed")).findFirst().orElse(null);

            if (replacedFeature == null) {
                return;
            }

            Set<PlacementModifierType<?>> replacedPlacements = new HashSet<>(Arrays.asList(
                    PlacementModifierType.COUNT, // Number of veins
                    PlacementModifierType.FIXED_PLACEMENT,
                    PlacementModifierType.IN_SQUARE,
                    ModPlacementModifiers.CENTER_CHUNK_PLACEMENT.get()
            ));

            // Get the new placement modifier, with a vein of 1
            List<PlacementModifier> newPlacementModifier = new java.util.ArrayList<>(replacedFeature.value().placement());
            for (int j =  newPlacementModifier.size() - 1; j >= 0; --j) {
                if (replacedPlacements.contains(newPlacementModifier.get(j).type())) {
                    newPlacementModifier.remove(j);
                }
            }
            newPlacementModifier.add(CountPlacement.of(1));
            //newPlacementModifier.add(FixedPlacement.of(
            //        new BlockPos(0, 0, 0)
            //));
            //newPlacementModifier.add(InSquarePlacement.spread());
            newPlacementModifier.add(CenterChunkPlacement.center());

            // make the veins huge
            ConfiguredFeature<?, ?> newConfiguration = null;
            if (replacedFeature.value().feature().value().config() instanceof OreConfiguration oreConfiguration) {
                newConfiguration = new ConfiguredFeature<>(Feature.ORE, new OreConfiguration(oreConfiguration.targetStates, 4, oreConfiguration.discardChanceOnAirExposure));
            } else {
                return;
            }

            // apply
            PlacedFeature newPlacedFeature = new PlacedFeature(Holder.direct(newConfiguration), newPlacementModifier);
            Holder<PlacedFeature> placedFeatureHolder = Holder.direct(newPlacedFeature);
            builder.getGenerationSettings().getFeatures(decoration).removeIf(s -> s.is(replacedFeature));
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

            /// removing features
            //for (int i = features.size() - 1; i >= 0; --i) {
            //    Holder<PlacedFeature> feature = features.get(i);
            //    builder.getGenerationSettings().getFeatures(decoration).removeIf(s -> s.is(feature));
            //    LogUtils.getLogger().warn("Removing feature %s from generation step %s in biome %s".formatted(feature.unwrapKey(), decoration.name().toLowerCase(), biome.getKey()));
            //}
        }

        @Override
        public MapCodec<? extends BiomeModifier> codec() {
            if (noneBiomeModCodec != null) {
                return noneBiomeModCodec;
            } else {
                return MapCodec.unit(INSTANCE);
            }
        }
    }

    private static class GenerationSettingsBuilderWrapped {
        protected final BiomeGenerationSettingsBuilder generation;

        public GenerationSettingsBuilderWrapped(BiomeGenerationSettingsBuilder generation) {
            this.generation = generation;
        }

        public Iterable<Holder<ConfiguredWorldCarver<?>>> getCarvers(GenerationStep.Carving carving) {
            return generation.getCarvers(carving);
        }

        public Iterable<Holder<PlacedFeature>> getFeatures(GenerationStep.Decoration decoration) {
            return generation.getFeatures(decoration);
        }
    }

    private static class MutableGenerationSettingsBuilderWrapped extends GenerationSettingsBuilderWrapped {
        public MutableGenerationSettingsBuilderWrapped(BiomeGenerationSettingsBuilder generation) {
            super(generation);
        }

        public void addFeature(GenerationStep.Decoration decoration, Holder<PlacedFeature> feature) {
            generation.addFeature(decoration, feature);
        }

        public void addFeature(GenerationStep.Decoration decoration, ResourceKey<PlacedFeature> feature) {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                Optional<? extends Registry<PlacedFeature>> registry = server.registryAccess().registry(Registries.PLACED_FEATURE);
                if (registry.isPresent()) {
                    Optional<Holder.Reference<PlacedFeature>> holder = registry.get().getHolder(feature);
                    if (holder.isPresent()) {
                        addFeature(decoration, holder.get());
                    } else {
                        throw new IllegalArgumentException("Unknown feature: " + feature);
                    }
                }
            }
        }

        public void addCarver(GenerationStep.Carving carving, Holder<ConfiguredWorldCarver<?>> feature) {
            generation.addCarver(carving, feature);
        }

        public void addCarver(GenerationStep.Carving carving, ResourceKey<ConfiguredWorldCarver<?>> feature) {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                Optional<? extends Registry<ConfiguredWorldCarver<?>>> registry = server.registryAccess().registry(Registries.CONFIGURED_CARVER);
                if (registry.isPresent()) {
                    Optional<Holder.Reference<ConfiguredWorldCarver<?>>> holder = registry.get().getHolder(feature);
                    if (holder.isPresent()) {
                        addCarver(carving, holder.get());
                    } else {
                        throw new IllegalArgumentException("Unknown carver: " + feature);
                    }
                }
            }
        }

        public void removeFeature(GenerationStep.Decoration decoration, ResourceKey<PlacedFeature> feature) {
            generation.getFeatures(decoration).removeIf(supplier -> supplier.is(feature));
        }

        public void removeCarver(GenerationStep.Carving carving, ResourceKey<ConfiguredWorldCarver<?>> feature) {
            generation.getCarvers(carving).removeIf(supplier -> supplier.is(feature));
        }
    }
}
