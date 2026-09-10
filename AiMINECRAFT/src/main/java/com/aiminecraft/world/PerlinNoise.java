package com.aiminecraft.world;

import java.util.Random;

/**
 * Klasyczny szum Perlina 2D i 3D + fbm.
 * Deterministyczny: ten sam seed = ten sam swiat.
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

    private static double grad2(int hash, double x, double y) {
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

    private static double grad3(int hash, double x, double y, double z) {
        int h = hash & 15;
        double u = h < 8 ? x : y;
        double v = h < 4 ? y : (h == 12 || h == 14 ? x : z);
        return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
    }

    /** Szum 2D w zakresie okolo [-1, 1]. */
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
                lerp(grad2(aa, x, y), grad2(ba, x - 1, y), u),
                lerp(grad2(ab, x, y - 1), grad2(bb, x - 1, y - 1), u),
                v);
    }

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

    /** Szum 3D w zakresie okolo [-1, 1] (jaskinie). */
    public double noise3(double x, double y, double z) {
        int xi = (int) Math.floor(x);
        int yi = (int) Math.floor(y);
        int zi = (int) Math.floor(z);
        int X = xi & 255;
        int Y = yi & 255;
        int Z = zi & 255;
        x -= xi;
        y -= yi;
        z -= zi;
        double u = fade(x);
        double v = fade(y);
        double w = fade(z);
        int aaa = p[p[p[X] + Y] + Z];
        int aba = p[p[p[X] + Y + 1] + Z];
        int aab = p[p[p[X] + Y] + Z + 1];
        int abb = p[p[p[X] + Y + 1] + Z + 1];
        int baa = p[p[p[X + 1] + Y] + Z];
        int bba = p[p[p[X + 1] + Y + 1] + Z];
        int bab = p[p[p[X + 1] + Y] + Z + 1];
        int bbb = p[p[p[X + 1] + Y + 1] + Z + 1];
        return lerp(
                lerp(
                        lerp(grad3(aaa, x, y, z), grad3(baa, x - 1, y, z), u),
                        lerp(grad3(aba, x, y - 1, z), grad3(bba, x - 1, y - 1, z), u),
                        v),
                lerp(
                        lerp(grad3(aab, x, y, z - 1), grad3(bab, x - 1, y, z - 1), u),
                        lerp(grad3(abb, x, y - 1, z - 1), grad3(bbb, x - 1, y - 1, z - 1), u),
                        v),
                w);
    }

    public double fbm3(double x, double y, double z, int octaves, double lacunarity, double gain) {
        double amplitude = 1;
        double frequency = 1;
        double sum = 0;
        double normalization = 0;
        for (int i = 0; i < octaves; i++) {
            sum += amplitude * noise3(x * frequency, y * frequency, z * frequency);
            normalization += amplitude;
            amplitude *= gain;
            frequency *= lacunarity;
        }
        return sum / normalization;
    }

    /** Deterministyczny hash 2D -> [0, 1). */
    public static double hash01(int x, int z, long seed) {
        long h = seed + (long) x * 0x9E3779B97F4A7C15L + (long) z * 0xBF58476D1CE4E5B9L;
        h ^= h >>> 30;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 27;
        h *= 0x94D049BB133111EBL;
        h ^= h >>> 31;
        return ((h >>> 11) & ((1L << 53) - 1)) / (double) (1L << 53);
    }

    /** Deterministyczny hash 3D -> [0, 1) (rudy). */
    public static double hash3(int x, int y, int z, long seed) {
        long h = seed + (long) x * 0x9E3779B97F4A7C15L
                + (long) y * 0xC2B2AE3D27D4EB4FL + (long) z * 0x165667B19E3779F9L;
        h ^= h >>> 29;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 32;
        h *= 0x94D049BB133111EBL;
        h ^= h >>> 31;
        return ((h >>> 11) & ((1L << 53) - 1)) / (double) (1L << 53);
    }
}
