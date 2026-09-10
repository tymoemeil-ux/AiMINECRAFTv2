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
 * Font bitmapowy (ASCII 32-126, 95 glyphow) + szerokosci glyphow.
 * Generowany przez tools/make_assets.py jako 256x96 (16 kolumn x 6 rzedow).
 */
public class FontAtlas {

    public static final int GRID = 16;
    public static final int GLYPH = 16;
    public static final int WIDTH = GRID * GLYPH; // 256

    private static final float STEP_U = 1.0f / GRID;

    private int textureId = 0;
    /** Liczba rzedow w teksturze (95 glyphow potrzebuje co najmniej 6). */
    private int rows = 6;
    private float stepV = 1.0f / 6;
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
        // Bez flipu: wiersz 0 pliku (gora) laduje na t=0, czyli t ROSNIE
        // w dol obrazu. Stad vTop = rzedzie * stepV (patrz vTop/vBottom).
        STBImage.stbi_set_flip_vertically_on_load(false);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer comp = stack.mallocInt(1);
            ByteBuffer image = STBImage.stbi_load_from_memory(data, w, h, comp, 4);
            if (image == null) {
                throw new IllegalStateException("STB nie wczytal font.png: " + STBImage.stbi_failure_reason());
            }
            int width = w.get(0);
            int height = h.get(0);
            if (width != WIDTH || height % GLYPH != 0 || height / GLYPH < 6) {
                STBImage.stbi_image_free(image);
                throw new IllegalStateException("font.png ma zly rozmiar " + width + "x" + height
                        + " (oczekiwano 256 x wielokrotnosc 16, min. 256x96)");
            }
            rows = height / GLYPH;
            stepV = 1.0f / rows;
            textureId = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, textureId);
            glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, image);
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
        return (index(c) % GRID) * STEP_U;
    }

    public float u1(char c) {
        int w = widths[index(c)];
        // Prawa krawedz tylko do konca glypha (oszczedza rozciaganie tla).
        return (index(c) % GRID) * STEP_U + (w / (float) WIDTH);
    }

    /** T krawedzi GORNEJ glypha (t=0 to gora pliku, bo ladujemy bez flipu). */
    public float vTop(char c) {
        return (index(c) / GRID) * stepV;
    }

    /** T krawedzi DOLNEJ glypha. */
    public float vBottom(char c) {
        return (index(c) / GRID + 1) * stepV;
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
