package io.github.shauryasanyal3.aetherlauncher;

import java.util.HashMap;
import java.util.Map;

public class VoxelWorld {
    public static final int WORLD_RADIUS_CHUNKS = 2; // 5x5 chunks = 80x80 blocks loaded
    private final Map<Long, VoxelChunk> chunks = new HashMap<>();

    public VoxelWorld() {
        initWorld();
    }

    private void initWorld() {
        for (int cx = -WORLD_RADIUS_CHUNKS; cx <= WORLD_RADIUS_CHUNKS; cx++) {
            for (int cz = -WORLD_RADIUS_CHUNKS; cz <= WORLD_RADIUS_CHUNKS; cz++) {
                int ox = cx * VoxelChunk.CHUNK_X;
                int oz = cz * VoxelChunk.CHUNK_Z;
                long key = chunkKey(cx, cz);
                VoxelChunk chunk = new VoxelChunk(ox, oz);
                chunks.put(key, chunk);
            }
        }
        for (VoxelChunk c : chunks.values()) {
            c.rebuildMesh(this);
        }
    }

    public static long chunkKey(int cx, int cz) {
        return (((long) cx) << 32) | (cz & 0xFFFFFFFFL);
    }

    public VoxelChunk getChunk(int chunkX, int chunkZ) {
        return chunks.get(chunkKey(chunkX, chunkZ));
    }

    public Map<Long, VoxelChunk> getLoadedChunks() {
        return chunks;
    }

    public byte getBlock(int x, int y, int z) {
        if (y < 0 || y >= VoxelChunk.CHUNK_Y) return VoxelBlock.AIR;
        int cx = Math.floorDiv(x, VoxelChunk.CHUNK_X);
        int cz = Math.floorDiv(z, VoxelChunk.CHUNK_Z);
        VoxelChunk chunk = getChunk(cx, cz);
        if (chunk == null) return VoxelBlock.AIR;
        int localX = x - (cx * VoxelChunk.CHUNK_X);
        int localZ = z - (cz * VoxelChunk.CHUNK_Z);
        return chunk.getBlock(localX, y, localZ);
    }

    public void setBlock(int x, int y, int z, byte block) {
        if (y < 0 || y >= VoxelChunk.CHUNK_Y) return;
        int cx = Math.floorDiv(x, VoxelChunk.CHUNK_X);
        int cz = Math.floorDiv(z, VoxelChunk.CHUNK_Z);
        VoxelChunk chunk = getChunk(cx, cz);
        if (chunk == null) return;
        int localX = x - (cx * VoxelChunk.CHUNK_X);
        int localZ = z - (cz * VoxelChunk.CHUNK_Z);
        chunk.setBlock(localX, y, localZ, block);

        // Mark neighbor chunks dirty if on border
        if (localX == 0) markDirty(cx - 1, cz);
        if (localX == VoxelChunk.CHUNK_X - 1) markDirty(cx + 1, cz);
        if (localZ == 0) markDirty(cx, cz - 1);
        if (localZ == VoxelChunk.CHUNK_Z - 1) markDirty(cx, cz + 1);
    }

    private void markDirty(int cx, int cz) {
        VoxelChunk c = getChunk(cx, cz);
        if (c != null) c.isDirty = true;
    }

    public static class RaycastHit {
        public int x, y, z;
        public int faceX, faceY, faceZ; // normal of hit face
        public byte block;
    }

    /**
     * Fast DDA voxel traversal raycast
     */
    public RaycastHit raycast(float startX, float startY, float startZ,
                              float dirX, float dirY, float dirZ, float maxDistance) {
        float stepSize = 0.05f;
        float traveled = 0f;

        int prevX = (int) Math.floor(startX);
        int prevY = (int) Math.floor(startY);
        int prevZ = (int) Math.floor(startZ);

        while (traveled < maxDistance) {
            float cx = startX + dirX * traveled;
            float cy = startY + dirY * traveled;
            float cz = startZ + dirZ * traveled;

            int bx = (int) Math.floor(cx);
            int by = (int) Math.floor(cy);
            int bz = (int) Math.floor(cz);

            byte b = getBlock(bx, by, bz);
            if (b != VoxelBlock.AIR) {
                RaycastHit hit = new RaycastHit();
                hit.x = bx;
                hit.y = by;
                hit.z = bz;
                hit.block = b;
                hit.faceX = prevX - bx;
                hit.faceY = prevY - by;
                hit.faceZ = prevZ - bz;
                return hit;
            }

            prevX = bx;
            prevY = by;
            prevZ = bz;
            traveled += stepSize;
        }

        return null;
    }
}
