package com.xiaoyu.gocstrike.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.vertex.VertexBuffer;
import net.minecraftforge.client.event.RenderLevelStageEvent.Stage;

final class ForgeRenderStage {
    static final Stage BLOOM = Stage.AFTER_LEVEL;

    private ForgeRenderStage() {
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
