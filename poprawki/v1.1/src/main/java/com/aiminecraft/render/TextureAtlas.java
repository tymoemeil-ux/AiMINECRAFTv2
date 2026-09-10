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
 * Atlas tekstur: 8 obrazkow 16x16 laczonych w jedna teksture 64x64.
 * Zeby podmienic teksture, wystarczy nadpisac PNG w resources/textures/blocks.
 */
public class TextureAtlas {

    public static final int TILE_SIZE = 16;
    public static final int GRID = 4;
    public static final int ATLAS_SIZE = TILE_SIZE * GRID;

    /** Kolejnosc = indeksy kafli uzywane w Block. */
    private static final String[] TILE_FILES = {
            "grass_top.png",   // 0
            "grass_side.png",  // 1
            "dirt.png",        // 2
            "stone.png",       // 3
            "oak_log_side.png", // 4
            "oak_log_top.png",  // 5
            "oak_leaves.png",   // 6
            "bedrock.png"       // 7
    };

    private static final float STEP = 1.0f / GRID;
    private static final float INSET = 0.5f / ATLAS_SIZE;

    private int textureId = 0;

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

        // Bufor byl wypelniany zapisami absolutnymi (put(i, v)), wiec cofamy
        // tylko pozycje - flip() by tu wyzerowal limit (zostalby pusty bufor).
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
        try (InputStream in = TextureAtlas.class.getResourceAsStream("/textures/blocks/" + fileName)) {
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

    /** Wspolrzedne UV kafla w atlasie (z malym marginesem). */
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
