package com.aiminecraft.player;

import com.aiminecraft.render.Window;
import com.aiminecraft.world.World;
import org.joml.Vector3f;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Gracz z pierwszej osoby: chodzenie, skakanie, latanie
 * i kolizje ze swiatem (AABB, os po osi).
 */
public class Player {

    public static final float HALF_WIDTH = 0.3f;
    public static final float HEIGHT = 1.8f;
    public static final float EYE_HEIGHT = 1.62f;

    private static final float WALK_SPEED = 4.3f;
    private static final float SPRINT_SPEED = 6.8f;
    private static final float FLY_SPEED = 10.0f;
    private static final float FLY_SPRINT_SPEED = 20.0f;
    private static final float FLY_VERTICAL_SPEED = 8.0f;
    private static final float GRAVITY = 28.0f;
    private static final float JUMP_VELOCITY = 8.8f;
    private static final float TERMINAL_VELOCITY = -50.0f;
    private static final float MOUSE_SENSITIVITY = 0.0026f;
    private static final float EPS = 1e-4f;

    private final Vector3f position = new Vector3f(0.5f, 40f, 0.5f); // stopy
    private final Vector3f velocity = new Vector3f();
    private final Vector3f eye = new Vector3f();
    private final Vector3f direction = new Vector3f(0, 0, -1);

    private float yaw = 0f;
    private float pitch = -0.08f; // lekko w dol, zeby od startu bylo widac teren
    private boolean onGround = false;
    private boolean flying = false;

    private double lastMouseX = 0;
    private double lastMouseY = 0;
    private boolean mouseInitialized = false;

    public void setPosition(float x, float y, float z) {
        position.set(x, y, z);
        velocity.set(0, 0, 0);
    }

    public Vector3f getPosition() {
        return position;
    }

    public boolean isFlying() {
        return flying;
    }

    public boolean isOnGround() {
        return onGround;
    }

    public void setFlying(boolean flying) {
        this.flying = flying;
        velocity.y = 0;
    }

    /** Pozycja oczu (do kamery i raycastu). */
    public Vector3f getEyePosition() {
        return eye.set(position.x, position.y + EYE_HEIGHT, position.z);
    }

    /** Znormalizowany kierunek patrzenia. */
    public Vector3f getDirection() {
        float cosPitch = (float) Math.cos(pitch);
        return direction.set(
                (float) (-Math.sin(yaw) * cosPitch),
                (float) Math.sin(pitch),
                (float) (-Math.cos(yaw) * cosPitch));
    }

    public void update(double deltaTime, Window window, World world) {
        float dt = (float) Math.min(deltaTime, 0.05);

        updateLook(window);

        // Ruch poziomy wzgledem kursora (yaw).
        float sin = (float) Math.sin(yaw);
        float cos = (float) Math.cos(yaw);
        float forwardX = -sin;
        float forwardZ = -cos;
        float rightX = cos;
        float rightZ = -sin;

        float wishX = 0;
        float wishZ = 0;
        if (window.isKeyPressed(GLFW_KEY_W)) {
            wishX += forwardX;
            wishZ += forwardZ;
        }
        if (window.isKeyPressed(GLFW_KEY_S)) {
            wishX -= forwardX;
            wishZ -= forwardZ;
        }
        if (window.isKeyPressed(GLFW_KEY_D)) {
            wishX += rightX;
            wishZ += rightZ;
        }
        if (window.isKeyPressed(GLFW_KEY_A)) {
            wishX -= rightX;
            wishZ -= rightZ;
        }
        float wishLen = (float) Math.sqrt(wishX * wishX + wishZ * wishZ);
        if (wishLen > 0.0001f) {
            wishX /= wishLen;
            wishZ /= wishLen;
        }

        boolean sprintKey = window.isKeyPressed(GLFW_KEY_LEFT_CONTROL)
                || (!flying && window.isKeyPressed(GLFW_KEY_LEFT_SHIFT));
        float speed = flying
                ? (sprintKey ? FLY_SPRINT_SPEED : FLY_SPEED)
                : (sprintKey ? SPRINT_SPEED : WALK_SPEED);
        velocity.x = wishX * speed;
        velocity.z = wishZ * speed;

        // Pion: latanie albo grawitacja + skok.
        if (flying) {
            float up = 0;
            if (window.isKeyPressed(GLFW_KEY_SPACE)) {
                up += 1;
            }
            if (window.isKeyPressed(GLFW_KEY_LEFT_SHIFT)) {
                up -= 1;
            }
            velocity.y = up * (sprintKey ? FLY_VERTICAL_SPEED * 1.6f : FLY_VERTICAL_SPEED);
        } else {
            velocity.y -= GRAVITY * dt;
            if (velocity.y < TERMINAL_VELOCITY) {
                velocity.y = TERMINAL_VELOCITY;
            }
            if (window.isKeyPressed(GLFW_KEY_SPACE) && onGround) {
                velocity.y = JUMP_VELOCITY;
                onGround = false;
            }
        }

        // Integracja z kolizjami, os po osi.
        moveAxis(world, velocity.x * dt, 0);
        moveAxis(world, velocity.z * dt, 2);
        if (!flying) {
            onGround = false;
            moveAxis(world, velocity.y * dt, 1);
        } else {
            moveAxis(world, velocity.y * dt, 1);
        }
    }

    private void updateLook(Window window) {
        if (!window.isCursorCaptured()) {
            mouseInitialized = false;
            return;
        }
        double mx = window.getMouseX();
        double my = window.getMouseY();
        if (!mouseInitialized) {
            lastMouseX = mx;
            lastMouseY = my;
            mouseInitialized = true;
            return;
        }
        float dx = (float) (mx - lastMouseX);
        float dy = (float) (my - lastMouseY);
        lastMouseX = mx;
        lastMouseY = my;
        yaw -= dx * MOUSE_SENSITIVITY;
        pitch -= dy * MOUSE_SENSITIVITY;
        if (pitch > 1.55f) {
            pitch = 1.55f;
        }
        if (pitch < -1.55f) {
            pitch = -1.55f;
        }
    }

    /** Przesuwa gracza wzdluz jednej osi i wysuwa go ze stalych blokow. */
    private void moveAxis(World world, float delta, int axis) {
        if (delta == 0f) {
            return;
        }
        if (axis == 0) {
            position.x += delta;
        } else if (axis == 1) {
            position.y += delta;
        } else {
            position.z += delta;
        }

        float minX = position.x - HALF_WIDTH;
        float maxX = position.x + HALF_WIDTH;
        float minY = position.y;
        float maxY = position.y + HEIGHT;
        float minZ = position.z - HALF_WIDTH;
        float maxZ = position.z + HALF_WIDTH;

        int x0 = (int) Math.floor(minX);
        int x1 = (int) Math.floor(maxX);
        int y0 = (int) Math.floor(minY);
        int y1 = (int) Math.floor(maxY);
        int z0 = (int) Math.floor(minZ);
        int z1 = (int) Math.floor(maxZ);

        for (int bx = x0; bx <= x1; bx++) {
            for (int by = y0; by <= y1; by++) {
                for (int bz = z0; bz <= z1; bz++) {
                    if (!world.isSolid(bx, by, bz)) {
                        continue;
                    }
                    if (axis == 0) {
                        if (delta > 0) {
                            position.x = Math.min(position.x, bx - HALF_WIDTH - EPS);
                        } else {
                            position.x = Math.max(position.x, bx + 1 + HALF_WIDTH + EPS);
                        }
                        velocity.x = 0;
                    } else if (axis == 2) {
                        if (delta > 0) {
                            position.z = Math.min(position.z, bz - HALF_WIDTH - EPS);
                        } else {
                            position.z = Math.max(position.z, bz + 1 + HALF_WIDTH + EPS);
                        }
                        velocity.z = 0;
                    } else {
                        if (delta > 0) {
                            position.y = Math.min(position.y, by - HEIGHT - EPS);
                        } else {
                            position.y = Math.max(position.y, by + 1 + EPS);
                            onGround = true;
                        }
                        velocity.y = 0;
                    }
                }
            }
        }
    }
}
