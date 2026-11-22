package com.xiaoyu.gocstrike.client;

import java.util.ArrayList;
import java.util.List;

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
import com.xiaoyu.gocstrike.DamoclesEntity;
import com.xiaoyu.gocstrike.DamoclesTimeline;
import com.xiaoyu.gocstrike.GOCStrike;
import com.xiaoyu.gocstrike.StrikeEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.CompiledShaderProgram;
import net.minecraft.client.renderer.CoreShaders;
import net.minecraft.client.renderer.ShaderDefines;
import net.minecraft.client.renderer.ShaderProgram;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/** 对特效几何单独执行屏幕空间泛光。 */
@EventBusSubscriber(modid = GOCStrike.MODID, value = Dist.CLIENT)
public final class StrikeBloom {
    private static final ShaderProgram BLUR = new ShaderProgram(
            ResourceLocation.fromNamespaceAndPath(GOCStrike.MODID, "core/bloom_blur"),
            DefaultVertexFormat.POSITION_TEX, ShaderDefines.EMPTY);
    private static final ShaderProgram COMPOSITE = new ShaderProgram(
            ResourceLocation.fromNamespaceAndPath(GOCStrike.MODID, "core/bloom_composite"),
            DefaultVertexFormat.POSITION_TEX, ShaderDefines.EMPTY);
    private static RenderTarget mask;
    private static RenderTarget halfScratch;
    private static RenderTarget halfGlow;
    private static RenderTarget quarterScratch;
    private static RenderTarget quarterGlow;
    private static final Matrix4f TRANSFORM = new Matrix4f();

    private StrikeBloom() {
    }

    static boolean isReady() {
        var shaders = Minecraft.getInstance().getShaderManager();
        return shaders.getProgram(BLUR) != null && shaders.getProgram(COMPOSITE) != null;
    }

    public static void registerShaders(RegisterShadersEvent event) {
        event.registerShader(BLUR);
        event.registerShader(COMPOSITE);
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
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL || !isReady()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            releaseTargets();
            StrikeRenderer.releaseGeometry();
            return;
        }
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        var camera = event.getCamera().getPosition();
        var dispatcher = minecraft.getEntityRenderDispatcher();
        StrikeEntity activeStrike = null;
        StrikeRenderer strikeRenderer = null;
        List<DamoclesEntity> swords = new ArrayList<>();
        for (var entity : minecraft.level.entitiesForRendering()) {
            if (activeStrike == null && entity instanceof StrikeEntity strike && !strike.isRemoved() && strike.hasStarted()
                    && strike.elapsed(partialTick) < StrikeEntity.DURATION
                    && strike.position().distanceToSqr(camera) < 512 * 512
                    && dispatcher.getRenderer(strike) instanceof StrikeRenderer renderer) {
                activeStrike = strike;
                strikeRenderer = renderer;
            } else if (entity instanceof DamoclesEntity sword && !sword.isRemoved()
                    && sword.renderTime(partialTick) < DamoclesTimeline.END
                    && dispatcher.getRenderer(sword) instanceof DamoclesRenderer renderer
                    && renderer.shouldRender(sword, event.getFrustum(), camera.x, camera.y, camera.z)) {
                swords.add(sword);
            }
        }
        if (activeStrike == null) {
            StrikeRenderer.releaseGeometry();
        }
        if (activeStrike == null && swords.isEmpty()) {
            releaseTargets();
            return;
        }

        RenderTarget main = minecraft.getMainRenderTarget();
        CompiledShaderProgram previousShader = RenderSystem.getShader();
        int previousTexture = RenderSystem.getShaderTexture(0);
        float[] previousColor = RenderSystem.getShaderColor().clone();
        var modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        try {
            ensureTargets(main.width, main.height);
            mask.clear();
            // 复制场景深度，让墙体遮挡光源，并允许模糊后的光晕越过遮挡边缘。
            mask.copyDepthFrom(main);
            mask.bindWrite(true);
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.enableCull();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1, 1, 1, 1);
            RenderSystem.setShader(CoreShaders.POSITION_COLOR);
            modelView.mul(event.getModelViewMatrix());

            // 只给剑的光晕染蓝，保留主画面里的白色剑芯和 OBJ 的细节配色。
            RenderSystem.setShaderColor(0.20F, 0.45F, 1.0F, 1.0F);
            drawSwords(minecraft, swords, partialTick, camera);
            RenderSystem.setShaderColor(1, 1, 1, 1);
            if (strikeRenderer != null) {
                TRANSFORM.translation((float) (activeStrike.getX() - camera.x),
                        (float) (activeStrike.getY() - camera.y), (float) (activeStrike.getZ() - camera.z));
                strikeRenderer.drawEmissive(activeStrike, partialTick, TRANSFORM);
            }

            // NeoForge 在 LevelRenderer 返回后发出 AFTER_LEVEL，此时 Iris 已完成
            // finalizeLevelRendering。使用未被光影替换的原版着色器绘制清晰本体，
            // 与下面的光晕共享时序，避免只留下模糊光晕或再次受到光影曝光处理。
            // 奇术打击保留原有的透明绘制方式。剑身为不透明模型，需要写深度来遮挡背面的刃口。
            main.bindWrite(true);
            drawSwords(minecraft, swords, partialTick, camera);
            if (strikeRenderer != null) {
                strikeRenderer.drawEmissive(activeStrike, partialTick, TRANSFORM);
            }

            RenderSystem.disableDepthTest();
            RenderSystem.disableCull();
            RenderSystem.disableBlend();
            blur(mask, halfScratch, true);
            blur(halfScratch, halfGlow, false);
            blur(halfGlow, quarterScratch, true);
            blur(quarterScratch, quarterGlow, false);

            main.bindWrite(true);
            RenderSystem.enableBlend();
            // 滤色混合保留法阵本体的色相，避免多层光晕相加后泛白。
            RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR);
            composite(halfGlow, 0.85F);
            composite(quarterGlow, 1.25F);
        } finally {
            main.bindWrite(true);
            modelView.popMatrix();
            RenderSystem.setShader(previousShader);
            RenderSystem.setShaderTexture(0, previousTexture);
            RenderSystem.setShaderColor(previousColor[0], previousColor[1], previousColor[2], previousColor[3]);
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            RenderSystem.defaultBlendFunc();
        }
    }

    private static void drawSwords(Minecraft minecraft, List<DamoclesEntity> swords, float partialTick, Vec3 camera) {
        for (var sword : swords) {
            if (minecraft.getEntityRenderDispatcher().getRenderer(sword) instanceof DamoclesRenderer renderer) {
                renderer.drawEmissive(sword, partialTick, camera);
            }
        }
    }

    private static void blur(RenderTarget input, RenderTarget output, boolean horizontal) {
        output.bindWrite(true);
        var blurShader = RenderSystem.setShader(BLUR);
        RenderSystem.setShaderTexture(0, input.getColorTextureId());
        blurShader.safeGetUniform("Step").set(horizontal ? 1.5F / output.width : 0,
                horizontal ? 0 : 1.5F / output.height);
        quad();
    }

    private static void composite(RenderTarget glow, float strength) {
        var compositeShader = RenderSystem.setShader(COMPOSITE);
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
        RenderTarget target = new TextureTarget(width, height, depth);
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
