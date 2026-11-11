package com.xiaoyu.gocstrike.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;

public abstract class StrikeRenderTypes extends RenderType {
    public static final RenderType GLOW = create("gocstrike_glow", DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.TRIANGLES, 1 << 20, false, false,
            CompositeState.builder().setShaderState(POSITION_COLOR_SHADER)
                    .setTransparencyState(LIGHTNING_TRANSPARENCY).setCullState(NO_CULL)
                    .setWriteMaskState(COLOR_WRITE).createCompositeState(false));

    private StrikeRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int size,
            boolean affectsCrumbling, boolean sort, Runnable setup, Runnable clear) {
        super(name, format, mode, size, affectsCrumbling, sort, setup, clear);
    }
}
