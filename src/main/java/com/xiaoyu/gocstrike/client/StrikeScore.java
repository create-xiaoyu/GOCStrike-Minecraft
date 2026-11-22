package com.xiaoyu.gocstrike.client;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.DoubleSupplier;

import javax.sound.sampled.AudioFormat;

import com.xiaoyu.gocstrike.DamoclesEntity;
import com.xiaoyu.gocstrike.DamoclesTimeline;
import com.xiaoyu.gocstrike.GOCStrike;
import com.xiaoyu.gocstrike.StrikeEntity;

import net.minecraft.Util;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;

/** Non-spatial streaming score. A late observer starts at the current animation time. */
public final class StrikeScore extends AbstractTickableSoundInstance {
    private static final SoundEvent SCORE = SoundEvent.createVariableRangeEvent(
            ResourceLocation.fromNamespaceAndPath(GOCStrike.MODID, "emplacement_score"));
    private static final SoundEvent DAMOCLES_SCORE = SoundEvent.createVariableRangeEvent(
            ResourceLocation.fromNamespaceAndPath(GOCStrike.MODID, "damocles_score"));
    private final Entity entity;
    private final DoubleSupplier time;
    private final float duration;
    private volatile float offset;

    public StrikeScore(StrikeEntity strike) {
        this(SCORE, strike, () -> strike.elapsed(0), StrikeEntity.DURATION);
    }

    public StrikeScore(DamoclesEntity sword) {
        this(DAMOCLES_SCORE, sword, () -> sword.elapsed(0), DamoclesTimeline.END);
    }

    private StrikeScore(SoundEvent sound, Entity entity, DoubleSupplier time, float duration) {
        super(sound, SoundSource.MUSIC, SoundInstance.createUnseededRandom());
        this.entity = entity;
        this.time = time;
        this.duration = duration;
        offset = (float) time.getAsDouble();
        relative = true;
        attenuation = SoundInstance.Attenuation.NONE;
        updateVolume();
    }

    public boolean belongsTo(Entity entity) {
        return this.entity == entity;
    }

    public void finish() {
        stop();
    }

    @Override
    public void tick() {
        offset = (float) time.getAsDouble();
        updateVolume();
        if (entity.isRemoved() || offset >= duration) {
            stop();
        }
    }

    private void updateVolume() {
        // 保留原音乐播放位置，只在达摩克里斯终击附近暂时让出音量。
        volume = 0.85F * (entity instanceof DamoclesEntity ? DamoclesTimeline.impactMusicGain(offset) : 1);
    }

    @Override
    public CompletableFuture<AudioStream> getStream(SoundBufferLibrary buffers, Sound sound, boolean looping) {
        return buffers.getStream(sound.getPath(), false).thenApplyAsync(stream -> {
            try {
                return new OffsetStream(stream, offset);
            } catch (IOException e) {
                try {
                    stream.close();
                } catch (IOException closeError) {
                    e.addSuppressed(closeError);
                }
                throw new CompletionException(e);
            }
        }, Util.nonCriticalIoPool());
    }

    private static final class OffsetStream implements AudioStream {
        private final AudioStream delegate;
        private ByteBuffer remainder;

        OffsetStream(AudioStream delegate, float seconds) throws IOException {
            this.delegate = delegate;
            AudioFormat format = delegate.getFormat();
            long remaining = (long) (seconds * format.getFrameRate()) * format.getFrameSize();
            while (remaining > 0) {
                ByteBuffer data = delegate.read((int) Math.min(remaining, 16384));
                if (!data.hasRemaining()) {
                    break;
                }
                if (data.remaining() > remaining) {
                    data.position(data.position() + (int) remaining);
                    remainder = data.slice();
                    break;
                }
                remaining -= data.remaining();
            }
        }

        @Override
        public AudioFormat getFormat() {
            return delegate.getFormat();
        }

        @Override
        public ByteBuffer read(int size) throws IOException {
            if (remainder != null) {
                int length = Math.min(size, remainder.remaining());
                ByteBuffer result = remainder.slice();
                result.limit(length);
                remainder.position(remainder.position() + length);
                if (!remainder.hasRemaining()) {
                    remainder = null;
                }
                return result;
            }
            return delegate.read(size);
        }

        @Override
        public void close() throws IOException {
            delegate.close();
        }
    }
}
