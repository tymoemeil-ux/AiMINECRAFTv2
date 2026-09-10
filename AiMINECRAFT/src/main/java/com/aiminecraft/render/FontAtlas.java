package com.aiminecraft.render;

import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;

/**
 * Font bitmapowy 16x16 (ASCII 32-126) + szerokosci glyphow.
 * Generowany przez tools/make_assets.py.
 */
public class FontAtlas {

    public static final int GRID = 16;
    public static final int GLYPH = 16;
    public static final int SIZE = GRID * GLYPH; // 256

    private static final float STEP = 1.0f / GRID;

    private int textureId = 0;
    private final int[] widths = new int[95];

    public void load() {
        loadWidths();
        byte[] raw;
        try (InputStream in = FontAtlas.class.getResourceAsStream("/font/font.png")) {
            if (in == null) {
                throw new IllegalStateException("Brak tekstury: font.png");
            }
            raw = in.readAllBytes();
        } catch (IOException e) {
            throw new IllegalStateException("Nie mozna wczytac font.png", e);
        }
        ByteBuffer data = BufferUtils.createByteBuffer(raw.length);
        data.put(raw);
        data.flip();
        // Bez flipu: pierwszy wiersz pliku laduje na dole tekstury (v=0),
        // wiec v liczymy od gory (patrz vTop/vBottom).
        STBImage.stbi_set_flip_vertically_on_load(false);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer comp = stack.mallocInt(1);
            ByteBuffer image = STBImage.stbi_load_from_memory(data, w, h, comp, 4);
            if (image == null) {
                throw new IllegalStateException("STB nie wczytal font.png: " + STBImage.stbi_failure_reason());
            }
            if (w.get(0) != SIZE || h.get(0) != SIZE) {
                STBImage.stbi_image_free(image);
                throw new IllegalStateException("font.png musi miec 256x256 pikseli");
            }
            textureId = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, textureId);
            glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, SIZE, SIZE, 0, GL_RGBA, GL_UNSIGNED_BYTE, image);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
            glBindTexture(GL_TEXTURE_2D, 0);
            STBImage.stbi_image_free(image);
        }
    }

    private void loadWidths() {
        String text;
        try (InputStream in = FontAtlas.class.getResourceAsStream("/font/widths.txt")) {
            if (in == null) {
                throw new IllegalStateException("Brak pliku: widths.txt");
            }
            text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Nie mozna wczytac widths.txt", e);
        }
        String[] parts = text.trim().split("\\s+");
        if (parts.length < 95) {
            throw new IllegalStateException("widths.txt musi miec 95 liczb");
        }
        for (int i = 0; i < 95; i++) {
            widths[i] = Integer.parseInt(parts[i]);
        }
    }

    private static int index(char c) {
        if (c < 32 || c > 126) {
            return '?' - 32;
        }
        return c - 32;
    }

    /** Szerokosc znaku w pikselach (przy skali 1 = tekst 16px). */
    public int charWidth(char c) {
        return widths[index(c)];
    }

    public float u0(char c) {
        return (index(c) % GRID) * STEP;
    }

    public float u1(char c) {
        int w = widths[index(c)];
        // Prawa krawedz tylko do konca glypha (oszczedza rozciaganie tla).
        return (index(c) % GRID) * STEP + (w / (float) SIZE);
    }

    /** Gorna krawedz glypha (rzad liczony od gory pliku). */
    public float vTop(char c) {
        return 1.0f - (index(c) / GRID) * STEP;
    }

    /** Dolna krawedz glypha. */
    public float vBottom(char c) {
        return 1.0f - (index(c) / GRID + 1) * STEP;
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
