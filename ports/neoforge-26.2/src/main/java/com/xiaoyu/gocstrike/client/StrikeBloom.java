package com.xiaoyu.gocstrike.client;

import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.platform.BlendFactor;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.textures.FilterMode;
import com.xiaoyu.gocstrike.DamoclesEntity;
import com.xiaoyu.gocstrike.DamoclesTimeline;
import com.xiaoyu.gocstrike.GOCStrike;
import com.xiaoyu.gocstrike.StrikeEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.model.standalone.SimpleUnbakedStandaloneModel;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

@EventBusSubscriber(modid = GOCStrike.MODID, value = Dist.CLIENT)
public final class StrikeBloom {
    private static final Vector4f CLEAR = new Vector4f();
    private static final Vector4f BLUE = new Vector4f(0.20F, 0.45F, 1, 1);
    private static final StandaloneModelKey<QuadCollection> SWORD = new StandaloneModelKey<>(() -> "GOC Strike sword");
    private static final RenderPipeline SOLID = geometryPipeline("solid", false, true);
    private static final RenderPipeline TRANSLUCENT = geometryPipeline("translucent", false, false);
    private static final RenderPipeline SEAL = geometryPipeline("seal", true, false);
    private static final RenderPipeline BLUR = screenPipeline("bloom_blur", ColorTargetState.DEFAULT);
    private static final RenderPipeline COMPOSITE = screenPipeline("bloom_composite", new ColorTargetState(
            Optional.of(new BlendFunction(BlendFactor.ONE, BlendFactor.ONE_MINUS_SRC_COLOR)),
            GpuFormat.RGBA8_UNORM, ColorTargetState.WRITE_COLOR));

    private static StrikeGeometry strikeGeometry;
    private static DamoclesGeometry damoclesGeometry;
    private static QuadCollection lastSword;
    private static DamoclesMesh swordMesh;
    private static ByteBufferBuilder solidBuffer;
    private static ByteBufferBuilder translucentBuffer;
    private static ByteBufferBuilder sealBuffer;
    private static ByteBufferBuilder strikeBuffer;
    private static TextureTarget mask;
    private static TextureTarget halfScratch;
    private static TextureTarget halfGlow;
    private static TextureTarget quarterScratch;
    private static TextureTarget quarterGlow;

    private StrikeBloom() {
    }

    static net.minecraft.world.phys.Vec3 cameraPosition(Minecraft game) {
        return game.gameRenderer.mainCamera().position();
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(GOCStrike.MODID, path);
    }

    private static RenderPipeline geometryPipeline(String name, boolean textured, boolean writeDepth) {
        String shader = textured ? "core/position_tex_color" : "core/position_color";
        var builder = RenderPipeline.builder()
                .withLocation(id("pipeline/" + name))
                .withVertexShader(shader)
                .withFragmentShader(shader)
                .withBindGroupLayout(BindGroupLayouts.PROJECTION)
                .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
                .withPrimitiveTopology(textured ? PrimitiveTopology.QUADS : PrimitiveTopology.TRIANGLES)
                .withVertexBinding(0, textured ? DefaultVertexFormat.POSITION_TEX_COLOR : DefaultVertexFormat.POSITION_COLOR)
                .withDepthStencilState(new DepthStencilState(DepthStencilState.DEFAULT.depthTest(), writeDepth))
                .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                .withCull(!textured);
        if (textured) {
            builder.withBindGroupLayout(BindGroupLayouts.SAMPLER0);
        }
        return builder.build();
    }

    private static RenderPipeline screenPipeline(String name, ColorTargetState blend) {
        return RenderPipeline.builder()
                .withLocation(id("pipeline/" + name))
                .withVertexShader("core/screenquad")
                .withFragmentShader(id("core/" + name))
                .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
                .withBindGroupLayout(BindGroupLayout.builder().withUniform("GocBloom", UniformType.UNIFORM_BUFFER).build())
                .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
                .withColorTargetState(blend)
                .withCull(false)
                .build();
    }

    @SubscribeEvent
    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        // 实体承担同步和跟踪。特效几何由 AfterLevel 统一绘制，便于与场景共用深度。
        event.registerEntityRenderer(GOCStrike.STRIKE.get(), NoopRenderer::new);
        event.registerEntityRenderer(GOCStrike.DAMOCLES.get(), NoopRenderer::new);
    }

    @SubscribeEvent
    public static void registerModels(ModelEvent.RegisterStandalone event) {
        event.register(SWORD, SimpleUnbakedStandaloneModel.quadCollection(id("item/damocles_sword")));
    }

    @SubscribeEvent
    public static void registerPipelines(RegisterRenderPipelinesEvent event) {
        for (RenderPipeline pipeline : List.of(SOLID, TRANSLUCENT, SEAL, BLUR, COMPOSITE)) {
            event.registerPipeline(pipeline);
        }
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (Minecraft.getInstance().level == null) {
            releaseTargets();
            releaseGeometry();
        }
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent.AfterLevel event) {
        Minecraft game = Minecraft.getInstance();
        if (game.level == null) {
            return;
        }
        var levelState = event.getLevelRenderState();
        var camera = levelState.cameraRenderState;
        float partialTick = game.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        BufferBuilder solid = null;
        BufferBuilder translucent = null;
        BufferBuilder seal = null;
        BufferBuilder strike = null;
        for (var entity : game.level.entitiesForRendering()) {
            if (entity.isRemoved()) {
                continue;
            }
            if (strike == null && entity instanceof StrikeEntity effect && effect.hasStarted()
                    && effect.elapsed(partialTick) < StrikeEntity.DURATION
                    && effect.position().distanceToSqr(camera.pos) < 512 * 512) {
                prepareGeometry();
                strike = colorBuilder(strikeBuffer);
                strikeGeometry.appendStrike(strike, effect, partialTick, camera.pos);
            } else if (entity instanceof DamoclesEntity sword && sword.renderTime(partialTick) < DamoclesTimeline.END
                    && sword.shouldRenderAtSqrDistance(sword.position().distanceToSqr(camera.pos))) {
                if (solid == null) {
                    prepareGeometry();
                    solid = colorBuilder(solidBuffer);
                    translucent = colorBuilder(translucentBuffer);
                    seal = new BufferBuilder(sealBuffer, PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
                    QuadCollection model = game.getModelManager().getStandaloneModel(SWORD);
                    if (model != lastSword) {
                        swordMesh = model == null ? null : new DamoclesMesh(model);
                        lastSword = model;
                    }
                }
                damoclesGeometry.append(sword, partialTick, camera.pos, swordMesh, solid, translucent, seal);
            }
        }
        if (solid == null && strike == null) {
            releaseTargets();
            releaseGeometry();
            return;
        }

        RenderTarget main = game.gameRenderer.mainRenderTarget();
        ensureTargets(main);
        mask.copyDepthFrom(main);
        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        var matrix = new Matrix4f(event.getModelViewMatrix());
        var plain = RenderSystem.getDynamicUniforms().writeTransform(matrix);
        var blue = RenderSystem.getDynamicUniforms().writeTransform(matrix, BLUE);

        try (Draw swordDraw = upload(solid); Draw effectsDraw = upload(translucent);
                Draw sealDraw = upload(seal); Draw strikeDraw = upload(strike)) {
            // 本体与泛光共享同一帧几何。墙体深度先复制到遮罩，模糊在遮罩上完成。
            renderGeometry(game, encoder, mask, true, plain, blue, swordDraw, effectsDraw, sealDraw, strikeDraw);
            renderGeometry(game, encoder, main, false, plain, plain, swordDraw, effectsDraw, sealDraw, strikeDraw);
            screen(encoder, BLUR, mask, halfScratch, 1F / mask.width, 0, 0);
            screen(encoder, BLUR, halfScratch, halfGlow, 0, 1F / halfScratch.height, 0);
            screen(encoder, BLUR, halfGlow, quarterScratch, 1F / halfGlow.width, 0, 0);
            screen(encoder, BLUR, quarterScratch, quarterGlow, 0, 1F / quarterScratch.height, 0);
            screen(encoder, COMPOSITE, halfGlow, main, 0, 0, 0.85F);
            screen(encoder, COMPOSITE, quarterGlow, main, 0, 0, 1.25F);
        }
    }

    private static BufferBuilder colorBuilder(ByteBufferBuilder buffer) {
        return new BufferBuilder(buffer, PrimitiveTopology.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
    }

    private static Draw upload(BufferBuilder builder) {
        if (builder == null) {
            return null;
        }
        try (var mesh = builder.build()) {
            return mesh == null ? null : new Draw(RenderSystem.getDevice().createBuffer(
                    () -> "GOC Strike vertices", GpuBuffer.USAGE_VERTEX, mesh.vertexBuffer()),
                    mesh.drawState().vertexCount(), mesh.drawState().indexCount());
        }
    }

    private static void renderGeometry(Minecraft game, CommandEncoder encoder, RenderTarget target, boolean clear,
            GpuBufferSlice plain, GpuBufferSlice swordColor, Draw sword, Draw effects, Draw seal, Draw strike) {
        var indices = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS);
        var sealIndices = seal == null ? null : indices.getBuffer(seal.indices);
        try (var pass = encoder.createRenderPass(() -> "GOC Strike geometry", target.getColorTextureView(),
                clear ? Optional.of(CLEAR) : Optional.empty(), target.getDepthTextureView(), OptionalDouble.empty())) {
            draw(pass, SOLID, sword, swordColor);
            draw(pass, TRANSLUCENT, effects, swordColor);
            if (seal != null) {
                pass.setPipeline(SEAL);
                pass.setUniform("Projection", RenderSystem.getProjectionMatrixBuffer());
                pass.setUniform("DynamicTransforms", swordColor);
                var texture = game.getTextureManager().getTexture(DamoclesGeometry.GOC_SEAL);
                pass.bindTexture("Sampler0", texture.getTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
                pass.setVertexBuffer(0, seal.vertices.slice());
                pass.setIndexBuffer(sealIndices, indices.type());
                pass.drawIndexed(seal.indices, 1, 0, 0, 0);
            }
            draw(pass, TRANSLUCENT, strike, plain);
        }
    }

    private static void draw(RenderPass pass, RenderPipeline pipeline, Draw geometry, GpuBufferSlice transform) {
        if (geometry != null) {
            pass.setPipeline(pipeline);
            pass.setUniform("Projection", RenderSystem.getProjectionMatrixBuffer());
            pass.setUniform("DynamicTransforms", transform);
            pass.setVertexBuffer(0, geometry.vertices.slice());
            pass.draw(geometry.count, 1, 0, 0);
        }
    }

    private static void screen(CommandEncoder encoder, RenderPipeline pipeline, RenderTarget input, RenderTarget output,
            float stepX, float stepY, float strength) {
        try (MemoryStack memory = MemoryStack.stackPush()) {
            var bytes = memory.malloc(16);
            bytes.putFloat(stepX).putFloat(stepY).putFloat(strength).putFloat(0).flip();
            try (GpuBuffer parameters = RenderSystem.getDevice().createBuffer(
                        () -> "GOC Strike bloom parameters", GpuBuffer.USAGE_UNIFORM, bytes);
                    var pass = encoder.createRenderPass(() -> "GOC Strike bloom", output.getColorTextureView(), Optional.empty())) {
                pass.setPipeline(pipeline);
                pass.setUniform("GocBloom", parameters);
                pass.bindTexture("Sampler0", input.getColorTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
                pass.draw(3, 1, 0, 0);
            }
        }
    }

    private static void prepareGeometry() {
        if (strikeGeometry == null) {
            strikeGeometry = new StrikeGeometry();
            damoclesGeometry = new DamoclesGeometry(strikeGeometry);
        }
        if (solidBuffer == null) {
            solidBuffer = new ByteBufferBuilder(64 * 1024);
            translucentBuffer = new ByteBufferBuilder(1024 * 1024);
            strikeBuffer = new ByteBufferBuilder(3 * 1024 * 1024);
            sealBuffer = new ByteBufferBuilder(32 * 1024);
        }
    }

    private static void ensureTargets(RenderTarget main) {
        if (mask != null && mask.width == main.width && mask.height == main.height) {
            return;
        }
        releaseTargets();
        mask = target("mask", main.width, main.height, main.getDepthTexture().getFormat());
        halfScratch = target("half scratch", Math.max(1, main.width / 2), Math.max(1, main.height / 2), null);
        halfGlow = target("half glow", halfScratch.width, halfScratch.height, null);
        quarterScratch = target("quarter scratch", Math.max(1, main.width / 4), Math.max(1, main.height / 4), null);
        quarterGlow = target("quarter glow", quarterScratch.width, quarterScratch.height, null);
    }

    private static TextureTarget target(String name, int width, int height, GpuFormat depthFormat) {
        return new TextureTarget("GOC Strike " + name, width, height, depthFormat != null, GpuFormat.RGBA8_UNORM);
    }

    private static void releaseTargets() {
        if (mask != null) {
            for (TextureTarget target : List.of(mask, halfScratch, halfGlow, quarterScratch, quarterGlow)) {
                target.destroyBuffers();
            }
            mask = halfScratch = halfGlow = quarterScratch = quarterGlow = null;
        }
    }

    private static void releaseGeometry() {
        if (solidBuffer != null) {
            for (ByteBufferBuilder buffer : List.of(solidBuffer, translucentBuffer, sealBuffer, strikeBuffer)) {
                buffer.close();
            }
            solidBuffer = translucentBuffer = sealBuffer = strikeBuffer = null;
        }
    }

    private record Draw(GpuBuffer vertices, int count, int indices) implements AutoCloseable {
        @Override
        public void close() {
            vertices.close();
        }
    }
}
