/* AiMINECRAFT Web - interfejs w DOM (mysz zawsze dziala). */
"use strict";

var AIMC = (typeof window !== "undefined")
    ? (window.AIMC || (window.AIMC = {}))
    : {};
if (typeof module !== "undefined" && module.exports) module.exports = AIMC;

AIMC.UI = function (game) {
    this.game = game;
    this.root = document.getElementById("ui");
    this.blockAtlasURL = "";
    this.itemAtlasURL = "";
    this.chatOpen = false;
    this.confirmYes = null;
    this.confirmNo = null;
    this.toastTimer = 0;
    this.build();
};

AIMC.UI.prototype.el = function (tag, cls, html) {
    var e = document.createElement(tag);
    if (cls) e.className = cls;
    if (html !== undefined) e.innerHTML = html;
    return e;
};

AIMC.UI.prototype.setAtlases = function (blockURL, itemURL) {
    this.blockAtlasURL = blockURL;
    this.itemAtlasURL = itemURL;
};

/** Styl ikony przedmiotu/bloku z atlasu. */
AIMC.UI.prototype.iconStyle = function (id) {
    var url, size, n, idx;
    if (id >= 100) {
        url = this.itemAtlasURL; size = 400; n = 3; idx = id - 100;
    } else {
        var b = AIMC.blockDef(id);
        url = this.blockAtlasURL; size = 800; n = 7;
        idx = b ? b.side : 0;
    }
    var c = idx % (n + 1), r = Math.floor(idx / (n + 1));
    return "background-image:url('" + url + "');background-size:" + size + "%;" +
        "background-position:" + (c / n * 100) + "% " + (r / n * 100) + "%;";
};

AIMC.UI.prototype.slotHTML = function (stack, extra, num) {
    var inner = "";
    if (num) inner += '<span class="num">' + num + "</span>";
    if (stack) {
        inner += '<div class="icon" style="' + this.iconStyle(stack.id) + '"></div>';
        if (stack.count > 1) inner += '<span class="cnt">' + stack.count + "</span>";
    }
    return '<div class="slot' + (extra || "") + '">' + inner + "</div>";
};

AIMC.UI.prototype.build = function () {
    var self = this;
    var R = this.root;
    R.innerHTML = "";

    // Ladowanie.
    this.scrLoading = this.el("div", "screen dim");
    this.scrLoading.appendChild(this.el("h2", "", "Ladowanie..."));
    this.loadBar = this.el("div", "", "");
    var loadWrap = this.el("div", "", "");
    loadWrap.id = "loadbar";
    this.loadFill = this.el("div", "", "");
    loadWrap.appendChild(this.loadFill);
    this.loadBar.appendChild(loadWrap);
    this.loadText = this.el("div", "", "Start");
    this.loadText.id = "loadtext";
    this.scrLoading.appendChild(this.loadBar);
    this.scrLoading.appendChild(this.loadText);
    R.appendChild(this.scrLoading);

    // Tytul.
    this.scrTitle = this.el("div", "screen title-bg hidden");
    var logo = this.el("h1", "logo", "Ai<span>MINECRAFT</span> <span>Web</span>");
    this.scrTitle.appendChild(logo);
    this.scrTitle.appendChild(this.el("div", "subtitle", "Zagraj w przegladarce - zero instalacji!"));
    this.inpName = document.createElement("input");
    this.inpName.type = "text"; this.inpName.value = "Nowy Swiat"; this.inpName.maxLength = 24;
    this.inpSeed = document.createElement("input");
    this.inpSeed.type = "text"; this.inpSeed.placeholder = "losowy"; this.inpSeed.maxLength = 24;
    var f1 = this.el("div", "field", "Nazwa: "); f1.appendChild(this.inpName);
    var f2 = this.el("div", "field", "Seed: "); f2.appendChild(this.inpSeed);
    this.scrTitle.appendChild(f1);
    this.scrTitle.appendChild(f2);
    var modeRow = this.el("div", "btnrow");
    this.btnModeS = this.el("button", "btn green small", "Survival");
    this.btnModeC = this.el("button", "btn small", "Creative");
    this.btnModeS.onclick = function () { self.game.setNewMode(0); };
    this.btnModeC.onclick = function () { self.game.setNewMode(1); };
    modeRow.appendChild(this.btnModeS);
    modeRow.appendChild(this.btnModeC);
    this.scrTitle.appendChild(modeRow);
    this.btnPlay = this.el("button", "btn green", "GRAJ - nowy swiat");
    this.btnPlay.onclick = function () { self.game.newGame(); };
    this.btnContinue = this.el("button", "btn", "KONTYNUUJ");
    this.btnContinue.onclick = function () { self.game.continueGame(); };
    var row = this.el("div", "btnrow");
    this.btnSettings = this.el("button", "btn small", "Ustawienia");
    this.btnSettings.onclick = function () { self.game.openSettings("title"); };
    this.btnHelp = this.el("button", "btn small", "Pomoc");
    this.btnHelp.onclick = function () { self.game.openHelp("title"); };
    this.btnDelSave = this.el("button", "btn red small", "Usun zapis");
    this.btnDelSave.onclick = function () {
        self.askConfirm("Usunac zapisany swiat?", function () { self.game.deleteSave(); });
    };
    row.appendChild(this.btnSettings);
    row.appendChild(this.btnHelp);
    row.appendChild(this.btnDelSave);
    this.scrTitle.appendChild(this.btnPlay);
    this.scrTitle.appendChild(this.btnContinue);
    this.scrTitle.appendChild(row);
    this.lblSaveInfo = this.el("div", "credit", "");
    this.scrTitle.appendChild(this.lblSaveInfo);
    this.scrTitle.appendChild(this.el("div", "credit", "AiMINECRAFT Web v1.0 - Three.js"));
    R.appendChild(this.scrTitle);

    // Menu gry (ESC w grze -> pauza; to menu po zapisie? nie: playmenu = glowne menu pauzy).
    this.scrPlay = this.el("div", "screen dim hidden");
    this.scrPlay.appendChild(this.el("h2", "", "Menu gry"));
    this.lblWorldInfo = this.el("div", "credit", "");
    this.scrPlay.appendChild(this.lblWorldInfo);
    var bResume = this.el("button", "btn green", "Wroc do gry");
    bResume.onclick = function () { self.game.resume(); };
    var bSave = this.el("button", "btn", "Zapisz swiat");
    bSave.onclick = function () { self.game.saveGame(false); };
    var bSet2 = this.el("button", "btn", "Ustawienia");
    bSet2.onclick = function () { self.game.openSettings("play"); };
    var bHelp2 = this.el("button", "btn", "Pomoc");
    bHelp2.onclick = function () { self.game.openHelp("play"); };
    var bTitle = this.el("button", "btn red", "Zapisz i wroc do tytulu");
    bTitle.onclick = function () { self.game.toTitle(); };
    this.scrPlay.appendChild(bResume);
    this.scrPlay.appendChild(bSave);
    this.scrPlay.appendChild(bSet2);
    this.scrPlay.appendChild(bHelp2);
    this.scrPlay.appendChild(bTitle);
    R.appendChild(this.scrPlay);

    // Ustawienia.
    this.scrSettings = this.el("div", "screen dim hidden");
    this.scrSettings.appendChild(this.el("h2", "", "Ustawienia"));
    var panel = this.el("div", "panel");
    this.setRows = {};
    function setRow(key, label, min, max, step) {
        var f = self.el("div", "field", label + ": ");
        var inp = document.createElement("input");
        inp.type = "range"; inp.min = min; inp.max = max; inp.step = step;
        var val = self.el("span", "val", "");
        inp.oninput = function () { self.game.setSetting(key, parseFloat(inp.value)); };
        f.appendChild(inp); f.appendChild(val);
        panel.appendChild(f);
        self.setRows[key] = {inp: inp, val: val};
    }
    setRow("renderDist", "Zasieg widzenia", 1, 4, 1);
    setRow("sensitivity", "Czulosci myszy", 0.5, 3.0, 0.1);
    setRow("fov", "Pole widzenia", 60, 110, 1);
    function togRow(key, label) {
        var f = self.el("div", "field", label + ": ");
        var b = self.el("button", "btn small", "");
        b.onclick = function () { self.game.toggleSetting(key); };
        f.appendChild(b);
        panel.appendChild(f);
        self.setRows[key] = {btn: b};
    }
    togRow("clouds", "Chmury");
    togRow("sounds", "Dzwieki");
    this.scrSettings.appendChild(panel);
    this.btnSetBack = this.el("button", "btn green", "Gotowe");
    this.btnSetBack.onclick = function () { self.game.settingsBack(); };
    this.scrSettings.appendChild(this.btnSetBack);
    R.appendChild(this.scrSettings);

    // Pauza (krotka, po ESC z pointer lock).
    this.scrPause = this.el("div", "screen dim hidden");
    this.scrPause.appendChild(this.el("h2", "", "Pauza"));
    var bResume2 = this.el("button", "btn green", "Wroc do gry");
    bResume2.onclick = function () { self.game.resume(); };
    var bMenu = this.el("button", "btn", "Menu gry");
    bMenu.onclick = function () { self.game.openPlayMenu(); };
    this.scrPause.appendChild(bResume2);
    this.scrPause.appendChild(bMenu);
    R.appendChild(this.scrPause);

    // Admin.
    this.scrAdmin = this.el("div", "screen dim hidden");
    this.scrAdmin.appendChild(this.el("h2", "", "Panel admina"));
    var ap = this.el("div", "panel");
    function adminRow(label, inputs, btnText, fn) {
        var r = self.el("div", "adminrow");
        r.appendChild(self.el("label", "", label));
        var ins = [];
        for (var i = 0; i < inputs; i++) {
            var inp = document.createElement("input");
            inp.type = "text";
            r.appendChild(inp);
            ins.push(inp);
        }
        var b = self.el("button", "btn small", btnText);
        b.onclick = function () {
            fn(ins.map(function (x) { return x.value; }));
        };
        r.appendChild(b);
        ap.appendChild(r);
    }
    adminRow("Daj przedmiot [id] [ilosc]", 2, "Daj", function (v) {
        self.game.adminAction("give", v);
    });
    adminRow("Teleport [x] [y] [z]", 3, "TP", function (v) {
        self.game.adminAction("tp", v);
    });
    adminRow("Czas [0-24000]", 1, "Ustaw", function (v) {
        self.game.adminAction("time", v);
    });
    adminRow("Tryb [0/1]", 1, "Ustaw", function (v) {
        self.game.adminAction("gamemode", v);
    });
    adminRow("Wyczysc ekwipunek", 0, "Czysc", function () {
        self.game.adminAction("clear", []);
    });
    adminRow("Wypelnij [x1 y1 z1 x2 y2 z2 id]", 1, "Wypelnij", function (v) {
        self.game.adminAction("fill", v);
    });
    var cycRow = this.el("div", "adminrow");
    cycRow.appendChild(this.el("label", "", "Cykl dnia/nocy"));
    this.btnCycle = this.el("button", "btn small", "ON");
    this.btnCycle.onclick = function () { self.game.adminAction("cycle", []); };
    cycRow.appendChild(this.btnCycle);
    ap.appendChild(cycRow);
    this.scrAdmin.appendChild(ap);
    var bAdminClose = this.el("button", "btn green", "Zamknij");
    bAdminClose.onclick = function () { self.game.resume(); };
    this.scrAdmin.appendChild(bAdminClose);
    R.appendChild(this.scrAdmin);

    // Potwierdzenie.
    this.scrConfirm = this.el("div", "screen dim hidden");
    this.scrConfirm.appendChild(this.el("h2", "", "Potwierdz"));
    this.lblConfirm = this.el("div", "field", "");
    this.scrConfirm.appendChild(this.lblConfirm);
    var cRow = this.el("div", "btnrow");
    var bYes = this.el("button", "btn green small", "TAK");
    bYes.onclick = function () {
        var f = self.confirmYes; self.confirmYes = null; self.confirmNo = null;
        self.hideAll(); if (f) f();
    };
    var bNo = this.el("button", "btn red small", "NIE");
    bNo.onclick = function () {
        var f = self.confirmNo; self.confirmYes = null; self.confirmNo = null;
        self.hideAll(); if (f) f();
    };
    cRow.appendChild(bYes); cRow.appendChild(bNo);
    this.scrConfirm.appendChild(cRow);
    R.appendChild(this.scrConfirm);

    // Pomoc.
    this.scrHelp = this.el("div", "screen dim hidden");
    this.scrHelp.appendChild(this.el("h2", "", "Pomoc - sterowanie"));
    var hp = this.el("div", "panel");
    hp.innerHTML =
        "<table class='keys'>" +
        "<tr><td>WASD</td><td>ruch</td></tr>" +
        "<tr><td>Mysz</td><td>rozgladanie (kliknij gre, by zlapac kursor)</td></tr>" +
        "<tr><td>LPM</td><td>niszcz blok</td></tr>" +
        "<tr><td>PPM</td><td>staw blok / uzyj (stoi craftingowy)</td></tr>" +
        "<tr><td>E</td><td>ekwipunek</td></tr>" +
        "<tr><td>1-9 / kolko</td><td>wybor slotu</td></tr>" +
        "<tr><td>Spacja</td><td>skok / plywanie w gore / latanie w gore</td></tr>" +
        "<tr><td>Shift</td><td>skradanie / latanie w dol</td></tr>" +
        "<tr><td>2x W</td><td>sprint</td></tr>" +
        "<tr><td>2x Spacja / F</td><td>latanie on/off (creative)</td></tr>" +
        "<tr><td>T lub /</td><td>czat / komendy</td></tr>" +
        "<tr><td>F3</td><td>debug</td></tr>" +
        "<tr><td>F11</td><td>pelny ekran (przegladarka)</td></tr>" +
        "<tr><td>ESC</td><td>pauza / zamknij okno</td></tr>" +
        "</table>" +
        "<div class='credit'>Komendy: /give /tp /time /gamemode /clear /seed /help</div>";
    this.scrHelp.appendChild(hp);
    this.btnHelpBack = this.el("button", "btn green", "Wroc");
    this.btnHelpBack.onclick = function () { self.game.helpBack(); };
    this.scrHelp.appendChild(this.btnHelpBack);
    R.appendChild(this.scrHelp);

    // HUD.
    this.hud = this.el("div", "hidden");
    this.hud.id = "hud";
    this.cross = this.el("div", "");
    this.cross.id = "cross";
    this.hud.appendChild(this.cross);
    this.hotbar = this.el("div", "");
    this.hotbar.id = "hotbar";
    this.hud.appendChild(this.hotbar);
    this.chatlog = this.el("div", "");
    this.chatlog.id = "chatlog";
    this.hud.appendChild(this.chatlog);
    this.chatinput = document.createElement("input");
    this.chatinput.id = "chatinput";
    this.chatinput.type = "text";
    this.chatinput.maxLength = 120;
    this.chatinput.className = "hidden";
    var self2 = this;
    this.chatinput.addEventListener("keydown", function (e) {
        e.stopPropagation();
        if (e.code === "Enter") self2.game.chatSubmit(self2.chatinput.value);
        else if (e.code === "Escape") self2.game.closeChat();
    });
    this.hud.appendChild(this.chatinput);
    this.debug = this.el("div", "hidden");
    this.debug.id = "debug";
    this.hud.appendChild(this.debug);
    this.toast = this.el("div", "hidden");
    this.toast.id = "toast";
    this.hud.appendChild(this.toast);
    this.waterfx = this.el("div", "hidden");
    this.waterfx.id = "waterfx";
    this.hud.appendChild(this.waterfx);
    R.appendChild(this.hud);

    // Ekwipunek.
    this.scrInv = this.el("div", "screen dim hidden");
    var ip = this.el("div", "panel");
    ip.appendChild(this.el("h2", "", "Ekwipunek"));
    var tabs = this.el("div", "tabs");
    this.tabBtns = [];
    var tabNames = ["EQ", "Crafting 2x2", "Creative"];
    for (var t = 0; t < 3; t++) {
        (function (ti) {
            var b = self.el("button", "tab", tabNames[ti]);
            b.onclick = function () { self.game.setTab(ti); };
            tabs.appendChild(b);
            self.tabBtns.push(b);
        })(t);
    }
    ip.appendChild(tabs);
    this.invBody = this.el("div", "");
    ip.appendChild(this.invBody);
    this.scrInv.appendChild(ip);
    R.appendChild(this.scrInv);

    // Stul craftingowy 3x3.
    this.scrCraft = this.el("div", "screen dim hidden");
    var cp = this.el("div", "panel");
    cp.appendChild(this.el("h2", "", "Stol rzemieslniczy"));
    this.craftBody = this.el("div", "");
    cp.appendChild(this.craftBody);
    this.scrCraft.appendChild(cp);
    R.appendChild(this.scrCraft);

    // Przeciagany stos.
    this.cursorEl = this.el("div", "hidden");
    this.cursorEl.id = "cursorstack";
    R.appendChild(this.cursorEl);
    document.addEventListener("mousemove", function (e) {
        self.cursorEl.style.left = e.clientX + "px";
        self.cursorEl.style.top = e.clientY + "px";
    });
    document.addEventListener("contextmenu", function (e) { e.preventDefault(); });
};

AIMC.UI.prototype.hideAll = function () {
    var ss = [this.scrLoading, this.scrTitle, this.scrPlay, this.scrSettings,
        this.scrPause, this.scrAdmin, this.scrConfirm, this.scrHelp,
        this.scrInv, this.scrCraft];
    for (var i = 0; i < ss.length; i++) ss[i].classList.add("hidden");
};

AIMC.UI.prototype.showLoading = function (pct, text) {
    this.hideAll();
    this.scrLoading.classList.remove("hidden");
    this.loadFill.style.width = Math.round(pct * 100) + "%";
    this.loadText.textContent = text;
};

AIMC.UI.prototype.showTitle = function (hasSave, saveInfo, mode) {
    this.hideAll();
    this.hud.classList.add("hidden");
    this.cursorEl.classList.add("hidden");
    this.scrTitle.classList.remove("hidden");
    this.btnContinue.disabled = !hasSave;
    this.btnDelSave.disabled = !hasSave;
    this.lblSaveInfo.textContent = hasSave ? saveInfo : "Brak zapisanego swiata.";
    this.btnModeS.className = "btn small" + (mode === 0 ? " green" : "");
    this.btnModeC.className = "btn small" + (mode === 1 ? " green" : "");
};

AIMC.UI.prototype.showHud = function () {
    if (document.activeElement && document.activeElement.blur) document.activeElement.blur();
    this.hideAll();
    this.hud.classList.remove("hidden");
    this.cursorEl.classList.add("hidden");
    this.chatOpen = false;
    this.chatinput.classList.add("hidden");
};

AIMC.UI.prototype.showPlayMenu = function (info) {
    this.hideAll();
    this.lblWorldInfo.textContent = info;
    this.scrPlay.classList.remove("hidden");
};

AIMC.UI.prototype.showSettings = function (s) {
    this.hideAll();
    this.setRows.renderDist.inp.value = s.renderDist;
    this.setRows.renderDist.val.textContent = s.renderDist;
    this.setRows.sensitivity.inp.value = s.sensitivity;
    this.setRows.sensitivity.val.textContent = s.sensitivity.toFixed(1);
    this.setRows.fov.inp.value = s.fov;
    this.setRows.fov.val.textContent = s.fov;
    this.setRows.clouds.btn.textContent = s.clouds ? "ON" : "OFF";
    this.setRows.sounds.btn.textContent = s.sounds ? "ON" : "OFF";
    this.scrSettings.classList.remove("hidden");
};

AIMC.UI.prototype.refreshSettings = function (s) {
    if (this.scrSettings.classList.contains("hidden")) return;
    this.showSettings(s);
};

AIMC.UI.prototype.showPause = function () {
    this.hideAll();
    this.scrPause.classList.remove("hidden");
};

AIMC.UI.prototype.showAdmin = function (cycle) {
    this.hideAll();
    this.btnCycle.textContent = cycle ? "ON" : "OFF";
    this.scrAdmin.classList.remove("hidden");
};

AIMC.UI.prototype.askConfirm = function (text, onYes, onNo) {
    this.hideAll();
    this.lblConfirm.textContent = text;
    this.confirmYes = onYes || null;
    this.confirmNo = onNo || null;
    this.scrConfirm.classList.remove("hidden");
};

AIMC.UI.prototype.showHelp = function () {
    this.hideAll();
    this.scrHelp.classList.remove("hidden");
};

AIMC.UI.prototype.renderHotbar = function (inv) {
    var html = "";
    for (var i = 0; i < 9; i++) {
        html += this.slotHTML(inv.get(i), i === inv.selected ? " sel" : "", i + 1);
    }
    this.hotbar.innerHTML = html;
    var self = this;
    var slots = this.hotbar.children;
    for (var j = 0; j < 9; j++) {
        (function (jj) {
            slots[jj].addEventListener("mousedown", function (e) {
                e.stopPropagation();
                self.game.hotbarSelect(jj);
            });
        })(j);
    }
};

AIMC.UI.prototype.bindSlots = function (container, fn) {
    var self = this;
    var slots = container.querySelectorAll(".slot");
    for (var i = 0; i < slots.length; i++) {
        (function (idx, elm) {
            elm.addEventListener("mousedown", function (e) {
                e.stopPropagation();
                e.preventDefault();
                fn(idx, e.button === 2);
            });
        })(i, slots[i]);
    }
};

AIMC.UI.prototype.renderInventory = function (inv, tab, cursor, craft2, creative) {
    this.hideAll();
    this.cursorEl.classList.remove("hidden");
    this.scrInv.classList.remove("hidden");
    for (var t = 0; t < 3; t++) {
        this.tabBtns[t].className = "tab" + (t === tab ? " sel" : "");
    }
    var html = "", i;
    if (tab === 0) {
        html += "<div>Glowny (sloty 9-35):</div><div class='invgrid'>";
        for (i = 9; i < 36; i++) html += this.slotHTML(inv.get(i));
        html += "</div><div>Pasek (1-9):</div><div class='invgrid'>";
        for (i = 0; i < 9; i++) html += this.slotHTML(inv.get(i));
        html += "</div>";
        this.invBody.innerHTML = html;
        var grids = this.invBody.querySelectorAll(".invgrid");
        this.bindSlots(grids[0], function (idx, right) { this.game.invClick(idx + 9, right); }.bind(this));
        this.bindSlots(grids[1], function (idx, right) { this.game.invClick(idx, right); }.bind(this));
    } else if (tab === 1) {
        html += "<div class='craftrow'><div class='craftgrid g2'>";
        for (i = 0; i < 4; i++) html += this.slotHTML(craft2.grid[i]);
        html += "</div><div class='craftarrow'>-&gt;</div><div id='craftres'>";
        html += this.slotHTML(craft2.result);
        html += "</div></div><div>Ekwipunek:</div><div class='invgrid'>";
        for (i = 9; i < 36; i++) html += this.slotHTML(inv.get(i));
        html += "</div><div class='invgrid'>";
        for (i = 0; i < 9; i++) html += this.slotHTML(inv.get(i));
        html += "</div>";
        this.invBody.innerHTML = html;
        this.bindSlots(this.invBody.querySelector(".craftgrid"), function (idx, right) {
            this.game.craftClick(idx, right);
        }.bind(this));
        this.bindSlots(this.invBody.querySelector("#craftres"), function (idx, right) {
            this.game.resultClick(right);
        }.bind(this));
        var grids2 = this.invBody.querySelectorAll(".invgrid");
        this.bindSlots(grids2[0], function (idx, right) { this.game.invClick(idx + 9, right); }.bind(this));
        this.bindSlots(grids2[1], function (idx, right) { this.game.invClick(idx, right); }.bind(this));
    } else {
        html += "<div>Kliknij, by wziac (PPM = caly stos):</div><div class='invgrid'>";
        for (i = 0; i < creative.length; i++) {
            html += this.slotHTML({id: creative[i], count: 1});
        }
        html += "</div>";
        this.invBody.innerHTML = html;
        this.bindSlots(this.invBody.querySelector(".invgrid"), function (idx, right) {
            this.game.creativeClick(creative[idx], right);
        }.bind(this));
    }
    this.renderCursor(cursor);
};

AIMC.UI.prototype.renderCrafting = function (inv, cursor, craft3) {
    this.hideAll();
    this.cursorEl.classList.remove("hidden");
    this.scrCraft.classList.remove("hidden");
    var html = "<div class='craftrow'><div class='craftgrid g3'>", i;
    for (i = 0; i < 9; i++) html += this.slotHTML(craft3.grid[i]);
    html += "</div><div class='craftarrow'>-&gt;</div><div id='craft3res'>";
    html += this.slotHTML(craft3.result);
    html += "</div></div><div>Ekwipunek:</div><div class='invgrid'>";
    for (i = 9; i < 36; i++) html += this.slotHTML(inv.get(i));
    html += "</div><div class='invgrid'>";
    for (i = 0; i < 9; i++) html += this.slotHTML(inv.get(i));
    html += "</div>";
    this.craftBody.innerHTML = html;
    this.bindSlots(this.craftBody.querySelector(".craftgrid"), function (idx, right) {
        this.game.craftClick(idx, right);
    }.bind(this));
    this.bindSlots(this.craftBody.querySelector("#craft3res"), function (idx, right) {
        this.game.resultClick(right);
    }.bind(this));
    var grids = this.craftBody.querySelectorAll(".invgrid");
    this.bindSlots(grids[0], function (idx, right) { this.game.invClick(idx + 9, right); }.bind(this));
    this.bindSlots(grids[1], function (idx, right) { this.game.invClick(idx, right); }.bind(this));
    this.renderCursor(cursor);
};

AIMC.UI.prototype.renderCursor = function (cursor) {
    if (cursor) {
        this.cursorEl.classList.remove("hidden");
        this.cursorEl.innerHTML = '<div class="icon" style="' + this.iconStyle(cursor.id) + '"></div>' +
            (cursor.count > 1 ? '<span class="cnt">' + cursor.count + "</span>" : "");
    } else {
        this.cursorEl.innerHTML = "";
    }
};

AIMC.UI.prototype.chatAdd = function (text) {
    var d = this.el("div", "", text);
    this.chatlog.appendChild(d);
    while (this.chatlog.children.length > 12) {
        this.chatlog.removeChild(this.chatlog.firstChild);
    }
    // starsze wiadomosci znikaja po 12 s
    var self = this;
    setTimeout(function () {
        if (d.parentNode && !self.chatOpen) d.parentNode.removeChild(d);
    }, 12000);
};

AIMC.UI.prototype.openChat = function () {
    this.chatOpen = true;
    this.chatinput.classList.remove("hidden");
    this.chatinput.value = "";
    var self = this;
    setTimeout(function () { self.chatinput.focus(); }, 0);
};

AIMC.UI.prototype.closeChat = function () {
    this.chatOpen = false;
    this.chatinput.classList.add("hidden");
    this.chatinput.blur();
};

AIMC.UI.prototype.toastShow = function (text, ms) {
    var self = this;
    this.toast.textContent = text;
    this.toast.classList.remove("hidden");
    if (this.toastTimer) clearTimeout(this.toastTimer);
    this.toastTimer = setTimeout(function () {
        self.toast.classList.add("hidden");
        self.toastTimer = 0;
    }, ms || 2500);
};

AIMC.UI.prototype.setDebug = function (html) {
    if (html === null) {
        this.debug.classList.add("hidden");
    } else {
        this.debug.classList.remove("hidden");
        this.debug.textContent = html;
    }
};

AIMC.UI.prototype.setWater = function (on) {
    if (on) this.waterfx.classList.remove("hidden");
    else this.waterfx.classList.add("hidden");
};

AIMC.UI.prototype.setCross = function (on) {
    if (on) this.cross.classList.remove("hidden");
    else this.cross.classList.add("hidden");
};
