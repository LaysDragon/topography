package com.bloodnbonesgaming.topography.world.generator.vanilla;

import java.util.Random;

import com.bloodnbonesgaming.topography.world.generator.IGenerator;

import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.feature.WorldGenHellLava;
import net.minecraft.world.gen.layer.GenLayer;

public class VanillaLavaPocketGenerator implements IGenerator
{
    private final WorldGenHellLava hellSpringGen = new WorldGenHellLava(Blocks.FLOWING_LAVA, false);

    @Override
    public void generate(World world, ChunkPrimer primer, int chunkX, int chunkZ)
    {

    }

    @Override
    public void populate(World world, int chunkX, int chunkZ, Random rand)
    {
        BlockPos blockpos = new BlockPos(chunkX * 16, 0, chunkZ * 16);
        
        for (int k = 0; k < 16; ++k)
        {
            this.hellSpringGen.generate(world, rand, blockpos.add(rand.nextInt(16) + 8, rand.nextInt(245) + 4, rand.nextInt(16) + 8));
        }
    }

    @Override
    public GenLayer getLayer(World world, GenLayer parent)
    {
        return null;
    }

}