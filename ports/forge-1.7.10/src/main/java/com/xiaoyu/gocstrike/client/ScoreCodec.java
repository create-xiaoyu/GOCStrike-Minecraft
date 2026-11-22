package com.xiaoyu.gocstrike.client;

import java.net.URL;
import java.util.Arrays;

import javax.sound.sampled.AudioFormat;
import paulscode.sound.SoundBuffer;
import paulscode.sound.codecs.CodecJOrbis;

public final class ScoreCodec extends CodecJOrbis {
    private SoundBuffer first;

    @Override
    public boolean initialize(URL url) {
        first = null;
        if (!super.initialize(url)) {
            return false;
        }
        String ref = url.getRef();
        if (ref == null) {
            return true;
        }
        float seconds = Float.parseFloat(ref);
        if (seconds <= 0) {
            return true;
        }

        // Paulscode 在自己的流式解码线程初始化该编解码器，可在这里跳过开头的 PCM。
        AudioFormat format = getAudioFormat();
        long remaining = (long) (seconds * format.getFrameRate()) * format.getFrameSize();
        while (remaining > 0) {
            SoundBuffer decoded = super.read();
            if (decoded == null || decoded.audioData == null || decoded.audioData.length == 0) {
                return true;
            }
            if (decoded.audioData.length > remaining) {
                first = new SoundBuffer(Arrays.copyOfRange(decoded.audioData, (int) remaining, decoded.audioData.length), decoded.audioFormat);
                decoded.cleanup();
                break;
            }
            remaining -= decoded.audioData.length;
            decoded.cleanup();
        }
        return true;
    }

    @Override
    public SoundBuffer read() {
        if (first == null) {
            return super.read();
        }
        SoundBuffer chunk = first;
        first = null;
        return chunk;
    }

    @Override
    public void cleanup() {
        super.cleanup();
        if (first != null) {
            first.cleanup();
            first = null;
        }
    }
}
