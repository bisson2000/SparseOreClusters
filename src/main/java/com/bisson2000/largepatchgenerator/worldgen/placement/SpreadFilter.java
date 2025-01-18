package com.bisson2000.largepatchgenerator.worldgen.placement;

import com.bisson2000.largepatchgenerator.config.LargePatchGeneratorConfig;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
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
    public static final Codec<ChunkPos> CHUNK_POS_CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    Codec.INT.fieldOf("x").forGetter(c -> c.x),
                    Codec.INT.fieldOf("z").forGetter(c -> c.z)
            ).apply(instance, ChunkPos::new)
    );

    public static final Codec<Map<ChunkPos, HashSet<Block>>> USED_CHUNKS_CODEC = Codec.unboundedMap(
            CHUNK_POS_CODEC,
            Codec.list(ForgeRegistries.BLOCKS.getCodec()).xmap(HashSet::new, blocks -> blocks.stream().toList())
    );

    public static final Codec<SpreadFilter> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    Codec.FLOAT.fieldOf("base_odd").forGetter(filter -> filter.chanceOfSpawningPerChunk),
                    USED_CHUNKS_CODEC.fieldOf("used_chunks").forGetter(filter -> SpreadFilter.trackedChunks)
            ).apply(instance, SpreadFilter::new)
    );

    private final float chanceOfSpawningPerChunk;

    // Static HashMap to track chunk positions
    public static final HashMap<ChunkPos, HashSet<Block>> trackedChunks = new HashMap<>();

    public SpreadFilter(float chanceOfSpawningPerChunk) {
        this.chanceOfSpawningPerChunk = chanceOfSpawningPerChunk;
    }

    public SpreadFilter(float chanceOfSpawning, Map<ChunkPos, HashSet<Block>> trackedChunks) {
        this.chanceOfSpawningPerChunk = chanceOfSpawning;
        trackedChunks.forEach((k, v) -> {
            if (!SpreadFilter.trackedChunks.containsKey(k)) {
                SpreadFilter.trackedChunks.get(k).addAll(v);
            }
        });
    }

    @Override
    protected boolean shouldPlace(@NotNull PlacementContext context, @NotNull RandomSource randomSource, @NotNull BlockPos blockPos) {
        final int KEPT_ORES_PER_CHUNK = 2;
        final ChunkPos currentChunkPos = new ChunkPos(blockPos);
        final Biome biome = context.getLevel().getLevel().getBiome(blockPos).get();

//        ChunkSelectedBlocksCapability.getChunkSelectedBlocks(context.getLevel().getLevel(), currentChunkPos).ifPresent(iChunkSelectedBlocks -> {
//            if (!(iChunkSelectedBlocks instanceof ChunkSelectedBlocks chunkSelectedBlocks)) return;
//
//            chunkSelectedBlocks.isBlockTargetedInBiome(biome, Blocks.IRON_ORE);
//        });




        // Calculate the chunk's permissions
        if (!SpreadFilter.trackedChunks.containsKey(currentChunkPos)) {
            if (chanceOfSpawningPerChunk > randomSource.nextFloat()) {
                // will spawn
                HashSet<Block> newAllowedBlocks = new HashSet<>(LargePatchGeneratorConfig.getKRandomTargetedBlocks(randomSource, KEPT_ORES_PER_CHUNK));
                SpreadFilter.trackedChunks.put(currentChunkPos, newAllowedBlocks);
            } else {
                // mark chunk as dead
                SpreadFilter.trackedChunks.put(currentChunkPos, new HashSet<>());
            }
        }
        HashSet<Block> allowedBlocks = SpreadFilter.trackedChunks.get(currentChunkPos);
        if (allowedBlocks.isEmpty()) {
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

    @Override
    public PlacementModifierType<?> type() {
        return ModPlacementModifiers.SPREAD_FILTER.get();
    }
}
