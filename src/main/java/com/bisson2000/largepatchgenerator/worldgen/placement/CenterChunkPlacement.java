package com.bisson2000.largepatchgenerator.worldgen.placement;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Stream;

public class CenterChunkPlacement extends PlacementModifier {
    private static final CenterChunkPlacement INSTANCE = new CenterChunkPlacement();
    public static final Codec<CenterChunkPlacement> CODEC = Codec.unit(() -> INSTANCE);

    public static CenterChunkPlacement center() {
        return INSTANCE;
    }

    @Override
    public @NotNull Stream<BlockPos> getPositions(@NotNull PlacementContext context, @NotNull RandomSource random, BlockPos pos) {
        // Calculate the center of the chunk
        // Debug command: /fill ~ ~ ~ ~16 ~-100 ~16 air replace minecraft:stone
        int centerX = SectionPos.blockToSectionCoord(pos.getX());
        int centerZ = SectionPos.blockToSectionCoord(pos.getZ());

        centerX = SectionPos.sectionToBlockCoord(centerX) + 8; // Align to chunk boundary, add 8 for center
        centerZ = SectionPos.sectionToBlockCoord(centerZ) + 8;
        return Stream.of(new BlockPos(centerX, pos.getY(), centerZ));
    }

    @Override
    public @NotNull PlacementModifierType<?> type() {
        return ModPlacementModifiers.CENTER_CHUNK_PLACEMENT.get();
    }
}
