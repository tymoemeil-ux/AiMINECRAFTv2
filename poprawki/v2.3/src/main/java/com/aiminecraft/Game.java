package com.aiminecraft;

import com.aiminecraft.item.Inventory;
import com.aiminecraft.item.Item;
import com.aiminecraft.item.ItemStack;
import com.aiminecraft.item.Recipes;
import com.aiminecraft.item.Tool;
import com.aiminecraft.player.BlockRaycast;
import com.aiminecraft.player.Player;
import com.aiminecraft.render.FontAtlas;
import com.aiminecraft.render.GuiRenderer;
import com.aiminecraft.render.Hud;
import com.aiminecraft.render.ItemAtlas;
import com.aiminecraft.render.Shader;
import com.aiminecraft.render.TextureAtlas;
import com.aiminecraft.render.Window;
import com.aiminecraft.ui.Screens;
import com.aiminecraft.util.Settings;
import com.aiminecraft.util.Sound;
import com.aiminecraft.world.Block;
import com.aiminecraft.world.Chunk;
import com.aiminecraft.world.World;
import com.aiminecraft.world.WorldSave;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

/**
 * Gra v2: menu, swiat, ekwipunek, crafting, chat, komendy, panel admina.
 */
public class Game {

    public enum Screen {
        TITLE, PLAY_MENU, CONFIRM_DELETE, SETTINGS, PLAY, PAUSE, INVENTORY, CRAFTING, CHAT, ADMIN
    }

    private static final int START_WIDTH = 1280;
    private static final int START_HEIGHT = 720;
    private static final double REACH = 6.0;
    private static final double PLACE_INTERVAL = 0.22;
    private static final double DAY_LENGTH = 600.0; // sekunda doby
    private static final double AUTOSAVE_INTERVAL = 30.0;

    private static final Vector3f SKY_DAY = new Vector3f(0.47f, 0.66f, 1.0f);
    private static final Vector3f SKY_NIGHT = new Vector3f(0.02f, 0.03f, 0.08f);
    private static final Vector3f UP = new Vector3f(0, 1, 0);

    // ---------- Stan publiczny (czyta go Screens) ----------
    public Screen state = Screen.TITLE;
    public Screen settingsReturn = Screen.TITLE;
    public float mouseX = 0;
    public float mouseY = 0;
    /** Czas do animacji menu (chmury). */
    public double uiTime = 0;
    public int windowW = START_WIDTH;
    public int windowH = START_HEIGHT;

    public GuiRenderer gui;
    public FontAtlas font;
    public TextureAtlas atlas;
    public ItemAtlas items;

    public Settings settings = new Settings();
    public boolean saveExists = false;
    public String saveInfo = "";
    public Long saveSeed = null;
    public final StringBuilder seedField = new StringBuilder();
    public int newGamemode = 0;

    public World world = null;
    public Player player = null;
    public WorldSave saver = null;
    public long seed = 0;
    public int gamemode = 0; // 0 = survival, 1 = creative
    public float time = 0.1f;

    public Inventory inventory = new Inventory();
    public ItemStack cursor = null;
    public final ItemStack[] invCraft = new ItemStack[4];
    public final ItemStack[] tableCraft = new ItemStack[9];
    public ItemStack craftResultInv = null;
    public ItemStack craftResultTable = null;
    public boolean recipeBookOpen = false;
    public List<Item> creativeList = new ArrayList<>();
    public int creativeScroll = 0;

    public final List<String> chatLines = new ArrayList<>();
    public final StringBuilder chatInput = new StringBuilder();
    public String heldName = null;
    public float heldNameTimer = 0;
    public float breakProgress = -1;
    public float tipTimer = 0;
    public boolean debugOverlay = false;

    // ---------- Stan prywatny ----------
    private Window window;
    private Shader blockShader;
    private Shader waterShader;
    private Hud hud;

    private final Matrix4f projection = new Matrix4f();
    private final Matrix4f view = new Matrix4f();
    private final Vector3f viewCenter = new Vector3f();
    private final Vector3f skyColor = new Vector3f();

    private final List<Double> chatTimes = new ArrayList<>();
    private final Random random = new Random();

    private int fps = 0;
    private double fpsAccum = 0;
    private int fpsFrames = 0;
    private double saveTimer = 0;

    private boolean leftPrev = false;
    private boolean rightPrev = false;
    private boolean escPrev = false;
    private boolean ePrev = false;
    private boolean tPrev = false;
    private boolean gPrev = false;
    private boolean fPrev = false;
    private boolean f3Prev = false;
    private boolean enterPrev = false;
    private boolean backPrev = false;
    private boolean cullPrev = false;
    private boolean fogPrev = false;
    private final boolean[] digitPrev = new boolean[9];
    private double backTimer = 0;

    private boolean cullEnabled = true;
    private boolean fogEnabled = true;
    private double placeCooldown = 0;
    private int breakX = 0;
    private int breakY = -1;
    private int breakZ = 0;
    private boolean wasInWater = false;
    private int lastHeldId = -1;

    /** Start z seedem z linii komend (trafia do pola w menu). */
    public void run(String argSeed) {
        if (argSeed != null && !argSeed.isEmpty()) {
            String s = argSeed.length() > 32 ? argSeed.substring(0, 32) : argSeed;
            seedField.append(s);
        }
        run();
    }

    public void run() {
        settings.load();
        Sound.setEnabled(settings.sound);
        creativeList = Item.creativeList();

        window = new Window(START_WIDTH, START_HEIGHT, "AiMINECRAFT 2.0");
        window.create();
        window.setVsync(settings.vsync);

        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
        glClearColor(SKY_DAY.x, SKY_DAY.y, SKY_DAY.z, 1.0f);

        blockShader = new Shader("/shaders/block.vert", "/shaders/block.frag");
        waterShader = new Shader("/shaders/water.vert", "/shaders/water.frag");
        hud = new Hud();
        atlas = new TextureAtlas();
        atlas.load();
        items = new ItemAtlas();
        items.load();
        font = new FontAtlas();
        font.load();
        gui = new GuiRenderer();

        hud.onResize(window.getWidth(), window.getHeight());
        windowW = window.getWidth();
        windowH = window.getHeight();

        double lastTime = window.getTime();
        while (!window.shouldClose()) {
            double now = window.getTime();
            double dt = Math.min(now - lastTime, 0.1);
            lastTime = now;

            window.pollEvents();
            if (window.updateViewport()) {
                windowW = window.getWidth();
                windowH = window.getHeight();
                hud.onResize(windowW, windowH);
            }
            mouseX = (float) window.getMouseX();
            mouseY = (float) window.getMouseY();
            uiTime = window.getTime();

            update(dt);
            render();
            updateFps(dt);

            window.swapBuffers();
        }

        cleanup();
    }

    // ================= UPDATE =================

    private void update(double dt) {
        // Scroll myszy.
        double scroll = window.consumeScroll();
        if (scroll != 0) {
            if (state == Screen.PLAY && window.isCursorCaptured()) {
                int sel = inventory.getSelected() - (int) Math.signum(scroll);
                sel = ((sel % 9) + 9) % 9;
                inventory.setSelected(sel);
            } else if (state == Screen.INVENTORY && gamemode == 1) {
                creativeScroll -= (int) Math.signum(scroll);
                creativeScroll = Math.max(0, Math.min(maxCreativeScroll(), creativeScroll));
            }
        }

        // Kliki myszy na GUI.
        boolean left = window.isMouseButtonPressed(GLFW_MOUSE_BUTTON_LEFT);
        boolean right = window.isMouseButtonPressed(GLFW_MOUSE_BUTTON_RIGHT);
        if (state != Screen.PLAY && state != Screen.CHAT) {
            if (left && !leftPrev) {
                Screens.click(this, mouseX, mouseY, false);
            } else if (right && !rightPrev) {
                Screens.click(this, mouseX, mouseY, true);
            }
        }
        leftPrev = left;
        rightPrev = right;

        // Klawisze globalne wg stanu.
        handleKeys(dt);

        // Znaki z klawiatury (seed, chat).
        drainChars();

        if (world == null) {
            return;
        }

        // Czas dnia leci (oprocz pauzy).
        if (state != Screen.PAUSE) {
            time += dt / DAY_LENGTH;
            if (time >= 1) {
                time -= 1;
            }
        }

        if (state == Screen.PLAY) {
            if (window.isCursorCaptured()) {
                player.update(dt, window, world);
                updateBreakPlace(dt);
            }
            // Siatka bezpieczenstwa.
            if (player.getPosition().y < -8) {
                respawn();
                player.setFlying(true);
            }
            // Dzwiek wpadniecia do wody.
            if (player.isInWater() && !wasInWater && !player.isFlying()) {
                Sound.play("splash");
            }
            wasInWater = player.isInWater();

            int pcx = Math.floorDiv((int) Math.floor(player.getPosition().x), Chunk.SIZE);
            int pcz = Math.floorDiv((int) Math.floor(player.getPosition().z), Chunk.SIZE);
            world.updateStreaming(pcx, pcz);
            world.updateMeshes(8);
        } else if (state != Screen.PAUSE) {
            // W oknach swiat sie dogenerowuje, ale gracz stoi.
            world.updateMeshes(8);
        }

        // Nazwa trzymanego przedmiotu.
        int held = inventory.heldId();
        if (held != lastHeldId) {
            lastHeldId = held;
            Item it = Item.get(held);
            heldName = it != null ? it.name : null;
            heldNameTimer = 2.5f;
        }
        if (heldNameTimer > 0) {
            heldNameTimer -= dt;
        }
        if (tipTimer > 0) {
            tipTimer -= dt;
        }

        // Auto-zapis.
        if (state != Screen.PAUSE) {
            saveTimer += dt;
            if (saveTimer >= AUTOSAVE_INTERVAL) {
                saveTimer = 0;
                flushSave();
            }
        }
    }

    private void handleKeys(double dt) {
        boolean esc = window.isKeyPressed(GLFW_KEY_ESCAPE);
        boolean escEdge = esc && !escPrev;
        escPrev = esc;

        boolean e = window.isKeyPressed(GLFW_KEY_E);
        boolean eEdge = e && !ePrev;
        ePrev = e;

        boolean t = window.isKeyPressed(GLFW_KEY_T);
        boolean tEdge = t && !tPrev;
        tPrev = t;

        boolean gk = window.isKeyPressed(GLFW_KEY_G);
        boolean gEdge = gk && !gPrev;
        gPrev = gk;

        boolean f = window.isKeyPressed(GLFW_KEY_F);
        boolean fEdge = f && !fPrev;
        fPrev = f;

        boolean f3 = window.isKeyPressed(GLFW_KEY_F3);
        boolean f3Edge = f3 && !f3Prev;
        f3Prev = f3;

        boolean enter = window.isKeyPressed(GLFW_KEY_ENTER) || window.isKeyPressed(GLFW_KEY_KP_ENTER);
        boolean enterEdge = enter && !enterPrev;
        enterPrev = enter;

        // Backspace z powtarzaniem.
        boolean back = window.isKeyPressed(GLFW_KEY_BACKSPACE);
        if (back && !backPrev) {
            backspaceOnce();
            backTimer = 0.4;
        } else if (back) {
            backTimer -= dt;
            if (backTimer <= 0) {
                backspaceOnce();
                backTimer = 1.0 / 30.0;
            }
        }
        backPrev = back;

        // C/V: testy renderingu.
        boolean cull = window.isKeyPressed(GLFW_KEY_C);
        if (cull && !cullPrev && state == Screen.PLAY && window.isCursorCaptured()) {
            cullEnabled = !cullEnabled;
            if (cullEnabled) {
                glEnable(GL_CULL_FACE);
            } else {
                glDisable(GL_CULL_FACE);
            }
        }
        cullPrev = cull;
        boolean fog = window.isKeyPressed(GLFW_KEY_V);
        if (fog && !fogPrev && state == Screen.PLAY && window.isCursorCaptured()) {
            fogEnabled = !fogEnabled;
        }
        fogPrev = fog;

        switch (state) {
            case TITLE:
                break;
            case PLAY_MENU:
                if (escEdge) {
                    uiBackToTitle();
                } else if (enterEdge) {
                    uiStartPlay();
                }
                break;
            case CONFIRM_DELETE:
                if (escEdge) {
                    uiCancelDelete();
                }
                break;
            case SETTINGS:
                if (escEdge) {
                    uiSettingsDone();
                }
                break;
            case PLAY:
                if (escEdge) {
                    setState(Screen.PAUSE);
                    window.setCursorCaptured(false);
                    Sound.play("click");
                } else if (eEdge) {
                    openInventory();
                } else if (tEdge) {
                    state = Screen.CHAT;
                    window.setCursorCaptured(false);
                    window.clearCharQueue();
                    chatInput.setLength(0);
                    Sound.play("click");
                } else if (gEdge) {
                    state = Screen.ADMIN;
                    window.setCursorCaptured(false);
                    Sound.play("click");
                } else if (f3Edge) {
                    debugOverlay = !debugOverlay;
                } else if (fEdge && window.isCursorCaptured()) {
                    player.setFlying(!player.isFlying());
                    addChat(player.isFlying() ? "Latanie: WLACZONE" : "Latanie: WYLACZONE");
                }
                if (window.isCursorCaptured()) {
                    for (int i = 0; i < 9; i++) {
                        boolean d = window.isKeyPressed(GLFW_KEY_1 + i);
                        if (d && !digitPrev[i]) {
                            inventory.setSelected(i);
                        }
                        digitPrev[i] = d;
                    }
                }
                break;
            case PAUSE:
                if (escEdge) {
                    uiResume();
                }
                break;
            case INVENTORY:
                if (escEdge || eEdge) {
                    closeGuiToPlay();
                }
                break;
            case CRAFTING:
                if (escEdge || eEdge) {
                    closeGuiToPlay();
                }
                break;
            case CHAT:
                if (escEdge) {
                    closeGuiToPlay();
                } else if (enterEdge) {
                    sendChat();
                }
                break;
            case ADMIN:
                if (escEdge || gEdge) {
                    closeGuiToPlay();
                }
                break;
        }
    }

    private void drainChars() {
        if (state == Screen.PLAY_MENU) {
            int cp;
            while ((cp = window.pollChar()) != -1) {
                if (seedField.length() >= 32) {
                    break;
                }
                if (cp == '-' && seedField.length() == 0) {
                    seedField.append('-');
                } else if ((cp >= '0' && cp <= '9') || (cp >= 'a' && cp <= 'z')
                        || (cp >= 'A' && cp <= 'Z')) {
                    seedField.append((char) cp);
                }
            }
        } else if (state == Screen.CHAT) {
            int cp;
            while ((cp = window.pollChar()) != -1) {
                if (chatInput.length() >= 100) {
                    break;
                }
                String norm = GuiRenderer.normalize(Character.toString((char) cp));
                if (!norm.isEmpty() && (norm.charAt(0) != '?' || cp == '?')) {
                    chatInput.append(norm);
                }
            }
        } else {
            window.clearCharQueue();
        }
    }

    private void backspaceOnce() {
        if (state == Screen.PLAY_MENU && seedField.length() > 0) {
            seedField.setLength(seedField.length() - 1);
        } else if (state == Screen.CHAT && chatInput.length() > 0) {
            chatInput.setLength(chatInput.length() - 1);
        }
    }

    // ================= Kopanie / stawianie =================

    private void updateBreakPlace(double dt) {
        BlockRaycast.Hit hit = currentHit();
        boolean left = window.isMouseButtonPressed(GLFW_MOUSE_BUTTON_LEFT);
        boolean right = window.isMouseButtonPressed(GLFW_MOUSE_BUTTON_RIGHT);

        // LPM: kopanie.
        if (left && hit != null) {
            Block block = Block.fromId(world.getBlock(hit.x, hit.y, hit.z));
            if (block == Block.AIR || block == Block.WATER) {
                breakProgress = -1;
            } else if (gamemode == 1) {
                destroyBlock(hit.x, hit.y, hit.z, block);
                breakProgress = -1;
            } else {
                if (hit.x != breakX || hit.y != breakY || hit.z != breakZ) {
                    breakX = hit.x;
                    breakY = hit.y;
                    breakZ = hit.z;
                    breakProgress = 0;
                }
                float digTime = digTime(block);
                breakProgress += dt / digTime;
                if (breakProgress >= 1) {
                    destroyBlock(hit.x, hit.y, hit.z, block);
                    breakProgress = -1;
                }
            }
        } else {
            breakProgress = -1;
            breakY = -1;
        }

        // PPM: stawianie / interakcja.
        placeCooldown -= dt;
        if (right && hit != null && placeCooldown <= 0 && window.isCursorCaptured()) {
            if (useOrPlace(hit)) {
                placeCooldown = PLACE_INTERVAL;
            }
        }
        if (!right) {
            placeCooldown = 0;
        }
    }

    private BlockRaycast.Hit currentHit() {
        if (world == null || player == null) {
            return null;
        }
        return BlockRaycast.raycast(world, player.getEyePosition(), player.getDirection(), REACH);
    }

    private Tool heldTool() {
        Item it = Item.get(inventory.heldId());
        return it != null ? it.tool : Tool.NONE;
    }

    private float digTime(Block block) {
        float t = block.hardness;
        if (block.tool != Tool.NONE && heldTool() == block.tool) {
            t *= 0.25f;
        }
        if (player.isInWater()) {
            t *= 2.0f;
        }
        return Math.max(0.05f, t);
    }

    private void destroyBlock(int x, int y, int z, Block block) {
        world.setBlock(x, y, z, Block.AIR.id);
        Sound.play("break");
        if (gamemode == 0 && block.dropId != 0) {
            boolean needsPick = block.dropId >= 100 || block == Block.STONE
                    || block == Block.COBBLESTONE || block == Block.OBSIDIAN;
            if (!needsPick || heldTool() == Tool.PICKAXE) {
                int left = inventory.add(block.dropId, 1);
                if (left == 0) {
                    Sound.play("pickup");
                }
            }
        }
    }

    /** PPM: interakcja ze stolem albo stawianie bloku. Zwraca true, jesli cos zrobiono. */
    private boolean useOrPlace(BlockRaycast.Hit hit) {
        Block target = Block.fromId(world.getBlock(hit.x, hit.y, hit.z));
        int held = inventory.heldId();
        Item heldItem = Item.get(held);
        boolean emptyHand = held == 0 || heldItem == null || heldItem.placeBlock < 0;

        // Stol: pusta reka / narzedzie = otworz.
        if (target == Block.CRAFTING_TABLE && emptyHand) {
            state = Screen.CRAFTING;
            window.setCursorCaptured(false);
            refreshCraftResult(true);
            Sound.play("click");
            return true;
        }
        if (heldItem == null || heldItem.placeBlock < 0) {
            return false;
        }
        Block toPlace = Block.fromId(heldItem.placeBlock);
        if (!world.canPlace(toPlace.id & 0xFF)) {
            return false;
        }

        int tx = hit.x;
        int ty = hit.y;
        int tz = hit.z;
        // Wode / kwiaty / pochodnie zastepujemy w miejscu.
        if (target.solid) {
            tx = hit.x + hit.nx;
            ty = hit.y + hit.ny;
            tz = hit.z + hit.nz;
        }
        if (ty < 0 || ty >= Chunk.HEIGHT) {
            return false;
        }
        Block existing = Block.fromId(world.getBlock(tx, ty, tz));
        if (existing.solid) {
            return false;
        }
        // Wymagania podloza.
        Block below = Block.fromId(world.getBlock(tx, ty - 1, tz));
        if (toPlace.kind == Block.Kind.CROSS) {
            if (below != Block.GRASS && below != Block.DIRT) {
                return false;
            }
        }
        if (toPlace == Block.TORCH && !below.solid) {
            return false;
        }
        if (toPlace == Block.CACTUS && below != Block.SAND) {
            return false;
        }
        // Nie stawiaj w gracza.
        if (toPlace.solid && blockIntersectsPlayer(tx, ty, tz)) {
            return false;
        }
        world.setBlock(tx, ty, tz, toPlace.id);
        Sound.play("place");
        if (gamemode == 0) {
            inventory.consumeHeld();
        }
        return true;
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

    // ================= RENDER =================

    private void render() {
        if (world == null) {
            glClearColor(0.1f, 0.1f, 0.12f, 1.0f);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            gui.begin(windowW, windowH);
            Screens.draw(this);
            gui.end();
            return;
        }

        computeSky();

        float aspect = (float) windowW / (float) windowH;
        projection.setPerspective((float) Math.toRadians(settings.fov), aspect, 0.1f, 600.0f);
        Vector3f eye = player.getEyePosition();
        Vector3f dir = player.getDirection();
        viewCenter.set(eye).add(dir);
        view.identity().lookAt(eye, viewCenter, UP);

        boolean underwater = player.isHeadInWater();
        float fogStart;
        float fogEnd;
        Vector3f fogColor = skyColor;
        float brightness = skyBrightness();
        if (underwater) {
            fogStart = 2;
            fogEnd = 18;
            fogColor = new Vector3f(0.1f, 0.2f, 0.6f);
            brightness *= 0.8f;
        } else {
            fogStart = settings.renderDistance * Chunk.SIZE * 0.5f;
            fogEnd = settings.renderDistance * Chunk.SIZE * 0.95f;
        }
        if (!fogEnabled) {
            fogStart = 100000f;
            fogEnd = 200000f;
        }

        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        blockShader.bind();
        blockShader.setMatrix4f("uProjection", projection);
        blockShader.setMatrix4f("uView", view);
        blockShader.setInt("uAtlas", 0);
        blockShader.setVector3f("uFogColor", fogColor);
        blockShader.setFloat("uFogStart", fogStart);
        blockShader.setFloat("uFogEnd", fogEnd);
        blockShader.setFloat("uBrightness", brightness);
        world.renderOpaque();
        blockShader.unbind();

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        waterShader.bind();
        waterShader.setMatrix4f("uProjection", projection);
        waterShader.setMatrix4f("uView", view);
        waterShader.setInt("uAtlas", 0);
        waterShader.setVector3f("uFogColor", fogColor);
        waterShader.setFloat("uFogStart", fogStart);
        waterShader.setFloat("uFogEnd", fogEnd);
        waterShader.setFloat("uBrightness", brightness);
        world.renderTranslucent();
        waterShader.unbind();
        glDisable(GL_BLEND);

        BlockRaycast.Hit hit = state == Screen.PLAY && window.isCursorCaptured() ? currentHit() : null;
        if (hit != null) {
            hud.drawHighlight(projection, view, hit.x, hit.y, hit.z);
        }
        if (state == Screen.PLAY) {
            hud.drawCrosshair();
        }

        gui.begin(windowW, windowH);
        Screens.draw(this);
        gui.end();
    }

    /** Pora dnia -> kolor nieba (ustawia tez clear color). */
    private void computeSky() {
        double sun = Math.cos(time * Math.PI * 2.0); // 1 = poludnie, -1 = polnoc
        float daylight = smoothstep(-0.12f, 0.3f, (float) sun);
        skyColor.set(SKY_NIGHT).lerp(SKY_DAY, daylight);
        // Zachod/wschod: pomaranczowa lawa.
        float sunset = Math.max(0, 1 - Math.abs((float) sun - 0.08f) * 6f);
        skyColor.x = Math.min(1, skyColor.x + 0.35f * sunset);
        skyColor.y = Math.min(1, skyColor.y + 0.12f * sunset);
        glClearColor(skyColor.x, skyColor.y, skyColor.z, 1.0f);
    }

    private float skyBrightness() {
        double sun = Math.cos(time * Math.PI * 2.0);
        float daylight = smoothstep(-0.12f, 0.3f, (float) sun);
        return 0.22f + 0.78f * daylight;
    }

    private static float smoothstep(float edge0, float edge1, float x) {
        float t = Math.max(0, Math.min(1, (x - edge0) / (edge1 - edge0)));
        return t * t * (3 - 2 * t);
    }

    public static String timeName(float t) {
        if (t < 0.42) {
            return "dzien";
        }
        if (t < 0.55) {
            return "zachod";
        }
        if (t < 0.95) {
            return "noc";
        }
        return "wschod";
    }

    private void updateFps(double dt) {
        fpsAccum += dt;
        fpsFrames++;
        if (fpsAccum >= 0.5) {
            fps = (int) Math.round(fpsFrames / fpsAccum);
            fpsAccum = 0;
            fpsFrames = 0;
            if (world != null && player != null) {
                Vector3f p = player.getPosition();
                window.setTitle(String.format(
                        "AiMINECRAFT 2.0 | FPS: %d | XYZ: %.1f / %.1f / %.1f | %s | %s | Seed: %d",
                        fps, p.x, p.y, p.z,
                        gamemode == 1 ? "Creative" : "Survival",
                        timeName(time), seed));
            } else {
                window.setTitle("AiMINECRAFT 2.0");
            }
        }
    }

    // ================= Swiat: start / zapis =================

    private void startWorld(long newSeed, int mode, boolean fresh) {
        seed = newSeed;
        if (fresh) {
            WorldSave.delete();
        }
        saver = new WorldSave(seed);
        WorldSave.PlayerData loaded = fresh ? null : saver.load();

        world = new World(seed, atlas, settings.renderDistance);
        world.setSaver(saver);
        player = new Player();
        player.setSensitivity(settings.sensitivity);
        inventory = new Inventory();
        cursor = null;
        clearCrafting();
        chatLines.clear();
        chatTimes.clear();
        breakProgress = -1;
        saveTimer = 0;

        if (loaded != null) {
            player.setPosition(loaded.x, loaded.y, loaded.z);
            player.setYaw(loaded.yaw);
            player.setPitch(loaded.pitch);
            player.setFlying(loaded.flying);
            gamemode = loaded.gamemode;
            time = loaded.time;
            for (int i = 0; i < Inventory.SIZE; i++) {
                if (loaded.invCounts[i] > 0) {
                    inventory.set(i, new ItemStack(loaded.invIds[i], loaded.invCounts[i]));
                }
            }
            inventory.setSelected(loaded.selected);
            if (loaded.cursorCount > 0) {
                cursor = new ItemStack(loaded.cursorId, loaded.cursorCount);
            }
            for (int i = 0; i < 4; i++) {
                if (loaded.craftCounts[i] > 0) {
                    invCraft[i] = new ItemStack(loaded.craftIds[i], loaded.craftCounts[i]);
                }
            }
            for (int i = 0; i < 9; i++) {
                if (loaded.craftCounts[4 + i] > 0) {
                    tableCraft[i] = new ItemStack(loaded.craftIds[4 + i], loaded.craftCounts[4 + i]);
                }
            }
            refreshCraftResult(false);
            refreshCraftResult(true);
            lastHeldId = -1;
            addChat("Wczytano zapisany swiat. Milej gry!");
        } else {
            gamemode = mode;
            time = 0.08f;
            int[] spawn = world.getGenerator().findSpawn();
            int h = world.getGenerator().getHeight(spawn[0], spawn[1]);
            player.setPosition(spawn[0] + 0.5f, h + 1.02f, spawn[1] + 0.5f);
            addChat("Witaj w AiMINECRAFT 2.0! Wpisz /help, aby zobaczyc komendy.");
        }

        int pcx = Math.floorDiv((int) Math.floor(player.getPosition().x), Chunk.SIZE);
        int pcz = Math.floorDiv((int) Math.floor(player.getPosition().z), Chunk.SIZE);
        world.updateStreaming(pcx, pcz);
        world.updateMeshes(Integer.MAX_VALUE);

        setState(Screen.PLAY);
        window.setCursorCaptured(true);
        tipTimer = 15;
        wasInWater = false;
        System.out.println("Swiat gotowy (seed " + seed + ", chunkow: " + world.getChunkCount() + ")");
    }

    private void respawn() {
        int[] spawn = world.getGenerator().findSpawn();
        int h = world.getGenerator().getHeight(spawn[0], spawn[1]);
        player.setPosition(spawn[0] + 0.5f, h + 1.02f, spawn[1] + 0.5f);
    }

    private WorldSave.PlayerData collectPlayerData() {
        WorldSave.PlayerData p = new WorldSave.PlayerData();
        p.x = player.getPosition().x;
        p.y = player.getPosition().y;
        p.z = player.getPosition().z;
        p.yaw = player.getYaw();
        p.pitch = player.getPitch();
        p.flying = player.isFlying();
        p.gamemode = gamemode;
        p.time = time;
        p.selected = inventory.getSelected();
        for (int i = 0; i < Inventory.SIZE; i++) {
            ItemStack s = inventory.get(i);
            if (s != null) {
                p.invIds[i] = s.id;
                p.invCounts[i] = s.count;
            }
        }
        // UWAGA: zapis nie rusza stanu gry (autosave w tle nie moze
        // popsuc ukladanego craftingu ani rzeczy na kursorze).
        if (cursor != null) {
            p.cursorId = cursor.id;
            p.cursorCount = cursor.count;
        }
        for (int i = 0; i < 4; i++) {
            if (invCraft[i] != null) {
                p.craftIds[i] = invCraft[i].id;
                p.craftCounts[i] = invCraft[i].count;
            }
        }
        for (int i = 0; i < 9; i++) {
            if (tableCraft[i] != null) {
                p.craftIds[4 + i] = tableCraft[i].id;
                p.craftCounts[4 + i] = tableCraft[i].count;
            }
        }
        return p;
    }

    private void flushSave() {
        if (world == null || saver == null || player == null) {
            return;
        }
        world.flushModified();
        saver.flush(collectPlayerData());
    }

    private void rebuildWorld() {
        if (world == null) {
            return;
        }
        flushSave();
        int pcx = Math.floorDiv((int) Math.floor(player.getPosition().x), Chunk.SIZE);
        int pcz = Math.floorDiv((int) Math.floor(player.getPosition().z), Chunk.SIZE);
        world.close();
        world = new World(seed, atlas, settings.renderDistance);
        world.setSaver(saver);
        world.updateStreaming(pcx, pcz);
        world.updateMeshes(64);
    }

    // ================= Ekwipunek / crafting =================

    private void clickSlotArray(ItemStack[] arr, int index, boolean right) {
        ItemStack slotStack = arr[index];
        if (cursor == null) {
            if (slotStack == null) {
                return;
            }
            if (right) {
                int half = (slotStack.count + 1) / 2;
                cursor = new ItemStack(slotStack.id, half);
                slotStack.count -= half;
                if (slotStack.count <= 0) {
                    arr[index] = null;
                }
            } else {
                cursor = slotStack;
                arr[index] = null;
            }
            Sound.play("click");
        } else if (slotStack == null) {
            if (right) {
                arr[index] = new ItemStack(cursor.id, 1);
                cursor.count--;
                if (cursor.count <= 0) {
                    cursor = null;
                }
            } else {
                arr[index] = cursor;
                cursor = null;
            }
            Sound.play("click");
        } else if (slotStack.id == cursor.id) {
            if (right) {
                if (slotStack.count < Inventory.MAX_STACK) {
                    slotStack.count++;
                    cursor.count--;
                    if (cursor.count <= 0) {
                        cursor = null;
                    }
                    Sound.play("click");
                }
            } else {
                int space = Inventory.MAX_STACK - slotStack.count;
                int move = Math.min(space, cursor.count);
                if (move > 0) {
                    slotStack.count += move;
                    cursor.count -= move;
                    if (cursor.count <= 0) {
                        cursor = null;
                    }
                    Sound.play("click");
                }
            }
        } else if (!right) {
            arr[index] = cursor;
            cursor = slotStack;
            Sound.play("click");
        }
    }

    public void uiClickPlayerSlot(int index, boolean right) {
        ItemStack[] proxy = new ItemStack[Inventory.SIZE];
        for (int i = 0; i < Inventory.SIZE; i++) {
            proxy[i] = inventory.get(i);
        }
        ItemStack before = proxy[index];
        clickSlotArray(proxy, index, right);
        // Przepisz zmiany (proxy dzieli obiekty ItemStack z inventory).
        for (int i = 0; i < Inventory.SIZE; i++) {
            inventory.set(i, proxy[i]);
        }
        if (proxy[index] != before) {
            refreshCraftResult(false);
        }
    }

    public void uiClickInvCraft(int index, boolean right) {
        clickSlotArray(invCraft, index, right);
        refreshCraftResult(false);
    }

    public void uiClickTableCraft(int index, boolean right) {
        clickSlotArray(tableCraft, index, right);
        refreshCraftResult(true);
    }

    private void refreshCraftResult(boolean table) {
        int[] grid = new int[9];
        if (table) {
            for (int i = 0; i < 9; i++) {
                grid[i] = tableCraft[i] != null ? tableCraft[i].id : 0;
            }
        } else {
            grid[0] = invCraft[0] != null ? invCraft[0].id : 0;
            grid[1] = invCraft[1] != null ? invCraft[1].id : 0;
            grid[3] = invCraft[2] != null ? invCraft[2].id : 0;
            grid[4] = invCraft[3] != null ? invCraft[3].id : 0;
        }
        Recipes.Recipe r = Recipes.match(grid);
        if (r != null && !table && !Recipes.fits2x2(r)) {
            r = null;
        }
        ItemStack result = r != null ? new ItemStack(r.resultId, r.resultCount) : null;
        if (table) {
            craftResultTable = result;
        } else {
            craftResultInv = result;
        }
    }

    public void uiClickCraftResult(boolean table) {
        ItemStack result = table ? craftResultTable : craftResultInv;
        if (result == null) {
            return;
        }
        if (cursor == null) {
            cursor = result.copy();
        } else if (cursor.id == result.id
                && cursor.count + result.count <= Inventory.MAX_STACK) {
            cursor.count += result.count;
        } else {
            return;
        }
        Sound.play("pickup");
        if (gamemode == 0) {
            ItemStack[] grid = table ? tableCraft : invCraft;
            int n = table ? 9 : 4;
            for (int i = 0; i < n; i++) {
                if (grid[i] != null) {
                    grid[i].count--;
                    if (grid[i].count <= 0) {
                        grid[i] = null;
                    }
                }
            }
            refreshCraftResult(table);
        }
    }

    public void uiClickCreative(int listIndex, boolean right) {
        if (listIndex < 0 || listIndex >= creativeList.size()) {
            return;
        }
        inventory.add(creativeList.get(listIndex).id, right ? 1 : 64);
        Sound.play("pickup");
    }

    public void uiToggleRecipeBook() {
        recipeBookOpen = !recipeBookOpen;
        Sound.play("click");
    }

    /** Uklada recepture ze skladnikow z plecaka. */
    public void uiClickRecipe(int recipeIndex) {
        List<Recipes.Recipe> recipes = Recipes.all();
        if (recipeIndex < 0 || recipeIndex >= recipes.size()) {
            return;
        }
        Recipes.Recipe r = recipes.get(recipeIndex);
        // Zwroc stara siatke.
        for (int i = 0; i < 9; i++) {
            if (tableCraft[i] != null) {
                inventory.add(tableCraft[i].id, tableCraft[i].count);
                tableCraft[i] = null;
            }
        }
        // Policz potrzeby.
        List<Character> need = new ArrayList<>();
        for (String row : r.shape) {
            for (int i = 0; i < row.length(); i++) {
                char c = row.charAt(i);
                if (c != '.') {
                    need.add(c);
                }
            }
        }
        // Sprawdz, czy mamy wszystko (na kopii stanow).
        int[] have = new int[Inventory.SIZE];
        for (int i = 0; i < Inventory.SIZE; i++) {
            ItemStack s = inventory.get(i);
            have[i] = s != null ? s.count : 0;
        }
        List<int[]> take = new ArrayList<>(); // [slot, id]
        for (char key : need) {
            boolean found = false;
            for (int i = 0; i < Inventory.SIZE && !found; i++) {
                ItemStack s = inventory.get(i);
                if (s != null && have[i] > 0 && Recipes.matches(key, s.id)) {
                    have[i]--;
                    take.add(new int[]{i, s.id});
                    found = true;
                }
            }
            if (!found) {
                addChat("Brakuje skladnikow na: " + r.name);
                Sound.play("break");
                return;
            }
        }
        // Zabierz i uloz w lewym gornym rogu siatki.
        int ptr = 0;
        for (int y = 0; y < r.shape.length; y++) {
            String row = r.shape[y];
            for (int x = 0; x < row.length(); x++) {
                if (row.charAt(x) == '.') {
                    continue;
                }
                int[] t = take.get(ptr++);
                ItemStack s = inventory.get(t[0]);
                s.count--;
                if (s.count <= 0) {
                    inventory.set(t[0], null);
                }
                int gi = y * 3 + x;
                if (tableCraft[gi] != null && tableCraft[gi].id == t[1]) {
                    tableCraft[gi].count++;
                } else {
                    tableCraft[gi] = new ItemStack(t[1], 1);
                }
            }
        }
        refreshCraftResult(true);
        Sound.play("click");
    }

    private void returnCraftItems() {
        for (int i = 0; i < 4; i++) {
            if (invCraft[i] != null) {
                inventory.add(invCraft[i].id, invCraft[i].count);
                invCraft[i] = null;
            }
        }
        for (int i = 0; i < 9; i++) {
            if (tableCraft[i] != null) {
                inventory.add(tableCraft[i].id, tableCraft[i].count);
                tableCraft[i] = null;
            }
        }
        craftResultInv = null;
        craftResultTable = null;
        if (cursor != null) {
            int left = inventory.add(cursor.id, cursor.count);
            if (left == 0) {
                cursor = null;
            } else {
                cursor.count = left;
            }
        }
    }

    private void clearCrafting() {
        for (int i = 0; i < 4; i++) {
            invCraft[i] = null;
        }
        for (int i = 0; i < 9; i++) {
            tableCraft[i] = null;
        }
        craftResultInv = null;
        craftResultTable = null;
        cursor = null;
        recipeBookOpen = false;
    }

    // ================= Chat i komendy =================

    public void addChat(String msg) {
        String clean = GuiRenderer.normalize(msg);
        if (clean.length() > 70) {
            clean = clean.substring(0, 70);
        }
        chatLines.add(clean);
        chatTimes.add(window.getTime());
        while (chatLines.size() > 50) {
            chatLines.remove(0);
            chatTimes.remove(0);
        }
        System.out.println("[chat] " + clean);
    }

    public double chatAge(int index) {
        if (index < 0 || index >= chatTimes.size()) {
            return 999;
        }
        return window.getTime() - chatTimes.get(index);
    }

    private void sendChat() {
        String text = chatInput.toString().trim();
        chatInput.setLength(0);
        if (!text.isEmpty()) {
            if (text.startsWith("/")) {
                runCommand(text.substring(1).trim());
            } else {
                addChat("<Gracz> " + text);
            }
        }
        closeGuiToPlay();
    }

    private void runCommand(String line) {
        if (line.isEmpty()) {
            return;
        }
        String[] p = line.split("\\s+");
        String cmd = p[0].toLowerCase();
        try {
            switch (cmd) {
                case "help":
                    if (p.length > 1 && p[1].equalsIgnoreCase("items")) {
                        addChat("Bloki: 1 trawa, 2 ziemia, 3 kamien, 4/17/19 pnie,");
                        addChat("9 bruk, 10 deski, 11 szyba, 12-16 rudy, 25 pochodnia,");
                        addChat("26 stol, 27 kaktus, 29 obsydian, 31 biblioteczka,");
                        addChat("32 dynia, 33 arbuz, 35 cegly. Itemy: 100 patyk,");
                        addChat("101 wegiel, 102 zelazo, 103 zloto, 104 diament,");
                        addChat("105 redstone, 110-114 drewno, 120-124 kamienne.");
                    } else {
                        addChat("Komendy: /help items, /seed, /spawn, /kill, /clear,");
                        addChat("/tp x z, /time day|night, /day, /night, /fly,");
                        addChat("/gamemode 0|1, /give id [n], /setblock x y z id,");
                        addChat("/fill x1 y1 z1 x2 y2 z2 id");
                    }
                    break;
                case "seed":
                    addChat("Seed: " + seed);
                    break;
                case "spawn":
                    respawn();
                    addChat("Teleportowano na spawn.");
                    break;
                case "kill":
                    respawn();
                    addChat("Zrespawniono.");
                    break;
                case "clear":
                    inventory.clear();
                    cursor = null;
                    addChat("Wyczyszczono ekwipunek.");
                    break;
                case "tp":
                    if (p.length == 3) {
                        teleport(parseNum(p[1]), null, parseNum(p[2]));
                    } else if (p.length == 4) {
                        teleport(parseNum(p[1]), parseNum(p[2]), parseNum(p[3]));
                    } else {
                        addChat("Uzycie: /tp x z  lub  /tp x y z");
                    }
                    break;
                case "time":
                    if (p.length >= 2) {
                        setTimeArg(p[1]);
                    } else {
                        addChat("Uzycie: /time day|night|0.0-1.0");
                    }
                    break;
                case "day":
                    time = 0.1f;
                    addChat("Ustawiono dzien.");
                    break;
                case "night":
                    time = 0.6f;
                    addChat("Ustawiono noc.");
                    break;
                case "fly":
                    player.setFlying(!player.isFlying());
                    addChat(player.isFlying() ? "Latanie: WLACZONE" : "Latanie: WYLACZONE");
                    break;
                case "gamemode":
                    if (p.length >= 2 && (p[1].equals("0") || p[1].equals("1"))) {
                        gamemode = Integer.parseInt(p[1]);
                        closeGuiToPlay();
                        addChat("Tryb: " + (gamemode == 1 ? "Creative" : "Survival"));
                    } else {
                        addChat("Uzycie: /gamemode 0|1");
                    }
                    break;
                case "give": {
                    if (p.length < 2) {
                        addChat("Uzycie: /give id [liczba]");
                        break;
                    }
                    int id = Integer.parseInt(p[1]);
                    if (Item.get(id) == null) {
                        addChat("Nie ma przedmiotu o id " + id);
                        break;
                    }
                    int count = 1;
                    if (p.length >= 3) {
                        count = Math.max(1, Math.min(64, Integer.parseInt(p[2])));
                    }
                    int left = inventory.add(id, count);
                    addChat(left == 0 ? "Dano: " + Item.get(id).name + " x" + count
                            : "Brak miejsca w ekwipunku!");
                    Sound.play("pickup");
                    break;
                }
                case "setblock": {
                    if (p.length < 5) {
                        addChat("Uzycie: /setblock x y z id");
                        break;
                    }
                    int x = Integer.parseInt(p[1]);
                    int y = Integer.parseInt(p[2]);
                    int z = Integer.parseInt(p[3]);
                    int id = Integer.parseInt(p[4]);
                    if (id < 0 || id > 35) {
                        addChat("Id bloku musi byc 0-35.");
                        break;
                    }
                    if (y < 0 || y >= Chunk.HEIGHT) {
                        addChat("Y musi byc 0-" + (Chunk.HEIGHT - 1) + ".");
                        break;
                    }
                    world.setBlock(x, y, z, (byte) id);
                    addChat("Ustawiono blok " + Block.fromId(id).displayName + ".");
                    break;
                }
                case "fill": {
                    if (p.length < 8) {
                        addChat("Uzycie: /fill x1 y1 z1 x2 y2 z2 id");
                        break;
                    }
                    int x1 = Integer.parseInt(p[1]);
                    int y1 = Integer.parseInt(p[2]);
                    int z1 = Integer.parseInt(p[3]);
                    int x2 = Integer.parseInt(p[4]);
                    int y2 = Integer.parseInt(p[5]);
                    int z2 = Integer.parseInt(p[6]);
                    int id = Integer.parseInt(p[7]);
                    if (id < 0 || id > 35) {
                        addChat("Id bloku musi byc 0-35.");
                        break;
                    }
                    int volume = (Math.abs(x2 - x1) + 1) * (Math.abs(y2 - y1) + 1) * (Math.abs(z2 - z1) + 1);
                    if (volume > 8192) {
                        addChat("Za duzy obszar (max 8192 bloki).");
                        break;
                    }
                    int n = 0;
                    for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
                        for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++) {
                            for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
                                if (y < 0 || y >= Chunk.HEIGHT) {
                                    continue;
                                }
                                world.setBlock(x, y, z, (byte) id);
                                n++;
                            }
                        }
                    }
                    addChat("Wypelniono " + n + " blokow.");
                    break;
                }
                default:
                    addChat("Nieznana komenda. Wpisz /help");
                    break;
            }
        } catch (NumberFormatException e) {
            addChat("Zla liczba w komendzie.");
        }
    }

    private double parseNum(String s) {
        return Double.parseDouble(s);
    }

    private void teleport(double x, Double y, double z) {
        float yy;
        if (y == null) {
            int h = world.getGenerator().getHeight((int) Math.floor(x), (int) Math.floor(z));
            yy = h + 1.02f;
        } else {
            yy = (float) Math.max(1, Math.min(Chunk.HEIGHT - 2, y));
        }
        player.setPosition((float) x + 0.5f, yy, (float) z + 0.5f);
        addChat(String.format("Teleportowano: %.0f / %.0f / %.0f", x, (double) yy, z));
    }

    private void setTimeArg(String arg) {
        if (arg.equalsIgnoreCase("day")) {
            time = 0.1f;
            addChat("Ustawiono dzien.");
        } else if (arg.equalsIgnoreCase("night")) {
            time = 0.6f;
            addChat("Ustawiono noc.");
        } else {
            float t = Float.parseFloat(arg);
            if (t < 0 || t >= 1) {
                addChat("Czas musi byc 0.0-0.99.");
                return;
            }
            time = t;
            addChat("Ustawiono czas: " + timeName(time) + ".");
        }
    }

    // ================= Akcje UI (wola je Screens) =================

    public void uiPlayMenu() {
        Sound.play("click");
        refreshSaveInfo();
        if (saveSeed != null) {
            seedField.setLength(0);
            seedField.append(saveSeed);
        }
        setState(Screen.PLAY_MENU);
    }

    public void uiBackToTitle() {
        Sound.play("click");
        setState(Screen.TITLE);
    }

    public void uiQuitGame() {
        Sound.play("click");
        flushSave();
        window.requestClose();
    }

    public void uiToggleNewGamemode() {
        newGamemode = 1 - newGamemode;
        Sound.play("click");
    }

    public void uiStartPlay() {
        Sound.play("click");
        if (saveExists && saveSeed != null) {
            startWorld(saveSeed, 0, false);
        } else {
            startWorld(parseSeed(), newGamemode, true);
        }
    }

    public void uiNewWorld() {
        Sound.play("click");
        startWorld(parseSeed(), newGamemode, true);
    }

    public void uiDeleteSave() {
        Sound.play("click");
        if (saveExists) {
            setState(Screen.CONFIRM_DELETE);
        }
    }

    public void uiConfirmDelete() {
        Sound.play("click");
        WorldSave.delete();
        refreshSaveInfo();
        setState(Screen.PLAY_MENU);
    }

    public void uiCancelDelete() {
        Sound.play("click");
        setState(Screen.PLAY_MENU);
    }

    public void uiOpenSettings(Screen ret) {
        Sound.play("click");
        settingsReturn = ret;
        setState(Screen.SETTINGS);
        window.setCursorCaptured(false);
    }

    public void uiSettingsDone() {
        Sound.play("click");
        settings.save();
        applySettings();
        setState(settingsReturn);
        if (world != null && settingsReturn == Screen.PAUSE) {
            window.setCursorCaptured(false);
        }
    }

    public void uiSetMinus(int row) {
        if (row == 0 && settings.renderDistance > 2) {
            settings.renderDistance--;
        } else if (row == 1 && settings.fov > 60) {
            settings.fov -= 5;
        } else if (row == 2 && settings.sensitivity > 0.25f) {
            settings.sensitivity = Math.round((settings.sensitivity - 0.1f) * 10) / 10f;
        } else {
            return;
        }
        Sound.play("click");
    }

    public void uiSetPlus(int row) {
        if (row == 0 && settings.renderDistance < 12) {
            settings.renderDistance++;
        } else if (row == 1 && settings.fov < 110) {
            settings.fov += 5;
        } else if (row == 2 && settings.sensitivity < 3.0f) {
            settings.sensitivity = Math.round((settings.sensitivity + 0.1f) * 10) / 10f;
        } else {
            return;
        }
        Sound.play("click");
    }

    public void uiToggleVsync() {
        settings.vsync = !settings.vsync;
        Sound.play("click");
    }

    public void uiToggleSound() {
        settings.sound = !settings.sound;
        Sound.setEnabled(settings.sound);
        Sound.play("click");
    }

    public void uiResume() {
        Sound.play("click");
        closeGuiToPlay();
    }

    public void uiSaveAndQuitToMenu() {
        Sound.play("click");
        flushSave();
        world.close();
        world = null;
        player = null;
        window.setCursorCaptured(false);
        refreshSaveInfo();
        setState(Screen.PLAY_MENU);
    }

    public void uiAdmin(int button) {
        Sound.play("click");
        switch (button) {
            case 0:
                runCommand("time day");
                break;
            case 1:
                runCommand("time night");
                break;
            case 2:
                runCommand("gamemode 0");
                break;
            case 3:
                runCommand("gamemode 1");
                break;
            case 4:
                runCommand("fly");
                break;
            case 5:
                runCommand("spawn");
                break;
            case 6:
                inventory.add(10, 32);
                inventory.add(100, 16);
                inventory.add(120, 1);
                inventory.add(25, 16);
                addChat("Dano zestaw startowy.");
                Sound.play("pickup");
                break;
            case 7:
                runCommand("kill");
                break;
        }
    }

    public int maxCreativeScroll() {
        int totalRows = (creativeList.size() + 8) / 9;
        return Math.max(0, totalRows - 5);
    }

    public List<String> debugLines() {
        List<String> lines = new ArrayList<>();
        lines.add("AiMINECRAFT 2.0 | FPS: " + fps);
        if (player != null && world != null) {
            Vector3f p = player.getPosition();
            lines.add(String.format("XYZ: %.2f / %.2f / %.2f", p.x, p.y, p.z));
            int cx = Math.floorDiv((int) Math.floor(p.x), Chunk.SIZE);
            int cz = Math.floorDiv((int) Math.floor(p.z), Chunk.SIZE);
            lines.add("Chunk: " + cx + " / " + cz + " (ladowanych: " + world.getChunkCount() + ")");
            lines.add("Indeksow: " + world.getTotalIndices());
            lines.add("Biom: " + world.getGenerator().getBiome((int) Math.floor(p.x), (int) Math.floor(p.z)));
            lines.add("Seed: " + seed + ", czas: " + timeName(time));
        }
        return lines;
    }

    // ================= Pomocnicze =================

    private void setState(Screen s) {
        state = s;
        breakProgress = -1;
    }

    private void openInventory() {
        state = Screen.INVENTORY;
        window.setCursorCaptured(false);
        creativeScroll = 0;
        refreshCraftResult(false);
        Sound.play("click");
    }

    private void closeGuiToPlay() {
        returnCraftItems();
        state = Screen.PLAY;
        window.setCursorCaptured(true);
    }

    private void refreshSaveInfo() {
        saveExists = WorldSave.exists();
        saveSeed = WorldSave.peekSeed();
        if (saveExists && saveSeed == null) {
            saveExists = false;
        }
        saveInfo = saveExists ? WorldSave.peekInfo() : "";
    }

    private long parseSeed() {
        String s = seedField.toString().trim();
        if (s.isEmpty()) {
            return random.nextLong();
        }
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return s.hashCode();
        }
    }

    private void applySettings() {
        window.setVsync(settings.vsync);
        Sound.setEnabled(settings.sound);
        if (player != null) {
            player.setSensitivity(settings.sensitivity);
        }
        if (world != null) {
            rebuildWorld();
        }
    }

    private void cleanup() {
        flushSave();
        if (world != null) {
            world.close();
        }
        gui.close();
        font.close();
        items.close();
        atlas.close();
        blockShader.close();
        waterShader.close();
        hud.close();
        window.destroy();
    }
}
