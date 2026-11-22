package com.xiaoyu.gocstrike.client;

import net.minecraft.util.MathHelper;

final class LegacyMath {
    static final float PI = (float) Math.PI;
    static final float HALF_PI = PI / 2;
    static final float TWO_PI = PI * 2;
    static final float DEG_TO_RAD = PI / 180;

    private LegacyMath() {
    }

    static float clamp(float value, float low, float high) {
        return MathHelper.clamp_float(value, low, high);
    }

    static float lerp(float amount, float from, float to) {
        return from + amount * (to - from);
    }

    static float abs(float value) {
        return Math.abs(value);
    }

    static float sin(float angle) {
        return MathHelper.sin(angle);
    }

    static float cos(float angle) {
        return MathHelper.cos(angle);
    }
}
