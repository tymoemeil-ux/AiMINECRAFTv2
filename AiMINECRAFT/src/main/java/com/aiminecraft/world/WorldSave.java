package com.aiminecraft.world;

import com.aiminecraft.item.Inventory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Zapis swiata: jeden plik ze seedem, pozycja gracza, ekwipunkiem,
 * czasem dnia i wszystkimi chunkami zmienionymi przez gracza.
 */
public class WorldSave {

    public static final String DIR = "worlds";
    public static final String FILE = DIR + File.separator + "swiat.dat";

    private static final int MAGIC = 0x41494D43; // "AIMC"
    private static final int VERSION = 2;

    /** Dane gracza do zapisu/odczytu. */
    public static final class PlayerData {
        public float x;
        public float y;
        public float z;
        public float yaw;
        public float pitch;
        public boolean flying;
        public int gamemode; // 0 = survival, 1 = creative
        public float time;   // pora dnia 0..1
        public final int[] invIds = new int[Inventory.SIZE];
        public final int[] invCounts = new int[Inventory.SIZE];
        public int selected;
        /** Stos niesiony na kursorze + siatki craftingu (13 slotow: 4 + 9). */
        public int cursorId;
        public int cursorCount;
        public final int[] craftIds = new int[13];
        public final int[] craftCounts = new int[13];
    }

    private final long seed;
    private final Map<Long, byte[]> savedChunks = new HashMap<>();
    private PlayerData loadedPlayer;

    public WorldSave(long seed) {
        this.seed = seed;
    }

    public static boolean exists() {
        return new File(FILE).isFile();
    }

    public static void delete() {
        File f = new File(FILE);
        if (f.isFile() && !f.delete()) {
            System.out.println("Nie moge usunac starego zapisu!");
        }
    }

    /** Odczytuje seed z pliku (bez wczytywania calosci). Null, jesli brak/zly plik. */
    public static Long peekSeed() {
        File f = new File(FILE);
        if (!f.isFile()) {
            return null;
        }
        try (DataInputStream in = new DataInputStream(new FileInputStream(f))) {
            if (in.readInt() != MAGIC || in.readInt() != VERSION) {
                return null;
            }
            return in.readLong();
        } catch (IOException e) {
            return null;
        }
    }

    /** Krotki opis zapisu do menu (seed, tryb, rozmiar). */
    public static String peekInfo() {
        File f = new File(FILE);
        if (!f.isFile()) {
            return "";
        }
        try (DataInputStream in = new DataInputStream(new FileInputStream(f))) {
            if (in.readInt() != MAGIC || in.readInt() != VERSION) {
                return "uszkodzony zapis";
            }
            long fileSeed = in.readLong();
            in.readFloat(); // czas
            int mode = in.readByte();
            long kb = Math.max(1, f.length() / 1024);
            return "Seed: " + fileSeed + ", tryb: " + (mode == 1 ? "Creative" : "Survival")
                    + ", rozmiar: " + kb + " KB";
        } catch (IOException e) {
            return "uszkodzony zapis";
        }
    }

    /**
     * Wczytuje plik (jesli seed sie zgadza).
     * Zwraca dane gracza albo null (nowy swiat).
     */
    public PlayerData load() {
        loadedPlayer = null;
        savedChunks.clear();
        File f = new File(FILE);
        if (!f.isFile()) {
            return null;
        }
        try (DataInputStream in = new DataInputStream(new FileInputStream(f))) {
            if (in.readInt() != MAGIC || in.readInt() != VERSION) {
                System.out.println("Zapis ma zly format - zaczynam nowy swiat.");
                return null;
            }
            long fileSeed = in.readLong();
            if (fileSeed != seed) {
                System.out.println("Zapis jest z innego seeda - zaczynam nowy swiat.");
                return null;
            }
            PlayerData p = new PlayerData();
            p.time = in.readFloat();
            p.gamemode = in.readByte();
            p.x = in.readFloat();
            p.y = in.readFloat();
            p.z = in.readFloat();
            p.yaw = in.readFloat();
            p.pitch = in.readFloat();
            p.flying = in.readByte() != 0;
            p.selected = in.readInt();
            for (int i = 0; i < Inventory.SIZE; i++) {
                p.invIds[i] = in.readInt();
                p.invCounts[i] = in.readInt();
            }
            p.cursorId = in.readInt();
            p.cursorCount = in.readInt();
            for (int i = 0; i < 13; i++) {
                p.craftIds[i] = in.readInt();
                p.craftCounts[i] = in.readInt();
            }
            int chunkCount = in.readInt();
            for (int i = 0; i < chunkCount; i++) {
                int cx = in.readInt();
                int cz = in.readInt();
                byte[] data = new byte[Chunk.SIZE * Chunk.HEIGHT * Chunk.SIZE];
                in.readFully(data);
                savedChunks.put(World.key(cx, cz), data);
            }
            loadedPlayer = p;
            System.out.println("Wczytano zapis: " + chunkCount + " chunkow.");
            return p;
        } catch (IOException e) {
            System.out.println("Nie moge wczytac zapisu: " + e.getMessage());
            savedChunks.clear();
            return null;
        }
    }

    /** Wpisuje zapisane dane do swiezo utworzonego chunka. Zwraca false, jesli brak. */
    public boolean loadInto(Chunk chunk, int cx, int cz) {
        byte[] data = savedChunks.get(World.key(cx, cz));
        if (data == null) {
            return false;
        }
        chunk.pasteBlocks(data);
        return true;
    }

    /** Kopiuje chunk do pamieci (trafia do pliku przy flush). */
    public void saveChunk(Chunk chunk) {
        savedChunks.put(World.key(chunk.getCx(), chunk.getCz()), chunk.copyBlocks());
    }

    /** Zapisuje wszystko do pliku. */
    public void flush(PlayerData player) {
        File dir = new File(DIR);
        if (!dir.isDirectory() && !dir.mkdirs()) {
            System.out.println("Nie moge utworzyc katalogu " + DIR);
            return;
        }
        try (DataOutputStream out = new DataOutputStream(new FileOutputStream(FILE))) {
            out.writeInt(MAGIC);
            out.writeInt(VERSION);
            out.writeLong(seed);
            out.writeFloat(player.time);
            out.writeByte(player.gamemode);
            out.writeFloat(player.x);
            out.writeFloat(player.y);
            out.writeFloat(player.z);
            out.writeFloat(player.yaw);
            out.writeFloat(player.pitch);
            out.writeByte(player.flying ? 1 : 0);
            out.writeInt(player.selected);
            for (int i = 0; i < Inventory.SIZE; i++) {
                out.writeInt(player.invIds[i]);
                out.writeInt(player.invCounts[i]);
            }
            out.writeInt(player.cursorId);
            out.writeInt(player.cursorCount);
            for (int i = 0; i < 13; i++) {
                out.writeInt(player.craftIds[i]);
                out.writeInt(player.craftCounts[i]);
            }
            out.writeInt(savedChunks.size());
            for (Map.Entry<Long, byte[]> e : savedChunks.entrySet()) {
                long k = e.getKey();
                out.writeInt((int) (k >> 32));
                out.writeInt((int) (long) k);
                out.write(e.getValue());
            }
        } catch (IOException e) {
            System.out.println("Nie moge zapisac swiata: " + e.getMessage());
        }
    }

    public PlayerData getLoadedPlayer() {
        return loadedPlayer;
    }
}
