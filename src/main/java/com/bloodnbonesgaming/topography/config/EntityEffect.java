package com.bloodnbonesgaming.topography.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import com.bloodnbonesgaming.topography.Topography;
import com.bloodnbonesgaming.topography.util.EDamageSource;
import com.bloodnbonesgaming.topography.util.EntityType;
import com.bloodnbonesgaming.topography.util.ExtendedDamageSource;

import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

public class EntityEffect
{
    private final int frequency;
    private List<ResourceLocation> effectedEntities = new ArrayList<ResourceLocation>();
    private List<EntityType> types = new ArrayList<EntityType>();
    private MinMaxBounds lightLevel = MinMaxBounds.UNBOUNDED;
    private Boolean canSeeSky = null;
    

    
    private final Map<DamageSource, Integer> damageMap = new LinkedHashMap<DamageSource, Integer>();
    private final Map<String, AttributeModifier> attributes = new HashMap<String, AttributeModifier>();
    
    public EntityEffect(final int frequency)
    {
        this.frequency = frequency;
    }
    
    public void apply(EntityLivingBase entity)
    {
        if (this.frequency == 0 || entity.getEntityWorld().getTotalWorldTime() % this.frequency == 0)
        {
            if (this.acceptableEntity(entity))
            {
                if (this.acceptableLight(entity))
                {
                    if (this.acceptableSky(entity))
                    {
                        this.applyAttributes(entity);
                        this.applyDamage(entity);
                    }
                }
            }
        }
    }
    
    
    
    private boolean acceptableEntity(final EntityLivingBase entity)
    {
        if (!this.types.isEmpty())
        {
            for (final EntityType type : this.types)
            {
                if (type.isAcceptable(entity))
                {
                    return true;
                }
            }
            return false;
        }
        if (!this.effectedEntities.isEmpty())
        {
            for (final ResourceLocation location : this.effectedEntities)
            {
                if (location.getPath().equalsIgnoreCase("player"))
                {
                    if (entity instanceof EntityPlayer)
                    {
                        return true;
                    }
                }
                else
                {
                    final EntityEntry entry = ForgeRegistries.ENTITIES.getValue(location);
                    
                    if (entry != null)
                    {
                        if (entry.getEntityClass() == entity.getClass())
                        {
                            return true;
                        }
                    }
                }
            }
            return false;
        }
        return true;
    }
    
    private boolean acceptableLight(final EntityLivingBase entity)
    {
        if (this.lightLevel != MinMaxBounds.UNBOUNDED)
        {
            return this.lightLevel.test(entity.world.getLightFromNeighbors(entity.getPosition()));
        }
        return true;
    }
    
    private boolean acceptableSky(final EntityLivingBase entity)
    {
        if (this.canSeeSky != null)
        {
            if (entity.world.canSeeSky(entity.getPosition()) != this.canSeeSky)
            {
                return false;
            }
        }
        return true;
    }
    
    
    
    private void applyDamage(final EntityLivingBase entity)
    {
        for (final Entry<DamageSource, Integer> entry : this.damageMap.entrySet())
        {
            entity.attackEntityFrom(entry.getKey(), entry.getValue());
        }
    }
    
    public void applyAttributes(final EntityLivingBase entity)
    {
        for (final Entry<String, AttributeModifier> attribute : this.attributes.entrySet())
        {
            IAttributeInstance instance = entity.getAttributeMap().getAttributeInstanceByName(attribute.getKey());
            
            if (instance != null && !instance.hasModifier(attribute.getValue()))
            {
                instance.applyModifier(attribute.getValue());
                
                if (attribute.getKey().equals("generic.maxHealth"))
                {
                    entity.heal(entity.getMaxHealth());
                }
            }
        }
    }
    
    
    
    
    
    public void addEntityType(final String string) throws Exception
    {
        final EntityType type = EntityType.valueOf(string.toUpperCase());
        
        if (type != null)
        {
            this.types.add(type);
        }
        else
        {
            throw new Exception(string + " is not a valid entity type!");
        }
    }
    
    
    
    
    
    
    
    
    public void addDamage(final String sourceString, final int amount)
    {
        final EDamageSource source = EDamageSource.valueOf(sourceString.toUpperCase());
        
        if (source != null)
        {
            this.damageMap.put(source.getSource(), amount);
        }
        else
        {
            Topography.instance.getLog().info(sourceString + " is not a valid damage source!");
        }
    }
    
    public void addDamage(final DamageSource source, final int amount)
    {
        this.damageMap.put(source, amount);
    }
    
    public DamageSource addDamageSource(final String name, final String deathMessage, final int amount)
    {
        final ExtendedDamageSource source = new ExtendedDamageSource(name, deathMessage);
        this.damageMap.put(source, amount);
        return source;
    }
    
    public void setLightLevel(final Float min, final Float max)
    {
        this.lightLevel = new MinMaxBounds(min, max);
    }
    
    public void canSeeSky(final boolean bool)
    {
        this.canSeeSky = bool;
    }
    
    public void addAttribute(final String attribute, final String modifierName, final double amount, final int operation)
    {
        this.attributes.put(attribute, new AttributeModifier(UUID.nameUUIDFromBytes(modifierName.getBytes()), modifierName, amount, operation));
    }
}