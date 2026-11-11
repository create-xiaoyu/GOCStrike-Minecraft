package com.xiaoyu.gocstrike.client;

import com.xiaoyu.gocstrike.GOCStrike;
import com.xiaoyu.gocstrike.StrikeEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber(modid = GOCStrike.MODID, value = Dist.CLIENT)
public final class StrikeEffects {
    private StrikeEffects() {
    }

    @SubscribeEvent
    public static void renderFinalFlash(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }
        float opacity = 0;
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        for (var entity : minecraft.level.entitiesForRendering()) {
            if (!(entity instanceof StrikeEntity strike) || strike.isRemoved()) {
                continue;
            }
            float time = strike.elapsed(partialTick);
            if (time < 113.322667F || time >= StrikeEntity.DURATION) {
                continue;
            }
            // Original expanding final front. Fade its tail to restore the HUD when the show ends.
            float progress = Mth.clamp((time + 0.15F - 113.322667F) / (116.032F - 113.322667F), 0, 1);
            float smooth = progress * progress * progress * (progress * (progress * 6 - 15) + 10);
            float radius = Mth.lerp(smooth, 2.2F, 250.0F) * StrikeEntity.WORLD_SCALE;
            float distance = (float) minecraft.gameRenderer.getMainCamera().getPosition().distanceTo(strike.position());
            float front = Mth.clamp(radius - distance, 0, 1);
            front *= front * (3 - 2 * front);
            float tail = Mth.clamp((StrikeEntity.DURATION - time) / 0.7F, 0, 1);
            opacity = Math.max(opacity, front * tail);
        }
        int alpha = (int) (opacity * 255);
        if (alpha > 0) {
            var graphics = event.getGuiGraphics();
            graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), (alpha << 24) | 0x00ffffff);
        }
    }
}
