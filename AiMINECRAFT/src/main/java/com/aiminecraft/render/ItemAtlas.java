package com.aiminecraft.render;

import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;

/**
 * Atlas ikon przedmiotow: 16 sprite'ow 16x16 w siatce 4x4.
 * Kolejnosc MUSI zgadzac sie z tools/make_assets.py (ITEM_JOBS).
 */
public class ItemAtlas {

    public static final int TILE_SIZE = 16;
    public static final int GRID = 4;
    public static final int ATLAS_SIZE = TILE_SIZE * GRID;

    private static final String[] TILE_FILES = {
            "stick.png",          // 0  -> id 100
            "coal.png",           // 1  -> id 101
            "raw_iron.png",       // 2  -> id 102
            "raw_gold.png",       // 3  -> id 103
            "diamond.png",        // 4  -> id 104
            "redstone.png",       // 5  -> id 105
            "wooden_pickaxe.png", // 6  -> id 110
            "wooden_axe.png",     // 7  -> id 111
            "wooden_shovel.png",  // 8  -> id 112
            "wooden_sword.png",   // 9  -> id 113
            "wooden_hoe.png",     // 10 -> id 114
            "stone_pickaxe.png",  // 11 -> id 120
            "stone_axe.png",      // 12 -> id 121
            "stone_shovel.png",   // 13 -> id 122
            "stone_sword.png",    // 14 -> id 123
            "stone_hoe.png"       // 15 -> id 124
    };

    private static final float STEP = 1.0f / GRID;
    private static final float INSET = 0.5f / ATLAS_SIZE;

    private int textureId = 0;

    /** Numer kafla dla id przedmiotu (100-105, 110-114, 120-124). */
    public static int tileFor(int itemId) {
        if (itemId >= 100 && itemId <= 105) {
            return itemId - 100;
        }
        if (itemId >= 110 && itemId <= 114) {
            return 6 + (itemId - 110);
        }
        if (itemId >= 120 && itemId <= 124) {
            return 11 + (itemId - 120);
        }
        return 0;
    }

    public void load() {
        ByteBuffer atlas = BufferUtils.createByteBuffer(ATLAS_SIZE * ATLAS_SIZE * 4);
        STBImage.stbi_set_flip_vertically_on_load(true);

        for (int i = 0; i < TILE_FILES.length; i++) {
            ByteBuffer tile = loadTile(TILE_FILES[i]);
            int col = i % GRID;
            int rowFromBottom = (GRID - 1) - i / GRID;
            for (int y = 0; y < TILE_SIZE; y++) {
                for (int x = 0; x < TILE_SIZE; x++) {
                    int src = (y * TILE_SIZE + x) * 4;
                    int dst = (((rowFromBottom * TILE_SIZE + y) * ATLAS_SIZE) + (col * TILE_SIZE + x)) * 4;
                    atlas.put(dst, tile.get(src));
                    atlas.put(dst + 1, tile.get(src + 1));
                    atlas.put(dst + 2, tile.get(src + 2));
                    atlas.put(dst + 3, tile.get(src + 3));
                }
            }
            STBImage.stbi_image_free(tile);
        }

        atlas.rewind();
        textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);
        glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, ATLAS_SIZE, ATLAS_SIZE, 0, GL_RGBA, GL_UNSIGNED_BYTE, atlas);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    private static ByteBuffer loadTile(String fileName) {
        byte[] raw;
        try (InputStream in = ItemAtlas.class.getResourceAsStream("/textures/items/" + fileName)) {
            if (in == null) {
                throw new IllegalStateException("Brak tekstury: " + fileName);
            }
            raw = in.readAllBytes();
        } catch (IOException e) {
            throw new IllegalStateException("Nie mozna wczytac tekstury: " + fileName, e);
        }
        ByteBuffer data = BufferUtils.createByteBuffer(raw.length);
        data.put(raw);
        data.flip();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer comp = stack.mallocInt(1);
            ByteBuffer image = STBImage.stbi_load_from_memory(data, w, h, comp, 4);
            if (image == null) {
                throw new IllegalStateException("STB nie wczytal tekstury " + fileName + ": " + STBImage.stbi_failure_reason());
            }
            if (w.get(0) != TILE_SIZE || h.get(0) != TILE_SIZE) {
                STBImage.stbi_image_free(image);
                throw new IllegalStateException("Tekstura " + fileName + " musi miec 16x16 pikseli");
            }
            return image;
        }
    }

    public float u0(int tile) {
        return (tile % GRID) * STEP + INSET;
    }

    public float u1(int tile) {
        return (tile % GRID) * STEP + STEP - INSET;
    }

    public float v0(int tile) {
        return ((GRID - 1) - tile / GRID) * STEP + INSET;
    }

    public float v1(int tile) {
        return ((GRID - 1) - tile / GRID) * STEP + STEP - INSET;
    }

    public void bind() {
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, textureId);
    }

    public void close() {
        if (textureId != 0) {
            glDeleteTextures(textureId);
            textureId = 0;
        }
    }
}
