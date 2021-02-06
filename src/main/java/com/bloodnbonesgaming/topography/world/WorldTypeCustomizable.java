package com.bloodnbonesgaming.topography.world;

import java.util.Map.Entry;

import com.bloodnbonesgaming.topography.Topography;
import com.bloodnbonesgaming.topography.client.gui.GuiCustomizeWorldType;
import com.bloodnbonesgaming.topography.config.ConfigPreset;
import com.bloodnbonesgaming.topography.config.ConfigurationManager;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.client.gui.GuiCreateWorld;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldType;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class WorldTypeCustomizable extends WorldType
{

    public WorldTypeCustomizable(String name)
    {
        super(name);
    }
    
//    @SideOnly(Side.CLIENT)
//    @Override
//    public void onCustomizeButton(net.minecraft.client.Minecraft mc, net.minecraft.client.gui.GuiCreateWorld guiCreateWorld)
//    {
//        mc.displayGuiScreen(new GuiCustomizeWorldType(guiCreateWorld));
//        WorldTypeCustomizable.gui = guiCreateWorld;
//    }

    @Override
    public boolean isCustomizable()
    {
        return false;
    }
    
    @Override
    public int getSpawnFuzz(WorldServer world, MinecraftServer server)
    {
        return 0;
    }
    
    @SideOnly(Side.CLIENT)
    @Override
    public String getTranslationKey()
    {
        return this.getName();
    }
    
    public static GuiCreateWorld gui = null;
    
    @Override
    public void onGUICreateWorldPress()
    {        
        if (gui.chunkProviderSettingsJson.isEmpty())
        {
            gui.chunkProviderSettingsJson = "{\"Topography-Preset\":\"" + ConfigurationManager.getInstance().getPreset().getName() + "\"}";
        }
        String settings = gui.chunkProviderSettingsJson;
        ConfigurationManager.setup();
        final JsonParser parser = new JsonParser();
        
        try {
            Topography.instance.getLog().info("reading json " + settings);
            JsonElement element = parser.parse(settings);
            
            if (element.isJsonObject())
            {
                Topography.instance.getLog().info("Is obj");
                JsonObject obj = (JsonObject) element;
                JsonElement member = obj.get("Topography-Preset");
                if (member != null)
                {
                    Topography.instance.getLog().info("Has member");
                    settings = member.getAsString();
                }
            }
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        if (settings.isEmpty())
        {
            for (final Entry<String, ConfigPreset> entry : ConfigurationManager.getInstance().getPresets().entrySet())
            {
                if (!entry.getValue().locked())
                {
                    settings = entry.getKey();
                    break;
                }
            }
        }
        ConfigurationManager.setGeneratorSettings(settings);
//        ConfigurationManager.getInstance().registerDimensions();
        
        final ConfigPreset preset = ConfigurationManager.getInstance().getPreset();
        final String type = preset.getWorldType();
        WorldType worldType = null;
        
        if (type != null)
        {
            worldType = WorldType.byName(type);
            
            if (worldType != null)
            {
                for (int i = 0; i < WorldType.WORLD_TYPES.length; i++)
                {
                    if (WorldType.WORLD_TYPES[i] == worldType)
                    {
                        gui.selectedIndex = i;
                        break;
                    }
                }
            }
        }
        final String options = preset.getGeneratorOptions();
        
        if (options != null)
        {
            Topography.instance.getLog().info("reading json " + options);
            JsonElement element = parser.parse(options);
            if (element.isJsonObject())
            {
                Topography.instance.getLog().info("Is obj");
                JsonObject obj = (JsonObject) element;
                
                if (!obj.has("Topography-Preset"))
                {
                    obj.addProperty("Topography-Preset", preset.getName());
                    String newOptions = obj.toString();
                    Topography.instance.getLog().info("Setting options: " + newOptions);
                    gui.chunkProviderSettingsJson = newOptions;
                }
            }
        }
        if (preset.hardcore())
        {
            gui.hardCoreMode = true;
            gui.gameMode = "hardcore";
        }
//        gui.chunkProviderSettingsJson = "{\"Topography-Preset\":\"" + settings + "\",\"schema\":\"test002\"}";
        
        if (worldType != null)
        {
            worldType.onGUICreateWorldPress();
        }
//        gui = null;
    }
}