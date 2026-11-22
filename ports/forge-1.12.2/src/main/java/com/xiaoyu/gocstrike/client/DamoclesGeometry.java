package com.xiaoyu.gocstrike.client;

import org.joml.Matrix4f;
import org.joml.Quaternionf;

import com.xiaoyu.gocstrike.DamoclesEntity;
import com.xiaoyu.gocstrike.DamoclesTimeline;
import com.xiaoyu.gocstrike.GOCStrike;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;

final class DamoclesGeometry {
    static final ResourceLocation GOC_SEAL = new ResourceLocation(GOCStrike.MODID, "textures/goc_seal.png");
    private static final float CORE_SCALE = 0.48F * DamoclesTimeline.GIANT_SCALE;
    private static final float CORE_CLEARANCE = 3.2F;
    private static final float SEAL_RADIUS = 8.5F;
    private static final float CHARGE_WAVE_RADIUS = 92;
    private static final int SEAL_TILES = 12;
    private static final int SEAL_BLUE = 0x80C7FF;
    private static final int IMPACT_WHITE = 0xEAF9FF;
    private static final int WAVE_COBALT = 0x397CFF;
    private static final int AFTERSHOCK_VIOLET = 0x8996FF;
    private static final int CHARGE_CYAN = 0x37B8FF;
    private static final int CHARGE_CREST = 0xBDF8FF;
    private final Matrix4f swordPose = new Matrix4f();
    private final Matrix4f scenePose = new Matrix4f();
    private final Matrix4f sealPose = new Matrix4f();
    private final Quaternionf orientation = new Quaternionf();
    private final StrikeGeometry strike;

    DamoclesGeometry(StrikeGeometry strike) {
        this.strike = strike;
    }

    void append(DamoclesEntity entity, float partialTick, Vec3d camera, DamoclesMesh mesh,
            LegacyBatch solid, LegacyBatch transparent, LegacyBatch seal) {
        float time = entity.renderTime(partialTick);
        float scatter = DamoclesTimeline.ease(time, DamoclesTimeline.SWORD_BREAK, DamoclesTimeline.END);
        Vec3d relative = camera.subtract(entity.renderPosition(partialTick));
        if (mesh != null && swordPose(entity, partialTick, relative)) {
            float printed = entity.hasLocked() ? DamoclesTimeline.printed(time) : 1;
            float charge = entity.hasLocked() ? DamoclesTimeline.turns(time) / 5 : 0.8F;
            mesh.draw(scatter == 0 ? solid : transparent, swordPose, printed, charge, scatter);
        }
        if (entity.hasLocked() && time < DamoclesTimeline.END) {
            Vec3d feet = camera.subtract(entity.captiveFeet(partialTick));
            scenePose.translation((float) relative.x, (float) relative.y, (float) relative.z);
            sealPose.translation((float) feet.x, (float) feet.y, (float) feet.z);
            float sealScatter = DamoclesTimeline.ease(time, DamoclesTimeline.SEAL_BREAK, DamoclesTimeline.END);
            drawProjection(transparent, entity, time, sealScatter);
            drawSeal(seal, time, sealScatter);
        }
    }

    private boolean swordPose(DamoclesEntity entity, float partialTick, Vec3d anchor) {
        swordPose.translation((float) anchor.x, (float) anchor.y, (float) anchor.z);
        if (!entity.hasLocked()) {
            Vec3d aim = entity.flightDirection(partialTick);
            orientation.rotationTo(0, 1, 0, (float) aim.x, (float) aim.y, (float) aim.z);
            swordPose.rotate(orientation).scale(2.1F).translate(-0.5F, -1, -0.5F);
            return true;
        }
        float time = entity.renderTime(partialTick);
        if (time <= DamoclesTimeline.PRINT_START || time >= DamoclesTimeline.END) {
            return false;
        }

        float spin = DamoclesTimeline.turns(time);
        float floatWindow = DamoclesTimeline.ease(time, DamoclesTimeline.SPIN_START, DamoclesTimeline.SPIN_START + 1.25F)
                * (1 - DamoclesTimeline.ease(time, DamoclesTimeline.SPIN_END - 1.25F, DamoclesTimeline.SPIN_END));
        float bob = LegacyMath.sin((time - DamoclesTimeline.SPIN_START) * 2.2F) * 0.27F * floatWindow;
        float head = entity.lift(time) + entity.captiveHeight();
        float tip = tipHeight(head, time, bob);
        // 铸剑时剑尖留在原位，法阵沿固定尺寸的剑向上扫过。下刺时剑尖落在目标头顶。
        swordPose.translate(0, tip, 0).rotateY(-entity.rotationYaw * LegacyMath.DEG_TO_RAD + spin * LegacyMath.TWO_PI)
                .rotateZ(LegacyMath.PI).scale(DamoclesTimeline.GIANT_LENGTH).translate(-0.5F, -1, -0.5F);
        return true;
    }

    private static float tipHeight(float head, float time, float bob) {
        float pullback = DamoclesTimeline.ease(time, DamoclesTimeline.PULLBACK_START, DamoclesTimeline.PLUNGE_START);
        return LegacyMath.lerp(DamoclesTimeline.plunge(time), head + CORE_CLEARANCE + bob + 4.6F * pullback, head);
    }

    private static float coreHeight(float head, float time) {
        float extraRise = DamoclesTimeline.CORE_OVERTRAVEL
                * DamoclesTimeline.ease(time, DamoclesTimeline.PRINT_END, DamoclesTimeline.CORE_RISE_END);
        return head + CORE_CLEARANCE + DamoclesTimeline.GIANT_LENGTH * DamoclesTimeline.printed(time) + extraRise;
    }

    private void drawProjection(LegacyBatch builder, DamoclesEntity entity, float time, float sealScatter) {
        float head = entity.lift(time) + entity.captiveHeight();
        if (time > DamoclesTimeline.LIFT_END) {
            float height = coreHeight(head, time);
            Matrix4f core = new Matrix4f(scenePose).translate(0, height, 0).scale(CORE_SCALE);
            float opening = DamoclesTimeline.progress(time, DamoclesTimeline.LIFT_END, DamoclesTimeline.CORE_OPEN_END);
            // 用原动画从 FanOut 第一拍到最外圈成形的采样，保留各圈出现的先后与转动。
            float sourceTime = LegacyMath.lerp(opening, 73.365333F, 76.8F)
                    + 12.2F * DamoclesTimeline.progress(time, DamoclesTimeline.CORE_OPEN_END, DamoclesTimeline.SPIN_END);
            float whiteMatter = DamoclesTimeline.ease(time, DamoclesTimeline.CORE_OPEN_END, DamoclesTimeline.PRINT_START);
            float coreScatter = DamoclesTimeline.ease(time, DamoclesTimeline.CORE_BREAK, DamoclesTimeline.END);
            strike.appendDamoclesCore(builder, core, sourceTime, time - DamoclesTimeline.LIFT_END, whiteMatter,
                    DamoclesTimeline.impactKick(time), coreScatter);
        }

        float lock = sealReveal(time);
        if (lock > 0) {
            float alpha = lock * (1 - sealScatter);
            float radius = SEAL_RADIUS * lock;
            // 两段淡光与徽记共用目标脚下的位置，并随徽记一起分解。
            ring(builder, sealPose, 0.004F + sealScatter * 1.6F, radius * (1 + sealScatter * 0.4F),
                    0.045F, 0, alpha * 0.80F, sealScatter, SEAL_BLUE);
            ring(builder, sealPose, 0.006F + sealScatter * 2.1F, radius * (1.05F + sealScatter * 0.45F),
                    0.024F, 0, alpha * 0.42F, sealScatter, SEAL_BLUE);
        }

        drawChargeWave(builder, scenePose, time, head + CORE_CLEARANCE);
        drawPlungeTrail(builder, time, head);
        drawImpactFlash(builder, time, head);
        drawImpactWaves(builder, scenePose, time, head);
    }

    private void drawPlungeTrail(LegacyBatch builder, float time, float head) {
        if (time <= DamoclesTimeline.PLUNGE_START || time >= DamoclesTimeline.IMPACT + 0.18F) {
            return;
        }
        float tip = tipHeight(head, time, 0);
        float lagged = tipHeight(head, Math.max(DamoclesTimeline.PLUNGE_START, time - 0.145F), 0);
        float length = lagged - tip;
        if (length < 0.04F) {
            return;
        }
        float strength = Math.min(1, length) * (1 - DamoclesTimeline.progress(time, DamoclesTimeline.IMPACT,
                DamoclesTimeline.IMPACT + 0.18F));
        // 残影只覆盖剑尖刚经过的路径，剑在停顿时仍保持清晰。
        for (int side = 0; side < 4; side++) {
            float angle = side * LegacyMath.HALF_PI;
            float x = LegacyMath.cos(angle) * 1.22F;
            float z = LegacyMath.sin(angle) * 1.22F;
            float shoulder = tip + length * 0.30F;
            impactVertex(builder, 0, tip, 0, IMPACT_WHITE, 0.92F * strength);
            impactVertex(builder, x, shoulder, z, SEAL_BLUE, 0.72F * strength);
            impactVertex(builder, 0, lagged + 0.42F, 0, SEAL_BLUE, 0);
            impactVertex(builder, 0, tip, 0, IMPACT_WHITE, 0.92F * strength);
            impactVertex(builder, 0, lagged + 0.42F, 0, SEAL_BLUE, 0);
            impactVertex(builder, x, shoulder, z, SEAL_BLUE, 0.72F * strength);
        }
    }

    private void drawImpactFlash(LegacyBatch builder, float time, float y) {
        if (time < DamoclesTimeline.IMPACT || time >= DamoclesTimeline.IMPACT + 0.33F) {
            return;
        }
        float phase = DamoclesTimeline.progress(time, DamoclesTimeline.IMPACT, DamoclesTimeline.IMPACT + 0.33F);
        float radius = LegacyMath.lerp(phase, 2.0F, 5.0F);
        float alpha = 1 - phase * phase;
        // 在接触点用向外衰减的双锥闪光，保持白光集中在剑尖周围。
        for (int side = 0; side < 12; side++) {
            float a = side * LegacyMath.TWO_PI / 12;
            float b = (side + 1) * LegacyMath.TWO_PI / 12;
            float xa = LegacyMath.sin(a) * radius, za = LegacyMath.cos(a) * radius;
            float xb = LegacyMath.sin(b) * radius, zb = LegacyMath.cos(b) * radius;
            impactVertex(builder, 0, y + 1.72F, 0, IMPACT_WHITE, alpha);
            impactVertex(builder, xa, y, za, SEAL_BLUE, alpha * 0.32F);
            impactVertex(builder, xb, y, zb, SEAL_BLUE, alpha * 0.32F);
            impactVertex(builder, 0, y - 1.50F, 0, IMPACT_WHITE, alpha);
            impactVertex(builder, xb, y, zb, SEAL_BLUE, alpha * 0.32F);
            impactVertex(builder, xa, y, za, SEAL_BLUE, alpha * 0.32F);
        }
    }

    private static void drawChargeWave(LegacyBatch builder, Matrix4f pose, float time, float y) {
        float progress = DamoclesTimeline.progress(time, DamoclesTimeline.SPIN_END, DamoclesTimeline.PULLBACK_START);
        if (progress > 0 && progress < 1) {
            // 充能环在上提前快速向外释放，立体青蓝波面和窄亮边共用同一条波前。
            float radius = LegacyMath.lerp(progress * (2 - progress), 2.2F, CHARGE_WAVE_RADIUS);
            float alpha = 1 - progress * progress;
            float rim = LegacyMath.lerp(progress, 3.6F, 1.35F);
            ring(builder, pose, y, radius, LegacyMath.lerp(progress, 7.2F, 3.8F), rim, alpha * 0.82F, 0, CHARGE_CYAN);
            ring(builder, pose, y + 0.07F, radius - 0.30F, LegacyMath.lerp(progress, 1.95F, 0.85F),
                    rim * 0.63F, alpha, 0, CHARGE_CREST);
        }
    }

    private static void drawImpactWaves(LegacyBatch builder, Matrix4f pose, float time, float y) {
        float front = DamoclesTimeline.progress(time, DamoclesTimeline.HIT_STOP_END, DamoclesTimeline.HIT_STOP_END + 1.30F);
        if (front > 0 && front < 1) {
            float radius = LegacyMath.lerp(front * (2 - front), 2.5F, DamoclesTimeline.IMPACT_WAVE_RADIUS);
            float alpha = 1 - front * front;
            float rim = LegacyMath.lerp(front, 4.8F, 1.8F);
            // 白色波峰压在蓝色光墙前缘，横向光带与竖向外缘在不同观看高度都能读出。
            ring(builder, pose, y, radius, LegacyMath.lerp(front, 9.0F, 4.0F), rim, alpha * 0.80F, 0, WAVE_COBALT);
            ring(builder, pose, y + 0.06F, Math.max(0.5F, radius - 0.22F),
                    LegacyMath.lerp(front, 2.9F, 1.1F), rim * 0.64F, alpha, 0, IMPACT_WHITE);
        }

        float echo = DamoclesTimeline.progress(time, DamoclesTimeline.CORE_BREAK, DamoclesTimeline.CORE_BREAK + 2.25F);
        if (echo > 0 && echo < 1) {
            float radius = LegacyMath.lerp(echo * (1.46F - 0.46F * echo), 2.0F, DamoclesTimeline.AFTERSHOCK_RADIUS);
            float alpha = 1 - echo * echo;
            float width = LegacyMath.lerp(echo, 12.0F, 6.0F);
            // 余震从同一接触点推出更宽的紫蓝波带，并在内侧保留一道较暗的尾缘。
            ring(builder, pose, y - 0.65F, radius, width, LegacyMath.lerp(echo, 3.1F, 1.2F),
                    alpha * 0.82F, 0, AFTERSHOCK_VIOLET);
            float wakeRadius = radius - width - 2.8F;
            if (wakeRadius > 0) {
                ring(builder, pose, y - 1.38F, wakeRadius, LegacyMath.lerp(echo, 2.8F, 1.1F),
                        LegacyMath.lerp(echo, 0.86F, 0.4F), alpha * 0.58F, 0, SEAL_BLUE);
            }
        }
    }

    private static void ring(LegacyBatch builder, Matrix4f pose, float y, float radius, float width, float rim,
            float alpha, float scatter, int color) {
        int segments = 96;
        float inner = Math.max(0, radius - width);
        for (int segment = 0; segment < segments; segment++) {
            float from = segment * LegacyMath.TWO_PI / segments + scatter * 0.25F;
            float to = (segment + 1 - scatter * 0.86F) * LegacyMath.TWO_PI / segments + scatter * 0.25F;
            float height = y + scatter * (segment % 7) * 0.12F;
            // 上下两个绕序都提交，仰视时也能看见环面。竖向外缘在接近平视时保留光带的厚度。
            ringStrip(builder, pose, from, to, inner, radius, height, height, color, alpha);
            ringStrip(builder, pose, to, from, inner, radius, height, height, color, alpha);
            if (rim > 0) {
                ringStrip(builder, pose, to, from, radius, radius, height - rim, height + rim, color, alpha * 0.72F);
                ringStrip(builder, pose, from, to, radius, radius, height - rim, height + rim, color, alpha * 0.72F);
            }
        }
    }

    private static void ringStrip(LegacyBatch builder, Matrix4f pose, float from, float to, float inner, float outer,
            float low, float high, int color, float alpha) {
        ringVertex(builder, pose, from, inner, low, color, alpha);
        ringVertex(builder, pose, from, outer, high, color, alpha);
        ringVertex(builder, pose, to, outer, high, color, alpha);
        ringVertex(builder, pose, from, inner, low, color, alpha);
        ringVertex(builder, pose, to, outer, high, color, alpha);
        ringVertex(builder, pose, to, inner, low, color, alpha);
    }

    private static void ringVertex(LegacyBatch builder, Matrix4f pose, float angle, float radius, float y, int color, float alpha) {
        coloredVertex(builder, pose, LegacyMath.sin(angle) * radius, y, LegacyMath.cos(angle) * radius, color, alpha);
    }

    private void impactVertex(LegacyBatch builder, float x, float y, float z, int color, float alpha) {
        coloredVertex(builder, scenePose, x, y, z, color, alpha);
    }

    private static void coloredVertex(LegacyBatch builder, Matrix4f pose, float x, float y, float z, int color, float alpha) {
        builder.colorVertex(pose, x, y, z,
                (color >>> 16) / 255F, (color >>> 8 & 255) / 255F, (color & 255) / 255F, alpha);
    }

    private void drawSeal(LegacyBatch builder, float time, float scatter) {
        float reveal = sealReveal(time);
        if (reveal <= 0) {
            return;
        }

        float radius = reveal * SEAL_RADIUS;
        float rotation = Math.max(0, time - DamoclesTimeline.LIFT_START) * 0.43F;
        sealPose.rotateY(rotation);
        if (scatter == 0) {
            sealTile(builder, radius, 0, 0, 1, 1, 0, reveal);
        } else {
            // 沿原徽记的 UV 分裂，碎片保留地图、五角星或字形。
            for (int z = 0; z < SEAL_TILES; z++) {
                for (int x = 0; x < SEAL_TILES; x++) {
                    sealTile(builder, radius, (float) x / SEAL_TILES, (float) z / SEAL_TILES,
                            (float) (x + 1) / SEAL_TILES, (float) (z + 1) / SEAL_TILES, scatter, 1 - scatter);
                }
            }
        }
    }

    private static float sealReveal(float time) {
        return DamoclesTimeline.ease(time, DamoclesTimeline.LIFT_START, DamoclesTimeline.LIFT_END);
    }

    private void sealTile(LegacyBatch builder, float radius, float u0, float v0, float u1, float v1,
            float scatter, float alpha) {
        float x = (u0 + u1 - 1) * radius;
        float z = (v0 + v1 - 1) * radius;
        float halfX = (u1 - u0) * radius * (1 - scatter * 0.85F);
        float halfZ = (v1 - v0) * radius * (1 - scatter * 0.85F);
        float y = 0.008F + scatter * (1.6F + 0.18F * LegacyMath.abs(x - z));
        x *= 1 + scatter * 0.45F;
        z *= 1 + scatter * 0.45F;
        sealVertex(builder, x - halfX, y, z - halfZ, u0, v0, alpha);
        sealVertex(builder, x - halfX, y, z + halfZ, u0, v1, alpha);
        sealVertex(builder, x + halfX, y, z + halfZ, u1, v1, alpha);
        sealVertex(builder, x + halfX, y, z - halfZ, u1, v0, alpha);
    }

    private void sealVertex(LegacyBatch builder, float x, float y, float z, float u, float v, float alpha) {
        builder.textureVertex(sealPose, x, y, z, u, v, 0.9F, 0.96F, 1, alpha);
    }

}
