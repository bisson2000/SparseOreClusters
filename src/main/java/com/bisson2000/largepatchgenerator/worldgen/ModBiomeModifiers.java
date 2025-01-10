package com.bisson2000.largepatchgenerator.worldgen;

import com.mojang.logging.LogUtils;
import com.bisson2000.largepatchgenerator.LargePatchGenerator;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.BiomeModifiers;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import java.io.Console;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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
            event.register(NeoForgeRegistries.Keys.BIOME_MODIFIERS, registry -> {
                ResourceLocation resourceLocation = ResourceLocation.fromNamespaceAndPath(LargePatchGenerator.MODID, "impl");
                registry.register(resourceLocation, BiomeModifierImpl.INSTANCE);
            });
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

            if (phase != Phase.REMOVE) {
                return;
            }

            GenerationStep.Decoration decoration = GenerationStep.Decoration.UNDERGROUND_ORES;
            List<Holder<PlacedFeature>> features = builder.getGenerationSettings().getFeatures(decoration);

            // removing features
            for (int i = features.size() - 1; i >= 0; --i) {
                Holder<PlacedFeature> feature = features.get(i);
                builder.getGenerationSettings().getFeatures(decoration).removeIf(s -> s.is(feature));
                LogUtils.getLogger().warn("Removing feature %s from generation step %s in biome %s".formatted(feature.unwrapKey(), decoration.name().toLowerCase(), biome.getKey()));
            }
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
}
