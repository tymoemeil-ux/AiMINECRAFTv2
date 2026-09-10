package com.aiminecraft.world;

import com.aiminecraft.render.Mesh;
import com.aiminecraft.render.TextureAtlas;

import java.util.Arrays;

/**
 * Buduje siatki chunka: pelne bloki, krzyzyki roslin/pochodni
 * i osobna przezroczysta woda.
 */
public final class ChunkMesher {

    private ChunkMesher() {
    }

    // 6 scian: +X, -X, +Y, -Y, +Z, -Z.
    private static final int[][] NEIGHBOR = {
            {1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}
    };

    private static final float[][][] CORNERS = {
            {{1, 0, 1}, {1, 0, 0}, {1, 1, 0}, {1, 1, 1}}, // +X
            {{0, 0, 0}, {0, 0, 1}, {0, 1, 1}, {0, 1, 0}}, // -X
            {{0, 1, 1}, {1, 1, 1}, {1, 1, 0}, {0, 1, 0}}, // +Y (gora)
            {{0, 0, 0}, {1, 0, 0}, {1, 0, 1}, {0, 0, 1}}, // -Y (dol)
            {{0, 0, 1}, {1, 0, 1}, {1, 1, 1}, {0, 1, 1}}, // +Z
            {{1, 0, 0}, {0, 0, 0}, {0, 1, 0}, {1, 1, 0}}  // -Z
    };

    private static final float[][] UV = {{0, 0}, {1, 0}, {1, 1}, {0, 1}};
    private static final float[] SHADE = {0.65f, 0.65f, 1.0f, 0.5f, 0.8f, 0.8f};

    // 0 = tekstura gory, 1 = dolu, 2 = boku.
    private static final int[] TILE_KIND = {2, 2, 0, 1, 2, 2};

    /** Wysokosc tafli wody. */
    private static final float WATER_TOP = 0.9f;

    /** Wynik: siatka nieprzezroczysta + przezroczysta. */
    public static final class MeshPair {
        public final Mesh opaque;
        public final Mesh translucent;

        public MeshPair(Mesh opaque, Mesh translucent) {
            this.opaque = opaque;
            this.translucent = translucent;
        }
    }

    public static MeshPair build(World world, Chunk chunk, TextureAtlas atlas) {
        FloatBuilder vOpaque = new FloatBuilder();
        IntBuilder iOpaque = new IntBuilder();
        FloatBuilder vTrans = new FloatBuilder();
        IntBuilder iTrans = new IntBuilder();
        int baseX = chunk.getCx() * Chunk.SIZE;
        int baseZ = chunk.getCz() * Chunk.SIZE;

        for (int y = 0; y < Chunk.HEIGHT; y++) {
            for (int z = 0; z < Chunk.SIZE; z++) {
                for (int x = 0; x < Chunk.SIZE; x++) {
                    byte id = chunk.getLocal(x, y, z);
                    if (id == Block.AIR.id) {
                        continue;
                    }
                    Block block = Block.fromId(id);
                    int wx = baseX + x;
                    int wz = baseZ + z;
                    if (block.kind == Block.Kind.WATER) {
                        buildWater(world, atlas, vTrans, iTrans, wx, y, wz);
                    } else if (block.kind == Block.Kind.CROSS) {
                        buildCross(atlas, vOpaque, iOpaque, wx, y, wz, block.tileSide, 0.15f, 0.85f, 1.0f);
                    } else if (block.kind == Block.Kind.TORCH) {
                        buildCross(atlas, vOpaque, iOpaque, wx, y, wz, block.tileSide, 0.375f, 0.625f, 0.625f);
                    } else {
                        buildCube(world, atlas, vOpaque, iOpaque, block, wx, y, wz);
                    }
                }
            }
        }
        return new MeshPair(
                new Mesh(vOpaque.toArray(), iOpaque.toArray()),
                new Mesh(vTrans.toArray(), iTrans.toArray()));
    }

    private static void buildCube(World world, TextureAtlas atlas,
                                  FloatBuilder vertices, IntBuilder indices,
                                  Block block, int wx, int y, int wz) {
        for (int face = 0; face < 6; face++) {
            int[] n = NEIGHBOR[face];
            Block neighbor = Block.fromId(world.getBlock(wx + n[0], y + n[1], wz + n[2]));
            // Liscie rysuja sciany miedzy soba (fancy), szyba ukrywa sciany z szyba.
            if (neighbor.opaque && (!block.isLeaves() || !neighbor.isLeaves())) {
                continue;
            }
            if (block == Block.GLASS && neighbor == Block.GLASS) {
                continue;
            }
            int kind = TILE_KIND[face];
            int tile = kind == 0 ? block.tileTop : (kind == 1 ? block.tileBottom : block.tileSide);
            float u0 = atlas.u0(tile);
            float u1 = atlas.u1(tile);
            float v0 = atlas.v0(tile);
            float v1 = atlas.v1(tile);
            float shade = SHADE[face];
            int base = vertices.count / 6;
            float[][] corners = CORNERS[face];
            for (int c = 0; c < 4; c++) {
                vertices.put(
                        wx + corners[c][0], y + corners[c][1], wz + corners[c][2],
                        UV[c][0] == 0 ? u0 : u1,
                        UV[c][1] == 0 ? v0 : v1,
                        shade);
            }
            indices.put(base, base + 1, base + 2, base, base + 2, base + 3);
        }
    }

    private static void buildWater(World world, TextureAtlas atlas,
                                   FloatBuilder vertices, IntBuilder indices,
                                   int wx, int y, int wz) {
        int tile = Block.WATER.tileSide;
        float u0 = atlas.u0(tile);
        float u1 = atlas.u1(tile);
        float v0 = atlas.v0(tile);
        float v1 = atlas.v1(tile);
        for (int face = 0; face < 6; face++) {
            int[] n = NEIGHBOR[face];
            Block neighbor = Block.fromId(world.getBlock(wx + n[0], y + n[1], wz + n[2]));
            if (neighbor == Block.WATER || neighbor.opaque) {
                continue;
            }
            float shade = SHADE[face] * 0.95f;
            int base = vertices.count / 6;
            float[][] corners = CORNERS[face];
            for (int c = 0; c < 4; c++) {
                float cy = corners[c][1];
                if (face != 3 && cy == 1) {
                    cy = WATER_TOP; // obnizona tafla
                }
                vertices.put(
                        wx + corners[c][0], y + cy, wz + corners[c][2],
                        UV[c][0] == 0 ? u0 : u1,
                        UV[c][1] == 0 ? v0 : v1,
                        shade);
            }
            indices.put(base, base + 1, base + 2, base, base + 2, base + 3);
        }
    }

    /** Dwa skrzyzowane quady (kwiaty, trawa, pochodnia). */
    private static void buildCross(TextureAtlas atlas,
                                   FloatBuilder vertices, IntBuilder indices,
                                   int wx, int y, int wz, int tile,
                                   float min, float max, float top) {
        float u0 = atlas.u0(tile);
        float u1 = atlas.u1(tile);
        float v0 = atlas.v0(tile);
        float v1 = atlas.v1(tile);
        float[][] quads = {
                {min, 0, min, max, top, max},
                {max, 0, min, min, top, max}
        };
        for (float[] q : quads) {
            int base = vertices.count / 6;
            vertices.put(wx + q[0], y, wz + q[2], u0, v0, 1.0f);
            vertices.put(wx + q[3], y, wz + q[5], u1, v0, 1.0f);
            vertices.put(wx + q[3], y + top, wz + q[5], u1, v1, 1.0f);
            vertices.put(wx + q[0], y + top, wz + q[2], u0, v1, 1.0f);
            indices.put(base, base + 1, base + 2, base, base + 2, base + 3);
            // Druga strona (zeby bylo widac z obu kierunkow).
            indices.put(base, base + 2, base + 1, base, base + 3, base + 2);
        }
    }

    /** Rosnaca tablica floatow (bez kosztownego boxingu). */
    private static final class FloatBuilder {
        private float[] data = new float[4096];
        private int count = 0;

        void put(float a, float b, float c, float d, float e, float f) {
            if (count + 6 > data.length) {
                data = Arrays.copyOf(data, data.length * 2);
            }
            data[count++] = a;
            data[count++] = b;
            data[count++] = c;
            data[count++] = d;
            data[count++] = e;
            data[count++] = f;
        }

        float[] toArray() {
            return Arrays.copyOf(data, count);
        }
    }

    private static final class IntBuilder {
        private int[] data = new int[1024];
        private int count = 0;

        void put(int... values) {
            if (count + values.length > data.length) {
                data = Arrays.copyOf(data, Math.max(data.length * 2, count + values.length));
            }
            for (int v : values) {
                data[count++] = v;
            }
        }

        int[] toArray() {
            return Arrays.copyOf(data, count);
        }
    }
}
