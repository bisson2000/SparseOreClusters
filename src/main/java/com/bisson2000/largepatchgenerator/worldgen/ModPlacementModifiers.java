package com.bisson2000.largepatchgenerator.worldgen;

import com.bisson2000.largepatchgenerator.LargePatchGenerator;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModPlacementModifiers {
    public static final DeferredRegister<PlacementModifierType<?>> PLACEMENTS = DeferredRegister.create(
            BuiltInRegistries.PLACEMENT_MODIFIER_TYPE, LargePatchGenerator.MODID
    );

    public static final DeferredHolder<PlacementModifierType<?>, PlacementModifierType<CenterChunkPlacement>> CENTER_CHUNK_PLACEMENT =
            PLACEMENTS.register("center_chunk_placement",  () -> () -> CenterChunkPlacement.CODEC);

    public static void register(IEventBus eventBus) {
        PLACEMENTS.register(eventBus);
    }
}
