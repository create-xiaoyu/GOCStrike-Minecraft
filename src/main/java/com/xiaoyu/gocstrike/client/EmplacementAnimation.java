package com.xiaoyu.gocstrike.client;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.joml.Matrix4f;
import org.joml.Quaternionf;

import com.xiaoyu.gocstrike.GOCStrike;

/** The original Unity choreography, sampled at 20 Hz with constant runs removed. */
public final class EmplacementAnimation {
    private static final int STRIDE = 14;
    private final Map<Integer, float[]> meshes = new HashMap<>();
    private final Node[] nodes;
    private final int sampleRate;
    private final Quaternionf rotation = new Quaternionf();
    private final Quaternionf nextRotation = new Quaternionf();

    public EmplacementAnimation() throws IOException {
        var resource = EmplacementAnimation.class.getResourceAsStream("/assets/gocstrike/animation/emplacement.bin.gz");
        if (resource == null) {
            throw new IOException("Missing emplacement animation");
        }
        ByteBuffer data;
        try (var stream = new GZIPInputStream(resource)) {
            data = ByteBuffer.wrap(stream.readAllBytes()).order(ByteOrder.LITTLE_ENDIAN);
        }
        if (data.getInt() != 0x474f4331 || data.getInt() != 1) {
            throw new IOException("Unsupported emplacement animation format");
        }
        data.getFloat(); // Duration is shared with the server entity.
        sampleRate = data.getInt();
        int meshCount = data.getInt();
        for (int i = 0; i < meshCount; i++) {
            int type = data.getInt();
            float[] vertices = new float[data.getInt() * 3];
            for (int v = 0; v < vertices.length; v++) {
                vertices[v] = data.getFloat();
            }
            meshes.put(type, vertices);
        }
        nodes = new Node[data.getInt()];
        for (int i = 0; i < nodes.length; i++) {
            int parent = data.getInt();
            int mesh = data.getInt();
            int count = data.getInt();
            if (parent >= i || parent < -1 || count < 1) {
                throw new IOException("Invalid animation hierarchy at node " + i);
            }
            int[] frames = new int[count];
            float[] keys = new float[count * STRIDE];
            for (int key = 0; key < count; key++) {
                frames[key] = data.getInt();
                for (int value = 0; value < STRIDE; value++) {
                    keys[key * STRIDE + value] = data.getFloat();
                }
            }
            nodes[i] = new Node(parent, meshes.get(mesh), frames, keys);
        }
        if (data.hasRemaining()) {
            throw new IOException("Unexpected trailing animation data");
        }
        GOCStrike.LOGGER.info("Loaded thaumaturgic strike: {} nodes", nodes.length);
    }

    public Node[] sample(float seconds, Matrix4f root) {
        float frame = seconds * sampleRate;
        for (Node node : nodes) {
            int key = Arrays.binarySearch(node.frames, (int) frame);
            if (key < 0) {
                key = Math.max(0, -key - 2);
            }
            int next = Math.min(key + 1, node.frames.length - 1);
            float weight = key == next ? 0 : Math.clamp((frame - node.frames[key]) / (node.frames[next] - node.frames[key]), 0, 1);
            int a = key * STRIDE;
            int b = next * STRIDE;
            float[] k = node.keys;
            for (int v = 0; v < STRIDE; v++) {
                node.pose[v] = k[a + v] + (k[b + v] - k[a + v]) * weight;
            }
            rotation.set(k[a + 3], k[a + 4], k[a + 5], k[a + 6]);
            nextRotation.set(k[b + 3], k[b + 4], k[b + 5], k[b + 6]);
            rotation.slerp(nextRotation, weight);
            Matrix4f parent = node.parent < 0 ? root : nodes[node.parent].matrix;
            float[] p = node.pose;
            node.matrix.set(parent).translate(p[0], p[1], p[2]).rotate(rotation).scale(p[7], p[8], p[9]);
            node.visible = (node.parent < 0 || nodes[node.parent].visible)
                    && Math.max(p[7], Math.max(p[8], p[9])) > 0.00011F;
        }
        return nodes;
    }

    public static final class Node {
        final int parent;
        final float[] vertices;
        final int[] frames;
        final float[] keys;
        final float[] pose = new float[STRIDE];
        final Matrix4f matrix = new Matrix4f();
        boolean visible;

        Node(int parent, float[] vertices, int[] frames, float[] keys) {
            this.parent = parent;
            this.vertices = vertices;
            this.frames = frames;
            this.keys = keys;
        }
    }
}
