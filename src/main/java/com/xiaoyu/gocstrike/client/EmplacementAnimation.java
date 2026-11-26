package com.xiaoyu.gocstrike.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.joml.Matrix4f;
import org.joml.Quaternionf;

/** The original Unity choreography, sampled at 20 Hz with constant runs removed. */
public final class EmplacementAnimation {
    private static final int STRIDE = 14;
    // 六个主体层是 1 号节点到 1777 号节点；第六层是法阵中央的白色五角星。
    static final int CORE_STAR_NODE = 1748;
    private static final int CORE_NODE_COUNT = 1778;
    private final Map<Integer, float[]> meshes = new HashMap<>();
    private final Node[] nodes;
    private final int sampleRate;
    private final Quaternionf rotation = new Quaternionf();
    private final Quaternionf nextRotation = new Quaternionf();

    public EmplacementAnimation() throws IOException {
        InputStream resource = EmplacementAnimation.class.getResourceAsStream("/assets/gocstrike/animation/emplacement.bin.gz");
        if (resource == null) {
            throw new IOException("Missing emplacement animation");
        }
        ByteBuffer data;
        try (InputStream input = resource; GZIPInputStream stream = new GZIPInputStream(input)) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] chunk = new byte[16384];
            int count;
            while ((count = stream.read(chunk)) != -1) {
                bytes.write(chunk, 0, count);
            }
            data = ByteBuffer.wrap(bytes.toByteArray()).order(ByteOrder.LITTLE_ENDIAN);
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
                    float decoded = data.getFloat();
                    // Version 1 stores sqrt-encoded HDR ratios. Decode their original saturation.
                    if (value >= 10 && value <= 12) {
                        decoded *= decoded;
                    }
                    keys[key * STRIDE + value] = decoded;
                }
            }
            nodes[i] = new Node(parent, meshes.get(mesh), frames, keys);
        }
        if (data.hasRemaining()) {
            throw new IOException("Unexpected trailing animation data");
        }
    }

    public int nodeCount() {
        return nodes.length;
    }

    public Node[] sample(float seconds, Matrix4f root) {
        sample(seconds, root, 0, nodes.length);
        return nodes;
    }

    Node[] sampleCore(float seconds, Matrix4f root) {
        sample(seconds, root, 0, CORE_NODE_COUNT);
        return nodes;
    }

    void sampleCoreStar(float seconds, Matrix4f root) {
        sample(seconds, root, CORE_STAR_NODE, CORE_NODE_COUNT);
    }

    static int coreNodeCount() {
        return CORE_NODE_COUNT;
    }

    private void sample(float seconds, Matrix4f root, int start, int end) {
        float frame = seconds * sampleRate;
        for (int index = start; index < end; index++) {
            Node node = nodes[index];
            if (node.parent >= 0 && !nodes[node.parent].visible) {
                node.visible = false;
                continue;
            }
            if (node.frames.length > 1) {
                int key = node.cursor;
                if (frame < node.frames[key]) {
                    key = Arrays.binarySearch(node.frames, (int) frame);
                    key = key < 0 ? Math.max(0, -key - 2) : key;
                }
                while (key + 1 < node.frames.length && node.frames[key + 1] <= frame) {
                    key++;
                }
                node.cursor = key;
                int next = Math.min(key + 1, node.frames.length - 1);
                float weight = key == next ? 0 : Math.max(0, Math.min(1, (frame - node.frames[key]) / (node.frames[next] - node.frames[key])));
                int a = key * STRIDE;
                int b = next * STRIDE;
                float[] k = node.keys;
                for (int v = 0; v < STRIDE; v++) {
                    node.pose[v] = k[a + v] + (k[b + v] - k[a + v]) * weight;
                }
                rotation.set(k[a + 3], k[a + 4], k[a + 5], k[a + 6]);
                nextRotation.set(k[b + 3], k[b + 4], k[b + 5], k[b + 6]);
                rotation.slerp(nextRotation, weight);
                float[] p = node.pose;
                node.local.translationRotateScale(p[0], p[1], p[2], rotation.x, rotation.y, rotation.z, rotation.w,
                        p[7], p[8], p[9]);
            }
            float[] p = node.pose;
            node.visible = Math.max(p[7], Math.max(p[8], p[9])) > 0.00011F;
            if (node.visible) {
                Matrix4f parent = node.parent < 0 ? root : nodes[node.parent].matrix;
                node.matrix.set(parent).mul(node.local);
            }
        }
    }

    public static final class Node {
        final int parent;
        final float[] vertices;
        final int[] frames;
        final float[] keys;
        final float[] pose = new float[STRIDE];
        final Matrix4f matrix = new Matrix4f();
        final Matrix4f local = new Matrix4f();
        int cursor;
        boolean visible;

        Node(int parent, float[] vertices, int[] frames, float[] keys) {
            this.parent = parent;
            this.vertices = vertices;
            this.frames = frames;
            this.keys = keys;
            System.arraycopy(keys, 0, pose, 0, STRIDE);
            local.translationRotateScale(pose[0], pose[1], pose[2], pose[3], pose[4], pose[5], pose[6],
                    pose[7], pose[8], pose[9]);
        }
    }
}
