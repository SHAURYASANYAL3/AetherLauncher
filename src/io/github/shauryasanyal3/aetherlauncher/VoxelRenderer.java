package io.github.shauryasanyal3.aetherlauncher;

import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class VoxelRenderer implements GLSurfaceView.Renderer {

    private final VoxelWorld world;
    public final VoxelPlayer player;

    private int program;
    private int uMvpMatrixLoc;

    private final float[] projectionMatrix = new float[16];
    private final float[] viewMatrix = new float[16];
    private final float[] mvpMatrix = new float[16];

    public float moveForward = 0f;
    public float moveStrafe = 0f;

    // Realtime FPS Measurement
    public volatile float currentFps = 0f;
    public volatile float currentFrametimeMs = 0f;
    private long lastTimeNanos = 0;
    private int frameCount = 0;
    private long fpsTimerMs = 0;

    public VoxelRenderer(VoxelWorld world, VoxelPlayer player) {
        this.world = world;
        this.player = player;
    }

    private static final String VERTEX_SHADER =
            "#version 300 es\n" +
            "layout(location = 0) in vec3 a_Position;\n" +
            "layout(location = 1) in vec4 a_Color;\n" +
            "uniform mat4 u_MVPMatrix;\n" +
            "out vec4 v_Color;\n" +
            "out float v_Distance;\n" +
            "void main() {\n" +
            "    vec4 pos = u_MVPMatrix * vec4(a_Position, 1.0);\n" +
            "    gl_Position = pos;\n" +
            "    v_Color = a_Color;\n" +
            "    v_Distance = length(pos.xyz);\n" +
            "}\n";

    private static final String FRAGMENT_SHADER =
            "#version 300 es\n" +
            "precision mediump float;\n" +
            "in vec4 v_Color;\n" +
            "in float v_Distance;\n" +
            "out vec4 fragColor;\n" +
            "void main() {\n" +
            "    vec4 fogColor = vec4(0.47, 0.65, 1.0, 1.0);\n" +
            "    float fogFactor = clamp((75.0 - v_Distance) / (75.0 - 20.0), 0.0, 1.0);\n" +
            "    fragColor = mix(fogColor, v_Color, fogFactor);\n" +
            "}\n";

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES30.glClearColor(0.47f, 0.65f, 1.0f, 1.0f); // Minecraft Sky Blue
        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
        GLES30.glDepthFunc(GLES30.GL_LEQUAL);
        GLES30.glEnable(GLES30.GL_CULL_FACE);
        GLES30.glCullFace(GLES30.GL_BACK);

        int vs = loadShader(GLES30.GL_VERTEX_SHADER, VERTEX_SHADER);
        int fs = loadShader(GLES30.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);

        program = GLES30.glCreateProgram();
        GLES30.glAttachShader(program, vs);
        GLES30.glAttachShader(program, fs);
        GLES30.glLinkProgram(program);

        uMvpMatrixLoc = GLES30.glGetUniformLocation(program, "u_MVPMatrix");
        lastTimeNanos = System.nanoTime();
        fpsTimerMs = System.currentTimeMillis();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES30.glViewport(0, 0, width, height);
        float aspect = (float) width / (float) (height > 0 ? height : 1);
        Matrix.perspectiveM(projectionMatrix, 0, 75.0f, aspect, 0.1f, 200.0f);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        // Measure delta time
        long nowNanos = System.nanoTime();
        float dt = (nowNanos - lastTimeNanos) / 1_000_000_000.0f;
        lastTimeNanos = nowNanos;

        // Update player movement & physics
        player.update(dt, moveForward, moveStrafe, world);

        // Update FPS Counter
        frameCount++;
        long nowMs = System.currentTimeMillis();
        if (nowMs - fpsTimerMs >= 250) {
            currentFps = (frameCount * 1000.0f) / (nowMs - fpsTimerMs);
            currentFrametimeMs = (currentFps > 0) ? (1000.0f / currentFps) : 0f;
            frameCount = 0;
            fpsTimerMs = nowMs;
        }

        // Setup Camera View Matrix
        float eyeX = player.x;
        float eyeY = player.getEyeY();
        float eyeZ = player.z;

        float[] look = player.getLookDirection();
        float targetX = eyeX + look[0];
        float targetY = eyeY + look[1];
        float targetZ = eyeZ + look[2];

        Matrix.setLookAtM(viewMatrix, 0,
                eyeX, eyeY, eyeZ,
                targetX, targetY, targetZ,
                0f, 1f, 0f);

        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, viewMatrix, 0);

        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);
        GLES30.glUseProgram(program);
        GLES30.glUniformMatrix4fv(uMvpMatrixLoc, 1, false, mvpMatrix, 0);

        // Render all loaded chunks
        for (VoxelChunk chunk : world.getLoadedChunks().values()) {
            if (chunk.isDirty) {
                chunk.rebuildMesh(world);
            }
            if (chunk.vertexCount > 0 && chunk.vertexBuffer != null) {
                chunk.vertexBuffer.position(0);
                GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 7 * 4, chunk.vertexBuffer);
                GLES30.glEnableVertexAttribArray(0);

                chunk.vertexBuffer.position(3);
                GLES30.glVertexAttribPointer(1, 4, GLES30.GL_FLOAT, false, 7 * 4, chunk.vertexBuffer);
                GLES30.glEnableVertexAttribArray(1);

                GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, chunk.vertexCount);
            }
        }
    }

    private static int loadShader(int type, String shaderCode) {
        int shader = GLES30.glCreateShader(type);
        GLES30.glShaderSource(shader, shaderCode);
        GLES30.glCompileShader(shader);
        return shader;
    }
}
