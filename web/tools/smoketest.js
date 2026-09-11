/* AiMINECRAFT Web - smoke test calej gry w node (stub DOM + THREE).
 * Uruchamia PRAWDZIWY boot, generowanie swiata, meshowanie, fizyke,
 * kopanie/stawianie, crafting, komendy i zapis. Wykrywa literowki,
 * bledne nazwy metod i wyjatki na sciezkach runtime.
 * Uzycie: node tools/smoketest.js
 */
"use strict";
const fs = require("fs");
const path = require("path");
const vm = require("vm");

const ROOT = path.dirname(__dirname);
let fails = 0;
function ok(cond, name) {
    if (cond) { console.log("OK  " + name); }
    else { fails++; console.log("FAIL " + name); }
}

// ---------- Stuby DOM ----------
function makeElement(tag) {
    const el = {
        tagName: (tag || "div").toUpperCase(),
        children: new Proxy({}, {
            get: (t, p) => (p === "length" ? 0 : makeElement("div"))
        }),
        childNodes: [],
        style: {},
        dataset: {},
        classList: {
            _s: new Set(),
            add(c) { this._s.add(c); },
            remove(c) { this._s.delete(c); },
            contains(c) { return this._s.has(c); }
        },
        _html: "",
        textContent: "",
        value: "",
        disabled: false,
        width: 0,
        height: 0,
        parentNode: null,
        firstChild: null,
        onclick: null,
        oninput: null,
        appendChild(c) { c.parentNode = el; el.childNodes.push(c); return c; },
        removeChild(c) {
            const i = el.childNodes.indexOf(c);
            if (i >= 0) el.childNodes.splice(i, 1);
            return c;
        },
        setAttribute() {},
        addEventListener() {},
        removeEventListener() {},
        querySelector() { return makeElement("div"); },
        querySelectorAll() {
            return new Proxy({length: 2}, {
                get: (t, p) => (p === "length" ? 2 : makeElement("div"))
            });
        },
        focus() {},
        blur() {},
        getContext() {
            return {
                clearRect() {},
                drawImage() {},
                getImageData(x, y, w, h) {
                    return {data: new Uint8ClampedArray(w * h * 4).fill(200)};
                }
            };
        },
        toDataURL() { return "data:image/png;base64,STUB"; },
        requestPointerLock() {}
    };
    Object.defineProperty(el, "innerHTML", {
        get() { return el._html; },
        set(v) { el._html = String(v); }
    });
    return el;
}

class StubImage {
    constructor() {
        this._src = "";
        this.onload = null;
        this.width = 16;
        this.height = 16;
    }
    set src(v) {
        this._src = v;
        setTimeout(() => { if (this.onload) this.onload(); }, 0);
    }
    get src() { return this._src; }
}

const elements = {};
const store = {};
const sandbox = {
    console,
    setTimeout,
    clearTimeout,
    setInterval,
    clearInterval,
    performance,
    Uint8Array,
    Uint8ClampedArray,
    Float32Array,
    Math,
    JSON,
    Promise,
    Proxy,
    Image: StubImage,
    requestAnimationFrame(cb) { sandbox._raf = cb; return 1; },
    _raf: null,
    document: {
        activeElement: null,
        pointerLockElement: null,
        createElement: (t) => makeElement(t),
        getElementById: (id) => {
            if (!elements[id]) elements[id] = makeElement("div");
            return elements[id];
        },
        addEventListener() {},
        removeEventListener() {},
    },
};
// window = globalny obiekt sandboxa
sandbox.window = sandbox;
sandbox.window.localStorage = {
    getItem: (k) => (k in store ? store[k] : null),
    setItem: (k, v) => { store[k] = String(v); },
    removeItem: (k) => { delete store[k]; },
};
sandbox.window.devicePixelRatio = 1;
sandbox.window.innerWidth = 1280;
sandbox.window.innerHeight = 720;
sandbox.window.addEventListener = () => {};
sandbox.window.exitPointerLock = undefined;
sandbox.document.exitPointerLock = () => {};

// ---------- Stub THREE ----------
function vec3() {
    return {x: 0, y: 0, z: 0, set(x, y, z) { this.x = x; this.y = y; this.z = z; }};
}
function stubMesh() {
    return {
        position: vec3(),
        rotation: {x: 0, y: 0, z: 0, order: "XYZ"},
        visible: true,
        renderOrder: 0,
        matrixAutoUpdate: true,
        frustumCulled: true,
        geometry: {dispose() {}},
        material: {},
        lookAt() {},
        updateMatrixWorld() {}
    };
}
sandbox.THREE = {
    NearestFilter: 1,
    DoubleSide: 2,
    Color: function () { return {setRGB() {}}; },
    Fog: function () { return {color: {setRGB() {}}, near: 0, far: 0}; },
    Scene: function () {
        return {
            background: null, fog: null,
            add() {}, remove() {}
        };
    },
    PerspectiveCamera: function () {
        return {
            aspect: 1, fov: 75,
            position: vec3(),
            rotation: {x: 0, y: 0, z: 0, order: "YXZ"},
            updateProjectionMatrix() {}
        };
    },
    WebGLRenderer: function () {
        return {
            domElement: makeElement("canvas"),
            setPixelRatio() {},
            setSize() {},
            render() {}
        };
    },
    HemisphereLight: function () { return {intensity: 0}; },
    DirectionalLight: function () {
        return {
            intensity: 0,
            color: {setRGB() {}},
            position: vec3(),
            target: {position: vec3(), updateMatrixWorld() {}}
        };
    },
    BufferGeometry: function () {
        return {
            attributes: {},
            setAttribute(n, a) { this.attributes[n] = a; },
            setIndex(ix) { this.index = ix; },
            dispose() {}
        };
    },
    Float32BufferAttribute: function (arr) {
        return {array: Float32Array.from(arr), needsUpdate: false};
    },
    Mesh: function (g, m) { const o = stubMesh(); o.geometry = g; o.material = m; return o; },
    Points: function (g, m) { const o = stubMesh(); o.geometry = g; o.material = m; return o; },
    LineSegments: function (g, m) { const o = stubMesh(); o.geometry = g; o.material = m; return o; },
    MeshLambertMaterial: function (o) { return o || {}; },
    MeshBasicMaterial: function (o) { return o || {}; },
    LineBasicMaterial: function (o) { return o || {}; },
    PointsMaterial: function (o) { return o || {}; },
    BoxGeometry: function () { return {}; },
    CircleGeometry: function () { return {}; },
    EdgesGeometry: function () { return {}; },
    CanvasTexture: function () {
        return {magFilter: 0, minFilter: 0, generateMipmaps: false};
    }
};
sandbox.window.THREE = sandbox.THREE;

// ---------- Ladowanie gry ----------
vm.createContext(sandbox);
const bundlePath = process.env.AIMC_BUNDLE || "";
if (bundlePath) {
    // Test gotowej paczki dist (nadpisz prawdziwy THREE stubem do logiki).
    const realThree = sandbox.THREE;
    const src = fs.readFileSync(bundlePath, "utf8");
    vm.runInContext(src, sandbox, {filename: "bundle.js"});
    sandbox.THREE = realThree;
    sandbox.window.THREE = realThree;
    console.log("--- tryb: paczka dist ---");
} else {
    const files = ["config.js", "noise.js", "worldgen.js", "inventory.js", "textures.js",
        "audio.js", "save.js", "raycast.js", "world.js", "player.js",
        "ui.js", "main.js"];
    for (const f of files) {
        const src = fs.readFileSync(path.join(ROOT, "js", f), "utf8");
        vm.runInContext(src, sandbox, {filename: f});
    }
}
ok(!!sandbox.AIMC.Game && !!sandbox.AIMC.UI, "klasy Game i UI zaladowane");

async function waitFor(fn, ms) {
    const t0 = Date.now();
    while (!fn()) {
        if (Date.now() - t0 > (ms || 15000)) return false;
        await new Promise((r) => setTimeout(r, 25));
    }
    return true;
}

(async () => {
    const game = new sandbox.AIMC.Game();
    ok(true, "konstruktor Game");

    // Boot (async).
    const bootP = game.boot();
    ok(await waitFor(() => game.state === "title", 15000), "boot -> title");
    await bootP;
    ok(game.blockAtlas && game.blockAtlas.colors.length === 64, "atlas 64 kolory kafli");
    ok(game.ui.blockAtlasURL.startsWith("data:"), "URL atlasu blokow");

    // Nowa gra.
    game.ui.inpName.value = "Testowy";
    game.ui.inpSeed.value = "smoke123";
    game.newGame();
    ok(await waitFor(() => game.state === "play", 30000), "startWorld -> play");
    ok(game.world.chunkCount() > 10, "chunki wokol spawnu (" + game.world.chunkCount() + ")");

    // Mesh ma wierzcholki?
    let verts = 0;
    for (const k in game.world.chunks) {
        const c = game.world.chunks[k];
        if (c.meshO) verts += c.meshO.geometry.attributes.position.array.length;
    }
    ok(verts > 10000, "meshowanie daje wierzcholki (" + verts + ")");

    // Kilka klatek petli.
    let t = 100;
    for (let i = 0; i < 5; i++) {
        t += 16;
        game.loop(t);
        // loop() rejestruje kolejny rAF - ignorujemy, wywolujemy recznie
    }
    ok(true, "5 klatek petli bez wyjatku");

    // Fizyka: gracz stoi na ziemi po opadzie.
    game.keys = {};
    for (let i = 0; i < 120; i++) {
        t += 16;
        game.loop(t);
    }
    ok(game.player.onGround, "gracz wyladowal (onGround)");
    const py = game.player.y;
    ok(py > 5 && py < 64, "pozycja Y sensowna (" + py.toFixed(1) + ")");

    // Raycast w dol: celuje w blok pod stopami.
    game.player.pitch = -1.5;
    const hit = game.aimBlock();
    ok(hit && hit.y <= Math.floor(game.player.y), "raycast trafia w ziemie (" +
        (hit ? hit.x + "," + hit.y + "," + hit.z : "null") + ")");

    // Kopanie: blok znika, drop w ekwipunku (survival).
    const under = game.world.getBlock(hit.x, hit.y, hit.z);
    ok(under !== 0, "blok pod graczem pelny (id=" + under + ")");
    game.tryBreak();
    ok(game.world.getBlock(hit.x, hit.y, hit.z) === 0, "kopanie usuwa blok");
    let total = 0;
    for (let i = 0; i < 36; i++) {
        const s = game.inv.get(i);
        if (s) total += s.count;
    }
    ok(total > 0, "drop trafil do ekwipunku (" + total + ")");

    // Stawianie: cel w sciane, poloz z reki.
    game.inv.set(0, {id: 9, count: 10});
    game.inv.selected = 0;
    game.player.yaw = 0;
    game.player.pitch = -0.6;
    const before = game.inv.get(0).count;
    game.tryPlace();
    const after = game.inv.get(0) ? game.inv.get(0).count : 0;
    ok(after === before - 1, "stawianie zuzywa blok (" + before + "->" + after + ")");

    // Ekwipunek + crafting 2x2: pien -> deski.
    game.openPanel("inv");
    game.setTab(1);
    game.cursor = {id: 4, count: 1};
    game.craftClick(0, false); // odloz pien do siatki
    ok(game.craft2.result && game.craft2.result.id === 10, "deski w wyniku craftingu");
    const planksBefore = game.inv.add(0, 0); // no-op, tylko pointer
    game.resultClick(false);
    let planks = 0;
    for (let i = 0; i < 36; i++) {
        const s = game.inv.get(i);
        if (s && s.id === 10) planks += s.count;
    }
    ok(planks === 4, "wyjeto 4 deski (" + planks + ")");
    game.closePanels();

    // Komendy.
    game.runCommand("give 25 8");
    let torches = 0;
    for (let i = 0; i < 36; i++) {
        const s = game.inv.get(i);
        if (s && s.id === 25) torches += s.count;
    }
    ok(torches === 8, "/give 25 8 dziala");
    game.runCommand("time night");
    ok(game.dayTime === 14000, "/time night");
    game.runCommand("gamemode 1");
    ok(game.mode === 1 && game.player.flying === false, "/gamemode 1");
    game.runCommand("tp 100 50 100");
    ok(game.player.x === 100 && game.player.y === 50, "/tp");
    game.runCommand("seed");
    game.runCommand("help");
    game.runCommand("nieznana");
    ok(true, "komendy bez wyjatku");

    // F3 debug text.
    const dbg = game.debugText();
    ok(dbg.includes("AiMINECRAFT") && dbg.includes("XYZ"), "tekst F3");

    // Zapis i odczyt.
    game.saveGame(true);
    const raw = store[sandbox.AIMC.SAVE_KEY];
    ok(!!raw, "zapis w localStorage");
    const s = JSON.parse(raw);
    ok(s.name === "Testowy" && s.diff.length > 0 && s.inv.slots.length === 36, "zapis zawiera diff+inv");
    game.continueGame();
    ok(await waitFor(() => game.state === "play", 30000), "wczytanie zapisu -> play");
    ok(game.world.getBlock(hit.x, hit.y, hit.z) === 0, "diff przetrwal zapis/odczyt");

    // Admin fill.
    game.adminAction("fill", ["99 45 99 101 45 101 9"]);
    ok(game.world.getBlock(100, 45, 100) === 9, "admin fill");

    console.log(fails === 0 ? "\nSMOKE: WSZYSTKO OK" : "\nSMOKE BLEDOW: " + fails);
    process.exit(fails === 0 ? 0 : 1);
})().catch((e) => {
    console.error("SMOKE WYJATEK:", e);
    process.exit(1);
});
