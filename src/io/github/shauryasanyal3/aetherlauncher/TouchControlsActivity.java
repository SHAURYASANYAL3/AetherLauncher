package io.github.shauryasanyal3.aetherlauncher;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.Choreographer;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class TouchControlsActivity extends Activity {

    private TextView tvFps;
    private TextView tvStats;
    private RelativeLayout joystickBase;
    private View joystickThumb;

    private Vibrator vibrator;
    private float joystickRadius = 100f;
    private float joystickCenterX = 0f;
    private float joystickCenterY = 0f;

    // FPS Counter variables
    private long lastFrameTimeNanos = 0;
    private int frameCount = 0;
    private long fpsTimer = 0;
    private Choreographer.FrameCallback frameCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_touch_controls);

        hideSystemUI();

        vibrator = (Context.VIBRATOR_SERVICE != null) ? (Vibrator) getSystemService(Context.VIBRATOR_SERVICE) : null;

        tvFps = findViewById(R.id.hud_fps_counter);
        tvStats = findViewById(R.id.hud_touch_stats);
        joystickBase = findViewById(R.id.joystick_base);
        joystickThumb = findViewById(R.id.joystick_thumb);

        Button btnClose = findViewById(R.id.btn_exit_touch_test);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> finish());
        }

        setupButtons();
        setupJoystick();
        setupFpsMeter();
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

    private void haptic() {
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(15, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(15);
            }
        }
    }

    private void setupButtons() {
        int[] buttonIds = {
            R.id.hud_btn_attack,
            R.id.hud_btn_use,
            R.id.hud_btn_jump,
            R.id.hud_btn_sneak,
            R.id.hud_btn_inv,
            R.id.hud_btn_f3,
            R.id.hud_btn_chat,
            R.id.hud_btn_keyboard,
            R.id.hud_btn_esc
        };

        for (int id : buttonIds) {
            View btn = findViewById(id);
            if (btn != null) {
                btn.setOnTouchListener((v, event) -> {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        haptic();
                        v.setAlpha(0.5f);
                        if (tvStats != null && v instanceof Button) {
                            tvStats.setText("Key Pressed: " + ((Button) v).getText() + " | State: ACTIVE");
                        }
                    } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                        v.setAlpha(1.0f);
                        if (tvStats != null) {
                            tvStats.setText("Multi-touch: Ready | Touch anywhere to test HUD");
                        }
                    }
                    return true;
                });
            }
        }
    }

    private void setupJoystick() {
        if (joystickBase == null || joystickThumb == null) return;

        joystickBase.post(() -> {
            joystickRadius = joystickBase.getWidth() / 2f;
            joystickCenterX = joystickRadius;
            joystickCenterY = joystickRadius;
        });

        joystickBase.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_MOVE:
                    float touchX = event.getX();
                    float touchY = event.getY();

                    float dx = touchX - joystickCenterX;
                    float dy = touchY - joystickCenterY;
                    float dist = (float) Math.sqrt(dx * dx + dy * dy);

                    if (dist > joystickRadius) {
                        dx = (dx / dist) * joystickRadius;
                        dy = (dy / dist) * joystickRadius;
                    }

                    joystickThumb.setTranslationX(dx);
                    joystickThumb.setTranslationY(dy);

                    if (tvStats != null) {
                        float normX = dx / joystickRadius;
                        float normY = -dy / joystickRadius;
                        tvStats.setText(String.format("Movement: X: %+.2f, Y: %+.2f | Walk: Active", normX, normY));
                    }
                    break;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    joystickThumb.setTranslationX(0f);
                    joystickThumb.setTranslationY(0f);
                    if (tvStats != null) {
                        tvStats.setText("Movement: Idle | Multi-touch: Ready");
                    }
                    break;
            }
            return true;
        });
    }

    private void setupFpsMeter() {
        fpsTimer = System.currentTimeMillis();
        frameCallback = new Choreographer.FrameCallback() {
            @Override
            public void doFrame(long frameTimeNanos) {
                frameCount++;
                long now = System.currentTimeMillis();
                if (now - fpsTimer >= 500) {
                    float fps = (frameCount * 1000f) / (now - fpsTimer);
                    if (tvFps != null) {
                        tvFps.setText(String.format("%.1f FPS (Active)", fps));
                    }
                    frameCount = 0;
                    fpsTimer = now;
                }
                Choreographer.getInstance().postFrameCallback(this);
            }
        };
        Choreographer.getInstance().postFrameCallback(frameCallback);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (frameCallback != null) {
            Choreographer.getInstance().removeFrameCallback(frameCallback);
        }
    }
}
