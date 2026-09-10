package com.aiminecraft.world;

/**
 * Prosty generator swiata: pagorki z szumu Perlina + drzewa (dab).
 * Bez wody, bez jaskin - na razie :)
 */
public class WorldGenerator {

    private static final double TREE_CHANCE = 0.012;
    private static final int SPAWN_CLEAR_RADIUS_SQ = 25;

    private final PerlinNoise noise;
    private final long seed;

    public WorldGenerator(long seed) {
        this.seed = seed;
        this.noise = new PerlinNoise(seed);
    }

    public long getSeed() {
        return seed;
    }

    /** Wysokosc terenu (y najwyzszego stalego bloku) dla kolumny (x, z). */
    public int getHeight(int x, int z) {
        double continent = noise.fbm(x * 0.008, z * 0.008, 4, 2.02, 0.5);
        double hills = noise.fbm(x * 0.045 + 500.0, z * 0.045 - 500.0, 3, 2.0, 0.5);
        int h = (int) Math.round(30 + continent * 9 + hills * 4.5);
        if (h < 3) {
            h = 3;
        }
        if (h > Chunk.HEIGHT - 14) {
            h = Chunk.HEIGHT - 14;
        }
        return h;
    }

    /** Wypelnia chunk blokami: teren + drzewa. */
    public void generate(Chunk chunk) {
        int baseX = chunk.getCx() * Chunk.SIZE;
        int baseZ = chunk.getCz() * Chunk.SIZE;

        for (int x = 0; x < Chunk.SIZE; x++) {
            for (int z = 0; z < Chunk.SIZE; z++) {
                int h = getHeight(baseX + x, baseZ + z);
                for (int y = 0; y <= h; y++) {
                    byte id;
                    if (y == 0) {
                        id = Block.BEDROCK.id;
                    } else if (y < h - 2) {
                        id = Block.STONE.id;
                    } else if (y < h) {
                        id = Block.DIRT.id;
                    } else {
                        id = Block.GRASS.id;
                    }
                    chunk.setLocal(x, y, z, id);
                }
            }
        }

        // Drzewa: sprawdzamy tez kolumny 2 bloki poza chunkiem,
        // bo korona drzewa zachodzi na sasiednie chunki.
        for (int gx = baseX - 2; gx < baseX + Chunk.SIZE + 2; gx++) {
            for (int gz = baseZ - 2; gz < baseZ + Chunk.SIZE + 2; gz++) {
                if (isTree(gx, gz)) {
                    stampTree(chunk, gx, gz);
                }
            }
        }
    }

    private boolean isTree(int x, int z) {
        if (x * x + z * z < SPAWN_CLEAR_RADIUS_SQ) {
            return false; // plac przy spawnie bez drzew
        }
        int h = getHeight(x, z);
        if (h < 6 || h > Chunk.HEIGHT - 16) {
            return false;
        }
        return PerlinNoise.hash01(x, z, seed ^ 0x51AB34D7L) < TREE_CHANCE;
    }

    private int treeHeight(int x, int z) {
        return 4 + (int) (PerlinNoise.hash01(x, z, seed ^ 0x77AA55L) * 3.0);
    }

    private void stampTree(Chunk chunk, int gx, int gz) {
        int h = getHeight(gx, gz);
        int trunkHeight = treeHeight(gx, gz);
        int baseY = h + 1;

        // Korona: 2 dolne warstwy w promieniu 2, 2 gorne w promieniu 1.
        for (int dy = trunkHeight - 2; dy <= trunkHeight + 1; dy++) {
            int r = dy >= trunkHeight ? 1 : 2;
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (dx == 0 && dz == 0 && dy <= trunkHeight) {
                        continue; // tu bedzie pien
                    }
                    // Zaokraglenie naroznikow korony.
                    if (r == 2 && Math.abs(dx) == 2 && Math.abs(dz) == 2
                            && PerlinNoise.hash01(gx + dx * 7 + dy * 13, gz + dz * 11 - dy * 7, seed) < 0.6) {
                        continue;
                    }
                    setLeaf(chunk, gx + dx, baseY + dy, gz + dz);
                }
            }
        }
        // Pien (nadpisuje liscie na swojej drodze).
        for (int dy = 0; dy < trunkHeight; dy++) {
            setLog(chunk, gx, baseY + dy, gz);
        }
    }

    private void setLeaf(Chunk chunk, int wx, int y, int wz) {
        int lx = wx - chunk.getCx() * Chunk.SIZE;
        int lz = wz - chunk.getCz() * Chunk.SIZE;
        if (lx < 0 || lx >= Chunk.SIZE || lz < 0 || lz >= Chunk.SIZE) {
            return;
        }
        if (y < 1 || y >= Chunk.HEIGHT) {
            return;
        }
        if (chunk.getLocal(lx, y, lz) == Block.AIR.id) {
            chunk.setLocal(lx, y, lz, Block.OAK_LEAVES.id);
        }
    }

    private void setLog(Chunk chunk, int wx, int y, int wz) {
        int lx = wx - chunk.getCx() * Chunk.SIZE;
        int lz = wz - chunk.getCz() * Chunk.SIZE;
        if (lx < 0 || lx >= Chunk.SIZE || lz < 0 || lz >= Chunk.SIZE) {
            return;
        }
        if (y < 1 || y >= Chunk.HEIGHT) {
            return;
        }
        chunk.setLocal(lx, y, lz, Block.OAK_LOG.id);
    }
}
