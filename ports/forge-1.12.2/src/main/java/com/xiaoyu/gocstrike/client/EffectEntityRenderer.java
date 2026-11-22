package com.xiaoyu.gocstrike.client;

import com.xiaoyu.gocstrike.TimedEntity;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;

final class EffectEntityRenderer extends Render<TimedEntity> {
    EffectEntityRenderer(RenderManager manager) {
        super(manager);
    }

    @Override
    public boolean shouldRender(TimedEntity entity, ICamera camera, double x, double y, double z) {
        return false;
    }

    @Override
    protected ResourceLocation getEntityTexture(TimedEntity entity) {
        return null;
    }
}
