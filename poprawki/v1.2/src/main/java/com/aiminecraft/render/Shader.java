package com.aiminecraft.render;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL11.GL_FALSE;
import static org.lwjgl.opengl.GL20.*;

/**
 * Program cieniujacy (vertex + fragment) ladowany z resources/shaders.
 */
public class Shader {

    private final int programId;
    private final Map<String, Integer> uniformCache = new HashMap<>();

    public Shader(String vertexPath, String fragmentPath) {
        String vertexSource = loadResource(vertexPath);
        String fragmentSource = loadResource(fragmentPath);
        int vertexId = compile(vertexPath, GL_VERTEX_SHADER, vertexSource);
        int fragmentId = compile(fragmentPath, GL_FRAGMENT_SHADER, fragmentSource);
        programId = glCreateProgram();
        glAttachShader(programId, vertexId);
        glAttachShader(programId, fragmentId);
        glLinkProgram(programId);
        if (glGetProgrami(programId, GL_LINK_STATUS) == GL_FALSE) {
            throw new IllegalStateException("Blad linkowania shadera:\n" + glGetProgramInfoLog(programId));
        }
        glDeleteShader(vertexId);
        glDeleteShader(fragmentId);
    }

    private static int compile(String name, int type, String source) {
        int id = glCreateShader(type);
        glShaderSource(id, source);
        glCompileShader(id);
        if (glGetShaderi(id, GL_COMPILE_STATUS) == GL_FALSE) {
            throw new IllegalStateException("Blad kompilacji shadera " + name + ":\n" + glGetShaderInfoLog(id));
        }
        return id;
    }

    private static String loadResource(String path) {
        try (InputStream in = Shader.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("Brak pliku: " + path);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Nie mozna wczytac pliku: " + path, e);
        }
    }

    private int uniform(String name) {
        Integer cached = uniformCache.get(name);
        if (cached == null) {
            cached = glGetUniformLocation(programId, name);
            uniformCache.put(name, cached);
        }
        return cached;
    }

    public void setMatrix4f(String name, Matrix4f matrix) {
        int location = uniform(name);
        if (location < 0) {
            return;
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer buffer = stack.mallocFloat(16);
            matrix.get(buffer);
            // UWAGA: JOML zapisuje macierz ABSOLUTNIE (pozycja bufora stoi w miejscu),
            // wiec flip() wyzerowalby limit i uniform by sie nie ustawil (count = 0).
            // rewind() jest poprawne niezaleznie od sposobu zapisu.
            buffer.rewind();
            glUniformMatrix4fv(location, false, buffer);
        }
    }

    public void setInt(String name, int value) {
        int location = uniform(name);
        if (location < 0) {
            return;
        }
        glUniform1i(location, value);
    }

    public void setFloat(String name, float value) {
        int location = uniform(name);
        if (location < 0) {
            return;
        }
        glUniform1f(location, value);
    }

    public void setVector3f(String name, Vector3f value) {
        int location = uniform(name);
        if (location < 0) {
            return;
        }
        glUniform3f(location, value.x, value.y, value.z);
    }

    public void bind() {
        glUseProgram(programId);
    }

    public void unbind() {
        glUseProgram(0);
    }

    public void close() {
        glDeleteProgram(programId);
    }
}
