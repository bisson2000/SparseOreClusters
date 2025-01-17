package com.bisson2000.largepatchgenerator.worldgen;

import com.bisson2000.largepatchgenerator.LargePatchGenerator;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModPlacementModifiers {
    private static final DeferredRegister<PlacementModifierType<?>> PLACEMENT_MODIFIER_TYPES =
            DeferredRegister.create(Registries.PLACEMENT_MODIFIER_TYPE, LargePatchGenerator.MOD_ID);

    public static final RegistryObject<PlacementModifierType<CenterChunkPlacement>> CENTER_CHUNK_PLACEMENT =
            PLACEMENT_MODIFIER_TYPES.register("center_chunk_placement",  () -> () -> CenterChunkPlacement.CODEC);

    public static void register(IEventBus eventBus) {
        PLACEMENT_MODIFIER_TYPES.register(eventBus);
    }
}
