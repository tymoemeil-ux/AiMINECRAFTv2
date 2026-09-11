/* AiMINECRAFT Web - dane: bloki, przedmioty, receptury. Bez DOM (testowalne w node). */
"use strict";

var AIMC = (typeof window !== "undefined")
    ? (window.AIMC || (window.AIMC = {}))
    : {};
if (typeof module !== "undefined" && module.exports) module.exports = AIMC;

// Narzedzia.
AIMC.TOOL_NONE = 0;
AIMC.TOOL_PICKAXE = 1;
AIMC.TOOL_AXE = 2;
AIMC.TOOL_SHOVEL = 3;
AIMC.TOOL_SWORD = 4;
AIMC.TOOL_HOE = 5;

// Bloki: id -> {name, top, side, bottom, opaque, solid, kind, hard, tool, drop}.
// kind: 0=cube, 1=cross, 2=torch, 3=water. hard=1e9 oznacza niezniszczalny.
AIMC.BLOCKS = {
    1:  {name: "Trawa", side: 1, top: 0, bottom: 2, opaque: 1, solid: 1, kind: 0, hard: 0.5, tool: 3, drop: 2},
    2:  {name: "Ziemia", side: 2, top: 2, bottom: 2, opaque: 1, solid: 1, kind: 0, hard: 0.5, tool: 3, drop: 2},
    3:  {name: "Kamien", side: 3, top: 3, bottom: 3, opaque: 1, solid: 1, kind: 0, hard: 2.0, tool: 1, drop: 9},
    4:  {name: "Pien debu", side: 4, top: 5, bottom: 5, opaque: 1, solid: 1, kind: 0, hard: 1.2, tool: 2, drop: 4},
    5:  {name: "Liscie debu", side: 6, top: 6, bottom: 6, opaque: 1, solid: 1, kind: 0, hard: 0.25, tool: 5, drop: 0, leaves: 1},
    6:  {name: "Bedrock", side: 7, top: 7, bottom: 7, opaque: 1, solid: 1, kind: 0, hard: 1e9, tool: 0, drop: 0},
    7:  {name: "Woda", side: 8, top: 8, bottom: 8, opaque: 0, solid: 0, kind: 3, hard: 1e9, tool: 0, drop: 0},
    8:  {name: "Piasek", side: 9, top: 9, bottom: 9, opaque: 1, solid: 1, kind: 0, hard: 0.5, tool: 3, drop: 8},
    9:  {name: "Bruk", side: 10, top: 10, bottom: 10, opaque: 1, solid: 1, kind: 0, hard: 2.0, tool: 1, drop: 9},
    10: {name: "Deski", side: 11, top: 11, bottom: 11, opaque: 1, solid: 1, kind: 0, hard: 1.2, tool: 2, drop: 10},
    11: {name: "Szyba", side: 12, top: 12, bottom: 12, opaque: 0, solid: 1, kind: 0, hard: 0.4, tool: 0, drop: 0},
    12: {name: "Ruda wegla", side: 13, top: 13, bottom: 13, opaque: 1, solid: 1, kind: 0, hard: 2.2, tool: 1, drop: 101},
    13: {name: "Ruda zelaza", side: 14, top: 14, bottom: 14, opaque: 1, solid: 1, kind: 0, hard: 2.4, tool: 1, drop: 102},
    14: {name: "Ruda zlota", side: 15, top: 15, bottom: 15, opaque: 1, solid: 1, kind: 0, hard: 2.4, tool: 1, drop: 103},
    15: {name: "Ruda diamentu", side: 16, top: 16, bottom: 16, opaque: 1, solid: 1, kind: 0, hard: 2.6, tool: 1, drop: 104},
    16: {name: "Ruda redstone", side: 17, top: 17, bottom: 17, opaque: 1, solid: 1, kind: 0, hard: 2.4, tool: 1, drop: 105},
    17: {name: "Pien brzozy", side: 18, top: 19, bottom: 19, opaque: 1, solid: 1, kind: 0, hard: 1.2, tool: 2, drop: 17},
    18: {name: "Liscie brzozy", side: 20, top: 20, bottom: 20, opaque: 1, solid: 1, kind: 0, hard: 0.25, tool: 5, drop: 0, leaves: 1},
    19: {name: "Pien swierku", side: 21, top: 22, bottom: 22, opaque: 1, solid: 1, kind: 0, hard: 1.2, tool: 2, drop: 19},
    20: {name: "Liscie swierku", side: 23, top: 23, bottom: 23, opaque: 1, solid: 1, kind: 0, hard: 0.25, tool: 5, drop: 0, leaves: 1},
    21: {name: "Snieg", side: 24, top: 24, bottom: 24, opaque: 1, solid: 1, kind: 0, hard: 0.4, tool: 3, drop: 21},
    22: {name: "Mlecz", side: 25, top: 25, bottom: 25, opaque: 0, solid: 0, kind: 1, hard: 0.1, tool: 0, drop: 22},
    23: {name: "Mak", side: 26, top: 26, bottom: 26, opaque: 0, solid: 0, kind: 1, hard: 0.1, tool: 0, drop: 23},
    24: {name: "Wysoka trawa", side: 27, top: 27, bottom: 27, opaque: 0, solid: 0, kind: 1, hard: 0.1, tool: 0, drop: 0},
    25: {name: "Pochodnia", side: 28, top: 28, bottom: 28, opaque: 0, solid: 0, kind: 2, hard: 0.1, tool: 0, drop: 25},
    26: {name: "Stol rzemieslniczy", side: 30, top: 29, bottom: 11, opaque: 1, solid: 1, kind: 0, hard: 1.2, tool: 2, drop: 26},
    27: {name: "Kaktus", side: 31, top: 32, bottom: 32, opaque: 1, solid: 1, kind: 0, hard: 0.5, tool: 4, drop: 27},
    28: {name: "Zwir", side: 33, top: 33, bottom: 33, opaque: 1, solid: 1, kind: 0, hard: 0.5, tool: 3, drop: 28},
    29: {name: "Obsydian", side: 34, top: 34, bottom: 34, opaque: 1, solid: 1, kind: 0, hard: 12.0, tool: 1, drop: 29},
    30: {name: "Lod", side: 35, top: 35, bottom: 35, opaque: 1, solid: 1, kind: 0, hard: 0.6, tool: 1, drop: 0},
    31: {name: "Biblioteczka", side: 36, top: 11, bottom: 11, opaque: 1, solid: 1, kind: 0, hard: 1.2, tool: 2, drop: 31},
    32: {name: "Dynia", side: 37, top: 38, bottom: 38, opaque: 1, solid: 1, kind: 0, hard: 0.8, tool: 2, drop: 32},
    33: {name: "Arbuz", side: 39, top: 40, bottom: 40, opaque: 1, solid: 1, kind: 0, hard: 0.8, tool: 2, drop: 33},
    34: {name: "Gabka", side: 41, top: 41, bottom: 41, opaque: 1, solid: 1, kind: 0, hard: 0.6, tool: 5, drop: 34},
    35: {name: "Cegly", side: 42, top: 42, bottom: 42, opaque: 1, solid: 1, kind: 0, hard: 2.0, tool: 1, drop: 35}
};
AIMC.AIR_ID = 0;
AIMC.WATER_ID = 7;

AIMC.blockDef = function (id) {
    return AIMC.BLOCKS[id] || null;
};

// Przedmioty: id -> {name, atlas:0=bloki/1=itemy, tile, place, tool}.
AIMC.ITEMS = {};
(function () {
    var id, b;
    for (id in AIMC.BLOCKS) {
        if (!AIMC.BLOCKS.hasOwnProperty(id)) continue;
        id = +id;
        if (id === 0 || id === AIMC.WATER_ID) continue;
        b = AIMC.BLOCKS[id];
        AIMC.ITEMS[id] = {name: b.name, atlas: 0, tile: b.side, place: id, tool: 0};
    }
    function mat(i, n, t) { AIMC.ITEMS[i] = {name: n, atlas: 1, tile: t, place: -1, tool: 0}; }
    mat(100, "Patyk", 0); mat(101, "Wegiel", 1); mat(102, "Surowe zelazo", 2);
    mat(103, "Surowe zloto", 3); mat(104, "Diament", 4); mat(105, "Redstone", 5);
    function tool(i, n, t, k) { AIMC.ITEMS[i] = {name: n, atlas: 1, tile: t, place: -1, tool: k}; }
    tool(110, "Kilof drewniany", 6, 1); tool(111, "Siekiera drewniana", 7, 2);
    tool(112, "Lopata drewniana", 8, 3); tool(113, "Miecz drewniany", 9, 4);
    tool(114, "Motyka drewniana", 10, 5);
    tool(120, "Kilof kamienny", 11, 1); tool(121, "Siekiera kamienna", 12, 2);
    tool(122, "Lopata kamienna", 13, 3); tool(123, "Miecz kamienny", 14, 4);
    tool(124, "Motyka kamienna", 15, 5);
})();

AIMC.itemDef = function (id) {
    return AIMC.ITEMS[id] || null;
};

AIMC.itemName = function (id) {
    var it = AIMC.itemDef(id);
    return it ? it.name : "?";
};

/** Lista creative: bloki po id, potem reszta. */
AIMC.creativeList = function () {
    var blocks = [], other = [], id, it;
    for (id in AIMC.ITEMS) {
        if (!AIMC.ITEMS.hasOwnProperty(id)) continue;
        it = AIMC.ITEMS[id];
        (it.place >= 0 ? blocks : other).push(+id);
    }
    blocks.sort(function (a, b) { return a - b; });
    other.sort(function (a, b) { return a - b; });
    return blocks.concat(other);
};

// Receptury: L=pien, P=deski, C=bruk, S=patyk, W=wegiel.
AIMC.RECIPES = [
    {name: "Deski", shape: ["L"], out: 10, count: 4},
    {name: "Patyki", shape: ["P", "P"], out: 100, count: 4},
    {name: "Stol rzemieslniczy", shape: ["PP", "PP"], out: 26, count: 1},
    {name: "Pochodnie", shape: ["W", "S"], out: 25, count: 4},
    {name: "Kilof drewniany", shape: ["PPP", ".S.", ".S."], out: 110, count: 1},
    {name: "Siekiera drewniana", shape: ["PP.", "PS.", ".S."], out: 111, count: 1},
    {name: "Lopata drewniana", shape: ["P", "S", "S"], out: 112, count: 1},
    {name: "Miecz drewniany", shape: ["P", "P", "S"], out: 113, count: 1},
    {name: "Motyka drewniana", shape: ["PP", ".S", ".S"], out: 114, count: 1},
    {name: "Kilof kamienny", shape: ["CCC", ".S.", ".S."], out: 120, count: 1},
    {name: "Siekiera kamienna", shape: ["CC.", "CS.", ".S."], out: 121, count: 1},
    {name: "Lopata kamienna", shape: ["C", "S", "S"], out: 122, count: 1},
    {name: "Miecz kamienny", shape: ["C", "C", "S"], out: 123, count: 1},
    {name: "Motyka kamienna", shape: ["CC", ".S", ".S"], out: 124, count: 1}
];

AIMC.recipeMatches = function (key, id) {
    if (key === "L") return id === 4 || id === 17 || id === 19;
    if (key === "P") return id === 10;
    if (key === "C") return id === 9;
    if (key === "S") return id === 100;
    if (key === "W") return id === 101;
    return false;
};

/** Dopasowanie siatki 3x3 (tablica 9 liczb, 0=pusto). Zwraca recepture lub null. */
AIMC.matchRecipe = function (grid) {
    var minX = 3, minY = 3, maxX = -1, maxY = -1, x, y;
    for (y = 0; y < 3; y++) {
        for (x = 0; x < 3; x++) {
            if (grid[y * 3 + x] !== 0) {
                if (x < minX) minX = x;
                if (y < minY) minY = y;
                if (x > maxX) maxX = x;
                if (y > maxY) maxY = y;
            }
        }
    }
    if (maxX < 0) return null;
    var w = maxX - minX + 1, h = maxY - minY + 1;
    for (var i = 0; i < AIMC.RECIPES.length; i++) {
        var r = AIMC.RECIPES[i];
        if (r.shape.length !== h || r.shape[0].length !== w) continue;
        var ok = true;
        for (y = 0; y < h && ok; y++) {
            for (x = 0; x < w; x++) {
                var key = r.shape[y].charAt(x);
                var id = grid[(minY + y) * 3 + (minX + x)];
                if (key === ".") {
                    if (id !== 0) { ok = false; break; }
                } else if (!AIMC.recipeMatches(key, id)) {
                    ok = false; break;
                }
            }
        }
        if (ok) return r;
    }
    return null;
};

AIMC.recipeFits2x2 = function (r) {
    return r.shape.length <= 2 && r.shape[0].length <= 2;
};

/** Co wypada z bloku (0 = nic). */
AIMC.dropOf = function (id) {
    switch (id) {
        case 12: return 101; // wegiel
        case 13: return 102; // zelazo
        case 14: return 103; // zloto
        case 15: return 104; // diament
        case 16: return 105; // redstone
        case 3: return 9;    // kamien -> bruk
        case 1: return 2;    // trawa -> ziemia
        case 11: case 21: case 30: return 0; // szyba, snieg, lod
        case 5: case 18: case 20: return 0;  // liscie
        case 22: case 23: case 24: return 0; // rosliny
        default: return id;
    }
};
