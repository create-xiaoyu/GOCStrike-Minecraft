package com.xiaoyu.gocstrike;

public final class DamoclesTimeline {
    public static final float LIFT_START = 1.5F;
    public static final float LIFT_END = 8;
    public static final float CORE_OPEN_END = 12.7F;
    public static final float PRINT_START = 14;
    public static final float PRINT_END = 24;
    public static final float CORE_RISE_END = 27;
    public static final float SPIN_START = PRINT_END;
    public static final float SPIN_END = 68.5F;
    public static final float PULLBACK_START = 69.15F;
    public static final float PLUNGE_START = 69.73F;
    public static final float IMPACT = 70;
    public static final float HIT_STOP_END = 70.27F;
    public static final float CORE_BREAK = 70.48F;
    public static final float SWORD_BREAK = 70.82F;
    public static final float SEAL_BREAK = 71.00F;
    public static final float END = 78;
    public static final float GIANT_SCALE = 5;
    public static final float GIANT_LENGTH = 12.4F * GIANT_SCALE;
    public static final float CORE_OVERTRAVEL = 3.1F * GIANT_SCALE;
    public static final float IMPACT_WAVE_RADIUS = 108;
    public static final float AFTERSHOCK_RADIUS = 148;

    private DamoclesTimeline() {
    }

    public static float progress(float time, float start, float end) {
        return Math.max(0, Math.min(1, (time - start) / (end - start)));
    }

    public static float ease(float time, float start, float end) {
        float t = progress(time, start, end);
        return t * t * (3 - 2 * t);
    }

    public static float turns(float time) {
        return 5 * ease(time, SPIN_START, SPIN_END);
    }

    public static float printed(float time) {
        return ease(time, PRINT_START, PRINT_END);
    }

    public static float plunge(float time) {
        float t = progress(time, PLUNGE_START, IMPACT);
        float squared = t * t;
        return squared * squared;
    }

    public static float impactKick(float time) {
        return ease(time, HIT_STOP_END, CORE_BREAK) * (1 - ease(time, CORE_BREAK, 71.45F));
    }

    public static float impactMusicGain(float time) {
        float duck = ease(time, IMPACT - 0.14F, IMPACT) * (1 - ease(time, IMPACT + 0.13F, IMPACT + 0.64F));
        return 1 - 0.73F * duck;
    }
}
