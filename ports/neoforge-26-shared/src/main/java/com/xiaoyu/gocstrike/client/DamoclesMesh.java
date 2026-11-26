package com.xiaoyu.gocstrike.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.util.Mth;

final class DamoclesMesh {
    private final List<Fragment> fragments;

    DamoclesMesh(QuadCollection geometry) {
        var faces = new ArrayList<Face>();
        for (BakedQuad quad : geometry.getAll()) {
            faces.add(readFace(quad));
        }

        // 连在同一条边上的面跟随同一块碎片。连续剑身按截面分段，护手与握柄保留原有小部件。
        int[] parents = new int[faces.size()];
        Map<Edge, Integer> owners = new HashMap<>();
        for (int i = 0; i < faces.size(); i++) {
            parents[i] = i;
            Face face = faces.get(i);
            for (int vertex = 0; vertex < 4; vertex++) {
                Point a = face.vertices[vertex].point;
                Point b = face.vertices[(vertex + 1) % 4].point;
                if (!a.equals(b)) {
                    Edge edge = a.compareTo(b) < 0 ? new Edge(a, b, face.band) : new Edge(b, a, face.band);
                    Integer owner = owners.putIfAbsent(edge, i);
                    if (owner != null) {
                        parents[find(parents, owner)] = find(parents, i);
                    }
                }
            }
        }
        Map<Integer, List<Face>> parts = new LinkedHashMap<>();
        for (int i = 0; i < faces.size(); i++) {
            parts.computeIfAbsent(find(parents, i), key -> new ArrayList<>()).add(faces.get(i));
        }
        fragments = parts.values().stream().map(Fragment::new).toList();
    }

    void draw(VertexConsumer builder, Matrix4f pose, float printed, float charge, float scatter) {
        var piece = new Matrix4f();
        var clipped = new Vertex[6];
        float cut = 1 - printed;
        int index = 0;
        for (Fragment fragment : fragments) {
            piece.set(pose);
            if (scatter > 0) {
                float angle = index * 2.399963F;
                float distance = scatter * (0.19F + (index % 9) * 0.038F);
                Point center = fragment.center;
                piece.translate(center.x + Mth.cos(angle) * distance,
                        center.y + ((index % 5) - 2) * scatter * 0.10F, center.z + Mth.sin(angle) * distance)
                        .rotateXYZ(scatter * (index % 3 - 1), angle * scatter, scatter)
                        .scale(1 - 0.9F * scatter).translate(-center.x, -center.y, -center.z);
            }
            for (Face face : fragment.faces) {
                Vertex[] polygon = face.vertices;
                int count = polygon.length;
                if (cut > 0) {
                    count = clip(polygon, cut, clipped);
                    polygon = clipped;
                }
                for (int vertex = 1; vertex + 1 < count; vertex++) {
                    emit(builder, piece, polygon[0], cut, charge, scatter);
                    emit(builder, piece, polygon[vertex], cut, charge, scatter);
                    emit(builder, piece, polygon[vertex + 1], cut, charge, scatter);
                }
            }
            index++;
        }
    }

    // 剑尖位于模型 Y=1。只保留扫描面走过的部分，并在穿过面时插入新的边缘顶点。
    private static int clip(Vertex[] polygon, float cut, Vertex[] output) {
        int count = 0;
        Vertex previous = polygon[polygon.length - 1];
        for (Vertex current : polygon) {
            boolean inside = current.point.y >= cut;
            if (inside != (previous.point.y >= cut)) {
                output[count++] = previous.intersect(current, cut);
            }
            if (inside) {
                output[count++] = current;
            }
            previous = current;
        }
        return count;
    }

    private static void emit(VertexConsumer builder, Matrix4f pose, Vertex vertex, float cut, float charge, float scatter) {
        Point point = vertex.point;
        int color = vertex.color;
        float lit = Mth.clamp((charge * 1.14F - point.y) / 0.14F, 0, 1);
        if (cut > 0) {
            float edge = 1 - Mth.clamp((point.y - cut) / 0.035F, 0, 1);
            lit = Math.max(lit, edge * 0.86F);
        }
        float gain = Mth.lerp(lit, 0.69F, 1F);
        builder.addVertex(pose, point.x, point.y, point.z).setColor(
                (color >>> 16 & 255) * gain / 255F, (color >>> 8 & 255) * gain / 255F,
                (color & 255) * Mth.lerp(lit, 0.86F, 1F) / 255F,
                (color >>> 24) * (1 - scatter) / 255F);
    }

    private static Face readFace(BakedQuad quad) {
        var face = new Vertex[4];
        float midpoint = 0;
        for (int index = 0; index < 4; index++) {
            var vertex = quad.position(index);
            var point = new Point(vertex.x(), vertex.y(), vertex.z());
            face[index] = new Vertex(point, quad.bakedColors().color(index));
            midpoint += point.y / 4;
        }
        int band = midpoint >= 0.83F ? 3 : midpoint >= 0.55F ? 2 : midpoint > 0.26F ? 1 : 0;
        return new Face(face, band);
    }

    private static int find(int[] parents, int index) {
        while (parents[index] != index) {
            parents[index] = parents[parents[index]];
            index = parents[index];
        }
        return index;
    }

    private record Point(float x, float y, float z) implements Comparable<Point> {
        @Override
        public int compareTo(Point other) {
            int compare = Float.compare(x, other.x);
            if (compare == 0) {
                compare = Float.compare(y, other.y);
            }
            return compare == 0 ? Float.compare(z, other.z) : compare;
        }
    }

    private record Edge(Point start, Point end, int band) { }

    private record Vertex(Point point, int color) {
        Vertex intersect(Vertex end, float y) {
            float fraction = (y - point.y) / (end.point.y - point.y);
            var crossing = new Point(Mth.lerp(fraction, point.x, end.point.x), y,
                    Mth.lerp(fraction, point.z, end.point.z));
            int result = 0;
            for (int shift = 0; shift < 32; shift += 8) {
                result |= Math.round(Mth.lerp(fraction, color >>> shift & 255, end.color >>> shift & 255)) << shift;
            }
            return new Vertex(crossing, result);
        }
    }

    private record Face(Vertex[] vertices, int band) { }

    private record Fragment(List<Face> faces, Point center) {
        Fragment(List<Face> faces) {
            this(faces, center(faces));
        }

        private static Point center(List<Face> faces) {
            float x = 0, y = 0, z = 0;
            for (Face face : faces) {
                for (Vertex vertex : face.vertices) {
                    Point point = vertex.point;
                    x += point.x;
                    y += point.y;
                    z += point.z;
                }
            }
            float weight = 1F / (4 * faces.size());
            return new Point(x * weight, y * weight, z * weight);
        }
    }
}
