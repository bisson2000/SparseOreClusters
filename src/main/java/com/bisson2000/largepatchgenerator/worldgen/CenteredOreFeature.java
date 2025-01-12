package com.bisson2000.largepatchgenerator.worldgen;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.OreFeature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;

public class CenteredOreFeature extends OreFeature {
    public CenteredOreFeature(Codec<OreConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<OreConfiguration> context) {
        RandomSource randomsource = context.random();
        BlockPos blockpos = context.origin();
        WorldGenLevel worldgenlevel = context.level();
        OreConfiguration oreconfiguration = context.config();
        float f = randomsource.nextFloat() * (float) Math.PI;

        // Division by 8 because we can explore in 2 * 2 * 2 directions
        float explorationRangeInDirection = (float)oreconfiguration.size / 8.0F;
        int i = Mth.ceil((explorationRangeInDirection + 1.0F) / 2.0F);

        double minX = blockpos.getX() - explorationRangeInDirection;
        double maxX = blockpos.getX() + explorationRangeInDirection;
        double minZ = blockpos.getZ() - explorationRangeInDirection;
        double maxZ = blockpos.getZ() + explorationRangeInDirection;
        double minY = blockpos.getY();
        double maxY = blockpos.getY();
        int x = blockpos.getX();
        int y = blockpos.getY();
        int z = blockpos.getZ();

        int width = 2 * (Mth.ceil(explorationRangeInDirection) + i);
        int height = 2 * (2 + i);

        for (int l1 = 0; l1 <= width; l1++) {
            for (int i2 = 0; i2 <= width; i2++) {
                if (y <= worldgenlevel.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, l1, i2)) {
                    return this.doPlace(worldgenlevel, randomsource, oreconfiguration, minX, maxX, minZ, maxZ, minY, maxY, x, y, z, width, height);
                }
            }
        }

        return false;
//        RandomSource randomsource = context.random();
//        BlockPos blockpos = context.origin();
//        WorldGenLevel worldgenlevel = context.level();
//        OreConfiguration oreconfiguration = context.config();
//        float f = randomsource.nextFloat() * (float) Math.PI;
//
//        // Division by 8 because we can explore in 2 * 2 * 2 directions
//        float f1 = (float)oreconfiguration.size / 8.0F;
//        int i = Mth.ceil(((float)oreconfiguration.size / 16.0F * 2.0F + 1.0F) / 2.0F);
//        double minX = (double)blockpos.getX() + Math.sin((double)f) * (double)f1;
//        double maxX = (double)blockpos.getX() - Math.sin((double)f) * (double)f1;
//        double minZ = (double)blockpos.getZ() + Math.cos((double)f) * (double)f1;
//        double maxZ = (double)blockpos.getZ() - Math.cos((double)f) * (double)f1;
//        double minY = (double)(blockpos.getY() + randomsource.nextInt(3) - 2);
//        double maxY = (double)(blockpos.getY() + randomsource.nextInt(3) - 2);
//        int x = blockpos.getX() - Mth.ceil(f1) - i;
//        int y = blockpos.getY() - 2 - i;
//        int z = blockpos.getZ() - Mth.ceil(f1) - i;
//        int width = 2 * (Mth.ceil(f1) + i);
//        int height = 2 * (2 + i);
//
//        for (int l1 = 0; l1 <= width; l1++) {
//            for (int i2 = 0; i2 <= width; i2++) {
//                if (y <= worldgenlevel.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, l1, i2)) {
//                    return this.doPlace(worldgenlevel, randomsource, oreconfiguration, minX, maxX, minZ, maxZ, minY, maxY, x, y, z, width, height);
//                }
//            }
//        }
//
//        return false;
    }
}
