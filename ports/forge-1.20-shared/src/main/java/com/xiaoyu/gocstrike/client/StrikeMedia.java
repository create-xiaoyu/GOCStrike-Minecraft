package com.xiaoyu.gocstrike.client;

import com.xiaoyu.gocstrike.DamoclesEntity;
import com.xiaoyu.gocstrike.DamoclesTimeline;
import com.xiaoyu.gocstrike.GOCStrike;
import com.xiaoyu.gocstrike.StrikeEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.TickEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.client.event.sound.PlaySoundEvent;

@EventBusSubscriber(modid = GOCStrike.MODID, value = Dist.CLIENT)
public final class StrikeMedia {
    private static StrikeScore score;
    private static int retryTicks;

    private StrikeMedia() {
    }

    @SubscribeEvent
    public static void suppressVanillaMusic(PlaySoundEvent event) {
        var sound = event.getSound();
        // Forge 1.20.1 没有选曲事件。在音源入口只拦截原版背景音乐。
        if (score != null && !score.isStopped() && sound != null && sound.getSource() == SoundSource.MUSIC
                && sound.getLocation().getNamespace().equals("minecraft")) {
            event.setSound(null);
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Entity active = null;
        double nearestSword = 72 * 72;
        if (minecraft.level != null && minecraft.player != null) {
            for (var entity : minecraft.level.entitiesForRendering()) {
                if (entity instanceof StrikeEntity strike && !strike.isRemoved() && strike.hasStarted()
                        && strike.elapsed(0) < StrikeEntity.DURATION) {
                    active = strike;
                    break;
                }
                if (entity instanceof DamoclesEntity sword && !sword.isRemoved() && sword.hasLocked()
                        && sword.elapsed(0) < DamoclesTimeline.END) {
                    double distance = sword.distanceToSqr(minecraft.player);
                    if (distance < nearestSword) {
                        active = sword;
                        nearestSword = distance;
                    }
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
        // 静音或重载资源可能阻止音源启动。间隔重试，并从当前演出时间恢复。
        if (retryTicks-- > 0) {
            return;
        }
        if (score != null) {
            score.finish();
            minecraft.getSoundManager().stop(score);
        }
        score = active instanceof StrikeEntity strike ? new StrikeScore(strike) : new StrikeScore((DamoclesEntity) active);
        minecraft.getMusicManager().stopPlaying();
        minecraft.getSoundManager().play(score);
        retryTicks = 40;
    }
}
