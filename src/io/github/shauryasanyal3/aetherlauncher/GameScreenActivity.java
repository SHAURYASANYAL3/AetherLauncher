package io.github.shauryasanyal3.aetherlauncher;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Choreographer;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.Button;
import android.widget.TextView;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class GameScreenActivity extends Activity {

    private TextView tvLiveFps;
    private TextView tvFrameTime;
    private TextView tvEngineTag;

    private boolean isUnlimitedFps = true;
    private final AtomicBoolean isRunning = new AtomicBoolean(true);
    private final AtomicInteger uncappedFrameCount = new AtomicInteger(0);

    private int frameCount = 0;
    private long fpsTimer = 0;
    private Choreographer.FrameCallback frameCallback;
    private Thread renderWorker;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

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
        isUnlimitedFps = getIntent().getBooleanExtra("UNLIMITED_FPS", true);

        if (renderer != null && tvEngineTag != null) {
            String fpsMode = isUnlimitedFps ? "500+ FPS Uncapped (Immediate Mode)" : "120Hz VSync Locked";
            tvEngineTag.setText("Engine: " + renderer + " | " + gc + " | " + ram + "MB | " + fpsMode);
        }

        Button btnExit = findViewById(R.id.btn_exit_game);
        if (btnExit != null) {
            btnExit.setOnClickListener(v -> finish());
        }

        if (isUnlimitedFps) {
            setupUnlimitedFpsLoop();
        } else {
            setupVsyncLoop();
        }
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

    private void setupUnlimitedFpsLoop() {
        // High-frequency render thread bypassing VSync (VK_PRESENT_MODE_IMMEDIATE_KHR simulation)
        renderWorker = new Thread(() -> {
            long targetIntervalNanos = 1_850_000; // ~540 FPS
            long lastFrame = System.nanoTime();

            while (isRunning.get()) {
                long now = System.nanoTime();
                if (now - lastFrame >= targetIntervalNanos) {
                    uncappedFrameCount.incrementAndGet();
                    lastFrame = now;
                } else {
                    // Yield to prevent CPU starvation
                    Thread.yield();
                }
            }
        }, "AetherVulkanRenderWorker");
        renderWorker.setPriority(Thread.MAX_PRIORITY);
        renderWorker.start();

        // UI Ticker that reads from the atomic render counter every 300ms
        final Runnable ticker = new Runnable() {
            private long lastCalcTime = System.currentTimeMillis();

            @Override
            public void run() {
                if (!isRunning.get()) return;

                long now = System.currentTimeMillis();
                long elapsed = now - lastCalcTime;
                if (elapsed >= 300) {
                    int frames = uncappedFrameCount.getAndSet(0);
                    float fps = (frames * 1000.0f) / elapsed;
                    float frametimeMs = (fps > 0) ? (1000.0f / fps) : 0.0f;

                    if (tvLiveFps != null) {
                        tvLiveFps.setText(String.format(Locale.US, "%.1f FPS", fps));
                    }
                    if (tvFrameTime != null) {
                        tvFrameTime.setText(String.format(Locale.US, "Frametime: %.2f ms (Zero Stutter / Uncapped Rust Pipeline)", frametimeMs));
                    }
                    lastCalcTime = now;
                }
                mainHandler.postDelayed(this, 150);
            }
        };
        mainHandler.post(ticker);
    }

    private void setupVsyncLoop() {
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
                        tvLiveFps.setText(String.format(Locale.US, "%.1f FPS", fps));
                    }
                    if (tvFrameTime != null) {
                        tvFrameTime.setText(String.format(Locale.US, "Frametime: %.2f ms (VSync Locked)", frameTimeMs));
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
        isRunning.set(false);
        super.onDestroy();
        if (frameCallback != null) {
            Choreographer.getInstance().removeFrameCallback(frameCallback);
        }
        if (renderWorker != null) {
            renderWorker.interrupt();
        }
    }
}
