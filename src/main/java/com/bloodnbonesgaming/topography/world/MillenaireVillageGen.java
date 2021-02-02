package com.bloodnbonesgaming.topography.world;

import com.bloodnbonesgaming.topography.Topography;
import com.bloodnbonesgaming.topography.config.*;
import com.bloodnbonesgaming.topography.world.chunkgenerator.ChunkGeneratorVoid;
import com.bloodnbonesgaming.topography.world.generator.IGenerator;
import com.bloodnbonesgaming.topography.world.generator.SkyIslandGenerator;
import com.bloodnbonesgaming.topography.world.generator.SkyIslandGeneratorV2;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraftforge.fml.common.IWorldGenerator;
import org.millenaire.common.config.MillConfigParameter;
import org.millenaire.common.config.MillConfigValues;
import org.millenaire.common.culture.Culture;
import org.millenaire.common.culture.VillageType;
import org.millenaire.common.world.WorldGenVillage;

import java.util.Map;
import java.util.Random;

public class MillenaireVillageGen extends WorldGenVillage {
    @Override
    public void generate(Random random, int chunkX, int chunkZ, World world, IChunkGenerator chunkGenerator, IChunkProvider chunkProvider) {
        if (world.provider.getDimension() != 0 || world.isRemote) {
            return;
        }

        ConfigPreset preset = ConfigurationManager.getInstance().getPreset();
        if (preset == null) {
            return;
        }
        DimensionDefinition definition = preset.getDefinition(world.provider.getDimension());
        for (final IGenerator generator : definition.getGenerators()) {
            if (generator instanceof SkyIslandGeneratorV2) {
                SkyIslandGeneratorV2 skyIslandsGenerator = (SkyIslandGeneratorV2) generator;
                if (!skyIslandsGenerator.isMillenaireIslandEnable()) {
                    continue;
                }

                Map<SkyIslandData, Map<BlockPos, SkyIslandType>> islandPositions = skyIslandsGenerator.getIslandPositions(world.getSeed(), chunkX * 16, chunkZ * 16);
                islandPositions.forEach((skyIslandData, blockPosSkyIslandTypeMap) -> {
                    if (skyIslandData instanceof SkyIslandDataV2MillenaireVillage) {
                        for (Map.Entry<BlockPos, SkyIslandType> entry : blockPosSkyIslandTypeMap.entrySet()) {
                            BlockPos pos = entry.getKey();
                            int islandChunkX = pos.getX() >> 4;
                            int islandChunkZ = pos.getZ() >> 4;
                            if (islandChunkX != chunkX || islandChunkZ != chunkZ) {
                                return;
                            }
                            SkyIslandDataV2MillenaireVillage data = (SkyIslandDataV2MillenaireVillage) skyIslandData;
                            VillageType villageType = data.getVillageType();
                            float completionRatio = 0;
                            if (MillConfigValues.villageSpawnCompletionMaxPercentage > 0) {
                                completionRatio = (float) random.nextInt(MillConfigValues.villageSpawnCompletionMaxPercentage) / 100;
                                completionRatio = random.nextFloat() * completionRatio;
                            }
                            boolean result = this.generateVillageAtPoint(world, random, pos.getX(), pos.getY(), pos.getZ(), null, false, true, true, 0, villageType, null, null, completionRatio);
                            if (result) {
                                return;
                            }

                            int tryCount = 10;
                            do {
                                BlockPos newPos = pos;
                                Topography.instance.getLog().info("failed to spawn village[" + villageType.culture + "," + villageType.name + "] at island[" + newPos.getX() + "," + newPos.getZ() + "] completionRatio:" + completionRatio);
                                if (tryCount < 0) {
                                    completionRatio -= 0.01;
                                } else {
                                    tryCount--;
                                }

                                newPos = generatePos(random, data, pos);
                                Topography.instance.getLog().info("try to spawn again village[" + villageType.culture + "," + villageType.name + "] at island[" + newPos.getX() + "," + newPos.getZ() + "] completionRatio:" + completionRatio);
                                result = this.generateVillageAtPoint(world, random, newPos.getX(), newPos.getY(), newPos.getZ(), null, false, true, true, 0, villageType, null, null, completionRatio);

                            } while (!result && completionRatio >= 0);
                        }

                    }
                });
            }
        }


    }

    private BlockPos generatePos(Random random, SkyIslandDataV2MillenaireVillage data, BlockPos center) {
        int maxRadius = (int) Math.round(data.getVillageType().radius * 0.5);
        int randomOffsetX = random.nextInt(maxRadius * 2) - maxRadius;
        int randomOffsetZ = random.nextInt(maxRadius * 2) - maxRadius;
        return center.add(randomOffsetX, 0, randomOffsetZ);


    }
}
