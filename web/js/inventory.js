/* AiMINECRAFT Web - ekwipunek i logika klikow w sloty. Bez DOM. */
"use strict";

var AIMC = (typeof window !== "undefined")
    ? (window.AIMC || (window.AIMC = {}))
    : (typeof module !== "undefined" && module.exports
        ? require("./worldgen.js")
        : {});
if (typeof module !== "undefined" && module.exports) module.exports = AIMC;

AIMC.INV_SIZE = 36;
AIMC.INV_HOTBAR = 9;
AIMC.MAX_STACK = 64;

/** Stos: {id, count} lub null. */
AIMC.makeStack = function (id, count) {
    return {id: id, count: count};
};

AIMC.Inventory = function () {
    this.slots = new Array(AIMC.INV_SIZE).fill(null);
    this.selected = 0;
};

AIMC.Inventory.prototype.get = function (i) {
    return this.slots[i];
};

AIMC.Inventory.prototype.set = function (i, stack) {
    this.slots[i] = (stack && stack.count > 0) ? stack : null;
};

AIMC.Inventory.prototype.held = function () {
    return this.slots[this.selected];
};

AIMC.Inventory.prototype.heldId = function () {
    var s = this.held();
    return s ? s.id : 0;
};

AIMC.Inventory.prototype.heldTool = function () {
    var it = AIMC.itemDef(this.heldId());
    return it ? it.tool : 0;
};

/** Dodaje; zwraca liczbe, ktora sie nie zmiescila. */
AIMC.Inventory.prototype.add = function (id, count) {
    var i, s, take, space;
    for (i = 0; i < AIMC.INV_SIZE && count > 0; i++) {
        s = this.slots[i];
        if (s && s.id === id && s.count < AIMC.MAX_STACK) {
            space = AIMC.MAX_STACK - s.count;
            take = Math.min(space, count);
            s.count += take;
            count -= take;
        }
    }
    for (i = 0; i < AIMC.INV_SIZE && count > 0; i++) {
        if (!this.slots[i]) {
            take = Math.min(AIMC.MAX_STACK, count);
            this.slots[i] = {id: id, count: take};
            count -= take;
        }
    }
    return count;
};

AIMC.Inventory.prototype.consumeHeld = function () {
    var s = this.slots[this.selected];
    if (!s) return false;
    s.count--;
    if (s.count <= 0) this.slots[this.selected] = null;
    return true;
};

AIMC.Inventory.prototype.clear = function () {
    this.slots.fill(null);
};

/**
 * Klik w slot (tablica + kursor {stack}).
 * Zwraca nowy kursor (stos lub null). Opcjonalnie gra dzwiek przez cb.
 */
AIMC.clickSlot = function (arr, index, cursor, right, cb) {
    var slot = arr[index];
    function snd() { if (cb) cb(); }
    if (!cursor) {
        if (!slot) return null;
        if (right) {
            var half = Math.ceil(slot.count / 2);
            slot.count -= half;
            if (slot.count <= 0) arr[index] = null;
            snd();
            return {id: slot.id, count: half};
        }
        arr[index] = null;
        snd();
        return slot;
    }
    if (!slot) {
        if (right) {
            arr[index] = {id: cursor.id, count: 1};
            cursor.count--;
            snd();
            return cursor.count > 0 ? cursor : null;
        }
        arr[index] = cursor;
        snd();
        return null;
    }
    if (slot.id === cursor.id) {
        if (right) {
            if (slot.count < AIMC.MAX_STACK) {
                slot.count++;
                cursor.count--;
                snd();
                return cursor.count > 0 ? cursor : null;
            }
            return cursor;
        }
        var space = AIMC.MAX_STACK - slot.count;
        var move = Math.min(space, cursor.count);
        if (move > 0) {
            slot.count += move;
            cursor.count -= move;
            snd();
            return cursor.count > 0 ? cursor : null;
        }
        return cursor;
    }
    if (!right) {
        arr[index] = cursor;
        snd();
        return slot;
    }
    return cursor;
};
