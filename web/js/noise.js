/* AiMINECRAFT Web - seedowany RNG + szum Perlina 2D/3D + hashe. Bez DOM. */
"use strict";

var AIMC = (typeof window !== "undefined")
    ? (window.AIMC || (window.AIMC = {}))
    : (typeof module !== "undefined" && module.exports
        ? require("./config.js")
        : {});
if (typeof module !== "undefined" && module.exports) module.exports = AIMC;

/** String/liczba -> uint32 seed. */
AIMC.seedFrom = function (value) {
    if (typeof value === "number" && isFinite(value)) {
        return value >>> 0;
    }
    var s = String(value);
    if (/^-?\d+$/.test(s)) {
        return (parseInt(s, 10) >>> 0) || 1;
    }
    var h1 = 0xdeadbeef, h2 = 0x41c6ce57, i;
    for (i = 0; i < s.length; i++) {
        var ch = s.charCodeAt(i);
        h1 = Math.imul(h1 ^ ch, 2654435761);
        h2 = Math.imul(h2 ^ ch, 1597334677);
    }
    h1 = Math.imul(h1 ^ (h1 >>> 16), 2246822507) ^ Math.imul(h2 ^ (h2 >>> 13), 3266489909);
    h2 = Math.imul(h2 ^ (h2 >>> 16), 2246822507) ^ Math.imul(h1 ^ (h1 >>> 13), 3266489909);
    return (h2 >>> 0) || 1;
};

/** Generator mulberry32. */
AIMC.makeRandom = function (seed) {
    var a = (seed >>> 0) || 1;
    return function () {
        a |= 0;
        a = (a + 0x6D2B79F5) | 0;
        var t = Math.imul(a ^ (a >>> 15), 1 | a);
        t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
        return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
    };
};

function fade(t) {
    return t * t * t * (t * (t * 6 - 15) + 10);
}

function lerp(a, b, t) {
    return a + t * (b - a);
}

function grad2(hash, x, y) {
    switch (hash & 7) {
        case 0: return x + y;
        case 1: return x - y;
        case 2: return -x + y;
        case 3: return -x - y;
        case 4: return x;
        case 5: return -x;
        case 6: return y;
        default: return -y;
    }
}

function grad3(hash, x, y, z) {
    var h = hash & 15;
    var u = h < 8 ? x : y;
    var v = h < 4 ? y : (h === 12 || h === 14 ? x : z);
    return ((h & 1) === 0 ? u : -u) + ((h & 2) === 0 ? v : -v);
}

/** Klasyczny Perlin 2D/3D z permutacja z seeda. */
AIMC.Perlin = function (seed) {
    var rand = AIMC.makeRandom(seed);
    var perm = new Array(256), i, j, tmp;
    for (i = 0; i < 256; i++) perm[i] = i;
    for (i = 255; i > 0; i--) {
        j = (rand() * (i + 1)) | 0;
        tmp = perm[i]; perm[i] = perm[j]; perm[j] = tmp;
    }
    this.p = new Array(512);
    for (i = 0; i < 512; i++) this.p[i] = perm[i & 255];
};

AIMC.Perlin.prototype.noise2 = function (x, y) {
    var p = this.p;
    var xi = Math.floor(x), yi = Math.floor(y);
    var X = xi & 255, Y = yi & 255;
    x -= xi; y -= yi;
    var u = fade(x), v = fade(y);
    var aa = p[p[X] + Y], ab = p[p[X] + Y + 1];
    var ba = p[p[X + 1] + Y], bb = p[p[X + 1] + Y + 1];
    return lerp(
        lerp(grad2(aa, x, y), grad2(ba, x - 1, y), u),
        lerp(grad2(ab, x, y - 1), grad2(bb, x - 1, y - 1), u),
        v);
};

AIMC.Perlin.prototype.fbm2 = function (x, y, octaves, lacunarity, gain) {
    var amp = 1, freq = 1, sum = 0, norm = 0, i;
    for (i = 0; i < octaves; i++) {
        sum += amp * this.noise2(x * freq, y * freq);
        norm += amp;
        amp *= gain;
        freq *= lacunarity;
    }
    return sum / norm;
};

AIMC.Perlin.prototype.noise3 = function (x, y, z) {
    var p = this.p;
    var xi = Math.floor(x), yi = Math.floor(y), zi = Math.floor(z);
    var X = xi & 255, Y = yi & 255, Z = zi & 255;
    x -= xi; y -= yi; z -= zi;
    var u = fade(x), v = fade(y), w = fade(z);
    var aaa = p[p[p[X] + Y] + Z], aba = p[p[p[X] + Y + 1] + Z];
    var aab = p[p[p[X] + Y] + Z + 1], abb = p[p[p[X] + Y + 1] + Z + 1];
    var baa = p[p[p[X + 1] + Y] + Z], bba = p[p[p[X + 1] + Y + 1] + Z];
    var bab = p[p[p[X + 1] + Y] + Z + 1], bbb = p[p[p[X + 1] + Y + 1] + Z + 1];
    return lerp(
        lerp(
            lerp(grad3(aaa, x, y, z), grad3(baa, x - 1, y, z), u),
            lerp(grad3(aba, x, y - 1, z), grad3(bba, x - 1, y - 1, z), u),
            v),
        lerp(
            lerp(grad3(aab, x, y, z - 1), grad3(bab, x - 1, y, z - 1), u),
            lerp(grad3(abb, x, y - 1, z - 1), grad3(bbb, x - 1, y - 1, z - 1), u),
            v),
        w);
};

AIMC.Perlin.prototype.fbm3 = function (x, y, z, octaves, lacunarity, gain) {
    var amp = 1, freq = 1, sum = 0, norm = 0, i;
    for (i = 0; i < octaves; i++) {
        sum += amp * this.noise3(x * freq, y * freq, z * freq);
        norm += amp;
        amp *= gain;
        freq *= lacunarity;
    }
    return sum / norm;
};

/** Deterministyczny hash 2D -> [0, 1). */
AIMC.hash01 = function (x, z, seed) {
    var h = (seed >>> 0) ^ Math.imul(x | 0, 0x9E3779B1) ^ Math.imul(z | 0, 0x85EBCA6B);
    h = Math.imul(h ^ (h >>> 15), 0xC2B2AE35);
    h = Math.imul(h ^ (h >>> 13), 0x27D4EB2F);
    h ^= h >>> 16;
    return (h >>> 0) / 4294967296;
};

/** Deterministyczny hash 3D -> [0, 1). */
AIMC.hash3 = function (x, y, z, seed) {
    var h = (seed >>> 0) ^ Math.imul(x | 0, 0x9E3779B1)
        ^ Math.imul(y | 0, 0xC2B2AE35) ^ Math.imul(z | 0, 0x165667B1);
    h = Math.imul(h ^ (h >>> 15), 0x85EBCA6B);
    h = Math.imul(h ^ (h >>> 13), 0xC2B2AE35);
    h ^= h >>> 16;
    return (h >>> 0) / 4294967296;
};
