package com.xiaoyu.gocstrike.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import com.xiaoyu.gocstrike.DamoclesEntity;
import com.xiaoyu.gocstrike.DamoclesTimeline;
import com.xiaoyu.gocstrike.StrikeEntity;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.world.WorldEvent;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GLContext;

public final class LegacyBloom {
    private final StrikeGeometry strikeGeometry = new StrikeGeometry();
    private final DamoclesGeometry damoclesGeometry = new DamoclesGeometry(strikeGeometry);
    private final DamoclesMesh mesh;
    private final LegacyBatch sword = new LegacyBatch(GL11.GL_TRIANGLES, false, 6000);
    private final LegacyBatch effects = new LegacyBatch(GL11.GL_TRIANGLES, false, 20000);
    private final LegacyBatch seal = new LegacyBatch(GL11.GL_QUADS, true, 576);
    private final LegacyBatch strike = new LegacyBatch(GL11.GL_TRIANGLES, false, 120000);
    private Program blur;
    private Program composite;
    private Program maskGeometry;
    private Framebuffer mask, halfScratch, halfGlow, quarterScratch, quarterGlow;

    LegacyBloom(DamoclesMesh mesh) {
        this.mesh = mesh;
    }

    @SubscribeEvent
    public void render(RenderWorldLastEvent event) {
        Minecraft game = Minecraft.getMinecraft();
        if (game.world == null) {
            return;
        }
        sword.clear();
        effects.clear();
        seal.clear();
        strike.clear();
        RenderManager renderManager = game.getRenderManager();
        Vec3d camera = new Vec3d(renderManager.viewerPosX, renderManager.viewerPosY, renderManager.viewerPosZ);
        for (Object object : game.world.loadedEntityList) {
            if (object instanceof StrikeEntity) {
                StrikeEntity entity = (StrikeEntity) object;
                if (!entity.isDead && entity.hasStarted() && entity.elapsed(event.getPartialTicks()) < StrikeEntity.DURATION
                        && entity.getDistanceSq(camera.x, camera.y, camera.z) < 512 * 512) {
                    strikeGeometry.appendStrike(strike, entity, event.getPartialTicks(), camera);
                }
            } else if (object instanceof DamoclesEntity) {
                DamoclesEntity entity = (DamoclesEntity) object;
                int range = entity.hasLocked() ? 256 : 128;
                if (!entity.isDead && entity.renderTime(event.getPartialTicks()) < DamoclesTimeline.END
                        && entity.getDistanceSq(camera.x, camera.y, camera.z) < range * range) {
                    damoclesGeometry.append(entity, event.getPartialTicks(), camera, mesh, sword, effects, seal);
                }
            }
        }
        if (sword.isEmpty() && effects.isEmpty() && seal.isEmpty() && strike.isEmpty()) {
            releaseTargets();
            return;
        }

        boolean gl30 = GLContext.getCapabilities().OpenGL30;
        int drawTarget = gl30 ? GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING) : 0;
        int readTarget = gl30 ? GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING) : 0;
        boolean shaders = GLContext.getCapabilities().OpenGL20;
        int previousProgram = shaders ? GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM) : 0;
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        try {
            if (shaders) {
                GL20.glUseProgram(0);
            }
            Framebuffer main = game.getFramebuffer();
            boolean glow = shaders && gl30 && OpenGlHelper.isFramebufferEnabled() && drawTarget == main.framebufferObject;
            if (glow) {
                prepare(main);
                mask.bindFramebuffer(true);
                GL11.glClearColor(0, 0, 0, 0);
                GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
                // 复制主画面的深度，只让未被墙体遮挡的特效像素进入泛光。
                GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, main.framebufferObject);
                GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, mask.framebufferObject);
                GL30.glBlitFramebuffer(0, 0, main.framebufferWidth, main.framebufferHeight,
                        0, 0, mask.framebufferWidth, mask.framebufferHeight, GL11.GL_DEPTH_BUFFER_BIT, GL11.GL_NEAREST);
                mask.bindFramebuffer(true);
                geometry(game, true);
                main.bindFramebuffer(true);
                GL20.glUseProgram(0);
            }
            geometry(game, false);
            if (glow) {
                GL11.glDisable(GL11.GL_DEPTH_TEST);
                GL11.glDisable(GL11.GL_CULL_FACE);
                GL11.glDisable(GL11.GL_BLEND);
                blur(mask, halfScratch, true);
                blur(halfScratch, halfGlow, false);
                blur(halfGlow, quarterScratch, true);
                blur(quarterScratch, quarterGlow, false);
                main.bindFramebuffer(true);
                GL11.glEnable(GL11.GL_BLEND);
                GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_COLOR);
                composite(halfGlow, 0.85F);
                composite(quarterGlow, 1.25F);
            }
        } finally {
            if (gl30) {
                GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readTarget);
                GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, drawTarget);
            }
            if (shaders) {
                GL20.glUseProgram(previousProgram);
            }
            GL11.glPopAttrib();
        }
    }

    private void geometry(Minecraft game, boolean isMask) {
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glDisable(GL11.GL_FOG);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        if (GLContext.getCapabilities().OpenGL13) {
            GL13.glActiveTexture(GL13.GL_TEXTURE1);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
        }
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDepthMask(true);
        if (isMask) {
            maskGeometry.use();
            maskGeometry.tint(0.20F, 0.45F, 1, false);
        }
        sword.draw();
        GL11.glDepthMask(false);
        effects.draw();

        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        game.getTextureManager().bindTexture(DamoclesGeometry.GOC_SEAL);
        if (isMask) {
            maskGeometry.tint(0.20F, 0.45F, 1, true);
        }
        seal.draw();

        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        if (isMask) {
            maskGeometry.tint(1, 1, 1, false);
        }
        strike.draw();
    }

    private void blur(Framebuffer input, Framebuffer output, boolean horizontal) {
        output.bindFramebuffer(true);
        blur.use();
        GL20.glUniform2f(blur.step, horizontal ? 1F / input.framebufferTextureWidth : 0,
                horizontal ? 0 : 1F / input.framebufferTextureHeight);
        quad(input);
    }

    private void composite(Framebuffer source, float strength) {
        composite.use();
        GL20.glUniform1f(composite.strength, strength);
        quad(source);
    }

    private static void quad(Framebuffer input) {
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, input.framebufferTexture);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0, 0);
        GL11.glVertex2f(-1, -1);
        GL11.glTexCoord2f(1, 0);
        GL11.glVertex2f(1, -1);
        GL11.glTexCoord2f(1, 1);
        GL11.glVertex2f(1, 1);
        GL11.glTexCoord2f(0, 1);
        GL11.glVertex2f(-1, 1);
        GL11.glEnd();
    }

    private void prepare(Framebuffer main) {
        int width = main.framebufferWidth;
        int height = main.framebufferHeight;
        if (blur == null) {
            blur = new Program("bloom_screen.vsh", "bloom_blur.fsh");
            composite = new Program("bloom_screen.vsh", "bloom_composite.fsh");
            maskGeometry = new Program("bloom_mask.vsh", "bloom_mask.fsh");
        }
        if (mask != null && mask.framebufferWidth == width && mask.framebufferHeight == height && mask.isStencilEnabled() == main.isStencilEnabled()) {
            return;
        }
        releaseTargets();
        mask = target(width, height, true);
        // 整合包能给主帧缓冲启用模板附件。深度复制要求两端使用相同的深度格式。
        if (main.isStencilEnabled()) {
            mask.enableStencil();
        }
        halfScratch = target(width / 2, height / 2, false);
        halfGlow = target(width / 2, height / 2, false);
        quarterScratch = target(width / 4, height / 4, false);
        quarterGlow = target(width / 4, height / 4, false);
    }

    private static Framebuffer target(int width, int height, boolean depth) {
        Framebuffer result = new Framebuffer(Math.max(1, width), Math.max(1, height), depth);
        result.setFramebufferColor(0, 0, 0, 0);
        result.setFramebufferFilter(GL11.GL_LINEAR);
        return result;
    }

    @SubscribeEvent
    public void unload(WorldEvent.Unload event) {
        if (event.getWorld().isRemote) {
            releaseTargets();
        }
    }

    private void releaseTargets() {
        if (mask == null) {
            return;
        }
        // 旧版 deleteFramebuffer 会绑定默认目标，删除后恢复世界当前的读写目标。
        int drawTarget = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int readTarget = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        GL11.glPushAttrib(GL11.GL_TEXTURE_BIT);
        try {
            for (Framebuffer target : new Framebuffer[]{mask, halfScratch, halfGlow, quarterScratch, quarterGlow}) {
                if (target != null) {
                    target.deleteFramebuffer();
                }
            }
            mask = halfScratch = halfGlow = quarterScratch = quarterGlow = null;
        } finally {
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readTarget);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, drawTarget);
            GL11.glPopAttrib();
        }
    }

    private static final class Program {
        private final int id;
        private final int sampler;
        private final int step;
        private final int strength;
        private final int tint;
        private final int textured;

        private Program(String vertex, String fragment) {
            int vs = compile(GL20.GL_VERTEX_SHADER, vertex);
            int fs = 0;
            int program = 0;
            try {
                fs = compile(GL20.GL_FRAGMENT_SHADER, fragment);
                program = GL20.glCreateProgram();
                GL20.glAttachShader(program, vs);
                GL20.glAttachShader(program, fs);
                GL20.glLinkProgram(program);
                if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
                    throw new IllegalStateException("Cannot link bloom shader: " + GL20.glGetProgramInfoLog(program, 4096));
                }
                id = program;
            } catch (RuntimeException exception) {
                if (program != 0) {
                    GL20.glDeleteProgram(program);
                }
                throw exception;
            } finally {
                GL20.glDeleteShader(vs);
                if (fs != 0) {
                    GL20.glDeleteShader(fs);
                }
            }
            sampler = GL20.glGetUniformLocation(id, "Sampler0");
            step = GL20.glGetUniformLocation(id, "Step");
            strength = GL20.glGetUniformLocation(id, "Strength");
            tint = GL20.glGetUniformLocation(id, "Tint");
            textured = GL20.glGetUniformLocation(id, "Textured");
        }

        private void use() {
            GL20.glUseProgram(id);
            GL20.glUniform1i(sampler, 0);
        }

        private void tint(float r, float g, float b, boolean texture) {
            GL20.glUniform3f(tint, r, g, b);
            GL20.glUniform1i(textured, texture ? 1 : 0);
        }

        private static int compile(int type, String name) {
            String source;
            try (InputStream stream = LegacyBloom.class.getResourceAsStream("/assets/gocstrike/shaders/legacy/" + name)) {
                if (stream == null) {
                    throw new IOException("Missing bloom shader " + name);
                }
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                byte[] buffer = new byte[2048];
                int read;
                while ((read = stream.read(buffer)) != -1) {
                    bytes.write(buffer, 0, read);
                }
                source = new String(bytes.toByteArray(), StandardCharsets.UTF_8);
            } catch (IOException exception) {
                throw new IllegalStateException("Cannot load bloom shader " + name, exception);
            }
            int shader = GL20.glCreateShader(type);
            GL20.glShaderSource(shader, source);
            GL20.glCompileShader(shader);
            if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
                String message = GL20.glGetShaderInfoLog(shader, 4096);
                GL20.glDeleteShader(shader);
                throw new IllegalStateException("Cannot compile " + name + ": " + message);
            }
            return shader;
        }
    }
}
