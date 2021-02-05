package com.bloodnbonesgaming.topography.config;


import com.bloodnbonesgaming.lib.util.script.ScriptClassDocumentation;
import com.bloodnbonesgaming.lib.util.script.ScriptMethodDocumentation;
import com.bloodnbonesgaming.topography.ModInfo;
import it.unimi.dsi.fastutil.Hash;
import scala.collection.parallel.ParIterableLike;

import java.util.*;

@ScriptClassDocumentation(documentationFile = ModInfo.SKY_ISLANDS_DOCUMENTATION_FOLDER + "SkyIslandDataV2RandomSizeExtend", classExplaination =
        "This file is for the SkyIslandDataV2RandomSizeExtend. This data object is for creating random size island types for the SkyIslandGeneratorV2. "
                + "These can be created in a dimension file using 'new SkyIslandType(biomeID)', with the biome id being the biome you want the island to be, or 'new SkyIslandType()' to default to the void biome.")
public class SkyIslandDataV2RandomSizeExtend extends SkyIslandDataV2AutoExtend {
    Map<Integer, Integer> scaleMap = new LinkedHashMap<Integer, Integer>() {{
        put(1, 9);
        put(100, 1);
    }};
    Map<Integer, Integer> fullScaleMap = new LinkedHashMap<>();
    int totalChance = 0;
    Map<Integer, Integer> chanceMap = new HashMap<>();

    public int getMaxVerticalRadius() {
        return maxVerticalRadius;
    }

    @ScriptMethodDocumentation(args = "int", usage = "maxVerticalRadius", notes = "set max random vertical radius")
    public void setMaxVerticalRadius(int maxVerticalRadius) {
        this.maxVerticalRadius = maxVerticalRadius;
    }

    private int maxVerticalRadius = 64;

    public boolean isInterpolation() {
        return interpolation;
    }

    @ScriptMethodDocumentation(args = "bool", usage = "should interpolation", notes = "should calculating interpolation of size between scale")
    public void setInterpolation(boolean interpolation) {
        this.interpolation = interpolation;
    }

    boolean interpolation = true;


    @ScriptMethodDocumentation(args = "", usage = "", notes = "clear the default scale map")
    public void clearScale() {
        this.scaleMap.clear();
        this.fullScaleMap.clear();
        this.chanceMap.clear();
    }

    @ScriptMethodDocumentation(args = "int , int", usage = "size,chance", notes = "add the scale chance")
    public void addScale(int size, int chance) {
        this.scaleMap.put(size, chance);
    }

    int calculateChance(int sizeA, int chanceA, int sizeC, int chanceC, int sizeB) {
        return (int)Math.round(((chanceC - chanceA) / (double)(sizeC - sizeA)) * (sizeB - sizeA) + chanceA);
    }

    int calculateChance(Map.Entry<Integer, Integer> entryA, Map.Entry<Integer, Integer> entryC, int sizeB) {
        return this.calculateChance(entryA.getKey(), entryA.getValue(), entryC.getKey(), entryC.getValue(), sizeB);
    }


    void calculateFullMap() {
        if (!fullScaleMap.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<Integer, Integer>> scaleIter = scaleMap.entrySet().iterator();
        Map.Entry<Integer, Integer> previousEntry = scaleIter.next();
        Map.Entry<Integer, Integer> nextEntry = scaleIter.next();
        int currentSize = previousEntry.getKey();
        while (true) {
            processCurrentSize(previousEntry, nextEntry, currentSize);


            if (this.interpolation) {
                currentSize++;
                if (currentSize == nextEntry.getKey()) {
                    if (scaleIter.hasNext()) {
                        previousEntry = nextEntry;
                        nextEntry = scaleIter.next();
                    } else {
                        processCurrentSize(previousEntry, nextEntry, currentSize);
                        break;
                    }
                }
            } else {
                currentSize = nextEntry.getKey();
                if (scaleIter.hasNext()) {
                    previousEntry = nextEntry;
                    nextEntry = scaleIter.next();
                } else {
                    processCurrentSize(previousEntry, nextEntry, currentSize);
                    break;
                }
            }

        }
    }

    private void processCurrentSize(Map.Entry<Integer, Integer> previousEntry, Map.Entry<Integer, Integer> nextEntry, int currentSize) {
        Integer currentChance;
        if (this.scaleMap.containsKey(currentSize)) {
            currentChance = this.scaleMap.get(currentSize);
        } else {
            currentChance = this.calculateChance(previousEntry, nextEntry, currentSize);
        }
        fullScaleMap.put(currentSize, currentChance);
        for (int i = 0; i < currentChance; i++) {
            this.totalChance += 1;
            chanceMap.put(totalChance, currentSize);
        }
    }

    Map<Integer, SkyIslandDataV2AutoExtend> cache = new HashMap<>();

    @Override
    public SkyIslandDataV2AutoExtend generate(Random random) {
        this.calculateFullMap();
        int randomPointer = random.nextInt(this.totalChance) + 1;
        int radius = this.chanceMap.get(randomPointer);
        if (cache.containsKey(radius)) {
            return cache.get(radius);
        }
        SkyIslandDataV2AutoExtend data = this.copy();
        data.setHorizontalRadius(radius);
        data.setVerticalRadius(Math.min(radius, maxVerticalRadius));
        cache.put(radius, data);
        return data;
    }

    @Override
    protected SkyIslandDataV2AutoExtend copy() {
        SkyIslandDataV2RandomSizeExtend newInstance = new SkyIslandDataV2RandomSizeExtend();
        return this.copyProperty(newInstance);

    }

    @Override
    protected SkyIslandDataV2AutoExtend copyProperty(SkyIslandDataV2AutoExtend data) {
        data = super.copyProperty(data);
        SkyIslandDataV2RandomSizeExtend newInstance = (SkyIslandDataV2RandomSizeExtend) data;

        //performance problem,careful about this share variable
        newInstance.scaleMap = this.scaleMap;//.putAll(this.scaleMap);
        newInstance.fullScaleMap = this.fullScaleMap; //.putAll(this.fullScaleMap);
        newInstance.chanceMap = this.chanceMap;//.putAll(this.chanceMap);
        newInstance.totalChance = this.totalChance;
        newInstance.cache = this.cache;
        newInstance.setInterpolation(this.isInterpolation());
        newInstance.setMaxVerticalRadius(this.getMaxVerticalRadius());
        return data;
    }

}