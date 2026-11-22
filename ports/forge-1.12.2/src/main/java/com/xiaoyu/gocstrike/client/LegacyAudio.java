package com.xiaoyu.gocstrike.client;

import java.net.MalformedURLException;
import java.net.URL;

import com.xiaoyu.gocstrike.DamoclesEntity;
import com.xiaoyu.gocstrike.DamoclesTimeline;
import com.xiaoyu.gocstrike.GOCStrike;
import com.xiaoyu.gocstrike.StrikeEntity;
import com.xiaoyu.gocstrike.TimedEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.client.audio.MusicTicker;
import net.minecraft.client.audio.SoundManager;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.client.event.sound.PlayStreamingSourceEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import paulscode.sound.SoundSystem;
import paulscode.sound.SoundSystemConfig;
import paulscode.sound.SoundSystemException;

public final class LegacyAudio {
    private static final String CODEC = "gocstrike_score";
    private Score score;
    private int retry;

    LegacyAudio() throws SoundSystemException {
        SoundSystemConfig.setCodec(CODEC, ScoreCodec.class);
    }

    @SubscribeEvent
    public void playVanillaMusic(PlaySoundEvent event) {
        ISound original = event.getResultSound();
        if (score != null && !score.isDonePlaying() && original != null && original.getCategory() == SoundCategory.MUSIC
                && "minecraft".equals(original.getSoundLocation().getNamespace())) {
            event.setResultSound(null);
        }
    }

    @SubscribeEvent
    public void seek(PlayStreamingSourceEvent event) {
        if (!(event.getSound() instanceof Score)) {
            return;
        }
        Score sound = (Score) event.getSound();
        SoundSystem system = ReflectionHelper.getPrivateValue(SoundManager.class, event.getManager(), "sndSystem", "field_148620_e");
        String asset = sound.getSoundLocation().getPath();
        URL original = LegacyAudio.class.getResource("/assets/gocstrike/sounds/" + asset + ".ogg");
        if (original == null) {
            throw new IllegalStateException("Missing GOC Strike music " + asset);
        }
        try {
            URL stream = new URL(original, "#" + sound.entity.elapsed(0));
            // 保留原版管理的声道和音乐音量，只在播放前替换流式来源及其解码器。
            system.newStreamingSource(false, event.getUuid(), stream, asset + "." + CODEC,
                    false, 0, 0, 0, SoundSystemConfig.ATTENUATION_NONE, 0);
        } catch (MalformedURLException exception) {
            throw new IllegalStateException("Cannot seek GOC Strike music", exception);
        }
    }

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft game = Minecraft.getMinecraft();
        TimedEntity active = findActive(game);
        if (score != null && (score.entity != active || score.isDonePlaying())) {
            game.getSoundHandler().stopSound(score);
            score = null;
            retry = 0;
        }
        if (active == null || game.isGamePaused()
                || score != null && game.getSoundHandler().isSoundPlaying(score)) {
            return;
        }
        if (retry-- > 0) {
            return;
        }
        if (score != null) {
            game.getSoundHandler().stopSound(score);
        }
        Score next = new Score(active);
        ISound vanilla = ReflectionHelper.getPrivateValue(MusicTicker.class, game.getMusicTicker(), "currentMusic", "field_147678_c");
        if (vanilla != null && "minecraft".equals(vanilla.getSoundLocation().getNamespace())) {
            game.getSoundHandler().stopSound(vanilla);
        }
        score = next;
        game.getSoundHandler().playSound(score);
        retry = 40;
    }

    private static TimedEntity findActive(Minecraft game) {
        if (game.world == null || game.player == null) {
            return null;
        }
        DamoclesEntity nearest = null;
        double best = 72 * 72;
        for (Object object : game.world.loadedEntityList) {
            if (!(object instanceof TimedEntity)) {
                continue;
            }
            TimedEntity entity = (TimedEntity) object;
            if (entity.isDead || !entity.hasStarted()) {
                continue;
            }
            if (entity instanceof StrikeEntity && entity.elapsed(0) < StrikeEntity.DURATION) {
                return entity;
            }
            if (entity instanceof DamoclesEntity && entity.elapsed(0) < DamoclesTimeline.END) {
                double distance = entity.getDistanceSq(game.player);
                if (distance < best) {
                    best = distance;
                    nearest = (DamoclesEntity) entity;
                }
            }
        }
        return nearest;
    }

    private static final class Score extends MovingSound {
        private final TimedEntity entity;

        private Score(TimedEntity entity) {
            super(entity instanceof StrikeEntity ? GOCStrike.EMPLACEMENT_SCORE : GOCStrike.DAMOCLES_SCORE, SoundCategory.MUSIC);
            this.entity = entity;
            attenuationType = ISound.AttenuationType.NONE;
            update();
        }

        @Override
        public void update() {
            float time = entity.elapsed(0);
            boolean sword = entity instanceof DamoclesEntity;
            volume = 0.85F * (sword ? DamoclesTimeline.impactMusicGain(time) : 1F);
            donePlaying = entity.isDead || time >= (sword ? DamoclesTimeline.END : StrikeEntity.DURATION);
        }
    }
}
