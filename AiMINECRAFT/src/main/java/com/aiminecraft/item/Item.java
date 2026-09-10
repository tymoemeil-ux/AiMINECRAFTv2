package com.aiminecraft.item;

import com.aiminecraft.world.Block;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Rejestr przedmiotow. Bloki (id 1-63) sa tez itemami,
 * reszta (100+) to materialy i narzedzia.
 */
public final class Item {

    public static final int ATLAS_BLOCKS = 0;
    public static final int ATLAS_ITEMS = 1;

    public final int id;
    public final String name;
    public final int iconAtlas;
    public final int iconTile;
    /** Jaki blok stawia (-1 = nie stawia). */
    public final int placeBlock;
    public final Tool tool;

    private Item(int id, String name, int iconAtlas, int iconTile, int placeBlock, Tool tool) {
        this.id = id;
        this.name = name;
        this.iconAtlas = iconAtlas;
        this.iconTile = iconTile;
        this.placeBlock = placeBlock;
        this.tool = tool;
    }

    private static final Map<Integer, Item> BY_ID = new HashMap<>();

    static {
        // Bloki (oprocz powietrza i wody - wody nie da sie nabrac bez wiadra).
        for (Block b : Block.values()) {
            int id = b.id & 0xFF;
            if (id == 0 || b == Block.WATER) {
                continue;
            }
            BY_ID.put(id, new Item(id, b.displayName, ATLAS_BLOCKS, b.tileSide, id, Tool.NONE));
        }
        // Materialy (sprite'y w atlasie itemow).
        reg(100, "Patyk", 0, Tool.NONE);
        reg(101, "Wegiel", 1, Tool.NONE);
        reg(102, "Surowe zelazo", 2, Tool.NONE);
        reg(103, "Surowe zloto", 3, Tool.NONE);
        reg(104, "Diament", 4, Tool.NONE);
        reg(105, "Redstone", 5, Tool.NONE);
        // Narzedzia drewniane.
        reg(110, "Kilof drewniany", 6, Tool.PICKAXE);
        reg(111, "Siekiera drewniana", 7, Tool.AXE);
        reg(112, "Lopata drewniana", 8, Tool.SHOVEL);
        reg(113, "Miecz drewniany", 9, Tool.SWORD);
        reg(114, "Motyka drewniana", 10, Tool.HOE);
        // Narzedzia kamienne.
        reg(120, "Kilof kamienny", 11, Tool.PICKAXE);
        reg(121, "Siekiera kamienna", 12, Tool.AXE);
        reg(122, "Lopata kamienna", 13, Tool.SHOVEL);
        reg(123, "Miecz kamienny", 14, Tool.SWORD);
        reg(124, "Motyka kamienna", 15, Tool.HOE);
    }

    private static void reg(int id, String name, int sprite, Tool tool) {
        BY_ID.put(id, new Item(id, name, ATLAS_ITEMS, sprite, -1, tool));
    }

    public static Item get(int id) {
        return BY_ID.get(id);
    }

    /** Wszystkie itemy do zakladki kreatywnej (bloki, potem materialy i narzedzia). */
    public static List<Item> creativeList() {
        List<Item> blocks = new ArrayList<>();
        List<Item> other = new ArrayList<>();
        for (Item it : BY_ID.values()) {
            if (it.placeBlock >= 0) {
                blocks.add(it);
            } else {
                other.add(it);
            }
        }
        blocks.sort((a, b) -> Integer.compare(a.id, b.id));
        other.sort((a, b) -> Integer.compare(a.id, b.id));
        blocks.addAll(other);
        return blocks;
    }
}
