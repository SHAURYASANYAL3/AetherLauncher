package io.github.shauryasanyal3.aetherlauncher;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.Choreographer;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.Button;
import android.widget.TextView;

public class GameScreenActivity extends Activity {

    private TextView tvLiveFps;
    private TextView tvFrameTime;
    private TextView tvEngineTag;

    private int frameCount = 0;
    private long fpsTimer = 0;
    private Choreographer.FrameCallback frameCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_screen);

        hideSystemUI();

        tvLiveFps = findViewById(R.id.game_live_fps);
        tvFrameTime = findViewById(R.id.game_frame_time);
        tvEngineTag = findViewById(R.id.game_engine_tag);

        String renderer = getIntent().getStringExtra("RENDERER");
        String gc = getIntent().getStringExtra("GC");
        int ram = getIntent().getIntExtra("RAM", 4096);

        if (renderer != null && tvEngineTag != null) {
            tvEngineTag.setText("Engine: " + renderer + " | " + gc + " | " + ram + "MB RAM");
        }

        Button btnExit = findViewById(R.id.btn_exit_game);
        if (btnExit != null) {
            btnExit.setOnClickListener(v -> finish());
        }

        setupGameLoop();
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

    private void setupGameLoop() {
        fpsTimer = System.currentTimeMillis();
        frameCallback = new Choreographer.FrameCallback() {
            @Override
            public void doFrame(long frameTimeNanos) {
                frameCount++;
                long now = System.currentTimeMillis();
                if (now - fpsTimer >= 500) {
                    float fps = (frameCount * 1000f) / (now - fpsTimer);
                    float frameTimeMs = (fps > 0) ? (1000f / fps) : 0f;
                    if (tvLiveFps != null) {
                        tvLiveFps.setText(String.format("%.1f FPS", fps));
                    }
                    if (tvFrameTime != null) {
                        tvFrameTime.setText(String.format("Frametime: %.2f ms (Smooth Pacing)", frameTimeMs));
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
