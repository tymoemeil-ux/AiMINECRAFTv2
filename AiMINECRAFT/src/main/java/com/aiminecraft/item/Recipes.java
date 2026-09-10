package com.aiminecraft.item;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Receptury craftingu (ksztaltowe). Siatka 3x3 (rzedy),
 * '.' = pusto. Dopasowanie po przycieciu pustych brzegow.
 */
public final class Recipes {

    public static final class Recipe {
        public final String[] shape; // np. {"PPP", ".S.", ".S."}
        public final int resultId;
        public final int resultCount;
        public final String name;

        public Recipe(String name, String[] shape, int resultId, int resultCount) {
            this.name = name;
            this.shape = shape;
            this.resultId = resultId;
            this.resultCount = resultCount;
        }
    }

    private static final List<Recipe> ALL = new ArrayList<>();

    static {
        // Litery: L = pien (dowolny), P = deski, C = bruk,
        // S = patyk, W = wegiel.
        // Deski z pnia (3 warianty).
        ALL.add(new Recipe("Deski", new String[]{"L"}, 10, 4));
        // Patyki.
        ALL.add(new Recipe("Patyki", new String[]{"P", "P"}, 100, 4));
        // Stol rzemieslniczy.
        ALL.add(new Recipe("Stol rzemieslniczy", new String[]{"PP", "PP"}, 26, 1));
        // Pochodnie.
        ALL.add(new Recipe("Pochodnie", new String[]{"W", "S"}, 25, 4));
        // Narzedzia drewniane.
        ALL.add(new Recipe("Kilof drewniany", new String[]{"PPP", ".S.", ".S."}, 110, 1));
        ALL.add(new Recipe("Siekiera drewniana", new String[]{"PP.", "PS.", ".S."}, 111, 1));
        ALL.add(new Recipe("Lopata drewniana", new String[]{"P", "S", "S"}, 112, 1));
        ALL.add(new Recipe("Miecz drewniany", new String[]{"P", "P", "S"}, 113, 1));
        ALL.add(new Recipe("Motyka drewniana", new String[]{"PP", ".S", ".S"}, 114, 1));
        // Narzedzia kamienne.
        ALL.add(new Recipe("Kilof kamienny", new String[]{"CCC", ".S.", ".S."}, 120, 1));
        ALL.add(new Recipe("Siekiera kamienna", new String[]{"CC.", "CS.", ".S."}, 121, 1));
        ALL.add(new Recipe("Lopata kamienna", new String[]{"C", "S", "S"}, 122, 1));
        ALL.add(new Recipe("Miecz kamienny", new String[]{"C", "C", "S"}, 123, 1));
        ALL.add(new Recipe("Motyka kamienna", new String[]{"CC", ".S", ".S"}, 124, 1));
    }

    public static List<Recipe> all() {
        return ALL;
    }

    /** Sprawdza, czy id pasuje do litery receptury. */
    public static boolean matches(char key, int id) {
        switch (key) {
            case 'L': return id == 4 || id == 17 || id == 19; // pnie
            case 'P': return id == 10; // deski
            case 'C': return id == 9;  // bruk
            case 'S': return id == 100; // patyk
            case 'W': return id == 101; // wegiel
            default: return false;
        }
    }

    /**
     * Dopasowuje recepture do siatki 3x3 (0 = pusto).
     * Zwraca recepture albo null.
     */
    public static Recipe match(int[] grid) {
        // Przytnij siatke do najmniejszego prostokata z itemami.
        int minX = 3, minY = 3, maxX = -1, maxY = -1;
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                if (grid[y * 3 + x] != 0) {
                    if (x < minX) minX = x;
                    if (y < minY) minY = y;
                    if (x > maxX) maxX = x;
                    if (y > maxY) maxY = y;
                }
            }
        }
        if (maxX < 0) {
            return null; // pusto
        }
        int w = maxX - minX + 1;
        int h = maxY - minY + 1;
        for (Recipe r : ALL) {
            if (r.shape.length != h) {
                continue;
            }
            if (r.shape[0].length() != w) {
                continue;
            }
            boolean ok = true;
            for (int y = 0; y < h && ok; y++) {
                for (int x = 0; x < w; x++) {
                    char key = r.shape[y].charAt(x);
                    int id = grid[(minY + y) * 3 + (minX + x)];
                    if (key == '.') {
                        if (id != 0) {
                            ok = false;
                            break;
                        }
                    } else if (!matches(key, id)) {
                        ok = false;
                        break;
                    }
                }
            }
            if (ok) {
                return r;
            }
        }
        return null;
    }

    /** Ikona podgladu dla litery (do ksiazki receptur). */
    public static int iconFor(char key) {
        switch (key) {
            case 'L': return 4;
            case 'P': return 10;
            case 'C': return 9;
            case 'S': return 100;
            case 'W': return 101;
            default: return 0;
        }
    }

    /** Czy receptura miesci sie w siatce 2x2 (crafting w ekwipunku). */
    public static boolean fits2x2(Recipe r) {
        return r.shape.length <= 2 && r.shape[0].length() <= 2;
    }
}
