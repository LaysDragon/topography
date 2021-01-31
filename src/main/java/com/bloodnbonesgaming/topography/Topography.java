package com.bloodnbonesgaming.topography;

import java.io.IOException;

import com.bloodnbonesgaming.lib.BNBGamingMod;
import com.bloodnbonesgaming.lib.util.script.ScriptDocumentationHandler;
import com.bloodnbonesgaming.topography.command.TopographyTreeCommand;
import com.bloodnbonesgaming.topography.config.ConfigurationManager;
import com.bloodnbonesgaming.topography.event.CoreEventHandler;
import com.bloodnbonesgaming.topography.network.PacketSyncPreset;
import com.bloodnbonesgaming.topography.proxy.CommonProxy;
import com.bloodnbonesgaming.topography.util.capabilities.TopographyPlayerData;
import com.bloodnbonesgaming.topography.world.MillenaireVillageGen;
import com.bloodnbonesgaming.topography.world.WorldTypeCustomizable;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLLoadCompleteEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppedEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;
import org.millenaire.common.world.WorldGenVillage;

@Mod(modid = ModInfo.MODID, name = ModInfo.MOD_NAME, version = ModInfo.VERSION, dependencies = "required-after:bnbgaminglib@[2.14.0,);after:crafttweaker;after:worldbook;after:rtg;",
        acceptedMinecraftVersions = "[1.12,1.13)")
public class Topography extends BNBGamingMod
{
    @Instance(ModInfo.MODID)
    public static Topography instance;

    @SidedProxy(clientSide = ModInfo.CLIENT_PROXY, serverSide = ModInfo.SERVER_PROXY)
    public static CommonProxy proxy;
    
    public static final SimpleNetworkWrapper network = NetworkRegistry.INSTANCE.newSimpleChannel(ModInfo.MODID);
    
    public static boolean worldbook = false;
    public static boolean betterWithMods = false;
    public static boolean rtg = false;
    public static boolean millenaire = false;

    @EventHandler
    public void preInit(final FMLPreInitializationEvent event)
    {
    	if (Loader.isModLoaded("bnbgamingcore"))
		{
			MinecraftForge.EVENT_BUS.register(new CoreEventHandler());
		}
		if (Loader.isModLoaded("worldbook"))
        {
			Topography.worldbook = true;
        }
		if (Loader.isModLoaded("betterwithmods"))
        {
			Topography.betterWithMods = true;
        }
		if (Loader.isModLoaded("rtg"))
        {
			Topography.rtg = true;
        }

        if (Loader.isModLoaded("millenaire"))
        {
            Topography.millenaire = true;
        }

		ScriptDocumentationHandler.setScriptDocs(event.getAsmData());
        Topography.proxy.registerEventHandlers();
        TopographyPlayerData.register();
    }

    @EventHandler
    public void init(final FMLInitializationEvent event)
    {
        new WorldTypeCustomizable("topography");
        
        PermissionAPI.registerNode("topography.island.accept", DefaultPermissionLevel.ALL, "/topography island accept command");
        PermissionAPI.registerNode("topography.island.home", DefaultPermissionLevel.ALL, "/topography island home command");
        PermissionAPI.registerNode("topography.island.info", DefaultPermissionLevel.ALL, "/topography island info command");
        PermissionAPI.registerNode("topography.island.invite", DefaultPermissionLevel.ALL, "/topography island invite command");
        PermissionAPI.registerNode("topography.island.new", DefaultPermissionLevel.OP, "/topography island new command");
        PermissionAPI.registerNode("topography.island.set", DefaultPermissionLevel.OP, "/topography island set command");
        PermissionAPI.registerNode("topography.world.spawn", DefaultPermissionLevel.ALL, "/topography spawn command");
        PermissionAPI.registerNode("topography.preset.lock", DefaultPermissionLevel.NONE, "/topography lock command");
        PermissionAPI.registerNode("topography.preset.unlock", DefaultPermissionLevel.NONE, "/topography unlock command");

        if(Topography.millenaire) {
            GameRegistry.registerWorldGenerator(new MillenaireVillageGen(), 1000);
        }

    }

    @EventHandler
    public void postInit(final FMLPostInitializationEvent event)
    {
        network.registerMessage(PacketSyncPreset.class, PacketSyncPreset.class, 0, Side.CLIENT);
    }

    @EventHandler
    public void loadComplete(final FMLLoadCompleteEvent event)
    {
    }

    @EventHandler
    public void serverAboutToSTart(final FMLServerAboutToStartEvent event)
    {
        Topography.proxy.onServerAboutToStart(event);
    }

    @EventHandler
    public void serverStarting(final FMLServerStartingEvent event) throws IOException
    {
        event.registerServerCommand(new TopographyTreeCommand());
    }

    @EventHandler
    public void serverStarted(final FMLServerStartedEvent event) throws IOException
    {
    }

    @EventHandler
    public void serverStopped(final FMLServerStoppedEvent event)
    {
        ConfigurationManager.cleanUp();
    }
}