package com.xiaoyu.gocstrike.client;

import java.io.IOException;

import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
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
    private final EmplacementAnimation animation;
    private final Matrix4f root = new Matrix4f();

    public StrikeRenderer(EntityRendererProvider.Context context) {
        super(context);
        try {
            animation = new EmplacementAnimation();
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load thaumaturgic strike animation", e);
        }
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
        float time = entity.elapsed(partialTick);
        if (time >= StrikeEntity.DURATION) {
            return;
        }
        root.set(poses.last().pose()).scale(StrikeEntity.WORLD_SCALE);
        drawEmissive(entity, partialTick, root, buffers.getBuffer(StrikeRenderTypes.STROKES));
    }

    /** Shared geometry for the sharp world pass and the isolated bloom mask. */
    public void drawEmissive(StrikeEntity entity, float partialTick, Matrix4f transform, VertexConsumer consumer) {
        for (var node : animation.sample(entity.elapsed(partialTick), transform)) {
            if (!node.visible || node.vertices == null || node.pose[13] <= 0) {
                continue;
            }
            float[] p = node.pose;
            float[] vertices = node.vertices;
            for (int i = 0; i < vertices.length; i += 3) {
                consumer.addVertex(node.matrix, vertices[i], vertices[i + 1], vertices[i + 2])
                        .setColor(p[10], p[11], p[12], p[13]);
            }
        }
    }

    @Override
    public ResourceLocation getTextureLocation(StrikeEntity entity) {
        return ResourceLocation.fromNamespaceAndPath(GOCStrike.MODID, "textures/item/thaumaturgic_strike.png");
    }
}
