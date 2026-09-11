/* AiMINECRAFT Web - raycast DDA po voxelach. Bez DOM. */
"use strict";

var AIMC = (typeof window !== "undefined")
    ? (window.AIMC || (window.AIMC = {}))
    : (typeof module !== "undefined" && module.exports
        ? require("./save.js")
        : {});
if (typeof module !== "undefined" && module.exports) module.exports = AIMC;

/**
 * Raycast DDA. world musi miec isSolid(x,y,z).
 * origin/dir: {x,y,z}. Zwraca {x,y,z,nx,ny,nz} lub null.
 */
AIMC.raycast = function (world, origin, dir, maxDist) {
    var x = Math.floor(origin.x), y = Math.floor(origin.y), z = Math.floor(origin.z);
    var dx = dir.x, dy = dir.y, dz = dir.z;
    var stepX = dx > 0 ? 1 : -1;
    var stepY = dy > 0 ? 1 : -1;
    var stepZ = dz > 0 ? 1 : -1;
    var tDX = dx !== 0 ? Math.abs(1 / dx) : Infinity;
    var tDY = dy !== 0 ? Math.abs(1 / dy) : Infinity;
    var tDZ = dz !== 0 ? Math.abs(1 / dz) : Infinity;
    var distX = stepX > 0 ? (x + 1 - origin.x) : (origin.x - x);
    var distY = stepY > 0 ? (y + 1 - origin.y) : (origin.y - y);
    var distZ = stepZ > 0 ? (z + 1 - origin.z) : (origin.z - z);
    var tMX = tDX === Infinity ? Infinity : tDX * distX;
    var tMY = tDY === Infinity ? Infinity : tDY * distY;
    var tMZ = tDZ === Infinity ? Infinity : tDZ * distZ;
    var nx = 0, ny = 0, nz = 0, t = 0;
    for (var i = 0; i < 256; i++) {
        if (tMX < tMY && tMX < tMZ) {
            x += stepX; t = tMX; tMX += tDX;
            nx = -stepX; ny = 0; nz = 0;
        } else if (tMY < tMZ) {
            y += stepY; t = tMY; tMY += tDY;
            nx = 0; ny = -stepY; nz = 0;
        } else {
            z += stepZ; t = tMZ; tMZ += tDZ;
            nx = 0; ny = 0; nz = -stepZ;
        }
        if (t > maxDist) return null;
        if (world.isSolid(x, y, z)) {
            return {x: x, y: y, z: z, nx: nx, ny: ny, nz: nz};
        }
    }
    return null;
};
