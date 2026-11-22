package com.xiaoyu.gocstrike.client;

import java.nio.FloatBuffer;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GLContext;

final class LegacyBatch {
    private static final int COMPONENTS = 9;
    private static final int STRIDE = COMPONENTS * Float.BYTES;

    private final int primitive;
    private final boolean textured;
    private final Vector3f position = new Vector3f();
    private FloatBuffer vertices;

    LegacyBatch(int primitive, boolean textured, int capacity) {
        this.primitive = primitive;
        this.textured = textured;
        vertices = BufferUtils.createFloatBuffer(capacity * COMPONENTS);
    }

    void clear() {
        vertices.clear();
    }

    void colorVertex(Matrix4f pose, float x, float y, float z, float red, float green, float blue, float alpha) {
        textureVertex(pose, x, y, z, 0, 0, red, green, blue, alpha);
    }

    void textureVertex(Matrix4f pose, float x, float y, float z, float u, float v, float red, float green, float blue, float alpha) {
        if (vertices.remaining() < COMPONENTS) {
            FloatBuffer expanded = BufferUtils.createFloatBuffer(Math.multiplyExact(vertices.capacity(), 2));
            vertices.flip();
            expanded.put(vertices);
            vertices = expanded;
        }
        position.set(x, y, z);
        pose.transformPosition(position);
        vertices.put(position.x).put(position.y).put(position.z);
        vertices.put(LegacyMath.clamp(red, 0F, 1F)).put(LegacyMath.clamp(green, 0F, 1F))
                .put(LegacyMath.clamp(blue, 0F, 1F)).put(LegacyMath.clamp(alpha, 0F, 1F));
        vertices.put(u).put(v);
    }

    boolean isEmpty() {
        return vertices.position() == 0;
    }

    void draw() {
        int count = vertices.position() / COMPONENTS;
        if (count == 0) {
            return;
        }
        boolean vboSupported = GLContext.getCapabilities().OpenGL15;
        int boundBuffer = vboSupported ? GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING) : 0;
        GL11.glPushClientAttrib(GL11.GL_CLIENT_VERTEX_ARRAY_BIT);
        try {
            if (vboSupported) {
                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
            }
            FloatBuffer input = (FloatBuffer) vertices.duplicate().flip();
            GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
            GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);
            GL11.glVertexPointer(3, STRIDE, input);
            input.position(3);
            GL11.glColorPointer(4, STRIDE, input);
            if (textured) {
                GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
                input.position(7);
                GL11.glTexCoordPointer(2, STRIDE, input);
            } else {
                GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
            }
            GL11.glDisableClientState(GL11.GL_NORMAL_ARRAY);
            GL11.glDrawArrays(primitive, 0, count);
        } finally {
            GL11.glPopClientAttrib();
            if (vboSupported) {
                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, boundBuffer);
            }
        }
    }
}
