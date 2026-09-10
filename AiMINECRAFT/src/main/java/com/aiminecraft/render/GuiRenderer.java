package com.aiminecraft.render;

import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Rysowanie GUI: prostokaty, tekstury i tekst.
 * Wspolrzedne w pikselach, (0,0) = lewy gorny rog.
 */
public class GuiRenderer {

    /** Bajty na wierzcholek: x, y, u, v, r, g, b, a. */
    private static final int FLOATS_PER_VERTEX = 8;

    private final Shader shader;
    private final Matrix4f ortho = new Matrix4f();
    private final FloatBuffer quadBuffer = BufferUtils.createFloatBuffer(6 * FLOATS_PER_VERTEX);

    private int vao;
    private int vbo;

    public GuiRenderer() {
        shader = new Shader("/shaders/gui.vert", "/shaders/gui.frag");
        vao = glGenVertexArrays();
        vbo = glGenBuffers();
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, (long) 6 * FLOATS_PER_VERTEX * Float.BYTES, GL_DYNAMIC_DRAW);
        int stride = FLOATS_PER_VERTEX * Float.BYTES;
        glVertexAttribPointer(0, 2, GL_FLOAT, false, stride, 0);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, stride, 2L * Float.BYTES);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(2, 4, GL_FLOAT, false, stride, 4L * Float.BYTES);
        glEnableVertexAttribArray(2);
        glBindVertexArray(0);
    }

    /** Rozpoczyna rysowanie GUI (ortho + blending, bez depth testu). */
    public void begin(int width, int height) {
        ortho.setOrtho2D(0, width, height, 0);
        glDisable(GL_DEPTH_TEST);
        glDisable(GL_CULL_FACE);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        shader.bind();
        shader.setMatrix4f("uProjection", ortho);
        shader.setInt("uTex", 0);
        glBindVertexArray(vao);
    }

    /** Konczy rysowanie GUI (przywraca stan pod swiat 3D). */
    public void end() {
        glBindVertexArray(0);
        shader.unbind();
        glDisable(GL_BLEND);
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
    }

    private void quad(float x0, float y0, float x1, float y1,
                      float u0, float v0, float u1, float v1, int argb, boolean textured) {
        float a = ((argb >>> 24) & 0xFF) / 255f;
        float r = ((argb >>> 16) & 0xFF) / 255f;
        float g = ((argb >>> 8) & 0xFF) / 255f;
        float b = (argb & 0xFF) / 255f;
        shader.setInt("uUseTex", textured ? 1 : 0);
        quadBuffer.clear();
        // Dwa trojkaty: (x0,y0)-(x1,y0)-(x1,y1) i (x0,y0)-(x1,y1)-(x0,y1).
        putVertex(x0, y0, u0, v0, r, g, b, a);
        putVertex(x1, y0, u1, v0, r, g, b, a);
        putVertex(x1, y1, u1, v1, r, g, b, a);
        putVertex(x0, y0, u0, v0, r, g, b, a);
        putVertex(x1, y1, u1, v1, r, g, b, a);
        putVertex(x0, y1, u0, v1, r, g, b, a);
        quadBuffer.flip();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferSubData(GL_ARRAY_BUFFER, 0, quadBuffer);
        glDrawArrays(GL_TRIANGLES, 0, 6);
    }

    private void putVertex(float x, float y, float u, float v, float r, float g, float b, float a) {
        quadBuffer.put(x).put(y).put(u).put(v).put(r).put(g).put(b).put(a);
    }

    /** Plaski prostokat w kolorze ARGB (0xAARRGGBB). */
    public void fillRect(float x, float y, float w, float h, int argb) {
        quad(x, y, x + w, y + h, 0, 0, 1, 1, argb, false);
    }

    /** Obramowanie (grubosc w pikselach). */
    public void drawBorder(float x, float y, float w, float h, float t, int argb) {
        fillRect(x, y, w, t, argb);
        fillRect(x, y + h - t, w, t, argb);
        fillRect(x, y + t, t, h - 2 * t, argb);
        fillRect(x + w - t, y + t, t, h - 2 * t, argb);
    }

    /** Prostokat z tekstura (atlas musi byc zbindowany wczesniej). */
    public void drawTexturedRect(float x, float y, float w, float h,
                                 float u0, float v0, float u1, float v1, int argb) {
        // Dla atlasow ladowanych z flipem: v0 = dol, v1 = gora.
        quad(x, y, x + w, y + h, u0, v1, u1, v0, argb, true);
    }

    /** Ikona bloku (boczny kafel). */
    public void drawBlockIcon(TextureAtlas atlas, int tileSide, float x, float y, float size) {
        atlas.bind();
        drawTexturedRect(x, y, size, size,
                atlas.u0(tileSide), atlas.v0(tileSide),
                atlas.u1(tileSide), atlas.v1(tileSide), 0xFFFFFFFF);
    }

    /** Ikona przedmiotu (sprite z atlasu itemow). */
    public void drawItemIcon(ItemAtlas atlas, int tile, float x, float y, float size) {
        atlas.bind();
        drawTexturedRect(x, y, size, size,
                atlas.u0(tile), atlas.v0(tile),
                atlas.u1(tile), atlas.v1(tile), 0xFFFFFFFF);
    }

    /** Zamienia polskie znaki na ASCII (font ma tylko 32-126). */
    public static String normalize(String text) {
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\u0104' || c == '\u0105') { // A a z ogonkiem
                sb.append(c == '\u0104' ? 'A' : 'a');
            } else if (c == '\u0106' || c == '\u0107') { // C c z kreska
                sb.append(c == '\u0106' ? 'C' : 'c');
            } else if (c == '\u0118' || c == '\u0119') { // E e z ogonkiem
                sb.append(c == '\u0118' ? 'E' : 'e');
            } else if (c == '\u0141' || c == '\u0142') { // L l przekreslone
                sb.append(c == '\u0141' ? 'L' : 'l');
            } else if (c == '\u0143' || c == '\u0144') { // N n z kreska
                sb.append(c == '\u0143' ? 'N' : 'n');
            } else if (c == '\u00D3' || c == '\u00F3') { // O o z kreska
                sb.append(c == '\u00D3' ? 'O' : 'o');
            } else if (c == '\u015A' || c == '\u015B') { // S s z kreska
                sb.append(c == '\u015A' ? 'S' : 's');
            } else if (c == '\u0179' || c == '\u017A' || c == '\u017B' || c == '\u017C') { // Z z
                sb.append(Character.isUpperCase(c) ? 'Z' : 'z');
            } else if (c < 32 || c > 126) {
                sb.append('?');
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /** Rysuje tekst (skala 1 = 16px wysokosci). Zwraca szerokosc w pikselach. */
    public float drawText(FontAtlas font, String text, float x, float y, float scale, int argb) {
        font.bind();
        float cursor = x;
        float glyphH = FontAtlas.GLYPH * scale;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c < 32 || c > 126) {
                c = '?';
            }
            float w = font.charWidth(c) * scale;
            // Glyph: gora pliku = vTop.
            quad(cursor, y, cursor + w, y + glyphH,
                    font.u0(c), font.vBottom(c), font.u1(c), font.vTop(c), argb, true);
            cursor += w;
        }
        return cursor - x;
    }

    /** Rysuje tekst wysrodkowany. */
    public void drawCenteredText(FontAtlas font, String text, float centerX, float y, float scale, int argb) {
        drawText(font, text, centerX - textWidth(font, text, scale) / 2f, y, scale, argb);
    }

    /** Szerokosc tekstu w pikselach. */
    public float textWidth(FontAtlas font, String text, float scale) {
        float w = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c < 32 || c > 126) {
                c = '?';
            }
            w += font.charWidth(c) * scale;
        }
        return w;
    }

    /** Tekst z cieniem (jak w MC). */
    public void drawTextShadow(FontAtlas font, String text, float x, float y, float scale, int argb) {
        int shadow = (argb & 0x00FFFFFF) | 0xAA000000;
        drawText(font, text, x + 2 * scale, y + 2 * scale, scale, shadow);
        drawText(font, text, x, y, scale, argb);
    }

    public void close() {
        glDeleteBuffers(vbo);
        glDeleteVertexArrays(vao);
        shader.close();
    }
}
