package com.bloodnbonesgaming.topography.command.island;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.bloodnbonesgaming.topography.IOHelper;
import com.bloodnbonesgaming.topography.StructureHelper;
import com.bloodnbonesgaming.topography.Topography;
import com.bloodnbonesgaming.topography.config.ConfigurationManager;
import com.bloodnbonesgaming.topography.config.DimensionDefinition;
import com.bloodnbonesgaming.topography.config.SkyIslandData;
import com.bloodnbonesgaming.topography.config.SkyIslandType;
import com.bloodnbonesgaming.topography.util.SpawnStructure;
import com.bloodnbonesgaming.topography.util.capabilities.ITopographyPlayerData;
import com.bloodnbonesgaming.topography.util.capabilities.TopographyPlayerData;
import com.bloodnbonesgaming.topography.world.WorldSavedDataTopography;
import com.bloodnbonesgaming.topography.world.generator.IGenerator;
import com.bloodnbonesgaming.topography.world.generator.SkyIslandGenerator;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.block.state.IBlockState;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.SyntaxErrorException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.structure.template.PlacementSettings;
import net.minecraft.world.gen.structure.template.Template;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.server.permission.PermissionAPI;

public class IslandNew extends CommandBase
{
    final List<String> aliases = new ArrayList<String>();

    @Override
    public String getName()
    {
        return "new";
    }

    @Override
    public String getUsage(ICommandSender sender)
    {
        return "Use /topography island new <player>";
    }

    @Override
    public List<String> getAliases()
    {
        return this.aliases;
    }
    
    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        if (args.length > 0)
        {
        	Entity entity = getEntity(server, sender, args[0], EntityPlayerMP.class);
        	
            if (!checkEntity(entity))
            {
                throw new CommandException("The entity selected (%s) is not valid.", entity.getName());
            }
            if (entity.world.provider.getDimension() != 0)
            {
            	throw new CommandException("This command may only be run in dimension 0.");
            }
            
        	final World world = DimensionManager.getWorld(0);
        	DimensionDefinition definition = ConfigurationManager.getInstance().getPreset().getDefinition(world.provider.getDimension());
            
            if (definition.getSpawnStructure() != null)
            {
                final BlockPos pos = this.findNextIsland((EntityPlayerMP) entity, definition);
                this.spawnIslands((EntityPlayerMP) entity, pos.getX(), pos.getZ());
                return;
            }
            else
            {                
                for (final IGenerator generator : definition.getGenerators())
                {
                    if (generator instanceof SkyIslandGenerator)
                    {
                        final BlockPos pos = this.findNextIsland((EntityPlayerMP) entity, definition);
                        this.setPlayerSpawn((EntityPlayerMP) entity, pos);
                        return;
                    }
                }
            }
            throw new CommandException("This preset does not have spawn structures or sky islands.");
        }
        else
        {
            throw new SyntaxErrorException(this.getUsage(sender));
        }
    }

    @Override
    public boolean isUsernameIndex(String[] args, int index)
    {
        return index == 0;
    }

    public int getRequiredPermissionLevel()
    {
        return 0;
    }
    
    private BlockPos findNextIsland(final EntityPlayer player, final DimensionDefinition definition) throws CommandException
    {
    	final World world = DimensionManager.getWorld(0);
        
        SkyIslandGenerator skyIslands = null;
        
        int x = 0;
        int z = 0;
        
        for (final IGenerator generator : definition.getGenerators())
        {
            if (generator instanceof SkyIslandGenerator)
            {
            	skyIslands = (SkyIslandGenerator) generator;
            	break;
            }
        }
        
        if (definition.getSpawnStructure() != null || skyIslands != null)
        {
        	int islandIndex = WorldSavedDataTopography.getIslandIndex(world);
        	int index = 0;
        	int ring = 0;
        	int posInRing = 0;
        	
        	for (int i = 1; i < 10000; i++)
        	{
        		index += (i * 8);
        		if (index >= islandIndex)
        		{
        			ring = i;
        			posInRing = islandIndex - (index - i * 8);
        			break;
        		}
        	}
        	int ringSideLength = (ring - 1) * 2 + 1;
        	x = ring;
        	z = ring + 1;
        	
        	for (int i = 0; i <= (ringSideLength + 1) * 4; i++)
        	{
        		if (i <= ringSideLength + 1)
        		{
        			z--;
        		}
        		else if (i <= (ringSideLength + 1) * 2)
        		{
        			x--;
        		}
        		else if (i <= (ringSideLength + 1) * 3)
        		{
        			z++;
        		}
        		else
        		{
        			x++;
        		}
        		if (i == posInRing)
        		{
        			break;
        		}
        	}
        	
        	islandIndex++;
        	WorldSavedDataTopography.saveIslandIndex(islandIndex, world);
        }
        
        if (definition.getSpawnStructure() != null)
        {
        	return new BlockPos(x * definition.getSpawnStructureSpacing(), 0, z * definition.getSpawnStructureSpacing());
        }
        else if (skyIslands != null)
        {
        	final Iterator<Entry<SkyIslandData, Map<BlockPos, SkyIslandType>>> iterator = skyIslands.getIslandPositions(world.getSeed(), x * skyIslands.getRegionSize(), z * skyIslands.getRegionSize()).entrySet().iterator();
            
            if (iterator.hasNext())
            {
                final Entry<SkyIslandData, Map<BlockPos, SkyIslandType>> islands = iterator.next();
                
                final Iterator<Entry<BlockPos, SkyIslandType>> positions = islands.getValue().entrySet().iterator();
                
                if (positions.hasNext())
                {
                    final Entry<BlockPos, SkyIslandType> island = positions.next();
                    
                    final BlockPos pos = island.getKey();
                    final BlockPos topBlock = IslandNew.getTopSolidOrLiquidBlock(world, pos);
                    
                	return new BlockPos(topBlock.up());
                }
            }
        }
        throw new CommandException("This preset does not have spawn structures or sky islands.");
    }
    
    private void spawnIslands(final EntityPlayerMP player, final int xOffset, final int zOffset)
    {
    	final WorldServer overworld = FMLCommonHandler.instance().getMinecraftServerInstance().getWorld(0);
    	String settings = overworld.getWorldInfo().getGeneratorOptions();
        
    	if (!settings.isEmpty())
    	{
            final JsonParser parser = new JsonParser();
            JsonElement element = parser.parse(settings);
            
            if (element.isJsonObject())
            {
                if (((JsonObject) element).has("Topography-Preset"))
                {
                	player.sendMessage(new TextComponentString("This may take a few seconds. Please wait."));
                	
        			for (final int dimension : ConfigurationManager.getInstance().getPreset().getDimensions())
        			{
        				DimensionDefinition definition = ConfigurationManager.getInstance().getPreset().getDefinition(dimension);
        	            
        	            SpawnStructure structure = definition.getSpawnStructure();
        	            
        	            if (structure != null)
        	            {
        	            	final World dimWorld = FMLCommonHandler.instance().getMinecraftServerInstance().getWorld(dimension);
                            
                            final Template template = IOHelper.loadStructureTemplate(structure.getStructure());
                            
                            if (template != null)
                            {
                                int preloadArea = template.getSize().getX();
                                preloadArea = template.getSize().getZ() > preloadArea ? template.getSize().getZ() : preloadArea;
                                preloadArea = preloadArea / 16;
                                preloadArea += 2;
                                Topography.instance.getLog().info("Preloading " + ((preloadArea * 2 + 1) * (preloadArea * 2 + 1)) + " chunks for spawn structure in dimension " + dimension);
            	            	
            	            	for (int x = -preloadArea; x < preloadArea; x++)
                                {
                                    for (int z = -preloadArea; z < preloadArea; z++)
                                    {
                                       dimWorld.getChunkProvider().provideChunk(x + xOffset, z + zOffset);
                                    }
                                }
            	            	final BlockPos structurePos = new BlockPos(xOffset * 16, structure.getHeight(), zOffset * 16);
            	            	Topography.instance.getLog().info("Spawning structure for dimension " + dimension + " at " + structurePos.toString());
            
                                template.addBlocksToWorld(dimWorld, structurePos, new PlacementSettings(), 2);
                                
                                if (dimension == 0)
                                {
                                    BlockPos spawn = StructureHelper.getSpawn(template);
                                    spawn = spawn.add(xOffset * 16, structure.getHeight(), zOffset * 16);
                                    player.setSpawnPoint(spawn, true);
                                    player.setPositionAndUpdate(spawn.getX(), spawn.getY(), spawn.getZ());

                                    ITopographyPlayerData data = player.getCapability(TopographyPlayerData.CAPABILITY_TOPOGRAPHY_PLAYER_DATA, null);
                                    if (data != null)
                                    {
                                        data.setIsland(structurePos.getX(), structurePos.getZ());
                                    }
                                }
                            }
        	            }
        			}
                	player.sendMessage(new TextComponentString("Finished spawning structures."));
                }
            }
    	}
    }
    
    private void setPlayerSpawn(final EntityPlayerMP player, final BlockPos pos)
    {
    	BlockPos spawn = pos.add(0.5, 0, 0.5);
    	player.setSpawnPoint(spawn, true);
        player.setPositionAndUpdate(spawn.getX(), spawn.getY(), spawn.getZ());

        ITopographyPlayerData data = player.getCapability(TopographyPlayerData.CAPABILITY_TOPOGRAPHY_PLAYER_DATA, null);
        if (data != null)
        {
            data.setIsland(pos.getX(), pos.getZ());
        }
    }

    //From the Forge CommandSetDimension
    private static boolean checkEntity(Entity entity)
    {
        // use vanilla portal logic, try to avoid doing anything too silly
        return !entity.isRiding() && !entity.isBeingRidden() && entity.isNonBoss();
    }
    
    public static BlockPos getTopSolidOrLiquidBlock(World world, BlockPos pos)
    {
        Chunk chunk = world.getChunk(pos);
        BlockPos blockpos;
        BlockPos blockpos1;

        for (blockpos = new BlockPos(pos.getX(), chunk.getTopFilledSegment() + 16, pos.getZ()); blockpos.getY() >= 0; blockpos = blockpos1)
        {
            blockpos1 = blockpos.down();
            IBlockState state = chunk.getBlockState(blockpos1);

            if (state.getMaterial().blocksMovement())
            {
                break;
            }
        }

        return blockpos;
    }
    
    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
    	return !(sender instanceof EntityPlayerMP) || PermissionAPI.hasPermission((EntityPlayerMP) sender, "topography.island.new");
    }
}
