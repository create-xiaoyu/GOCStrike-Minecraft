package com.xiaoyu.gocstrike.client;

import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

final class EffectEntityRenderer extends Render {
    @Override
    public void doRender(Entity entity, double x, double y, double z, float yaw, float partialTick) {
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return null;
    }
}
