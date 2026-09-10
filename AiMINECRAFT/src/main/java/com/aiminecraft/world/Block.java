package com.aiminecraft.world;

import com.aiminecraft.item.Tool;

/**
 * Wszystkie bloki w grze.
 * Kafelki: indeksy w atlasie 8x8 (TextureAtlas).
 */
public enum Block {

    AIR(0, false, false, Kind.CUBE, -1, -1, -1, "Powietrze", 0f, Tool.NONE, 0),
    GRASS(1, true, true, Kind.CUBE, 0, 2, 1, "Trawa", 0.5f, Tool.SHOVEL, 2),
    DIRT(2, true, true, Kind.CUBE, 2, 2, 2, "Ziemia", 0.5f, Tool.SHOVEL, 2),
    STONE(3, true, true, Kind.CUBE, 3, 3, 3, "Kamien", 2.0f, Tool.PICKAXE, 9),
    OAK_LOG(4, true, true, Kind.CUBE, 5, 5, 4, "Pien debu", 1.2f, Tool.AXE, 4),
    OAK_LEAVES(5, true, true, Kind.CUBE, 6, 6, 6, "Liscie debu", 0.25f, Tool.HOE, 0),
    BEDROCK(6, true, true, Kind.CUBE, 7, 7, 7, "Bedrock", Float.MAX_VALUE, Tool.NONE, 0),
    WATER(7, false, false, Kind.WATER, 8, 8, 8, "Woda", Float.MAX_VALUE, Tool.NONE, 0),
    SAND(8, true, true, Kind.CUBE, 9, 9, 9, "Piasek", 0.5f, Tool.SHOVEL, 8),
    COBBLESTONE(9, true, true, Kind.CUBE, 10, 10, 10, "Bruk", 2.0f, Tool.PICKAXE, 9),
    PLANKS(10, true, true, Kind.CUBE, 11, 11, 11, "Deski", 1.2f, Tool.AXE, 10),
    GLASS(11, false, true, Kind.CUBE, 12, 12, 12, "Szyba", 0.4f, Tool.NONE, 0),
    COAL_ORE(12, true, true, Kind.CUBE, 13, 13, 13, "Ruda wegla", 2.2f, Tool.PICKAXE, 101),
    IRON_ORE(13, true, true, Kind.CUBE, 14, 14, 14, "Ruda zelaza", 2.4f, Tool.PICKAXE, 102),
    GOLD_ORE(14, true, true, Kind.CUBE, 15, 15, 15, "Ruda zlota", 2.4f, Tool.PICKAXE, 103),
    DIAMOND_ORE(15, true, true, Kind.CUBE, 16, 16, 16, "Ruda diamentu", 2.6f, Tool.PICKAXE, 104),
    REDSTONE_ORE(16, true, true, Kind.CUBE, 17, 17, 17, "Ruda redstone", 2.4f, Tool.PICKAXE, 105),
    BIRCH_LOG(17, true, true, Kind.CUBE, 19, 19, 18, "Pien brzozy", 1.2f, Tool.AXE, 17),
    BIRCH_LEAVES(18, true, true, Kind.CUBE, 20, 20, 20, "Liscie brzozy", 0.25f, Tool.HOE, 0),
    SPRUCE_LOG(19, true, true, Kind.CUBE, 22, 22, 21, "Pien swierku", 1.2f, Tool.AXE, 19),
    SPRUCE_LEAVES(20, true, true, Kind.CUBE, 23, 23, 23, "Liscie swierku", 0.25f, Tool.HOE, 0),
    SNOW(21, true, true, Kind.CUBE, 24, 24, 24, "Snieg", 0.4f, Tool.SHOVEL, 21),
    DANDELION(22, false, false, Kind.CROSS, 25, 25, 25, "Mlecz", 0.1f, Tool.NONE, 22),
    POPPY(23, false, false, Kind.CROSS, 26, 26, 26, "Mak", 0.1f, Tool.NONE, 23),
    SHORT_GRASS(24, false, false, Kind.CROSS, 27, 27, 27, "Wysoka trawa", 0.1f, Tool.NONE, 0),
    TORCH(25, false, false, Kind.TORCH, 28, 28, 28, "Pochodnia", 0.1f, Tool.NONE, 25),
    CRAFTING_TABLE(26, true, true, Kind.CUBE, 29, 11, 30, "Stol rzemieslniczy", 1.2f, Tool.AXE, 26),
    CACTUS(27, true, true, Kind.CUBE, 32, 32, 31, "Kaktus", 0.5f, Tool.SWORD, 27),
    GRAVEL(28, true, true, Kind.CUBE, 33, 33, 33, "Zwir", 0.5f, Tool.SHOVEL, 28),
    OBSIDIAN(29, true, true, Kind.CUBE, 34, 34, 34, "Obsydian", 12.0f, Tool.PICKAXE, 29),
    ICE(30, true, true, Kind.CUBE, 35, 35, 35, "Lod", 0.6f, Tool.PICKAXE, 0),
    BOOKSHELF(31, true, true, Kind.CUBE, 11, 11, 36, "Biblioteczka", 1.2f, Tool.AXE, 31),
    PUMPKIN(32, true, true, Kind.CUBE, 38, 38, 37, "Dynia", 0.8f, Tool.AXE, 32),
    MELON(33, true, true, Kind.CUBE, 40, 40, 39, "Arbuz", 0.8f, Tool.AXE, 33),
    SPONGE(34, true, true, Kind.CUBE, 41, 41, 41, "Gabka", 0.6f, Tool.HOE, 34),
    BRICKS(35, true, true, Kind.CUBE, 42, 42, 42, "Cegly", 2.0f, Tool.PICKAXE, 35);

    public enum Kind {
        CUBE, CROSS, TORCH, WATER
    }

    public final byte id;
    public final boolean opaque;
    public final boolean solid;
    public final Kind kind;
    public final int tileTop;
    public final int tileBottom;
    public final int tileSide;
    public final String displayName;
    /** Czas kopania reka w survivalu (sekundy). */
    public final float hardness;
    /** Narzedzie, ktore kopie 4x szybciej. */
    public final Tool tool;
    /** Co wypada po zniszczeniu (id bloku/itemu, 0 = nic). */
    public final int dropId;

    Block(int id, boolean opaque, boolean solid, Kind kind,
          int tileTop, int tileBottom, int tileSide, String displayName,
          float hardness, Tool tool, int dropId) {
        this.id = (byte) id;
        this.opaque = opaque;
        this.solid = solid;
        this.kind = kind;
        this.tileTop = tileTop;
        this.tileBottom = tileBottom;
        this.tileSide = tileSide;
        this.displayName = displayName;
        this.hardness = hardness;
        this.tool = tool;
        this.dropId = dropId;
    }

    public boolean isLeaves() {
        return this == OAK_LEAVES || this == BIRCH_LEAVES || this == SPRUCE_LEAVES;
    }

    public boolean isLog() {
        return this == OAK_LOG || this == BIRCH_LOG || this == SPRUCE_LOG;
    }

    private static final Block[] BY_ID = new Block[256];

    static {
        for (Block block : values()) {
            BY_ID[block.id & 0xFF] = block;
        }
    }

    public static Block fromId(int id) {
        if (id < 0 || id >= BY_ID.length) {
            return AIR;
        }
        Block block = BY_ID[id];
        return block != null ? block : AIR;
    }
}
