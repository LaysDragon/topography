package com.bloodnbonesgaming.topography.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.bloodnbonesgaming.lib.util.data.ItemBlockData;
import com.bloodnbonesgaming.lib.util.script.ArgType;
import com.bloodnbonesgaming.lib.util.script.ScriptArgs;
import com.bloodnbonesgaming.lib.util.script.ScriptClassDocumentation;
import com.bloodnbonesgaming.lib.util.script.ScriptMethodDocumentation;
import com.bloodnbonesgaming.topography.ModInfo;
import com.bloodnbonesgaming.topography.world.decorator.DecorationData;
import com.bloodnbonesgaming.topography.world.decorator.DecoratorScattered;
import com.bloodnbonesgaming.topography.world.generator.IGenerator;

import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.world.biome.Biome;

@ScriptClassDocumentation(documentationFile = ModInfo.SKY_ISLANDS_DOCUMENTATION_FOLDER + "SkyIslandType", classExplaination = 
"This file is for the SkyIslandType. This data object is for creating island types for the SkyIslandGeneratorV2. "
+ "These can be created in a dimension file using 'new SkyIslandType(biomeID)', with the biome id being the biome you want the island to be, or 'new SkyIslandType()' to default to the void biome.")
public class SkyIslandType
{
    private IBlockState mainBlock = Blocks.STONE.getDefaultState();
    private boolean genBiomeBlocks = true;
    private boolean genDecorations = true;
    private int biome = Biome.getIdForBiome(Biomes.VOID);
    private final Map<MinMaxBounds, IBlockState> boundsToState = new LinkedHashMap<MinMaxBounds, IBlockState>();
    private boolean genAnimals = true;
    private final List<DecorationData> decorators = new ArrayList<DecorationData>();
    private double fluidPercentage = 0.5;
    private IBlockState fluidBlock = Blocks.WATER.getDefaultState();
    private boolean generateFluid = true;
    private boolean generateMineshafts = false;
    private int mineshaftChance = 4;
    private boolean generateStrongholds = false;
    private int strongholdChance = 4;
    private boolean generateVillages = false;
    private int villageChance = 4;
    private List<IGenerator> generators = new ArrayList<IGenerator>();



    public SkyIslandType()
    {
//        this.decorators.put(new DecoratorScattered(Blocks.BEDROCK.getDefaultState()), 64);
    }

    public SkyIslandType(SkyIslandType other) {
        this.mainBlock = other.mainBlock;
        this.genBiomeBlocks = other.genBiomeBlocks;
        this.genDecorations = other.genDecorations;
        this.biome = other.biome;
        this.genAnimals = other.genAnimals;
        this.fluidPercentage = other.fluidPercentage;
        this.fluidBlock = other.fluidBlock;
        this.generateFluid = other.generateFluid;
        this.generateMineshafts = other.generateMineshafts;
        this.mineshaftChance = other.mineshaftChance;
        this.generateStrongholds = other.generateStrongholds;
        this.strongholdChance = other.strongholdChance;
        this.generateVillages = other.generateVillages;
        this.villageChance = other.villageChance;
        this.generators.addAll(other.generators);
        this.decorators.addAll(other.decorators);
        this.boundsToState.putAll(other.boundsToState);

    }

    @ScriptArgs(args = {ArgType.NON_NULL_BIOME_ID})
    public SkyIslandType(final int biome)
    {
        this();
        this.biome = biome;
    }
    
    public IBlockState getMainBlock()
    {
        return mainBlock;
    }
    public void setMainBlock(IBlockState mainBlock)
    {
        this.mainBlock = mainBlock;
    }
    public boolean isGenBiomeBlocks()
    {
        return genBiomeBlocks;
    }
    
    public boolean isGenDecorations()
    {
        return genDecorations;
    }
    
    public boolean genAnimals()
    {
        return this.genAnimals;
    }
    
    public int getBiome()
    {
        return biome;
    }
    
    @ScriptMethodDocumentation(usage = "biome id", notes = "Sets the biome to be used for the island type.")
	@ScriptArgs(args = {ArgType.NON_NULL_BIOME_ID})
    public void setBiome(int biome)
    {
        this.biome = biome;
    }
    
    @ScriptMethodDocumentation(args = "ItemBlockData", usage = "block", notes = "Sets the main block for the island type to be made of.")
	public void setMainBlock(final ItemBlockData data) throws Exception
    {
        this.mainBlock = data.buildBlockState();
    }
    
    @ScriptMethodDocumentation(usage = "", notes = "Disables biome block replacement for the island type.")
	public void disableBiomeBlockReplacement()
    {
        this.genBiomeBlocks = false;
    }
    
    @ScriptMethodDocumentation(usage = "", notes = "Disables decorations for the island type.")
	public void disableDecorations()
    {
        this.genDecorations = false;
    }
    
    @ScriptMethodDocumentation(usage = "", notes = "Disables initial animal spawning for the island type.")
	public void disableAnimals()
    {
        this.genAnimals = false;
    }
    
    @ScriptMethodDocumentation(args = "ItemBlockData, MinMaxBounds", usage = "block, bounds", notes = "Sets a block to be used for generation within a range of distance from the center of the island type. "
    		+ "0.0 being the center of the island, 1.0 being the outermost possible blocks.")
	public void setBlockInRange(final ItemBlockData data, final MinMaxBounds bounds) throws Exception
    {
        final IBlockState state = data.buildBlockState();
        
        this.boundsToState.put(bounds, state);
    }

    public Map<MinMaxBounds, IBlockState> getBoundsToStateMap()
    {
        return boundsToState;
    }
    
    //TODO Make something better than this.
    public void addDecorator(final DecoratorScattered decorator, final int count, final int regionSize)
    {
        this.decorators.add(new DecorationData(decorator, count, regionSize));
    }
    
    public List<DecorationData> getDecorators()
    {
        return this.decorators;
    }
    
    //V2 only
    @ScriptMethodDocumentation(args = "IGenerator", usage = "generator", notes = "Adds the provided generator. This generator will only generate in chunks containing islands generated with this type.")
	public void addGenerator(final IGenerator generator) {
    	this.generators.add(generator);
    }
    
    public List<IGenerator> getGenerators() {
    	return this.generators;
    }
    
    //V2 only
    @ScriptMethodDocumentation(args = "ItemBlockData", usage = "block", notes = "Sets the fluid block for the lakes on the islands to generate.")
	public void setFluidBlock(final ItemBlockData data) throws Exception
    {
        this.fluidBlock = data.buildBlockState();
    }
    
    //V2 only
    public IBlockState getFluidBlock()
    {
    	return this.fluidBlock;
    }
    
    //V2 only
    @ScriptMethodDocumentation(usage = "", notes = "Disables lake generation for the island type.")
	public void disableFluid()
    {
    	this.generateFluid = false;
    }
    
    //V2 only
    public boolean generateFluid()
    {
    	return this.generateFluid;
    }

    //V2 only
    public boolean shouldGenerateMineshafts() {
		return generateMineshafts;
	}

    //V2 only
    @ScriptMethodDocumentation(args = "int", usage = "chance", notes = "Sets the 1/chance for mineshafts to generate for the island type. These are centered on the island, and size limited.")
	public void generateMineshafts(final int chance) {
		this.mineshaftChance = chance;
		this.generateMineshafts = true;
	}

    //V2 only
    public boolean shouldGenerateStrongholds() {
		return generateStrongholds;
	}

    //V2 only
    @ScriptMethodDocumentation(args = "int", usage = "chance", notes = "Sets the 1/chance for strongholds to generate for the island type. These are centered on the island, and only generate the end portal room.")
	public void generateStrongholds(final int chance) {
		this.strongholdChance = chance;
		this.generateStrongholds = true;
	}

    //V2 only
    public boolean shouldGenerateVillages() {
		return generateVillages;
	}

    //V2 only
    @ScriptMethodDocumentation(args = "int", usage = "chance", notes = "Sets the 1/chance for villages to generate for the island type. These are centered on the island, and size limited to avoid floating buildings.")
	public void generateVillages(final int chance) {
		this.villageChance = chance;
		this.generateVillages = true;
	}

    //V2 only
    public int getMineshaftChance() {
		return mineshaftChance;
	}
	
    //V2 only
    public int getStrongholdChance() {
		return this.strongholdChance;
	}
	
    //V2 only
    public int getVillageChance() {
		return this.villageChance;
	}
	
    //V2 only
    @ScriptMethodDocumentation(args = "double", usage = "percentage", notes = "Defaults to 0.5. Higher values increase lake size, lower values decrease.")
	public void setFluidPercentage(final double fluidPercentage) {
		this.fluidPercentage = fluidPercentage;
	}
	
    //V2 only
    public double getFluidPercentage() {
		return this.fluidPercentage;
	}
}
