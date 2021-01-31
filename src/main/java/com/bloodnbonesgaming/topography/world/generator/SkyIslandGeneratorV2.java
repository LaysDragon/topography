package com.bloodnbonesgaming.topography.world.generator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import com.bloodnbonesgaming.lib.util.noise.OpenSimplexNoiseGeneratorOctaves;
import com.bloodnbonesgaming.lib.util.script.ScriptClassDocumentation;
import com.bloodnbonesgaming.lib.util.script.ScriptMethodDocumentation;
import com.bloodnbonesgaming.topography.ModInfo;
import com.bloodnbonesgaming.topography.Topography;
import com.bloodnbonesgaming.topography.config.*;
import com.bloodnbonesgaming.topography.util.BWMUtil;
import com.bloodnbonesgaming.topography.util.MathUtil;
import com.bloodnbonesgaming.topography.util.noise.RunnableSimplexNoise1x1;
import com.bloodnbonesgaming.topography.world.decorator.DecorationData;
import com.bloodnbonesgaming.topography.world.generator.structure.SkyIslandMineshaftGenerator;
import com.bloodnbonesgaming.topography.world.generator.structure.SkyIslandStrongholdSimpleGenerator;
import com.bloodnbonesgaming.topography.world.generator.structure.SkyIslandVillageGenerator;
import com.bloodnbonesgaming.topography.world.layer.GenLayerBiomeSkyIslands;

import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.block.BlockFalling;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.BlockSand;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldEntitySpawner;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.NoiseGeneratorPerlin;
import net.minecraft.world.gen.layer.GenLayer;
import net.minecraft.world.gen.structure.MapGenMineshaft;
import net.minecraft.world.gen.structure.MapGenStronghold;
import net.minecraft.world.gen.structure.MapGenVillage;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureComponent;
import net.minecraft.world.gen.structure.StructureStart;

@ScriptClassDocumentation(documentationFile = ModInfo.SKY_ISLANDS_DOCUMENTATION_FOLDER + "SkyIslandGeneratorV2", classExplaination =
        "This generator generates sky islands in a pseudo-random pattern within grid regions, "
                + "allowing for a high level of generation control while giving the appearance of randomness. These can be created in a dimension file using 'new SkyIslandGeneratorV2()'. "
                + "When a player spawns in a dimension with this generator, they will spawn in the center of the first island to be generated.")
public class SkyIslandGeneratorV2 extends SkyIslandGenerator implements IStructureHandler {

    OpenSimplexNoiseGeneratorOctaves horizontalConeSkewNoise = null;
    private MapGenMineshaft mineshaft;
    private final MapGenStronghold stronghold = new SkyIslandStrongholdSimpleGenerator(this);
    private MapGenVillage village;
    private boolean BWMVillageCompat = false;
    private boolean BWMMineshaftCompat = false;
    double[] islandNoiseArray = new double[65536];
    double[] islandWaterNoiseArray = new double[65536];

    @Override
    public void generate(final World world, ChunkPrimer primer, int chunkX, int chunkZ, final Random random) {
        if (this.horizontalConeSkewNoise == null) {
            this.horizontalConeSkewNoise = new OpenSimplexNoiseGeneratorOctaves(world.getSeed());
        }

        final long seed = world.getSeed();

        if (this.terrainNoise == null) {
            this.terrainNoise = new OpenSimplexNoiseGeneratorOctaves(seed);
        }

        this.rand.setSeed((long) chunkX * 341873128712L + (long) chunkZ * 132897987541L);

        this.mountainRand.setSeed((long) ((int) Math.floor(chunkX * 16D / this.getRegionSize())) * 341873128712L + (long) ((int) Math.floor(chunkZ * 16D / this.getRegionSize())) * 132897987541L + seed);

//        this.biomesForGeneration = world.getBiomeProvider().getBiomes(this.biomesForGeneration, chunkX * 16, chunkZ * 16, 16, 16);

        this.generateIslands(seed, chunkX, chunkZ, primer);
//        this.flowWaterVertical(primer);

        this.replaceBiomeBlocks(seed, chunkX, chunkZ, primer);

        this.genDecorations(seed, chunkX, chunkZ, primer);


    }

    public void generateIslands(final long seed, final int chunkX, final int chunkZ, final ChunkPrimer primer) {
        final Iterator<Entry<SkyIslandData, Map<BlockPos, SkyIslandType>>> iterator = this.getIslandPositions(seed, chunkX * 16, chunkZ * 16).entrySet().iterator();

        while (iterator.hasNext()) {
            final Entry<SkyIslandData, Map<BlockPos, SkyIslandType>> entry = iterator.next();
            final SkyIslandDataV2 data = (SkyIslandDataV2) entry.getKey();
            final int chunkBlockX = chunkX * 16;
            final int chunkBlockZ = chunkZ * 16;

            for (final Entry<BlockPos, SkyIslandType> islandPos : entry.getValue().entrySet()) {
                if (this.canBeInChunk(islandPos.getKey(), data.getHorizontalRadius(), chunkBlockX, chunkBlockZ)) {


                    final int featureCenterX = islandPos.getKey().getX();
                    final int featureCenterZ = islandPos.getKey().getZ();
                    final int midHeight = islandPos.getKey().getY();
                    final double maxHorizontalRadius = data.getHorizontalRadius();
                    final double maxConeCoordinateSkewValue = maxHorizontalRadius * 0.6;
                    final double maxTopHeight = data.getTopHeight();
                    final double maxBottomHeight = data.getBottomHeight();
                    final double maxWaterHeight = data.getFluidDepth();
                    final double waterPercentage = Math.max(1 - islandPos.getValue().getFluidPercentage(), 0.0001);

                    RunnableSimplexNoise1x1.getNoise(this.islandNoiseArray, this.horizontalConeSkewNoise, (int) (midHeight - maxBottomHeight), (int) (midHeight + maxTopHeight), chunkBlockX, chunkBlockZ, maxConeCoordinateSkewValue, 3, 0.5);
                    RunnableSimplexNoise1x1.getNoise(this.islandWaterNoiseArray, this.horizontalConeSkewNoise, (int) (midHeight - maxBottomHeight), (int) (midHeight + maxTopHeight), chunkBlockX, chunkBlockZ, maxConeCoordinateSkewValue * 1.5, 3, 0.5);


                    for (double x = 0; x < 16; x++) {
                        final double realX = x + chunkBlockX;
                        final double xDistance = Math.pow(Math.abs(featureCenterX - realX), 2);

                        for (double z = 0; z < 16; z++) {
                            final double realZ = z + chunkBlockZ;

                            final double zDistance = Math.pow(Math.abs(featureCenterZ - realZ), 2);

                            if (Math.sqrt(xDistance + zDistance) <= maxHorizontalRadius) {
                                final SkyIslandType type = islandPos.getValue();
                                final Map<MinMaxBounds, IBlockState> boundsToState = type.getBoundsToStateMap();
                                //distance from the anti water ring radius
                                //they can be 100 in any direction already.
                                //If the skew is 60%, they can be another +30 
                                //If the radius is reduced by 30%, they can still be 100 in any direction, they are just unlikely to hit it

                                for (double y = midHeight - maxBottomHeight; y <= midHeight + maxTopHeight; y++) {
                                    //final double horizontalConeCoordinateSkew = (this.horizontalConeSkewNoise.eval(realX / maxConeCoordinateSkewValue, y / maxConeCoordinateSkewValue, realZ / maxConeCoordinateSkewValue, 3, 0.5) - 0.5) * maxConeCoordinateSkewValue;
                                    final double horizontalConeCoordinateSkew = (this.islandNoiseArray[(int) ((x * 16 + z) * 256 + y)] - 0.5) * maxConeCoordinateSkewValue;
                                    final double skewedXDistance = Math.pow(Math.abs(featureCenterX - (realX + horizontalConeCoordinateSkew)), 2);
                                    final double skewedZDistance = Math.pow(Math.abs(featureCenterZ - (realZ + horizontalConeCoordinateSkew)), 2);
                                    final double maxTopConeHeightAtPos = maxTopHeight / (maxHorizontalRadius * 0.7) * Math.sqrt(skewedXDistance + skewedZDistance);
                                    final double maxBottomConeHeightAtPos = maxBottomHeight / (maxHorizontalRadius * 0.7) * Math.sqrt(skewedXDistance + skewedZDistance);

                                    //Rather than having an artificial minimum height ring, instead use the normal height of the cone.
                                    //This should be possible because of the horizontal skew on the cones, which allows them to look fine without extra noise.
                                    //Noise can be applied, using the height above the minimum required for keeping water from falling into the void as a maximum that the noise can reduce the height by.
                                    //This will need to go up faster than the actual height changes, so that it can go below the height required for water to spawn.


                                    final double bottomHeight = maxBottomConeHeightAtPos + midHeight - maxBottomHeight;
                                    //double waterHeightReductionNoise = this.horizontalConeSkewNoise.eval(realX / (maxConeCoordinateSkewValue * 1.5), y / (maxConeCoordinateSkewValue * 1.5), realZ / (maxConeCoordinateSkewValue * 1.5), 3, 0.5);
                                    double waterHeightReductionNoise = this.islandWaterNoiseArray[(int) ((x * 16 + z) * 256 + y)];
                                    double topHeight;

                                    boolean water = false;
                                    //If far enough into the island to be safe with water.
                                    if (maxTopHeight - maxTopConeHeightAtPos > maxWaterHeight) {
                                        //If low enough to generate water.
                                        if (y < midHeight + maxWaterHeight) {
                                            water = true;
                                        }
                                        topHeight = midHeight + maxTopHeight - maxTopConeHeightAtPos - ((maxTopHeight - maxTopConeHeightAtPos - maxWaterHeight) / waterPercentage) * waterHeightReductionNoise;
                                    } else {
                                        topHeight = midHeight + maxTopHeight - maxTopConeHeightAtPos;
                                    }

                                    final int mid = (int) Math.floor(((topHeight) - bottomHeight) / 2 + bottomHeight);

                                    final double distance = Math.floor(((topHeight) - bottomHeight) / 2);
                                    IBlockState state = type.getMainBlock();
                                    //Bottom
                                    if (y < midHeight) {
                                        for (final Entry<MinMaxBounds, IBlockState> bounds : boundsToState.entrySet()) {
                                            if (bounds.getKey().test((float) (Math.floor(Math.abs(y - mid) + 1) / distance))) {
                                                state = bounds.getValue();
                                            }
                                        }

                                        if (bottomHeight <= y) {
                                            primer.setBlockState((int) x, (int) y, (int) z, state);
                                        }
                                    } else {//Top
                                        for (final Entry<MinMaxBounds, IBlockState> bounds : boundsToState.entrySet()) {
                                            if (bounds.getKey().test((float) (Math.floor(Math.abs(y + midHeight - mid) + 1) / distance))) {
                                                state = bounds.getValue();
                                                break;
                                            }
                                        }

                                        if (topHeight >= y) {
                                            primer.setBlockState((int) x, (int) (y), (int) z, state);
                                        } else if (water && type.generateFluid()) {
                                            primer.setBlockState((int) x, (int) (y), (int) z, type.getFluidBlock());
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public void replaceBiomeBlocks(final long seed, int chunkX, int chunkZ, ChunkPrimer primer) {
        for (int x = 0; x < 16; x++) {
            x:
            for (int z = 0; z < 16; z++) {
                final BlockPos pos = new BlockPos(chunkX * 16 + x, 0, chunkZ * 16 + z);

                final Iterator<Entry<SkyIslandData, Map<BlockPos, SkyIslandType>>> iterator = this.getIslandPositions(seed, chunkX * 16, chunkZ * 16).entrySet().iterator();

                while (iterator.hasNext()) {
                    final Entry<SkyIslandData, Map<BlockPos, SkyIslandType>> set = iterator.next();
                    final SkyIslandDataV2 data = (SkyIslandDataV2) set.getKey();
                    final double minDistance = data.getHorizontalRadius();

                    for (final Entry<BlockPos, SkyIslandType> islandPos : set.getValue().entrySet()) {
                        if (MathUtil.getDistance(pos, islandPos.getKey()) <= minDistance) {
                            final SkyIslandType type = islandPos.getValue();

                            if (type.isGenBiomeBlocks()) {
                                Biome biome = Biome.getBiome(type.getBiome());

                                if (biome != Biomes.VOID) {
                                    this.genBiomeTerrainBlocks(biome, this.rand, primer, chunkX * 16 + x, chunkZ * 16 + z, islandPos.getKey().getY(), 2.625, type);
                                }
                            }
                            continue x;
                        }
                    }
                }
            }
        }
    }

    public void genBiomeTerrainBlocks(Biome biome, Random rand, ChunkPrimer chunkPrimerIn, int x, int z, int islandMid,
                                      double noiseVal, final SkyIslandType type) {
        int i = islandMid;
        IBlockState iblockstate = biome.topBlock;
        IBlockState iblockstate1 = biome.fillerBlock;
        int j = -1;
        int k = (int) (noiseVal / 3.0D + 3.0D + rand.nextDouble() * 0.25D);
        int l = z & 15;
        int i1 = x & 15;
        BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

        for (int j1 = 255; j1 >= 0; --j1) {
            IBlockState iblockstate2 = chunkPrimerIn.getBlockState(i1, j1, l);

            if (iblockstate2.getMaterial() == Material.AIR) {
                j = -1;
            } else if (iblockstate2 == type.getMainBlock()) {
                if (j == -1) {
                    if (k <= 0) {
                        iblockstate = AIR;
                        iblockstate1 = type.getMainBlock();
                    } else if (j1 >= i - 4 && j1 <= i + 1) {
                        iblockstate = biome.topBlock;
                        iblockstate1 = biome.fillerBlock;
                    }

                    if (j1 < i && (iblockstate == null || iblockstate.getMaterial() == Material.AIR)) {
                        if (biome.getTemperature(blockpos$mutableblockpos.setPos(x, j1, z)) < 0.15F) {
                            iblockstate = ICE;
                        } else {
                            iblockstate = WATER;
                        }
                    }

                    j = k;

                    if (j1 >= i - 1) {
                        chunkPrimerIn.setBlockState(i1, j1, l, iblockstate == biome.topBlock && chunkPrimerIn.getBlockState(i1, j1 + 1, l) == Blocks.WATER.getDefaultState() ? biome.fillerBlock : iblockstate);
                    } else if (j1 < i - 7 - k) {
                        iblockstate = AIR;
                        iblockstate1 = type.getMainBlock();
                        chunkPrimerIn.setBlockState(i1, j1, l, GRAVEL);
                    } else {
                        chunkPrimerIn.setBlockState(i1, j1, l, iblockstate1);
                    }
                } else if (j > 0) {
                    --j;
                    chunkPrimerIn.setBlockState(i1, j1, l, iblockstate1);

                    if (j == 0 && iblockstate1.getBlock() == Blocks.SAND && k > 1) {
                        j = rand.nextInt(4) + Math.max(0, j1 - 63);
                        iblockstate1 = iblockstate1.getValue(BlockSand.VARIANT) == BlockSand.EnumType.RED_SAND ? RED_SANDSTONE : SANDSTONE;
                    }
                }
            }
        }
    }


    //TODO Update documentation
    @ScriptMethodDocumentation(args = "int, int, int, int, boolean", usage = "horizontal radius, top height, bottom height, count, randomTypes", notes = "Generates a SkyIslandDataV2 and returns it. "
            + "Count is the number of times to attempt to generate sky islands, randomTypes is how to use the SkyIslandTypes. "
            + "If randomTypes is set to true it will randomly choose a SkyIslandType from the list when an island is generated. "
            + "If it is set to false, then every time an island is generated it will use the next SkyIslandType in the list. This allows you to guarantee certain islands are generated in a region.")
    public SkyIslandDataV2 addSkyIslands(final int horizontalRadius, final int topHeight, final int bottomHeight, final int count, final boolean randomTypes) throws Exception {
        if (topHeight + bottomHeight > 256) {
            throw new Exception("Top height and bottom height cannot add to more than 256.");
        }
        final SkyIslandDataV2 data = new SkyIslandDataV2();
        data.setHorizontalRadius(horizontalRadius);
        data.setTopHeight(topHeight);
        data.setBottomHeight(bottomHeight);
        data.setCount(count);
        data.setRandomTypes(randomTypes);

        this.SkyIslandDataV2.add(data);

        return data;
    }

    //TODO Update documentation
    @ScriptMethodDocumentation(args = "int, int, int, int, boolean, int", usage = "horizontal radius, top height, bottom height, count, randomTypes, minCount", notes = "Generates a SkyIslandDataV2 and returns it. "
            + "Count is the number of times to attempt to generate sky islands, randomTypes is how to use the SkyIslandTypes, minCount is the minimum number of the sky islands which must be generated. "
            + "If randomTypes is set to true it will randomly choose a SkyIslandType from the list when an island is generated. "
            + "If it is set to false, then every time an island is generated it will use the next SkyIslandType in the list. This allows you to guarantee certain islands are generated in a region.")
    public SkyIslandDataV2 addSkyIslands(final int horizontalRadius, final int topHeight, final int bottomHeight, final int count, final boolean randomTypes, final int minCount) throws Exception {
        if (topHeight + bottomHeight > 256) {
            throw new Exception("Top height and bottom height cannot add to more than 256.");
        }
        final SkyIslandDataV2 data = new SkyIslandDataV2();
        data.setHorizontalRadius(horizontalRadius);
        data.setTopHeight(topHeight);
        data.setBottomHeight(bottomHeight);
        data.setCount(count);
        data.setRandomTypes(randomTypes);
        data.setMinCount(minCount);

        this.SkyIslandDataV2.add(data);

        return data;
    }

    @ScriptMethodDocumentation(usage = "", notes = "Sets the generator to use Better With Mods villages when generating villages.")
    public void enableBWMVillageCompat() {
        this.BWMVillageCompat = true;
    }

    @ScriptMethodDocumentation(usage = "", notes = "Sets the generator to use Better With Mods mineshafts when generating mineshafts.")
    public void enableBWMMineshaftCompat() {
        this.BWMMineshaftCompat = true;
    }

    @Override
    public void generateStructures(World world, int chunkX, int chunkZ, ChunkPrimer primer) {
        if (this.mineshaft == null) {
            if (this.BWMMineshaftCompat && Topography.betterWithMods) {
                this.mineshaft = BWMUtil.getMineshaft(this);
            } else {
                this.mineshaft = new SkyIslandMineshaftGenerator(this);
            }
        }
        this.mineshaft.generate(world, chunkX, chunkZ, primer);

        if (this.village == null) {
            if (this.BWMVillageCompat && Topography.betterWithMods) {
                this.village = BWMUtil.getVillage(this);
            } else {
                this.village = new SkyIslandVillageGenerator(this);
            }
        }
        this.village.generate(world, chunkX, chunkZ, primer);

        if (this.stronghold != null) {
            this.stronghold.generate(world, chunkX, chunkZ, primer);
        }
    }

    @Override
    public void populateStructures(World world, Random rand, ChunkPos chunkPos) {

        if (this.mineshaft != null && this.mineshaft.structureMap != null) {
            int i = (chunkPos.x << 4) + 8;
            int j = (chunkPos.z << 4) + 8;

            ObjectIterator<StructureStart> objectiterator = this.mineshaft.structureMap.values().iterator();

            while (objectiterator.hasNext()) {
                StructureStart structurestart = (StructureStart) objectiterator.next();

                //if (structurestart.isSizeableStructure() && structurestart.isValidForPostProcess(chunkPos) && structurestart.getBoundingBox().intersectsWith(i, j, i + 15, j + 15)) {

                StructureBoundingBox structurebb = new StructureBoundingBox(i, j, i + 15, j + 15);
                structurestart.generateStructure(world, rand, structurebb);

                Iterator<StructureComponent> iterator = structurestart.getComponents().iterator();

                iterator:
                while (iterator.hasNext()) {
                    StructureComponent structurecomponent = iterator.next();

//                        if (structurecomponent.getBoundingBox().intersectsWith(structurebb) && !structurecomponent.addComponentParts(world, rand, structurebb))
//                        {
//                            iterator.remove();
//                        }

                    Map<SkyIslandData, Map<BlockPos, SkyIslandType>> islandPositions = null;

                    final ConfigurationManager manager = ConfigurationManager.getInstance();

                    if (manager != null) {
                        final ConfigPreset preset = manager.getPreset();

                        if (preset != null) {
                            final DimensionDefinition dimensionDef = preset.getDefinition(world.provider.getDimension());

                            if (dimensionDef != null) {
                                for (final IGenerator generator : dimensionDef.getGenerators()) {
                                    if (generator instanceof SkyIslandGeneratorV2) {
                                        final SkyIslandGeneratorV2 islandGenerator = (SkyIslandGeneratorV2) generator;

                                        islandPositions = islandGenerator.getIslandPositions(world.getSeed(),
                                                (structurecomponent.getBoundingBox().maxX
                                                        - structurecomponent.getBoundingBox().minX) / 2
                                                        + structurecomponent.getBoundingBox().minX,
                                                (structurecomponent.getBoundingBox().maxZ
                                                        - structurecomponent.getBoundingBox().minZ) / 2
                                                        + structurecomponent.getBoundingBox().minZ);
                                        break;
                                    }
                                }

                                if (islandPositions != null) {

                                    BlockPos componentMid = new BlockPos(
                                            (structurecomponent.getBoundingBox().maxX
                                                    - structurecomponent.getBoundingBox().minX) / 2
                                                    + structurecomponent.getBoundingBox().minX,
                                            0,
                                            (structurecomponent.getBoundingBox().maxZ
                                                    - structurecomponent.getBoundingBox().minZ) / 2
                                                    + structurecomponent.getBoundingBox().minZ);

                                    for (Entry<SkyIslandData, Map<BlockPos, SkyIslandType>> set : islandPositions
                                            .entrySet()) {
                                        for (Entry<BlockPos, SkyIslandType> innerSet : set.getValue().entrySet()) {
                                            if (set.getKey() instanceof SkyIslandDataV2) {
                                                SkyIslandDataV2 data = (SkyIslandDataV2) set.getKey();
                                                double distance = MathUtil.getDistance(componentMid, innerSet.getKey());

                                                if (distance < data.getHorizontalRadius() * 0.35) {
                                                    // continue iterator loop
                                                    continue iterator;
                                                }
                                            }
                                        }
                                    }
                                    iterator.remove();// Calls if iterator loop isn't continued
                                }
                            }
                        }
                    }
                }
                //}
            }
            this.mineshaft.generateStructure(world, rand, chunkPos);
        }


        if (this.village != null && this.village.structureMap != null) {
            int i = (chunkPos.x << 4) + 8;
            int j = (chunkPos.z << 4) + 8;

            ObjectIterator<StructureStart> objectiterator = this.village.structureMap.values().iterator();

            while (objectiterator.hasNext()) {
                StructureStart structurestart = (StructureStart) objectiterator.next();

                //if (structurestart.isSizeableStructure() && structurestart.isValidForPostProcess(chunkPos) && structurestart.getBoundingBox().intersectsWith(i, j, i + 15, j + 15)) {

                StructureBoundingBox structurebb = new StructureBoundingBox(i, j, i + 15, j + 15);
                structurestart.generateStructure(world, rand, structurebb);

                Iterator<StructureComponent> iterator = structurestart.getComponents().iterator();

                iterator:
                while (iterator.hasNext()) {
                    StructureComponent structurecomponent = iterator.next();

//                        if (structurecomponent.getBoundingBox().intersectsWith(structurebb) && !structurecomponent.addComponentParts(world, rand, structurebb))
//                        {
//                            iterator.remove();
//                        }

                    Map<SkyIslandData, Map<BlockPos, SkyIslandType>> islandPositions = null;

                    final ConfigurationManager manager = ConfigurationManager.getInstance();

                    if (manager != null) {
                        final ConfigPreset preset = manager.getPreset();

                        if (preset != null) {
                            final DimensionDefinition dimensionDef = preset.getDefinition(world.provider.getDimension());

                            if (dimensionDef != null) {
                                for (final IGenerator generator : dimensionDef.getGenerators()) {
                                    if (generator instanceof SkyIslandGeneratorV2) {
                                        final SkyIslandGeneratorV2 islandGenerator = (SkyIslandGeneratorV2) generator;

                                        islandPositions = islandGenerator.getIslandPositions(world.getSeed(),
                                                (structurecomponent.getBoundingBox().maxX
                                                        - structurecomponent.getBoundingBox().minX) / 2
                                                        + structurecomponent.getBoundingBox().minX,
                                                (structurecomponent.getBoundingBox().maxZ
                                                        - structurecomponent.getBoundingBox().minZ) / 2
                                                        + structurecomponent.getBoundingBox().minZ);
                                        break;
                                    }
                                }

                                if (islandPositions != null) {

                                    BlockPos componentMid = new BlockPos(
                                            (structurecomponent.getBoundingBox().maxX
                                                    - structurecomponent.getBoundingBox().minX) / 2
                                                    + structurecomponent.getBoundingBox().minX,
                                            0,
                                            (structurecomponent.getBoundingBox().maxZ
                                                    - structurecomponent.getBoundingBox().minZ) / 2
                                                    + structurecomponent.getBoundingBox().minZ);

                                    for (Entry<SkyIslandData, Map<BlockPos, SkyIslandType>> set : islandPositions
                                            .entrySet()) {
                                        for (Entry<BlockPos, SkyIslandType> innerSet : set.getValue().entrySet()) {
                                            if (set.getKey() instanceof SkyIslandDataV2) {
                                                SkyIslandDataV2 data = (SkyIslandDataV2) set.getKey();
                                                double distance = MathUtil.getDistance(componentMid, innerSet.getKey());

                                                if (distance < data.getHorizontalRadius() * 0.5) {
                                                    // continue iterator loop
                                                    continue iterator;
                                                }
                                            }
                                        }
                                    }
                                    iterator.remove();// Calls if iterator loop isn't continued
                                }
                            }
                        }
                    }
                }
                //}
            }
            this.village.generateStructure(world, rand, chunkPos);
        }
        if (this.stronghold != null) {
            this.stronghold.generateStructure(world, rand, chunkPos);
        }
    }

    @Override
    public BlockPos getNearestStructurePos(World worldIn, String structureName, BlockPos position, boolean findUnexplored) {
        switch (structureName) {
            case "Stronghold": {
                return this.stronghold != null ? this.stronghold.getNearestStructurePos(worldIn, position, findUnexplored) : null;
            }
            case "Village": {
                return this.village != null ? this.village.getNearestStructurePos(worldIn, position, findUnexplored) : null;
            }
            case "Mineshaft": {
                return this.mineshaft != null ? this.mineshaft.getNearestStructurePos(worldIn, position, findUnexplored) : null;
            }
            default: {
                return null;
            }
        }
    }

    @Override
    public boolean isInsideStructure(World worldIn, String structureName, BlockPos pos) {
        switch (structureName) {
            case "Stronghold": {
                return this.stronghold != null ? this.stronghold.isInsideStructure(pos) : false;
            }
            case "Village": {
                return this.village != null ? this.village.isInsideStructure(pos) : false;
            }
            case "Mineshaft": {
                return this.mineshaft != null ? this.mineshaft.isInsideStructure(pos) : false;
            }
            default: {
                return false;
            }
        }
    }

    @Override
    public void recreateStructures(World world, Chunk chunkIn, int x, int z) {
        if (this.mineshaft != null) {
            this.mineshaft.generate(world, x, z, (ChunkPrimer) null);
        }

        if (this.village != null) {
            this.village.generate(world, x, z, (ChunkPrimer) null);
        }

        if (this.stronghold != null) {
            this.stronghold.generate(world, x, z, (ChunkPrimer) null);
        }
    }

    protected final List<SkyIslandDataV2> SkyIslandDataV2 = new ArrayList<SkyIslandDataV2>();
    private Map<SkyIslandData, Map<BlockPos, SkyIslandType>> islandPositions = new LinkedHashMap<SkyIslandData, Map<BlockPos, SkyIslandType>>();
    private final Random islandPositionRandom = new Random();
    private double regionSize = 464;

    private int currentRegionX = -100000000;
    private int currentRegionZ = -100000000;


    private void generateIslandPositions(final long worldSeed) {
        this.islandPositionRandom.setSeed((long) (this.currentRegionX) * 341873128712L
                + (long) (this.currentRegionZ) * 132897987541L + worldSeed);
        Random extendRandom = new Random((long) (this.currentRegionX) * 341873128712L
                + (long) (this.currentRegionZ) * 132897987541L + worldSeed);
        this.islandPositions = new LinkedHashMap<SkyIslandData, Map<BlockPos, SkyIslandType>>();
        for (SkyIslandDataV2 data : this.SkyIslandDataV2) {
            int genCount = 0;
            for (int i = 0; i < data.getCount() || (genCount < data.getMinCount() && i < data.getCount() * 2); i++) {
//                long subSeed = rand.nextLong();
                if (data instanceof SkyIslandDataV2AutoExtend) {
                    data = ((SkyIslandDataV2AutoExtend)data).generate(extendRandom);
                }

                final double maxHorizontalFeatureRadius = data.getHorizontalRadius();

                final int minMidHeight = (int) Math.max(data.getMinHeight(), data.getBottomHeight());
                final int maxMidHeight = (int) Math.min(data.getMaxHeight(), 256 - data.getTopHeight());
                final int randomVerticalSpace = Math.max(maxMidHeight - minMidHeight, 1);
                final double midHeight = this.islandPositionRandom.nextInt(randomVerticalSpace) + minMidHeight;

                final int regionCenterX = (int) ((this.currentRegionX) * regionSize + regionSize / 2);
                final int regionCenterZ = (int) ((this.currentRegionZ) * regionSize + regionSize / 2);

                final int randomSpace = (int) (regionSize - maxHorizontalFeatureRadius * 2);

                final int featureCenterX = this.islandPositionRandom.nextInt(randomSpace) - randomSpace / 2 + regionCenterX;
                final int featureCenterZ = this.islandPositionRandom.nextInt(randomSpace) - randomSpace / 2 + regionCenterZ;

                final BlockPos pos = new BlockPos(featureCenterX, midHeight, featureCenterZ);

                if (this.islandPostionAcceptable(pos, maxHorizontalFeatureRadius)) {

                    if (!this.islandPositions.containsKey(data)) {
                        this.islandPositions.put(data, new LinkedHashMap<BlockPos, SkyIslandType>());
                    }
                    if (data.isRandomIslands()) {
                        final Map<BlockPos, SkyIslandType> positions = this.islandPositions.get(data);
                        positions.put(pos, data.getType(this.islandPositionRandom.nextInt(128)));
                    } else {
                        final Map<BlockPos, SkyIslandType> positions = this.islandPositions.get(data);
                        positions.put(pos, data.getType(genCount));
                    }
                    genCount++;
                }
            }
        }
    }

    private boolean islandPostionAcceptable(BlockPos pos, double maxHorizontalFeatureRadius) {
        Iterator<Entry<SkyIslandData, Map<BlockPos, SkyIslandType>>> setIterator = this.islandPositions.entrySet().iterator();

        while (setIterator.hasNext()) {
            final Entry<SkyIslandData, Map<BlockPos, SkyIslandType>> set = setIterator.next();
            final SkyIslandDataV2 islandData = (com.bloodnbonesgaming.topography.config.SkyIslandDataV2) set.getKey();

            final double minDistance = islandData.getHorizontalRadius() + maxHorizontalFeatureRadius + 16;

            for (final Entry<BlockPos, SkyIslandType> islandPos : set.getValue().entrySet()) {
                if (SkyIslandGenerator.getDistance(pos, islandPos.getKey()) < minDistance) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public Map<SkyIslandData, Map<BlockPos, SkyIslandType>> getIslandPositions(final long worldSeed, final int x, final int z) {
        if (((int) Math.floor(Math.floor(x / 16.0D) * 16D / this.regionSize)) != this.currentRegionX || ((int) Math.floor(Math.floor(z / 16.0D) * 16D / this.regionSize)) != this.currentRegionZ) {
            this.currentRegionX = ((int) Math.floor(Math.floor(x / 16.0D) * 16D / this.regionSize));
            this.currentRegionZ = ((int) Math.floor(Math.floor(z / 16.0D) * 16D / this.regionSize));
            this.generateIslandPositions(worldSeed);
        }
        //return this.islandPositions;
        //return new LinkedHashMap<SkyIslandData, Map<BlockPos, SkyIslandType>>(this.islandPositions);

        //Clone map to fix concurrency issues
        LinkedHashMap<SkyIslandData, Map<BlockPos, SkyIslandType>> positions = new LinkedHashMap<SkyIslandData, Map<BlockPos, SkyIslandType>>();

        for (Entry<SkyIslandData, Map<BlockPos, SkyIslandType>> entry : this.islandPositions.entrySet()) {
            positions.put(entry.getKey(), entry.getValue());
        }
        return positions;
    }

    public static double getDistance(final BlockPos pos, final BlockPos pos2) {
        double d0 = pos.getX() - pos2.getX();
        double d2 = pos.getZ() - pos2.getZ();
        return Math.sqrt(d0 * d0 + d2 * d2);
    }

    public int getRegionSize() {
        return (int) this.regionSize;
    }

    @ScriptMethodDocumentation(args = "int", usage = "size", notes = "Sets the grid region size in chunks. Default is 29.")
    public void setRegionSize(final int size) {
        this.regionSize = size * 16;
    }

    //TODO Update documentation
    @ScriptMethodDocumentation(args = "int, int, boolean", usage = "radius, count, randomTypes", notes = "Generates a SkyIslandDataV2 and returns it. "
            + "Radius is the radius of the sky islands to be generated, count is the number of times to attempt to generate sky islands, randomTypes is how to use the SkyIslandTypes. "
            + "If randomTypes is set to true it will randomly choose a SkyIslandType from the list when an island is generated. "
            + "If it is set to false, then every time an island is generated it will use the next SkyIslandType in the list. This allows you to guarantee certain islands are generated in a region.")
    public SkyIslandDataV2 addSkyIslands(final int radius, final int count, final boolean randomTypes) {
        final SkyIslandDataV2 data = new SkyIslandDataV2();
        data.setHorizontalRadius(radius);
        data.setVerticalRadius(radius);
        data.setCount(count);
        data.setRandomTypes(randomTypes);

        this.SkyIslandDataV2.add(data);

        return data;
    }

    //TODO Update documentation
    @ScriptMethodDocumentation(args = "int, int, boolean, int", usage = "radius, count, randomTypes, minCount", notes = "Generates a SkyIslandDataV2 and returns it. "
            + "Radius is the radius of the sky islands to be generated, count is the number of times to attempt to generate sky islands, randomTypes is how to use the SkyIslandTypes, minCount is the minimum number of the sky islands which must be generated. "
            + "If randomTypes is set to true it will randomly choose a SkyIslandType from the list when an island is generated. "
            + "If it is set to false, then every time an island is generated it will use the next SkyIslandType in the list. This allows you to guarantee certain islands are generated in a region.")
    public SkyIslandDataV2 addSkyIslands(final int radius, final int count, final boolean randomTypes, final int minCount) {
        final SkyIslandDataV2 data = new SkyIslandDataV2();
        data.setHorizontalRadius(radius);
        data.setVerticalRadius(radius);
        data.setCount(count);
        data.setRandomTypes(randomTypes);
        data.setMinCount(minCount);

        this.SkyIslandDataV2.add(data);

        return data;
    }


    @ScriptMethodDocumentation(args = "int,int,  int", usage = "radius, count, minCount", notes = "Generates a Millenaire SkyIslandDataV2 and returns it. "
            + "Radius is the default radius of the sky islands to be generated, count is the number of times to attempt to generate sky islands, minCount is the minimum number of the sky islands which must be generated. ")
    public SkyIslandDataV2 addMillenaireSkyIslands(final int radius ,final int count ,final int minCount) {
        final SkyIslandDataV2MillenaireVillage data = new SkyIslandDataV2MillenaireVillage();
        data.setHorizontalRadius(radius);
        data.setVerticalRadius(radius);
        data.setCount(count);
        data.setRandomTypes(true);
        data.setMinCount(minCount);

        this.SkyIslandDataV2.add(data);

        return data;
    }

    //ChunkGenerator
    final Random rand = new Random();

    protected OpenSimplexNoiseGeneratorOctaves terrainNoise = null;
    ;
    final Random mountainRand = new Random();


    protected NoiseGeneratorPerlin surfaceNoise = new NoiseGeneratorPerlin(this.rand, 4);
    protected double[] depthBuffer = new double[256];


    protected void generateNoise(final double[] array, final int arraySizeX, final int arraySizeY, final int arraySizeZ, final int x, final int y, final int z, final int xCoordinateScale, final int yCoordinateScale, final int zCoordinateScale) {
        for (int xI = 0; xI < arraySizeX; xI++) {
            for (int zI = 0; zI < arraySizeZ; zI++) {
                for (int yI = 0; yI < arraySizeY; yI++) {
                    int index = (xI * arraySizeX + zI) * arraySizeY + yI;
                    double noise = this.terrainNoise.eval((x + xI * xCoordinateScale) / 128.0, (y + yI * yCoordinateScale) / 32.0, (z + zI * zCoordinateScale) / 128.0, 3, 0.5);

                    array[index] = noise;
                }
            }
        }
    }

    public void flowWaterVertical(ChunkPrimer primer) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                boolean water = false;

                for (int y = 255; y >= 0; y--) {
                    IBlockState state = primer.getBlockState(x, y, z);

                    if (state == Blocks.WATER.getDefaultState()) {
                        water = true;
                    } else if (water == true && state == Blocks.AIR.getDefaultState()) {
                        primer.setBlockState(x, y, z, Blocks.WATER.getDefaultState().withProperty(BlockLiquid.LEVEL, 8));
                        water = true;
                    } else {
                        water = false;
                    }
                }
            }
        }
    }

    protected static final IBlockState AIR = Blocks.AIR.getDefaultState();
    protected static final IBlockState GRAVEL = Blocks.GRAVEL.getDefaultState();
    protected static final IBlockState RED_SANDSTONE = Blocks.RED_SANDSTONE.getDefaultState();
    protected static final IBlockState SANDSTONE = Blocks.SANDSTONE.getDefaultState();
    protected static final IBlockState ICE = Blocks.ICE.getDefaultState();
    protected static final IBlockState WATER = Blocks.WATER.getDefaultState();

    public void genBiomeTerrainBlocks(Biome biome, Random rand, ChunkPrimer chunkPrimerIn, int x, int z, int islandMid, double noiseVal) {
        int i = islandMid;
        IBlockState iblockstate = biome.topBlock;
        IBlockState iblockstate1 = biome.fillerBlock;
        int j = -1;
        int k = (int) (noiseVal / 3.0D + 3.0D + rand.nextDouble() * 0.25D);
        int l = z & 15;
        int i1 = x & 15;
        BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

        for (int j1 = 255; j1 >= 0; --j1) {
            IBlockState iblockstate2 = chunkPrimerIn.getBlockState(i1, j1, l);

            if (iblockstate2.getMaterial() == Material.AIR) {
                j = -1;
            } else if (iblockstate2 == Blocks.STONE.getDefaultState()) {
                if (j == -1) {
                    if (k <= 0) {
                        iblockstate = AIR;
                        iblockstate1 = Blocks.STONE.getDefaultState();
                    } else if (j1 >= i - 4 && j1 <= i + 1) {
                        iblockstate = biome.topBlock;
                        iblockstate1 = biome.fillerBlock;
                    }

                    if (j1 < i && (iblockstate == null || iblockstate.getMaterial() == Material.AIR)) {
                        if (biome.getTemperature(blockpos$mutableblockpos.setPos(x, j1, z)) < 0.15F) {
                            iblockstate = ICE;
                        } else {
                            iblockstate = WATER;
                        }
                    }

                    j = k;

                    if (j1 >= i - 1) {
                        chunkPrimerIn.setBlockState(i1, j1, l, iblockstate);
                    } else if (j1 < i - 7 - k) {
                        iblockstate = AIR;
                        iblockstate1 = Blocks.STONE.getDefaultState();
                        chunkPrimerIn.setBlockState(i1, j1, l, GRAVEL);
                    } else {
                        chunkPrimerIn.setBlockState(i1, j1, l, iblockstate1);
                    }
                } else if (j > 0) {
                    --j;
                    chunkPrimerIn.setBlockState(i1, j1, l, iblockstate1);

                    if (j == 0 && iblockstate1.getBlock() == Blocks.SAND && k > 1) {
                        j = rand.nextInt(4) + Math.max(0, j1 - 63);
                        iblockstate1 = iblockstate1.getValue(BlockSand.VARIANT) == BlockSand.EnumType.RED_SAND ? RED_SANDSTONE : SANDSTONE;
                    }
                }
            }
        }
    }

    private void genDecorations(final long seed, final int chunkX, final int chunkZ, final ChunkPrimer primer) {
        final BlockPos pos = new BlockPos(chunkX * 16, 0, chunkZ * 16);

        outer:
        for (final Entry<SkyIslandData, Map<BlockPos, SkyIslandType>> set : this.getIslandPositions(seed, chunkX * 16, chunkZ * 16).entrySet()) {
            final SkyIslandDataV2 data = (SkyIslandDataV2) set.getKey();
            final double minDistance = data.getHorizontalRadius();

            for (final Entry<BlockPos, SkyIslandType> islandPos : set.getValue().entrySet()) {
                //if (SkyIslandGenerator.getDistance(pos, islandPos.getKey()) < minDistance + 16) {
                if (this.canBeInChunk(islandPos.getKey(), data.getHorizontalRadius(), chunkX * 16, chunkZ * 16)) {
                    final SkyIslandType type = islandPos.getValue();

                    for (final DecorationData decoration : type.getDecorators()) {
                        decoration.generateForSkyIsland(seed, chunkX, chunkZ, primer, islandPos.getKey(), data, type, this);
                    }
                    break outer;
                }
            }
        }
    }

    @Override
    public void populate(World world, int chunkX, int chunkZ, Random rand) {
        final long seed = world.getSeed();
        BlockFalling.fallInstantly = true;
        int i = chunkX * 16;
        int j = chunkZ * 16;
        BlockPos blockpos = new BlockPos(i, 0, j);
//        Biome biome = world.getBiome(blockpos.add(16, 0, 16));
        this.rand.setSeed(seed);
        long k = this.rand.nextLong() / 2L * 2L + 1L;
        long l = this.rand.nextLong() / 2L * 2L + 1L;
        this.rand.setSeed((long) chunkX * k + (long) chunkZ * l ^ seed);
//        boolean flag = false;

        final BlockPos pos = new BlockPos(i, 0, j);

//        if (biome == Biomes.RIVER)
//        {
//        	biome.decorate(world, this.rand, new BlockPos(i, 0, j));
//        }
//        else
//        {
        outer:
        for (final Entry<SkyIslandData, Map<BlockPos, SkyIslandType>> set : this.getIslandPositions(seed, i, j)
                .entrySet()) {
            final SkyIslandDataV2 data = (SkyIslandDataV2) set.getKey();
            final double minDistance = data.getHorizontalRadius();

            for (final Entry<BlockPos, SkyIslandType> islandPos : set.getValue().entrySet()) {
                //if (MathUtil.getDistance(pos, islandPos.getKey()) < minDistance + 16) {
                if (this.canBeInChunk(islandPos.getKey(), data.getHorizontalRadius(), i, j)) {
                    final SkyIslandType type = islandPos.getValue();
                    Biome typeBiome = Biome.getBiome(type.getBiome());

                    if (type.isGenDecorations()) {
                        if (typeBiome != Biomes.VOID) {
                            typeBiome.decorate(world, this.rand, new BlockPos(i, 0, j));
                        }
                    }
                    if (type.genAnimals()) {
                        WorldEntitySpawner.performWorldGenSpawning(world, typeBiome, i + 8, j + 8, 16, 16, this.rand);
                    }
                    break outer;
                }
            }
        }
//        }

        blockpos = blockpos.add(8, 0, 8);

        {
            for (int k2 = 0; k2 < 16; ++k2) {
                for (int j3 = 0; j3 < 16; ++j3) {
                    BlockPos blockpos1 = world.getPrecipitationHeight(blockpos.add(k2, 0, j3));
                    BlockPos blockpos2 = blockpos1.down();

                    if (world.canBlockFreezeWater(blockpos2)) {
                        world.setBlockState(blockpos2, Blocks.ICE.getDefaultState(), 2);
                    }

                    if (world.canSnowAt(blockpos1, true)) {
                        world.setBlockState(blockpos1, Blocks.SNOW_LAYER.getDefaultState(), 2);
                    }
                }
            }
        }

        this.genGenerators(world, this.rand, chunkX, chunkZ, seed);

        BlockFalling.fallInstantly = false;
    }

    private void genGenerators(final World world, final Random random, final int chunkX, final int chunkZ, final long seed) {
        int x = chunkX * 16;
        int z = chunkZ * 16;

        final BlockPos pos = new BlockPos(x, 0, z);

        outer:
        for (final Entry<SkyIslandData, Map<BlockPos, SkyIslandType>> set : this.getIslandPositions(seed, x, z)
                .entrySet()) {
            final SkyIslandDataV2 data = (SkyIslandDataV2) set.getKey();
            final double minDistance = data.getHorizontalRadius();

            for (final Entry<BlockPos, SkyIslandType> islandPos : set.getValue().entrySet()) {
//				if (MathUtil.getDistance(pos, islandPos.getKey()) < minDistance + 16) {
                if (this.canBeInChunk(islandPos.getKey(), data.getHorizontalRadius(), x, z)) {
                    final SkyIslandType type = islandPos.getValue();

                    for (IGenerator generator : type.getGenerators()) {
                        generator.populate(world, chunkX, chunkZ, this.rand);
                    }
                    break outer;
                }
            }
        }
    }

    private boolean canBeInChunk(BlockPos center, double radius, final int chunkMinX, final int chunkMinZ) {
        if (center.getX() - radius > chunkMinX + 16) {//If outside chunk in +X direction, and radius isn't large enough to get into chunk
            return false;
        }
        if (center.getX() + radius < chunkMinX) {//If outside chunk in -X direction, and radius isn't large enough to get into chunk
            return false;
        }
        if (center.getZ() - radius > chunkMinZ + 16) {//If outside chunk in +Z direction, and radius isn't large enough to get into chunk
            return false;
        }
        if (center.getZ() + radius < chunkMinZ) {//If outside chunk in -Z direction, and radius isn't large enough to get into chunk
            return false;
        }
        return true;
    }

    @Override
    public GenLayer getLayer(World world, GenLayer parent) {
//    	GenLayer river = new GenLayerRiverInit(1001, parent);
//        river = GenLayerZoom.magnify(1000L, river, 4); //Raise to reduce frequency
//        river = new GenLayerRiver(1L, river);
//        river = GenLayerZoom.magnify(1000L, river, 3); //Lower to reduce size
//        river = new GenLayerSmooth(1000L, river);
//        river = GenLayerZoom.magnify(1000L, river, 1);
        GenLayer biomes = new GenLayerBiomeSkyIslands(world.getSeed(), this);
//        GenLayerRiverMixDC mix = new GenLayerRiverMixDC(1000L, biomes, river);
//        mix.setRiverBiome(Biome.getIdForBiome(Biomes.VOID), Biome.getIdForBiome(Biomes.VOID));
        return biomes;
    }
}
