package com.bloodnbonesgaming.topography.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.bloodnbonesgaming.lib.util.script.ScriptMethodDocumentation;
import com.bloodnbonesgaming.topography.IOHelper;
import com.bloodnbonesgaming.topography.Topography;

import net.minecraft.util.ResourceLocation;
import net.minecraft.world.EnumDifficulty;

public class ConfigPreset
{
    private final String name;
    private final String image;
    private final String description;
    private final String worldType;
    private final String generatorSettings;
    
    private boolean enableHardcore;
    private EnumDifficulty initialDifficulty = null;
    private boolean lockDifficulty = false;
    private List<ResourceLocation> initialPlayerFunctions = new ArrayList<ResourceLocation>();
    private List<ResourceLocation> initialServerFunctions = new ArrayList<ResourceLocation>();
    
    private final Map<Integer, String> scripts = new HashMap<Integer, String>();
    private final Map<Integer, DimensionDefinition> definitions = new HashMap<Integer, DimensionDefinition>();
    private boolean locked = false;
    private boolean disableNetherPortal = false;
    
    public ConfigPreset(final String name, final String image)
    {
        this(name, image, null);
    }
    
    public ConfigPreset(final String name, final String image, final String description)
    {
        this(name, image, description, null, null);
    }
    
    public ConfigPreset(final String name, final String image, final String description, final String worldType)
    {
        this(name, image, description, worldType, null);
    }
    
    public ConfigPreset(final String name, final String image, final String description, final String worldType, final String generatorOptions)
    {
        this.name = name != null ? name : "";
        this.image = image;
        this.description = description;
        this.worldType = worldType;
        this.generatorSettings = generatorOptions;
    }
    
    @ScriptMethodDocumentation(args = "int, String", usage = "dimension ID, script file path", notes = "Registers a script file to be used to create a dimension. Script file path is relative to the config/topography folder, and should not include the file extension.")
	public void registerDimension(final int dimension, final String script) throws Exception
    {
        this.scripts.put(dimension, script);
    }

    public String getName()
    {
        return name;
    }

    public String getScript(final int dimension)
    {
        return this.scripts.get(dimension);
    }
    
    public Set<Integer> getDimensions()
    {
        return this.scripts.keySet();
    }

    public String getImage()
    {
        return image;
    }
    
    public String getDescription()
    {
        return this.description;
    }
    
    public String getWorldType()
    {
        return this.worldType;
    }
    
    public String getGeneratorOptions()
    {
        return this.generatorSettings;
    }
    
    public boolean hardcore()
    {
        return this.enableHardcore;
    }
    
    @ScriptMethodDocumentation(usage = "", notes = "Sets the preset as hardcore.")
	public void enableHardcore()
    {
        this.enableHardcore = true;
    }
    
    @ScriptMethodDocumentation(usage = "", notes = "Locks the preset from being chosen in the gui. Unlocking the preset is done with the /topography unlock command.")
	public void lock()
    {
        this.locked = true;
    }
    
    public boolean locked()
    {
        return this.locked ? !ConfigurationManager.getInstance().getLockHandler().unlocked(this.name) : false;
    }
    
    @ScriptMethodDocumentation(args = "String", usage = "function resource location", notes = "Adds a command function to be run on the player when they first log in. Multiple of these can be added.")
	public void addInitialPlayerFunction(final String function)
    {
    	this.initialPlayerFunctions.add(new ResourceLocation(function));
    }
    
	public void setInitialPlayerFunction(final String function)
    {
		this.addInitialPlayerFunction(function);
    }
    
    public List<ResourceLocation> getInitialPlayerFunctions()
    {
    	return this.initialPlayerFunctions;
    }
    
    @ScriptMethodDocumentation(args = "String", usage = "function resource location", notes = "Adds a command function to be run on the server at the start of the first dimension 0 world tick. Multiple of these can be added.")
	public void addInitialServerFunction(final String function)
    {
    	this.initialServerFunctions.add(new ResourceLocation(function));
    }
    
    public void setInitialServerFunction(final String function)
    {
    	this.addInitialServerFunction(function);
    }
    
    public List<ResourceLocation> getInitialServerFunctions()
    {
    	return this.initialServerFunctions;
    }
    
    @ScriptMethodDocumentation(args = "int", usage = "difficulty value", notes = "Sets the initial difficulty of the preset.")
	public void setDifficulty(final int difficulty) throws Exception
    {
    	final EnumDifficulty e = EnumDifficulty.byId(difficulty);
    	
    	if (e != null)
    	{
    		this.initialDifficulty = e;
    	}
    	else
    	{
    		throw new Exception(difficulty + " is not a valid difficulty.");
    	}
    }
    
    @ScriptMethodDocumentation(args = "String", usage = "difficulty value", notes = "Sets the initial difficulty of the preset.")
	public void setDifficulty(final String difficulty) throws Exception
    {
    	final EnumDifficulty e = EnumDifficulty.valueOf(difficulty.toUpperCase());
    	
    	if (e != null)
    	{
    		this.initialDifficulty = e;
    	}
    	else
    	{
    		throw new Exception(difficulty + " is not a valid difficulty.");
    	}
    }
    
    public EnumDifficulty getDifficulty()
    {
    	return this.initialDifficulty;
    }
    
    @ScriptMethodDocumentation(usage = "", notes = "Locks the difficulty of the preset.")
	public void lockDifficulty()
    {
    	this.lockDifficulty = true;
    }
    
    public boolean shouldLockDifficulty()
    {
    	return this.lockDifficulty;
    }
	
	@ScriptMethodDocumentation(usage = "", notes = "Disables the creation of a nether portal in the preset.")
	public void disableNetherPortal()
	{
		this.disableNetherPortal = true;
	}
	
	public boolean shouldDisableNetherPortal()
	{
		return this.disableNetherPortal;
	}
	
	public DimensionDefinition getDefinition(final int dimension)
	{
		if (!this.definitions.containsKey(dimension))
		{
			Topography.instance.getLog().info("Loading Dimension Definition for " + dimension);
			final DimensionDefinition definition = new DimensionDefinition();
			IOHelper.loadDimensionDefinition(this.scripts.get(dimension), definition);
			this.definitions.put(dimension, definition);
		}
		return this.definitions.get(dimension);
	}
	
	public Map<Integer, DimensionDefinition> getAllDefinitions()
	{
		return this.definitions;
	}
	
	public void loadAllDefinitions()
	{
		for (Entry<Integer, String> entry : this.scripts.entrySet())
		{
			Topography.instance.getLog().info("Loading Dimension Definition for " + entry.getKey());
			final DimensionDefinition definition = new DimensionDefinition();
			IOHelper.loadDimensionDefinition(entry.getValue(), definition);
			this.definitions.put(entry.getKey(), definition);
		}
	}
}
