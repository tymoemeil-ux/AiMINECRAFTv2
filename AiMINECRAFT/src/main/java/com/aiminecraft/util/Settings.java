package com.aiminecraft.util;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/** Ustawienia gry zapisywane w settings.properties. */
public class Settings {

    private static final String FILE = "settings.properties";

    public int renderDistance = 6;   // 2 - 12
    public int fov = 75;             // 60 - 110
    public float sensitivity = 1.0f; // 0.2 - 3.0 (mnoznik)
    public boolean vsync = true;
    public boolean sound = true;

    public void load() {
        Properties p = new Properties();
        try (FileInputStream in = new FileInputStream(FILE)) {
            p.load(in);
        } catch (IOException e) {
            return; // brak pliku = domyslne
        }
        renderDistance = clampInt(p, "renderDistance", renderDistance, 2, 12);
        fov = clampInt(p, "fov", fov, 60, 110);
        sensitivity = clampFloat(p, "sensitivity", sensitivity, 0.2f, 3.0f);
        vsync = Boolean.parseBoolean(p.getProperty("vsync", "true"));
        sound = Boolean.parseBoolean(p.getProperty("sound", "true"));
    }

    public void save() {
        Properties p = new Properties();
        p.setProperty("renderDistance", Integer.toString(renderDistance));
        p.setProperty("fov", Integer.toString(fov));
        p.setProperty("sensitivity", Float.toString(sensitivity));
        p.setProperty("vsync", Boolean.toString(vsync));
        p.setProperty("sound", Boolean.toString(sound));
        try (FileOutputStream out = new FileOutputStream(FILE)) {
            p.store(out, "AiMINECRAFT settings");
        } catch (IOException e) {
            System.out.println("Nie moge zapisac ustawien: " + e.getMessage());
        }
    }

    private static int clampInt(Properties p, String key, int def, int min, int max) {
        try {
            int v = Integer.parseInt(p.getProperty(key, Integer.toString(def)));
            return Math.max(min, Math.min(max, v));
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static float clampFloat(Properties p, String key, float def, float min, float max) {
        try {
            float v = Float.parseFloat(p.getProperty(key, Float.toString(def)));
            return Math.max(min, Math.min(max, v));
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
