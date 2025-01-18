package com.bisson2000.largepatchgenerator.worldgen.placement;

import com.bisson2000.largepatchgenerator.config.LargePatchGeneratorConfig;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementFilter;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SpreadFilter extends PlacementFilter {

    public static final Codec<SpreadFilter> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    Codec.FLOAT.fieldOf("chanceOfSpawningPerChunk").forGetter(filter -> filter.chanceOfSpawningPerChunk)
            ).apply(instance, SpreadFilter::new)
    );

    private final float chanceOfSpawningPerChunk;

    // Static HashMap to track chunk positions
    public static final ConcurrentHashMap<ServerLevel, HashMap<ChunkPos, Tuple<Biome, HashSet<Block>>>> trackedChunks = new ConcurrentHashMap<>();

    public SpreadFilter(float chanceOfSpawningPerChunk) {
        this.chanceOfSpawningPerChunk = chanceOfSpawningPerChunk;
    }

    @Override
    protected boolean shouldPlace(@NotNull PlacementContext context, @NotNull RandomSource randomSource, @NotNull BlockPos blockPos) {
        final ChunkPos currentChunkPos = new ChunkPos(blockPos);
        final ServerLevel serverLevel = context.getLevel().getLevel();
        final Biome biome = serverLevel.getBiome(blockPos).get();

        if (!trackedChunks.containsKey(serverLevel) || !trackedChunks.get(serverLevel).containsKey(currentChunkPos)) {
            if (chanceOfSpawningPerChunk > randomSource.nextFloat()) {
                // will spawn
                generateAllowedBlocksInChunk(context, randomSource, blockPos);
            } else {
                // mark chunk as dead, with no availabilities
                generateDeadChunk(context, blockPos, biome);
            }
        }

        Biome allowedBiome = trackedChunks.get(serverLevel).get(currentChunkPos).getA();
        HashSet<Block> allowedBlocks = trackedChunks.get(serverLevel).get(currentChunkPos).getB();
        if (allowedBiome != biome || allowedBlocks.isEmpty()) {
            return false;
        }

        // Make sure it's an ore
        if (context.topFeature().isEmpty() || !(context.topFeature().get().feature().value().config() instanceof OreConfiguration oreConfiguration)) {
            return false;
        }

        // check if valid
        for (OreConfiguration.TargetBlockState targetBlockState : oreConfiguration.targetStates) {
            if (allowedBlocks.contains(targetBlockState.state.getBlock())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check at 4 locations in the middle, with varying heights:
     * (Min + Current Position) / 2
     * Current Position
     * (Max + Current Position) / 2
     * Max
     * Pick the biome in a random position
     * This algorithm is totally arbitrary, but allows for checks of vertical biomes and minimize starving some biomes
     *
     * */
    private void generateAllowedBlocksInChunk(@NotNull PlacementContext context, @NotNull RandomSource randomSource, @NotNull BlockPos blockPos) {
        final int KEPT_ORES_PER_CHUNK = 2;

        final ChunkPos currentChunkPos = new ChunkPos(blockPos);
        final ChunkAccess protoChunk = context.getLevel().getChunk(blockPos);
        final ServerLevel serverLevel = context.getLevel().getLevel();

        // Get the biome target
        final List<Integer> OPTIONS = Arrays.asList(
                (protoChunk.getMinBuildHeight() + blockPos.getY()) / 2,
                blockPos.getY(),
                (protoChunk.getMaxBuildHeight() + blockPos.getY()) / 2,
                protoChunk.getMaxBuildHeight()
        );
        final int randomY = OPTIONS.get(randomSource.nextInt(OPTIONS.size()));
        final BlockPos randomPos = new BlockPos(currentChunkPos.getMiddleBlockX(), randomY, currentChunkPos.getMiddleBlockZ());
        final Biome biome = context.getLevel().getLevel().getBiome(randomPos).get();

        // get the allowed blocks
        final HashSet<Block> allowedBlocks = new HashSet<>(LargePatchGeneratorConfig.getKRandomTargetedBlocks(randomSource, KEPT_ORES_PER_CHUNK, biome));

        // Generate tuple
        final Tuple<Biome, HashSet<Block>> newTuple = new Tuple<>(biome, allowedBlocks);

        // put new value
        if (trackedChunks.containsKey(serverLevel)) {
            trackedChunks.get(serverLevel).put(currentChunkPos, newTuple);
        } else {
            HashMap<ChunkPos, Tuple<Biome, HashSet<Block>>> innerMap = new HashMap<>();
            innerMap.put(currentChunkPos, newTuple);
            trackedChunks.put(serverLevel, innerMap);
        }
    }

    private void generateDeadChunk(@NotNull PlacementContext context, @NotNull BlockPos blockPos, @NotNull Biome biome) {
        final ChunkPos currentChunkPos = new ChunkPos(blockPos);
        final ServerLevel serverLevel = context.getLevel().getLevel();

        // Generate tuple
        final Tuple<Biome, HashSet<Block>> deadTuple = new Tuple<>(biome, new HashSet<>());

        // put new value
        if (trackedChunks.containsKey(serverLevel)) {
            trackedChunks.get(serverLevel).put(currentChunkPos, deadTuple);
        } else {
            HashMap<ChunkPos, Tuple<Biome, HashSet<Block>>> innerMap = new HashMap<>();
            innerMap.put(currentChunkPos, deadTuple);
            trackedChunks.put(serverLevel, innerMap);
        }
    }

    @Override
    public PlacementModifierType<?> type() {
        return ModPlacementModifiers.SPREAD_FILTER.get();
    }
}
