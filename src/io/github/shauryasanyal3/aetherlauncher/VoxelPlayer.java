package io.github.shauryasanyal3.aetherlauncher;

public class VoxelPlayer {
    public float x = 8.0f;
    public float y = 28.0f;
    public float z = 8.0f;

    public float yaw = 0f;   // degrees
    public float pitch = 0f; // degrees

    public float vx = 0f;
    public float vy = 0f;
    public float vz = 0f;

    public boolean isOnGround = false;
    public byte selectedBlock = VoxelBlock.COBBLESTONE;

    public static final float GRAVITY = -22.0f;
    public static final float JUMP_SPEED = 9.0f;
    public static final float MOVE_SPEED = 6.5f;
    public static final float PLAYER_RADIUS = 0.3f;
    public static final float PLAYER_HEIGHT = 1.8f;

    public void jump() {
        if (isOnGround) {
            vy = JUMP_SPEED;
            isOnGround = false;
        }
    }

    public void update(float dt, float forward, float strafe, VoxelWorld world) {
        if (dt > 0.05f) dt = 0.05f; // Clamp delta time

        // Calculate horizontal direction vectors
        float radYaw = (float) Math.toRadians(yaw);
        float sinY = (float) Math.sin(radYaw);
        float cosY = (float) Math.cos(radYaw);

        float dx = (-sinY * forward + cosY * strafe) * MOVE_SPEED;
        float dz = (cosY * forward + sinY * strafe) * MOVE_SPEED;

        // Apply proposed horizontal moves
        float nextX = x + dx * dt;
        if (!hasCollision(nextX, y, z, world)) {
            x = nextX;
        }

        float nextZ = z + dz * dt;
        if (!hasCollision(x, y, nextZ, world)) {
            z = nextZ;
        }

        // Apply vertical gravity
        vy += GRAVITY * dt;
        float nextY = y + vy * dt;

        if (hasCollision(x, nextY, z, world)) {
            if (vy < 0) {
                // Falling down: land on top of block
                isOnGround = true;
                y = (float) Math.floor(nextY) + 1.0f;
            } else {
                // Hitting head on ceiling
                y = (float) Math.floor(nextY + PLAYER_HEIGHT) - PLAYER_HEIGHT;
            }
            vy = 0f;
        } else {
            y = nextY;
            isOnGround = false;
        }

        // Prevent falling into void
        if (y < 2.0f) {
            y = 30.0f;
            x = 8.0f;
            z = 8.0f;
            vy = 0f;
        }
    }

    public boolean hasCollision(float px, float py, float pz, VoxelWorld world) {
        int minX = (int) Math.floor(px - PLAYER_RADIUS);
        int maxX = (int) Math.floor(px + PLAYER_RADIUS);
        int minY = (int) Math.floor(py);
        int maxY = (int) Math.floor(py + PLAYER_HEIGHT - 0.05f);
        int minZ = (int) Math.floor(pz - PLAYER_RADIUS);
        int maxZ = (int) Math.floor(pz + PLAYER_RADIUS);

        for (int bx = minX; bx <= maxX; bx++) {
            for (int by = minY; by <= maxY; by++) {
                for (int bz = minZ; bz <= maxZ; bz++) {
                    if (VoxelBlock.isSolid(world.getBlock(bx, by, bz))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public float getEyeY() {
        return y + 1.62f;
    }

    public float[] getLookDirection() {
        float radYaw = (float) Math.toRadians(yaw);
        float radPitch = (float) Math.toRadians(pitch);

        float dirX = (float) (-Math.sin(radYaw) * Math.cos(radPitch));
        float dirY = (float) (-Math.sin(radPitch));
        float dirZ = (float) (Math.cos(radYaw) * Math.cos(radPitch));

        return new float[]{dirX, dirY, dirZ};
    }
}
