package com.xiaoyu.gocstrike.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

final class ForgeItemRendering {
    private ForgeItemRendering() {
    }

    static void drawSword(ItemRenderer renderer, ItemStack sword, PoseStack poses, MultiBufferSource buffers,
            Level level, int entityId) {
        renderer.renderStatic(sword, ItemDisplayContext.NONE, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                poses, buffers, level, entityId);
    }
}
