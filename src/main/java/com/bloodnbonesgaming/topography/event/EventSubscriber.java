package com.bloodnbonesgaming.topography.event;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.bloodnbonesgaming.topography.IOHelper;
import com.bloodnbonesgaming.topography.StructureHelper;
import com.bloodnbonesgaming.topography.Topography;
import com.bloodnbonesgaming.topography.command.island.IslandNew;
import com.bloodnbonesgaming.topography.config.ConfigPreset;
import com.bloodnbonesgaming.topography.config.ConfigurationManager;
import com.bloodnbonesgaming.topography.config.DimensionDefinition;
import com.bloodnbonesgaming.topography.config.EntityEffect;
import com.bloodnbonesgaming.topography.config.SkyIslandData;
import com.bloodnbonesgaming.topography.config.SkyIslandType;
import com.bloodnbonesgaming.topography.util.SpawnStructure;
import com.bloodnbonesgaming.topography.util.capabilities.ITopographyPlayerData;
import com.bloodnbonesgaming.topography.util.capabilities.TopographyPlayerData;
import com.bloodnbonesgaming.topography.world.WorldSavedDataTopography;
import com.bloodnbonesgaming.topography.world.WorldTypeCustomizable;
import com.bloodnbonesgaming.topography.world.generator.IGenerator;
import com.bloodnbonesgaming.topography.world.generator.SkyIslandGenerator;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.block.Block;
import net.minecraft.block.BlockPortal;
import net.minecraft.command.CommandResultStats;
import net.minecraft.command.CommandSenderWrapper;
import net.minecraft.command.FunctionObject;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.template.PlacementSettings;
import net.minecraft.world.gen.structure.template.Template;
import net.minecraftforge.common.util.ITeleporter;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.WorldTickEvent;

public class EventSubscriber
{
    @SubscribeEvent
    public void onCreateWorldSpawn(final WorldEvent.CreateSpawnPosition event)
    {
    	final ConfigurationManager manager = ConfigurationManager.getInstance();
    	
    	if (manager != null)
    	{
        	final ConfigPreset preset = manager.getPreset();
        	
        	if (preset != null)
            {
            	final DimensionDefinition dimensionDef = preset.getDefinition(event.getWorld().provider.getDimension());
            	
            	if (dimensionDef != null)
            	{
            		for (final IGenerator generator : dimensionDef.getGenerators())
                    {
                        if (generator instanceof SkyIslandGenerator)
                        {
                            final SkyIslandGenerator islandGenerator = (SkyIslandGenerator) generator;
                            
                            final Iterator<Entry<SkyIslandData, Map<BlockPos, SkyIslandType>>> iterator = islandGenerator.getIslandPositions(event.getWorld().getSeed(), 0, 0).entrySet().iterator();
                            
                            if (iterator.hasNext())
                            {
                                final Entry<SkyIslandData, Map<BlockPos, SkyIslandType>> islands = iterator.next();
                                
                                final Iterator<Entry<BlockPos, SkyIslandType>> positions = islands.getValue().entrySet().iterator();
                                
                                if (positions.hasNext())
                                {
                                    final Entry<BlockPos, SkyIslandType> island = positions.next();
                                    
                                    final BlockPos pos = island.getKey();
                                    final BlockPos topBlock = event.getWorld().getTopSolidOrLiquidBlock(pos);
                                    
                                    event.getWorld().getWorldInfo().setSpawn(topBlock.up());
                                    event.setCanceled(true);
                                }
                            }
                            break;
                        }
                    }
            	}
            }
    	}
    }
    
    @SubscribeEvent
    public void onGetPotentialSpawns(final WorldEvent.PotentialSpawns event)
    {
    	if (event.getWorld().getChunk(event.getPos()).isEmpty())
        {
            event.setCanceled(true);
        }
    }
    
    @SubscribeEvent
    public void onEntityTick(final LivingUpdateEvent event)
    {
    	final ConfigurationManager manager = ConfigurationManager.getInstance();
    	
    	if (manager != null)
    	{
        	final ConfigPreset preset = manager.getPreset();
        	
        	if (preset != null)
            {
            	final DimensionDefinition dimensionDef = preset.getDefinition(event.getEntityLiving().world.provider.getDimension());
            	
            	if (dimensionDef != null)
            	{
            		for (final EntityEffect effect : dimensionDef.getEntityEffects())
                    {
                        effect.apply(event.getEntityLiving());
                    }
            	}
            }
    	}
    }
    
    @SubscribeEvent
    public void playerLoginEvent(final PlayerLoggedInEvent event)
    {
    	if (!event.player.world.isRemote)
    	{    		
    		if (ConfigurationManager.getInstance() != null)
    		{
    			final NBTTagCompound nbt = event.player.getEntityData();
        		
        		if (!nbt.hasKey(EntityPlayer.PERSISTED_NBT_TAG))
        		{
        			nbt.setTag(EntityPlayer.PERSISTED_NBT_TAG, new NBTTagCompound());
        		}
        		
        		final NBTTagCompound persistent = nbt.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
        		
        		if (!persistent.hasKey("topography_initial"))
        		{
        			if (ConfigurationManager.getInstance().getPreset() != null)
        			{
            			final List<ResourceLocation> functions = ConfigurationManager.getInstance().getPreset().getInitialPlayerFunctions();
            			
            			for (final ResourceLocation function : functions)
            			{
            				final MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance().getServer();
                			
                            FunctionObject functionobject = server.getFunctionManager().getFunction(function);
                            
                            
//                            server.getFunctionManager().execute(functionobject, CommandSenderWrapper.create(event.player).computePositionVector().withPermissionLevel(42).withSendCommandFeedback(true));

                	        if (functionobject != null)
                	        {
                    			Topography.instance.getLog().info("Running initial function " + function.toString() + " for player " + event.player.getName());
                	            ICommandSender icommandsender = new ICommandSender()
                	            {
                	                public String getName()
                	                {
                	                    return event.player.getName();
                	                }
                	                public ITextComponent getDisplayName()
                	                {
                	                    return event.player.getDisplayName();
                	                }
                	                public void sendMessage(ITextComponent component)
                	                {
                	                }
                	                public boolean canUseCommand(int permLevel, String commandName)
                	                {
                	                    return true;
                	                }
                	                public BlockPos getPosition()
                	                {
                	                    return event.player.getPosition();
                	                }
                	                public Vec3d getPositionVector()
                	                {
                	                    return event.player.getPositionVector();
                	                }
                	                public World getEntityWorld()
                	                {
                	                    return event.player.world;
                	                }
                	                public Entity getCommandSenderEntity()
                	                {
                	                    return event.player;
                	                }
                	                public boolean sendCommandFeedback()
                	                {
                	                	return true;
//                	                    return server.worlds[0].getGameRules().getBoolean("commandBlockOutput");
                	                }
                	                public void setCommandStat(CommandResultStats.Type type, int amount)
                	                {
                	                	event.player.setCommandStat(type, amount);
                	                }
                	                public MinecraftServer getServer()
                	                {
                	                    return event.player.getServer();
                	                }
                	            };
                	            server.getFunctionManager().execute(functionobject, icommandsender);
                	        }
            			}
            			persistent.setBoolean("topography_initial", true);
        			}
        		}
    		}
    	}
    }
    
    @SubscribeEvent
    public void onWorldTick(final WorldTickEvent event)
    {
    	if (event.phase == TickEvent.Phase.START)
    	{
    		if (!event.world.isRemote)
    		{
    			if (event.world.provider.getDimension() == 0)
                {
        			if (!WorldSavedDataTopography.exists(event.world))
    				{
        				String settings = event.world.getWorldInfo().getGeneratorOptions();
                        
                    	if (!settings.isEmpty())
                    	{
                            final JsonParser parser = new JsonParser();
                            JsonElement element = parser.parse(settings);
                            
                            if (element.isJsonObject())
                            {
                                if (((JsonObject) element).has("Topography-Preset"))
                                {
                                	final ConfigPreset preset = ConfigurationManager.getInstance().getPreset();
                                    
                                    if (preset.getDifficulty() != null)
                                    {
                                    	event.world.getWorldInfo().setDifficulty(preset.getDifficulty());
                                    }
                                    
                                    if (preset.shouldLockDifficulty())
                                    {
                                    	event.world.getWorldInfo().setDifficultyLocked(true);
                                    }
                                	
                                    if (preset.hardcore())
                                    {
                                    	event.world.getWorldInfo().setHardcore(true);
                                    }
                                	
                                	final List<ResourceLocation> functions = ConfigurationManager.getInstance().getPreset().getInitialServerFunctions();
                                	
                                	for (final ResourceLocation function : functions)
                                	{
                                		final MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance().getServer();
                            			
                                        FunctionObject functionobject = server.getFunctionManager().getFunction(function);
                                        
                                        if (functionobject != null)
                                        {
                                        	Topography.instance.getLog().info("Running initial server function: " + function);
        	                                server.getFunctionManager().execute(functionobject, CommandSenderWrapper.create(server).computePositionVector().withPermissionLevel(2).withSendCommandFeedback(false));
                                        }
                                	}
//                        			if (event.world.provider instanceof WorldProviderConfigurable)
//                        			{
//                        				final SpawnStructure structure = ((WorldProviderConfigurable)event.world.provider).getDefinition().getSpawnStructure();
//                                        
//                                        if (structure != null)
//                                        {
//                                            for (int x = -3; x < 4; x++)
//                                            {
//                                                for (int z = -4; z < 4; z++)
//                                                {
//                                                    event.world.getChunkProvider().provideChunk(x, z);
//                                                }
//                                            }
//                                            
//                                            final Template template = IOHelper.loadStructureTemplate(structure.getStructure());
//                        
//                                            Topography.instance.getLog().info("Spawning structure");
//                        
//                                            template.addBlocksToWorld(event.world, new BlockPos(0, structure.getHeight(), 0), new PlacementSettings(), 0);
//                                            
//                                            final BlockPos spawn = StructureHelper.getSpawn(template);
//                                            
//                                            if (spawn != null)
//                                            {
//                                                event.world.getGameRules().setOrCreateGameRule("spawnRadius", "0");
//                                                event.world.getWorldInfo().setSpawn(spawn.add(0, structure.getHeight(), 0));
////                                                event.setCanceled(true);
//                                            }
//                                        }
//                        			}
                        			for (final int dimension : ConfigurationManager.getInstance().getPreset().getDimensions())
                        			{
                        				String script = ConfigurationManager.getInstance().getPreset().getScript(dimension);
                        	            DimensionDefinition definition = new DimensionDefinition();
                        	            IOHelper.loadDimensionDefinition(script, definition);
                        	            
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
                                                preloadArea += 4;
                                                Topography.instance.getLog().info("Preloading " + ((preloadArea * 2 + 1) * (preloadArea * 2 + 1)) + " chunks for spawn structure in dimension " + dimension);
                            	            	
                            	            	for (int x = -preloadArea; x < preloadArea; x++)
                                                {
                                                    for (int z = -preloadArea; z < preloadArea; z++)
                                                    {
                                                       dimWorld.getChunkProvider().provideChunk(x, z);
                                                    }
                                                }
                            	            	Topography.instance.getLog().info("Spawning structure for dimension " + dimension);
                            
                                                template.addBlocksToWorld(dimWorld, new BlockPos(0, structure.getHeight(), 0), new PlacementSettings(), 2);
                                                
                                                if (dimension == 0)
                                                {
                                                    final BlockPos spawn = StructureHelper.getSpawn(template);
                                                    
                                                    if (spawn != null)
                                                    {
                                                        dimWorld.getGameRules().setOrCreateGameRule("spawnRadius", "0");
                                                        dimWorld.getWorldInfo().setSpawn(spawn.add(0, structure.getHeight(), 0));
                                                    }
                                                }
                                            }
                        	            }
                        			}
                                }
                            }
                    	}
    				}
                }
    		}
    	}
    }
    
    private boolean teleporting = false;
    
    @SubscribeEvent
    public void onDimensionChange(final EntityTravelToDimensionEvent event)
    {
    	if (!teleporting)
    	{
        	final World world = event.getEntity().getEntityWorld().getMinecraftServer().getWorld(event.getDimension());
        	
        	final ConfigurationManager manager = ConfigurationManager.getInstance();
        	
        	if (manager != null)
        	{
            	final ConfigPreset preset = manager.getPreset();
            	
            	if (preset != null)
                {
                	final DimensionDefinition dimensionDef = preset.getDefinition(world.provider.getDimension());
                	
                	if (dimensionDef != null)
                	{
                		if (dimensionDef.shouldCaptureTeleports())
                        {
                        	if (event.getEntity() instanceof EntityPlayer)
                        	{
                        		final EntityPlayer player = (EntityPlayer) event.getEntity();
                        		
                        		BlockPos blockpos = player.getBedLocation(event.getDimension());
                                boolean forced = player.isSpawnForced(event.getDimension());
                        		
                        		if (blockpos != null)
                                {
                                    BlockPos blockpos1 = EntityPlayer.getBedSpawnLocation(world, blockpos, forced);

                                    if (blockpos1 != null)
                                    {
                                        event.setCanceled(true);
                                        this.teleporting = true;
                                        event.getEntity().changeDimension(event.getDimension(), new ReTeleporter(blockpos1));
                                        this.teleporting = false;
                                        return;
                                    }
                                }
                        	}
                        	
                        	
                            SpawnStructure structure = dimensionDef.getSpawnStructure();
                            
                            if (structure != null)
                            {
                                final Template template = IOHelper.loadStructureTemplate(structure.getStructure());

                                if (template != null)
                                {
                                    BlockPos spawn = StructureHelper.getSpawn(template);
                                    
                                    if (spawn != null)
                                    {
                                    	ITopographyPlayerData data = event.getEntity().getCapability(TopographyPlayerData.CAPABILITY_TOPOGRAPHY_PLAYER_DATA, null);
                                        if (data != null)
                                        {
                                            spawn = spawn.add(data.getIslandX(), 0, data.getIslandZ());
                                        }
                                    	
                                        event.setCanceled(true);
                                        this.teleporting = true;
                                        event.getEntity().changeDimension(event.getDimension(), new ReTeleporter(spawn.add(0, structure.getHeight(), 0)));
                                        this.teleporting = false;
                                    }
                                }
                            }
                            else
                            {
                            	for (final IGenerator generator : dimensionDef.getGenerators())
                                {
                                    if (generator instanceof SkyIslandGenerator)
                                    {
                                        final SkyIslandGenerator islandGenerator = (SkyIslandGenerator) generator;
                                        
                                        ITopographyPlayerData data = event.getEntity().getCapability(TopographyPlayerData.CAPABILITY_TOPOGRAPHY_PLAYER_DATA, null);
                                        if (data != null && (data.getIslandX() != 0 || data.getIslandZ() != 0))
                                        {
                                            final BlockPos pos = new BlockPos(data.getIslandX(), 0, data.getIslandZ());
                                            final BlockPos topBlock = IslandNew.getTopSolidOrLiquidBlock(world, pos);
                                            
                                            event.setCanceled(true);
                                            this.teleporting = true;
                                            event.getEntity().changeDimension(event.getDimension(), new ReTeleporter(topBlock.up()));
                                            this.teleporting = false;
                                            return;
                                        }
                                        
                                        final Iterator<Entry<SkyIslandData, Map<BlockPos, SkyIslandType>>> iterator = islandGenerator.getIslandPositions(world.getSeed(), 0, 0).entrySet().iterator();
                                        
                                        if (iterator.hasNext())
                                        {
                                            final Entry<SkyIslandData, Map<BlockPos, SkyIslandType>> islands = iterator.next();
                                            
                                            final Iterator<Entry<BlockPos, SkyIslandType>> positions = islands.getValue().entrySet().iterator();
                                            
                                            if (positions.hasNext())
                                            {
                                                final Entry<BlockPos, SkyIslandType> island = positions.next();
                                                
                                                final BlockPos pos = island.getKey();
                                                final BlockPos topBlock = IslandNew.getTopSolidOrLiquidBlock(world, pos);
                                                
                                                event.setCanceled(true);
                                                this.teleporting = true;
                                                event.getEntity().changeDimension(event.getDimension(), new ReTeleporter(topBlock.up()));
                                                this.teleporting = false;
                                            }
                                        }
                                        break;
                                    }
                                }
                            }
                        }
                	}
                }
        	}
    	}
    }

    public static class ReTeleporter implements ITeleporter
    {
        private final BlockPos targetPos;

        public ReTeleporter(BlockPos targetPos)
        {
            this.targetPos = targetPos;
        }

        @Override
        public void placeEntity(World world, Entity entity, float yaw)
        {
            entity.moveToBlockPosAndAngles(targetPos, yaw, entity.rotationPitch);
        }
    }
    
    @SubscribeEvent
    public void onAttachCapability(final AttachCapabilitiesEvent<Entity> event)
    {
    	if (event.getObject() instanceof EntityPlayerMP)
    	{
    		event.addCapability(TopographyPlayerData.Provider.location, new TopographyPlayerData.Provider());
    	}
    }
    
    @SubscribeEvent
    public void onPlayerClone(final PlayerEvent.Clone event)
    {
    	if (!event.getEntityPlayer().world.isRemote)
    	{
    		if (event.getOriginal().isDead)
    		{
    			ITopographyPlayerData orgData = event.getOriginal().getCapability(TopographyPlayerData.CAPABILITY_TOPOGRAPHY_PLAYER_DATA, null);
    			ITopographyPlayerData newData = event.getEntityPlayer().getCapability(TopographyPlayerData.CAPABILITY_TOPOGRAPHY_PLAYER_DATA, null);
    			
    			newData.setIsland(orgData.getIslandX(), orgData.getIslandZ());
    		}
    	}
    }
    
    @SubscribeEvent
    public void registerBlocks(final RegistryEvent.Register<Block> event)
    {
    	event.getRegistry().register(new BlockPortal() {
    		
    		@Override
    		public boolean trySpawnPortal(World world, BlockPos pos) {
    			
    			final ConfigurationManager manager = ConfigurationManager.getInstance();
            	
            	if (manager != null)
            	{
                	final ConfigPreset preset = manager.getPreset();
                	
                	if (preset != null)
                    {
                    	final DimensionDefinition dimensionDef = preset.getDefinition(world.provider.getDimension());
                    	
                    	if (dimensionDef != null)
                    	{
                    		if (dimensionDef.shouldDisableNetherPortal() || ConfigurationManager.getInstance().getPreset().shouldDisableNetherPortal())
                        	{
                        		return false;
                        	}
                    	}
                    }
            	}
    			return super.trySpawnPortal(world, pos);
    		}
    	}.setRegistryName("minecraft:portal"));
    }
}
