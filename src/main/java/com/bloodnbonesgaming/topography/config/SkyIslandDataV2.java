package com.bloodnbonesgaming.topography.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.bloodnbonesgaming.lib.util.script.ScriptClassDocumentation;
import com.bloodnbonesgaming.lib.util.script.ScriptMethodDocumentation;
import com.bloodnbonesgaming.topography.ModInfo;

@ScriptClassDocumentation(documentationFile = ModInfo.SKY_ISLANDS_DOCUMENTATION_FOLDER + "SkyIslandDataV2", classExplaination = 
"This file is for the SkyIslandDataV2. This data object is for holding size and type data of islands for the SkyIslandGeneratorV2.")
public class SkyIslandDataV2 extends SkyIslandData {
	
    private int count = 1;
    private double horizontalRadius = 100;
    private double verticalRadius = 100;
    private double topHeight = verticalRadius;
    private double bottomHeight = verticalRadius;
    private double fluidDepth = -1;
    private int minHeight = 5;
    private int maxHeight = 220;
    protected List<SkyIslandType> types = new ArrayList<SkyIslandType>();
    private boolean randomTypes = true;
    
    private int minCount = 0;
    
    
    public int getCount()
    {
        return count;
    }
    
	public void setCount(int count)
    {
        this.count = count;
    }
	
    public double getHorizontalRadius()
    {
        return horizontalRadius;
    }
    
	public void setHorizontalRadius(int radius)
    {
        this.horizontalRadius = radius;
    }
	
//    public double getVerticalRadius()
//    {
//        return verticalRadius;
//    }
    
	public void setVerticalRadius(int radius)
    {
        this.verticalRadius = radius;
        this.topHeight = radius;
        this.bottomHeight = radius;
    }
	
	public double getTopHeight()
	{
		return this.topHeight;
	}
	
	public void setTopHeight(double height)
	{
		this.topHeight = height;
	}
	
	public double getBottomHeight()
	{
		return this.bottomHeight;
	}
	
	public void setBottomHeight(double height)
	{
		this.bottomHeight = height;
	}
	
	public double getFluidDepth()
	{
		if (this.fluidDepth < 0)
		{
	        fluidDepth = Math.floor(this.horizontalRadius / 20D);
		}
		return this.fluidDepth;
	}
	
	@ScriptMethodDocumentation(args = "double", usage = "depth", notes = "Sets the fluid depth for lakes on the islands. Default horizontalRadius / 20.")
	public void setFluidDepth(final double depth)
	{
		this.fluidDepth = depth;
	}
	
	@ScriptMethodDocumentation(args = "int, int", usage = "min, max", notes = "Sets the min/max heights for islands to generate.")
	public void setHeightRange(final int min, final int max)
	{
		this.minHeight = min;
		this.maxHeight = max;
	}
	
	public int getMinHeight()
	{
		return this.minHeight;
	}
	
	public int getMaxHeight()
	{
		return this.maxHeight;
	}
    
    @ScriptMethodDocumentation(args = "SkyIslandType", usage = "type", notes = "Adds a type of sky island to be generated.")
	public void addType(final SkyIslandType type)
    {
        this.types.add(type);
    }
    
    public SkyIslandType getType(final int index)
    {
        return this.types.get(index % types.size());
    }
    
    public SkyIslandType getType(final Random rand)
    {
        return this.types.get(rand.nextInt(this.types.size()));
    }
    
    public boolean isRandomIslands()
    {
        return randomTypes;
    }
    
    public void setRandomTypes(boolean randomTypes)
    {
        this.randomTypes = randomTypes;
    }
    
    public int getMinCount()
    {
        return this.minCount;
    }
    
    public void setMinCount(final int count)
    {
        this.minCount = count;
    }

}

