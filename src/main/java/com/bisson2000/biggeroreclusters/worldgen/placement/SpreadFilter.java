package com.bisson2000.biggeroreclusters.worldgen.placement;

import com.bisson2000.biggeroreclusters.BiggerOreClusters;
import com.bisson2000.biggeroreclusters.config.BiggerOreClustersConfig;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementFilter;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class SpreadFilter extends PlacementFilter {

    private static final SpreadFilter INSTANCE = new SpreadFilter();
    public static final Codec<SpreadFilter> CODEC = Codec.unit(() -> INSTANCE);

    // Static HashMap to track chunk positions
    public static final ConcurrentHashMap<ServerLevel, HashMap<ChunkPos, Tuple<Biome, HashSet<Holder<PlacedFeature>>>>> trackedChunks = new ConcurrentHashMap<>();

    // DEBUG
    // public static final ConcurrentHashMap<ServerLevel, ChunkPos> removedChunks = new ConcurrentHashMap<>();


    @Override
    protected boolean shouldPlace(@NotNull PlacementContext context, @NotNull RandomSource randomSource, @NotNull BlockPos blockPos) {
        final ChunkPos currentChunkPos = new ChunkPos(blockPos);
        final ServerLevel serverLevel = context.getLevel().getLevel();
        final Biome biome = serverLevel.getBiome(blockPos).get();

        if (!trackedChunks.containsKey(serverLevel) || !trackedChunks.get(serverLevel).containsKey(currentChunkPos)) {
            if (BiggerOreClustersConfig.ODDS_OF_ORES_IN_CHUNK.get() > randomSource.nextDouble()) {
                // will spawn
                generateAllowedFeaturesInChunk(context, randomSource, blockPos);
            } else {
                // mark chunk as dead, with no availabilities
                generateDeadChunk(context, blockPos, biome);
            }
        }

//        if (removedChunks.containsKey(serverLevel) && removedChunks.get(serverLevel) == currentChunkPos) {
//            System.out.println("Regenerating in a removed chunk! at pos x:" + currentChunkPos.x + " z:" + currentChunkPos.z);
//        }

        Biome allowedBiome = trackedChunks.get(serverLevel).get(currentChunkPos).getA();
        HashSet<Holder<PlacedFeature>> allowedHoldingFeatures = trackedChunks.get(serverLevel).get(currentChunkPos).getB();
        if (allowedBiome != biome || allowedHoldingFeatures.isEmpty()) {
            return false;
        }

        // Make sure it's a valid feature
        HashSet<PlacedFeature> allowedFeatures = allowedHoldingFeatures.stream().map(Holder::value).collect(Collectors.toCollection(HashSet::new));
        if (context.topFeature().isEmpty() || !allowedFeatures.contains(context.topFeature().get())) {
            return false;
        }

        // valid feature
        return true;
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
    private void generateAllowedFeaturesInChunk(@NotNull PlacementContext context, @NotNull RandomSource randomSource, @NotNull BlockPos blockPos) {
        final int VARIETY_PER_CHUNK = BiggerOreClustersConfig.VARIETY_PER_CHUNK.get();

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

        // get the allowed features
        final HashSet<Holder<PlacedFeature>> allowedFeatures = new HashSet<>(BiggerOreClustersConfig.getKRandomTargets(randomSource, VARIETY_PER_CHUNK, biome));

        // Generate tuple
        final Tuple<Biome, HashSet<Holder<PlacedFeature>>> newTuple = new Tuple<>(biome, allowedFeatures);

        // put new value
        if (trackedChunks.containsKey(serverLevel)) {
            trackedChunks.get(serverLevel).put(currentChunkPos, newTuple);
        } else {
            HashMap<ChunkPos, Tuple<Biome, HashSet<Holder<PlacedFeature>>>> innerMap = new HashMap<>();
            innerMap.put(currentChunkPos, newTuple);
            trackedChunks.put(serverLevel, innerMap);
        }
    }

    private void generateDeadChunk(@NotNull PlacementContext context, @NotNull BlockPos blockPos, @NotNull Biome biome) {
        final ChunkPos currentChunkPos = new ChunkPos(blockPos);
        final ServerLevel serverLevel = context.getLevel().getLevel();

        // Generate tuple
        final Tuple<Biome, HashSet<Holder<PlacedFeature>>> deadTuple = new Tuple<>(biome, new HashSet<>());

        // put new value
        if (trackedChunks.containsKey(serverLevel)) {
            trackedChunks.get(serverLevel).put(currentChunkPos, deadTuple);
        } else {
            HashMap<ChunkPos, Tuple<Biome, HashSet<Holder<PlacedFeature>>>> innerMap = new HashMap<>();
            innerMap.put(currentChunkPos, deadTuple);
            trackedChunks.put(serverLevel, innerMap);
        }
    }

    @Override
    public PlacementModifierType<?> type() {
        return ModPlacementModifiers.SPREAD_FILTER.get();
    }

    /**
     * Using this event to clear the trackedChunks cache
     * Once a chunk is generated, the cache for it becomes useless
     * This event is only called once the chunk is gully generated.
     * Therefore, we exploit this event.
     *
     * */
    @Mod.EventBusSubscriber(modid = BiggerOreClusters.MOD_ID)
    private static class EventHandler {
        @SubscribeEvent
        public static void attachChunkCapabilities(final AttachCapabilitiesEvent<LevelChunk> event) {
            final LevelChunk chunk = event.getObject();
            if (!(chunk.getLevel() instanceof ServerLevel serverLevel)) return;

            if(SpreadFilter.trackedChunks.containsKey(serverLevel)) {
                SpreadFilter.trackedChunks.get(serverLevel).remove(chunk.getPos());
                if (SpreadFilter.trackedChunks.get(serverLevel).isEmpty()) {
                    SpreadFilter.trackedChunks.remove(serverLevel);
                }
            }

            //removedChunks.put(serverLevel, chunk.getPos());
        }
    }
}
