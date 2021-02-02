package com.bloodnbonesgaming.topography.client.gui.element;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import com.bloodnbonesgaming.topography.Topography;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.IResource;
import net.minecraft.util.ResourceLocation;

public class GuiElementTextureAnimated extends GuiElementTextureStretch
{
    private int frames = 1;
    private int currentFrame = 0;
    private int updateTime = 5;
    private DynamicTexture gifTexture = null;
    
    public GuiElementTextureAnimated(EnumGuiLocation location, ResourceLocation texture)
    {
        super(location, texture);
        this.frames = this.imageHeight / this.imageWidth;
        this.imageHeight = this.imageWidth;
    }
    
    public void setUpdateTime(final int time)
    {
        this.updateTime = time;
    }
    
    public void clean()
    {
        if (this.gifTexture != null)
        {
            this.gifTexture.deleteGlTexture();
            this.gifTexture = null;
            this.currentFrame = 0;
        }
    }
    
    @Override
    public void render(Minecraft minecraft, int guiWidth, int guiHeight)
    {        
        if (this.texture.getPath().endsWith(".gif"))
        {
            if (this.gifTexture == null)
            {
                if (!this.loadAndTransformGIF(minecraft, this.texture))
                {
                    Topography.instance.getLog().info("Failed to load gif: " + this.texture.toString() + ".");
                    return;
                }
            }
            else
            {
                this.gifTexture.updateDynamicTexture();
            }
        }
        else
        {
            minecraft.getTextureManager().bindTexture(this.texture);
        }
        
        int currentFrame = (this.currentFrame / this.updateTime) % this.frames;
        
        this.drawTexture(this.location.getX(guiWidth, (int)(this.relRenderWidth * guiWidth + this.absRenderWidth)), this.location.getY(guiHeight, (int)(this.relRenderHeight * guiHeight + this.absRenderHeight)), this.imageWidth, this.imageHeight * this.frames, (int)(this.relRenderWidth * guiWidth + this.absRenderWidth), (int)(this.relRenderHeight * guiHeight + this.absRenderHeight), 0, this.imageHeight * currentFrame, this.imageWidth, this.imageHeight * currentFrame + this.imageHeight);
        
        this.currentFrame++;
    }
    
    private boolean loadAndTransformGIF(final Minecraft minecraft, final ResourceLocation location)
    {
        try (final IResource resource = minecraft.getResourceManager().getResource(location); final InputStream input = resource.getInputStream(); ImageInputStream imageStream = ImageIO.createImageInputStream(input))
        {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageStream);
            if(!readers.hasNext()) throw new IOException("No suitable reader found for image" + location);
            ImageReader reader = readers.next();
            reader.setInput(imageStream);
            
            int frames = reader.getNumImages(true);
            BufferedImage[] images = new BufferedImage[frames];
            for(int i = 0; i < frames; i++)
            {
                images[i] = reader.read(i);
            }
            reader.dispose();
            int width = images[0].getWidth();
            int height = images[0].getHeight();
            
            // Animation strip
            if (height > width && height % width == 0)
            {
                frames = height / width;
                BufferedImage original = images[0];
                height = width;
                images = new BufferedImage[frames];
                for (int i = 0; i < frames; i++)
                {
                    images[i] = original.getSubimage(0, i * height, width, height);
                }
            }
            
            this.frames = frames;
            final BufferedImage combined = this.combineBufferedImages(images);
            this.gifTexture = new DynamicTexture(combined);
            return true;
        }
        catch (Exception e)
        {
            Topography.instance.getLog().error("Could not load animated texture for " + location.toString());
        }
        return false;
    }
    
    private BufferedImage combineBufferedImages(final BufferedImage[] images)
    {
        int singleWidth = images[0].getWidth();
        int singleHeight = images[0].getHeight();
        int totalHeight = singleHeight * images.length;
        final BufferedImage masterImage = new BufferedImage(singleWidth, totalHeight, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D graphics = masterImage.createGraphics();
        
        for (int i = 0; i < images.length; i++)
        {
            graphics.drawImage(images[i], 0, singleHeight * i, null);
        }
        return masterImage;
    }
}