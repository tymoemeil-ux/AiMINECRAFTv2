package com.aiminecraft.player;

import com.aiminecraft.world.World;
import org.joml.Vector3f;

/**
 * Raycast po voxelach (DDA, Amanatides &amp; Woo).
 * Znajduje blok, na ktory patrzy gracz.
 */
public final class BlockRaycast {

    private BlockRaycast() {
    }

    /** Trafienie: wybrany blok + normalna sciany (kierunek do pustego voxela obok). */
    public static final class Hit {
        public final int x;
        public final int y;
        public final int z;
        public final int nx;
        public final int ny;
        public final int nz;

        public Hit(int x, int y, int z, int nx, int ny, int nz) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.nx = nx;
            this.ny = ny;
            this.nz = nz;
        }
    }

    /** Zwraca trafiony blok albo null, jesli nic w zasiegu. */
    public static Hit raycast(World world, Vector3f origin, Vector3f dir, double maxDistance) {
        int x = (int) Math.floor(origin.x);
        int y = (int) Math.floor(origin.y);
        int z = (int) Math.floor(origin.z);

        double dx = dir.x;
        double dy = dir.y;
        double dz = dir.z;
        int stepX = dx > 0 ? 1 : -1;
        int stepY = dy > 0 ? 1 : -1;
        int stepZ = dz > 0 ? 1 : -1;

        double tDeltaX = dx != 0 ? Math.abs(1.0 / dx) : Double.POSITIVE_INFINITY;
        double tDeltaY = dy != 0 ? Math.abs(1.0 / dy) : Double.POSITIVE_INFINITY;
        double tDeltaZ = dz != 0 ? Math.abs(1.0 / dz) : Double.POSITIVE_INFINITY;

        double distX = stepX > 0 ? (x + 1 - origin.x) : (origin.x - x);
        double distY = stepY > 0 ? (y + 1 - origin.y) : (origin.y - y);
        double distZ = stepZ > 0 ? (z + 1 - origin.z) : (origin.z - z);

        double tMaxX = tDeltaX == Double.POSITIVE_INFINITY ? Double.POSITIVE_INFINITY : tDeltaX * distX;
        double tMaxY = tDeltaY == Double.POSITIVE_INFINITY ? Double.POSITIVE_INFINITY : tDeltaY * distY;
        double tMaxZ = tDeltaZ == Double.POSITIVE_INFINITY ? Double.POSITIVE_INFINITY : tDeltaZ * distZ;

        int nx = 0;
        int ny = 0;
        int nz = 0;
        double t = 0;
        for (int i = 0; i < 256; i++) {
            if (tMaxX < tMaxY && tMaxX < tMaxZ) {
                x += stepX;
                t = tMaxX;
                tMaxX += tDeltaX;
                nx = -stepX;
                ny = 0;
                nz = 0;
            } else if (tMaxY < tMaxZ) {
                y += stepY;
                t = tMaxY;
                tMaxY += tDeltaY;
                nx = 0;
                ny = -stepY;
                nz = 0;
            } else {
                z += stepZ;
                t = tMaxZ;
                tMaxZ += tDeltaZ;
                nx = 0;
                ny = 0;
                nz = -stepZ;
            }
            if (t > maxDistance) {
                return null;
            }
            if (world.isSolid(x, y, z)) {
                return new Hit(x, y, z, nx, ny, nz);
            }
        }
        return null;
    }
}
