package com.xiaoyu.gocstrike.client;

import java.io.IOException;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.xiaoyu.gocstrike.GOCStrike;
import com.xiaoyu.gocstrike.StrikeEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/** Selective screen-space bloom: only the thaumaturgic geometry supplies light. */
@EventBusSubscriber(modid = GOCStrike.MODID, value = Dist.CLIENT)
public final class StrikeBloom {
    private static ShaderInstance blurShader;
    private static ShaderInstance compositeShader;
    private static RenderTarget mask;
    private static RenderTarget halfScratch;
    private static RenderTarget halfGlow;
    private static RenderTarget quarterScratch;
    private static RenderTarget quarterGlow;
    private static final Matrix4f TRANSFORM = new Matrix4f();

    private StrikeBloom() {
    }

    public static void registerShaders(RegisterShadersEvent event) throws IOException {
        event.registerShader(new ShaderInstance(event.getResourceProvider(),
                ResourceLocation.fromNamespaceAndPath(GOCStrike.MODID, "bloom_blur"), DefaultVertexFormat.POSITION_TEX), shader -> {
                    blurShader = shader;
                    releaseTargets();
                });
        event.registerShader(new ShaderInstance(event.getResourceProvider(),
                ResourceLocation.fromNamespaceAndPath(GOCStrike.MODID, "bloom_composite"), DefaultVertexFormat.POSITION_TEX),
                shader -> compositeShader = shader);
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (Minecraft.getInstance().level == null) {
            releaseTargets();
            StrikeRenderer.releaseGeometry();
        }
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL || blurShader == null || compositeShader == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            releaseTargets();
            StrikeRenderer.releaseGeometry();
            return;
        }
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        StrikeEntity active = null;
        for (var entity : minecraft.level.entitiesForRendering()) {
            if (entity instanceof StrikeEntity strike && !strike.isRemoved() && strike.hasStarted()
                    && strike.elapsed(partialTick) < StrikeEntity.DURATION
                    && strike.position().distanceToSqr(event.getCamera().getPosition()) < 512 * 512) {
                active = strike;
                break;
            }
        }
        if (active == null) {
            releaseTargets();
            StrikeRenderer.releaseGeometry();
            return;
        }
        if (!(minecraft.getEntityRenderDispatcher().getRenderer(active) instanceof StrikeRenderer renderer)) {
            return;
        }

        RenderTarget main = minecraft.getMainRenderTarget();
        ShaderInstance previousShader = RenderSystem.getShader();
        int previousTexture = RenderSystem.getShaderTexture(0);
        float[] previousColor = RenderSystem.getShaderColor().clone();
        var modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        try {
            ensureTargets(main.width, main.height);
            mask.clear(Minecraft.ON_OSX);
            // Copy scene depth before rendering the hand. Solid walls still hide the source;
            // the blur may extend softly past an edge, as a camera bloom normally does.
            mask.copyDepthFrom(main);
            mask.bindWrite(true);
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.enableCull();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1, 1, 1, 1);
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            modelView.mul(event.getModelViewMatrix());
            RenderSystem.applyModelViewMatrix();

            var camera = event.getCamera().getPosition();
            TRANSFORM.translation((float) (active.getX() - camera.x), (float) (active.getY() - camera.y),
                    (float) (active.getZ() - camera.z));
            renderer.drawEmissive(active, partialTick, TRANSFORM);

            // NeoForge 在 LevelRenderer 返回后发出 AFTER_LEVEL，此时 Iris 已完成
            // finalizeLevelRendering。使用未被光影替换的原版着色器绘制清晰本体，
            // 与下面的光晕共享时序，避免只留下模糊光晕或再次受到光影曝光处理。
            // 主目标仍保留场景深度；不写深度，以免影响后续透明效果与手部绘制。
            main.bindWrite(true);
            renderer.drawEmissive(active, partialTick, TRANSFORM);

            RenderSystem.disableDepthTest();
            RenderSystem.disableCull();
            RenderSystem.disableBlend();
            blur(mask, halfScratch, true);
            blur(halfScratch, halfGlow, false);
            blur(halfGlow, quarterScratch, true);
            blur(quarterScratch, quarterGlow, false);

            main.bindWrite(true);
            RenderSystem.enableBlend();
            // Screen blending preserves the colored cores instead of adding them up to white.
            RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR);
            composite(halfGlow, 0.85F);
            composite(quarterGlow, 1.25F);
        } finally {
            main.bindWrite(true);
            modelView.popMatrix();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setShader(() -> previousShader);
            RenderSystem.setShaderTexture(0, previousTexture);
            RenderSystem.setShaderColor(previousColor[0], previousColor[1], previousColor[2], previousColor[3]);
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            RenderSystem.defaultBlendFunc();
        }
    }

    private static void blur(RenderTarget input, RenderTarget output, boolean horizontal) {
        output.bindWrite(true);
        RenderSystem.setShader(() -> blurShader);
        RenderSystem.setShaderTexture(0, input.getColorTextureId());
        blurShader.safeGetUniform("Step").set(horizontal ? 1.5F / output.width : 0,
                horizontal ? 0 : 1.5F / output.height);
        quad();
    }

    private static void composite(RenderTarget glow, float strength) {
        RenderSystem.setShader(() -> compositeShader);
        RenderSystem.setShaderTexture(0, glow.getColorTextureId());
        compositeShader.safeGetUniform("Strength").set(strength);
        quad();
    }

    private static void quad() {
        var builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        builder.addVertex(-1, -1, 0).setUv(0, 0);
        builder.addVertex(1, -1, 0).setUv(1, 0);
        builder.addVertex(1, 1, 0).setUv(1, 1);
        builder.addVertex(-1, 1, 0).setUv(0, 1);
        BufferUploader.drawWithShader(builder.buildOrThrow());
    }

    private static void ensureTargets(int width, int height) {
        if (mask != null && mask.width == width && mask.height == height) {
            return;
        }
        releaseTargets();
        mask = target(width, height, true);
        halfScratch = target(Math.max(1, width / 2), Math.max(1, height / 2), false);
        halfGlow = target(halfScratch.width, halfScratch.height, false);
        quarterScratch = target(Math.max(1, width / 4), Math.max(1, height / 4), false);
        quarterGlow = target(quarterScratch.width, quarterScratch.height, false);
    }

    private static RenderTarget target(int width, int height, boolean depth) {
        RenderTarget target = new TextureTarget(width, height, depth, Minecraft.ON_OSX);
        target.setClearColor(0, 0, 0, 0);
        target.setFilterMode(GL11.GL_LINEAR);
        return target;
    }

    private static void releaseTargets() {
        boolean allocated = mask != null || halfScratch != null || halfGlow != null || quarterScratch != null || quarterGlow != null;
        if (mask != null) {
            mask.destroyBuffers();
            mask = null;
        }
        if (halfScratch != null) {
            halfScratch.destroyBuffers();
            halfScratch = null;
        }
        if (halfGlow != null) {
            halfGlow.destroyBuffers();
            halfGlow = null;
        }
        if (quarterScratch != null) {
            quarterScratch.destroyBuffers();
            quarterScratch = null;
        }
        if (quarterGlow != null) {
            quarterGlow.destroyBuffers();
            quarterGlow = null;
        }
        if (allocated) {
            Minecraft.getInstance().getMainRenderTarget().bindWrite(true);
        }
    }
}
