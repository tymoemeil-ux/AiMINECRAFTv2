package com.aiminecraft;

/**
 * Punkt startowy gry.
 *
 * <p>Mozesz podac seed swiata jako argument, np. {@code 12345}.
 * Bez argumentu seed jest losowany przy kazdym uruchomieniu.</p>
 */
public class Main {

    /**
     * Punkt startowy gry. Seed wybiera sie w menu (lub losuje),
     * ale nadal mozna go podac jako argument.
     */
    public static void main(String[] args) {
        String argSeed = args.length > 0 ? args[0].trim() : "";
        new Game().run(argSeed);
    }
}
