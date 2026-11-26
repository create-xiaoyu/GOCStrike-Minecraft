package com.xiaoyu.gocstrike.client;

import java.net.MalformedURLException;
import java.net.URL;

import com.xiaoyu.gocstrike.DamoclesEntity;
import com.xiaoyu.gocstrike.DamoclesTimeline;
import com.xiaoyu.gocstrike.GOCStrike;
import com.xiaoyu.gocstrike.StrikeEntity;
import com.xiaoyu.gocstrike.TimedEntity;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.ReflectionHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.client.audio.MusicTicker;
import net.minecraft.client.audio.SoundCategory;
import net.minecraft.client.audio.SoundManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.sound.PlaySoundEvent17;
import net.minecraftforge.client.event.sound.PlayStreamingSourceEvent;
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
    public void playVanillaMusic(PlaySoundEvent17 event) {
        if (score != null && !score.isDonePlaying() && event.result != null && event.category == SoundCategory.MUSIC
                && "minecraft".equals(event.result.getPositionedSoundLocation().getResourceDomain())) {
            event.result = null;
        }
    }

    @SubscribeEvent
    public void seek(PlayStreamingSourceEvent event) {
        if (!(event.sound instanceof Score)) {
            return;
        }
        Score sound = (Score) event.sound;
        SoundSystem system = ReflectionHelper.getPrivateValue(SoundManager.class, event.manager, "sndSystem", "field_148620_e");
        URL original = LegacyAudio.class.getResource("/assets/gocstrike/sounds/" + sound.asset + ".ogg");
        if (original == null) {
            throw new IllegalStateException("Missing GOC Strike music " + sound.asset);
        }
        try {
            URL stream = new URL(original, "#" + sound.entity.elapsed(0));
            // 保留原版管理的声道和音乐音量，只在播放前替换流式来源及其解码器。
            system.newStreamingSource(false, event.uuid, stream, sound.asset + "." + CODEC,
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
        MusicTicker ticker = ReflectionHelper.getPrivateValue(Minecraft.class, game, "mcMusicTicker", "field_147126_aw");
        ISound vanilla = ReflectionHelper.getPrivateValue(MusicTicker.class, ticker, "field_147678_c");
        if (vanilla != null && "minecraft".equals(vanilla.getPositionedSoundLocation().getResourceDomain())) {
            game.getSoundHandler().stopSound(vanilla);
        }
        score = next;
        game.getSoundHandler().playSound(score);
        retry = 40;
    }

    private static TimedEntity findActive(Minecraft game) {
        if (game.theWorld == null || game.thePlayer == null) {
            return null;
        }
        DamoclesEntity nearest = null;
        double best = 72 * 72;
        for (Object object : game.theWorld.getLoadedEntityList()) {
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
                double distance = entity.getDistanceSqToEntity(game.thePlayer);
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
        private final String asset;

        private Score(TimedEntity entity) {
            this(entity, entity instanceof StrikeEntity ? "emplacement_score" : "damocles_score");
        }

        private Score(TimedEntity entity, String asset) {
            super(new ResourceLocation(GOCStrike.MODID, asset));
            this.entity = entity;
            this.asset = asset;
            field_147666_i = ISound.AttenuationType.NONE;
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
