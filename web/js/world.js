/* AiMINECRAFT Web - swiat: chunki, streaming, meshowanie (Three.js). */
"use strict";

var AIMC = (typeof window !== "undefined")
    ? (window.AIMC || (window.AIMC = {}))
    : {};
if (typeof module !== "undefined" && module.exports) module.exports = AIMC;

/** Laduje obrazki z data-URLi. Zwraca Promise z tablica Image. */
AIMC.loadImages = function (urls) {
    return Promise.all(urls.map(function (u) {
        return new Promise(function (resolve, reject) {
            var img = new Image();
            img.onload = function () { resolve(img); };
            img.onerror = reject;
            img.src = u;
        });
    }));
};

/** Buduje atlas na canvasie + THREE.CanvasTexture. */
AIMC.buildAtlas = function (THREE, images, grid) {
    var size = grid * 16;
    var canvas = document.createElement("canvas");
    canvas.width = size;
    canvas.height = size;
    var ctx = canvas.getContext("2d");
    ctx.clearRect(0, 0, size, size);
    for (var i = 0; i < images.length; i++) {
        ctx.drawImage(images[i], (i % grid) * 16, Math.floor(i / grid) * 16);
    }
    var tex = new THREE.CanvasTexture(canvas);
    tex.magFilter = THREE.NearestFilter;
    tex.minFilter = THREE.NearestFilter;
    tex.generateMipmaps = false;
    // Sredni kolor kazdego kafla (do particles).
    var colors = [];
    var img = ctx.getImageData(0, 0, size, size).data;
    for (var t = 0; t < grid * grid; t++) {
        var r = 0, g = 0, b = 0, n = 0;
        var ox = (t % grid) * 16, oy = Math.floor(t / grid) * 16;
        for (var py = 0; py < 16; py++) {
            for (var px = 0; px < 16; px++) {
                var o = (((oy + py) * size) + (ox + px)) * 4;
                if (img[o + 3] > 32) { r += img[o]; g += img[o + 1]; b += img[o + 2]; n++; }
            }
        }
        if (n === 0) n = 1;
        colors.push([r / n / 255, g / n / 255, b / n / 255]);
    }
    return {canvas: canvas, texture: tex, url: canvas.toDataURL(), grid: grid, colors: colors};
};

// --- Mesher ---

var NEIGHBOR = [[1, 0, 0], [-1, 0, 0], [0, 1, 0], [0, -1, 0], [0, 0, 1], [0, 0, -1]];
var CORNERS = [
    [[1, 0, 1], [1, 0, 0], [1, 1, 0], [1, 1, 1]],
    [[0, 0, 0], [0, 0, 1], [0, 1, 1], [0, 1, 0]],
    [[0, 1, 1], [1, 1, 1], [1, 1, 0], [0, 1, 0]],
    [[0, 0, 0], [1, 0, 0], [1, 0, 1], [0, 0, 1]],
    [[0, 0, 1], [1, 0, 1], [1, 1, 1], [0, 1, 1]],
    [[1, 0, 0], [0, 0, 0], [0, 1, 0], [1, 1, 0]]
];
var NORMALS = [[1, 0, 0], [-1, 0, 0], [0, 1, 0], [0, -1, 0], [0, 0, 1], [0, 0, -1]];
var FACE_UV = [[0, 0], [1, 0], [1, 1], [0, 1]];
var SHADE = [0.65, 0.65, 1.0, 0.5, 0.8, 0.8];
var TILE_KIND = [2, 2, 0, 1, 2, 2]; // 0=gora, 1=dol, 2=bok
var WATER_TOP = 0.9;

function tileUV(tile, grid) {
    var inset = 0.5 / (grid * 16);
    var c = tile % grid, r = Math.floor(tile / grid);
    return {
        u0: c / grid + inset,
        u1: (c + 1) / grid - inset,
        vTop: 1 - r / grid - inset,      // gora obrazka kafla
        vBottom: 1 - (r + 1) / grid + inset
    };
}

AIMC.World = function (THREE, scene, atlasTex, seed, renderDist) {
    this.THREE = THREE;
    this.scene = scene;
    this.renderDist = renderDist;
    this.gen = new AIMC.WorldGen(seed >>> 0);
    this.diff = new AIMC.DiffIndex();
    this.chunks = {};
    this.dirty = [];
    this.queued = {};
    this.centerCx = 1e9;
    this.centerCz = 1e9;

    this.matOpaque = new THREE.MeshLambertMaterial({
        map: atlasTex, vertexColors: true, alphaTest: 0.5
    });
    this.matWater = new THREE.MeshLambertMaterial({
        map: atlasTex, vertexColors: true, transparent: true,
        opacity: 0.72, depthWrite: false, side: THREE.DoubleSide
    });
};

AIMC.World.prototype.key = function (cx, cz) {
    return cx + "," + cz;
};

AIMC.World.prototype.getChunk = function (cx, cz) {
    return this.chunks[this.key(cx, cz)] || null;
};

AIMC.World.prototype.getBlock = function (x, y, z) {
    if (y < 0) return 3;
    if (y >= AIMC.HEIGHT) return 0;
    var S = AIMC.CHUNK;
    var c = this.getChunk(Math.floor(x / S), Math.floor(z / S));
    if (!c) return 0;
    var lx = x - Math.floor(x / S) * S, lz = z - Math.floor(z / S) * S;
    return c.data[(y << 8) | (lz << 4) | lx];
};

AIMC.World.prototype.isSolid = function (x, y, z) {
    var b = AIMC.blockDef(this.getBlock(x, y, z));
    return !!b && !!b.solid;
};

AIMC.World.prototype.setBlock = function (x, y, z, id, record) {
    if (y < 0 || y >= AIMC.HEIGHT) return;
    var S = AIMC.CHUNK;
    var cx = Math.floor(x / S), cz = Math.floor(z / S);
    var c = this.getChunk(cx, cz);
    if (!c) return;
    var lx = x - cx * S, lz = z - cz * S;
    c.data[(y << 8) | (lz << 4) | lx] = id;
    if (record !== false) this.diff.set(x, y, z, id);
    this.markDirty(cx, cz);
    if (lx === 0) this.markDirty(cx - 1, cz);
    if (lx === S - 1) this.markDirty(cx + 1, cz);
    if (lz === 0) this.markDirty(cx, cz - 1);
    if (lz === S - 1) this.markDirty(cx, cz + 1);
};

AIMC.World.prototype.markDirty = function (cx, cz) {
    if (Math.max(Math.abs(cx - this.centerCx), Math.abs(cz - this.centerCz)) > this.renderDist) return;
    var k = this.key(cx, cz);
    if (!this.chunks[k] || this.queued[k]) return;
    this.queued[k] = true;
    this.dirty.push(k);
};

AIMC.World.prototype.updateStreaming = function (pcx, pcz) {
    if (pcx === this.centerCx && pcz === this.centerCz) return;
    this.centerCx = pcx;
    this.centerCz = pcz;
    var dataR = this.renderDist + 1, unloadR = this.renderDist + 2;
    var k, c, parts, dx, dz;
    for (k in this.chunks) {
        if (!this.chunks.hasOwnProperty(k)) continue;
        parts = k.split(",");
        dx = Math.abs(+parts[0] - pcx);
        dz = Math.abs(+parts[1] - pcz);
        if (Math.max(dx, dz) > unloadR) {
            c = this.chunks[k];
            this.disposeChunk(c);
            delete this.chunks[k];
            delete this.queued[k];
        }
    }
    for (var r = 0; r <= dataR; r++) {
        for (dx = -r; dx <= r; dx++) {
            for (dz = -r; dz <= r; dz++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) !== r) continue;
                this.ensureData(pcx + dx, pcz + dz);
            }
        }
    }
};

AIMC.World.prototype.ensureData = function (cx, cz) {
    var k = this.key(cx, cz);
    if (this.chunks[k]) return;
    var data = this.gen.generate(cx, cz);
    this.diff.applyTo(cx, cz, data);
    this.chunks[k] = {cx: cx, cz: cz, data: data, meshO: null, meshW: null};
    this.markDirty(cx, cz);
    this.markDirty(cx + 1, cz);
    this.markDirty(cx - 1, cz);
    this.markDirty(cx, cz + 1);
    this.markDirty(cx, cz - 1);
};

AIMC.World.prototype.hasDataAround = function (cx, cz) {
    for (var dx = -1; dx <= 1; dx++) {
        for (var dz = -1; dz <= 1; dz++) {
            if (!this.chunks[this.key(cx + dx, cz + dz)]) return false;
        }
    }
    return true;
};

AIMC.World.prototype.updateMeshes = function (budget) {
    var done = 0, attempts = this.dirty.length * 2 + 16;
    while (done < budget && this.dirty.length > 0 && attempts-- > 0) {
        var k = this.dirty.shift();
        this.queued[k] = false;
        var c = this.chunks[k];
        if (!c) continue;
        if (!this.hasDataAround(c.cx, c.cz)) {
            if (!this.queued[k]) { this.queued[k] = true; this.dirty.push(k); }
            continue;
        }
        this.remesh(c);
        done++;
    }
    return done;
};

AIMC.World.prototype.disposeChunk = function (c) {
    if (c.meshO) { this.scene.remove(c.meshO); c.meshO.geometry.dispose(); c.meshO = null; }
    if (c.meshW) { this.scene.remove(c.meshW); c.meshW.geometry.dispose(); c.meshW = null; }
};

AIMC.World.prototype.remesh = function (c) {
    var S = AIMC.CHUNK, H = AIMC.HEIGHT;
    var pos = [], nor = [], uv = [], col = [], idx = [];
    var wpos = [], wnor = [], wuv = [], wcol = [], widx = [];
    var baseX = c.cx * S, baseZ = c.cz * S;
    var grid = 8;
    var x, y, z, id, b;

    for (y = 0; y < H; y++) {
        for (z = 0; z < S; z++) {
            for (x = 0; x < S; x++) {
                id = c.data[(y << 8) | (z << 4) | x];
                if (id === 0) continue;
                b = AIMC.blockDef(id);
                if (!b) continue;
                var wx = baseX + x, wz = baseZ + z;
                if (b.kind === 3) {
                    this.emitWater(wx, y, wz, wpos, wnor, wuv, wcol, widx, grid);
                } else if (b.kind === 1) {
                    this.emitCross(wx, y, wz, b.side, 0.15, 0.85, 1.0, pos, nor, uv, col, idx, grid);
                } else if (b.kind === 2) {
                    this.emitCross(wx, y, wz, b.side, 0.375, 0.625, 0.625, pos, nor, uv, col, idx, grid);
                } else {
                    this.emitCube(b, wx, y, wz, pos, nor, uv, col, idx, grid);
                }
            }
        }
    }
    this.disposeChunk(c);
    var THREE = this.THREE;
    if (idx.length > 0) {
        var g = new THREE.BufferGeometry();
        g.setAttribute("position", new THREE.Float32BufferAttribute(pos, 3));
        g.setAttribute("normal", new THREE.Float32BufferAttribute(nor, 3));
        g.setAttribute("uv", new THREE.Float32BufferAttribute(uv, 2));
        g.setAttribute("color", new THREE.Float32BufferAttribute(col, 3));
        g.setIndex(idx);
        c.meshO = new THREE.Mesh(g, this.matOpaque);
        c.meshO.matrixAutoUpdate = false;
        this.scene.add(c.meshO);
    }
    if (widx.length > 0) {
        var wg = new THREE.BufferGeometry();
        wg.setAttribute("position", new THREE.Float32BufferAttribute(wpos, 3));
        wg.setAttribute("normal", new THREE.Float32BufferAttribute(wnor, 3));
        wg.setAttribute("uv", new THREE.Float32BufferAttribute(wuv, 2));
        wg.setAttribute("color", new THREE.Float32BufferAttribute(wcol, 3));
        wg.setIndex(widx);
        c.meshW = new THREE.Mesh(wg, this.matWater);
        c.meshW.matrixAutoUpdate = false;
        c.meshW.renderOrder = 1;
        this.scene.add(c.meshW);
    }
};

AIMC.World.prototype.emitCube = function (b, wx, y, wz, pos, nor, uv, col, idx, grid) {
    for (var f = 0; f < 6; f++) {
        var n = NEIGHBOR[f];
        var nb = AIMC.blockDef(this.getBlock(wx + n[0], y + n[1], wz + n[2]));
        if (nb && nb.opaque && !(b.leaves && nb.leaves)) continue;
        if (b === AIMC.blockDef(11) && nb === AIMC.blockDef(11)) continue; // szyba-szyba
        var kind = TILE_KIND[f];
        var tile = kind === 0 ? b.top : (kind === 1 ? b.bottom : b.side);
        var t = tileUV(tile, grid);
        var shade = SHADE[f];
        var base = pos.length / 3;
        var corners = CORNERS[f], normal = NORMALS[f];
        for (var i = 0; i < 4; i++) {
            pos.push(wx + corners[i][0], y + corners[i][1], wz + corners[i][2]);
            nor.push(normal[0], normal[1], normal[2]);
            uv.push(FACE_UV[i][0] === 0 ? t.u0 : t.u1,
                    FACE_UV[i][1] === 0 ? t.vBottom : t.vTop);
            col.push(shade, shade, shade);
        }
        idx.push(base, base + 1, base + 2, base, base + 2, base + 3);
    }
};

AIMC.World.prototype.emitWater = function (wx, y, wz, pos, nor, uv, col, idx, grid) {
    var t = tileUV(8, grid);
    for (var f = 0; f < 6; f++) {
        var n = NEIGHBOR[f];
        var nid = this.getBlock(wx + n[0], y + n[1], wz + n[2]);
        if (nid === 7) continue;
        var nb = AIMC.blockDef(nid);
        if (nb && nb.opaque) continue;
        var shade = SHADE[f] * 0.95;
        var base = pos.length / 3;
        var corners = CORNERS[f], normal = NORMALS[f];
        for (var i = 0; i < 4; i++) {
            var cy = corners[i][1];
            if (f !== 3 && cy === 1) cy = WATER_TOP;
            pos.push(wx + corners[i][0], y + cy, wz + corners[i][2]);
            nor.push(normal[0], normal[1], normal[2]);
            uv.push(FACE_UV[i][0] === 0 ? t.u0 : t.u1,
                    FACE_UV[i][1] === 0 ? t.vBottom : t.vTop);
            col.push(shade, shade, shade);
        }
        idx.push(base, base + 1, base + 2, base, base + 2, base + 3);
    }
};

AIMC.World.prototype.emitCross = function (wx, y, wz, tile, min, max, top,
                                            pos, nor, uv, col, idx, grid) {
    var t = tileUV(tile, grid);
    var quads = [
        [min, min, max, max],
        [max, min, min, max]
    ];
    for (var q = 0; q < 2; q++) {
        var a = quads[q];
        var base = pos.length / 3;
        var verts = [
            [a[0], 0, a[1], t.u0, t.vBottom],
            [a[2], 0, a[3], t.u1, t.vBottom],
            [a[2], top, a[3], t.u1, t.vTop],
            [a[0], top, a[1], t.u0, t.vTop]
        ];
        for (var i = 0; i < 4; i++) {
            pos.push(wx + verts[i][0], y + verts[i][1], wz + verts[i][2]);
            nor.push(0, 1, 0);
            uv.push(verts[i][3], verts[i][4]);
            col.push(1, 1, 1);
        }
        // Obie strony.
        idx.push(base, base + 1, base + 2, base, base + 2, base + 3);
        idx.push(base, base + 2, base + 1, base, base + 3, base + 2);
    }
};

AIMC.World.prototype.chunkCount = function () {
    return Object.keys(this.chunks).length;
};

AIMC.World.prototype.dispose = function () {
    for (var k in this.chunks) {
        if (this.chunks.hasOwnProperty(k)) this.disposeChunk(this.chunks[k]);
    }
    this.chunks = {};
    this.dirty = [];
    this.queued = {};
};
