package com.bloodnbonesgaming.topography.world.chunkgenerator;

import java.util.List;
import java.util.Random;

import javax.annotation.Nullable;

import com.bloodnbonesgaming.topography.config.DimensionDefinition;
import com.bloodnbonesgaming.topography.world.generator.IGenerator;

import net.minecraft.block.BlockFalling;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldEntitySpawner;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.terraingen.DecorateBiomeEvent;

public class ChunkGeneratorVoid implements IChunkGenerator
{
    protected final Random rand;
    public final World worldObj;
    public Biome[] biomesForGeneration;
    final DimensionDefinition definition;

    public ChunkGeneratorVoid(World worldIn, long seed, final DimensionDefinition definition)
    {
        this.worldObj = worldIn;
        this.rand = new Random(seed);
        this.definition = definition;
    }

    @Override
    public Chunk generateChunk(int x, int z)
    {
        this.rand.setSeed((long)x * 341873128712L + (long)z * 132897987541L);
        ChunkPrimer chunkprimer = new ChunkPrimer();
        this.biomesForGeneration = this.worldObj.getBiomeProvider().getBiomes(this.biomesForGeneration, x * 16, z * 16, 16, 16);
        net.minecraftforge.event.ForgeEventFactory.onReplaceBiomeBlocks(this, x, z, chunkprimer, this.worldObj);

        for (final IGenerator generator : this.definition.getGenerators())
        {
            generator.generate(this.worldObj, chunkprimer, x, z);
        }
        
        this.definition.getStructureHandler().generateStructures(this.worldObj, x, z, chunkprimer);
        
        Chunk chunk = new Chunk(this.worldObj, chunkprimer, x, z);
        byte[] abyte = chunk.getBiomeArray();

        for (int i = 0; i < abyte.length; ++i)
        {
            abyte[i] = (byte)Biome.getIdForBiome(this.biomesForGeneration[i]);
        }

        chunk.generateSkylightMap();
        if (this.definition.resetRelightChecks())
            chunk.resetRelightChecks();
//        chunk.checkLight();
        return chunk;
    }

    @Override
    public void populate(int x, int z)
    {
        BlockFalling.fallInstantly = true;
        int i = x * 16;
        int j = z * 16;
        this.rand.setSeed((long)x * 341873128712L + (long)z * 132897987541L);

        ForgeEventFactory.onChunkPopulate(true, this, this.worldObj, this.rand, x, z, false);
        
        for (final IGenerator generator : this.definition.getGenerators())
        {
            generator.populate(this.worldObj, x, z, this.rand);
        }
        
        this.definition.getStructureHandler().populateStructures(this.worldObj, this.rand, new ChunkPos(x, z));
        
        MinecraftForge.EVENT_BUS.post(new DecorateBiomeEvent.Pre(this.worldObj, this.rand, new BlockPos(i, 0, j)));
        
        MinecraftForge.EVENT_BUS.post(new DecorateBiomeEvent.Post(this.worldObj, this.rand, new BlockPos(i, 0, j)));
        
        WorldEntitySpawner.performWorldGenSpawning(this.worldObj, this.worldObj.getBiome(new BlockPos(i + 16, 0, j + 16)), i + 8, j + 8, 16, 16, this.rand);
        
        ForgeEventFactory.onChunkPopulate(false, this, this.worldObj, this.rand, x, z, false);

        BlockFalling.fallInstantly = false;
    }

    @Override
    public boolean generateStructures(Chunk chunkIn, int x, int z)
    {
        return false;
    }

    @Override
    public List<Biome.SpawnListEntry> getPossibleCreatures(EnumCreatureType creatureType, BlockPos pos)
    {
        if (this.worldObj.getChunkFromBlockCoords(pos).isEmpty())
            return null;
        Biome biome = this.worldObj.getBiome(pos);

        return this.definition.getStructureHandler().getPossibleCreatures(creatureType, this.worldObj, pos, biome.getSpawnableList(creatureType));
    }

    @Override
    public void recreateStructures(Chunk chunkIn, int x, int z)
    {
        this.definition.getStructureHandler().recreateStructures(this.worldObj, chunkIn, x, z);
    }

    @Override
    @Nullable
    public BlockPos getNearestStructurePos(World worldIn, String structureName, BlockPos position, boolean findUnexplored) {
        return this.definition.getStructureHandler().getNearestStructurePos(worldIn, structureName, position, findUnexplored);
    }

    @Override
    public boolean isInsideStructure(World worldIn, String structureName, BlockPos pos) {
        return this.definition.getStructureHandler().isInsideStructure(worldIn, structureName, pos);
    }
}