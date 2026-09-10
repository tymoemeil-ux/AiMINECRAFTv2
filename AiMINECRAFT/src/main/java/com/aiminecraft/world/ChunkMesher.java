package com.aiminecraft.world;

import com.aiminecraft.render.Mesh;
import com.aiminecraft.render.TextureAtlas;

import java.util.Arrays;

/**
 * Buduje siatke trojkatow chunka. Sciany stykajace sie
 * z nieprzezroczystym blokiem sa pomijane (face culling).
 */
public final class ChunkMesher {

    private ChunkMesher() {
    }

    // 6 scian: +X, -X, +Y, -Y, +Z, -Z.
    private static final int[][] NEIGHBOR = {
            {1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}
    };

    // Rogi kazdej sciany (przeciwnie do ruchu wskazowek zegara, patrzac z zewnatrz).
    private static final float[][][] CORNERS = {
            {{1, 0, 1}, {1, 0, 0}, {1, 1, 0}, {1, 1, 1}}, // +X
            {{0, 0, 0}, {0, 0, 1}, {0, 1, 1}, {0, 1, 0}}, // -X
            {{0, 1, 1}, {1, 1, 1}, {1, 1, 0}, {0, 1, 0}}, // +Y (gora)
            {{0, 0, 0}, {1, 0, 0}, {1, 0, 1}, {0, 0, 1}}, // -Y (dol)
            {{0, 0, 1}, {1, 0, 1}, {1, 1, 1}, {0, 1, 1}}, // +Z
            {{1, 0, 0}, {0, 0, 0}, {0, 1, 0}, {1, 1, 0}}  // -Z
    };

    private static final float[][] UV = {{0, 0}, {1, 0}, {1, 1}, {0, 1}};

    // Proste cieniowanie kierunkowe: gora najjasniejsza, dol najciemniejszy.
    private static final float[] SHADE = {0.65f, 0.65f, 1.0f, 0.5f, 0.8f, 0.8f};

    // 0 = tekstura gory, 1 = dolu, 2 = boku.
    private static final int[] TILE_KIND = {2, 2, 0, 1, 2, 2};

    public static Mesh build(World world, Chunk chunk, TextureAtlas atlas) {
        FloatBuilder vertices = new FloatBuilder();
        IntBuilder indices = new IntBuilder();
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
                    for (int face = 0; face < 6; face++) {
                        int[] n = NEIGHBOR[face];
                        Block neighbor = Block.fromId(world.getBlock(wx + n[0], y + n[1], wz + n[2]));
                        // Sciane pomijamy tylko przy pelnym sasiadzie.
                        // Liscie rysuja sciany miedzy soba (tryb "fancy" - widac przez dziury).
                        if (neighbor.opaque && (block != Block.OAK_LEAVES || neighbor != Block.OAK_LEAVES)) {
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
            }
        }
        return new Mesh(vertices.toArray(), indices.toArray());
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
