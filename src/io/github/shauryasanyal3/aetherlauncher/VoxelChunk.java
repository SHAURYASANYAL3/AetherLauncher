package io.github.shauryasanyal3.aetherlauncher;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;

public class VoxelChunk {
    public static final int CHUNK_X = 16;
    public static final int CHUNK_Z = 16;
    public static final int CHUNK_Y = 48;

    public final int chunkOriginX;
    public final int chunkOriginZ;
    public final byte[][][] blocks = new byte[CHUNK_X][CHUNK_Y][CHUNK_Z];

    public FloatBuffer vertexBuffer;
    public int vertexCount = 0;
    public boolean isDirty = true;

    public VoxelChunk(int originX, int originZ) {
        this.chunkOriginX = originX;
        this.chunkOriginZ = originZ;
        generateTerrain();
    }

    private void generateTerrain() {
        for (int x = 0; x < CHUNK_X; x++) {
            for (int z = 0; z < CHUNK_Z; z++) {
                int worldX = chunkOriginX + x;
                int worldZ = chunkOriginZ + z;

                // Procedural rolling terrain height formula
                double heightD = 18.0 + 7.0 * Math.sin(worldX * 0.12) * Math.cos(worldZ * 0.12)
                                      + 4.0 * Math.sin((worldX + worldZ) * 0.06);
                int surfaceY = (int) Math.max(4, Math.min(CHUNK_Y - 8, heightD));

                for (int y = 0; y < CHUNK_Y; y++) {
                    if (y == 0) {
                        blocks[x][y][z] = VoxelBlock.STONE; // Bedrock level
                    } else if (y < surfaceY - 3) {
                        // Rare diamond vein
                        if (y < 8 && ((worldX * 31 + worldZ * 17 + y * 7) % 43 == 0)) {
                            blocks[x][y][z] = VoxelBlock.DIAMOND;
                        } else {
                            blocks[x][y][z] = VoxelBlock.STONE;
                        }
                    } else if (y < surfaceY) {
                        blocks[x][y][z] = VoxelBlock.DIRT;
                    } else if (y == surfaceY) {
                        blocks[x][y][z] = VoxelBlock.GRASS;
                    } else {
                        blocks[x][y][z] = VoxelBlock.AIR;
                    }
                }

                // Procedural Oak Trees
                if (x >= 3 && x < CHUNK_X - 3 && z >= 3 && z < CHUNK_Z - 3) {
                    if (((worldX * 73 + worldZ * 91) % 59) == 0 && surfaceY + 6 < CHUNK_Y) {
                        plantTree(x, surfaceY + 1, z);
                    }
                }
            }
        }
    }

    private void plantTree(int tx, int ty, int tz) {
        int trunkHeight = 4;
        for (int y = 0; y < trunkHeight; y++) {
            if (ty + y < CHUNK_Y) blocks[tx][ty + y][tz] = VoxelBlock.WOOD;
        }

        int leafStart = ty + trunkHeight - 1;
        for (int lx = -2; lx <= 2; lx++) {
            for (int lz = -2; lz <= 2; lz++) {
                for (int ly = 0; ly <= 2; ly++) {
                    int bx = tx + lx;
                    int by = leafStart + ly;
                    int bz = tz + lz;
                    if (bx >= 0 && bx < CHUNK_X && bz >= 0 && bz < CHUNK_Z && by < CHUNK_Y) {
                        if (Math.abs(lx) == 2 && Math.abs(lz) == 2 && ly == 2) continue; // Corner cut
                        if (blocks[bx][by][bz] == VoxelBlock.AIR) {
                            blocks[bx][by][bz] = VoxelBlock.LEAVES;
                        }
                    }
                }
            }
        }
    }

    public byte getBlock(int x, int y, int z) {
        if (x < 0 || x >= CHUNK_X || y < 0 || y >= CHUNK_Y || z < 0 || z >= CHUNK_Z) {
            return VoxelBlock.AIR;
        }
        return blocks[x][y][z];
    }

    public void setBlock(int x, int y, int z, byte block) {
        if (x >= 0 && x < CHUNK_X && y >= 0 && y < CHUNK_Y && z >= 0 && z < CHUNK_Z) {
            blocks[x][y][z] = block;
            isDirty = true;
        }
    }

    public void rebuildMesh(VoxelWorld world) {
        ArrayList<Float> verts = new ArrayList<>(16384);

        for (int x = 0; x < CHUNK_X; x++) {
            for (int y = 0; y < CHUNK_Y; y++) {
                for (int z = 0; z < CHUNK_Z; z++) {
                    byte b = blocks[x][y][z];
                    if (b == VoxelBlock.AIR) continue;

                    int wx = chunkOriginX + x;
                    int wy = y;
                    int wz = chunkOriginZ + z;

                    // Face 0: TOP (+Y, normal=[0, 1, 0])
                    byte neighborTop = (y < CHUNK_Y - 1) ? blocks[x][y + 1][z] : (world != null ? world.getBlock(wx, wy + 1, wz) : VoxelBlock.AIR);
                    if (!VoxelBlock.isOpaque(neighborTop)) {
                        addQuad(verts,
                                wx, wy + 1, wz + 1,
                                wx + 1, wy + 1, wz + 1,
                                wx + 1, wy + 1, wz,
                                wx, wy + 1, wz,
                                VoxelBlock.getColor(b, 0), 1.0f);
                    }

                    // Face 1: BOTTOM (-Y, normal=[0, -1, 0])
                    byte neighborBottom = (y > 0) ? blocks[x][y - 1][z] : (world != null ? world.getBlock(wx, wy - 1, wz) : VoxelBlock.AIR);
                    if (!VoxelBlock.isOpaque(neighborBottom)) {
                        addQuad(verts,
                                wx, wy, wz,
                                wx + 1, wy, wz,
                                wx + 1, wy, wz + 1,
                                wx, wy, wz + 1,
                                VoxelBlock.getColor(b, 1), 0.55f);
                    }

                    // Face 2: NORTH (-Z, normal=[0, 0, -1])
                    byte neighborNorth = (z > 0) ? blocks[x][y][z - 1] : (world != null ? world.getBlock(wx, wy, wz - 1) : VoxelBlock.AIR);
                    if (!VoxelBlock.isOpaque(neighborNorth)) {
                        addQuad(verts,
                                wx + 1, wy, wz,
                                wx, wy, wz,
                                wx, wy + 1, wz,
                                wx + 1, wy + 1, wz,
                                VoxelBlock.getColor(b, 2), 0.75f);
                    }

                    // Face 3: SOUTH (+Z, normal=[0, 0, 1])
                    byte neighborSouth = (z < CHUNK_Z - 1) ? blocks[x][y][z + 1] : (world != null ? world.getBlock(wx, wy, wz + 1) : VoxelBlock.AIR);
                    if (!VoxelBlock.isOpaque(neighborSouth)) {
                        addQuad(verts,
                                wx, wy, wz + 1,
                                wx + 1, wy, wz + 1,
                                wx + 1, wy + 1, wz + 1,
                                wx, wy + 1, wz + 1,
                                VoxelBlock.getColor(b, 3), 0.85f);
                    }

                    // Face 4: WEST (-X, normal=[-1, 0, 0])
                    byte neighborWest = (x > 0) ? blocks[x - 1][y][z] : (world != null ? world.getBlock(wx - 1, wy, wz) : VoxelBlock.AIR);
                    if (!VoxelBlock.isOpaque(neighborWest)) {
                        addQuad(verts,
                                wx, wy, wz,
                                wx, wy, wz + 1,
                                wx, wy + 1, wz + 1,
                                wx, wy + 1, wz,
                                VoxelBlock.getColor(b, 4), 0.70f);
                    }

                    // Face 5: EAST (+X, normal=[1, 0, 0])
                    byte neighborEast = (x < CHUNK_X - 1) ? blocks[x + 1][y][z] : (world != null ? world.getBlock(wx + 1, wy, wz) : VoxelBlock.AIR);
                    if (!VoxelBlock.isOpaque(neighborEast)) {
                        addQuad(verts,
                                wx + 1, wy, wz + 1,
                                wx + 1, wy, wz,
                                wx + 1, wy + 1, wz,
                                wx + 1, wy + 1, wz + 1,
                                VoxelBlock.getColor(b, 5), 0.80f);
                    }
                }
            }
        }

        vertexCount = verts.size() / 7;
        float[] arr = new float[verts.size()];
        for (int i = 0; i < verts.size(); i++) arr[i] = verts.get(i);

        ByteBuffer bb = ByteBuffer.allocateDirect(arr.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(arr);
        vertexBuffer.position(0);
        isDirty = false;
    }

    public void rebuildMesh() {
        rebuildMesh(null);
    }

    private void addQuad(ArrayList<Float> list,
                         float x0, float y0, float z0,
                         float x1, float y1, float z1,
                         float x2, float y2, float z2,
                         float x3, float y3, float z3,
                         float[] col, float light) {
        float r = col[0] * light;
        float g = col[1] * light;
        float b = col[2] * light;
        float a = col[3];

        // Triangle 1 (0, 1, 2)
        addVertex(list, x0, y0, z0, r, g, b, a);
        addVertex(list, x1, y1, z1, r, g, b, a);
        addVertex(list, x2, y2, z2, r, g, b, a);

        // Triangle 2 (0, 2, 3)
        addVertex(list, x0, y0, z0, r, g, b, a);
        addVertex(list, x2, y2, z2, r, g, b, a);
        addVertex(list, x3, y3, z3, r, g, b, a);
    }

    private void addVertex(ArrayList<Float> list, float x, float y, float z, float r, float g, float b, float a) {
        list.add(x);
        list.add(y);
        list.add(z);
        list.add(r);
        list.add(g);
        list.add(b);
        list.add(a);
    }
}
