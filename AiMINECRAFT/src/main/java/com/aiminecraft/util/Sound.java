package com.aiminecraft.util;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/** Proste dzwieki WAV (javax.sound, bez dodatkowych bibliotek). */
public final class Sound {

    private static final Map<String, Clip> CLIPS = new HashMap<>();
    private static boolean enabled = true;

    private Sound() {
    }

    public static void setEnabled(boolean enabled) {
        Sound.enabled = enabled;
    }

    /** Odtwarza dzwiek (click, break, place, pickup, splash). */
    public static void play(String name) {
        if (!enabled) {
            return;
        }
        try {
            Clip clip = CLIPS.get(name);
            if (clip == null) {
                clip = load("/sounds/" + name + ".wav");
                if (clip == null) {
                    return;
                }
                CLIPS.put(name, clip);
            }
            if (clip.isRunning()) {
                clip.stop();
            }
            clip.setFramePosition(0);
            clip.start();
        } catch (Exception e) {
            // dzwiek nie moze wywalic gry
        }
    }

    private static Clip load(String path) {
        try (InputStream raw = Sound.class.getResourceAsStream(path)) {
            if (raw == null) {
                return null;
            }
            try (AudioInputStream in = AudioSystem.getAudioInputStream(new BufferedInputStream(raw))) {
                Clip clip = AudioSystem.getClip();
                clip.open(in);
                return clip;
            }
        } catch (Exception e) {
            return null;
        }
    }
}
