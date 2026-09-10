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
    private final int[] winWidth = new int[1];
    private final int[] winHeight = new int[1];
    private final double[] cursorX = new double[1];
    private final double[] cursorY = new double[1];
    /** Rozmiar okna w jednostkach ekranu (do skalowania myszy przy DPI != 100%). */
    private int windowWidth = 1;
    private int windowHeight = 1;
    /** Pelny ekran + zapamietany rozmiar okna. */
    private boolean fullscreen = false;
    private int savedX;
    private int savedY;
    private int savedW = 1280;
    private int savedH = 720;

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
        glfwGetWindowSize(handle, winWidth, winHeight);
        windowWidth = Math.max(1, winWidth[0]);
        windowHeight = Math.max(1, winHeight[0]);
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

    /** Pozycja myszy w pikselach bufora (zgodnie z rysowaniem, tez przy DPI 125/150%). */
    public double getMouseX() {
        pollCursor();
        return cursorX[0] * width / windowWidth;
    }

    public double getMouseY() {
        pollCursor();
        return cursorY[0] * height / windowHeight;
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

    /** Przelacza pelny ekran (F11). */
    public void toggleFullscreen() {
        long monitor = glfwGetPrimaryMonitor();
        if (monitor == NULL) {
            return;
        }
        if (!fullscreen) {
            int[] x = new int[1];
            int[] y = new int[1];
            int[] w = new int[1];
            int[] h = new int[1];
            glfwGetWindowPos(handle, x, y);
            glfwGetWindowSize(handle, w, h);
            savedX = x[0];
            savedY = y[0];
            savedW = Math.max(640, w[0]);
            savedH = Math.max(360, h[0]);
            GLFWVidMode mode = glfwGetVideoMode(monitor);
            if (mode == null) {
                return;
            }
            glfwSetWindowMonitor(handle, monitor, 0, 0, mode.width(), mode.height(), mode.refreshRate());
            fullscreen = true;
        } else {
            glfwSetWindowMonitor(handle, NULL, savedX, savedY, savedW, savedH, 0);
            fullscreen = false;
        }
    }

    public void destroy() {
        glfwDestroyWindow(handle);
        glfwTerminate();
    }
}
