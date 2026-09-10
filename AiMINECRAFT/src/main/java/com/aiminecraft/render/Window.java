package com.aiminecraft.render;

import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * Okno GLFW + kontekst OpenGL. Same odpytywanie stanu (polling),
 * zadnych callbackow - prosto i bez niespodzianek.
 */
public class Window {

    private final int startWidth;
    private final int startHeight;
    private final String title;

    private long handle = NULL;
    private int width = 1;
    private int height = 1;
    private boolean cursorCaptured = false;

    private final int[] fbWidth = new int[1];
    private final int[] fbHeight = new int[1];
    private final double[] cursorX = new double[1];
    private final double[] cursorY = new double[1];

    private final java.util.ArrayDeque<Integer> charQueue = new java.util.ArrayDeque<>();
    private double scrollAccum = 0;

    public Window(int startWidth, int startHeight, String title) {
        this.startWidth = startWidth;
        this.startHeight = startHeight;
        this.title = title;
    }

    public void create() {
        if (!glfwInit()) {
            throw new IllegalStateException("Nie mozna zainicjalizowac GLFW");
        }
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);

        handle = glfwCreateWindow(startWidth, startHeight, title, NULL, NULL);
        if (handle == NULL) {
            glfwTerminate();
            throw new IllegalStateException("Nie mozna utworzyc okna (czy karta wspiera OpenGL 3.3?)");
        }

        GLFWVidMode videoMode = glfwGetVideoMode(glfwGetPrimaryMonitor());
        if (videoMode != null) {
            glfwSetWindowPos(handle,
                    (videoMode.width() - startWidth) / 2,
                    (videoMode.height() - startHeight) / 2);
        }

        glfwSetCharCallback(handle, (win, codepoint) -> {
            if (charQueue.size() < 256) {
                charQueue.add(codepoint);
            }
        });
        glfwSetScrollCallback(handle, (win, xoffset, yoffset) -> {
            scrollAccum += yoffset;
        });

        glfwMakeContextCurrent(handle);
        glfwSwapInterval(1); // vsync
        glfwShowWindow(handle);

        GL.createCapabilities();
        updateViewport();
    }

    /** Sprawdza rozmiar okna i ustawia viewport. Zwraca true, jesli rozmiar sie zmienil. */
    public boolean updateViewport() {
        glfwGetFramebufferSize(handle, fbWidth, fbHeight);
        int w = Math.max(1, fbWidth[0]);
        int h = Math.max(1, fbHeight[0]);
        if (w == width && h == height) {
            return false;
        }
        width = w;
        height = h;
        glViewport(0, 0, w, h);
        return true;
    }

    public boolean shouldClose() {
        return glfwWindowShouldClose(handle);
    }

    public void pollEvents() {
        glfwPollEvents();
    }

    public void swapBuffers() {
        glfwSwapBuffers(handle);
    }

    public double getTime() {
        return glfwGetTime();
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void setTitle(String title) {
        glfwSetWindowTitle(handle, title);
    }

    public boolean isKeyPressed(int keyCode) {
        return glfwGetKey(handle, keyCode) == GLFW_PRESS;
    }

    public boolean isMouseButtonPressed(int button) {
        return glfwGetMouseButton(handle, button) == GLFW_PRESS;
    }

    public double getMouseX() {
        pollCursor();
        return cursorX[0];
    }

    public double getMouseY() {
        pollCursor();
        return cursorY[0];
    }

    private void pollCursor() {
        glfwGetCursorPos(handle, cursorX, cursorY);
    }

    /** Pobiera wpisany znak (chat) albo -1, jesli kolejka pusta. */
    public int pollChar() {
        Integer cp = charQueue.poll();
        return cp != null ? cp : -1;
    }

    public void clearCharQueue() {
        charQueue.clear();
    }

    /** Pobiera i zeruje ruch kolka myszy (hotbar, listy). */
    public double consumeScroll() {
        double s = scrollAccum;
        scrollAccum = 0;
        return s;
    }

    public void setVsync(boolean vsync) {
        glfwSwapInterval(vsync ? 1 : 0);
    }

    /** true = mysz zlapana (rozgladanie), false = kursor wolny. */
    public void setCursorCaptured(boolean captured) {
        cursorCaptured = captured;
        glfwSetInputMode(handle, GLFW_CURSOR, captured ? GLFW_CURSOR_DISABLED : GLFW_CURSOR_NORMAL);
    }

    public boolean isCursorCaptured() {
        return cursorCaptured;
    }

    public void requestClose() {
        glfwSetWindowShouldClose(handle, true);
    }

    public void destroy() {
        glfwDestroyWindow(handle);
        glfwTerminate();
    }
}
