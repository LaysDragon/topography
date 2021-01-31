package com.bloodnbonesgaming.topography.config;

import java.util.Random;

public class SkyIslandDataV2AutoExtend extends SkyIslandDataV2 {



    SkyIslandType lockType = null;

    public SkyIslandDataV2AutoExtend generate(Random random) {
        return this;
    }

    @Override
    public SkyIslandType getType(int index) {
        if (lockType != null) {
            return lockType;
        }
        return super.getType(index);
    }

    @Override
    public SkyIslandType getType(Random rand) {
        if (lockType != null) {
            return lockType;
        }
        return super.getType(rand);
    }


    public SkyIslandType getLockType() {
        return lockType;
    }

    public void setLockType(SkyIslandType lockType) {
        this.lockType = lockType;
    }

    protected SkyIslandDataV2AutoExtend copy() {
        SkyIslandDataV2AutoExtend newInstance = new SkyIslandDataV2AutoExtend();
        return this.copyProperty(newInstance);
    }

    protected SkyIslandDataV2AutoExtend copyProperty(SkyIslandDataV2AutoExtend data) {
        data.setBottomHeight(this.getBottomHeight());
        data.setCount(this.getCount());
        data.setFluidDepth(this.getFluidDepth());
        data.setHeightRange(this.getMinHeight(), this.getMaxHeight());
        data.setRadius(this.getRadius());
        data.setTopHeight(this.getTopHeight());
        data.setMinCount(this.getMinCount());
//        newInstance.setVerticalRadius(this.V);
        data.setHorizontalRadius((int) this.getHorizontalRadius());
        data.setRandomTypes(this.isRandomIslands());
        data.types.addAll(this.types);

        data.setLockType(this.getLockType());

        return data;
    }
}