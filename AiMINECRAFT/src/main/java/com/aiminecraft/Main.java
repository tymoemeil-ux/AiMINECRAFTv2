package com.aiminecraft;

/**
 * Punkt startowy gry.
 *
 * <p>Mozesz podac seed swiata jako argument, np. {@code 12345}.
 * Bez argumentu seed jest losowany przy kazdym uruchomieniu.</p>
 */
public class Main {

    public static void main(String[] args) {
        long seed = System.currentTimeMillis();
        if (args.length > 0) {
            try {
                seed = Long.parseLong(args[0].trim());
            } catch (NumberFormatException e) {
                seed = args[0].hashCode();
            }
        }
        System.out.println("Seed swiata: " + seed);
        new Game(seed).run();
    }
}
