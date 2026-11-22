package com.xiaoyu.gocstrike.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.platform.GlStateManager;
import org.lwjgl.opengl.GL30;
import net.minecraftforge.client.event.RenderLevelStageEvent.Stage;

final class ForgeRenderStage {
    static final Stage BLOOM = Stage.AFTER_WEATHER;

    private ForgeRenderStage() {
    }

    static VertexBuffer createVertexBuffer() {
        return new VertexBuffer();
    }

    static int saveTarget() {
        // 极佳画质在此阶段使用独立天气缓冲，之后仍要往该缓冲渲染世界边界。
        return GL30.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
    }

    static void restoreTarget(int previousFramebuffer, RenderTarget main) {
        GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, previousFramebuffer);
    }
}
