package com.aiminecraft.world;

import com.aiminecraft.render.Mesh;

/**
 * Chunk 16 x 64 x 16 blokow + dwie siatki:
 * nieprzezroczysta i przezroczysta (woda).
 */
public class Chunk {

    public static final int SIZE = 16;
    public static final int HEIGHT = 64;

    private final int cx;
    private final int cz;
    private final byte[] blocks = new byte[SIZE * HEIGHT * SIZE];
    private Mesh meshOpaque;
    private Mesh meshTranslucent;

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

    /** Kopia surowych danych (do zapisu). */
    public byte[] copyBlocks() {
        return blocks.clone();
    }

    /** Wczytuje surowe dane (z zapisu). */
    public void pasteBlocks(byte[] data) {
        if (data.length != blocks.length) {
            return;
        }
        System.arraycopy(data, 0, blocks, 0, blocks.length);
    }

    public void setMeshes(Mesh opaque, Mesh translucent) {
        if (this.meshOpaque != null) {
            this.meshOpaque.close();
        }
        if (this.meshTranslucent != null) {
            this.meshTranslucent.close();
        }
        this.meshOpaque = opaque;
        this.meshTranslucent = translucent;
    }

    public void drawOpaque() {
        if (meshOpaque != null) {
            meshOpaque.draw();
        }
    }

    public void drawTranslucent() {
        if (meshTranslucent != null) {
            meshTranslucent.draw();
        }
    }

    /** Liczba indeksow w obu siatkach (do diagnostyki). */
    public int getIndexCount() {
        int total = 0;
        if (meshOpaque != null) {
            total += meshOpaque.getIndexCount();
        }
        if (meshTranslucent != null) {
            total += meshTranslucent.getIndexCount();
        }
        return total;
    }

    /** Zwalnia zasoby OpenGL (wywoluj przy wyladowaniu chunka). */
    public void dispose() {
        if (meshOpaque != null) {
            meshOpaque.close();
            meshOpaque = null;
        }
        if (meshTranslucent != null) {
            meshTranslucent.close();
            meshTranslucent = null;
        }
    }
}
