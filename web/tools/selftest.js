/* AiMINECRAFT Web - selftest logiki (node tools/selftest.js). */
"use strict";
const AIMC = require("../js/inventory.js");

let fails = 0;
function ok(cond, name) {
    if (cond) { console.log("OK  " + name); }
    else { fails++; console.log("FAIL " + name); }
}

// --- Seedy i RNG ---
ok(AIMC.seedFrom("12345") === 12345, "seed z liczby");
ok(AIMC.seedFrom("") !== 0, "seed z tekstu niezerowy");
ok(AIMC.seedFrom("abc") === AIMC.seedFrom("abc"), "seed deterministyczny");
const r1 = AIMC.makeRandom(7), r2 = AIMC.makeRandom(7);
ok(r1() === r2() && r1() === r2(), "RNG deterministyczny");

// --- Szum ---
const p1 = new AIMC.Perlin(99), p2 = new AIMC.Perlin(99);
ok(p1.noise2(3.5, -8.25) === p2.noise2(3.5, -8.25), "noise2 deterministyczny");
ok(p1.fbm3(1, 2, 3, 2, 2, 0.5) === p2.fbm3(1, 2, 3, 2, 2, 0.5), "fbm3 deterministyczny");
ok(AIMC.hash01(5, 9, 3) === AIMC.hash01(5, 9, 3), "hash01 deterministyczny");
ok(AIMC.hash3(1, 2, 3, 4) !== AIMC.hash3(1, 2, 4, 4), "hash3 rozroznia");

// --- Receptury ---
function grid9(rows) {
    // rows: [["P",...]] nazwy -> id; uproszczone: podajemy id wprost
    return rows;
}
let g = [10, 10, 10, 0, 100, 0, 0, 100, 0]; // kilof drewniany
let rec = AIMC.matchRecipe(g);
ok(rec && rec.out === 110, "kilof drewniany match");
g = [10, 0, 0, 10, 0, 0, 0, 0, 0]; // patyki w kolumnie 0 (przyciete)
rec = AIMC.matchRecipe(g);
ok(rec && rec.out === 100 && rec.count === 4, "patyki match + x4");
g = [4, 0, 0, 0, 0, 0, 0, 0, 0]; // pien -> deski
rec = AIMC.matchRecipe(g);
ok(rec && rec.out === 10 && AIMC.recipeFits2x2(rec), "deski 1x1 + fits2x2");
g = [9, 9, 9, 0, 100, 0, 0, 100, 0]; // kilof kamienny
rec = AIMC.matchRecipe(g);
ok(rec && rec.out === 120 && !AIMC.recipeFits2x2(rec), "kilof kamienny + nie fits2x2");
ok(AIMC.matchRecipe([0,0,0,0,0,0,0,0,0]) === null, "pusta siatka = null");
ok(AIMC.matchRecipe([3,0,0,0,0,0,0,0,0]) === null, "kamien sam = null");
ok(AIMC.recipeMatches("L", 17) && !AIMC.recipeMatches("L", 10), "L = tylko pnie");

// --- Ekwipunek ---
const inv = new AIMC.Inventory();
ok(inv.add(10, 70) === 0 && inv.get(0).count === 64 && inv.get(1).count === 6, "stackowanie 64+6");
inv.set(0, null); inv.set(1, null);
inv.add(100, 4);
let cur = AIMC.clickSlot(inv.slots, 0, null, false); // podnies
ok(cur && cur.count === 4 && inv.get(0) === null, "podniesienie stosu");
cur = AIMC.clickSlot(inv.slots, 5, cur, true); // poloz 1 PPM
ok(inv.get(5).count === 1 && cur.count === 3, "PPM kladzie 1");
cur = AIMC.clickSlot(inv.slots, 6, null, false);
ok(cur === null, "klik w pusty pustym = null");
inv.set(2, {id: 101, count: 10});
cur = AIMC.clickSlot(inv.slots, 2, null, true); // wez polowe
ok(cur.count === 5 && inv.get(2).count === 5, "PPM bierze polowe");
inv.selected = 5;
ok(inv.consumeHeld() && inv.get(5) === null, "consumeHeld zjada ostatni");

// --- Generator ---
const gen = new AIMC.WorldGen(AIMC.seedFrom("test123"));
const gen2 = new AIMC.WorldGen(AIMC.seedFrom("test123"));
ok(gen.getHeight(10, -20) === gen2.getHeight(10, -20), "getHeight deterministyczny");
const sp = gen.findSpawn();
ok(gen.getHeight(sp[0], sp[1]) > AIMC.SEA + 1, "spawn na ladzie");
// Roznorodnosc biomow na obszarze.
const seen = {};
for (let x = -160; x <= 160; x += 8) {
    for (let z = -160; z <= 160; z += 8) {
        seen[gen.getBiome(x, z)] = true;
    }
}
const nBiomes = Object.keys(seen).length;
ok(nBiomes >= 4, "biomy roznorodne (" + nBiomes + " z 6)");
// Chunk: determinizm + zawartosc.
const c1 = gen.generate(0, 0), c2 = gen2.generate(0, 0);
let same = c1.length === c2.length;
for (let i = 0; i < c1.length && same; i++) if (c1[i] !== c2[i]) same = false;
ok(same, "chunk deterministyczny");
let counts = {};
for (let i = 0; i < c1.length; i++) counts[c1[i]] = (counts[c1[i]] || 0) + 1;
ok(counts[6] === 256, "bedrock na dnie calego chunka");
ok((counts[3] || 0) > 1000, "kamien dominuje (" + (counts[3] || 0) + ")");
// Rudy gdzies w okolicy (kilka chunkow).
let ores = 0, water = 0, trees = 0, caves = 0;
for (let cx = -2; cx <= 2; cx++) {
    for (let cz = -2; cz <= 2; cz++) {
        const d = gen.generate(cx, cz);
        for (let i = 0; i < d.length; i++) {
            const id = d[i];
            if (id >= 12 && id <= 16) ores++;
            if (id === 7) water++;
            if (id === 4 || id === 17 || id === 19) trees++;
        }
    }
}
// Jaskinie: powietrze gleboko pod ziemia (skan obszaru, jaskinie sa rzadkie i w kupach).
let airDeep = 0;
for (let ccx = -4; ccx <= 4; ccx++) {
    for (let ccz = -4; ccz <= 4; ccz++) {
        const dm = gen.generate(ccx, ccz);
        for (let lx = 0; lx < 16; lx++) for (let lz = 0; lz < 16; lz++) {
            const h = gen.getHeight(ccx * 16 + lx, ccz * 16 + lz);
            for (let y = 4; y < h - 4; y++) if (dm[(y << 8) | (lz << 4) | lx] === 0) airDeep++;
        }
    }
}
ok(ores > 20, "rudy istnieja (" + ores + ")");
ok(water > 0, "woda istnieje (" + water + ")");
ok(trees > 0, "drzewa istnieja (" + trees + " pni)");
ok(airDeep > 0, "jaskinie istnieja (" + airDeep + " powietrza gleboko)");
// Wysokosci w zakresie.
let hOk = true;
for (let x = -64; x < 64 && hOk; x += 4) {
    for (let z = -64; z < 64 && hOk; z += 4) {
        const h = gen.getHeight(x, z);
        if (h < 6 || h > AIMC.HEIGHT - 16) hOk = false;
    }
}
ok(hOk, "wysokosci w zakresie 6..48");

// --- Creative / itemy ---
ok(AIMC.creativeList().length === 34 + 16, "creative ma 50 pozycji");
ok(AIMC.itemDef(7) === null, "wody nie ma w itemach");
ok(AIMC.itemDef(110).tool === 1 && AIMC.itemDef(26).place === 26, "narzedzie i blok-place");

console.log(fails === 0 ? "\nWSZYSTKO OK" : "\nBLEDOW: " + fails);
process.exit(fails === 0 ? 0 : 1);
