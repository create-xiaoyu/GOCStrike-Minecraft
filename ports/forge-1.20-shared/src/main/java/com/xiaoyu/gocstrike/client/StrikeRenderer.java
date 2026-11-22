package com.xiaoyu.gocstrike.client;

import java.io.IOException;

import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.BufferBuilder;
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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterShadersEvent;

@EventBusSubscriber(modid = GOCStrike.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class StrikeRenderer extends EntityRenderer<StrikeEntity> {
    private static StrikeRenderer current;
    private final EmplacementAnimation animation;
    private final Matrix4f root = new Matrix4f().scaling(StrikeEntity.WORLD_SCALE);
    private final Matrix4f modelView = new Matrix4f();
    private final VertexBuffer[] frameBuffers = new VertexBuffer[3];
    private BufferBuilder staging;
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
        event.registerEntityRenderer(GOCStrike.DAMOCLES.get(), DamoclesRenderer::new);
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

    /** 每帧生成一份实体局部网格，供世界本体和泛光共用。 */
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

    static void appendDamoclesCore(BufferBuilder builder, Matrix4f root, float authoredTime, float spinTime,
            float whiteMatter, float impactKick, float scatter) {
        if (current == null) {
            return;
        }
        var nodes = current.animation.sampleCore(authoredTime, root);
        var star = nodes[EmplacementAnimation.CORE_STAR_NODE];
        // 环层沿用原本的展开时间。白色中心另取原动画中已成形的那一拍。
        float heightOffset = -star.pose[1] * root.m11();
        if (whiteMatter > 0) {
            current.animation.sampleCoreStar(Math.max(authoredTime, 84.1F), root);
        }
        float starHeightOffset = -star.pose[1] * root.m11();
        var layerSpin = new Matrix4f();
        var fragment = new Matrix4f();
        int layer = -1;
        for (int i = 0; i < EmplacementAnimation.coreNodeCount(); i++) {
            var node = nodes[i];
            if (node.parent == 0) {
                // 延续原动画各层交替的转向，同时绕法阵中心增加少量转速。
                layer++;
                float direction = layer % 2 == 0 ? 1 : -1;
                float angle = (spinTime * (0.04F + 0.006F * layer) + impactKick * 0.085F) * direction;
                // 余震先把各层沿径向撑开并拉高层间距，随后才进入逐块分解。
                float spread = 1 + impactKick * (0.08F - layer * 0.008F);
                float recoil = impactKick * (layer - 2.5F) * 0.44F;
                layerSpin.translation(root.m30(), recoil, root.m32()).rotateY(angle).scale(spread, 1, spread)
                        .translate(-root.m30(), 0, -root.m32());
            }
            boolean inner = i >= EmplacementAnimation.CORE_STAR_NODE;
            if (!node.visible || node.vertices == null || node.pose[13] <= 0 || (inner && whiteMatter <= 0)) {
                continue;
            }
            float phase = i * 2.399963F;
            float drift = scatter * (1.4F + (i % 7) * 0.35F);
            fragment.set(layerSpin).mul(node.matrix);
            fragment.m31(fragment.m31() + (inner ? starHeightOffset : heightOffset));
            if (scatter > 0) {
                fragment.m30(fragment.m30() + (float) Math.cos(phase) * drift);
                fragment.m31(fragment.m31() + scatter * ((i % 5) - 1) * 0.65F);
                fragment.m32(fragment.m32() + (float) Math.sin(phase) * drift);
                fragment.rotateY(scatter * ((i % 3) - 1)).scale(1 - scatter * 0.85F);
            }
            float[] color = node.pose;
            float[] vertices = node.vertices;
            float alpha = color[13] * (1 - scatter) * (inner ? whiteMatter : 1);
            for (int vertex = 0; vertex < vertices.length; vertex += 3) {
                builder.vertex(fragment, vertices[vertex], vertices[vertex + 1], vertices[vertex + 2])
                        .color(color[10], color[11], color[12], alpha).endVertex();
            }
        }
    }

    private void prepare(float time) {
        if (Float.compare(time, preparedTime) == 0) {
            return;
        }
        if (staging == null) {
            // 预留完整模型的空间，降低高峰阶段重新分配缓冲的开销。
            staging = new BufferBuilder(3 * 1024 * 1024);
        }
        staging.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        for (var node : animation.sample(time, root)) {
            if (!node.visible || node.vertices == null || node.pose[13] <= 0) {
                continue;
            }
            float[] p = node.pose;
            float[] vertices = node.vertices;
            for (int i = 0; i < vertices.length; i += 3) {
                staging.vertex(node.matrix, vertices[i], vertices[i + 1], vertices[i + 2])
                        .color(p[10], p[11], p[12], p[13]).endVertex();
            }
        }
        var mesh = staging.endOrDiscardIfEmpty();
        hasVertices = mesh != null;
        if (hasVertices) {
            // 轮换缓冲区，避免覆盖 GPU 仍在读取的网格。
            if (frameBuffers[nextBuffer] == null) {
                frameBuffers[nextBuffer] = ForgeRenderStage.createVertexBuffer();
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
        current.staging = null;
        current.preparedBuffer = null;
        current.preparedTime = Float.NaN;
        current.hasVertices = false;
        current.nextBuffer = 0;
    }

    @Override
    public ResourceLocation getTextureLocation(StrikeEntity entity) {
        return GOCStrike.id("textures/item/thaumaturgic_strike.png");
    }
}
