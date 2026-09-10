package com.aiminecraft.world;

import com.aiminecraft.render.Mesh;
import com.aiminecraft.render.TextureAtlas;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Nieskonczony swiat z chunkow: generowanie danych, strumieniowanie
 * wokol gracza, przebudowa siatek i rysowanie.
 */
public class World {

    private final WorldGenerator generator;
    private final TextureAtlas atlas;
    private final int renderDistance;

    private final Map<Long, Chunk> chunks = new HashMap<>();
    private final Deque<Chunk> dirtyQueue = new ArrayDeque<>();
    private final Set<Long> queuedKeys = new HashSet<>();

    private int centerCx = Integer.MIN_VALUE;
    private int centerCz = Integer.MIN_VALUE;

    public World(long seed, TextureAtlas atlas, int renderDistance) {
        this.generator = new WorldGenerator(seed);
        this.atlas = atlas;
        this.renderDistance = renderDistance;
    }

    public WorldGenerator getGenerator() {
        return generator;
    }

    public static long key(int cx, int cz) {
        return (((long) cx) << 32) | (cz & 0xFFFFFFFFL);
    }

    public Chunk getChunk(int cx, int cz) {
        return chunks.get(key(cx, cz));
    }

    public byte getBlock(int x, int y, int z) {
        if (y < 0) {
            return Block.STONE.id;
        }
        if (y >= Chunk.HEIGHT) {
            return Block.AIR.id;
        }
        Chunk chunk = getChunk(Math.floorDiv(x, Chunk.SIZE), Math.floorDiv(z, Chunk.SIZE));
        if (chunk == null) {
            return Block.AIR.id;
        }
        return chunk.getLocal(Math.floorMod(x, Chunk.SIZE), y, Math.floorMod(z, Chunk.SIZE));
    }

    public boolean isOpaque(int x, int y, int z) {
        return Block.fromId(getBlock(x, y, z)).opaque;
    }

    public boolean isSolid(int x, int y, int z) {
        return Block.fromId(getBlock(x, y, z)).solid;
    }

    /** Stawia blok i oznacza chunki do przebudowy (rowniez sasiadow na granicy). */
    public void setBlock(int x, int y, int z, Block block) {
        if (y < 0 || y >= Chunk.HEIGHT) {
            return;
        }
        int cx = Math.floorDiv(x, Chunk.SIZE);
        int cz = Math.floorDiv(z, Chunk.SIZE);
        Chunk chunk = getChunk(cx, cz);
        if (chunk == null) {
            return;
        }
        int lx = Math.floorMod(x, Chunk.SIZE);
        int lz = Math.floorMod(z, Chunk.SIZE);
        chunk.setLocal(lx, y, lz, block.id);
        markDirty(cx, cz);
        if (lx == 0) {
            markDirty(cx - 1, cz);
        }
        if (lx == Chunk.SIZE - 1) {
            markDirty(cx + 1, cz);
        }
        if (lz == 0) {
            markDirty(cx, cz - 1);
        }
        if (lz == Chunk.SIZE - 1) {
            markDirty(cx, cz + 1);
        }
    }

    private void markDirty(int cx, int cz) {
        if (centerCx != Integer.MIN_VALUE
                && Math.max(Math.abs(cx - centerCx), Math.abs(cz - centerCz)) > renderDistance) {
            return;
        }
        Chunk chunk = getChunk(cx, cz);
        if (chunk == null) {
            return;
        }
        long k = key(cx, cz);
        if (queuedKeys.add(k)) {
            dirtyQueue.add(chunk);
        }
    }

    /** Laduje dane chunkow wokol gracza i wyrzuca dalekie. */
    public void updateStreaming(int playerCx, int playerCz) {
        if (playerCx == centerCx && playerCz == centerCz) {
            return;
        }
        centerCx = playerCx;
        centerCz = playerCz;

        int dataRadius = renderDistance + 1;
        int unloadRadius = renderDistance + 2;

        // Wyladuj dalekie (razem z zasobami OpenGL).
        chunks.entrySet().removeIf(entry -> {
            Chunk chunk = entry.getValue();
            int dx = Math.abs(chunk.getCx() - playerCx);
            int dz = Math.abs(chunk.getCz() - playerCz);
            if (Math.max(dx, dz) > unloadRadius) {
                queuedKeys.remove(key(chunk.getCx(), chunk.getCz()));
                chunk.dispose();
                return true;
            }
            return false;
        });

        // Dogeneruj brakujace dane, od najblizszych chunkow.
        for (int r = 0; r <= dataRadius; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                        continue;
                    }
                    ensureData(playerCx + dx, playerCz + dz);
                }
            }
        }
    }

    private void ensureData(int cx, int cz) {
        if (getChunk(cx, cz) != null) {
            return;
        }
        Chunk chunk = new Chunk(cx, cz);
        generator.generate(chunk);
        chunks.put(key(cx, cz), chunk);
        markDirty(cx, cz);
        markDirty(cx + 1, cz);
        markDirty(cx - 1, cz);
        markDirty(cx, cz + 1);
        markDirty(cx, cz - 1);
    }

    /** Przebudowuje siatki chunkow (z limitem na klatke, zeby nie bylo sciec). */
    public void updateMeshes(int budget) {
        int processed = 0;
        int attempts = dirtyQueue.size() * 2 + 16;
        while (processed < budget && !dirtyQueue.isEmpty() && attempts-- > 0) {
            Chunk chunk = dirtyQueue.poll();
            long k = key(chunk.getCx(), chunk.getCz());
            queuedKeys.remove(k);
            if (chunks.get(k) != chunk) {
                continue; // wyladowany w miedzyczasie
            }
            if (!hasDataAround(chunk.getCx(), chunk.getCz())) {
                if (queuedKeys.add(k)) {
                    dirtyQueue.add(chunk);
                }
                continue;
            }
            Mesh mesh = ChunkMesher.build(this, chunk, atlas);
            chunk.setMesh(mesh);
            processed++;
        }
    }

    private boolean hasDataAround(int cx, int cz) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (getChunk(cx + dx, cz + dz) == null) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Rysuje wszystkie chunki (shader musi byc juz zbindowany). */
    public void render() {
        atlas.bind();
        for (Chunk chunk : chunks.values()) {
            chunk.draw();
        }
    }

    public void close() {
        for (Chunk chunk : chunks.values()) {
            chunk.dispose();
        }
        chunks.clear();
        dirtyQueue.clear();
        queuedKeys.clear();
    }
}
