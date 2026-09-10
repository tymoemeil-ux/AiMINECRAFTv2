package com.aiminecraft.render;

import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Siatka OpenGL: VAO + VBO + IBO.
 * Wierzcholek: pozycja (3) + UV (2) + cien (1) = 6 floatow.
 */
public class Mesh {

    private int vao = -1;
    private int vbo = -1;
    private int ibo = -1;
    private int indexCount = 0;

    public Mesh(float[] vertices, int[] indices) {
        indexCount = indices.length;
        if (indexCount == 0) {
            return;
        }
        vao = glGenVertexArrays();
        glBindVertexArray(vao);

        vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(vertices.length);
        vertexBuffer.put(vertices);
        vertexBuffer.flip();
        glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW);

        ibo = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ibo);
        IntBuffer indexBuffer = BufferUtils.createIntBuffer(indices.length);
        indexBuffer.put(indices);
        indexBuffer.flip();
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL_STATIC_DRAW);

        int stride = 6 * Float.BYTES;
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, stride, 3L * Float.BYTES);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(2, 1, GL_FLOAT, false, stride, 5L * Float.BYTES);
        glEnableVertexAttribArray(2);

        glBindVertexArray(0);
    }

    public void draw() {
        if (vao < 0) {
            return;
        }
        glBindVertexArray(vao);
        glDrawElements(GL_TRIANGLES, indexCount, GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);
    }

    /** Liczba indeksow (do diagnostyki). */
    public int getIndexCount() {
        return indexCount;
    }

    public void close() {
        if (vbo >= 0) {
            glDeleteBuffers(vbo);
        }
        if (ibo >= 0) {
            glDeleteBuffers(ibo);
        }
        if (vao >= 0) {
            glDeleteVertexArrays(vao);
        }
        vao = -1;
        vbo = -1;
        ibo = -1;
    }
}
