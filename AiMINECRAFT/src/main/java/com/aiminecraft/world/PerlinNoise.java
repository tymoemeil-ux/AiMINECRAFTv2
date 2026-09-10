package com.aiminecraft.world;

import java.util.Random;

/**
 * Klasyczny szum Perlina 2D + fraktalne skladanie (fbm).
 * Deterministyczny: ten sam seed zawsze daje ten sam teren.
 */
public final class PerlinNoise {

    private final int[] p = new int[512];

    public PerlinNoise(long seed) {
        int[] perm = new int[256];
        for (int i = 0; i < 256; i++) {
            perm[i] = i;
        }
        Random random = new Random(seed);
        for (int i = 255; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int tmp = perm[i];
            perm[i] = perm[j];
            perm[j] = tmp;
        }
        for (int i = 0; i < 512; i++) {
            p[i] = perm[i & 255];
        }
    }

    private static double fade(double t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    private static double lerp(double a, double b, double t) {
        return a + t * (b - a);
    }

    private static double grad(int hash, double x, double y) {
        switch (hash & 7) {
            case 0: return x + y;
            case 1: return x - y;
            case 2: return -x + y;
            case 3: return -x - y;
            case 4: return x;
            case 5: return -x;
            case 6: return y;
            default: return -y;
        }
    }

    /** Szum w zakresie okolo [-1, 1]. */
    public double noise(double x, double y) {
        int xi = (int) Math.floor(x);
        int yi = (int) Math.floor(y);
        int X = xi & 255;
        int Y = yi & 255;
        x -= xi;
        y -= yi;
        double u = fade(x);
        double v = fade(y);
        int aa = p[p[X] + Y];
        int ab = p[p[X] + Y + 1];
        int ba = p[p[X + 1] + Y];
        int bb = p[p[X + 1] + Y + 1];
        return lerp(
                lerp(grad(aa, x, y), grad(ba, x - 1, y), u),
                lerp(grad(ab, x, y - 1), grad(bb, x - 1, y - 1), u),
                v);
    }

    /** Kilka oktaw szumu nalozenych na siebie (fraktal). */
    public double fbm(double x, double y, int octaves, double lacunarity, double gain) {
        double amplitude = 1;
        double frequency = 1;
        double sum = 0;
        double normalization = 0;
        for (int i = 0; i < octaves; i++) {
            sum += amplitude * noise(x * frequency, y * frequency);
            normalization += amplitude;
            amplitude *= gain;
            frequency *= lacunarity;
        }
        return sum / normalization;
    }

    /** Deterministyczny hash 2D -> [0, 1). Szybki i stabilny, idealny np. do drzew. */
    public static double hash01(int x, int z, long seed) {
        long h = seed + (long) x * 0x9E3779B97F4A7C15L + (long) z * 0xBF58476D1CE4E5B9L;
        h ^= h >>> 30;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 27;
        h *= 0x94D049BB133111EBL;
        h ^= h >>> 31;
        return ((h >>> 11) & ((1L << 53) - 1)) / (double) (1L << 53);
    }
}
