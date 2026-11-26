package com.xiaoyu.gocstrike.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xiaoyu.gocstrike.StrikeEntity;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.entity.Entity;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import org.lwjgl.opengl.GL11;

public final class LegacySubtitles {
    private static final String GHOSTS = "ἐντῇοἰκίᾳτοῦπατρόςμουμοναὶπολλαίεἰσινἵναὅπουεἰμὶἐγώκαὶὑμεῖςἦτε";
    private final List<Cue> cues = new ArrayList<>();

    LegacySubtitles() throws IOException {
        InputStream resource = LegacySubtitles.class.getResourceAsStream("/assets/gocstrike/subtitles.json");
        if (resource == null) {
            throw new IOException("Missing thaumaturgic strike captions");
        }
        try (InputStreamReader reader = new InputStreamReader(resource, StandardCharsets.UTF_8)) {
            for (JsonElement entry : new JsonParser().parse(reader).getAsJsonArray()) {
                JsonObject cue = entry.getAsJsonObject();
                cues.add(new Cue(cue.get("start").getAsFloat(), cue.get("resolved").getAsFloat(),
                        cue.get("end").getAsFloat(), cue.get("text").getAsString(),
                        Integer.parseInt(cue.get("color").getAsString(), 16), cue.get("top").getAsBoolean()));
            }
        }
    }

    @SubscribeEvent
    public void render(RenderGameOverlayEvent.Post event) {
        Minecraft game = Minecraft.getMinecraft();
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL || game.world == null || game.getRenderViewEntity() == null) {
            return;
        }
        for (Object object : game.world.loadedEntityList) {
            if (!(object instanceof StrikeEntity)) {
                continue;
            }
            StrikeEntity strike = (StrikeEntity) object;
            float time = strike.elapsed(event.getPartialTicks());
            if (strike.isDead || !strike.hasStarted() || time >= StrikeEntity.DURATION) {
                continue;
            }
            int width = event.getResolution().getScaledWidth();
            int height = event.getResolution().getScaledHeight();
            drawCaption(game.fontRenderer, width, height, time);
            if (time >= 113.322667F) {
                Entity eye = game.getRenderViewEntity();
                float opacity = flashOpacity(time, (float) strike.getDistance(eye.posX, eye.posY, eye.posZ));
                if (opacity > 0) {
                    Gui.drawRect(0, 0, width, height, (int) (255 * opacity) << 24 | 0xffffff);
                }
            }
            return;
        }
    }

    private static float flashOpacity(float time, float distance) {
        float t = LegacyMath.clamp((time + 0.15F - 113.322667F) / (116.032F - 113.322667F), 0, 1);
        float smooth = t * t * t * (t * (t * 6 - 15) + 10);
        float radius = LegacyMath.lerp(smooth, 2.2F, 250) * StrikeEntity.WORLD_SCALE;
        float front = LegacyMath.clamp(radius - distance, 0, 1);
        return front * front * (3 - 2 * front) * LegacyMath.clamp((StrikeEntity.DURATION - time) / 0.7F, 0, 1);
    }

    private void drawCaption(FontRenderer font, int width, int height, float time) {
        for (int index = 0; index < cues.size(); index++) {
            Cue cue = cues.get(index);
            if (time < cue.start || time >= cue.end) {
                continue;
            }
            char[] chars = cue.text.toCharArray();
            boolean[] ghosts = new boolean[chars.length];
            if (time < cue.resolved) {
                reveal(chars, ghosts, cue, time, index);
            }
            int textWidth = font.getStringWidth(new String(chars));
            float scale = Math.min(1.25F, Math.max(1, width - 24F) / Math.max(1, textWidth));
            int y = (int) (height * (cue.top ? 0.07F : 0.50F) / scale);
            int x = (int) (width / (2 * scale) - textWidth / 2F);

            // 旧字库没有逐字 RGB 的聊天组件，直接把同一行的字形绘到 HUD。
            GL11.glPushAttrib(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT);
            GL11.glPushMatrix();
            try {
                GL11.glScalef(scale, scale, 1);
                for (int i = 0; i < chars.length; i++) {
                    String glyph = String.valueOf(chars[i]);
                    font.drawStringWithShadow(glyph, x, y, ghosts[i] ? 0x7293a3 : cue.color);
                    x += font.getCharWidth(chars[i]);
                }
            } finally {
                GL11.glPopMatrix();
                GL11.glPopAttrib();
            }
            return;
        }
    }

    private static void reveal(char[] chars, boolean[] ghosts, Cue cue, float time, int seed) {
        float progress = Math.max(0, (time - cue.start - 0.3F) / (cue.resolved - cue.start - 0.3F));
        int visible = (int) (progress * chars.length);
        int frame = (int) (time * 12);
        for (int i = 0; i < chars.length; i++) {
            int hash = seed * 397 + i * 7919 + frame * 104729;
            hash ^= hash >>> 16;
            hash *= 0x85ebca6b;
            hash ^= hash >>> 13;
            ghosts[i] = i >= visible || Integer.remainderUnsigned(hash, 11) == 0;
            if (ghosts[i]) {
                chars[i] = GHOSTS.charAt(Integer.remainderUnsigned(hash, GHOSTS.length()));
            }
        }
    }

    private static final class Cue {
        private final float start, resolved, end;
        private final String text;
        private final int color;
        private final boolean top;

        private Cue(float start, float resolved, float end, String text, int color, boolean top) {
            this.start = start;
            this.resolved = resolved;
            this.end = end;
            this.text = text;
            this.color = color;
            this.top = top;
        }
    }
}
