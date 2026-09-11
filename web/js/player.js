/* AiMINECRAFT Web - gracz: ruch, fizyka, kolizje. Bez DOM i bez THREE. */
"use strict";

var AIMC = (typeof window !== "undefined")
    ? (window.AIMC || (window.AIMC = {}))
    : {};
if (typeof module !== "undefined" && module.exports) module.exports = AIMC;

AIMC.EYE = 1.62;
AIMC.RADIUS = 0.3;
AIMC.HEIGHT_P = 1.8;

AIMC.Player = function () {
    this.x = 0.5; this.y = 40; this.z = 0.5;
    this.vx = 0; this.vy = 0; this.vz = 0;
    this.yaw = 0; this.pitch = 0;
    this.onGround = false;
    this.inWater = false;
    this.flying = false;
    this.sprinting = false;
};

AIMC.Player.prototype.eye = function () {
    return {x: this.x, y: this.y + AIMC.EYE, z: this.z};
};

AIMC.Player.prototype.dir = function () {
    var cp = Math.cos(this.pitch);
    return {
        x: -Math.sin(this.yaw) * cp,
        y: Math.sin(this.pitch),
        z: -Math.cos(this.yaw) * cp
    };
};

AIMC.Player.prototype.look = function (dx, dy, sensitivity) {
    this.yaw -= dx * sensitivity;
    this.pitch -= dy * sensitivity;
    var lim = Math.PI / 2 - 0.01;
    if (this.pitch > lim) this.pitch = lim;
    if (this.pitch < -lim) this.pitch = -lim;
};

/**
 * keys: {f,b,l,r,jump,sneak,sprint,flyUp,flyDown} (bool).
 * update() zwraca true, gdy gracz wlasnie wyladowal w wodzie (dla dzwieku).
 */
AIMC.Player.prototype.update = function (dt, keys, world) {
    if (dt > 0.05) dt = 0.05;
    var wasInWater = this.inWater;
    var feet = world.getBlock(Math.floor(this.x), Math.floor(this.y), Math.floor(this.z));
    var waist = world.getBlock(Math.floor(this.x), Math.floor(this.y + 0.6), Math.floor(this.z));
    this.inWater = (feet === 7 || waist === 7);

    this.sprinting = !!keys.sprint && !!keys.f && !this.inWater;
    var speed = this.flying ? 10.0 : (this.sprinting ? 6.4 : 4.3);
    if (keys.sneak && !this.flying) speed = 2.2;
    if (this.inWater && !this.flying) speed = 2.6;

    var sy = Math.sin(this.yaw), cy = Math.cos(this.yaw);
    var mx = 0, mz = 0;
    if (keys.f) { mx += -sy; mz += -cy; }
    if (keys.b) { mx += sy; mz += cy; }
    if (keys.l) { mx += -cy; mz += sy; }
    if (keys.r) { mx += cy; mz += -sy; }
    var len = Math.sqrt(mx * mx + mz * mz);
    if (len > 0) { mx = mx / len * speed; mz = mz / len * speed; }

    var accel = this.onGround ? 14.0 : (this.inWater ? 6.0 : 4.0);
    if (this.flying) accel = 12.0;
    function approach(v, target, a, dt) {
        if (v < target) return Math.min(v + a * dt, target);
        return Math.max(v - a * dt, target);
    }
    this.vx = approach(this.vx, mx, accel, dt);
    this.vz = approach(this.vz, mz, accel, dt);

    if (this.flying) {
        var vyT = 0;
        if (keys.flyUp || keys.jump) vyT += 8.0;
        if (keys.flyDown || keys.sneak) vyT -= 8.0;
        this.vy = approach(this.vy, vyT, 12.0, dt);
    } else if (this.inWater) {
        if (keys.jump) this.vy += 30.0 * dt;
        this.vy -= 12.0 * dt;
        this.vy *= Math.max(0, 1 - 3.0 * dt);
        if (this.vy > 3.5) this.vy = 3.5;
        if (this.vy < -3.5) this.vy = -3.5;
    } else {
        this.vy -= 28.0 * dt;
        if (this.vy < -50) this.vy = -50;
        if (keys.jump && this.onGround) this.vy = 9.0;
    }

    var R = AIMC.RADIUS, H = AIMC.HEIGHT_P;
    this.onGround = false;
    if (this.vx !== 0) this.x = this.moveAxis(world, this.x, this.vx * dt, 0, R, H);
    if (this.vz !== 0) this.z = this.moveAxis(world, this.z, this.vz * dt, 1, R, H);
    if (this.vy !== 0) this.y = this.moveAxis(world, this.y, this.vy * dt, 2, R, H);

    if (this.y < 0) { this.y = 0; this.vy = 0; this.onGround = true; }
    if (this.y > AIMC.HEIGHT + 20) { this.y = AIMC.HEIGHT + 20; this.vy = 0; }
    return !wasInWater && this.inWater && this.vy < -2;
};

AIMC.Player.prototype.moveAxis = function (world, coord, delta, axis, R, H) {
    if (delta === 0) return coord;
    var nx = this.x, ny = this.y, nz = this.z;
    if (axis === 0) nx += delta;
    else if (axis === 1) nz += delta;
    else ny += delta;
    var minX = Math.floor(nx - R), maxX = Math.floor(nx + R);
    var minY = Math.floor(ny), maxY = Math.floor(ny + H);
    var minZ = Math.floor(nz - R), maxZ = Math.floor(nz + R);
    for (var bx = minX; bx <= maxX; bx++) {
        for (var by = minY; by <= maxY; by++) {
            for (var bz = minZ; bz <= maxZ; bz++) {
                if (world.isSolid(bx, by, bz)) {
                    if (axis === 0) {
                        this.vx = 0;
                        return delta > 0 ? bx - R - 0.001 : bx + 1 + R + 0.001;
                    } else if (axis === 1) {
                        this.vz = 0;
                        return delta > 0 ? bz - R - 0.001 : bz + 1 + R + 0.001;
                    } else {
                        if (delta < 0) this.onGround = true;
                        this.vy = 0;
                        return delta > 0 ? by - H - 0.001 : by + 1;
                    }
                }
            }
        }
    }
    return axis === 0 ? nx : (axis === 1 ? nz : ny);
};

AIMC.Player.prototype.serialize = function () {
    return {x: this.x, y: this.y, z: this.z, yaw: this.yaw, pitch: this.pitch, flying: this.flying};
};

AIMC.Player.prototype.load = function (s) {
    if (!s) return;
    this.x = s.x; this.y = s.y; this.z = s.z;
    this.yaw = s.yaw || 0; this.pitch = s.pitch || 0;
    this.flying = !!s.flying;
    this.vx = 0; this.vy = 0; this.vz = 0;
};
