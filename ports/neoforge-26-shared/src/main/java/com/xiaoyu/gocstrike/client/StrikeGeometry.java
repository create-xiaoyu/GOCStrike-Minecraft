package com.xiaoyu.gocstrike.client;

import java.io.IOException;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.xiaoyu.gocstrike.StrikeEntity;
import org.joml.Matrix4f;
import net.minecraft.world.phys.Vec3;

final class StrikeGeometry {
    private final EmplacementAnimation animation;
    private final Matrix4f strikeRoot = new Matrix4f();

    StrikeGeometry() {
        try {
            animation = new EmplacementAnimation();
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot load thaumaturgic strike animation", exception);
        }
    }

    void appendStrike(VertexConsumer sink, StrikeEntity entity, float partialTick, Vec3 camera) {
        Vec3 relative = entity.position().subtract(camera);
        strikeRoot.translation((float) relative.x, (float) relative.y, (float) relative.z).scale(StrikeEntity.WORLD_SCALE);
        for (var node : animation.sample(entity.elapsed(partialTick), strikeRoot)) {
            if (!node.visible || node.vertices == null || node.pose[13] <= 0) {
                continue;
            }
            float[] pose = node.pose;
            float[] vertices = node.vertices;
            for (int vertex = 0; vertex < vertices.length; vertex += 3) {
                sink.addVertex(node.matrix, vertices[vertex], vertices[vertex + 1], vertices[vertex + 2])
                        .setColor(pose[10], pose[11], pose[12], pose[13]);
            }
        }
    }

    void appendDamoclesCore(VertexConsumer builder, Matrix4f root, float authoredTime, float spinTime,
            float whiteMatter, float impactKick, float scatter) {
        var nodes = animation.sampleCore(authoredTime, root);
        var star = nodes[EmplacementAnimation.CORE_STAR_NODE];
        // 环层沿用原本的展开时间。白色中心另取原动画中已成形的那一拍。
        float heightOffset = -star.pose[1] * root.m11();
        if (whiteMatter > 0) {
            animation.sampleCoreStar(Math.max(authoredTime, 84.1F), root);
        }
        float starHeightOffset = -star.pose[1] * root.m11();
        var layerSpin = new Matrix4f();
        var fragment = new Matrix4f();
        int layer = -1;
        for (int i = 0; i < EmplacementAnimation.coreNodeCount(); i++) {
            var node = nodes[i];
            if (node.parent == 0) {
                // 延续原动画各层交替的转向，同时绕法阵中心增加少量转速。
                layer++;
                float direction = layer % 2 == 0 ? 1 : -1;
                float angle = (spinTime * (0.04F + 0.006F * layer) + impactKick * 0.085F) * direction;
                // 余震先把各层沿径向撑开并拉高层间距，随后才进入逐块分解。
                float spread = 1 + impactKick * (0.08F - layer * 0.008F);
                float recoil = impactKick * (layer - 2.5F) * 0.44F;
                layerSpin.translation(root.m30(), recoil, root.m32()).rotateY(angle).scale(spread, 1, spread)
                        .translate(-root.m30(), 0, -root.m32());
            }
            boolean inner = i >= EmplacementAnimation.CORE_STAR_NODE;
            if (!node.visible || node.vertices == null || node.pose[13] <= 0 || (inner && whiteMatter <= 0)) {
                continue;
            }
            float phase = i * 2.399963F;
            float drift = scatter * (1.4F + (i % 7) * 0.35F);
            fragment.set(layerSpin).mul(node.matrix);
            fragment.m31(fragment.m31() + (inner ? starHeightOffset : heightOffset));
            if (scatter > 0) {
                fragment.m30(fragment.m30() + (float) Math.cos(phase) * drift);
                fragment.m31(fragment.m31() + scatter * ((i % 5) - 1) * 0.65F);
                fragment.m32(fragment.m32() + (float) Math.sin(phase) * drift);
                fragment.rotateY(scatter * ((i % 3) - 1)).scale(1 - scatter * 0.85F);
            }
            float[] color = node.pose;
            float[] vertices = node.vertices;
            float alpha = color[13] * (1 - scatter) * (inner ? whiteMatter : 1);
            for (int vertex = 0; vertex < vertices.length; vertex += 3) {
                builder.addVertex(fragment, vertices[vertex], vertices[vertex + 1], vertices[vertex + 2])
                        .setColor(color[10], color[11], color[12], alpha);
            }
        }
    }

}
