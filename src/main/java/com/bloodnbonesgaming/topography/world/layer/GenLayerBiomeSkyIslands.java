package com.bloodnbonesgaming.topography.world.layer;

import java.util.Map;
import java.util.Map.Entry;

import com.bloodnbonesgaming.topography.config.SkyIslandData;
import com.bloodnbonesgaming.topography.config.SkyIslandDataV2;
import com.bloodnbonesgaming.topography.config.SkyIslandType;
import com.bloodnbonesgaming.topography.util.MathUtil;
import com.bloodnbonesgaming.topography.world.generator.SkyIslandGenerator;

import net.minecraft.init.Biomes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.layer.GenLayer;
import net.minecraft.world.gen.layer.IntCache;

public class GenLayerBiomeSkyIslands extends GenLayer {
    final SkyIslandGenerator generator;
    GenLayer parent;
    final long worldSeed;


    public GenLayerBiomeSkyIslands(long p_i2125_1_, final SkyIslandGenerator generator,GenLayer parent) {
        super(p_i2125_1_);
        this.worldSeed = p_i2125_1_;
        this.generator = generator;
        this.parent = parent;
    }

    @Override
    public int[] getInts(int chunkX, int chunkZ, int width, int depth) {
        int[] returnInts;
        if(parent != null){
            returnInts = this.parent.getInts(chunkX,chunkZ,width,depth);
        }else{
            returnInts = IntCache.getIntCache(width * depth);
        }

        final Map<SkyIslandData, Map<BlockPos, SkyIslandType>> islandPositions = this.generator.getIslandPositions(this.worldSeed, chunkX, chunkZ);

        for (int x = 0; x < width; x++) {
            x:
            for (int z = 0; z < depth; z++) {
                final BlockPos pos = new BlockPos(chunkX + x, 0, chunkZ + z);

                for (final Entry<SkyIslandData, Map<BlockPos, SkyIslandType>> set : islandPositions.entrySet()) {
                    final SkyIslandData data = set.getKey();
                    final double minDistance;

                    if (data instanceof SkyIslandDataV2) {
                        minDistance = ((SkyIslandDataV2) data).getHorizontalRadius();
                    } else {
                        minDistance = data.getRadius();
                    }

                    for (final Entry<BlockPos, SkyIslandType> islandPos : set.getValue().entrySet()) {
                        if (MathUtil.getDistance(pos, islandPos.getKey()) <= minDistance) {
                            final SkyIslandType type = islandPos.getValue();

                            returnInts[x + z * width] = type.getBiome();
                            continue x;
                        }
                    }
                }
                if(parent == null){
                    returnInts[x + z * width] = Biome.getIdForBiome(Biomes.VOID);
                }
            }
        }
        return returnInts;
    }

}
