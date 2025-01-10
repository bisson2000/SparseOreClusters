package com.bisson2000.largepatchgenerator.worldgen;

import com.bisson2000.largepatchgenerator.LargePatchGenerator;
import com.bisson2000.largepatchgenerator.blocks.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;
import net.neoforged.neoforge.common.Tags;

import java.util.List;

public class ModConfiguredFeatures {
    // CF -> PF -> BM
    public static final ResourceKey<ConfiguredFeature<?, ?>> OVERWORLD_BISMUTH_ORE_KEY = registerKey("bismuth_ore");
    public static final ResourceKey<ConfiguredFeature<?, ?>> NETHER_BISMUTH_ORE_KEY = registerKey("nether_bismuth_ore");
    public static final ResourceKey<ConfiguredFeature<?, ?>> END_BISMUTH_ORE_KEY = registerKey("end_bismuth_ore");

    public static void bootstrap(BootstrapContext<ConfiguredFeature<?, ?>> context) {

        RuleTest stoneReplaceables = new TagMatchTest(BlockTags.STONE_ORE_REPLACEABLES);
        RuleTest deepslateReplaceables = new TagMatchTest(BlockTags.DEEPSLATE_ORE_REPLACEABLES);
        RuleTest netherrackReplaceables = new TagMatchTest(Tags.Blocks.NETHERRACKS);
        RuleTest endReplaceables = new TagMatchTest(Tags.Blocks.END_STONES);

        List<OreConfiguration.TargetBlockState> overworldBismuthOres = List.of(
                OreConfiguration.target(stoneReplaceables, ModBlocks.BISMUTH_ORE.get().defaultBlockState()),
                OreConfiguration.target(deepslateReplaceables, ModBlocks.BISMUTH_ORE.get().defaultBlockState())
        );

        List<OreConfiguration.TargetBlockState> netherBismuthOres = List.of(
                OreConfiguration.target(netherrackReplaceables, ModBlocks.BISMUTH_ORE.get().defaultBlockState())
        );

        List<OreConfiguration.TargetBlockState> endBismuthOres = List.of(
                OreConfiguration.target(endReplaceables, ModBlocks.BISMUTH_ORE.get().defaultBlockState())
        );

        register(context, OVERWORLD_BISMUTH_ORE_KEY, Feature.ORE, new OreConfiguration(overworldBismuthOres, 9));
        register(context, NETHER_BISMUTH_ORE_KEY, Feature.ORE, new OreConfiguration(netherBismuthOres, 9));
        register(context, END_BISMUTH_ORE_KEY, Feature.ORE, new OreConfiguration(endBismuthOres, 9));
    }

    public static ResourceKey<ConfiguredFeature<?, ?>> registerKey(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath(LargePatchGenerator.MODID, name));
    }

    private static <FC extends FeatureConfiguration, F extends Feature<FC>> void register(BootstrapContext<ConfiguredFeature<?, ?>> context,
                                                                                          ResourceKey<ConfiguredFeature<?, ?>> key, F feature, FC configuration) {
        context.register(key, new ConfiguredFeature<>(feature, configuration));
    }
}
