package com.bloodnbonesgaming.topography.world.generator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import com.bloodnbonesgaming.lib.util.NumberHelper;
import com.bloodnbonesgaming.lib.util.noise.OpenSimplexNoiseGeneratorOctaves;
import com.bloodnbonesgaming.topography.config.SkyIslandData;
import com.bloodnbonesgaming.topography.config.SkyIslandType;
import com.bloodnbonesgaming.topography.util.MathUtil;
import com.bloodnbonesgaming.topography.world.decorator.DecorationData;
import com.bloodnbonesgaming.topography.world.layer.GenLayerBiomeSkyIslands;

import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.block.BlockFalling;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.BlockSand;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldEntitySpawner;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.NoiseGeneratorPerlin;
import net.minecraft.world.gen.layer.GenLayer;

public class SkyIslandGenerator implements IGenerator
{
//	public Biome[] biomesForGeneration;
	
    @Override
    public void generate(final World world, ChunkPrimer primer, int chunkX, int chunkZ, final Random random)
    {
        final long seed = world.getSeed();
        this.terrainNoise = new OpenSimplexNoiseGeneratorOctaves(seed);
        
        this.rand.setSeed((long)chunkX * 341873128712L + (long)chunkZ * 132897987541L);

        this.mountainRand.setSeed((long)((int)Math.floor(chunkX * 16D / this.getRegionSize())) * 341873128712L + (long)((int)Math.floor(chunkZ * 16D / this.getRegionSize())) * 132897987541L + seed);
        
//        this.biomesForGeneration = world.getBiomeProvider().getBiomes(this.biomesForGeneration, chunkX * 16, chunkZ * 16, 16, 16);
        
        this.generateIslands(seed, chunkX, chunkZ, primer);
//        this.flowWaterVertical(primer);
        
        this.replaceBiomeBlocks(seed, chunkX, chunkZ, primer);
                
        this.genDecorations(seed, chunkX, chunkZ, primer);

        
    }

    private final List<SkyIslandData> skyIslandData = new ArrayList<SkyIslandData>();
    private Map<SkyIslandData, Map<BlockPos, SkyIslandType>> islandPositions = new LinkedHashMap<SkyIslandData, Map<BlockPos, SkyIslandType>>();
    private final Random islandPositionRandom = new Random();
    private double regionSize = 464;

    private int currentRegionX = -100000000;
    private int currentRegionZ = -100000000;
    

    private void generateIslandPositions(final long worldSeed)
    {
        this.islandPositionRandom.setSeed((long) (this.currentRegionX) * 341873128712L
                + (long) (this.currentRegionZ) * 132897987541L + worldSeed);
        this.islandPositions = new LinkedHashMap<SkyIslandData, Map<BlockPos, SkyIslandType>>();
        for (final SkyIslandData data : this.skyIslandData)
        {
            int genCount = 0;
            countLoop: for (int i = 0; i < data.getCount() || genCount < data.getMinCount(); i++)
            {
                final double maxFeatureRadius = data.getRadius();
                final double midHeight = Math.min(maxFeatureRadius, 110) + this.islandPositionRandom.nextInt((int) Math.max(220 - (maxFeatureRadius * 2), 1));

                final int regionCenterX = (int) ((this.currentRegionX) * regionSize + regionSize / 2);
                final int regionCenterZ = (int) ((this.currentRegionZ) * regionSize + regionSize / 2);

                final int randomSpace = (int) (regionSize - maxFeatureRadius * 2);

                final int featureCenterX = this.islandPositionRandom.nextInt(randomSpace) - randomSpace / 2 + regionCenterX;
                final int featureCenterZ = this.islandPositionRandom.nextInt(randomSpace) - randomSpace / 2 + regionCenterZ;

                final BlockPos pos = new BlockPos(featureCenterX, midHeight, featureCenterZ);

                for (final Entry<SkyIslandData, Map<BlockPos, SkyIslandType>> set : this.islandPositions.entrySet())
                {
                    final double minDistance = set.getKey().getRadius() + maxFeatureRadius + 25;

                    for (final Entry<BlockPos, SkyIslandType> islandPos : set.getValue().entrySet())
                    {
                        if (SkyIslandGenerator.getDistance(pos, islandPos.getKey()) < minDistance)
                        {
                            continue countLoop;
                        }
                    }
                }

                if (!this.islandPositions.containsKey(data))
                {
                    this.islandPositions.put(data, new LinkedHashMap<BlockPos, SkyIslandType>());
                }
                if (data.isRandomIslands())
                {
                    final Map<BlockPos, SkyIslandType> positions = this.islandPositions.get(data);
                    positions.put(pos, data.getType(this.islandPositionRandom.nextInt(128)));
                }
                else
                {
                    final Map<BlockPos, SkyIslandType> positions = this.islandPositions.get(data);
                    positions.put(pos, data.getType(genCount));
                }
                genCount++;
            }
        }
    }

    public Map<SkyIslandData, Map<BlockPos, SkyIslandType>> getIslandPositions(final long worldSeed, final int x, final int z)
    {
        if (((int) Math.floor(Math.floor(x / 16.0D) * 16D / this.regionSize)) != this.currentRegionX || ((int) Math.floor(Math.floor(z / 16.0D) * 16D / this.regionSize)) != this.currentRegionZ)
        {
            this.currentRegionX = ((int) Math.floor(Math.floor(x / 16.0D) * 16D / this.regionSize));
            this.currentRegionZ = ((int) Math.floor(Math.floor(z / 16.0D) * 16D / this.regionSize));
            this.generateIslandPositions(worldSeed);
        }
        return this.islandPositions;
    }

    public static double getDistance(final BlockPos pos, final BlockPos pos2)
    {
        double d0 = pos.getX() - pos2.getX();
        double d2 = pos.getZ() - pos2.getZ();
        return Math.sqrt(d0 * d0 + d2 * d2);
    }

    public int getRegionSize()
    {
        return (int) this.regionSize;
    }

	public void setRegionSize(final int size)
    {
        this.regionSize = size * 16;
    }

    public SkyIslandData addSkyIslands(final int radius, final int count, final boolean randomTypes)
    {
        final SkyIslandData data = new SkyIslandData();
        data.setRadius(radius);
        data.setCount(count);
        data.setRandomTypes(randomTypes);

        this.skyIslandData.add(data);

        return data;
    }

    public SkyIslandData addSkyIslands(final int radius, final int count, final boolean randomTypes, final int minCount)
    {
        final SkyIslandData data = new SkyIslandData();
        data.setRadius(radius);
        data.setCount(count);
        data.setRandomTypes(randomTypes);
        data.setMinCount(minCount);

        this.skyIslandData.add(data);

        return data;
    }
    
    
    
    
    
    
    //ChunkGenerator
    final Random rand = new Random();
    
    protected OpenSimplexNoiseGeneratorOctaves terrainNoise;
    final Random mountainRand = new Random();
    double[] smallNoiseArray = new double[825];
    double[] largeNoiseArray = new double[65536];
    

    protected NoiseGeneratorPerlin surfaceNoise = new NoiseGeneratorPerlin(this.rand, 4);
    protected double[] depthBuffer = new double[256];
    
    
    private void generateNoise(final double[] array, final int arraySizeX, final int arraySizeY, final int arraySizeZ, final int x, final int y, final int z, final int xCoordinateScale, final int yCoordinateScale, final int zCoordinateScale)
    {
        for (int xI = 0; xI < arraySizeX; xI++)
        {
            for (int zI = 0; zI < arraySizeZ; zI++)
            {
                for (int yI = 0; yI < arraySizeY; yI++)
                {
                    int index = (xI * arraySizeX + zI) * arraySizeY + yI;
                    double noise = this.terrainNoise.eval((x + xI * xCoordinateScale) / 128.0, (y + yI * yCoordinateScale) / 32.0, (z + zI * zCoordinateScale) / 128.0, 3, 0.5);
                    
                    array[index] = noise;
                }
            }
        }
    }
    
    public void flowWaterVertical(ChunkPrimer primer)
    {
    	for (int x = 0; x < 16; x++)
    	{
    		for (int z = 0; z < 16; z++)
    		{
    			boolean water = false;
    			
    			for (int y = 255; y >= 0; y--)
    			{
    				IBlockState state = primer.getBlockState(x, y, z);
    				
    				if (state == Blocks.WATER.getDefaultState())
    				{
    					water = true;
    				}
    				else if (water == true && state == Blocks.AIR.getDefaultState())
    				{
    					primer.setBlockState(x, y, z, Blocks.WATER.getDefaultState().withProperty(BlockLiquid.LEVEL, 8));
    					water = true;
    				}
    				else
    				{
    					water = false;
    				}
    			}
    		}
    	}
    }
    
    public void generateIslands(final long seed, final int chunkX, final int chunkZ, final ChunkPrimer primer)
    {
        this.generateNoise(this.smallNoiseArray, 5, 33, 5, chunkX * 16, 0, chunkZ * 16, 4, 8, 4);
        NumberHelper.interpolate(this.smallNoiseArray, this.largeNoiseArray, 5, 33, 5, 4, 8, 4);
        
//        FastNoise noise = new FastNoise();
//		noise.SetNoiseType(FastNoise.NoiseType.Cellular);
//        noise.SetFrequency(0.01f);
//        noise.SetCellularDistanceFunction(FastNoise.CellularDistanceFunction.Natural);
//        noise.SetCellularReturnType(FastNoise.CellularReturnType.Distance3Div);
//        noise.SetSeed((int) seed);
        
        final Iterator<Entry<SkyIslandData, Map<BlockPos, SkyIslandType>>> iterator = this.getIslandPositions(seed, chunkX * 16, chunkZ * 16).entrySet().iterator();
        
        while (iterator.hasNext())
        {
            final Entry<SkyIslandData, Map<BlockPos, SkyIslandType>> entry = iterator.next();
            final SkyIslandData data = entry.getKey();
            final int chunkBlockX = chunkX * 16;
            final int chunkBlockZ = chunkZ * 16;
            
            for (final Entry<BlockPos, SkyIslandType> islandPos : entry.getValue().entrySet())
            {
                final int featureCenterX = islandPos.getKey().getX();
                final int featureCenterZ = islandPos.getKey().getZ();
                final int midHeight = islandPos.getKey().getY();
                final int maxFeatureRadius = data.getRadius();
                
                for (double x = 0; x < 16; x++)
                {
                    final double realX = x + chunkBlockX;
                    final double xDistance = Math.pow(Math.abs(featureCenterX - realX), 2);
                    
                    for (double z = 0; z < 16; z++)
                    {
                        final double realZ = z + chunkBlockZ;
                        
                        final double zDistance = Math.pow(Math.abs(featureCenterZ - realZ), 2);
                        
                        final double maxNoiseDistance = (maxFeatureRadius - Math.sqrt(xDistance + zDistance)) * 1.5;
                        
                        final double noiseDistance = maxFeatureRadius * 0.32;
                        
                        final double noise2 = this.terrainNoise.eval((realX) / noiseDistance, (realZ) / noiseDistance, 3, 0.5);
                        
                        if (Math.sqrt(xDistance + zDistance) <= maxFeatureRadius)
                        {
                            final SkyIslandType type = islandPos.getValue();
                            final Map<MinMaxBounds, IBlockState> boundsToState = type.getBoundsToStateMap();
//                            float river = noise.GetNoise((float) x + chunkX * 16, (float) z + chunkZ * 16);
                            
                            for (double y = 0; y < midHeight; y++)
                            {
                                final double skewNoise = this.largeNoiseArray[(int) ((x * 16 + z) * 256 + y)] * 2 - 1;
                                final double skewedNoise = this.terrainNoise.eval((realX + 16 * skewNoise) / noiseDistance, (realZ + 16 * skewNoise) / noiseDistance, 3, 0.5);
                                
                                final double bottomHeight = midHeight - skewedNoise * (maxNoiseDistance - noiseDistance * noise2);
                                final double topHeight = skewedNoise * ((maxNoiseDistance - noiseDistance * noise2) / 4.0);
                                
                                final int mid = (int) Math.floor(((topHeight + midHeight) - bottomHeight) / 2 + bottomHeight);
                                
                                final double distance = Math.floor(((topHeight + midHeight) - bottomHeight) / 2);
                                IBlockState state = type.getMainBlock();
                                //Bottom
                                {                                    
                                    for (final Entry<MinMaxBounds, IBlockState> bounds : boundsToState.entrySet())
                                    {
                                        if (bounds.getKey().test((float) (Math.floor(Math.abs(y - mid) + 1) / distance)))
                                        {
                                            state = bounds.getValue();
                                        }
                                    }
                                    
                                    if (bottomHeight < y)
                                    {
                                        primer.setBlockState((int) x, (int) y, (int) z, state);
                                    }
                                }
                                
                                //Top
                                {//                                                                        
                                    for (final Entry<MinMaxBounds, IBlockState> bounds : boundsToState.entrySet())
                                    {
                                        if (bounds.getKey().test((float) (Math.floor(Math.abs(y + midHeight - mid) + 1) / distance)))
                                        {
                                            state = bounds.getValue();
                                        }
                                    }
                                    
                                    if (topHeight > y)
                                    {
//                                    	float scaled = 1.0f / 0.3f;
//                                    	if (topHeight > y + ((1 - Math.abs(river) * scaled) * 25))
//                                    		primer.setBlockState((int) x, (int) (y + midHeight), (int) z, state);
//                                    	else if (y == 0 || y == 1)
//                                    		primer.setBlockState((int) x, (int) (y + midHeight), (int) z, Blocks.WATER.getDefaultState());
                                		primer.setBlockState((int) x, (int) (y + midHeight), (int) z, state);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            
        }
    }
    
    public void replaceBiomeBlocks(final long seed, int chunkX, int chunkZ, ChunkPrimer primer)
    {
        for (int x = 0; x < 16; x++)
        {
            x:
            for (int z = 0; z < 16; z++)
            {
                final BlockPos pos = new BlockPos(chunkX * 16 + x, 0, chunkZ * 16 + z);
                
                final Iterator<Entry<SkyIslandData, Map<BlockPos, SkyIslandType>>> iterator = this.getIslandPositions(seed, chunkX * 16, chunkZ * 16).entrySet().iterator();
                
                while (iterator.hasNext())
                {
                    final Entry<SkyIslandData, Map<BlockPos, SkyIslandType>> set = iterator.next();
                    final SkyIslandData data = set.getKey();
                    final double minDistance = data.getRadius();
                    
                    for (final Entry<BlockPos, SkyIslandType> islandPos : set.getValue().entrySet())
                    {
                        if (MathUtil.getDistance(pos, islandPos.getKey()) <= minDistance)
                        {
                            final SkyIslandType type = islandPos.getValue();
                            
                            if (type.isGenBiomeBlocks())
                            {
                                Biome biome = Biome.getBiome(type.getBiome());
                                
                                if (biome != Biomes.VOID)
                                {
                                    this.genBiomeTerrainBlocks(biome, this.rand, primer, chunkX * 16 + x, chunkZ * 16 + z, islandPos.getKey().getY(), 16);
                                }
                            }
                            continue x;
                        }
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
    
    public void genBiomeTerrainBlocks(Biome biome, Random rand, ChunkPrimer chunkPrimerIn, int x, int z, int islandMid, double noiseVal)
    {
        int i = islandMid;
        IBlockState iblockstate = biome.topBlock;
        IBlockState iblockstate1 = biome.fillerBlock;
        int j = -1;
        int k = (int)(noiseVal / 3.0D + 3.0D + rand.nextDouble() * 0.25D);
        int l = z & 15;
        int i1 = x & 15;
        BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

        for (int j1 = 255; j1 >= 0; --j1)
        {
            IBlockState iblockstate2 = chunkPrimerIn.getBlockState(i1, j1, l);

            if (iblockstate2.getMaterial() == Material.AIR)
            {
                j = -1;
            }
            else if (iblockstate2 == Blocks.STONE.getDefaultState())
            {
                if (j == -1)
                {
                    if (k <= 0)
                    {
                        iblockstate = AIR;
                        iblockstate1 = Blocks.STONE.getDefaultState();
                    }
                    else if (j1 >= i - 4 && j1 <= i + 1)
                    {
                        iblockstate = biome.topBlock;
                        iblockstate1 = biome.fillerBlock;
                    }

                    if (j1 < i && (iblockstate == null || iblockstate.getMaterial() == Material.AIR))
                    {
                        if (biome.getTemperature(blockpos$mutableblockpos.setPos(x, j1, z)) < 0.15F)
                        {
                            iblockstate = ICE;
                        }
                        else
                        {
                            iblockstate = WATER;
                        }
                    }

                    j = k;

                    if (j1 >= i - 1)
                    {
                        chunkPrimerIn.setBlockState(i1, j1, l, iblockstate);
                    }
                    else if (j1 < i - 7 - k)
                    {
                        iblockstate = AIR;
                        iblockstate1 = Blocks.STONE.getDefaultState();
                        chunkPrimerIn.setBlockState(i1, j1, l, GRAVEL);
                    }
                    else
                    {
                        chunkPrimerIn.setBlockState(i1, j1, l, iblockstate1);
                    }
                }
                else if (j > 0)
                {
                    --j;
                    chunkPrimerIn.setBlockState(i1, j1, l, iblockstate1);

                    if (j == 0 && iblockstate1.getBlock() == Blocks.SAND && k > 1)
                    {
                        j = rand.nextInt(4) + Math.max(0, j1 - 63);
                        iblockstate1 = iblockstate1.getValue(BlockSand.VARIANT) == BlockSand.EnumType.RED_SAND ? RED_SANDSTONE : SANDSTONE;
                    }
                }
            }
        }
    }
    
    private void genDecorations(final long seed, final int chunkX, final int chunkZ, final ChunkPrimer primer)
    {
        final BlockPos pos = new BlockPos(chunkX * 16, 0, chunkZ * 16);

        outer: for (final Entry<SkyIslandData, Map<BlockPos, SkyIslandType>> set : this.getIslandPositions(seed, chunkX * 16, chunkZ * 16).entrySet())
        {
            final SkyIslandData data = set.getKey();
            final double minDistance = data.getRadius();

            for (final Entry<BlockPos, SkyIslandType> islandPos : set.getValue().entrySet())
            {
                if (SkyIslandGenerator.getDistance(pos, islandPos.getKey()) < minDistance + 16)
                {
                    final SkyIslandType type = islandPos.getValue();
                    
                    for (final DecorationData decoration : type.getDecorators())
                    {
                        decoration.generateForSkyIsland(seed, chunkX, chunkZ, primer, islandPos.getKey(), data, type, this);
                    }
                    break outer;
                }
            }
        }
    }

    @Override
    public void populate(World world, int chunkX, int chunkZ, Random rand)
    {
        final long seed = world.getSeed();
        BlockFalling.fallInstantly = true;
        int i = chunkX * 16;
        int j = chunkZ * 16;
        BlockPos blockpos = new BlockPos(i, 0, j);
//        Biome biome = world.getBiome(blockpos.add(16, 0, 16));
        this.rand.setSeed(seed);
        long k = this.rand.nextLong() / 2L * 2L + 1L;
        long l = this.rand.nextLong() / 2L * 2L + 1L;
        this.rand.setSeed((long)chunkX * k + (long)chunkZ * l ^ seed);
//        boolean flag = false;
        
        final BlockPos pos = new BlockPos(i, 0, j);
        
//        if (biome == Biomes.RIVER)
//        {
//        	biome.decorate(world, this.rand, new BlockPos(i, 0, j));
//        }
//        else
//        {
        outer: for (final Entry<SkyIslandData, Map<BlockPos, SkyIslandType>> set : this.getIslandPositions(seed, i, j).entrySet())
        {
            final SkyIslandData data = set.getKey();
            final double minDistance = data.getRadius();

            for (final Entry<BlockPos, SkyIslandType> islandPos : set.getValue().entrySet())
            {
                if (MathUtil.getDistance(pos, islandPos.getKey()) < minDistance + 16)
                {
                    final SkyIslandType type = islandPos.getValue();
                    Biome typeBiome = Biome.getBiome(type.getBiome());

                    if (type.isGenDecorations())
                    {
                        if (typeBiome != Biomes.VOID)
                        {
                            typeBiome.decorate(world, this.rand, new BlockPos(i, 0, j));
                        }
                    }
                    if (type.genAnimals())
                    {
                        WorldEntitySpawner.performWorldGenSpawning(world, typeBiome, i + 8, j + 8, 16, 16, this.rand);
                    }
                    break outer;
                }
            }
        }
//        }
        
        blockpos = blockpos.add(8, 0, 8);

            {
                for (int k2 = 0; k2 < 16; ++k2)
                {
                    for (int j3 = 0; j3 < 16; ++j3)
                    {
                        BlockPos blockpos1 = world.getPrecipitationHeight(blockpos.add(k2, 0, j3));
                        BlockPos blockpos2 = blockpos1.down();

                        if (world.canBlockFreezeWater(blockpos2))
                        {
                            world.setBlockState(blockpos2, Blocks.ICE.getDefaultState(), 2);
                        }

                        if (world.canSnowAt(blockpos1, true))
                        {
                            world.setBlockState(blockpos1, Blocks.SNOW_LAYER.getDefaultState(), 2);
                        }
                    }
                }
            }

        BlockFalling.fallInstantly = false;
    }

    @Override
    public GenLayer getLayer(World world, GenLayer parent)
    {
//    	GenLayer river = new GenLayerRiverInit(1001, parent);
//        river = GenLayerZoom.magnify(1000L, river, 4); //Raise to reduce frequency
//        river = new GenLayerRiver(1L, river);
//        river = GenLayerZoom.magnify(1000L, river, 3); //Lower to reduce size
//        river = new GenLayerSmooth(1000L, river);
//        river = GenLayerZoom.magnify(1000L, river, 1);
        GenLayer biomes = new GenLayerBiomeSkyIslands(world.getSeed(), this,parent);
//        GenLayerRiverMixDC mix = new GenLayerRiverMixDC(1000L, biomes, river);
//        mix.setRiverBiome(Biome.getIdForBiome(Biomes.VOID), Biome.getIdForBiome(Biomes.VOID));
        return biomes;
    }
}