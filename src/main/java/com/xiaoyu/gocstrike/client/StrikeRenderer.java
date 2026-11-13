package com.xiaoyu.gocstrike.client;

import java.io.IOException;

import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.systems.RenderSystem;
import com.xiaoyu.gocstrike.GOCStrike;
import com.xiaoyu.gocstrike.StrikeEntity;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;

@EventBusSubscriber(modid = GOCStrike.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class StrikeRenderer extends EntityRenderer<StrikeEntity> {
    private static StrikeRenderer current;
    private final EmplacementAnimation animation;
    private final Matrix4f root = new Matrix4f().scaling(StrikeEntity.WORLD_SCALE);
    private final Matrix4f modelView = new Matrix4f();
    private final VertexBuffer[] frameBuffers = new VertexBuffer[3];
    private ByteBufferBuilder staging;
    private VertexBuffer preparedBuffer;
    private float preparedTime = Float.NaN;
    private int nextBuffer;
    private boolean hasVertices;

    public StrikeRenderer(EntityRendererProvider.Context context) {
        super(context);
        try {
            animation = new EmplacementAnimation();
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load thaumaturgic strike animation", e);
        }
        releaseGeometry();
        current = this;
        GOCStrike.LOGGER.info("Loaded thaumaturgic strike: {} nodes", animation.nodeCount());
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(GOCStrike.STRIKE.get(), StrikeRenderer::new);
    }

    @SubscribeEvent
    public static void registerShaders(RegisterShadersEvent event) throws IOException {
        StrikeBloom.registerShaders(event);
    }

    @Override
    public void render(StrikeEntity entity, float yaw, float partialTick, PoseStack poses,
            MultiBufferSource buffers, int packedLight) {
        // 本体与光晕统一在 AFTER_LEVEL 绘制。实体阶段的原版着色器会被 Iris 替换，
        // 使本体进入光影的雾与后处理，而光晕仍在后处理结束后绘制，造成两者不一致。
        // 保留实体作为动画锚点；实际几何由 StrikeBloom 绘制并复用同一份 GPU 缓冲。
    }

    /** One entity-local GPU mesh per frame, reused unchanged by the world and bloom passes. */
    public void drawEmissive(StrikeEntity entity, float partialTick, Matrix4f transform) {
        prepare(entity.elapsed(partialTick));
        if (!hasVertices) {
            return;
        }
        modelView.set(RenderSystem.getModelViewMatrix()).mul(transform);
        preparedBuffer.bind();
        try {
            preparedBuffer.drawWithShader(modelView, RenderSystem.getProjectionMatrix(), RenderSystem.getShader());
        } finally {
            VertexBuffer.unbind();
        }
    }

    private void prepare(float time) {
        if (Float.compare(time, preparedTime) == 0) {
            return;
        }
        if (staging == null) {
            // Fits the full authored model without a buffer growth during the climax.
            staging = new ByteBufferBuilder(3 * 1024 * 1024);
        }
        var geometry = new BufferBuilder(staging, VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        for (var node : animation.sample(time, root)) {
            if (!node.visible || node.vertices == null || node.pose[13] <= 0) {
                continue;
            }
            float[] p = node.pose;
            float[] vertices = node.vertices;
            for (int i = 0; i < vertices.length; i += 3) {
                geometry.addVertex(node.matrix, vertices[i], vertices[i + 1], vertices[i + 2])
                        .setColor(p[10], p[11], p[12], p[13]);
            }
        }
        var mesh = geometry.build();
        hasVertices = mesh != null;
        if (hasVertices) {
            // Rotate buffers to avoid overwriting a mesh the GPU is still consuming.
            if (frameBuffers[nextBuffer] == null) {
                frameBuffers[nextBuffer] = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
            }
            preparedBuffer = frameBuffers[nextBuffer];
            preparedBuffer.bind();
            try {
                preparedBuffer.upload(mesh);
            } finally {
                VertexBuffer.unbind();
            }
            nextBuffer = (nextBuffer + 1) % frameBuffers.length;
        }
        preparedTime = time;
    }

    public static void releaseGeometry() {
        if (current == null) {
            return;
        }
        for (int i = 0; i < current.frameBuffers.length; i++) {
            if (current.frameBuffers[i] != null) {
                current.frameBuffers[i].close();
                current.frameBuffers[i] = null;
            }
        }
        if (current.staging != null) {
            current.staging.close();
            current.staging = null;
        }
        current.preparedBuffer = null;
        current.preparedTime = Float.NaN;
        current.hasVertices = false;
        current.nextBuffer = 0;
    }

    @Override
    public ResourceLocation getTextureLocation(StrikeEntity entity) {
        return ResourceLocation.fromNamespaceAndPath(GOCStrike.MODID, "textures/item/thaumaturgic_strike.png");
    }
}
