package com.aiminecraft.world;

import com.aiminecraft.render.Mesh;

/**
 * Chunk 16 x 64 x 16 blokow + jego siatka do rysowania.
 */
public class Chunk {

    public static final int SIZE = 16;
    public static final int HEIGHT = 64;

    private final int cx;
    private final int cz;
    private final byte[] blocks = new byte[SIZE * HEIGHT * SIZE];
    private Mesh mesh;

    public Chunk(int cx, int cz) {
        this.cx = cx;
        this.cz = cz;
    }

    public int getCx() {
        return cx;
    }

    public int getCz() {
        return cz;
    }

    public static int index(int x, int y, int z) {
        return (y << 8) | (z << 4) | x;
    }

    public byte getLocal(int x, int y, int z) {
        if (x < 0 || x >= SIZE || z < 0 || z >= SIZE) {
            return Block.AIR.id;
        }
        if (y < 0) {
            return Block.STONE.id;
        }
        if (y >= HEIGHT) {
            return Block.AIR.id;
        }
        return blocks[index(x, y, z)];
    }

    public void setLocal(int x, int y, int z, byte id) {
        if (x < 0 || x >= SIZE || z < 0 || z >= SIZE || y < 0 || y >= HEIGHT) {
            return;
        }
        blocks[index(x, y, z)] = id;
    }

    public void setMesh(Mesh mesh) {
        if (this.mesh != null) {
            this.mesh.close();
        }
        this.mesh = mesh;
    }

    public void draw() {
        if (mesh != null) {
            mesh.draw();
        }
    }

    /** Zwalnia zasoby OpenGL (wywoluj przy wyladowaniu chunka). */
    public void dispose() {
        if (mesh != null) {
            mesh.close();
            mesh = null;
        }
    }
}
