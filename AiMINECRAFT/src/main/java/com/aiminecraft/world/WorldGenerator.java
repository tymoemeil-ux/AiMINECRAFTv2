package com.aiminecraft.world;

/**
 * Generator swiata v2: biomy, oceany i plaze, gory, snieg,
 * jaskinie, rudy, kwiaty i 3 rodzaje drzew + kaktusy.
 */
public class WorldGenerator {

    public static final int SEA_LEVEL = 27;

    private final PerlinNoise noise;
    private final long seed;

    public WorldGenerator(long seed) {
        this.seed = seed;
        this.noise = new PerlinNoise(seed);
    }

    public long getSeed() {
        return seed;
    }

    private double continent(int x, int z) {
        return noise.fbm(x * 0.008, z * 0.008, 4, 2.02, 0.5);
    }

    private double detail(int x, int z) {
        return noise.fbm(x * 0.045 + 500.0, z * 0.045 - 500.0, 3, 2.0, 0.5);
    }

    private double mountain(int x, int z) {
        double m = noise.fbm(x * 0.012 + 900.0, z * 0.012 + 300.0, 3, 2.1, 0.5);
        m = Math.max(0, m - 0.15) * 2.2;
        return Math.min(1.0, m);
    }

    private double temperature(int x, int z) {
        return noise.fbm(x * 0.006 + 2000.0, z * 0.006 - 700.0, 2, 2.0, 0.5) * 0.5 + 0.5;
    }

    private double moisture(int x, int z) {
        return noise.fbm(x * 0.007 - 1500.0, z * 0.007 + 1100.0, 2, 2.0, 0.5) * 0.5 + 0.5;
    }

    /** Wysokosc terenu (staly grunt, bez wody i sniegu). */
    public int getHeight(int x, int z) {
        double h = 30 + continent(x, z) * 9 + detail(x, z) * 4.5 + mountain(x, z) * 26;
        int hi = (int) Math.round(h);
        if (hi < 6) {
            hi = 6;
        }
        if (hi > Chunk.HEIGHT - 16) {
            hi = Chunk.HEIGHT - 16;
        }
        return hi;
    }

    public Biome getBiome(int x, int z) {
        int h = getHeight(x, z);
        if (h <= SEA_LEVEL) {
            return Biome.OCEAN;
        }
        double m = mountain(x, z);
        double t = temperature(x, z) - Math.max(0, (h - 38)) * 0.02;
        double moist = moisture(x, z);
        if (m > 0.55 || h > 46) {
            return t < 0.35 ? Biome.SNOWY : Biome.MOUNTAINS;
        }
        if (t < 0.32) {
            return Biome.SNOWY;
        }
        if (t > 0.62 && moist < 0.45) {
            return Biome.DESERT;
        }
        if (moist > 0.52) {
            return Biome.FOREST;
        }
        return Biome.PLAINS;
    }

    /** Szuka ladu na spawn (spirala od 0,0). */
    public int[] findSpawn() {
        if (getHeight(0, 0) > SEA_LEVEL) {
            return new int[]{0, 0};
        }
        for (int r = 1; r < 128; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                        continue;
                    }
                    if (getHeight(dx, dz) > SEA_LEVEL + 1) {
                        return new int[]{dx, dz};
                    }
                }
            }
        }
        return new int[]{0, 0};
    }

    public void generate(Chunk chunk) {
        int baseX = chunk.getCx() * Chunk.SIZE;
        int baseZ = chunk.getCz() * Chunk.SIZE;

        for (int x = 0; x < Chunk.SIZE; x++) {
            for (int z = 0; z < Chunk.SIZE; z++) {
                int gx = baseX + x;
                int gz = baseZ + z;
                int h = getHeight(gx, gz);
                Biome biome = getBiome(gx, gz);
                boolean beach = h >= SEA_LEVEL && h <= SEA_LEVEL + 2 && biome != Biome.OCEAN;
                boolean desert = biome == Biome.DESERT || beach;

                for (int y = 0; y <= h; y++) {
                    byte id;
                    if (y == 0) {
                        id = Block.BEDROCK.id;
                    } else if (y == h) {
                        id = surfaceBlock(biome, beach, gx, gz);
                    } else if (y > h - 4) {
                        id = desert ? Block.SAND.id : Block.DIRT.id;
                    } else {
                        id = stoneWithOre(gx, y, gz);
                    }
                    // Jaskinie (nie pod oceanem, nie za gleboko).
                    if (id != Block.BEDROCK.id && y > 3 && y < h - 4 && h > SEA_LEVEL + 1 && isCave(gx, y, gz)) {
                        id = Block.AIR.id;
                    }
                    chunk.setLocal(x, y, z, id);
                }

                // Woda do poziomu morza.
                for (int y = h + 1; y <= SEA_LEVEL; y++) {
                    chunk.setLocal(x, y, z, Block.WATER.id);
                }
                // Lof na zamarznietych jeziorach.
                if (biome == Biome.SNOWY && h < SEA_LEVEL && h >= SEA_LEVEL - 3) {
                    chunk.setLocal(x, SEA_LEVEL, z, Block.ICE.id);
                }
                // Czapa sniegu na ladzie w snieznym biomie.
                if ((biome == Biome.SNOWY || h > 48) && h > SEA_LEVEL && h + 1 < Chunk.HEIGHT) {
                    chunk.setLocal(x, h + 1, z, Block.SNOW.id);
                }
            }
        }

        // Rosliny i drzewa (z marginesem na sasiadow).
        for (int gx = baseX - 2; gx < baseX + Chunk.SIZE + 2; gx++) {
            for (int gz = baseZ - 2; gz < baseZ + Chunk.SIZE + 2; gz++) {
                plant(chunk, gx, gz);
                TreeKind tree = treeAt(gx, gz);
                if (tree != null) {
                    stampTree(chunk, gx, gz, tree);
                }
                if (isCactus(gx, gz)) {
                    stampCactus(chunk, gx, gz);
                }
            }
        }
    }

    private byte surfaceBlock(Biome biome, boolean beach, int x, int z) {
        if (biome == Biome.OCEAN) {
            double r = PerlinNoise.hash01(x * 3 + 1, z * 5 + 2, seed);
            if (r < 0.25) {
                return Block.GRAVEL.id;
            }
            if (r < 0.6) {
                return Block.SAND.id;
            }
            return Block.DIRT.id;
        }
        if (beach || biome == Biome.DESERT) {
            return Block.SAND.id;
        }
        if (biome == Biome.MOUNTAINS && getHeight(x, z) > 42) {
            return Block.STONE.id;
        }
        return Block.GRASS.id;
    }

    private byte stoneWithOre(int x, int y, int z) {
        double r = PerlinNoise.hash3(x, y, z, seed ^ 0x0E5EEDL);
        if (y <= 12 && r < 0.003) {
            return Block.DIAMOND_ORE.id;
        }
        if (y <= 14 && r < 0.009) {
            return Block.REDSTONE_ORE.id;
        }
        if (y <= 16 && r < 0.013) {
            return Block.GOLD_ORE.id;
        }
        if (y <= 32 && r < 0.025) {
            return Block.IRON_ORE.id;
        }
        if (y <= 50 && r < 0.045) {
            return Block.COAL_ORE.id;
        }
        return Block.STONE.id;
    }

    private boolean isCave(int x, int y, int z) {
        double c = noise.fbm3(x * 0.055, y * 0.08, z * 0.055, 2, 2.0, 0.5);
        return c > 0.42;
    }

    private enum TreeKind {
        OAK, BIRCH, SPRUCE
    }

    private TreeKind treeAt(int x, int z) {
        if (x * x + z * z < 25) {
            return null;
        }
        int h = getHeight(x, z);
        if (h <= SEA_LEVEL || h > Chunk.HEIGHT - 18) {
            return null;
        }
        Biome biome = getBiome(x, z);
        double r = PerlinNoise.hash01(x, z, seed ^ 0x51AB34D7L);
        double pick = PerlinNoise.hash01(x * 7 - 3, z * 5 + 9, seed ^ 0x7BEE5L);
        switch (biome) {
            case FOREST:
                if (r > 0.035) {
                    return null;
                }
                return pick < 0.6 ? TreeKind.OAK : TreeKind.BIRCH;
            case PLAINS:
                if (r > 0.010) {
                    return null;
                }
                return TreeKind.OAK;
            case SNOWY:
                if (r > 0.016) {
                    return null;
                }
                return TreeKind.SPRUCE;
            case MOUNTAINS:
                if (r > 0.005) {
                    return null;
                }
                return TreeKind.SPRUCE;
            default:
                return null;
        }
    }

    private int treeHeight(int x, int z) {
        return 4 + (int) (PerlinNoise.hash01(x, z, seed ^ 0x77AA55L) * 3.0);
    }

    private void stampTree(Chunk chunk, int gx, int gz, TreeKind kind) {
        int h = getHeight(gx, gz);
        int trunkHeight = treeHeight(gx, gz);
        int baseY = h + 1;
        byte log = Block.OAK_LOG.id;
        byte leaves = Block.OAK_LEAVES.id;
        if (kind == TreeKind.BIRCH) {
            log = Block.BIRCH_LOG.id;
            leaves = Block.BIRCH_LEAVES.id;
        } else if (kind == TreeKind.SPRUCE) {
            log = Block.SPRUCE_LOG.id;
            leaves = Block.SPRUCE_LEAVES.id;
        }

        if (kind == TreeKind.SPRUCE) {
            // Swierk: waskie pietra lisciane.
            for (int dy = 1; dy <= trunkHeight + 1; dy++) {
                int r = dy <= 1 ? 2 : (dy >= trunkHeight ? 1 : (dy % 2 == 0 ? 2 : 1));
                if (dy == trunkHeight + 1) {
                    r = 0;
                }
                for (int dx = -r; dx <= r; dx++) {
                    for (int dz = -r; dz <= r; dz++) {
                        if (dx == 0 && dz == 0 && dy <= trunkHeight) {
                            continue;
                        }
                        if (Math.abs(dx) == 2 && Math.abs(dz) == 2) {
                            continue;
                        }
                        setIfAir(chunk, gx + dx, baseY + dy, gz + dz, leaves);
                    }
                }
            }
        } else {
            // Dab / brzoza: okragla korona.
            for (int dy = trunkHeight - 2; dy <= trunkHeight + 1; dy++) {
                int r = dy >= trunkHeight ? 1 : 2;
                for (int dx = -r; dx <= r; dx++) {
                    for (int dz = -r; dz <= r; dz++) {
                        if (dx == 0 && dz == 0 && dy <= trunkHeight) {
                            continue;
                        }
                        if (r == 2 && Math.abs(dx) == 2 && Math.abs(dz) == 2
                                && PerlinNoise.hash01(gx + dx * 7 + dy * 13, gz + dz * 11 - dy * 7, seed) < 0.6) {
                            continue;
                        }
                        setIfAir(chunk, gx + dx, baseY + dy, gz + dz, leaves);
                    }
                }
            }
        }
        for (int dy = 0; dy < trunkHeight; dy++) {
            setBlock(chunk, gx, baseY + dy, gz, log);
        }
    }

    private boolean isCactus(int x, int z) {
        if (getBiome(x, z) != Biome.DESERT) {
            return false;
        }
        int h = getHeight(x, z);
        if (h <= SEA_LEVEL) {
            return false;
        }
        return PerlinNoise.hash01(x, z, seed ^ 0xCAC705L) < 0.008;
    }

    private void stampCactus(Chunk chunk, int gx, int gz) {
        int h = getHeight(gx, gz);
        int tall = 2 + (int) (PerlinNoise.hash01(gx * 3, gz * 7, seed ^ 0xCAC06L) * 2.0);
        for (int dy = 1; dy <= tall; dy++) {
            setIfAir(chunk, gx, h + dy, gz, Block.CACTUS.id);
        }
    }

    private void plant(Chunk chunk, int gx, int gz) {
        int h = getHeight(gx, gz);
        if (h <= SEA_LEVEL) {
            return;
        }
        Biome biome = getBiome(gx, gz);
        if (biome != Biome.PLAINS && biome != Biome.FOREST) {
            return;
        }
        if (h + 1 >= Chunk.HEIGHT) {
            return;
        }
        double r = PerlinNoise.hash01(gx, gz, seed ^ 0xF10E5L);
        byte flower = 0;
        if (r < 0.012) {
            flower = Block.DANDELION.id;
        } else if (r < 0.024) {
            flower = Block.POPPY.id;
        } else if (r < 0.09) {
            flower = Block.SHORT_GRASS.id;
        }
        if (flower != 0) {
            // Tylko na trawie i tylko w powietrze.
            int lx = gx - chunk.getCx() * Chunk.SIZE;
            int lz = gz - chunk.getCz() * Chunk.SIZE;
            if (lx < 0 || lx >= Chunk.SIZE || lz < 0 || lz >= Chunk.SIZE) {
                return;
            }
            if (chunk.getLocal(lx, h, lz) == Block.GRASS.id
                    && chunk.getLocal(lx, h + 1, lz) == Block.AIR.id) {
                chunk.setLocal(lx, h + 1, lz, flower);
            }
        }
    }

    private void setIfAir(Chunk chunk, int wx, int y, int wz, byte id) {
        int lx = wx - chunk.getCx() * Chunk.SIZE;
        int lz = wz - chunk.getCz() * Chunk.SIZE;
        if (lx < 0 || lx >= Chunk.SIZE || lz < 0 || lz >= Chunk.SIZE) {
            return;
        }
        if (y < 1 || y >= Chunk.HEIGHT) {
            return;
        }
        if (chunk.getLocal(lx, y, lz) == Block.AIR.id) {
            chunk.setLocal(lx, y, lz, id);
        }
    }

    private void setBlock(Chunk chunk, int wx, int y, int wz, byte id) {
        int lx = wx - chunk.getCx() * Chunk.SIZE;
        int lz = wz - chunk.getCz() * Chunk.SIZE;
        if (lx < 0 || lx >= Chunk.SIZE || lz < 0 || lz >= Chunk.SIZE) {
            return;
        }
        if (y < 1 || y >= Chunk.HEIGHT) {
            return;
        }
        chunk.setLocal(lx, y, lz, id);
    }
}
