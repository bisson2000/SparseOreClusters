package com.bisson2000.largepatchgenerator.worldgen.placement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementFilter;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import net.minecraft.world.level.storage.LevelData;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;

public class SpreadFilter extends PlacementFilter {
    private static final Codec<ChunkPos> CHUNK_POS_CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    Codec.INT.fieldOf("x").forGetter(c -> c.x),
                    Codec.INT.fieldOf("z").forGetter(c -> c.z)
            ).apply(instance, ChunkPos::new)
    );

    public static final Codec<Map<ChunkPos, ResourceLocation>> USED_CHUNKS_CODEC = Codec.unboundedMap(
            CHUNK_POS_CODEC, ResourceLocation.CODEC
    );

    public static final Codec<SpreadFilter> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    Codec.FLOAT.fieldOf("base_odd").forGetter(filter -> filter.baseOdd),
                    Codec.FLOAT.fieldOf("distance_multiplier").forGetter(filter -> filter.distanceMultiplier),
                    Codec.INT.fieldOf("min_distance").forGetter(filter -> filter.minDistance),
                    USED_CHUNKS_CODEC.fieldOf("used_chunks").forGetter(filter -> SpreadFilter.allowedChunks)
                    //CHUNK_POS_CODEC.fieldOf("tracked_chunks").forGetter(filter -> SpreadFilter.trackedChunks)
            ).apply(instance, SpreadFilter::new)
    );

    private final float baseOdd;
    private final float distanceMultiplier;
    private final int minDistance;

    // Static HashSet to track chunk positions
    public static final HashMap<ChunkPos, ResourceLocation> allowedChunks = new HashMap<>();

    public SpreadFilter(float baseOdd, float distanceMultiplier, int minDistance) {
        this.baseOdd = baseOdd;
        this.distanceMultiplier = distanceMultiplier;
        this.minDistance = minDistance;
    }

    public SpreadFilter(float baseOdd, float distanceMultiplier, int minDistance, Map<ChunkPos, ResourceLocation> trackedChunks) {
        this.baseOdd = baseOdd;
        this.distanceMultiplier = distanceMultiplier;
        this.minDistance = minDistance;
        trackedChunks.forEach((k, v) -> {
            if (!SpreadFilter.allowedChunks .containsKey(k)) {
                SpreadFilter.allowedChunks.put(k, v);
            }
        });
    }

    @Override
    protected boolean shouldPlace(PlacementContext context, @NotNull RandomSource randomSource, @NotNull BlockPos blockPos) {
        LevelData levelData = context.getLevel().getLevelData();
        BlockPos spawnPos = new BlockPos(levelData.getXSpawn(), 0, levelData.getZSpawn());

        // Calculate the chunk positions
        ChunkPos currentChunkPos = new ChunkPos(blockPos);
        Optional<ResourceKey<Block>> currentBlockResource = context.getLevel().getBlockState(blockPos).getBlockHolder().unwrapKey();

        //context.topFeature()

        // Deny placement if chunk placement was not allowed
        if (currentBlockResource.isEmpty() || (allowedChunks.containsKey(currentChunkPos) && allowedChunks.get(currentChunkPos) != currentBlockResource.get().location())) {
            return false;
        }

        // Compute odds for placement based on distance
        double distance = currentChunkPos.getMiddleBlockPosition(0).distSqr(spawnPos);
        double odds = Math.max(0, (distance * distanceMultiplier + baseOdd) - minDistance);
        boolean willPlace = randomSource.nextDouble() < odds;

        if (!allowedChunks.containsKey(currentChunkPos)) {
            allowedChunks.put(currentChunkPos, currentBlockResource.get().location());
        }

        return willPlace;
    }

    @Override
    public PlacementModifierType<?> type() {
        return ModPlacementModifiers.SPREAD_FILTER.get();
    }
}
