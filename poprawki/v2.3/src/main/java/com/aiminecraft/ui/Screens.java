package com.aiminecraft.ui;

import com.aiminecraft.Game;
import com.aiminecraft.item.Inventory;
import com.aiminecraft.item.Item;
import com.aiminecraft.item.ItemStack;
import com.aiminecraft.item.Recipes;
import com.aiminecraft.render.FontAtlas;
import com.aiminecraft.render.GuiRenderer;
import com.aiminecraft.world.Block;

import java.util.List;

/**
 * Wszystkie ekrany GUI: rysowanie + obsluga klikow.
 * Layout slotow jest wspolny dla draw() i click().
 */
public final class Screens {

    private Screens() {
    }

    // ---------- Kolory ----------
    private static final int BLACK = 0xFF000000;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int YELLOW = 0xFFFFFF55;
    private static final int GRAY = 0xFFAAAAAA;
    private static final int DARK_BG = 0xE02B2B2B;
    private static final int PANEL = 0xF03C3C3C;
    private static final int PANEL_EDGE = 0xFF5A5A5A;
    private static final int SLOT_BG = 0xFF1E1E1E;
    private static final int SLOT_EDGE = 0xFF6B6B6B;
    private static final int BTN_BG = 0xFF4C4C4C;
    private static final int BTN_BG_HOVER = 0xFF5F7DC0;
    private static final int BTN_EDGE = 0xFF0A0A0A;
    private static final int GREEN = 0xFF55FF55;
    private static final int RED = 0xFFFF5555;

    // ---------- Rysowanie glowne ----------

    public static void draw(Game g) {
        GuiRenderer r = g.gui;
        int w = g.windowW;
        int h = g.windowH;
        switch (g.state) {
            case TITLE:
                drawTitle(g, r, w, h);
                break;
            case PLAY_MENU:
                drawPlayMenu(g, r, w, h);
                break;
            case CONFIRM_DELETE:
                drawPlayMenu(g, r, w, h);
                drawConfirmDelete(g, r, w, h);
                break;
            case SETTINGS:
                drawSettings(g, r, w, h);
                break;
            case PLAY:
                drawHud(g, r, w, h);
                break;
            case PAUSE:
                drawHud(g, r, w, h);
                drawPause(g, r, w, h);
                break;
            case INVENTORY:
                drawHud(g, r, w, h);
                if (g.gamemode == 1) {
                    drawCreative(g, r, w, h);
                } else {
                    drawInventory(g, r, w, h);
                }
                drawCursorStack(g, r);
                break;
            case CRAFTING:
                drawHud(g, r, w, h);
                drawCrafting(g, r, w, h);
                drawCursorStack(g, r);
                break;
            case CHAT:
                drawHud(g, r, w, h);
                drawChatInput(g, r, w, h);
                break;
            case ADMIN:
                drawHud(g, r, w, h);
                drawAdmin(g, r, w, h);
                break;
        }
    }

    /** Klikniecie myszy na ekranie GUI. */
    public static void click(Game g, float x, float y, boolean right) {
        switch (g.state) {
            case TITLE:
                clickTitle(g, x, y);
                break;
            case PLAY_MENU:
                clickPlayMenu(g, x, y);
                break;
            case CONFIRM_DELETE:
                clickConfirmDelete(g, x, y);
                break;
            case SETTINGS:
                clickSettings(g, x, y);
                break;
            case PAUSE:
                clickPause(g, x, y);
                break;
            case INVENTORY:
                if (g.gamemode == 1) {
                    clickCreative(g, x, y, right);
                } else {
                    clickInventory(g, x, y, right);
                }
                break;
            case CRAFTING:
                clickCrafting(g, x, y, right);
                break;
            case ADMIN:
                clickAdmin(g, x, y);
                break;
            default:
                break;
        }
    }

    // ---------- Pomocnicze ----------

    private static boolean inside(float x, float y, float rx, float ry, float rw, float rh) {
        return x >= rx && x < rx + rw && y >= ry && y < rh + rh;
    }

    private static void button(Game g, GuiRenderer r, float x, float y, float w, float h, String label) {
        boolean hover = inside(g.mouseX, g.mouseY, x, y, w, h);
        // Styl jak w MC: fazowana kostka, po najechaniu niebieska + zolty tekst.
        int bg = hover ? 0xFF5B7BD5 : 0xFF525252;
        int top = hover ? 0xFF8FA5FF : 0xFF7D7D7D;
        int bottom = hover ? 0xFF2E3F8F : 0xFF2B2B2B;
        r.fillRect(x, y, w, h, BLACK);
        r.fillRect(x + 2, y + 2, w - 4, h - 4, bg);
        r.fillRect(x + 2, y + 2, w - 4, 3, top);
        r.fillRect(x + 2, y + h - 5, w - 4, 3, bottom);
        int textCol = hover ? 0xFFFFFFAA : WHITE;
        r.drawCenteredText(g.font, label, x + w / 2f, y + h / 2f - 8, 1.0f, textCol);
    }

    private static void slot(Game g, GuiRenderer r, float x, float y, float s, ItemStack stack) {
        r.fillRect(x, y, s, s, SLOT_BG);
        r.drawBorder(x, y, s, s, 2, SLOT_EDGE);
        if (stack != null && !stack.isEmpty()) {
            drawStackIcon(g, r, stack, x + 4, y + 4, s - 8);
            if (stack.count > 1) {
                String n = Integer.toString(stack.count);
                float tw = r.textWidth(g.font, n, 1.0f);
                r.drawTextShadow(g.font, n, x + s - tw - 4, y + s - 18, 1.0f, WHITE);
            }
        }
    }

    private static void drawStackIcon(Game g, GuiRenderer r, ItemStack stack, float x, float y, float size) {
        Item it = stack.item();
        if (it == null) {
            return;
        }
        if (it.iconAtlas == Item.ATLAS_BLOCKS) {
            r.drawBlockIcon(g.atlas, it.iconTile, x, y, size);
        } else {
            r.drawItemIcon(g.items, it.iconTile, x, y, size);
        }
    }

    private static void panel(Game g, GuiRenderer r, float x, float y, float w, float h) {
        r.fillRect(x, y, w, h, PANEL);
        r.drawBorder(x, y, w, h, 3, PANEL_EDGE);
        r.drawBorder(x + 3, y + 3, w - 6, h - 6, 1, BLACK);
    }

    private static void tooltip(Game g, GuiRenderer r, String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        float tw = r.textWidth(g.font, text, 1.0f);
        float x = Math.min(g.mouseX + 14, g.windowW - tw - 16);
        float y = Math.max(4, g.mouseY - 26);
        r.fillRect(x - 4, y - 4, tw + 8, 24, 0xEE101010);
        r.drawBorder(x - 4, y - 4, tw + 8, 24, 1, 0xFF5555FF);
        r.drawText(g.font, text, x, y, 1.0f, WHITE);
    }

    /** Nazwa itemu pod kursorem (do tooltipu). */
    private static String hoveredStackName(Game g, float x, float y, float s, ItemStack stack) {
        if (stack != null && !stack.isEmpty() && inside(g.mouseX, g.mouseY, x, y, s, s)) {
            Item it = stack.item();
            return it != null ? it.name : null;
        }
        return null;
    }

    // ---------- Menu tytulowe ----------

    private static void drawTitle(Game g, GuiRenderer r, int w, int h) {
        // Niebo: gladki gradient.
        int bands = 40;
        for (int i = 0; i < bands; i++) {
            float t = i / (float) (bands - 1);
            int cr = (int) (30 + t * 95);
            int cg = (int) (55 + t * 130);
            int cb = (int) (125 + t * 115);
            r.fillRect(0, h * t, w, (float) h / bands + 1, 0xFF000000 | (cr << 16) | (cg << 8) | cb);
        }
        // Slonce z poswiata.
        float sunX = w * 0.8f;
        float sunY = h * 0.12f;
        r.fillRect(sunX - 40, sunY - 40, 120, 120, 0x33FFF3A0);
        r.fillRect(sunX - 24, sunY - 24, 88, 88, 0x55FFF3A0);
        r.fillRect(sunX, sunY, 40, 40, 0xFFFFE27A);
        // Dryfujace chmury.
        drawCloud(g, r, w, h, 0.1f, 0.1f, 1.0f, 10.0);
        drawCloud(g, r, w, h, 0.5f, 0.2f, 1.5f, 16.0);
        drawCloud(g, r, w, h, 0.8f, 0.06f, 0.8f, 7.0);
        // Ziemia z prawdziwych tekstur blokow.
        float tile = 48;
        float groundTop = h - tile * 3;
        int cols = (int) Math.ceil(w / tile) + 1;
        for (int i = 0; i < cols; i++) {
            float x = i * tile;
            r.drawBlockIcon(g.atlas, Block.GRASS.tileSide, x, groundTop, tile);
            r.drawBlockIcon(g.atlas, Block.DIRT.tileSide, x, groundTop + tile, tile);
            r.drawBlockIcon(g.atlas, Block.DIRT.tileSide, x, groundTop + tile * 2, tile + 4);
        }
        // Logo z cieniem.
        String logo = "AiMINECRAFT";
        r.drawTextShadow(g.font, logo, (w - r.textWidth(g.font, logo, 4.0f)) / 2f,
                h * 0.16f, 4.0f, WHITE);
        r.drawCenteredText(g.font, "wersja 2.0", w / 2f, h * 0.16f + 72, 1.0f, YELLOW);

        float bw = 340;
        float bx = (w - bw) / 2f;
        float by = h * 0.45f;
        button(g, r, bx, by, bw, 44, "Graj");
        button(g, r, bx, by + 54, bw, 44, "Ustawienia");
        button(g, r, bx, by + 108, bw, 44, "Wyjdz");
        String footer = "WASD + mysz | E - ekwipunek | T - chat | G - panel admina";
        r.drawTextShadow(g.font, footer, (w - r.textWidth(g.font, footer, 1.0f)) / 2f,
                h - 158, 1.0f, WHITE);
    }

    /** Pikselowa chmura dryfujaca przez niebo w menu. */
    private static void drawCloud(Game g, GuiRenderer r, int w, int h,
                                  float basePos, float yPos, float scale, double speed) {
        float span = w + 480;
        float x = (float) ((basePos * span + g.uiTime * speed) % span) - 240;
        float y = h * yPos;
        float s = 26 * scale;
        int col = 0xAAFFFFFF;
        r.fillRect(x, y + s * 0.5f, s * 6, s, col);
        r.fillRect(x + s, y, s * 3, s * 1.2f, col);
        r.fillRect(x + s * 3.5f, y + s * 0.2f, s * 2, s, col);
    }

    private static void clickTitle(Game g, float x, float y) {
        float bw = 340;
        float bx = (g.windowW - bw) / 2f;
        float by = g.windowH * 0.45f;
        if (inside(x, y, bx, by, bw, 44)) {
            g.uiPlayMenu();
        } else if (inside(x, y, bx, by + 54, bw, 44)) {
            g.uiOpenSettings(Game.Screen.TITLE);
        } else if (inside(x, y, bx, by + 108, bw, 44)) {
            g.uiQuitGame();
        }
    }

    // ---------- Wybor swiata ----------

    private static void drawPlayMenu(Game g, GuiRenderer r, int w, int h) {
        r.fillRect(0, 0, w, h, DARK_BG);
        float pw = 560;
        float ph = 430;
        float px = (w - pw) / 2f;
        float py = (h - ph) / 2f;
        panel(g, r, px, py, pw, ph);
        r.drawCenteredText(g.font, "Wybierz swiat", w / 2f, py + 18, 2.0f, WHITE);

        // Status zapisu.
        r.drawText(g.font, "Zapisany swiat:", px + 30, py + 70, 1.0f, GRAY);
        if (g.saveExists) {
            r.drawText(g.font, g.saveInfo, px + 30, py + 92, 1.0f, GREEN);
        } else {
            r.drawText(g.font, "brak zapisu - zacznie sie nowy swiat", px + 30, py + 92, 1.0f, YELLOW);
        }

        // Seed.
        r.drawText(g.font, "Seed (pusty = losowy):", px + 30, py + 140, 1.0f, GRAY);
        r.fillRect(px + 30, py + 162, pw - 60, 34, BLACK);
        r.drawBorder(px + 30, py + 162, pw - 60, 34, 2, SLOT_EDGE);
        String seed = g.seedField.toString();
        if (seed.length() > 24) {
            seed = seed.substring(seed.length() - 24);
        }
        r.drawText(g.font, seed + "_", px + 38, py + 170, 1.0f, WHITE);

        // Tryb gry.
        r.drawText(g.font, "Tryb nowego swiata:", px + 30, py + 214, 1.0f, GRAY);
        String mode = g.newGamemode == 1 ? "Creative" : "Survival";
        button(g, r, px + 30, py + 236, pw - 60, 36, "Tryb: " + mode + " (kliknij, aby zmienic)");

        float bw = (pw - 90) / 2f;
        button(g, r, px + 30, py + 288, bw, 40, g.saveExists ? "Graj (kontynuuj)" : "Graj (nowy swiat)");
        button(g, r, px + 45 + bw, py + 288, bw, 40, "Nowy swiat");
        button(g, r, px + 30, py + 338, bw, 40, "Usun zapis");
        button(g, r, px + 45 + bw, py + 338, bw, 40, "Wroc");
    }

    private static void clickPlayMenu(Game g, float x, float y) {
        float pw = 560;
        float ph = 430;
        float px = (g.windowW - pw) / 2f;
        float py = (g.windowH - ph) / 2f;
        if (inside(x, y, px + 30, py + 236, pw - 60, 36)) {
            g.uiToggleNewGamemode();
            return;
        }
        float bw = (pw - 90) / 2f;
        if (inside(x, y, px + 30, py + 288, bw, 40)) {
            g.uiStartPlay();
        } else if (inside(x, y, px + 45 + bw, py + 288, bw, 40)) {
            g.uiNewWorld();
        } else if (inside(x, y, px + 30, py + 338, bw, 40)) {
            g.uiDeleteSave();
        } else if (inside(x, y, px + 45 + bw, py + 338, bw, 40)) {
            g.uiBackToTitle();
        }
    }

    private static void drawConfirmDelete(Game g, GuiRenderer r, int w, int h) {
        r.fillRect(0, 0, w, h, 0xAA000000);
        float pw = 440;
        float ph = 170;
        float px = (w - pw) / 2f;
        float py = (h - ph) / 2f;
        panel(g, r, px, py, pw, ph);
        r.drawCenteredText(g.font, "Usunac zapisany swiat?", w / 2f, py + 24, 1.5f, RED);
        r.drawCenteredText(g.font, "Tej operacji nie da sie cofnac!", w / 2f, py + 58, 1.0f, GRAY);
        button(g, r, px + 30, py + 100, 175, 40, "Tak, usun");
        button(g, r, px + 235, py + 100, 175, 40, "Anuluj");
    }

    private static void clickConfirmDelete(Game g, float x, float y) {
        float pw = 440;
        float ph = 170;
        float px = (g.windowW - pw) / 2f;
        float py = (g.windowH - ph) / 2f;
        if (inside(x, y, px + 30, py + 100, 175, 40)) {
            g.uiConfirmDelete();
        } else if (inside(x, y, px + 235, py + 100, 175, 40)) {
            g.uiCancelDelete();
        }
    }

    // ---------- Ustawienia ----------

    private static final String[] SETTING_NAMES = {
            "Zasieg widzenia", "Pole widzenia (FOV)", "Czulosc myszy", "VSync", "Dzwiek"
    };

    private static String settingValue(Game g, int row) {
        switch (row) {
            case 0: return Integer.toString(g.settings.renderDistance);
            case 1: return Integer.toString(g.settings.fov);
            case 2: return String.format("%.1f", g.settings.sensitivity);
            case 3: return g.settings.vsync ? "WL" : "WYL";
            default: return g.settings.sound ? "WL" : "WYL";
        }
    }

    private static void drawSettings(Game g, GuiRenderer r, int w, int h) {
        r.fillRect(0, 0, w, h, DARK_BG);
        float pw = 520;
        float ph = 440;
        float px = (w - pw) / 2f;
        float py = (h - ph) / 2f;
        panel(g, r, px, py, pw, ph);
        r.drawCenteredText(g.font, "Ustawienia", w / 2f, py + 18, 2.0f, WHITE);
        float y = py + 70;
        for (int row = 0; row < 5; row++) {
            r.drawText(g.font, SETTING_NAMES[row], px + 30, y + 8, 1.0f, GRAY);
            if (row < 3) {
                button(g, r, px + pw - 190, y, 40, 34, "-");
                String v = settingValue(g, row);
                r.drawCenteredText(g.font, v, px + pw - 120, y + 8, 1.0f, WHITE);
                button(g, r, px + pw - 70, y, 40, 34, "+");
            } else {
                button(g, r, px + pw - 190, y, 160, 34, settingValue(g, row));
            }
            y += 46;
        }
        r.drawText(g.font, "Zmiana zasiegu przebudowuje swiat (chwile potrwa).",
                px + 30, y + 2, 1.0f, GRAY);
        button(g, r, px + 30, py + ph - 60, pw - 60, 40, "Gotowe");
    }

    private static void clickSettings(Game g, float x, float y) {
        float pw = 520;
        float ph = 440;
        float px = (g.windowW - pw) / 2f;
        float py = (g.windowH - ph) / 2f;
        float ry = py + 70;
        for (int row = 0; row < 5; row++) {
            if (row < 3) {
                if (inside(x, y, px + pw - 190, ry, 40, 34)) {
                    g.uiSetMinus(row);
                    return;
                }
                if (inside(x, y, px + pw - 70, ry, 40, 34)) {
                    g.uiSetPlus(row);
                    return;
                }
            } else if (inside(x, y, px + pw - 190, ry, 160, 34)) {
                if (row == 3) {
                    g.uiToggleVsync();
                } else {
                    g.uiToggleSound();
                }
                return;
            }
            ry += 46;
        }
        if (inside(x, y, px + 30, py + ph - 60, pw - 60, 40)) {
            g.uiSettingsDone();
        }
    }

    // ---------- HUD ----------

    private static float hotbarX(int w) {
        return (w - (9 * 48)) / 2f;
    }

    private static float hotbarY(int h) {
        return h - 56;
    }

    private static void drawHud(Game g, GuiRenderer r, int w, int h) {
        FontAtlas f = g.font;
        // Hotbar.
        float hx = hotbarX(w);
        float hy = hotbarY(h);
        for (int i = 0; i < Inventory.HOTBAR; i++) {
            float sx = hx + i * 48;
            slot(g, r, sx, hy, 44, g.inventory.get(i));
            if (i == g.inventory.getSelected()) {
                r.drawBorder(sx - 2, hy - 2, 48, 48, 3, WHITE);
            }
            // Numerki 1-9.
            r.drawText(f, Integer.toString(i + 1), sx + 4, hy - 18, 0.75f, GRAY);
        }
        // Nazwa trzymanego przedmiotu.
        if (g.heldNameTimer > 0 && g.heldName != null) {
            r.drawCenteredText(f, g.heldName, w / 2f, hy - 44, 1.0f, WHITE);
        }
        // Pasek kopania.
        if (g.breakProgress >= 0) {
            float bw = 200;
            float bx = (w - bw) / 2f;
            float by = h / 2f + 40;
            r.fillRect(bx, by, bw, 12, BLACK);
            r.drawBorder(bx, by, bw, 12, 2, SLOT_EDGE);
            r.fillRect(bx + 2, by + 2, (bw - 4) * Math.min(1, g.breakProgress), 8, GREEN);
        }
        // Chat (ostatnie linie).
        drawChatHistory(g, r, w, h, false);
        // Podpowiedz sterowania na starcie.
        if (g.tipTimer > 0) {
            r.drawCenteredText(f, "WASD - ruch, Spacja - skok/plywanie, E - ekwipunek, T - chat, G - panel admina",
                    w / 2f, 8, 1.0f, YELLOW);
        }
        // Ekran debug (F3).
        if (g.debugOverlay) {
            float y = 8;
            for (String line : g.debugLines()) {
                r.drawTextShadow(f, line, 8, y, 1.0f, WHITE);
                y += 18;
            }
        }
    }

    private static void drawChatHistory(Game g, GuiRenderer r, int w, int h, boolean chatOpen) {
        List<String> lines = g.chatLines;
        int max = chatOpen ? 12 : 8;
        int count = Math.min(max, lines.size());
        float y = h - 220;
        if (chatOpen) {
            y = h - 260;
        }
        for (int i = lines.size() - count; i < lines.size(); i++) {
            if (i < 0) {
                continue;
            }
            // Starsze niz 12 s znikaja (gdy chat zamkniety).
            if (!chatOpen && g.chatAge(i) > 12.0) {
                y += 18;
                continue;
            }
            r.drawTextShadow(g.font, lines.get(i), 10, y, 1.0f, WHITE);
            y += 18;
        }
    }

    private static void drawChatInput(Game g, GuiRenderer r, int w, int h) {
        drawChatHistory(g, r, w, h, true);
        r.fillRect(8, h - 200, w - 16, 30, 0xCC000000);
        r.drawBorder(8, h - 200, w - 16, 30, 1, SLOT_EDGE);
        String text = g.chatInput.toString();
        if (text.length() > 60) {
            text = text.substring(text.length() - 60);
        }
        r.drawText(g.font, "> " + text + "_", 14, h - 194, 1.0f, WHITE);
    }

    // ---------- Pauza ----------

    private static void drawPause(Game g, GuiRenderer r, int w, int h) {
        r.fillRect(0, 0, w, h, 0xAA000000);
        float bw = 320;
        float bx = (w - bw) / 2f;
        float by = h * 0.32f;
        r.drawCenteredText(g.font, "Pauza", w / 2f, by - 60, 3.0f, WHITE);
        button(g, r, bx, by, bw, 44, "Wroc do gry");
        button(g, r, bx, by + 54, bw, 44, "Ustawienia");
        button(g, r, bx, by + 108, bw, 44, "Zapisz i wyjdz do menu");
        r.drawCenteredText(g.font, "Swiat zapisuje sie sam co 30 sekund.", w / 2f, by + 170, 1.0f, GRAY);
    }

    private static void clickPause(Game g, float x, float y) {
        float bw = 320;
        float bx = (g.windowW - bw) / 2f;
        float by = g.windowH * 0.32f;
        if (inside(x, y, bx, by, bw, 44)) {
            g.uiResume();
        } else if (inside(x, y, bx, by + 54, bw, 44)) {
            g.uiOpenSettings(Game.Screen.PAUSE);
        } else if (inside(x, y, bx, by + 108, bw, 44)) {
            g.uiSaveAndQuitToMenu();
        }
    }

    // ---------- Ekwipunek (survival) ----------

    private static float invPanelX(int w) {
        return (w - 500) / 2f;
    }

    private static float invPanelY(int h) {
        return (h - 480) / 2f;
    }

    /** Pozycja slotu ekwipunku (0-35): 0-8 hotbar na dole, 9-35 plecak 9x3. */
    private static float[] playerSlotRect(Game g, int index) {
        float px = invPanelX(g.windowW);
        float py = invPanelY(g.windowH);
        if (index < 9) {
            return new float[]{px + 28 + index * 48, py + 424, 44};
        }
        int i = index - 9;
        return new float[]{px + 28 + (i % 9) * 48, py + 250 + (i / 9) * 48, 44};
    }

    /** Pozycja slotu craftingu 2x2 w ekwipunku. */
    private static float[] invCraftRect(Game g, int index) {
        float px = invPanelX(g.windowW);
        float py = invPanelY(g.windowH);
        return new float[]{px + 60 + (index % 2) * 48, py + 60 + (index / 2) * 48, 44};
    }

    private static float[] invResultRect(Game g) {
        float px = invPanelX(g.windowW);
        float py = invPanelY(g.windowH);
        return new float[]{px + 300, py + 84, 44};
    }

    private static void drawPlayerSlots(Game g, GuiRenderer r) {
        String tip = null;
        for (int i = 0; i < Inventory.SIZE; i++) {
            float[] rc = playerSlotRect(g, i);
            slot(g, r, rc[0], rc[1], rc[2], g.inventory.get(i));
            String name = hoveredStackName(g, rc[0], rc[1], rc[2], g.inventory.get(i));
            if (name != null) {
                tip = name;
            }
        }
        if (tip != null) {
            tooltip(g, r, tip);
        }
    }

    private static void drawInventory(Game g, GuiRenderer r, int w, int h) {
        r.fillRect(0, 0, w, h, 0x88000000);
        float px = invPanelX(w);
        float py = invPanelY(h);
        panel(g, r, px, py, 500, 480);
        r.drawText(g.font, "Ekwipunek", px + 24, py + 14, 1.5f, WHITE);
        r.drawText(g.font, "Wytwarzanie", px + 60, py + 40, 1.0f, GRAY);
        String tip = null;
        for (int i = 0; i < 4; i++) {
            float[] rc = invCraftRect(g, i);
            slot(g, r, rc[0], rc[1], rc[2], g.invCraft[i]);
            String name = hoveredStackName(g, rc[0], rc[1], rc[2], g.invCraft[i]);
            if (name != null) {
                tip = name;
            }
        }
        float[] rr = invResultRect(g);
        r.drawText(g.font, "->", rr[0] - 56, rr[1] + 12, 1.5f, GRAY);
        slot(g, r, rr[0], rr[1], rr[2], g.craftResultInv);
        String name = hoveredStackName(g, rr[0], rr[1], rr[2], g.craftResultInv);
        if (name != null) {
            tip = name;
        }
        r.drawText(g.font, "LPM - podnies/poloz, PPM - poloz 1 / wez polowe", px + 24, py + 214, 1.0f, GRAY);
        drawPlayerSlots(g, r);
        if (tip != null) {
            tooltip(g, r, tip);
        }
    }

    private static void clickInventory(Game g, float x, float y, boolean right) {
        for (int i = 0; i < 4; i++) {
            float[] rc = invCraftRect(g, i);
            if (inside(x, y, rc[0], rc[1], rc[2], rc[2])) {
                g.uiClickInvCraft(i, right);
                return;
            }
        }
        float[] rr = invResultRect(g);
        if (inside(x, y, rr[0], rr[1], rr[2], rr[2])) {
            g.uiClickCraftResult(false);
            return;
        }
        for (int i = 0; i < Inventory.SIZE; i++) {
            float[] rc = playerSlotRect(g, i);
            if (inside(x, y, rc[0], rc[1], rc[2], rc[2])) {
                g.uiClickPlayerSlot(i, right);
                return;
            }
        }
    }

    // ---------- Creative ----------

    private static final int CREATIVE_COLS = 9;
    private static final int CREATIVE_ROWS = 5;

    private static float creativeX(int w) {
        return (w - 500) / 2f;
    }

    private static float creativeY(int h) {
        return (h - 560) / 2f;
    }

    private static void drawCreative(Game g, GuiRenderer r, int w, int h) {
        r.fillRect(0, 0, w, h, 0x88000000);
        float px = creativeX(w);
        float py = creativeY(h);
        panel(g, r, px, py, 500, 560);
        r.drawText(g.font, "Creative - kliknij, aby wziasc (LPM x64, PPM x1)", px + 24, py + 14, 1.0f, WHITE);

        List<Item> list = g.creativeList;
        int start = g.creativeScroll * CREATIVE_COLS;
        String tip = null;
        for (int row = 0; row < CREATIVE_ROWS; row++) {
            for (int col = 0; col < CREATIVE_COLS; col++) {
                int idx = start + row * CREATIVE_COLS + col;
                float sx = px + 28 + col * 48;
                float sy = py + 44 + row * 48;
                ItemStack stack = null;
                if (idx < list.size()) {
                    stack = new ItemStack(list.get(idx).id, 64);
                }
                slot(g, r, sx, sy, 44, stack);
                String name = hoveredStackName(g, sx, sy, 44, stack);
                if (name != null) {
                    tip = name;
                }
            }
        }
        // Pasek przewijania.
        int totalRows = (list.size() + CREATIVE_COLS - 1) / CREATIVE_COLS;
        int maxScroll = Math.max(0, totalRows - CREATIVE_ROWS);
        float barX = px + 466;
        float barY = py + 44;
        float barH = CREATIVE_ROWS * 48 - 4;
        r.fillRect(barX, barY, 10, barH, BLACK);
        float knobH = Math.max(20, barH * CREATIVE_ROWS / Math.max(1, totalRows));
        float knobY = barY;
        if (maxScroll > 0) {
            knobY = barY + (barH - knobH) * g.creativeScroll / maxScroll;
        }
        r.fillRect(barX, knobY, 10, knobH, SLOT_EDGE);

        // Hotbar gracza (podglad) + czyszczenie.
        r.drawText(g.font, "Twoj ekwipunek (PPM na slot = wyczysc):", px + 24, py + 300, 1.0f, GRAY);
        for (int i = 0; i < Inventory.HOTBAR; i++) {
            float sx = px + 28 + i * 48;
            float sy = py + 324;
            slot(g, r, sx, sy, 44, g.inventory.get(i));
            String name = hoveredStackName(g, sx, sy, 44, g.inventory.get(i));
            if (name != null) {
                tip = name;
            }
        }
        // Plecak (podglad, tez czyszczalny).
        for (int i = 9; i < Inventory.SIZE; i++) {
            int k = i - 9;
            float sx = px + 28 + (k % 9) * 48;
            float sy = py + 376 + (k / 9) * 48;
            // Plecak w mniejszych slotach, zeby sie zmiescil.
            float small = 40;
            sx = px + 28 + (k % 9) * 48 + 2;
            sy = py + 376 + (k / 9) * 44 + 2;
            slot(g, r, sx, sy, small, g.inventory.get(i));
            String name = hoveredStackName(g, sx, sy, small, g.inventory.get(i));
            if (name != null) {
                tip = name;
            }
        }
        if (tip != null) {
            tooltip(g, r, tip);
        }
    }

    private static void clickCreative(Game g, float x, float y, boolean right) {
        float px = creativeX(g.windowW);
        float py = creativeY(g.windowH);
        int start = g.creativeScroll * CREATIVE_COLS;
        for (int row = 0; row < CREATIVE_ROWS; row++) {
            for (int col = 0; col < CREATIVE_COLS; col++) {
                int idx = start + row * CREATIVE_COLS + col;
                float sx = px + 28 + col * 48;
                float sy = py + 44 + row * 48;
                if (inside(x, y, sx, sy, 44, 44) && idx < g.creativeList.size()) {
                    g.uiClickCreative(idx, right);
                    return;
                }
            }
        }
        // Hotbar: PPM czysci slot.
        if (right) {
            for (int i = 0; i < Inventory.HOTBAR; i++) {
                float sx = px + 28 + i * 48;
                float sy = py + 324;
                if (inside(x, y, sx, sy, 44, 44)) {
                    g.inventory.set(i, null);
                    return;
                }
            }
            for (int i = 9; i < Inventory.SIZE; i++) {
                int k = i - 9;
                float sx = px + 28 + (k % 9) * 48 + 2;
                float sy = py + 376 + (k / 9) * 44 + 2;
                if (inside(x, y, sx, sy, 40, 40)) {
                    g.inventory.set(i, null);
                    return;
                }
            }
        }
    }

    // ---------- Stol rzemieslniczy ----------

    private static float tablePanelX(int w) {
        return (w - 700) / 2f;
    }

    private static float tablePanelY(int h) {
        return (h - 500) / 2f;
    }

    private static float[] tableCraftRect(Game g, int index) {
        float px = tablePanelX(g.windowW);
        float py = tablePanelY(g.windowH);
        return new float[]{px + 40 + (index % 3) * 48, py + 60 + (index / 3) * 48, 44};
    }

    private static float[] tableResultRect(Game g) {
        float px = tablePanelX(g.windowW);
        float py = tablePanelY(g.windowH);
        return new float[]{px + 260, py + 108, 44};
    }

    /** Sloty ekwipunku w oknie stolu (przesuniete w dol). */
    private static float[] tablePlayerSlotRect(Game g, int index) {
        float px = tablePanelX(g.windowW);
        float py = tablePanelY(g.windowH);
        if (index < 9) {
            return new float[]{px + 40 + index * 48, py + 448, 44};
        }
        int i = index - 9;
        return new float[]{px + 40 + (i % 9) * 48, py + 274 + (i / 9) * 48, 44};
    }

    private static void drawCrafting(Game g, GuiRenderer r, int w, int h) {
        r.fillRect(0, 0, w, h, 0x88000000);
        float px = tablePanelX(w);
        float py = tablePanelY(h);
        panel(g, r, px, py, 700, 500);
        r.drawText(g.font, "Stol rzemieslniczy", px + 24, py + 12, 1.5f, WHITE);
        String tip = null;
        for (int i = 0; i < 9; i++) {
            float[] rc = tableCraftRect(g, i);
            slot(g, r, rc[0], rc[1], rc[2], g.tableCraft[i]);
            String name = hoveredStackName(g, rc[0], rc[1], rc[2], g.tableCraft[i]);
            if (name != null) {
                tip = name;
            }
        }
        float[] rr = tableResultRect(g);
        r.drawText(g.font, "->", rr[0] - 52, rr[1] + 12, 1.5f, GRAY);
        slot(g, r, rr[0], rr[1], rr[2], g.craftResultTable);
        String name = hoveredStackName(g, rr[0], rr[1], rr[2], g.craftResultTable);
        if (name != null) {
            tip = name;
        }
        button(g, r, px + 40, py + 212, 200, 32, g.recipeBookOpen ? "Ksiazka [X]" : "Ksiazka [?]");

        // Ksiazka receptur.
        if (g.recipeBookOpen) {
            float bx = px + 360;
            r.drawText(g.font, "Receptury (klik = uloz):", bx, py + 40, 1.0f, YELLOW);
            List<Recipes.Recipe> recipes = Recipes.all();
            for (int i = 0; i < recipes.size(); i++) {
                Recipes.Recipe rec = recipes.get(i);
                float ry = py + 62 + i * 26;
                boolean hover = inside(g.mouseX, g.mouseY, bx, ry, 300, 24);
                r.fillRect(bx, ry, 300, 24, hover ? 0xFF4A4A6A : 0xFF2A2A2A);
                ItemStack out = new ItemStack(rec.resultId, rec.resultCount);
                drawStackIcon(g, r, out, bx + 4, ry + 2, 20);
                r.drawText(g.font, rec.name + (rec.resultCount > 1 ? " x" + rec.resultCount : ""),
                        bx + 30, ry + 4, 1.0f, WHITE);
            }
        } else {
            r.drawText(g.font, "Otworz ksiazke, aby", px + 360, py + 100, 1.0f, GRAY);
            r.drawText(g.font, "zobaczyc receptury.", px + 360, py + 120, 1.0f, GRAY);
        }

        for (int i = 0; i < Inventory.SIZE; i++) {
            float[] rc = tablePlayerSlotRect(g, i);
            slot(g, r, rc[0], rc[1], rc[2], g.inventory.get(i));
            String n = hoveredStackName(g, rc[0], rc[1], rc[2], g.inventory.get(i));
            if (n != null) {
                tip = n;
            }
        }
        if (tip != null) {
            tooltip(g, r, tip);
        }
    }

    private static void clickCrafting(Game g, float x, float y, boolean right) {
        float px = tablePanelX(g.windowW);
        float py = tablePanelY(g.windowH);
        if (inside(x, y, px + 40, py + 212, 200, 32)) {
            g.uiToggleRecipeBook();
            return;
        }
        if (g.recipeBookOpen) {
            float bx = px + 360;
            List<Recipes.Recipe> recipes = Recipes.all();
            for (int i = 0; i < recipes.size(); i++) {
                float ry = py + 62 + i * 26;
                if (inside(x, y, bx, ry, 300, 24)) {
                    g.uiClickRecipe(i);
                    return;
                }
            }
        }
        for (int i = 0; i < 9; i++) {
            float[] rc = tableCraftRect(g, i);
            if (inside(x, y, rc[0], rc[1], rc[2], rc[2])) {
                g.uiClickTableCraft(i, right);
                return;
            }
        }
        float[] rr = tableResultRect(g);
        if (inside(x, y, rr[0], rr[1], rr[2], rr[2])) {
            g.uiClickCraftResult(true);
            return;
        }
        for (int i = 0; i < Inventory.SIZE; i++) {
            float[] rc = tablePlayerSlotRect(g, i);
            if (inside(x, y, rc[0], rc[1], rc[2], rc[2])) {
                g.uiClickPlayerSlot(i, right);
                return;
            }
        }
    }

    // ---------- Panel admina ----------

    private static final String[] ADMIN_BTNS = {
            "Dzien", "Noc", "Tryb: Survival", "Tryb: Creative",
            "Latanie WL/WYL", "Teleport: spawn", "Daj starter", "Zabij (respawn)"
    };

    private static void drawAdmin(Game g, GuiRenderer r, int w, int h) {
        r.fillRect(0, 0, w, h, 0x88000000);
        float pw = 520;
        float ph = 470;
        float px = (w - pw) / 2f;
        float py = (h - ph) / 2f;
        panel(g, r, px, py, pw, ph);
        r.drawCenteredText(g.font, "Panel admina", w / 2f, py + 16, 2.0f, YELLOW);
        r.drawText(g.font, "Seed: " + g.seed, px + 30, py + 60, 1.0f, GRAY);
        if (g.player != null) {
            r.drawText(g.font, String.format("XYZ: %.1f / %.1f / %.1f",
                    g.player.getPosition().x, g.player.getPosition().y, g.player.getPosition().z),
                    px + 30, py + 82, 1.0f, GRAY);
        }
        r.drawText(g.font, "Tryb: " + (g.gamemode == 1 ? "Creative" : "Survival")
                + ", pora dnia: " + Game.timeName(g.time), px + 30, py + 104, 1.0f, GRAY);
        for (int i = 0; i < ADMIN_BTNS.length; i++) {
            float bx = px + 30 + (i % 2) * 235;
            float by = py + 136 + (i / 2) * 50;
            String label = ADMIN_BTNS[i];
            if (i == 2) {
                label = "Tryb: Survival" + (g.gamemode == 0 ? " [X]" : "");
            }
            if (i == 3) {
                label = "Tryb: Creative" + (g.gamemode == 1 ? " [X]" : "");
            }
            button(g, r, bx, by, 220, 40, label);
        }
        r.drawText(g.font, "Wiecej w chacie (T): /help", px + 30, py + 344, 1.0f, GRAY);
        button(g, r, px + 30, py + ph - 66, pw - 60, 40, "Zamknij [G / Esc]");
    }

    private static void clickAdmin(Game g, float x, float y) {
        float pw = 520;
        float ph = 470;
        float px = (g.windowW - pw) / 2f;
        float py = (g.windowH - ph) / 2f;
        for (int i = 0; i < ADMIN_BTNS.length; i++) {
            float bx = px + 30 + (i % 2) * 235;
            float by = py + 136 + (i / 2) * 50;
            if (inside(x, y, bx, by, 220, 40)) {
                g.uiAdmin(i);
                return;
            }
        }
        if (inside(x, y, px + 30, py + ph - 66, pw - 60, 40)) {
            g.uiResume();
        }
    }

    // ---------- Stos na kursorze ----------

    private static void drawCursorStack(Game g, GuiRenderer r) {
        if (g.cursor == null || g.cursor.isEmpty()) {
            return;
        }
        float s = 36;
        float x = g.mouseX - s / 2f;
        float y = g.mouseY - s / 2f;
        r.fillRect(x, y, s, s, 0xAA1E1E1E);
        drawStackIcon(g, r, g.cursor, x + 2, y + 2, s - 4);
        if (g.cursor.count > 1) {
            String n = Integer.toString(g.cursor.count);
            float tw = r.textWidth(g.font, n, 1.0f);
            r.drawTextShadow(g.font, n, x + s - tw - 3, y + s - 17, 1.0f, WHITE);
        }
    }
}
