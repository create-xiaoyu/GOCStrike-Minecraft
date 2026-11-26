package com.xiaoyu.gocstrike.client;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonParser;
import com.xiaoyu.gocstrike.GOCStrike;

import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/** 沿演出时间轴显示参考视频字幕。 */
public final class StrikeSubtitles {
    private static final String GHOSTS = "ἐντῇοἰκίᾳτοῦπατρόςμουμοναὶπολλαίεἰσινἵναὅπουεἰμὶἐγώκαὶὑμεῖςἦτε";
    private static final List<Cue> CUES = load();

    private StrikeSubtitles() {
    }

    private static List<Cue> load() {
        var resource = StrikeSubtitles.class.getResourceAsStream("/assets/gocstrike/subtitles.json");
        if (resource == null) {
            throw new IllegalStateException("Missing thaumaturgic strike captions");
        }
        List<Cue> cues = new ArrayList<>();
        try (var reader = new InputStreamReader(resource, StandardCharsets.UTF_8)) {
            for (var entry : JsonParser.parseReader(reader).getAsJsonArray()) {
                var cue = entry.getAsJsonObject();
                cues.add(new Cue(cue.get("start").getAsFloat(), cue.get("resolved").getAsFloat(),
                        cue.get("end").getAsFloat(), cue.get("text").getAsString(),
                        Integer.parseInt(cue.get("color").getAsString(), 16), cue.get("top").getAsBoolean()));
            }
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load thaumaturgic strike captions", e);
        }
        GOCStrike.LOGGER.info("Loaded {} thaumaturgic strike captions", cues.size());
        return List.copyOf(cues);
    }

    public static void render(float time, PoseStack poses, int width, int height) {
        for (int i = 0; i < CUES.size(); i++) {
            Cue cue = CUES.get(i);
            if (time < cue.start || time >= cue.end) {
                continue;
            }
            MutableComponent caption = reveal(cue, time, i);
            var font = Minecraft.getInstance().font;
            float scale = Math.min(1.25F, (width - 24.0F) / Math.max(1, font.width(caption)));
            float y = height * (cue.top ? 0.07F : 0.50F);
            poses.pushPose();
            poses.scale(scale, scale, 1);
            GuiComponent.drawCenteredString(poses, font, caption, (int) (width / (2 * scale)),
                    (int) (y / scale), 0xffffffff);
            poses.popPose();
            return;
        }
    }

    private static MutableComponent reveal(Cue cue, float time, int seed) {
        if (time >= cue.resolved) {
            return Component.literal(cue.text).withStyle(style -> style.withColor(cue.color));
        }
        float progress = Math.max(0, (time - cue.start - 0.3F) / (cue.resolved - cue.start - 0.3F));
        int visible = (int) (progress * cue.text.length());
        int frame = (int) (time * 12);
        MutableComponent result = Component.empty();
        for (int i = 0; i < cue.text.length(); i++) {
            int hash = seed * 397 + i * 7919 + frame * 104729;
            hash ^= hash >>> 16;
            hash *= 0x85ebca6b;
            hash ^= hash >>> 13;
            boolean ghost = i >= visible || Integer.remainderUnsigned(hash, 11) == 0;
            char glyph = ghost ? GHOSTS.charAt(Integer.remainderUnsigned(hash, GHOSTS.length())) : cue.text.charAt(i);
            int color = ghost ? 0x7293a3 : cue.color;
            result.append(Component.literal(String.valueOf(glyph)).withStyle(style -> style.withColor(color)));
        }
        return result;
    }

    private record Cue(float start, float resolved, float end, String text, int color, boolean top) {
    }
}
