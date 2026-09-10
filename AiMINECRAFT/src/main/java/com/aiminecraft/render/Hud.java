package com.aiminecraft.render;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * HUD rysowany liniami: celownik (+) i ramka wokol celowanego bloku.
 */
public class Hud {

    private static final Vector3f WHITE = new Vector3f(1, 1, 1);
    private static final Vector3f DARK = new Vector3f(0.05f, 0.05f, 0.05f);

    private final Shader lineShader;
    private final Matrix4f mvp = new Matrix4f();

    private int crossVao;
    private int crossVbo;
    private int boxVao;
    private int boxVbo;

    public Hud() {
        lineShader = new Shader("/shaders/line.vert", "/shaders/line.frag");

        crossVao = glGenVertexArrays();
        crossVbo = glGenBuffers();
        glBindVertexArray(crossVao);
        glBindBuffer(GL_ARRAY_BUFFER, crossVbo);
        glBufferData(GL_ARRAY_BUFFER, 12L * Float.BYTES, GL_DYNAMIC_DRAW);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
        glEnableVertexAttribArray(0);
        glBindVertexArray(0);

        float a = -0.002f;
        float b = 1.002f;
        float[] edges = {
                // dol
                a, a, a, b, a, a,
                b, a, a, b, a, b,
                b, a, b, a, a, b,
                a, a, b, a, a, a,
                // gora
                a, b, a, b, b, a,
                b, b, a, b, b, b,
                b, b, b, a, b, b,
                a, b, b, a, b, a,
                // piony
                a, a, a, a, b, a,
                b, a, a, b, b, a,
                b, a, b, b, b, b,
                a, a, b, a, b, b,
        };
        boxVao = glGenVertexArrays();
        boxVbo = glGenBuffers();
        glBindVertexArray(boxVao);
        glBindBuffer(GL_ARRAY_BUFFER, boxVbo);
        FloatBuffer boxBuffer = BufferUtils.createFloatBuffer(edges.length);
        boxBuffer.put(edges);
        boxBuffer.flip();
        glBufferData(GL_ARRAY_BUFFER, boxBuffer, GL_STATIC_DRAW);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
        glEnableVertexAttribArray(0);
        glBindVertexArray(0);
    }

    /** Przelicza celownik na wspolrzedne ekranu (piksele -> NDC). */
    public void onResize(int width, int height) {
        float nx = 18.0f / width;
        float ny = 18.0f / height;
        FloatBuffer buffer = BufferUtils.createFloatBuffer(12);
        buffer.put(new float[]{-nx, 0, 0, nx, 0, 0, 0, -ny, 0, 0, ny, 0});
        buffer.flip();
        glBindBuffer(GL_ARRAY_BUFFER, crossVbo);
        glBufferData(GL_ARRAY_BUFFER, buffer, GL_DYNAMIC_DRAW);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
    }

    public void drawCrosshair() {
        glDisable(GL_DEPTH_TEST);
        lineShader.bind();
        lineShader.setMatrix4f("uMVP", mvp.identity());
        lineShader.setVector3f("uColor", WHITE);
        glBindVertexArray(crossVao);
        glLineWidth(2.0f);
        glDrawArrays(GL_LINES, 0, 4);
        glBindVertexArray(0);
        lineShader.unbind();
        glEnable(GL_DEPTH_TEST);
    }

    public void drawHighlight(Matrix4f projection, Matrix4f view, int x, int y, int z) {
        lineShader.bind();
        mvp.set(projection).mul(view).translate(x, y, z);
        lineShader.setMatrix4f("uMVP", mvp);
        lineShader.setVector3f("uColor", DARK);
        glBindVertexArray(boxVao);
        glLineWidth(2.0f);
        glDrawArrays(GL_LINES, 0, 24);
        glBindVertexArray(0);
        lineShader.unbind();
    }

    public void close() {
        glDeleteBuffers(crossVbo);
        glDeleteVertexArrays(crossVao);
        glDeleteBuffers(boxVbo);
        glDeleteVertexArrays(boxVao);
        lineShader.close();
    }
}
