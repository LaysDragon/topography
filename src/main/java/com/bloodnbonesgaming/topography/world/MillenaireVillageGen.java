package com.bloodnbonesgaming.topography.world;

import com.bloodnbonesgaming.topography.Topography;
import com.bloodnbonesgaming.topography.config.*;
import com.bloodnbonesgaming.topography.world.generator.IGenerator;
import com.bloodnbonesgaming.topography.world.generator.SkyIslandGeneratorV2;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import org.millenaire.common.config.MillConfigValues;
import org.millenaire.common.culture.Culture;
import org.millenaire.common.culture.VillageType;
import org.millenaire.common.forge.Mill;
import org.millenaire.common.utilities.MillCommonUtilities;
import org.millenaire.common.utilities.MillLog;
import org.millenaire.common.utilities.Point;
import org.millenaire.common.world.MillWorldData;
import org.millenaire.common.world.UserProfile;
import org.millenaire.common.world.WorldGenVillage;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MillenaireVillageGen extends WorldGenVillage {
    final private static Method REFLECT_GENERATE_VILLAGE_ACCESSOR = ObfuscationReflectionHelper.findMethod(WorldGenVillage.class, "generateVillage", boolean.class, Point.class, World.class, VillageType.class, EntityPlayer.class, EntityPlayer.class, Random.class, int.class, String.class, Point.class, float.class, boolean.class, boolean.class);

    @Override
    public void generate(Random random, int chunkX, int chunkZ, World world, IChunkGenerator chunkGenerator, IChunkProvider chunkProvider) {
        if (world.provider.getDimension() != 0 || world.isRemote) {
            return;
        }

        generateVillageTask(random, chunkX, chunkZ, world);

        generateLoneTask(random, chunkX, chunkZ, world);
//

//

    }

    private void generateLoneTask(Random random, int chunkX, int chunkZ, World world) {
        //lone building generating logic
        BlockPos pos;
        int tryCount = 5;
        do {
            pos = new BlockPos(chunkX * 16 + random.nextInt(16), 255, chunkZ * 16 + random.nextInt(16));
            pos = world.getTopSolidOrLiquidBlock(pos);
            tryCount--;
        } while (pos.getY() <= 0 && tryCount > 0);
        String biomeName = SkyIslandDataV2MillenaireVillage.getBiomeName(Biome.getIdForBiome(world.getBiome(pos)));
        EntityPlayer worldClosestPlayer = world.getClosestPlayer(pos.getX(), pos.getY(), pos.getZ(), 200, false);
        MillWorldData millWorld = Mill.getMillWorld(world);
        HashMap<String, Integer> loneTypesCountingMap = (HashMap<String, Integer>) millWorld.loneBuildingsList.types.stream().collect(Collectors.groupingBy(Function.identity(), Collectors.summingInt(x -> 1)));
        BlockPos finalPos = pos;
        List<VillageType> effectiveLoneTypes = Culture.ListCultures.stream()
                .flatMap(culture -> culture.listLoneBuildingTypes.stream())
                .filter(villageType -> villageType.isValidForGeneration(millWorld, worldClosestPlayer, loneTypesCountingMap, new Point(finalPos), biomeName, true))
                .collect(Collectors.toList());
        if (effectiveLoneTypes.isEmpty()) {
            return;
        }
        VillageType choiceLoneType = (VillageType) MillCommonUtilities.getWeightedChoice(effectiveLoneTypes, worldClosestPlayer);
        if (choiceLoneType == null) {
            return;
        }
        float completionRatio = calculatingCompletionRatio(random);
//        this.generateVillageAtPoint(world, random, finalPos.getX(), finalPos.getY(), finalPos.getZ(), null, false, true, true, 0, choiceLoneType, null, null, completionRatio);
        try {
            boolean result = this.generateVillagePrivate(new Point(finalPos), world, choiceLoneType, null, worldClosestPlayer, random, 2147483647, null, null, completionRatio, true, false);
            if (result && worldClosestPlayer != null && choiceLoneType.isKeyLoneBuildingForGeneration(worldClosestPlayer) && choiceLoneType.keyLoneBuildingGenerateTag != null) {
                UserProfile profile = millWorld.getProfile(worldClosestPlayer);
                profile.clearTag(choiceLoneType.keyLoneBuildingGenerateTag);
            }
        } catch (MillLog.MillenaireException e) {
            Topography.instance.getLog().error("Exception while trying to generating village");
            e.printStackTrace();
        }
    }

    private boolean generateVillagePrivate(Point targetPos, World world, VillageType type, EntityPlayer generatingPlayer, EntityPlayer closestPlayer, Random random, int minDistance, String name, Point parentVillage, float completion, boolean testBiome, boolean alwaysSpawn) throws MillLog.MillenaireException {
        try {
            return (boolean) REFLECT_GENERATE_VILLAGE_ACCESSOR.invoke(this, targetPos, world, type, generatingPlayer, closestPlayer, random, minDistance, name, parentVillage, completion, testBiome, alwaysSpawn);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private void generateVillageTask(Random random, int chunkX, int chunkZ, World world) {
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
                            float completionRatio = calculatingCompletionRatio(random);
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

                                newPos = choiceRandomPosOnIsland(random, data, pos);
                                Topography.instance.getLog().info("try to spawn again village[" + villageType.culture + "," + villageType.name + "] at island[" + newPos.getX() + "," + newPos.getZ() + "] completionRatio:" + completionRatio);
                                result = this.generateVillageAtPoint(world, random, newPos.getX(), newPos.getY(), newPos.getZ(), null, false, true, true, 0, villageType, null, null, completionRatio);

                            } while (!result && completionRatio >= 0);
                        }

                    }
                });
            }
        }
    }

    private float calculatingCompletionRatio(Random random) {
        float completionRatio = 0;
        if (MillConfigValues.villageSpawnCompletionMaxPercentage > 0) {
            completionRatio = (float) random.nextInt(MillConfigValues.villageSpawnCompletionMaxPercentage) / 100;
            completionRatio = random.nextFloat() * completionRatio;
        }
        return completionRatio;
    }

    private BlockPos choiceRandomPosOnIsland(Random random, SkyIslandDataV2MillenaireVillage data, BlockPos center) {
        int maxRadius = (int) Math.round(data.getVillageType().radius * 0.5);
        int randomOffsetX = random.nextInt(maxRadius * 2) - maxRadius;
        int randomOffsetZ = random.nextInt(maxRadius * 2) - maxRadius;
        return center.add(randomOffsetX, 0, randomOffsetZ);


    }
}
