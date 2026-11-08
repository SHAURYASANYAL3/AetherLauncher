package io.github.shauryasanyal3.aetherlauncher;

import android.app.Activity;
import android.graphics.Color;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public class GameScreenActivity extends Activity {

    private GLSurfaceView glSurfaceView;
    private VoxelWorld world;
    private VoxelPlayer player;
    private VoxelRenderer renderer;

    private TextView tvGameFps;
    private TextView tvGameCoords;

    private float lastTouchX;
    private float lastTouchY;
    private boolean isLooking = false;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean isRunning = true;

    // Joystick State
    private float joystickCenterX;
    private float joystickCenterY;
    private static final float JOYSTICK_MAX_RADIUS = 110.0f; // pixels

    // Hotbar buttons
    private Button[] hotbarButtons;
    private final byte[] hotbarBlocks = {
            VoxelBlock.GRASS,
            VoxelBlock.COBBLESTONE,
            VoxelBlock.WOOD,
            VoxelBlock.DIAMOND,
            VoxelBlock.BRICK,
            VoxelBlock.GLASS
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_screen);

        hideSystemUI();

        glSurfaceView = findViewById(R.id.gl_surface_view);
        tvGameFps = findViewById(R.id.tv_game_fps);
        tvGameCoords = findViewById(R.id.tv_game_coords);

        // Initialize 3D Voxel World and Player
        world = new VoxelWorld();
        player = new VoxelPlayer();
        player.x = 8.0f;
        player.y = 28.0f;
        player.z = 8.0f;
        player.selectedBlock = VoxelBlock.COBBLESTONE;

        // Configure GLES 3.0 Surface
        glSurfaceView.setEGLContextClientVersion(3);
        renderer = new VoxelRenderer(world, player);
        glSurfaceView.setRenderer(renderer);
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        setupLookControls();
        setupJoystick();
        setupActionButtons();
        setupHotbar();

        Button btnExit = findViewById(R.id.btn_game_exit);
        if (btnExit != null) {
            btnExit.setOnClickListener(v -> finish());
        }

        startHudTicker();
    }

    private void hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            final WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
    }

    private void setupLookControls() {
        View touchLookZone = findViewById(R.id.touch_look_zone);
        if (touchLookZone == null) return;

        touchLookZone.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    lastTouchX = event.getX();
                    lastTouchY = event.getY();
                    isLooking = true;
                    return true;

                case MotionEvent.ACTION_MOVE:
                    if (isLooking) {
                        float dx = event.getX() - lastTouchX;
                        float dy = event.getY() - lastTouchY;
                        lastTouchX = event.getX();
                        lastTouchY = event.getY();

                        // Update camera rotation (intuitive FPS look)
                        player.yaw += dx * 0.22f;
                        player.pitch = Math.max(-89.0f, Math.min(89.0f, player.pitch + dy * 0.22f));
                    }
                    return true;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    isLooking = false;
                    return true;
            }
            return false;
        });
    }

    private void setupJoystick() {
        View stickBase = findViewById(R.id.game_joystick_base);
        View stickThumb = findViewById(R.id.game_joystick_thumb);
        if (stickBase == null || stickThumb == null) return;

        stickBase.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    joystickCenterX = stickBase.getWidth() / 2.0f;
                    joystickCenterY = stickBase.getHeight() / 2.0f;
                    // fallthrough to handle first touch position

                case MotionEvent.ACTION_MOVE:
                    float touchX = event.getX();
                    float touchY = event.getY();
                    float dx = touchX - joystickCenterX;
                    float dy = touchY - joystickCenterY;
                    float distance = (float) Math.sqrt(dx * dx + dy * dy);

                    float clampedDist = Math.min(distance, JOYSTICK_MAX_RADIUS);
                    float dirX = (distance > 0) ? (dx / distance) : 0f;
                    float dirY = (distance > 0) ? (dy / distance) : 0f;

                    float thumbX = dirX * clampedDist;
                    float thumbY = dirY * clampedDist;
                    stickThumb.setTranslationX(thumbX);
                    stickThumb.setTranslationY(thumbY);

                    // Map to forward/strafe inputs (-1.0 to 1.0)
                    float intensity = clampedDist / JOYSTICK_MAX_RADIUS;
                    renderer.moveForward = -dirY * intensity;
                    renderer.moveStrafe = dirX * intensity;
                    return true;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    stickThumb.setTranslationX(0f);
                    stickThumb.setTranslationY(0f);
                    renderer.moveForward = 0f;
                    renderer.moveStrafe = 0f;
                    return true;
            }
            return false;
        });
    }

    private void setupActionButtons() {
        Button btnBreak = findViewById(R.id.btn_game_break);
        Button btnPlace = findViewById(R.id.btn_game_place);
        Button btnJump = findViewById(R.id.btn_game_jump);

        if (btnBreak != null) {
            btnBreak.setOnClickListener(v -> {
                v.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP);
                float[] look = player.getLookDirection();
                VoxelWorld.RaycastHit hit = world.raycast(player.x, player.getEyeY(), player.z, look[0], look[1], look[2], 6.0f);
                if (hit != null) {
                    world.setBlock(hit.x, hit.y, hit.z, VoxelBlock.AIR);
                }
            });
        }

        if (btnPlace != null) {
            btnPlace.setOnClickListener(v -> {
                v.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP);
                float[] look = player.getLookDirection();
                VoxelWorld.RaycastHit hit = world.raycast(player.x, player.getEyeY(), player.z, look[0], look[1], look[2], 6.0f);
                if (hit != null) {
                    int placeX = hit.x + hit.faceX;
                    int placeY = hit.y + hit.faceY;
                    int placeZ = hit.z + hit.faceZ;

                    if (!intersectsPlayer(placeX, placeY, placeZ, player)) {
                        world.setBlock(placeX, placeY, placeZ, player.selectedBlock);
                    }
                }
            });
        }

        if (btnJump != null) {
            btnJump.setOnClickListener(v -> {
                v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
                player.jump();
            });
        }
    }

    private boolean intersectsPlayer(int bx, int by, int bz, VoxelPlayer p) {
        boolean overlapX = (p.x + VoxelPlayer.PLAYER_RADIUS > bx) && (p.x - VoxelPlayer.PLAYER_RADIUS < bx + 1);
        boolean overlapY = (p.y + VoxelPlayer.PLAYER_HEIGHT > by) && (p.y < by + 1);
        boolean overlapZ = (p.z + VoxelPlayer.PLAYER_RADIUS > bz) && (p.z - VoxelPlayer.PLAYER_RADIUS < bz + 1);
        return overlapX && overlapY && overlapZ;
    }

    private void setupHotbar() {
        hotbarButtons = new Button[]{
                findViewById(R.id.slot_grass),
                findViewById(R.id.slot_cobble),
                findViewById(R.id.slot_wood),
                findViewById(R.id.slot_diamond),
                findViewById(R.id.slot_brick),
                findViewById(R.id.slot_glass)
        };

        for (int i = 0; i < hotbarButtons.length; i++) {
            final int index = i;
            if (hotbarButtons[i] != null) {
                hotbarButtons[i].setOnClickListener(v -> selectSlot(index));
            }
        }
        selectSlot(1); // Default to Cobblestone
    }

    private void selectSlot(int index) {
        if (index < 0 || index >= hotbarBlocks.length) return;
        player.selectedBlock = hotbarBlocks[index];

        for (int i = 0; i < hotbarButtons.length; i++) {
            if (hotbarButtons[i] != null) {
                if (i == index) {
                    hotbarButtons[i].setBackgroundColor(Color.parseColor("#38BDF8")); // Active Cyan
                } else {
                    hotbarButtons[i].setBackgroundResource(R.drawable.bg_button_secondary);
                }
            }
        }
    }

    private void startHudTicker() {
        final Runnable ticker = new Runnable() {
            @Override
            public void run() {
                if (!isRunning) return;

                if (tvGameFps != null && renderer != null) {
                    tvGameFps.setText(String.format(Locale.US, "%.1f FPS (%.2f ms)",
                            renderer.currentFps, renderer.currentFrametimeMs));
                }

                if (tvGameCoords != null && player != null && world != null) {
                    float[] look = player.getLookDirection();
                    VoxelWorld.RaycastHit hit = world.raycast(player.x, player.getEyeY(), player.z, look[0], look[1], look[2], 6.0f);
                    String targetName = (hit != null) ? VoxelBlock.getName(hit.block) : "Air";
                    tvGameCoords.setText(String.format(Locale.US, "XYZ: %.1f / %.1f / %.1f | Block: %s",
                            player.x, player.y, player.z, targetName));
                }

                mainHandler.postDelayed(this, 120);
            }
        };
        mainHandler.post(ticker);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (glSurfaceView != null) {
            glSurfaceView.onResume();
        }
        hideSystemUI();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (glSurfaceView != null) {
            glSurfaceView.onPause();
        }
    }

    @Override
    protected void onDestroy() {
        isRunning = false;
        super.onDestroy();
    }
}
