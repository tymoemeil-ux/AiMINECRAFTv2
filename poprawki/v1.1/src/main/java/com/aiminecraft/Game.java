package com.aiminecraft;

import com.aiminecraft.player.BlockRaycast;
import com.aiminecraft.player.Player;
import com.aiminecraft.render.Hud;
import com.aiminecraft.render.Shader;
import com.aiminecraft.render.TextureAtlas;
import com.aiminecraft.render.Window;
import com.aiminecraft.world.Block;
import com.aiminecraft.world.Chunk;
import com.aiminecraft.world.World;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

/**
 * Gra: petla glowna, sterowanie, niszczenie/stawianie blokow, rysowanie.
 */
public class Game {

    private static final int START_WIDTH = 1280;
    private static final int START_HEIGHT = 720;
    private static final int RENDER_DISTANCE = 6;
    private static final double REACH = 6.0;
    private static final double BREAK_INTERVAL = 0.22;
    private static final double PLACE_INTERVAL = 0.22;

    private static final Vector3f SKY_COLOR = new Vector3f(0.47f, 0.66f, 1.0f);
    private static final Vector3f UP = new Vector3f(0, 1, 0);

    private static final Block[] PALETTE = {
            Block.GRASS, Block.DIRT, Block.STONE, Block.OAK_LOG, Block.OAK_LEAVES, Block.BEDROCK
    };

    private final long seed;

    private Window window;
    private Shader blockShader;
    private Hud hud;
    private TextureAtlas atlas;
    private World world;
    private Player player;

    private final Matrix4f projection = new Matrix4f();
    private final Matrix4f view = new Matrix4f();
    private final Vector3f viewCenter = new Vector3f();

    private int selectedIndex = 0;

    private boolean leftPrev = false;
    private boolean rightPrev = false;
    private boolean escPrev = false;
    private boolean flyKeyPrev = false;
    private boolean cullKeyPrev = false;
    private boolean fogKeyPrev = false;
    private final boolean[] digitPrev = new boolean[PALETTE.length];

    // Awaryjne przelaczniki renderingu (C = culling, V = mgla).
    private boolean cullEnabled = true;
    private boolean fogEnabled = true;

    private double breakCooldown = 0;
    private double placeCooldown = 0;

    private double fpsAccum = 0;
    private int fpsFrames = 0;

    public Game(long seed) {
        this.seed = seed;
    }

    public void run() {
        window = new Window(START_WIDTH, START_HEIGHT, "AiMINECRAFT");
        window.create();

        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
        glClearColor(SKY_COLOR.x, SKY_COLOR.y, SKY_COLOR.z, 1.0f);

        blockShader = new Shader("/shaders/block.vert", "/shaders/block.frag");
        hud = new Hud();
        atlas = new TextureAtlas();
        atlas.load();

        world = new World(seed, atlas, RENDER_DISTANCE);
        player = new Player();

        int spawnH = world.getGenerator().getHeight(0, 0);
        player.setPosition(0.5f, spawnH + 1.02f, 0.5f);

        System.out.println("Generowanie swiata (seed: " + seed + ")...");
        world.updateStreaming(0, 0);
        int meshed = world.updateMeshes(Integer.MAX_VALUE);
        System.out.println("Gotowe! Milej gry :)");
        printDiagnostics(spawnH, meshed);

        window.setCursorCaptured(true);
        hud.onResize(window.getWidth(), window.getHeight());

        double lastTime = window.getTime();
        while (!window.shouldClose()) {
            double now = window.getTime();
            double dt = Math.min(now - lastTime, 0.1);
            lastTime = now;

            window.pollEvents();
            if (window.updateViewport()) {
                hud.onResize(window.getWidth(), window.getHeight());
            }

            player.update(dt, window, world);

            // Siatka bezpieczenstwa: nigdy nie spadnij w otchlan.
            if (player.getPosition().y < -8) {
                int h = world.getGenerator().getHeight(0, 0);
                player.setPosition(0.5f, h + 2f, 0.5f);
                player.setFlying(true);
            }

            int pcx = Math.floorDiv((int) Math.floor(player.getPosition().x), Chunk.SIZE);
            int pcz = Math.floorDiv((int) Math.floor(player.getPosition().z), Chunk.SIZE);
            world.updateStreaming(pcx, pcz);
            world.updateMeshes(8);

            BlockRaycast.Hit hit = BlockRaycast.raycast(
                    world, player.getEyePosition(), player.getDirection(), REACH);

            handleInput(dt, hit);
            render(hit);
            updateTitle(dt);

            window.swapBuffers();
        }

        cleanup();
    }

    private void handleInput(double dt, BlockRaycast.Hit hit) {
        boolean esc = window.isKeyPressed(GLFW_KEY_ESCAPE);
        if (esc && !escPrev) {
            window.setCursorCaptured(false);
        }
        escPrev = esc;

        // LPM: niszczenie (przytrzymanie = ciagle).
        boolean left = window.isMouseButtonPressed(GLFW_MOUSE_BUTTON_LEFT);
        if (!window.isCursorCaptured()) {
            if (left && !leftPrev) {
                window.setCursorCaptured(true); // klik = zlap mysz
            }
        } else {
            breakCooldown -= dt;
            if (left && hit != null && breakCooldown <= 0) {
                world.setBlock(hit.x, hit.y, hit.z, Block.AIR);
                breakCooldown = BREAK_INTERVAL;
            }
            if (!left) {
                breakCooldown = 0;
            }
        }
        leftPrev = left;

        // PPM: stawianie.
        boolean right = window.isMouseButtonPressed(GLFW_MOUSE_BUTTON_RIGHT);
        if (window.isCursorCaptured()) {
            placeCooldown -= dt;
            if (right && hit != null && placeCooldown <= 0) {
                int tx = hit.x + hit.nx;
                int ty = hit.y + hit.ny;
                int tz = hit.z + hit.nz;
                if (world.getBlock(tx, ty, tz) == Block.AIR.id && !blockIntersectsPlayer(tx, ty, tz)) {
                    world.setBlock(tx, ty, tz, PALETTE[selectedIndex]);
                    placeCooldown = PLACE_INTERVAL;
                }
            }
            if (!right) {
                placeCooldown = 0;
            }
        }
        rightPrev = right;

        // F: latanie wl./wyl.
        boolean flyKey = window.isKeyPressed(GLFW_KEY_F);
        if (flyKey && !flyKeyPrev && window.isCursorCaptured()) {
            player.setFlying(!player.isFlying());
            System.out.println(player.isFlying() ? "Latanie: WLACZONE" : "Latanie: WYLACZONE");
        }
        flyKeyPrev = flyKey;

        // 1-6: wybor bloku.
        for (int i = 0; i < PALETTE.length; i++) {
            boolean d = window.isKeyPressed(GLFW_KEY_1 + i);
            if (d && !digitPrev[i]) {
                selectedIndex = i;
            }
            digitPrev[i] = d;
        }

        // C: awaryjne wylaczenie cullingu (test renderingu).
        boolean cullKey = window.isKeyPressed(GLFW_KEY_C);
        if (cullKey && !cullKeyPrev && window.isCursorCaptured()) {
            cullEnabled = !cullEnabled;
            if (cullEnabled) {
                glEnable(GL_CULL_FACE);
            } else {
                glDisable(GL_CULL_FACE);
            }
            System.out.println("Culling: " + (cullEnabled ? "WL" : "WYL"));
        }
        cullKeyPrev = cullKey;

        // V: awaryjne wylaczenie mgly (test renderingu).
        boolean fogKey = window.isKeyPressed(GLFW_KEY_V);
        if (fogKey && !fogKeyPrev && window.isCursorCaptured()) {
            fogEnabled = !fogEnabled;
            System.out.println("Mgla: " + (fogEnabled ? "WL" : "WYL"));
        }
        fogKeyPrev = fogKey;
    }

    private void render(BlockRaycast.Hit hit) {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        float aspect = (float) window.getWidth() / (float) window.getHeight();
        projection.setPerspective((float) Math.toRadians(75.0), aspect, 0.1f, 600.0f);

        Vector3f eye = player.getEyePosition();
        Vector3f dir = player.getDirection();
        viewCenter.set(eye).add(dir);
        view.identity().lookAt(eye, viewCenter, UP);

        blockShader.bind();
        blockShader.setMatrix4f("uProjection", projection);
        blockShader.setMatrix4f("uView", view);
        blockShader.setInt("uAtlas", 0);
        blockShader.setVector3f("uFogColor", SKY_COLOR);
        blockShader.setFloat("uFogStart", fogEnabled ? RENDER_DISTANCE * Chunk.SIZE * 0.5f : 100000f);
        blockShader.setFloat("uFogEnd", fogEnabled ? RENDER_DISTANCE * Chunk.SIZE * 0.92f : 200000f);
        world.render();
        blockShader.unbind();

        if (window.isCursorCaptured()) {
            if (hit != null) {
                hud.drawHighlight(projection, view, hit.x, hit.y, hit.z);
            }
            hud.drawCrosshair();
        }
    }

    private void updateTitle(double dt) {
        fpsAccum += dt;
        fpsFrames++;
        if (fpsAccum >= 0.5) {
            int fps = (int) Math.round(fpsFrames / fpsAccum);
            fpsAccum = 0;
            fpsFrames = 0;
            Vector3f p = player.getPosition();
            window.setTitle(String.format(
                    "AiMINECRAFT | FPS: %d | XYZ: %.1f / %.1f / %.1f | Blok: %s [%d] | Latanie: %s | Seed: %d",
                    fps, p.x, p.y, p.z,
                    PALETTE[selectedIndex].displayName, selectedIndex + 1,
                    player.isFlying() ? "WL" : "WYL", seed));
        }
    }

    /** Wypisuje diagnostyke startowa - ulatwia znalezienie problemu. */
    private void printDiagnostics(int spawnH, int meshed) {
        System.out.println("=== DIAGNOSTYKA ===");
        System.out.println("OpenGL: " + glGetString(GL_VENDOR)
                + " / " + glGetString(GL_RENDERER)
                + " / " + glGetString(GL_VERSION));
        System.out.println("Chunki: " + world.getChunkCount()
                + ", przebudowanych siatek: " + meshed
                + ", indeksow razem: " + world.getTotalIndices());
        Block top = Block.fromId(world.getBlock(0, spawnH, 0));
        Block above = Block.fromId(world.getBlock(0, spawnH + 1, 0));
        System.out.println("Spawn: kolumna (0,0) h=" + spawnH
                + ", blok na h=" + top.displayName + "(" + top.id + ")"
                + ", nad nim=" + above.displayName + "(" + above.id + ")");
        Vector3f eye = player.getEyePosition();
        Vector3f dir = player.getDirection();
        System.out.println(String.format("Kamera: oko=(%.2f, %.2f, %.2f) kierunek=(%.2f, %.2f, %.2f)",
                eye.x, eye.y, eye.z, dir.x, dir.y, dir.z));
        if (world.getTotalIndices() == 0) {
            System.out.println("UWAGA: siatki sa puste - swiat nie wygenerowal geometrii!");
        }
        System.out.println("Testy: C = culling WL/WYL, V = mgla WL/WYL");
        System.out.println("===================");
    }

    private boolean blockIntersectsPlayer(int bx, int by, int bz) {
        Vector3f p = player.getPosition();
        float minX = p.x - Player.HALF_WIDTH;
        float maxX = p.x + Player.HALF_WIDTH;
        float minY = p.y;
        float maxY = p.y + Player.HEIGHT;
        float minZ = p.z - Player.HALF_WIDTH;
        float maxZ = p.z + Player.HALF_WIDTH;
        return bx + 1 > minX && bx < maxX
                && by + 1 > minY && by < maxY
                && bz + 1 > minZ && bz < maxZ;
    }

    private void cleanup() {
        world.close();
        atlas.close();
        blockShader.close();
        hud.close();
        window.destroy();
    }
}
