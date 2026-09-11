/* AiMINECRAFT Web - gra: petla, niebo, input, komendy, zapis. */
"use strict";

var AIMC = (typeof window !== "undefined")
    ? (window.AIMC || (window.AIMC = {}))
    : {};
if (typeof module !== "undefined" && module.exports) module.exports = AIMC;

AIMC.Game = function () {
    this.state = "boot";       // boot | title | loading | play
    this.panel = null;         // null | inv | craft
    this.tab = 0;
    this.cursor = null;
    this.craft2 = {grid: [null, null, null, null], result: null};
    this.craft3 = {grid: [null, null, null, null, null, null, null, null, null], result: null};
    this.creative = AIMC.creativeList();

    this.settings = {renderDist: 2, sensitivity: 1.0, fov: 75, clouds: true, sounds: true};
    this.setFrom = "title";
    this.helpFrom = "title";
    this.newMode = 0;

    this.worldName = "";
    this.seed = 0;
    this.mode = 0;
    this.dayTime = 2000;
    this.cycle = true;
    this.spawn = {x: 0.5, y: 40, z: 0.5};

    this.world = null;
    this.inv = new AIMC.Inventory();
    this.player = new AIMC.Player();
    this.audio = new AIMC.Audio();
    this.ui = null;

    this.renderer = null;
    this.scene = null;
    this.camera = null;
    this.sunLight = null;
    this.hemi = null;
    this.selBox = null;
    this.stars = null;
    this.sunMesh = null;
    this.moonMesh = null;
    this.cloudMeshes = [];
    this.blockAtlas = null;

    this.keys = {};
    this.lmb = false;
    this.rmb = false;
    this.breakCd = 0;
    this.placeCd = 0;
    this.showDebug = false;
    this.fps = 60;
    this.lastT = 0;
    this.lastSpace = 0;
    this.lastWUp = 0;
    this.sprintLock = false;
    this.debugCd = 0;
    this.autosaveCd = 60;

    // Particles.
    this.particles = [];
    this.pGeo = null;
    this.pPts = null;
    this.PMAX = 240;
};

AIMC.Game.prototype.boot = async function () {
    this.ui = new AIMC.UI(this);
    this.loadSettings();
    this.ui.showLoading(0.05, "Ladowanie tekstur...");
    await this.tick(30);

    var THREE = window.THREE;
    var blockImgs = await AIMC.loadImages(AIMC.TEX_BLOCKS);
    this.ui.showLoading(0.3, "Atlas blokow...");
    await this.tick(30);
    var itemImgs = await AIMC.loadImages(AIMC.TEX_ITEMS);
    this.blockAtlas = AIMC.buildAtlas(THREE, blockImgs, 8);
    var itemAtlas = AIMC.buildAtlas(THREE, itemImgs, 4);
    this.ui.setAtlases(this.blockAtlas.url, itemAtlas.url);
    this.ui.showLoading(0.5, "Silnik 3D...");
    await this.tick(30);

    this.initThree(THREE, itemAtlas);
    this.bindInput();
    this.ui.showLoading(1.0, "Gotowe!");
    await this.tick(200);
    this.state = "title";
    this.refreshTitle();
    var self = this;
    requestAnimationFrame(function (t) { self.loop(t); });
};

AIMC.Game.prototype.tick = function (ms) {
    return new Promise(function (res) { setTimeout(res, ms); });
};

AIMC.Game.prototype.initThree = function (THREE, itemAtlas) {
    var container = document.getElementById("game");
    this.renderer = new THREE.WebGLRenderer({antialias: true});
    this.renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, 2));
    this.renderer.setSize(window.innerWidth, window.innerHeight);
    container.appendChild(this.renderer.domElement);
    this.scene = new THREE.Scene();
    this.scene.background = new THREE.Color(0x87ceeb);
    this.scene.fog = new THREE.Fog(0x87ceeb, 16, 48);
    this.camera = new THREE.PerspectiveCamera(this.settings.fov,
        window.innerWidth / window.innerHeight, 0.1, 1000);
    this.camera.rotation.order = "YXZ";

    this.hemi = new THREE.HemisphereLight(0xbdd7ff, 0x6b5b3e, 0.9);
    this.scene.add(this.hemi);
    this.sunLight = new THREE.DirectionalLight(0xffffff, 0.8);
    this.scene.add(this.sunLight);
    this.scene.add(this.sunLight.target);

    // Ramka zaznaczenia.
    var edges = new THREE.EdgesGeometry(new THREE.BoxGeometry(1.002, 1.002, 1.002));
    this.selBox = new THREE.LineSegments(edges,
        new THREE.LineBasicMaterial({color: 0x000000, transparent: true, opacity: 0.7}));
    this.selBox.visible = false;
    this.scene.add(this.selBox);

    // Gwiazdy.
    var starPos = [];
    for (var i = 0; i < 350; i++) {
        var th = Math.random() * Math.PI * 2, ph = Math.random() * Math.PI * 0.48;
        var R = 400;
        starPos.push(R * Math.cos(th) * Math.cos(ph), R * Math.sin(ph) + 10, R * Math.sin(th) * Math.cos(ph));
    }
    var sg = new THREE.BufferGeometry();
    sg.setAttribute("position", new THREE.Float32BufferAttribute(starPos, 3));
    this.stars = new THREE.Points(sg, new THREE.PointsMaterial({
        color: 0xffffff, size: 1.6, sizeAttenuation: false, transparent: true, opacity: 0, fog: false
    }));
    this.stars.frustumCulled = false;
    this.scene.add(this.stars);

    // Slonce i ksiezyc.
    this.sunMesh = new THREE.Mesh(new THREE.CircleGeometry(18, 24),
        new THREE.MeshBasicMaterial({color: 0xffe27a, fog: false}));
    this.moonMesh = new THREE.Mesh(new THREE.CircleGeometry(12, 24),
        new THREE.MeshBasicMaterial({color: 0xdfe6f5, fog: false}));
    this.scene.add(this.sunMesh);
    this.scene.add(this.moonMesh);

    // Chmury.
    var cmat = new THREE.MeshBasicMaterial({color: 0xffffff, transparent: true, opacity: 0.55});
    var cgeo = new THREE.BoxGeometry(14, 1.5, 14);
    for (var c = 0; c < 26; c++) {
        var m = new THREE.Mesh(cgeo, cmat);
        m.position.set((Math.random() - 0.5) * 220, AIMC.HEIGHT + 8 + Math.random() * 4,
            (Math.random() - 0.5) * 220);
        m.visible = this.settings.clouds;
        this.scene.add(m);
        this.cloudMeshes.push(m);
    }

    // Particles (kopanie).
    this.pGeo = new THREE.BufferGeometry();
    this.pGeo.setAttribute("position", new THREE.Float32BufferAttribute(new Array(this.PMAX * 3).fill(0), 3));
    this.pGeo.setAttribute("color", new THREE.Float32BufferAttribute(new Array(this.PMAX * 3).fill(1), 3));
    this.pPts = new THREE.Points(this.pGeo, new THREE.PointsMaterial({
        size: 0.14, vertexColors: true, transparent: true, opacity: 0.95
    }));
    this.pPts.frustumCulled = false;
    this.pPts.visible = false;
    this.scene.add(this.pPts);

    var self = this;
    window.addEventListener("resize", function () {
        self.camera.aspect = window.innerWidth / window.innerHeight;
        self.camera.updateProjectionMatrix();
        self.renderer.setSize(window.innerWidth, window.innerHeight);
    });
};

AIMC.Game.prototype.bindInput = function () {
    var self = this;
    var canvas = this.renderer.domElement;

    document.addEventListener("keydown", function (e) {
        if (e.code === "F3") {
            e.preventDefault();
            if (self.state === "play") {
                self.showDebug = !self.showDebug;
                if (!self.showDebug) self.ui.setDebug(null);
            }
            return;
        }
        var tag = (document.activeElement && document.activeElement.tagName) || "";
        var typing = tag === "INPUT" || tag === "TEXTAREA" || self.ui.chatOpen;
        if (typing) return;
        // Blokuj domyslne akcje przegladarki w grze (scroll, quick-find, klik spacja).
        if (self.state === "play" && (e.code === "Space" || e.code === "Slash" ||
            e.code === "ArrowUp" || e.code === "ArrowDown" ||
            e.code === "ArrowLeft" || e.code === "ArrowRight")) {
            e.preventDefault();
        }
        self.keys[e.code] = true;
        // Sprint: podwojne W (Ctrl+W zamyka karte w przegladarce!).
        if (e.code === "KeyW" && !e.repeat && self.state === "play") {
            var nowW = performance.now();
            if (nowW - self.lastWUp < 280) self.sprintLock = true;
        }

        if (self.state !== "play") return;
        if (e.code === "Escape") { self.onEscape(); return; }
        if (self.panel === "inv" || self.panel === "craft") {
            if (e.code === "KeyE") self.closePanels();
            return;
        }
        switch (e.code) {
            case "KeyE": self.openPanel("inv"); break;
            case "KeyT": self.openChat(); break;
            case "Slash": self.openChat("/"); break;
            case "Digit1": case "Digit2": case "Digit3": case "Digit4":
            case "Digit5": case "Digit6": case "Digit7": case "Digit8":
            case "Digit9":
                self.hotbarSelect(parseInt(e.code.slice(5), 10) - 1);
                break;
            case "Space": {
                var now = performance.now();
                if (now - self.lastSpace < 300 && self.mode === 1) {
                    self.player.flying = !self.player.flying;
                    self.ui.toastShow(self.player.flying ? "Latanie: ON" : "Latanie: OFF");
                }
                self.lastSpace = now;
                break;
            }
            case "KeyF":
                if (self.mode === 1) {
                    self.player.flying = !self.player.flying;
                    self.ui.toastShow(self.player.flying ? "Latanie: ON" : "Latanie: OFF");
                }
                break;
        }
    });
    document.addEventListener("keyup", function (e) {
        self.keys[e.code] = false;
        if (e.code === "KeyW") {
            self.sprintLock = false;
            self.lastWUp = performance.now();
        }
    });
    window.addEventListener("blur", function () {
        self.keys = {};
        self.lmb = false;
        self.rmb = false;
    });

    canvas.addEventListener("mousedown", function (e) {
        self.audio.unlock();
        self.audio.resume();
        if (self.state !== "play" || self.panel || self.ui.chatOpen) return;
        if (document.pointerLockElement !== canvas) {
            canvas.requestPointerLock();
            return;
        }
        if (e.button === 0) { self.lmb = true; self.breakCd = 0; }
        if (e.button === 2) { self.rmb = true; self.placeCd = 0; }
    });
    document.addEventListener("mouseup", function (e) {
        if (e.button === 0) self.lmb = false;
        if (e.button === 2) self.rmb = false;
    });
    document.addEventListener("mousemove", function (e) {
        if (document.pointerLockElement !== canvas) return;
        if (self.state !== "play" || self.panel || self.ui.chatOpen) return;
        self.player.look(e.movementX, e.movementY, 0.0022 * self.settings.sensitivity);
    });
    document.addEventListener("wheel", function (e) {
        if (self.state !== "play" || self.panel || self.ui.chatOpen) return;
        var d = e.deltaY > 0 ? 1 : -1;
        self.hotbarSelect((self.inv.selected + d + 9) % 9);
    }, {passive: true});
    document.addEventListener("pointerlockchange", function () {
        if (document.pointerLockElement !== canvas &&
            self.state === "play" && !self.panel && !self.ui.chatOpen && !self.stayOpen) {
            self.openPause();
        }
        self.stayOpen = false;
    });
};

AIMC.Game.prototype.requestLock = function () {
    var canvas = this.renderer.domElement;
    try {
        if (document.pointerLockElement !== canvas) canvas.requestPointerLock();
    } catch (e) {}
};

AIMC.Game.prototype.releaseLock = function () {
    try {
        if (document.exitPointerLock) document.exitPointerLock();
    } catch (e) {}
};

// ---------- Ekrany ----------

AIMC.Game.prototype.refreshTitle = function () {
    var raw = AIMC.storeGet(AIMC.SAVE_KEY);
    var has = !!raw, info = "";
    if (has) {
        try {
            var s = JSON.parse(raw);
            info = "Zapis: " + s.name + " (" + (s.mode === 1 ? "Creative" : "Survival") +
                ", seed " + s.seed + ")";
        } catch (e) { has = false; }
    }
    this.ui.showTitle(has, info, this.newMode);
};

AIMC.Game.prototype.setNewMode = function (m) {
    this.newMode = m;
    this.audio.play("click");
    this.refreshTitle();
};

AIMC.Game.prototype.newGame = function () {
    this.audio.unlock();
    this.audio.play("click");
    this.worldName = (this.ui.inpName.value || "Nowy Swiat").slice(0, 24);
    var seedText = this.ui.inpSeed.value || "";
    this.seed = AIMC.seedFrom(seedText || String((Math.random() * 1e9) | 0));
    this.mode = this.newMode;
    this.dayTime = 2000;
    this.cycle = true;
    this.startWorld(null);
};

AIMC.Game.prototype.continueGame = function () {
    this.audio.unlock();
    this.audio.play("click");
    var raw = AIMC.storeGet(AIMC.SAVE_KEY);
    if (!raw) return;
    var s;
    try { s = JSON.parse(raw); } catch (e) { return; }
    this.worldName = s.name;
    this.seed = s.seed >>> 0;
    this.mode = s.mode;
    this.dayTime = s.time;
    this.cycle = s.cycle !== false;
    this.startWorld(s);
};

AIMC.Game.prototype.deleteSave = function () {
    AIMC.storeDel(AIMC.SAVE_KEY);
    this.audio.play("break");
    this.refreshTitle();
    this.ui.toastShow("Usunieto zapis.");
};

AIMC.Game.prototype.startWorld = async function (save) {
    this.state = "loading";
    this.panel = null;
    this.ui.closeChat();
    this.ui.showLoading(0.1, "Generowanie swiata...");
    await this.tick(30);

    if (this.world) this.world.dispose();
    this.world = new AIMC.World(window.THREE, this.scene, this.blockAtlas.texture,
        this.seed, this.settings.renderDist);
    this.inv = new AIMC.Inventory();
    this.player = new AIMC.Player();
    this.cursor = null;
    this.craft2 = {grid: [null, null, null, null], result: null};
    this.craft3 = {grid: [null, null, null, null, null, null, null, null, null], result: null};
    this.particles = [];

    var sp = this.world.gen.findSpawn();
    var sy = this.world.gen.getHeight(sp[0], sp[1]);
    this.spawn = {x: sp[0] + 0.5, y: sy + 2, z: sp[1] + 0.5};
    this.player.x = this.spawn.x;
    this.player.y = this.spawn.y;
    this.player.z = this.spawn.z;

    if (save) {
        if (save.diff) this.world.diff.load(save.diff);
        if (save.player) this.player.load(save.player);
        if (save.inv) {
            for (var i = 0; i < save.inv.slots.length && i < 36; i++) {
                var st = save.inv.slots[i];
                this.inv.slots[i] = st ? {id: st[0], count: st[1]} : null;
            }
            this.inv.selected = save.inv.selected || 0;
        }
        if (save.spawn) this.spawn = save.spawn;
    }

    // Wstepne chunki wokol spawnu (partiami, zeby pasek zyl).
    this.ui.showLoading(0.3, "Teren...");
    await this.tick(30);
    var pcx = Math.floor(this.player.x / 16), pcz = Math.floor(this.player.z / 16);
    this.world.updateStreaming(pcx, pcz);
    var guard = 0;
    while (this.world.dirty.length > 0 && guard++ < 400) {
        this.world.updateMeshes(6);
        if (guard % 10 === 0) {
            this.ui.showLoading(0.3 + 0.6 * (1 - this.world.dirty.length / 200), "Teren...");
            await this.tick(0);
        }
    }
    this.ui.renderHotbar(this.inv);
    this.ui.chatAdd("Witaj w " + this.worldName + "! Seed: " + this.seed);
    this.ui.chatAdd("Wpisz /help po liste komend.");
    this.state = "play";
    this.ui.showHud();
    this.requestLock();
};

AIMC.Game.prototype.resume = function () {
    if (this.state !== "play") return;
    this.audio.play("click");
    this.closePanels();
    this.ui.closeChat();
    this.ui.showHud();
    this.ui.renderHotbar(this.inv);
    this.requestLock();
};

AIMC.Game.prototype.openPause = function () {
    if (this.state !== "play") return;
    this.lmb = false;
    this.rmb = false;
    this.ui.showPause();
};

AIMC.Game.prototype.openPlayMenu = function () {
    this.audio.play("click");
    var info = this.worldName + " | " + (this.mode === 1 ? "Creative" : "Survival") +
        " | seed " + this.seed;
    this.ui.showPlayMenu(info);
};

AIMC.Game.prototype.openSettings = function (from) {
    this.audio.play("click");
    this.setFrom = from;
    this.ui.showSettings(this.settings);
};

AIMC.Game.prototype.settingsBack = function () {
    this.audio.play("click");
    this.saveSettings();
    if (this.setFrom === "title" || this.state !== "play") this.refreshTitle();
    else this.openPlayMenu();
};

AIMC.Game.prototype.openHelp = function (from) {
    this.audio.play("click");
    this.helpFrom = from;
    this.ui.showHelp();
};

AIMC.Game.prototype.helpBack = function () {
    this.audio.play("click");
    if (this.helpFrom === "title" || this.state !== "play") this.refreshTitle();
    else this.openPlayMenu();
};

AIMC.Game.prototype.openAdmin = function () {
    this.audio.play("click");
    this.stayOpen = true;
    this.releaseLock();
    this.ui.showAdmin(this.cycle);
};

AIMC.Game.prototype.onEscape = function () {
    if (this.panel) { this.closePanels(); this.resume(); return; }
    if (this.ui.chatOpen) { this.closeChat(); return; }
    // ESC przy locku i tak zdejmuje lock -> pointerlockchange otworzy pauze.
};

AIMC.Game.prototype.toTitle = function () {
    this.audio.play("click");
    this.saveGame(true);
    this.state = "title";
    this.releaseLock();
    this.refreshTitle();
};

// ---------- Ustawienia ----------

AIMC.Game.prototype.loadSettings = function () {
    try {
        var raw = AIMC.storeGet(AIMC.SETTINGS_KEY);
        if (raw) {
            var s = JSON.parse(raw);
            for (var k in s) {
                if (this.settings.hasOwnProperty(k)) this.settings[k] = s[k];
            }
        }
    } catch (e) {}
    this.applySettings();
};

AIMC.Game.prototype.saveSettings = function () {
    try { AIMC.storeSet(AIMC.SETTINGS_KEY, JSON.stringify(this.settings)); } catch (e) {}
};

AIMC.Game.prototype.setSetting = function (key, value) {
    if (key === "renderDist") value = Math.round(value);
    this.settings[key] = value;
    this.applySettings();
    this.ui.refreshSettings(this.settings);
};

AIMC.Game.prototype.toggleSetting = function (key) {
    this.settings[key] = !this.settings[key];
    this.audio.play("click");
    this.applySettings();
    this.ui.refreshSettings(this.settings);
    this.saveSettings();
};

AIMC.Game.prototype.applySettings = function () {
    if (this.camera) {
        this.camera.fov = this.settings.fov;
        this.camera.updateProjectionMatrix();
    }
    this.audio.enabled = this.settings.sounds;
    for (var i = 0; i < this.cloudMeshes.length; i++) {
        this.cloudMeshes[i].visible = this.settings.clouds;
    }
    if (this.world && this.world.renderDist !== this.settings.renderDist) {
        this.world.renderDist = this.settings.renderDist;
        this.world.centerCx = 1e9; // wymus streaming
    }
};

// ---------- Zapis ----------

AIMC.Game.prototype.saveGame = function (silent) {
    if (this.state !== "play" || !this.world) return;
    var slots = [];
    for (var i = 0; i < 36; i++) {
        var s = this.inv.slots[i];
        slots.push(s ? [s.id, s.count] : null);
    }
    var data = {
        version: 1, name: this.worldName, seed: this.seed, mode: this.mode,
        time: Math.round(this.dayTime), cycle: this.cycle,
        player: this.player.serialize(),
        inv: {slots: slots, selected: this.inv.selected},
        diff: this.world.diff.serialize(),
        spawn: this.spawn
    };
    try {
        AIMC.storeSet(AIMC.SAVE_KEY, JSON.stringify(data));
        if (!silent) {
            this.audio.play("level");
            this.ui.toastShow("Zapisano swiat.");
        }
    } catch (e) {
        if (!silent) this.ui.toastShow("BLAD zapisu!");
    }
};

// ---------- Panele ----------

AIMC.Game.prototype.openPanel = function (which) {
    this.panel = which;
    this.lmb = false;
    this.rmb = false;
    this.stayOpen = true;
    this.releaseLock();
    this.audio.play("click");
    this.refreshPanels();
};

AIMC.Game.prototype.closePanels = function () {
    // Zwroc siatke craftingu do ekwipunku? Nie - zostaw w siatce (prosciej).
    this.panel = null;
    this.ui.renderCursor(null);
    this.ui.cursorEl.classList.add("hidden");
};

AIMC.Game.prototype.refreshPanels = function () {
    if (this.panel === "inv") {
        this.recomputeCraft(this.craft2, true);
        this.ui.renderInventory(this.inv, this.tab, this.cursor, this.craft2, this.creative);
    } else if (this.panel === "craft") {
        this.recomputeCraft(this.craft3, false);
        this.ui.renderCrafting(this.inv, this.cursor, this.craft3);
    }
    this.ui.renderHotbar(this.inv);
};

AIMC.Game.prototype.setTab = function (t) {
    this.tab = t;
    this.audio.play("click");
    this.refreshPanels();
};

AIMC.Game.prototype.invClick = function (i, right) {
    var self = this;
    this.cursor = AIMC.clickSlot(this.inv.slots, i, this.cursor, right, function () {
        self.audio.play("click");
    });
    this.refreshPanels();
};

AIMC.Game.prototype.craftClick = function (i, right) {
    var self = this;
    var grid = this.panel === "craft" ? this.craft3.grid : this.craft2.grid;
    this.cursor = AIMC.clickSlot(grid, i, this.cursor, right, function () {
        self.audio.play("click");
    });
    this.refreshPanels();
};

AIMC.Game.prototype.gridTo9 = function (grid, small) {
    function id(s) { return s ? s.id : 0; }
    if (!small) return grid.map(id);
    return [id(grid[0]), id(grid[1]), 0, id(grid[2]), id(grid[3]), 0, 0, 0, 0];
};

AIMC.Game.prototype.recomputeCraft = function (craft, small) {
    var rec = AIMC.matchRecipe(this.gridTo9(craft.grid, small));
    if (rec && (small ? AIMC.recipeFits2x2(rec) : true)) {
        craft.result = {id: rec.out, count: rec.count};
        craft.recipe = rec;
    } else {
        craft.result = null;
        craft.recipe = null;
    }
};

AIMC.Game.prototype.resultClick = function (right) {
    var craft = this.panel === "craft" ? this.craft3 : this.craft2;
    var small = this.panel !== "craft";
    this.recomputeCraft(craft, small);
    if (!craft.result || !craft.recipe) return;
    // Sprawdz miejsce.
    var test = new AIMC.Inventory();
    for (var i = 0; i < 36; i++) {
        var s = this.inv.slots[i];
        test.slots[i] = s ? {id: s.id, count: s.count} : null;
    }
    var left = test.add(craft.result.id, craft.result.count);
    if (left > 0) {
        this.ui.toastShow("Brak miejsca w ekwipunku!");
        return;
    }
    this.inv.add(craft.result.id, craft.result.count);
    if (this.mode === 0) {
        for (var g = 0; g < craft.grid.length; g++) {
            var st = craft.grid[g];
            if (st) {
                st.count--;
                if (st.count <= 0) craft.grid[g] = null;
            }
        }
    }
    this.audio.play("pickup");
    this.refreshPanels();
};

AIMC.Game.prototype.creativeClick = function (id, right) {
    this.cursor = {id: id, count: 64};
    this.audio.play("click");
    this.refreshPanels();
};

AIMC.Game.prototype.hotbarSelect = function (i) {
    if (this.inv.selected === i) return;
    this.inv.selected = i;
    this.ui.renderHotbar(this.inv);
};

// ---------- Czat ----------

AIMC.Game.prototype.openChat = function (prefix) {
    if (this.panel) return;
    this.lmb = false;
    this.rmb = false;
    this.stayOpen = true;
    this.releaseLock();
    this.ui.openChat();
    if (prefix) this.ui.chatinput.value = prefix;
};

AIMC.Game.prototype.closeChat = function () {
    this.ui.closeChat();
    this.resume();
};

AIMC.Game.prototype.chatSubmit = function (text) {
    text = (text || "").trim();
    this.ui.chatinput.value = "";
    this.ui.closeChat();
    if (!text) { this.resume(); return; }
    if (text[0] === "/") this.runCommand(text.slice(1));
    else this.ui.chatAdd("[Ty] " + text);
    this.resume();
};

AIMC.Game.prototype.runCommand = function (line) {
    var parts = line.trim().split(/\s+/);
    var cmd = (parts[0] || "").toLowerCase();
    var args = parts.slice(1);
    var self = this;
    function num(v, dflt) {
        var n = parseFloat(v);
        return isNaN(n) ? dflt : n;
    }
    switch (cmd) {
        case "help":
            this.ui.chatAdd("Komendy: /give /tp /time /gamemode /clear /seed /spawn /admin");
            break;
        case "seed":
            this.ui.chatAdd("Seed: " + this.seed);
            break;
        case "give": {
            var id = parseInt(args[0], 10) || 0;
            var n = Math.max(1, parseInt(args[1], 10) || 1);
            if (!AIMC.itemDef(id)) { this.ui.chatAdd("Zly id przedmiotu."); break; }
            var left = 0;
            while (n > 0) {
                var take = Math.min(64, n);
                left += this.inv.add(id, take);
                n -= take;
            }
            this.audio.play("pickup");
            this.ui.renderHotbar(this.inv);
            this.ui.chatAdd(left > 0 ? "Ekwipunek pelny!" : "Dano przedmiot " + id + ".");
            break;
        }
        case "tp": {
            var x = num(args[0], this.player.x), y = num(args[1], this.player.y + 1), z = num(args[2], this.player.z);
            this.player.x = x; this.player.y = y; this.player.z = z;
            this.player.vx = 0; this.player.vy = 0; this.player.vz = 0;
            this.ui.chatAdd("Teleport: " + Math.round(x) + " " + Math.round(y) + " " + Math.round(z));
            break;
        }
        case "spawn":
            this.player.x = this.spawn.x; this.player.y = this.spawn.y; this.player.z = this.spawn.z;
            this.ui.chatAdd("Wrociles na spawn.");
            break;
        case "time": {
            var t = args[0] || "";
            if (t === "day") this.dayTime = 1000;
            else if (t === "night") this.dayTime = 14000;
            else this.dayTime = ((parseInt(t, 10) || 0) % 24000 + 24000) % 24000;
            this.ui.chatAdd("Czas: " + Math.round(this.dayTime));
            break;
        }
        case "gamemode": case "gm": {
            var m = parseInt(args[0], 10);
            if (m === 0 || m === 1) {
                this.mode = m;
                if (m === 0) this.player.flying = false;
                this.ui.chatAdd("Tryb: " + (m === 1 ? "Creative" : "Survival"));
            } else this.ui.chatAdd("Uzyj: /gamemode 0|1");
            break;
        }
        case "clear":
            this.inv.clear();
            this.ui.renderHotbar(this.inv);
            this.ui.chatAdd("Wyczyszczono ekwipunek.");
            break;
        case "admin":
            this.openAdmin();
            break;
        default:
            this.ui.chatAdd("Nieznana komenda. /help");
    }
};

AIMC.Game.prototype.adminAction = function (name, v) {
    function num(x, d) {
        var n = parseFloat(x);
        return isNaN(n) ? d : n;
    }
    if (name === "give") {
        this.ui.closeChat();
        this.runCommand("give " + (v[0] || "") + " " + (v[1] || ""));
        this.refreshPanels();
    } else if (name === "tp") {
        this.runCommand("tp " + (v[0] || "") + " " + (v[1] || "") + " " + (v[2] || ""));
    } else if (name === "time") {
        this.runCommand("time " + (v[0] || ""));
    } else if (name === "gamemode") {
        this.runCommand("gamemode " + (v[0] || ""));
    } else if (name === "clear") {
        this.runCommand("clear");
    } else if (name === "cycle") {
        this.cycle = !this.cycle;
        this.ui.showAdmin(this.cycle);
        this.ui.chatAdd("Cykl dnia: " + (this.cycle ? "ON" : "OFF"));
    } else if (name === "fill") {
        var p = (v[0] || "").trim().split(/\s+/).map(Number);
        if (p.length < 7 || p.some(isNaN)) {
            this.ui.chatAdd("Format: x1 y1 z1 x2 y2 z2 id");
            return;
        }
        var id = Math.round(p[6]);
        if (!AIMC.blockDef(id)) { this.ui.chatAdd("Zly id bloku."); return; }
        var x1 = Math.min(p[0], p[3]) | 0, x2 = Math.max(p[0], p[3]) | 0;
        var y1 = Math.max(0, Math.min(p[1], p[4]) | 0), y2 = Math.min(AIMC.HEIGHT - 1, Math.max(p[1], p[4]) | 0);
        var z1 = Math.min(p[2], p[5]) | 0, z2 = Math.max(p[2], p[5]) | 0;
        var vol = (x2 - x1 + 1) * (y2 - y1 + 1) * (z2 - z1 + 1);
        if (vol > 20000) { this.ui.chatAdd("Za duzy obszar (max 20000)."); return; }
        for (var x = x1; x <= x2; x++) {
            for (var y = y1; y <= y2; y++) {
                for (var z = z1; z <= z2; z++) {
                    this.world.setBlock(x, y, z, id, true);
                }
            }
        }
        this.audio.play("place");
        this.ui.chatAdd("Wypelniono " + vol + " blokow.");
    }
};

// ---------- Kopanie / stawianie ----------

AIMC.Game.prototype.aimBlock = function () {
    return AIMC.raycast(this.world, this.player.eye(), this.player.dir(), 6);
};

AIMC.Game.prototype.tryBreak = function () {
    var hit = this.aimBlock();
    if (!hit) return;
    var id = this.world.getBlock(hit.x, hit.y, hit.z);
    if (id === 0) return;
    if (id === 6 && this.mode === 0) {
        this.ui.toastShow("Bedrocku nie zniszczysz!");
        return;
    }
    this.world.setBlock(hit.x, hit.y, hit.z, 0, true);
    this.audio.play("break");
    var b = AIMC.blockDef(id);
    if (b && this.blockAtlas.colors[b.side]) {
        this.burst(hit.x + 0.5, hit.y + 0.5, hit.z + 0.5, this.blockAtlas.colors[b.side]);
    }
    if (this.mode === 0) {
        var drop = AIMC.dropOf(id);
        if (drop > 0) {
            var left = this.inv.add(drop, 1);
            if (left > 0) this.ui.toastShow("Ekwipunek pelny!");
            else this.audio.play("pickup");
        }
    }
    this.ui.renderHotbar(this.inv);
};

AIMC.Game.prototype.canPlaceOn = function (x, y, z, pid, faceY) {
    var b = AIMC.blockDef(pid);
    if (!b) return false;
    var below = this.world.getBlock(x, y - 1, z);
    if (pid === 25) return true; // pochodnia: na boku tez (uproszczone)
    if (pid === 22 || pid === 23 || pid === 24) return below === 1 || below === 2;
    if (pid === 27) return below === 8; // kaktus na piasku
    if (pid === 21) return faceY === 1; // snieg na wierzchu
    return true;
};

AIMC.Game.prototype.tryPlace = function () {
    var held = this.inv.held();
    var hit = this.aimBlock();
    // Stol craftingowy: PPM otwiera.
    if (hit && this.world.getBlock(hit.x, hit.y, hit.z) === 26) {
        this.openPanel("craft");
        return;
    }
    if (!held || !hit) return;
    var pid = held.id;
    if (pid < 1 || pid > 35 || !AIMC.blockDef(pid)) return; // narzedzia/materialy
    if (pid === 7) return;
    var x = hit.x + hit.nx, y = hit.y + hit.ny, z = hit.z + hit.nz;
    if (y < 1 || y >= AIMC.HEIGHT) return;
    var cur = this.world.getBlock(x, y, z);
    if (cur !== 0 && cur !== 7) return;
    if (!this.canPlaceOn(x, y, z, pid, hit.ny)) {
        this.ui.toastShow("Tu nie mozna postawic.");
        return;
    }
    // Kolizja z graczem.
    var b = AIMC.blockDef(pid);
    if (b.solid) {
        var p = this.player, R = AIMC.RADIUS;
        if (x + 1 > p.x - R && x < p.x + R &&
            z + 1 > p.z - R && z < p.z + R &&
            y + 1 > p.y && y < p.y + AIMC.HEIGHT_P) {
            return;
        }
    }
    this.world.setBlock(x, y, z, pid, true);
    this.audio.play("place");
    if (this.mode === 0) this.inv.consumeHeld();
    this.ui.renderHotbar(this.inv);
};

AIMC.Game.prototype.burst = function (x, y, z, color) {
    for (var i = 0; i < 12; i++) {
        if (this.particles.length >= this.PMAX) this.particles.shift();
        this.particles.push({
            x: x, y: y, z: z,
            vx: (Math.random() - 0.5) * 3,
            vy: Math.random() * 3.5,
            vz: (Math.random() - 0.5) * 3,
            life: 0.5 + Math.random() * 0.3,
            r: color[0], g: color[1], b: color[2]
        });
    }
};

AIMC.Game.prototype.updateParticles = function (dt) {
    var posA = this.pGeo.attributes.position.array;
    var colA = this.pGeo.attributes.color.array;
    for (var i = this.particles.length - 1; i >= 0; i--) {
        var p = this.particles[i];
        p.life -= dt;
        if (p.life <= 0) {
            this.particles.splice(i, 1);
            continue;
        }
        p.vy -= 12 * dt;
        p.x += p.vx * dt; p.y += p.vy * dt; p.z += p.vz * dt;
    }
    for (var j = 0; j < this.PMAX; j++) {
        var q = this.particles[j];
        if (q) {
            posA[j * 3] = q.x; posA[j * 3 + 1] = q.y; posA[j * 3 + 2] = q.z;
            colA[j * 3] = q.r; colA[j * 3 + 1] = q.g; colA[j * 3 + 2] = q.b;
        } else {
            posA[j * 3 + 1] = -1000;
        }
    }
    this.pGeo.attributes.position.needsUpdate = true;
    this.pGeo.attributes.color.needsUpdate = true;
    this.pPts.visible = this.particles.length > 0;
};

// ---------- Petla ----------

AIMC.Game.prototype.loop = function (t) {
    var self = this;
    requestAnimationFrame(function (tt) { self.loop(tt); });
    if (this.state !== "play") {
        if (this.state === "loading") this.renderer.render(this.scene, this.camera);
        return;
    }
    var dt = this.lastT ? (t - this.lastT) / 1000 : 0.016;
    this.lastT = t;
    if (dt > 0.1) dt = 0.1;
    if (dt > 0) this.fps = this.fps * 0.95 + (1 / dt) * 0.05;

    var busy = this.panel || this.ui.chatOpen ||
        !this.ui.scrPause.classList.contains("hidden") ||
        !this.ui.scrPlay.classList.contains("hidden") ||
        !this.ui.scrAdmin.classList.contains("hidden") ||
        !this.ui.scrSettings.classList.contains("hidden") ||
        !this.ui.scrHelp.classList.contains("hidden") ||
        !this.ui.scrConfirm.classList.contains("hidden");

    if (!busy) {
        var k = this.keys;
        var keys = {
            f: !!(k.KeyW || k.ArrowUp), b: !!(k.KeyS || k.ArrowDown),
            l: !!(k.KeyA || k.ArrowLeft), r: !!(k.KeyD || k.ArrowRight),
            jump: !!k.Space, sneak: !!(k.ShiftLeft || k.ShiftRight),
            sprint: !!this.sprintLock,
            flyUp: !!k.Space, flyDown: !!(k.ShiftLeft || k.ShiftRight)
        };
        var splashed = this.player.update(dt, keys, this.world);
        if (splashed) this.audio.play("splash");
        if (this.player.y < -12) {
            this.player.x = this.spawn.x;
            this.player.y = this.spawn.y;
            this.player.z = this.spawn.z;
            this.ui.toastShow("Wrociles na spawn.");
        }
        // Kopanie / stawianie (przytrzymanie).
        this.breakCd -= dt;
        this.placeCd -= dt;
        if (this.lmb && this.breakCd <= 0) {
            this.tryBreak();
            this.breakCd = this.mode === 1 ? 0.18 : 0.25;
        }
        if (this.rmb && this.placeCd <= 0) {
            this.tryPlace();
            this.placeCd = 0.25;
            if (this.panel) this.rmb = false;
        }
        if (this.cycle) {
            this.dayTime += dt * 20; // pelna doba = 20 min
            if (this.dayTime >= 24000) this.dayTime -= 24000;
        }
        this.autosaveCd -= dt;
        if (this.autosaveCd <= 0) {
            this.autosaveCd = 60;
            this.saveGame(true);
        }
    }

    // Streaming + meshowanie.
    var pcx = Math.floor(this.player.x / 16), pcz = Math.floor(this.player.z / 16);
    this.world.updateStreaming(pcx, pcz);
    this.world.updateMeshes(2);

    // Kamera.
    var eye = this.player.eye();
    this.camera.position.set(eye.x, eye.y, eye.z);
    this.camera.rotation.y = this.player.yaw;
    this.camera.rotation.x = this.player.pitch;

    this.updateSky(dt);
    this.updateParticles(dt);

    // Zaznaczenie bloku.
    if (!busy) {
        var hit = this.aimBlock();
        if (hit) {
            this.selBox.visible = true;
            this.selBox.position.set(hit.x + 0.5, hit.y + 0.5, hit.z + 0.5);
        } else this.selBox.visible = false;
    } else this.selBox.visible = false;

    // Woda / F3.
    var headId = this.world.getBlock(Math.floor(eye.x), Math.floor(eye.y), Math.floor(eye.z));
    this.ui.setWater(headId === 7);
    this.debugCd -= dt;
    if (this.showDebug && this.debugCd <= 0) {
        this.debugCd = 0.25;
        this.ui.setDebug(this.debugText());
    }

    this.renderer.render(this.scene, this.camera);
};

AIMC.Game.prototype.sunDir = function () {
    var a = (this.dayTime / 24000) * Math.PI * 2;
    // t=0 wschod, 6000 zenit, 12000 zachod, 18000 dol.
    return {
        x: -Math.cos(a),
        y: Math.sin(a),
        z: 0.25
    };
};

AIMC.Game.prototype.updateSky = function (dt) {
    var sd = this.sunDir();
    var len = Math.sqrt(sd.x * sd.x + sd.y * sd.y + sd.z * sd.z);
    sd.x /= len; sd.y /= len; sd.z /= len;
    var e = sd.y; // wysokosc slonca -1..1
    var dayness = Math.max(0, Math.min(1, (e + 0.08) / 0.33));
    var dusk = Math.max(0, 1 - Math.abs(e) * 5) * (e > -0.12 ? 1 : 0);

    var r = 0.01 + dayness * 0.5 + dusk * 0.35;
    var g = 0.015 + dayness * 0.68 + dusk * 0.12;
    var b = 0.05 + dayness * 0.92 - dusk * 0.15;
    this.scene.background.setRGB(r, g, b);
    this.scene.fog.color.setRGB(r, g, b);

    var rd = this.settings.renderDist * 16;
    this.scene.fog.near = rd * 0.45;
    this.scene.fog.far = rd * 1.0;

    this.hemi.intensity = 0.3 + dayness * 0.65;
    this.sunLight.intensity = 0.08 + Math.max(0, e) * 0.85;
    this.sunLight.color.setRGB(1, 0.75 + dayness * 0.25, 0.6 + dayness * 0.4);
    var p = this.player;
    this.sunLight.position.set(p.x + sd.x * 120, p.y + sd.y * 120, p.z + sd.z * 120);
    this.sunLight.target.position.set(p.x, p.y, p.z);
    this.sunLight.target.updateMatrixWorld();

    this.stars.material.opacity = 1 - dayness;
    this.stars.position.set(p.x, 0, p.z);

    this.sunMesh.position.set(p.x + sd.x * 380, p.y + sd.y * 380, p.z + sd.z * 380);
    this.sunMesh.lookAt(p.x, p.y, p.z);
    this.sunMesh.visible = e > -0.12;
    this.moonMesh.position.set(p.x - sd.x * 380, p.y - sd.y * 380, p.z - sd.z * 380);
    this.moonMesh.lookAt(p.x, p.y, p.z);
    this.moonMesh.visible = e < 0.12;

    for (var i = 0; i < this.cloudMeshes.length; i++) {
        var m = this.cloudMeshes[i];
        m.position.x += dt * 1.2;
        if (m.position.x - p.x > 120) m.position.x -= 240;
    }
};

AIMC.Game.prototype.debugText = function () {
    var p = this.player;
    var bx = Math.floor(p.x), by = Math.floor(p.y), bz = Math.floor(p.z);
    var hit = this.aimBlock();
    var facing = "N";
    var yaw = ((p.yaw % (Math.PI * 2)) + Math.PI * 2) % (Math.PI * 2);
    if (yaw < Math.PI / 4 || yaw >= Math.PI * 7 / 4) facing = "-Z";
    else if (yaw < Math.PI * 3 / 4) facing = "-X";
    else if (yaw < Math.PI * 5 / 4) facing = "+Z";
    else facing = "+X";
    var biome = AIMC.BIOME_NAMES[this.world.gen.getBiome(bx, bz)];
    var lines = [
        "AiMINECRAFT Web | " + Math.round(this.fps) + " fps",
        "XYZ: " + p.x.toFixed(2) + " / " + p.y.toFixed(2) + " / " + p.z.toFixed(2),
        "Blok: " + bx + " " + by + " " + bz + " | Chunk: " + Math.floor(bx / 16) + " " + Math.floor(bz / 16),
        "Biom: " + biome + " | Kierunek: " + facing,
        "Cel: " + (hit ? (hit.x + " " + hit.y + " " + hit.z + " id=" + this.world.getBlock(hit.x, hit.y, hit.z)) : "-"),
        "Czas: " + Math.round(this.dayTime) + " | Tryb: " + (this.mode === 1 ? "Creative" : "Survival"),
        "Chunki: " + this.world.chunkCount() + " | Seed: " + this.seed,
        "Latanie: " + (p.flying ? "ON" : "OFF") + " | W wodzie: " + (p.inWater ? "tak" : "nie")
    ];
    return lines.join("\n");
};

if (typeof window !== "undefined" && window.addEventListener) {
    window.addEventListener("load", function () {
        var game = new AIMC.Game();
        window.AIMC_GAME = game;
        game.boot();
    });
}
