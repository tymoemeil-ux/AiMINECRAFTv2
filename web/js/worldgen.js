/* AiMINECRAFT Web - generator swiata: biomy, oceany, jaskinie, rudy, drzewa. Bez DOM. */
"use strict";

var AIMC = (typeof window !== "undefined")
    ? (window.AIMC || (window.AIMC = {}))
    : (typeof module !== "undefined" && module.exports
        ? require("./noise.js")
        : {});
if (typeof module !== "undefined" && module.exports) module.exports = AIMC;

AIMC.CHUNK = 16;
AIMC.HEIGHT = 64;
AIMC.SEA = 27;

// Id blokow (jak w config.js).
var AIR = 0, GRASS = 1, DIRT = 2, STONE = 3, OAK_LOG = 4, OAK_LEAVES = 5,
    BEDROCK = 6, WATER = 7, SAND = 8, COAL_ORE = 12, IRON_ORE = 13,
    GOLD_ORE = 14, DIAMOND_ORE = 15, REDSTONE_ORE = 16, BIRCH_LOG = 17,
    BIRCH_LEAVES = 18, SPRUCE_LOG = 19, SPRUCE_LEAVES = 20, SNOW = 21,
    DANDELION = 22, POPPY = 23, SHORT_GRASS = 24, CACTUS = 27,
    GRAVEL = 28, ICE = 30;

// Biomy.
AIMC.BIOME_OCEAN = 0;
AIMC.BIOME_PLAINS = 1;
AIMC.BIOME_FOREST = 2;
AIMC.BIOME_DESERT = 3;
AIMC.BIOME_MOUNTAINS = 4;
AIMC.BIOME_SNOWY = 5;
AIMC.BIOME_NAMES = ["Ocean", "Rowniny", "Las", "Pustynia", "Gory", "Sniezny"];

AIMC.WorldGen = function (seed) {
    this.seed = seed >>> 0;
    this.noise = new AIMC.Perlin(this.seed);
};

AIMC.WorldGen.prototype.continent = function (x, z) {
    return this.noise.fbm2(x * 0.008, z * 0.008, 4, 2.02, 0.5);
};

AIMC.WorldGen.prototype.detail = function (x, z) {
    return this.noise.fbm2(x * 0.045 + 500.0, z * 0.045 - 500.0, 3, 2.0, 0.5);
};

AIMC.WorldGen.prototype.mountain = function (x, z) {
    var m = this.noise.fbm2(x * 0.012 + 900.0, z * 0.012 + 300.0, 3, 2.1, 0.5);
    m = Math.max(0, m - 0.15) * 2.2;
    return Math.min(1.0, m);
};

AIMC.WorldGen.prototype.temperature = function (x, z) {
    return this.noise.fbm2(x * 0.006 + 2000.0, z * 0.006 - 700.0, 2, 2.0, 0.5) * 0.5 + 0.5;
};

AIMC.WorldGen.prototype.moisture = function (x, z) {
    return this.noise.fbm2(x * 0.007 - 1500.0, z * 0.007 + 1100.0, 2, 2.0, 0.5) * 0.5 + 0.5;
};

AIMC.WorldGen.prototype.getHeight = function (x, z) {
    var h = 30 + this.continent(x, z) * 9 + this.detail(x, z) * 4.5 + this.mountain(x, z) * 26;
    var hi = Math.round(h);
    if (hi < 6) hi = 6;
    if (hi > AIMC.HEIGHT - 16) hi = AIMC.HEIGHT - 16;
    return hi;
};

AIMC.WorldGen.prototype.getBiome = function (x, z) {
    var h = this.getHeight(x, z);
    if (h <= AIMC.SEA) return AIMC.BIOME_OCEAN;
    var m = this.mountain(x, z);
    var t = this.temperature(x, z) - Math.max(0, h - 38) * 0.02;
    var moist = this.moisture(x, z);
    if (m > 0.55 || h > 46) {
        return t < 0.35 ? AIMC.BIOME_SNOWY : AIMC.BIOME_MOUNTAINS;
    }
    if (t < 0.32) return AIMC.BIOME_SNOWY;
    if (t > 0.62 && moist < 0.45) return AIMC.BIOME_DESERT;
    if (moist > 0.52) return AIMC.BIOME_FOREST;
    return AIMC.BIOME_PLAINS;
};

AIMC.WorldGen.prototype.findSpawn = function () {
    if (this.getHeight(0, 0) > AIMC.SEA) return [0, 0];
    for (var r = 1; r < 128; r++) {
        for (var dx = -r; dx <= r; dx++) {
            for (var dz = -r; dz <= r; dz++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) !== r) continue;
                if (this.getHeight(dx, dz) > AIMC.SEA + 1) return [dx, dz];
            }
        }
    }
    return [0, 0];
};

AIMC.WorldGen.prototype.surfaceBlock = function (biome, beach, x, z) {
    if (biome === AIMC.BIOME_OCEAN) {
        var r = AIMC.hash01(x * 3 + 1, z * 5 + 2, this.seed);
        if (r < 0.25) return GRAVEL;
        if (r < 0.6) return SAND;
        return DIRT;
    }
    if (beach || biome === AIMC.BIOME_DESERT) return SAND;
    if (biome === AIMC.BIOME_MOUNTAINS && this.getHeight(x, z) > 42) return STONE;
    return GRASS;
};

AIMC.WorldGen.prototype.stoneWithOre = function (x, y, z) {
    var r = AIMC.hash3(x, y, z, (this.seed ^ 0x0E5EED) >>> 0);
    if (y <= 12 && r < 0.003) return DIAMOND_ORE;
    if (y <= 14 && r < 0.009) return REDSTONE_ORE;
    if (y <= 16 && r < 0.013) return GOLD_ORE;
    if (y <= 32 && r < 0.025) return IRON_ORE;
    if (y <= 50 && r < 0.045) return COAL_ORE;
    return STONE;
};

AIMC.WorldGen.prototype.isCave = function (x, y, z) {
    return this.noise.fbm3(x * 0.055, y * 0.08, z * 0.055, 2, 2.0, 0.5) > 0.42;
};

/** Rodzaj drzewa w kolumnie: 0=brak, 1=dab, 2=brzoza, 3=swierk. */
AIMC.WorldGen.prototype.treeAt = function (x, z) {
    if (x * x + z * z < 25) return 0;
    var h = this.getHeight(x, z);
    if (h <= AIMC.SEA || h > AIMC.HEIGHT - 18) return 0;
    var biome = this.getBiome(x, z);
    var r = AIMC.hash01(x, z, (this.seed ^ 0x51AB34D7) >>> 0);
    var pick = AIMC.hash01(x * 7 - 3, z * 5 + 9, (this.seed ^ 0x7BEE5) >>> 0);
    if (biome === AIMC.BIOME_FOREST) {
        if (r > 0.035) return 0;
        return pick < 0.6 ? 1 : 2;
    }
    if (biome === AIMC.BIOME_PLAINS) {
        if (r > 0.010) return 0;
        return 1;
    }
    if (biome === AIMC.BIOME_SNOWY) {
        if (r > 0.016) return 0;
        return 3;
    }
    if (biome === AIMC.BIOME_MOUNTAINS) {
        if (r > 0.005) return 0;
        return 3;
    }
    return 0;
};

AIMC.WorldGen.prototype.treeHeight = function (x, z) {
    return 4 + ((AIMC.hash01(x, z, (this.seed ^ 0x77AA55) >>> 0) * 3.0) | 0);
};

AIMC.WorldGen.prototype.isCactus = function (x, z) {
    if (this.getBiome(x, z) !== AIMC.BIOME_DESERT) return false;
    if (this.getHeight(x, z) <= AIMC.SEA) return false;
    return AIMC.hash01(x, z, (this.seed ^ 0xCAC705) >>> 0) < 0.008;
};

/**
 * Generuje chunk do Uint8Array(16*64*16), indeks (y<<8)|(z<<4)|x.
 * setLocal/getLocal operuja na tej tablicy.
 */
AIMC.WorldGen.prototype.generate = function (cx, cz) {
    var S = AIMC.CHUNK, H = AIMC.HEIGHT;
    var data = new Uint8Array(S * H * S);
    var baseX = cx * S, baseZ = cz * S;
    var x, y, z, gx, gz, h, biome, beach, desert, id;

    function idx(lx, ly, lz) { return (ly << 8) | (lz << 4) | lx; }
    function setLocal(chunk, wx, wy, wz, v) {
        var lx = wx - baseX, lz = wz - baseZ;
        if (lx < 0 || lx >= S || lz < 0 || lz >= S || wy < 1 || wy >= H) return;
        chunk[idx(lx, wy, lz)] = v;
    }
    function setIfAir(chunk, wx, wy, wz, v) {
        var lx = wx - baseX, lz = wz - baseZ;
        if (lx < 0 || lx >= S || lz < 0 || lz >= S || wy < 1 || wy >= H) return;
        if (chunk[idx(lx, wy, lz)] === AIR) chunk[idx(lx, wy, lz)] = v;
    }

    for (x = 0; x < S; x++) {
        for (z = 0; z < S; z++) {
            gx = baseX + x; gz = baseZ + z;
            h = this.getHeight(gx, gz);
            biome = this.getBiome(gx, gz);
            beach = h >= AIMC.SEA && h <= AIMC.SEA + 2 && biome !== AIMC.BIOME_OCEAN;
            desert = biome === AIMC.BIOME_DESERT || beach;
            for (y = 0; y <= h; y++) {
                if (y === 0) id = BEDROCK;
                else if (y === h) id = this.surfaceBlock(biome, beach, gx, gz);
                else if (y > h - 4) id = desert ? SAND : DIRT;
                else id = this.stoneWithOre(gx, y, gz);
                if (id !== BEDROCK && y > 3 && y < h - 4 && h > AIMC.SEA + 1 && this.isCave(gx, y, gz)) {
                    id = AIR;
                }
                data[idx(x, y, z)] = id;
            }
            for (y = h + 1; y <= AIMC.SEA; y++) {
                data[idx(x, y, z)] = WATER;
            }
            if (biome === AIMC.BIOME_SNOWY && h < AIMC.SEA && h >= AIMC.SEA - 3) {
                data[idx(x, AIMC.SEA, z)] = ICE;
            }
            if ((biome === AIMC.BIOME_SNOWY || h > 48) && h > AIMC.SEA && h + 1 < H) {
                data[idx(x, h + 1, z)] = SNOW;
            }
        }
    }

    var self = this;
    for (gx = baseX - 2; gx < baseX + S + 2; gx++) {
        for (gz = baseZ - 2; gz < baseZ + S + 2; gz++) {
            self.plantInto(data, idx, baseX, baseZ, gx, gz);
            var tree = self.treeAt(gx, gz);
            if (tree !== 0) self.stampTree(data, setIfAir, setLocal, gx, gz, tree);
            if (self.isCactus(gx, gz)) self.stampCactus(data, setIfAir, gx, gz);
        }
    }
    return data;
};

AIMC.WorldGen.prototype.stampTree = function (data, setIfAir, setLocal, gx, gz, kind) {
    var h = this.getHeight(gx, gz);
    var trunkH = this.treeHeight(gx, gz);
    var baseY = h + 1;
    var log = OAK_LOG, leaves = OAK_LEAVES;
    if (kind === 2) { log = BIRCH_LOG; leaves = BIRCH_LEAVES; }
    else if (kind === 3) { log = SPRUCE_LOG; leaves = SPRUCE_LEAVES; }
    var dx, dy, dz, r;
    if (kind === 3) {
        for (dy = 1; dy <= trunkH + 1; dy++) {
            r = dy <= 1 ? 2 : (dy >= trunkH ? 1 : (dy % 2 === 0 ? 2 : 1));
            if (dy === trunkH + 1) r = 0;
            for (dx = -r; dx <= r; dx++) {
                for (dz = -r; dz <= r; dz++) {
                    if (dx === 0 && dz === 0 && dy <= trunkH) continue;
                    if (Math.abs(dx) === 2 && Math.abs(dz) === 2) continue;
                    setIfAir(data, gx + dx, baseY + dy, gz + dz, leaves);
                }
            }
        }
    } else {
        for (dy = trunkH - 2; dy <= trunkH + 1; dy++) {
            r = dy >= trunkH ? 1 : 2;
            for (dx = -r; dx <= r; dx++) {
                for (dz = -r; dz <= r; dz++) {
                    if (dx === 0 && dz === 0 && dy <= trunkH) continue;
                    if (r === 2 && Math.abs(dx) === 2 && Math.abs(dz) === 2 &&
                            AIMC.hash01(gx + dx * 7 + dy * 13, gz + dz * 11 - dy * 7, this.seed) < 0.6) {
                        continue;
                    }
                    setIfAir(data, gx + dx, baseY + dy, gz + dz, leaves);
                }
            }
        }
    }
    for (dy = 0; dy < trunkH; dy++) {
        setLocal(data, gx, baseY + dy, gz, log);
    }
};

AIMC.WorldGen.prototype.stampCactus = function (data, setIfAir, gx, gz) {
    var h = this.getHeight(gx, gz);
    var tall = 2 + ((AIMC.hash01(gx * 3, gz * 7, (this.seed ^ 0xCAC06) >>> 0) * 2.0) | 0);
    for (var dy = 1; dy <= tall; dy++) {
        setIfAir(data, gx, h + dy, gz, CACTUS);
    }
};

AIMC.WorldGen.prototype.plantInto = function (data, idx, baseX, baseZ, gx, gz) {
    var S = AIMC.CHUNK, H = AIMC.HEIGHT;
    var h = this.getHeight(gx, gz);
    if (h <= AIMC.SEA) return;
    var biome = this.getBiome(gx, gz);
    if (biome !== AIMC.BIOME_PLAINS && biome !== AIMC.BIOME_FOREST) return;
    if (h + 1 >= H) return;
    var r = AIMC.hash01(gx, gz, (this.seed ^ 0xF10E5) >>> 0);
    var flower = 0;
    if (r < 0.012) flower = DANDELION;
    else if (r < 0.024) flower = POPPY;
    else if (r < 0.09) flower = SHORT_GRASS;
    if (flower === 0) return;
    var lx = gx - baseX, lz = gz - baseZ;
    if (lx < 0 || lx >= S || lz < 0 || lz >= S) return;
    if (data[idx(lx, h, lz)] === GRASS && data[idx(lx, h + 1, lz)] === AIR) {
        data[idx(lx, h + 1, lz)] = flower;
    }
};
