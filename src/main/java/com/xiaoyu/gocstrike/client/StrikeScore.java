package com.xiaoyu.gocstrike.client;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import javax.sound.sampled.AudioFormat;

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

/** Non-spatial streaming score. A late observer starts at the current animation time. */
public final class StrikeScore extends AbstractTickableSoundInstance {
    private static final SoundEvent SCORE = SoundEvent.createVariableRangeEvent(
            ResourceLocation.fromNamespaceAndPath(GOCStrike.MODID, "emplacement_score"));
    private final StrikeEntity strike;
    private final float offset;

    public StrikeScore(StrikeEntity strike) {
        super(SCORE, SoundSource.MUSIC, SoundInstance.createUnseededRandom());
        this.strike = strike;
        offset = strike.elapsed(0);
        relative = true;
        attenuation = SoundInstance.Attenuation.NONE;
        volume = 0.85F;
    }

    public boolean belongsTo(StrikeEntity entity) {
        return strike == entity;
    }

    public void finish() {
        stop();
    }

    @Override
    public void tick() {
        if (strike.isRemoved() || strike.elapsed(0) >= StrikeEntity.DURATION) {
            stop();
        }
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
