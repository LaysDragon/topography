package com.bloodnbonesgaming.topography.proxy;

import com.bloodnbonesgaming.topography.Topography;
import com.bloodnbonesgaming.topography.config.ConfigurationManager;
import com.bloodnbonesgaming.topography.event.ServerEventSubscriber;
import com.bloodnbonesgaming.topography.world.WorldTypeCustomizable;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLServerAboutToStartEvent;

public class ServerProxy extends CommonProxy
{
    @Override
    public void registerEventHandlers()
    {
        super.registerEventHandlers();
        MinecraftForge.EVENT_BUS.register(new ServerEventSubscriber());
    }
    
    @Override
    public void onServerAboutToStart(FMLServerAboutToStartEvent event)
    {
        super.onServerAboutToStart(event);
        
        if (event.getServer() instanceof DedicatedServer && event.getServer().isDedicatedServer())
        {
            final DedicatedServer server = (DedicatedServer) event.getServer();
            
//            String s1 = server.getStringProperty("level-type", "DEFAULT");
//            WorldType worldType = WorldType.parseWorldType(s1);
//            
//            if (worldType instanceof WorldTypeCustomizable)
//            {
//                ConfigurationManager.setup();
//                
//                String backup = "";
//                
//                for (final String name : ConfigurationManager.getInstance().getPresets().keySet())
//                {
//                    backup = name;
//                    break;
//                }
                
                String settings = server.getStringProperty("generator-settings", "");
                
                if (!settings.isEmpty())
                {
                    final JsonParser parser = new JsonParser();
                    Topography.instance.getLog().info("reading json " + settings);
                    JsonElement element = parser.parse(settings);
                    if (element.isJsonObject())
                    {
                        JsonObject obj = (JsonObject) element;
                        JsonElement member = obj.get("Topography-Preset");
                        if (member != null)
                        {
                            settings = member.getAsString();
                        }
                    }
                    ConfigurationManager.setGeneratorSettings(settings);
                    ConfigurationManager.getInstance().registerDimensions();
                }
//            }
        }
    }
}
