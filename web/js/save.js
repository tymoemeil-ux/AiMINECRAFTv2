/* AiMINECRAFT Web - zapis w localStorage (seed, gracz, ekwipunek, diff blokow). */
"use strict";

var AIMC = (typeof window !== "undefined")
    ? (window.AIMC || (window.AIMC = {}))
    : (typeof module !== "undefined" && module.exports
        ? require("./inventory.js")
        : {});
if (typeof module !== "undefined" && module.exports) module.exports = AIMC;

AIMC.SAVE_KEY = "aiminecraft_web_save_v1";
AIMC.SETTINGS_KEY = "aiminecraft_web_settings_v1";
AIMC.MAX_DIFF = 150000;

/** Pamiec zapasowa, gdyby localStorage byl niedostepny (np. file:// w starej przegladarce). */
AIMC.memStore = {};

AIMC.storeGet = function (key) {
    try {
        if (typeof window !== "undefined" && window.localStorage) {
            return window.localStorage.getItem(key);
        }
    } catch (e) {}
    return AIMC.memStore.hasOwnProperty(key) ? AIMC.memStore[key] : null;
};

AIMC.storeSet = function (key, value) {
    try {
        if (typeof window !== "undefined" && window.localStorage) {
            window.localStorage.setItem(key, value);
            return;
        }
    } catch (e) {}
    AIMC.memStore[key] = value;
};

AIMC.storeDel = function (key) {
    try {
        if (typeof window !== "undefined" && window.localStorage) {
            window.localStorage.removeItem(key);
        }
    } catch (e) {}
    delete AIMC.memStore[key];
};

/**
 * Indeks zmian gracza: chunkKey -> tablica [lx, y, lz, id].
 * chunkKey = cx + "," + cz.
 */
AIMC.DiffIndex = function () {
    this.map = {};
    this.count = 0;
};

AIMC.DiffIndex.prototype.chunkKey = function (cx, cz) {
    return cx + "," + cz;
};

AIMC.DiffIndex.prototype.set = function (x, y, z, id) {
    var S = AIMC.CHUNK || 16;
    var cx = Math.floor(x / S), cz = Math.floor(z / S);
    var k = this.chunkKey(cx, cz);
    var lx = x - cx * S, lz = z - cz * S;
    var arr = this.map[k];
    if (!arr) { arr = []; this.map[k] = arr; }
    // Nadpisz istniejacy wpis dla tej kolumny.
    for (var i = 0; i < arr.length; i++) {
        if (arr[i][0] === lx && arr[i][1] === y && arr[i][2] === lz) {
            arr[i][3] = id;
            return;
        }
    }
    arr.push([lx, y, lz, id]);
    this.count++;
};

/** Wpisz zmiany do danych chunka (Uint8Array). */
AIMC.DiffIndex.prototype.applyTo = function (cx, cz, data) {
    var arr = this.map[this.chunkKey(cx, cz)];
    if (!arr) return;
    for (var i = 0; i < arr.length; i++) {
        var e = arr[i];
        data[(e[1] << 8) | (e[2] << 4) | e[0]] = e[3];
    }
};

/** Serializacja do zapisu: [[x,y,z,id], ...] (wspolrzedne swiata). */
AIMC.DiffIndex.prototype.serialize = function () {
    var S = AIMC.CHUNK || 16;
    var out = [], k, arr, i, j, parts, cx, cz;
    for (k in this.map) {
        if (!this.map.hasOwnProperty(k)) continue;
        parts = k.split(",");
        cx = +parts[0]; cz = +parts[1];
        arr = this.map[k];
        for (i = 0; i < arr.length && out.length < AIMC.MAX_DIFF; i++) {
            out.push([cx * S + arr[i][0], arr[i][1], cz * S + arr[i][2], arr[i][3]]);
        }
    }
    return out;
};

AIMC.DiffIndex.prototype.load = function (list) {
    this.map = {};
    this.count = 0;
    if (!list) return;
    for (var i = 0; i < list.length; i++) {
        this.set(list[i][0], list[i][1], list[i][2], list[i][3]);
    }
};
