package com.bisson2000.sparseoreclusters.worldgen.placement;

import com.bisson2000.sparseoreclusters.SparseOreClusters;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModPlacementModifiers {
    private static final DeferredRegister<PlacementModifierType<?>> PLACEMENT_MODIFIER_TYPES =
            DeferredRegister.create(Registries.PLACEMENT_MODIFIER_TYPE, SparseOreClusters.MOD_ID);

    public static final RegistryObject<PlacementModifierType<CenterChunkPlacement>> CENTER_CHUNK_PLACEMENT =
            PLACEMENT_MODIFIER_TYPES.register("center_chunk_placement",  () -> () -> CenterChunkPlacement.CODEC);

    public static final RegistryObject<PlacementModifierType<SpreadFilter>> SPREAD_FILTER =
            PLACEMENT_MODIFIER_TYPES.register("spread_filter",  () -> () -> SpreadFilter.CODEC);

    public static void register(IEventBus eventBus) {
        PLACEMENT_MODIFIER_TYPES.register(eventBus);
    }
}
