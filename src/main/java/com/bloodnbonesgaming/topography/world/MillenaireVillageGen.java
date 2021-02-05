package com.bloodnbonesgaming.topography.world;

import com.bloodnbonesgaming.topography.Topography;
import com.bloodnbonesgaming.topography.config.*;
import com.bloodnbonesgaming.topography.world.generator.IGenerator;
import com.bloodnbonesgaming.topography.world.generator.SkyIslandGeneratorV2;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
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
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MillenaireVillageGen extends WorldGenVillage {
    final private static Method REFLECT_GENERATE_VILLAGE_ACCESSOR = ObfuscationReflectionHelper.findMethod(WorldGenVillage.class, "generateVillage", boolean.class, Point.class, World.class, VillageType.class, EntityPlayer.class, EntityPlayer.class, Random.class, int.class, String.class, Point.class, float.class, boolean.class, boolean.class);
    private Map<SkyIslandGeneratorV2, Map<SkyIslandData, Map<BlockPos, SkyIslandType>>> cacheIslandsPositions = new HashMap<>();
    private Map<SkyIslandGeneratorV2, Map<ChunkPos, CachedIslandData>> cacheIslandsChunkPositions = new HashMap<>();

    class CachedIslandData {
        public CachedIslandData(SkyIslandData data, BlockPos pos, SkyIslandType type) {
            this.data = data;
            this.pos = pos;
            this.type = type;
        }

        SkyIslandData data;
        BlockPos pos;
        SkyIslandType type;
    }

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
                Map<SkyIslandData, Map<BlockPos, SkyIslandType>> islandPositions = skyIslandsGenerator.getIslandPositions(world.getSeed(), chunkX * 16, chunkZ * 16);

                if (!cacheIslandsPositions.containsKey(skyIslandsGenerator) || cacheIslandsPositions.get(skyIslandsGenerator) != islandPositions) {
                    cacheIslandsPositions.put(skyIslandsGenerator, islandPositions);
                    HashMap<ChunkPos, CachedIslandData> chunkMap = new HashMap<>();
                    cacheIslandsChunkPositions.put(skyIslandsGenerator, chunkMap);

                    for (Map.Entry<SkyIslandData, Map<BlockPos, SkyIslandType>> dataEntry : islandPositions.entrySet()) {
                        for (Map.Entry<BlockPos, SkyIslandType> islandEntry : dataEntry.getValue().entrySet()) {
                            BlockPos islandpos = islandEntry.getKey();
                            ChunkPos islandChunkPos = new ChunkPos(islandpos);
                            CachedIslandData cachedIslandData = new CachedIslandData(dataEntry.getKey(), islandpos, islandEntry.getValue());
                            chunkMap.put(islandChunkPos,cachedIslandData);
                        }
                    }
                }
                ChunkPos currentChunkPos = new ChunkPos(chunkX,chunkZ);
                if(!cacheIslandsChunkPositions.get(skyIslandsGenerator).containsKey(currentChunkPos)){
                  continue;
                };
                CachedIslandData cachedData = cacheIslandsChunkPositions.get(skyIslandsGenerator).get(currentChunkPos);

                if (skyIslandsGenerator.isMillenaireIslandEnable()) {
                    generateVillageTask(random, chunkX, chunkZ, world, cachedData);
                }

                generateLoneTask(random, chunkX, chunkZ, world, cachedData, skyIslandsGenerator.getLoneBuildingChance(), skyIslandsGenerator.getKeyLoneBuildingChance());
            }
        }
//

//

    }

    private void generateLoneTask(Random random, int chunkX, int chunkZ, World world, CachedIslandData islandData, double generateRatio, double keyRatio) {
        if (!(islandData.data instanceof SkyIslandDataV2MillenaireVillage)) {
            BlockPos islandpos = islandData.pos;
            int islandChunkX = islandpos.getX() >> 4;
            int islandChunkZ = islandpos.getZ() >> 4;
            if (islandChunkX != chunkX || islandChunkZ != chunkZ) {
                return;
            }
            if (random.nextInt() > generateRatio) {
                return;
            }

            MillWorldData millWorld = Mill.getMillWorld(world);
            int minVillagesDistance = Math.min(2147483647, MillConfigValues.minDistanceBetweenVillagesAndLoneBuildings);
            int minLoneBuildingsDistance = Math.min(2147483647, MillConfigValues.minDistanceBetweenLoneBuildings);
            boolean tryKeyLoneBuilding = false;

            Point islandPoint = new Point(islandpos);
            // it wouldn't work will cause island's usable area is too distribute
//                    tryKeyLoneBuilding = millWorld.villagesList.pos.stream().anyMatch(point -> islandPoint.distanceTo(point) < minVillagesDistance && islandPoint.distanceTo(point) > (minVillagesDistance / 2.0));
//
//
//                    if(!tryKeyLoneBuilding){
//                        tryKeyLoneBuilding = millWorld.villagesList.pos.stream().anyMatch(point -> islandPoint.distanceTo(point) < minLoneBuildingsDistance && islandPoint.distanceTo(point) < (minLoneBuildingsDistance / 4.0));
//                    }
            tryKeyLoneBuilding = random.nextInt() < keyRatio;
            EntityPlayer worldClosestPlayer = world.getClosestPlayer(islandpos.getX(), islandpos.getY(), islandpos.getZ(), 200, false);
            HashMap<String, Integer> loneTypesCountingMap = (HashMap<String, Integer>) millWorld.loneBuildingsList.types.stream().collect(Collectors.groupingBy(Function.identity(), Collectors.summingInt(x -> 1)));

            boolean result;
            int spawnTry = 20;
            do {


                BlockPos pos;// = choiceRandomPosOnIsland(random, islandpos, (int) Math.round(skyIslandData.getRadius() * 0.7));
//                      lone building generating logic
                int tryCount = 5;
                do {
                    pos = choiceRandomPosOnIsland(random, islandpos, (int) Math.round(islandData.data.getRadius() * 0.7));
                    pos = world.getTopSolidOrLiquidBlock(pos);
                    tryCount--;
                } while (pos.getY() <= 0 && tryCount > 0);
                if (pos.getY() <= 0) {
                    return;
                }


//                        BlockPos finalPos = pos;
                String biomeName = SkyIslandDataV2MillenaireVillage.getBiomeName(Biome.getIdForBiome(world.getBiome(pos)));


                float completionRatio = calculatingCompletionRatio(random);
//        this.generateVillageAtPoint(world, random, finalPos.getX(), finalPos.getY(), finalPos.getZ(), null, false, true, true, 0, choiceLoneType, null, null, completionRatio);
                try {
                    Point targetPos = new Point(pos);


                    VillageType choiceLoneType = getRandomChoiceLoneType(biomeName, worldClosestPlayer, millWorld, loneTypesCountingMap, targetPos, true, tryKeyLoneBuilding);

                    if (choiceLoneType == null) {
                        if(tryKeyLoneBuilding){
                            tryKeyLoneBuilding = false;
                            result = false;
                            continue;
                        }
                        return;
                    }

                    result = this.generateVillagePrivate(targetPos, world, choiceLoneType, null, worldClosestPlayer, random, 2147483647, null, null, completionRatio, false, true);
                    spawnTry--;
                    if (!result) {
                        Topography.instance.getLog().info("failed to spawn lone building[" + choiceLoneType.culture + "," + choiceLoneType.name + "] at island[" + pos.getX() + "," + pos.getZ() + "] completionRatio:" + completionRatio);
                    }
                    if (result && worldClosestPlayer != null && choiceLoneType.isKeyLoneBuildingForGeneration(worldClosestPlayer) && choiceLoneType.keyLoneBuildingGenerateTag != null) {
                        UserProfile profile = millWorld.getProfile(worldClosestPlayer);
                        profile.clearTag(choiceLoneType.keyLoneBuildingGenerateTag);
                    }
                } catch (MillLog.MillenaireException e) {
                    Topography.instance.getLog().error("Exception while trying to generating lone building");
                    e.printStackTrace();
                    return;
                }
            } while (!result && spawnTry > 0);


        }

    }

    private VillageType getRandomChoiceLoneType(String biomeName, EntityPlayer worldClosestPlayer, MillWorldData millWorld, HashMap<String, Integer> loneTypesCountingMap, Point targetPoint, boolean ignoreBiome, boolean tryKeyLoneBuilding) {
        List<VillageType> effectiveLoneTypes = Culture.ListCultures.stream()
                .flatMap(culture -> culture.listLoneBuildingTypes.stream())
//                .filter(villageType -> villageType.biomes.size() >0)
                //if ignoreBiome then feed it with one of it's biome list value to bypass the biome check
                .filter(villageType -> villageType.isValidForGeneration(millWorld, worldClosestPlayer, loneTypesCountingMap, targetPoint, ignoreBiome && villageType.biomes.size() > 0 ? villageType.biomes.get(0) : biomeName, tryKeyLoneBuilding))
                .collect(Collectors.toList());
        if (effectiveLoneTypes.isEmpty()) {
            return null;
        }
        VillageType choiceLoneType = (VillageType) MillCommonUtilities.getWeightedChoice(effectiveLoneTypes, worldClosestPlayer);
        return choiceLoneType;
    }

    private boolean generateVillagePrivate(Point targetPos, World world, VillageType type, EntityPlayer generatingPlayer, EntityPlayer closestPlayer, Random random, int minDistance, String name, Point parentVillage, float completion, boolean testBiome, boolean alwaysSpawn) throws MillLog.MillenaireException {
        try {
            return (boolean) REFLECT_GENERATE_VILLAGE_ACCESSOR.invoke(this, targetPos, world, type, generatingPlayer, closestPlayer, random, minDistance, name, parentVillage, completion, testBiome, alwaysSpawn);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private void generateVillageTask(Random random, int chunkX, int chunkZ, World world, CachedIslandData islandData) {
        if (islandData.data instanceof SkyIslandDataV2MillenaireVillage) {
            BlockPos pos = islandData.pos;
            int islandChunkX = pos.getX() >> 4;
            int islandChunkZ = pos.getZ() >> 4;
            if (islandChunkX != chunkX || islandChunkZ != chunkZ) {
                return;
            }
            SkyIslandDataV2MillenaireVillage data = (SkyIslandDataV2MillenaireVillage) islandData.data;
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

                newPos = choiceRandomPosOnIsland(random, pos, (int) Math.round(data.getVillageType().radius * 0.5));
                Topography.instance.getLog().info("try to spawn again village[" + villageType.culture + "," + villageType.name + "] at island[" + newPos.getX() + "," + newPos.getZ() + "] completionRatio:" + completionRatio);
                result = this.generateVillageAtPoint(world, random, newPos.getX(), newPos.getY(), newPos.getZ(), null, false, true, true, 0, villageType, null, null, completionRatio);

            } while (!result && completionRatio >= 0);
//                    if(!result){
//                        completionRatio = calculatingCompletionRatio(random);
//                        String biomeName = SkyIslandDataV2MillenaireVillage.getBiomeName(Biome.getIdForBiome(world.getBiome(pos)));
//                        EntityPlayer worldClosestPlayer = world.getClosestPlayer(pos.getX(), pos.getY(), pos.getZ(), 200, false);
//                        MillWorldData millWorld = Mill.getMillWorld(world);
//                        HashMap<String, Integer> loneTypesCountingMap = (HashMap<String, Integer>) millWorld.loneBuildingsList.types.stream().collect(Collectors.groupingBy(Function.identity(), Collectors.summingInt(x -> 1)));
//                        VillageType choiceLoneType = getRandomChoiceLoneType(biomeName, worldClosestPlayer, millWorld, loneTypesCountingMap, finalPos);
//
//                        if (choiceLoneType == null) {
//                            return;
//                        }
//                        if (result && worldClosestPlayer != null && choiceLoneType.isKeyLoneBuildingForGeneration(worldClosestPlayer) && choiceLoneType.keyLoneBuildingGenerateTag != null) {
//                            UserProfile profile = millWorld.getProfile(worldClosestPlayer);
//                            profile.clearTag(choiceLoneType.keyLoneBuildingGenerateTag);
//                        }
//                    }


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

    private BlockPos choiceRandomPosOnIsland(Random random, BlockPos center, int maxRadius) {
//        int maxRadius = (int) Math.round(data.getVillageType().radius * range);
        int randomOffsetX = random.nextInt(maxRadius * 2) - maxRadius;
        int randomOffsetZ = random.nextInt(maxRadius * 2) - maxRadius;
        return center.add(randomOffsetX, 0, randomOffsetZ);


    }
}
