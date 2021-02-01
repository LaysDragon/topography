package com.bloodnbonesgaming.topography.config;

import com.bloodnbonesgaming.lib.util.script.ScriptClassDocumentation;
import com.bloodnbonesgaming.lib.util.script.ScriptMethodDocumentation;
import com.bloodnbonesgaming.topography.ModInfo;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.world.biome.Biome;
import org.millenaire.common.culture.Culture;
import org.millenaire.common.culture.VillageType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@ScriptClassDocumentation(documentationFile = ModInfo.SKY_ISLANDS_DOCUMENTATION_FOLDER + "SkyIslandDataV2MillenaireVillage", classExplaination =
        "This file is for the SkyIslandDataV2MillenaireVillage. This data object is for creating Millenaire island types for the SkyIslandGeneratorV2. "
                + "These can be created in a dimension file using 'new SkyIslandType(biomeID)', with the biome id being the biome you want the island to be, or 'new SkyIslandType()' to default to the void biome.")
public class SkyIslandDataV2MillenaireVillage extends SkyIslandDataV2AutoExtend {

//    String cultureName = "";
//    String villageName = "";


    MinMaxBounds resizeRatioBound = new MinMaxBounds(1F, 1.4F);
    VillageType villageType = null;


    public int getMaxBottomHeight() {
        return maxBottomHeight;
    }

    @ScriptMethodDocumentation(args = "double, double", usage = "lower, upper", notes = "Sets the lower/upper bound of island radius resize ratio")
    public void setMaxBottomHeight(int maxBBottomHeight) {
        this.maxBottomHeight = maxBottomHeight;
    }

    @ScriptMethodDocumentation(args = "double, double", usage = "lower, upper", notes = "Sets the lower/upper bound of island radius resize ratio")
    public double getMaxFluidPercentage() {
        return maxFluidPercentage;
    }

    @ScriptMethodDocumentation(args = "double, double", usage = "lower, upper", notes = "Sets the lower/upper bound of island radius resize ratio")
    public void setMaxFluidPercentage(double maxFluidPercentage) {
        this.maxFluidPercentage = maxFluidPercentage;
    }

    private int maxBottomHeight = 64;
    private double maxFluidPercentage = 0.4;


    @ScriptMethodDocumentation(args = "double, double", usage = "lower, upper", notes = "Sets the lower/upper bound of island radius resize ratio")
    public void setResizeRatioBound(MinMaxBounds bounds) {
        this.resizeRatioBound = bounds;
    }

    public MinMaxBounds getResizeRatioBound() {
        return this.resizeRatioBound;
    }


    public float getResizeRatioUpperBound() {
        return this.resizeRatioBound.max;
    }


    public float getResizeRatioLowerBound() {
        return this.resizeRatioBound.min;
    }


    public VillageType getVillageType() {
        return villageType;
    }

    public void setVillageType(VillageType villageType) {
        this.villageType = villageType;
    }

    public void setVillageType(String cultureName, String villageName) {
        this.villageType = Culture.getCultureByName(cultureName).getVillageType(villageName);

    }


    public SkyIslandDataV2MillenaireVillage() {
    }

//    public void setCultureName(String cultureName) {
//        this.cultureName = cultureName;
//    }
//
//    public void setVillageName(String villageName) {
//        this.villageName = villageName;
//    }

//    public String getCultureName() {
//        return cultureName;
//    }
//
//    public String getVillageName() {
//        return villageName;
//    }

    @Override
    public void addType(SkyIslandType type) {
        for (Culture culture : Culture.ListCultures) {
            for (VillageType villageType : culture.listVillageTypes) {
                if (villageType.weight > 0) {
                    if (villageType.biomes.contains(Biome.getBiome(type.getBiome()).getBiomeName().toLowerCase())) {
                        type = new SkyIslandType(type);
                        if (type.getFluidPercentage() > maxFluidPercentage) {
                            type.setFluidPercentage(maxFluidPercentage);
                        }
                        super.addType(type);
                        return;
                    }
                }
            }
        }
    }

    @Override
    public SkyIslandDataV2AutoExtend generate(Random random) {
        SkyIslandDataV2MillenaireVillage newData = (SkyIslandDataV2MillenaireVillage) this.copy();
        SkyIslandType islandType = this.getType(random);
        List<VillageType> validVillageTypes = new ArrayList<>();
        for (Culture culture : Culture.ListCultures) {
            for (VillageType villageType : culture.listVillageTypes) {
                if (villageType.biomes.contains(Biome.getBiome(islandType.getBiome()).getBiomeName().toLowerCase())) {
                    validVillageTypes.add(villageType);
                }
            }
        }
//        MillCommonUtilities.random = random;
        VillageType selectVillage = getVillageWithWeight(validVillageTypes, random);
        if (selectVillage == null) {
            return newData;
        }
//        VillageType selectVillage = (VillageType)MillCommonUtilities.getWeightedChoice(validVillageTypes, null);
        newData.setVillageType(selectVillage);
        newData.setLockType(islandType);
        newData.setHorizontalRadius((int) Math.floor(newData.getVillageType().radius * (random.nextDouble() * (newData.getResizeRatioUpperBound() - newData.getResizeRatioLowerBound()) + newData.getResizeRatioLowerBound()))); //0.8~1.4
        newData.setBottomHeight(Math.min(newData.getVillageType().radius, maxBottomHeight));
//        Biome.getBiome(this.types.get(0).getBiome()).getBiomeName().toLowerCase();


        return newData;
    }

    VillageType getVillageWithWeight(List<VillageType> villages, Random random) {
        int weightSum = 0;
        List<Integer> weights = new ArrayList<>();
        for (VillageType village : villages) {
            int choiceWeight = village.getChoiceWeight(null);
            weightSum += choiceWeight;
            weights.add(choiceWeight);
        }


        int r = random.nextInt(weightSum);
        int weightCount = 0;
        for (int i = 0; i < weights.size(); i++) {
            weightCount += weights.get(i);
            if (r < weightCount) {
                return villages.get(i);
            }

        }
        return villages.get(0);

    }

    @Override
    protected SkyIslandDataV2AutoExtend copy() {
        SkyIslandDataV2MillenaireVillage newInstance = new SkyIslandDataV2MillenaireVillage();
        return this.copyProperty(newInstance);

    }

    @Override
    protected SkyIslandDataV2AutoExtend copyProperty(SkyIslandDataV2AutoExtend data) {
        data = super.copyProperty(data);
        SkyIslandDataV2MillenaireVillage newInstance = (SkyIslandDataV2MillenaireVillage) data;
//        newInstance.setCultureName(this.getCultureName());
//        newInstance.setVillageName(this.getVillageName());
        newInstance.setVillageType(this.getVillageType());
        newInstance.setResizeRatioBound(this.getResizeRatioBound());
        newInstance.setMaxBottomHeight(this.getMaxBottomHeight());
        newInstance.setMaxFluidPercentage(this.getMaxFluidPercentage());
        return data;
    }
}
