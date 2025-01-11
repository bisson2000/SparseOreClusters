package com.bisson2000.largepatchgenerator.worldgen;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

import java.util.stream.Stream;

public class CenterChunkPlacement extends PlacementModifier {
    private static final CenterChunkPlacement INSTANCE = new CenterChunkPlacement();
    public static final MapCodec<CenterChunkPlacement> CODEC = MapCodec.unit(() -> INSTANCE);

    public static CenterChunkPlacement center() {
        return INSTANCE;
    }

    @Override
    public Stream<BlockPos> getPositions(PlacementContext context, RandomSource random, BlockPos pos) {
        // Calculate the center of the chunk /fill ~ ~ ~ ~16 ~-100 ~16 air replace minecraft:stone
        int centerX = SectionPos.blockToSectionCoord(pos.getX());
        int centerZ = SectionPos.blockToSectionCoord(pos.getZ());

        centerX = SectionPos.sectionToBlockCoord(centerX) + 8; // Align to chunk boundary, add 8 for center
        centerZ = SectionPos.sectionToBlockCoord(centerZ) + 8;
        return Stream.of(new BlockPos(centerX, pos.getY(), centerZ));
    }

    @Override
    public PlacementModifierType<?> type() {
        return ModPlacementModifiers.CENTER_CHUNK_PLACEMENT.get();
    }
}
