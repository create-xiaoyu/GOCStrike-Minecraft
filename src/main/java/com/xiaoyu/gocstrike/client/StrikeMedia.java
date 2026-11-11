package com.xiaoyu.gocstrike.client;

import com.xiaoyu.gocstrike.GOCStrike;
import com.xiaoyu.gocstrike.StrikeEntity;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.SelectMusicEvent;

@EventBusSubscriber(modid = GOCStrike.MODID, value = Dist.CLIENT)
public final class StrikeMedia {
    private static StrikeScore score;
    private static int retryTicks;

    private StrikeMedia() {
    }

    @SubscribeEvent
    public static void selectMusic(SelectMusicEvent event) {
        if (score != null && !score.isStopped()) {
            // Suppress only vanilla situational music; ambient and gameplay sounds remain.
            event.setMusic(null);
        }
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        StrikeEntity active = null;
        if (minecraft.level != null && minecraft.player != null) {
            for (var entity : minecraft.level.entitiesForRendering()) {
                if (entity instanceof StrikeEntity strike && !strike.isRemoved() && strike.hasStarted()
                        && strike.elapsed(0) < StrikeEntity.DURATION) {
                    active = strike;
                    break;
                }
            }
        }
        if (score != null && (!score.belongsTo(active) || score.isStopped())) {
            score.finish();
            minecraft.getSoundManager().stop(score);
            score = null;
            retryTicks = 0;
        }
        if (active == null) {
            return;
        }
        if (score != null && minecraft.getSoundManager().isActive(score)) {
            return;
        }
        // Muted music or a resource reload can reject/stop a source; retry without
        // restarting every client tick, and always seek to the current show position.
        if (retryTicks-- > 0) {
            return;
        }
        if (score != null) {
            score.finish();
            minecraft.getSoundManager().stop(score);
        }
        score = new StrikeScore(active);
        minecraft.getSoundManager().play(score);
        retryTicks = 40;
    }
}
