package com.bloodnbonesgaming.topography.command.island;

import java.util.ArrayList;
import java.util.List;

import com.bloodnbonesgaming.topography.IOHelper;
import com.bloodnbonesgaming.topography.StructureHelper;
import com.bloodnbonesgaming.topography.config.ConfigPreset;
import com.bloodnbonesgaming.topography.config.ConfigurationManager;
import com.bloodnbonesgaming.topography.config.DimensionDefinition;
import com.bloodnbonesgaming.topography.event.EventSubscriber.ReTeleporter;
import com.bloodnbonesgaming.topography.util.SpawnStructure;
import com.bloodnbonesgaming.topography.util.capabilities.ITopographyPlayerData;
import com.bloodnbonesgaming.topography.util.capabilities.TopographyPlayerData;
import com.bloodnbonesgaming.topography.world.generator.IGenerator;
import com.bloodnbonesgaming.topography.world.generator.SkyIslandGenerator;

import net.minecraft.block.state.IBlockState;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.structure.template.Template;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.server.permission.PermissionAPI;

public class IslandSet extends CommandBase
{
    final List<String> aliases = new ArrayList<String>();

    @Override
    public String getName()
    {
        return "set";
    }

    @Override
    public String getUsage(ICommandSender sender)
    {
        return "Use /topography island set [player]";
    }

    @Override
    public List<String> getAliases()
    {
        return this.aliases;
    }
    
    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
    	EntityPlayerMP player;
    	BlockPos pos;
    	WorldServer world = DimensionManager.getWorld(0);
    	
    	if (args.length == 2)
    	{
    		if (sender instanceof EntityPlayerMP)
    		{
    			player = (EntityPlayerMP) sender;
    			
    			final ConfigurationManager manager = ConfigurationManager.getInstance();
    	    	
    	    	if (manager != null)
    	    	{
    	        	final ConfigPreset preset = manager.getPreset();
    	        	
    	        	if (preset != null)
    	            {
    	            	final DimensionDefinition dimensionDef = preset.getDefinition(world.provider.getDimension());
    	            	
    	            	if (dimensionDef != null)
    	            	{
    	            		ITopographyPlayerData data = player.getCapability(TopographyPlayerData.CAPABILITY_TOPOGRAPHY_PLAYER_DATA, null);
    	    	            
    	    	            //TODO set pos, set spawn and teleport
    	    	        	CoordinateArg x = CommandBase.parseCoordinate(player.posX, args[0], false);
    	    	        	CoordinateArg z = CommandBase.parseCoordinate(player.posZ, args[1], false);
    	    	        	
    	    	        	data.setIsland((int)x.getAmount(), (int)z.getAmount());
    	            	}
    	    	    	else
    	    	    	{
    	    	            throw new CommandException("Command can only be used if the overworld is created using Topography.");
    	    	    	}
    	            }
        	    	else
        	    	{
        	            throw new CommandException("Command can only be used if the overworld is created using Topography.");
        	    	}
    	    	}
    	    	else
    	    	{
    	            throw new CommandException("Command can only be used if the overworld is created using Topography.");
    	    	}
    		}
    		else
    		{
                throw new CommandException("Command must have a target argument if not run by a player.");
    		}
    	}
    	else
    	{
    		player = CommandBase.getPlayer(server, sender, args[0]);
    		
    		if (player == null)
    		{
                throw new CommandException("The entity selected (%s) is not a valid player.", args[0]);
    		}
    		
    		final ConfigurationManager manager = ConfigurationManager.getInstance();
	    	
	    	if (manager != null)
	    	{
	        	final ConfigPreset preset = manager.getPreset();
	        	
	        	if (preset != null)
	            {
	            	final DimensionDefinition dimensionDef = preset.getDefinition(world.provider.getDimension());
	            	
	            	if (dimensionDef != null)
	            	{
	            		ITopographyPlayerData data = player.getCapability(TopographyPlayerData.CAPABILITY_TOPOGRAPHY_PLAYER_DATA, null);
	    	            
	    	            //TODO set pos, set spawn and teleport
	    	        	CoordinateArg x = CommandBase.parseCoordinate(player.posX, args[1], false);
	    	        	CoordinateArg z = CommandBase.parseCoordinate(player.posZ, args[2], false);
	    	        	
	    	        	data.setIsland((int)x.getAmount(), (int)z.getAmount());
	            	}
	    	    	else
	    	    	{
	    	            throw new CommandException("Command can only be used if the overworld is created using Topography.");
	    	    	}
	            }
		    	else
		    	{
		            throw new CommandException("Command can only be used if the overworld is created using Topography.");
		    	}
	    	}
	    	else
	    	{
	            throw new CommandException("Command can only be used if the overworld is created using Topography.");
	    	}
    	}
    	final ConfigurationManager manager = ConfigurationManager.getInstance();
    	
    	if (manager != null)
    	{
        	final ConfigPreset preset = manager.getPreset();
        	
        	if (preset != null)
            {
            	final DimensionDefinition dimensionDef = preset.getDefinition(world.provider.getDimension());
            	
            	if (dimensionDef != null)
            	{                	
                	SpawnStructure structure = dimensionDef.getSpawnStructure();
                    
                    if (structure != null)
                    {
                        final Template template = IOHelper.loadStructureTemplate(structure.getStructure());

                        if (template != null)
                        {
                            BlockPos spawn = StructureHelper.getSpawn(template);
                            
                            if (spawn != null)
                            {
                            	ITopographyPlayerData data = player.getCapability(TopographyPlayerData.CAPABILITY_TOPOGRAPHY_PLAYER_DATA, null);
                                spawn = spawn.add(data.getIslandX(), 0, data.getIslandZ());
                                
                                pos = spawn.add(0, structure.getHeight(), 0);
                                
                            	player.sendMessage(new TextComponentString("Teleporting you to your structure."));
                                player.setSpawnPoint(pos, true);
                                this.teleportPlayer(player, 0, pos);
                                return;
                            }
                        }
                    }
                    else
                    {
                    	for (final IGenerator generator : dimensionDef.getGenerators())
                        {
                            if (generator instanceof SkyIslandGenerator)
                            {                    
                                ITopographyPlayerData data = player.getCapability(TopographyPlayerData.CAPABILITY_TOPOGRAPHY_PLAYER_DATA, null);
                                
                                pos = new BlockPos(data.getIslandX(), 0, data.getIslandZ());
                            	pos = IslandHome.getTopSolidOrLiquidBlock(world, pos).up();

                            	player.sendMessage(new TextComponentString("Teleporting you to your sky island."));
                                player.setSpawnPoint(pos, true);
                                this.teleportPlayer(player, 0, pos);
                                return;
                            }
                        }
                    }
            	}
            }
    	}
        throw new CommandException("Unable to find a spawn structure or sky island in the dimension.");
    }

    @Override
    public boolean isUsernameIndex(String[] args, int index)
    {
        return index == 0;
    }

    @Override
    public int getRequiredPermissionLevel()
    {
        return 0;
    }
    
    private void teleportPlayer(EntityPlayerMP player, int dimension, BlockPos pos)
    {
    	if (player.world.provider.getDimension() != dimension)
    	{
        	player.changeDimension(0, new ReTeleporter(pos));
    	}
    	else
    	{
            player.setPositionAndUpdate(pos.getX(), pos.getY(), pos.getZ());
    	}
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
    	return !(sender instanceof EntityPlayerMP) || PermissionAPI.hasPermission((EntityPlayerMP) sender, "topography.island.set");
    }
}
