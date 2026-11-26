package com.xiaoyu.gocstrike.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Objects;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.joml.Matrix4f;

final class DamoclesMesh {
    private static final String ASSET = "/assets/gocstrike/models/item/damocles_sword.";
    private final List<Fragment> fragments;

    DamoclesMesh() throws IOException {
        List<Face> faces = readFaces();

        // 连在同一条边上的面跟随同一块碎片。连续剑身按截面分段，护手与握柄保留原有小部件。
        int[] parents = new int[faces.size()];
        Map<Edge, Integer> owners = new HashMap<>();
        for (int i = 0; i < faces.size(); i++) {
            parents[i] = i;
            Face face = faces.get(i);
            for (int vertex = 0; vertex < face.vertices.length; vertex++) {
                Point a = face.vertices[vertex].point;
                Point b = face.vertices[(vertex + 1) % face.vertices.length].point;
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
        fragments = new ArrayList<>(parts.size());
        for (List<Face> part : parts.values()) {
            fragments.add(new Fragment(part));
        }
    }

    void draw(LegacyBatch builder, Matrix4f pose, float printed, float charge, float scatter) {
        Matrix4f piece = new Matrix4f();
        Vertex[] clipped = new Vertex[6];
        float cut = 1 - printed;
        int index = 0;
        for (Fragment fragment : fragments) {
            piece.set(pose);
            if (scatter > 0) {
                float angle = index * 2.399963F;
                float distance = scatter * (0.19F + (index % 9) * 0.038F);
                Point center = fragment.center;
                piece.translate(center.x + LegacyMath.cos(angle) * distance,
                        center.y + ((index % 5) - 2) * scatter * 0.10F, center.z + LegacyMath.sin(angle) * distance)
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

    private static void emit(LegacyBatch builder, Matrix4f pose, Vertex vertex, float cut, float charge, float scatter) {
        Point point = vertex.point;
        int color = vertex.color;
        float lit = LegacyMath.clamp((charge * 1.14F - point.y) / 0.14F, 0, 1);
        if (cut > 0) {
            float edge = 1 - LegacyMath.clamp((point.y - cut) / 0.035F, 0, 1);
            lit = Math.max(lit, edge * 0.86F);
        }
        float gain = LegacyMath.lerp(lit, 0.69F, 1F);
        builder.colorVertex(pose, point.x, point.y, point.z,
                (color >>> 16 & 255) * gain / 255F, (color >>> 8 & 255) * gain / 255F,
                (color & 255) * LegacyMath.lerp(lit, 0.86F, 1F) / 255F,
                (color >>> 24) * (1 - scatter) / 255F);
    }

    // 现有 OBJ 的颜色来自 MTL 漫反射颜色。旧版 Forge 的 OBJ 渲染器不保留这组颜色。
    private static List<Face> readFaces() throws IOException {
        Map<String, Integer> materials = readMaterials();
        List<Point> positions = new ArrayList<>();
        List<Face> faces = new ArrayList<>();
        int color = -1;
        try (BufferedReader reader = open("obj")) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] fields = line.trim().split("\\s+");
                if ("v".equals(fields[0])) {
                    positions.add(new Point(Float.parseFloat(fields[1]), Float.parseFloat(fields[2]), Float.parseFloat(fields[3])));
                } else if ("usemtl".equals(fields[0])) {
                    Integer material = materials.get(fields[1]);
                    if (material == null) {
                        throw new IOException("Unknown sword material " + fields[1]);
                    }
                    color = material;
                } else if ("f".equals(fields[0])) {
                    Vertex[] vertices = new Vertex[fields.length - 1];
                    float midpoint = 0;
                    for (int i = 0; i < vertices.length; i++) {
                        int reference = Integer.parseInt(fields[i + 1].split("/", 2)[0]);
                        Point position = positions.get(reference < 0 ? positions.size() + reference : reference - 1);
                        vertices[i] = new Vertex(position, color);
                        midpoint += position.y / vertices.length;
                    }
                    int band = midpoint >= 0.83F ? 3 : midpoint >= 0.55F ? 2 : midpoint > 0.26F ? 1 : 0;
                    faces.add(new Face(vertices, band));
                }
            }
        }
        if (faces.isEmpty()) {
            throw new IOException("The sword model contains no faces");
        }
        return faces;
    }

    private static Map<String, Integer> readMaterials() throws IOException {
        Map<String, Integer> colors = new HashMap<>();
        String material = null;
        try (BufferedReader reader = open("mtl")) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] fields = line.trim().split("\\s+");
                if ("newmtl".equals(fields[0])) {
                    material = fields[1];
                } else if ("Kd".equals(fields[0]) && material != null) {
                    int red = Math.round(Float.parseFloat(fields[1]) * 255);
                    int green = Math.round(Float.parseFloat(fields[2]) * 255);
                    int blue = Math.round(Float.parseFloat(fields[3]) * 255);
                    colors.put(material, 0xff000000 | red << 16 | green << 8 | blue);
                }
            }
        }
        return colors;
    }

    private static BufferedReader open(String extension) throws IOException {
        InputStream stream = DamoclesMesh.class.getResourceAsStream(ASSET + extension);
        if (stream == null) {
            throw new IOException("Missing sword asset " + ASSET + extension);
        }
        return new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
    }

    private static int find(int[] parents, int index) {
        while (parents[index] != index) {
            parents[index] = parents[parents[index]];
            index = parents[index];
        }
        return index;
    }

    private static final class Point implements Comparable<Point> {
        private final float x;
        private final float y;
        private final float z;

        private Point(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Point && compareTo((Point) other) == 0;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y, z);
        }

        @Override
        public int compareTo(Point other) {
            int compare = Float.compare(x, other.x);
            if (compare == 0) {
                compare = Float.compare(y, other.y);
            }
            return compare == 0 ? Float.compare(z, other.z) : compare;
        }
    }

    private static final class Edge {
        private final Point start;
        private final Point end;
        private final int band;

        private Edge(Point start, Point end, int band) {
            this.start = start;
            this.end = end;
            this.band = band;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof Edge)) {
                return false;
            }
            Edge edge = (Edge) other;
            return band == edge.band && start.equals(edge.start) && end.equals(edge.end);
        }

        @Override
        public int hashCode() {
            return Objects.hash(start, end, band);
        }
    }

    private static final class Vertex {
        private final Point point;
        private final int color;

        private Vertex(Point point, int color) {
            this.point = point;
            this.color = color;
        }

        Vertex intersect(Vertex end, float y) {
            float fraction = (y - point.y) / (end.point.y - point.y);
            Point crossing = new Point(LegacyMath.lerp(fraction, point.x, end.point.x), y,
                    LegacyMath.lerp(fraction, point.z, end.point.z));
            int result = 0;
            for (int shift = 0; shift < 32; shift += 8) {
                result |= Math.round(LegacyMath.lerp(fraction, color >>> shift & 255, end.color >>> shift & 255)) << shift;
            }
            return new Vertex(crossing, result);
        }
    }

    private static final class Face {
        private final Vertex[] vertices;
        private final int band;

        private Face(Vertex[] vertices, int band) {
            this.vertices = vertices;
            this.band = band;
        }
    }

    private static final class Fragment {
        private final List<Face> faces;
        private final Point center;

        Fragment(List<Face> faces) {
            this.faces = faces;
            this.center = center(faces);
        }

        private static Point center(List<Face> faces) {
            float x = 0, y = 0, z = 0;
            int count = 0;
            for (Face face : faces) {
                for (Vertex vertex : face.vertices) {
                    count++;
                    Point point = vertex.point;
                    x += point.x;
                    y += point.y;
                    z += point.z;
                }
            }
            float weight = 1F / count;
            return new Point(x * weight, y * weight, z * weight);
        }
    }
}
