package com.bisson2000.largepatchgenerator.worldgen;

import com.bisson2000.largepatchgenerator.LargePatchGenerator;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModOreFeatures {

    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(
            BuiltInRegistries.FEATURE, LargePatchGenerator.MODID
    );

    public static final DeferredHolder<Feature<?>, Feature<OreConfiguration>> CENTER_ORE_FEATURE =
            FEATURES.register("center_ore_feature",  () -> new CenteredOreFeature(OreConfiguration.CODEC));

    public static void register(IEventBus eventBus) {
        FEATURES.register(eventBus);
    }
}
