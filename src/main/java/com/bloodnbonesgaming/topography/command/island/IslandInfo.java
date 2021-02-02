package com.bloodnbonesgaming.topography.command.island;

import java.util.ArrayList;
import java.util.List;

import com.bloodnbonesgaming.topography.IOHelper;
import com.bloodnbonesgaming.topography.StructureHelper;
import com.bloodnbonesgaming.topography.config.ConfigPreset;
import com.bloodnbonesgaming.topography.config.ConfigurationManager;
import com.bloodnbonesgaming.topography.config.DimensionDefinition;
import com.bloodnbonesgaming.topography.util.SpawnStructure;
import com.bloodnbonesgaming.topography.util.capabilities.ITopographyPlayerData;
import com.bloodnbonesgaming.topography.util.capabilities.TopographyPlayerData;
import com.bloodnbonesgaming.topography.world.generator.IGenerator;
import com.bloodnbonesgaming.topography.world.generator.SkyIslandGenerator;

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
import net.minecraft.world.gen.structure.template.Template;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.server.permission.PermissionAPI;

public class IslandInfo extends CommandBase
{
    final List<String> aliases = new ArrayList<String>();

    @Override
    public String getName()
    {
        return "info";
    }

    @Override
    public String getUsage(ICommandSender sender)
    {
        return "Use /topography island info [player]";
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
            		ITopographyPlayerData data = player.getCapability(TopographyPlayerData.CAPABILITY_TOPOGRAPHY_PLAYER_DATA, null);
                    
                    if (data != null)
                    {
                    	if (data.getIslandX() != 0 || data.getIslandZ() != 0)
                    	{
                        	sender.sendMessage(new TextComponentString(this.findPlayerIsland(player, world, dimensionDef).toString()));
                    	}
                    	else
                    	{
                            throw new CommandException("Player island is 0, 0.");
                    	}
                    }
                    else
                    {
                        throw new CommandException("No island data found.");
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
    	return !(sender instanceof EntityPlayerMP) || PermissionAPI.hasPermission((EntityPlayerMP) sender, "topography.island.info");
    }
    
    private BlockPos findPlayerIsland(final EntityPlayerMP player, WorldServer world, DimensionDefinition definition) throws CommandException
    {
    	BlockPos blockpos;
        SpawnStructure structure = definition.getSpawnStructure();
        
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
                    
                    blockpos = spawn.add(0, structure.getHeight(), 0);
                    
                    return blockpos;
                }
            }
        }
        else
        {
        	for (final IGenerator generator : definition.getGenerators())
            {
                if (generator instanceof SkyIslandGenerator)
                {                    
                    ITopographyPlayerData data = player.getCapability(TopographyPlayerData.CAPABILITY_TOPOGRAPHY_PLAYER_DATA, null);
                    
                    blockpos = new BlockPos(data.getIslandX(), 0, data.getIslandZ());
                	blockpos = IslandInfo.getTopSolidOrLiquidBlock(world, blockpos).up();
                	
                	return blockpos;
                }
            }
        }
        throw new CommandException("This world does not appear to have spawn structures or sky islands.");
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
