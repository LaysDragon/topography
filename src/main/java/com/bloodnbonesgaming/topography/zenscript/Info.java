package com.bloodnbonesgaming.topography.zenscript;

import com.bloodnbonesgaming.topography.config.ConfigurationManager;

import crafttweaker.annotations.ZenRegister;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

@ZenClass("mods.topography.Info")
@ZenRegister
public class Info {
    
    @ZenMethod
    public static String getPreset() {

    	if (ConfigurationManager.getInstance() != null && ConfigurationManager.getInstance().getGeneratorSettings() != null)
    	{
    		return ConfigurationManager.getInstance().getGeneratorSettings();
    	}
    	return "";
    }
}
