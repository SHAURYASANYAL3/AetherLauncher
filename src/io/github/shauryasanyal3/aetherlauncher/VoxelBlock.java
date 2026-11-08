package io.github.shauryasanyal3.aetherlauncher;

public class VoxelBlock {
    public static final byte AIR = 0;
    public static final byte GRASS = 1;
    public static final byte DIRT = 2;
    public static final byte STONE = 3;
    public static final byte COBBLESTONE = 4;
    public static final byte WOOD = 5;
    public static final byte LEAVES = 6;
    public static final byte DIAMOND = 7;
    public static final byte GLASS = 8;
    public static final byte BRICK = 9;

    public static boolean isOpaque(byte block) {
        return block != AIR && block != GLASS;
    }

    public static boolean isSolid(byte block) {
        return block != AIR;
    }

    public static float[] getColor(byte block, int face) {
        // face: 0=TOP (+Y), 1=BOTTOM (-Y), 2=NORTH (-Z), 3=SOUTH (+Z), 4=WEST (-X), 5=EAST (+X)
        switch (block) {
            case GRASS:
                if (face == 0) return new float[]{0.36f, 0.65f, 0.22f, 1.0f}; // Top Green
                if (face == 1) return new float[]{0.42f, 0.29f, 0.20f, 1.0f}; // Bottom Dirt
                return new float[]{0.48f, 0.36f, 0.25f, 1.0f}; // Side Dirt with grass
            case DIRT:
                return new float[]{0.45f, 0.32f, 0.22f, 1.0f};
            case STONE:
                return new float[]{0.50f, 0.50f, 0.50f, 1.0f};
            case COBBLESTONE:
                return new float[]{0.40f, 0.40f, 0.42f, 1.0f};
            case WOOD:
                if (face == 0 || face == 1) return new float[]{0.70f, 0.58f, 0.40f, 1.0f};
                return new float[]{0.42f, 0.28f, 0.16f, 1.0f};
            case LEAVES:
                return new float[]{0.18f, 0.55f, 0.20f, 1.0f};
            case DIAMOND:
                return new float[]{0.15f, 0.85f, 0.95f, 1.0f};
            case GLASS:
                return new float[]{0.75f, 0.90f, 0.95f, 0.6f};
            case BRICK:
                return new float[]{0.68f, 0.24f, 0.20f, 1.0f};
            default:
                return new float[]{1.0f, 0.0f, 1.0f, 1.0f};
        }
    }

    public static String getName(byte block) {
        switch (block) {
            case GRASS: return "Grass Block";
            case DIRT: return "Dirt";
            case STONE: return "Stone";
            case COBBLESTONE: return "Cobblestone";
            case WOOD: return "Oak Log";
            case LEAVES: return "Oak Leaves";
            case DIAMOND: return "Diamond Block";
            case GLASS: return "Glass";
            case BRICK: return "Bricks";
            default: return "Air";
        }
    }
}
