package com.aiminecraft.world;

/**
 * Wszystkie bloki w grze.
 *
 * <p>Kazdy blok ma: id, flagi (nieprzezroczysty / ma kolizje),
 * indeksy kafli w atlasie tekstur (gora / dol / bok) i polska nazwe.</p>
 */
public enum Block {

    AIR(0, false, false, -1, -1, -1, "Powietrze"),
    GRASS(1, true, true, 0, 2, 1, "Trawa"),
    DIRT(2, true, true, 2, 2, 2, "Ziemia"),
    STONE(3, true, true, 3, 3, 3, "Kamien"),
    OAK_LOG(4, true, true, 5, 5, 4, "Drewno"),
    OAK_LEAVES(5, true, true, 6, 6, 6, "Liscie"),
    BEDROCK(6, true, true, 7, 7, 7, "Bedrock");

    public final byte id;
    public final boolean opaque;
    public final boolean solid;
    public final int tileTop;
    public final int tileBottom;
    public final int tileSide;
    public final String displayName;

    Block(int id, boolean opaque, boolean solid,
          int tileTop, int tileBottom, int tileSide, String displayName) {
        this.id = (byte) id;
        this.opaque = opaque;
        this.solid = solid;
        this.tileTop = tileTop;
        this.tileBottom = tileBottom;
        this.tileSide = tileSide;
        this.displayName = displayName;
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
