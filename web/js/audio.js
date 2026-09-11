/* AiMINECRAFT Web - syntezowane dzwieki (WebAudio, zero plikow). */
"use strict";

var AIMC = (typeof window !== "undefined")
    ? (window.AIMC || (window.AIMC = {}))
    : {};
if (typeof module !== "undefined" && module.exports) module.exports = AIMC;

AIMC.Audio = function () {
    this.ctx = null;
    this.enabled = true;
    this.noiseBuf = null;
};

/** Tworzy kontekst (wywolaj po pierwszym kliknieciu). */
AIMC.Audio.prototype.unlock = function () {
    if (this.ctx || typeof window === "undefined") return;
    try {
        var AC = window.AudioContext || window.webkitAudioContext;
        if (!AC) return;
        this.ctx = new AC();
        var len = this.ctx.sampleRate | 0;
        this.noiseBuf = this.ctx.createBuffer(1, len, this.ctx.sampleRate);
        var d = this.noiseBuf.getChannelData(0);
        for (var i = 0; i < len; i++) d[i] = Math.random() * 2 - 1;
    } catch (e) { this.ctx = null; }
};

AIMC.Audio.prototype.resume = function () {
    if (this.ctx && this.ctx.state === "suspended") {
        try { this.ctx.resume(); } catch (e) {}
    }
};

AIMC.Audio.prototype.tone = function (freq, dur, type, vol, slideTo) {
    if (!this.ctx || !this.enabled) return;
    try {
        var t = this.ctx.currentTime;
        var o = this.ctx.createOscillator();
        var g = this.ctx.createGain();
        o.type = type || "square";
        o.frequency.setValueAtTime(freq, t);
        if (slideTo) o.frequency.exponentialRampToValueAtTime(slideTo, t + dur);
        g.gain.setValueAtTime(vol || 0.15, t);
        g.gain.exponentialRampToValueAtTime(0.001, t + dur);
        o.connect(g);
        g.connect(this.ctx.destination);
        o.start(t);
        o.stop(t + dur + 0.02);
    } catch (e) {}
};

AIMC.Audio.prototype.noise = function (dur, vol, filterFreq, sweepTo) {
    if (!this.ctx || !this.enabled || !this.noiseBuf) return;
    try {
        var t = this.ctx.currentTime;
        var src = this.ctx.createBufferSource();
        src.buffer = this.noiseBuf;
        src.loop = true;
        var f = this.ctx.createBiquadFilter();
        f.type = "lowpass";
        f.frequency.setValueAtTime(filterFreq || 1000, t);
        if (sweepTo) f.frequency.exponentialRampToValueAtTime(sweepTo, t + dur);
        var g = this.ctx.createGain();
        g.gain.setValueAtTime(vol || 0.2, t);
        g.gain.exponentialRampToValueAtTime(0.001, t + dur);
        src.connect(f);
        f.connect(g);
        g.connect(this.ctx.destination);
        src.start(t);
        src.stop(t + dur + 0.02);
    } catch (e) {}
};

AIMC.Audio.prototype.play = function (name) {
    if (!this.enabled) return;
    this.resume();
    if (name === "click") this.tone(950, 0.05, "square", 0.08);
    else if (name === "break") this.noise(0.18, 0.25, 900, 200);
    else if (name === "place") { this.tone(180, 0.09, "triangle", 0.2); this.noise(0.05, 0.1, 2000); }
    else if (name === "pickup") this.tone(620, 0.12, "sine", 0.15, 1240);
    else if (name === "splash") this.noise(0.4, 0.25, 2500, 400);
    else if (name === "level") { this.tone(523, 0.12, "square", 0.1); this.tone(784, 0.18, "square", 0.1); }
};
