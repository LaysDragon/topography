package com.bloodnbonesgaming.topography.command;

import java.util.ArrayList;
import java.util.List;

import com.bloodnbonesgaming.topography.config.ConfigPreset;
import com.bloodnbonesgaming.topography.config.ConfigurationManager;
import com.bloodnbonesgaming.topography.config.DimensionDefinition;
import com.bloodnbonesgaming.topography.event.EventSubscriber.ReTeleporter;

import net.minecraft.block.state.IBlockState;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.server.permission.PermissionAPI;

public class Spawn extends CommandBase
{
    final List<String> aliases = new ArrayList<String>();

    @Override
    public String getName()
    {
        return "spawn";
    }

    @Override
    public String getUsage(ICommandSender sender)
    {
        return "Use /topography spawn [player]";
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
    	
    	if (args.length == 0)
    	{
    		if (sender instanceof EntityPlayerMP)
    		{
    			player = (EntityPlayerMP) sender;
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
    	}
    	
    	WorldServer world = DimensionManager.getWorld(0);
    	
    	final ConfigurationManager manager = ConfigurationManager.getInstance();
    	
    	if (manager != null)
    	{
        	final ConfigPreset preset = manager.getPreset();
        	
        	if (preset != null)
            {
            	final DimensionDefinition dimensionDef = preset.getDefinition(world.provider.getDimension());
            	
            	if (dimensionDef != null)
            	{
                	player.sendMessage(new TextComponentString("Teleporting you to world spawn."));
            		this.teleportPlayer(player, 0, Spawn.getTopSolidOrLiquidBlock(world, world.getSpawnPoint()));
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

    @Override
    public boolean isUsernameIndex(String[] args, int index)
    {
        return index == 0;
    }

    public int getRequiredPermissionLevel()
    {
        return 0;
    }
    
    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
    	return !(sender instanceof EntityPlayerMP) || PermissionAPI.hasPermission((EntityPlayerMP) sender, "topography.world.spawn");
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
}
