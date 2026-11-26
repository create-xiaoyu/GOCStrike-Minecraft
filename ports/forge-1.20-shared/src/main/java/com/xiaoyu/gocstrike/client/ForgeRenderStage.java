package com.xiaoyu.gocstrike.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.vertex.VertexBuffer;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent.Stage;
import org.joml.Matrix4f;

final class ForgeRenderStage {
    private static final Matrix4f WORLD_VIEW = new Matrix4f();
    private static boolean worldViewCaptured;

    private ForgeRenderStage() {
    }

    static Matrix4f viewForBloom(RenderLevelStageEvent event) {
        if (event.getStage() == Stage.AFTER_ENTITIES) {
            // Forge 1.20.x 在 AFTER_LEVEL 的姿态栈里传入投影矩阵，因此提前保存本帧的世界视图。
            WORLD_VIEW.set(event.getPoseStack().last().pose());
            worldViewCaptured = true;
        } else if (event.getStage() == Stage.AFTER_LEVEL && worldViewCaptured) {
            worldViewCaptured = false;
            return WORLD_VIEW;
        }
        return null;
    }

    static VertexBuffer createVertexBuffer() {
        return new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
    }

    static int saveTarget() {
        return 0;
    }

    static void restoreTarget(int previousFramebuffer, RenderTarget main) {
        main.bindWrite(true);
    }
}
