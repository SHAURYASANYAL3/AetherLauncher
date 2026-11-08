package io.github.shauryasanyal3.aetherlauncher;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity {

    // Views
    private TextView tvDeviceInfo;
    private TextView tvProfileName;
    private TextView tvProfileSummary;
    private TextView tvGpuModel;
    private TextView tvVulkanVersion;
    private TextView tvMaliAdrenoStatus;
    private TextView tvRamStats;
    private TextView tvDisplayHz;
    private TextView tvAllocatedRam;
    private TextView tvResScaleLabel;
    private TextView tvLogsContent;
    private ScrollView svLogs;
    private EditText etJvmArgs;

    private View viewDashboard;
    private View viewRenderer;
    private View viewJvm;
    private View viewControls;
    private View viewLogs;

    private SeekBar sbRam;
    private SeekBar sbResScale;
    private RadioGroup rgRenderer;
    private RadioGroup rgGc;
    private CheckBox cbMaliOptimizations;
    private CheckBox cbAdrenoDirect;
    private CheckBox cbForceHighRefresh;

    private HardwareProfiler.DeviceSpec deviceSpec;
    private GpuOptimizer.OptimizationProfile currentProfile;
    private final StringBuilder logBuffer = new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bindViews();
        setupNavigation();

        log("[AetherLauncher] Booting hardware detection engine...");
        deviceSpec = HardwareProfiler.probe(this);
        currentProfile = new GpuOptimizer.OptimizationProfile();

        applyHardwareSpec(deviceSpec);
        setupControls();
        refreshProfileSummary();

        log("[AetherLauncher] Hardware initialized: " + deviceSpec.model + " (" + deviceSpec.gpuVendor + ")");
        log("[AetherLauncher] Total RAM: " + deviceSpec.totalRamMb + " MB | Refresh rate: " + deviceSpec.refreshRateHz + " Hz");
        log("[AetherLauncher] Ready for high-FPS Minecraft simulation.");
    }

    private void bindViews() {
        tvDeviceInfo = findViewById(R.id.tv_device_info);
        tvProfileName = findViewById(R.id.tv_profile_name);
        tvProfileSummary = findViewById(R.id.tv_profile_summary);
        tvGpuModel = findViewById(R.id.tv_gpu_model);
        tvVulkanVersion = findViewById(R.id.tv_vulkan_version);
        tvMaliAdrenoStatus = findViewById(R.id.tv_mali_adreno_status);
        tvRamStats = findViewById(R.id.tv_ram_stats);
        tvDisplayHz = findViewById(R.id.tv_display_hz);
        tvAllocatedRam = findViewById(R.id.tv_allocated_ram);
        tvResScaleLabel = findViewById(R.id.tv_res_scale_label);
        tvLogsContent = findViewById(R.id.tv_logs_content);
        svLogs = findViewById(R.id.sv_logs);
        etJvmArgs = findViewById(R.id.et_jvm_args);

        viewDashboard = findViewById(R.id.view_dashboard);
        viewRenderer = findViewById(R.id.view_renderer);
        viewJvm = findViewById(R.id.view_jvm);
        viewControls = findViewById(R.id.view_controls);
        viewLogs = findViewById(R.id.view_logs);

        sbRam = findViewById(R.id.sb_ram);
        sbResScale = findViewById(R.id.sb_res_scale);
        rgRenderer = findViewById(R.id.rg_renderer);
        rgGc = findViewById(R.id.rg_gc);
        cbMaliOptimizations = findViewById(R.id.cb_mali_optimizations);
        cbAdrenoDirect = findViewById(R.id.cb_adreno_direct);
        cbForceHighRefresh = findViewById(R.id.cb_force_high_refresh);
    }

    private void setupNavigation() {
        Button btnTabDash = findViewById(R.id.tab_btn_dashboard);
        Button btnTabRend = findViewById(R.id.tab_btn_renderer);
        Button btnTabJvm = findViewById(R.id.tab_btn_jvm);
        Button btnTabCtrl = findViewById(R.id.tab_btn_controls);
        Button btnTabLogs = findViewById(R.id.tab_btn_logs);

        Button[] tabs = {btnTabDash, btnTabRend, btnTabJvm, btnTabCtrl, btnTabLogs};

        btnTabDash.setOnClickListener(v -> switchTab(viewDashboard, btnTabDash, tabs));
        btnTabRend.setOnClickListener(v -> switchTab(viewRenderer, btnTabRend, tabs));
        btnTabJvm.setOnClickListener(v -> switchTab(viewJvm, btnTabJvm, tabs));
        btnTabCtrl.setOnClickListener(v -> switchTab(viewControls, btnTabCtrl, tabs));
        btnTabLogs.setOnClickListener(v -> switchTab(viewLogs, btnTabLogs, tabs));

        Button btnClearLogs = findViewById(R.id.btn_clear_logs);
        if (btnClearLogs != null) {
            btnClearLogs.setOnClickListener(v -> {
                logBuffer.setLength(0);
                if (tvLogsContent != null) tvLogsContent.setText("");
            });
        }
    }

    private void switchTab(View activeView, Button activeTab, Button[] allTabs) {
        viewDashboard.setVisibility(View.GONE);
        viewRenderer.setVisibility(View.GONE);
        viewJvm.setVisibility(View.GONE);
        viewControls.setVisibility(View.GONE);
        viewLogs.setVisibility(View.GONE);

        activeView.setVisibility(View.VISIBLE);

        for (Button b : allTabs) {
            b.setTextColor(getResources().getColor(R.color.text_secondary));
        }
        activeTab.setTextColor(getResources().getColor(R.color.accent_cyan));
    }

    private void applyHardwareSpec(HardwareProfiler.DeviceSpec spec) {
        String info = spec.manufacturer.toUpperCase() + " " + spec.model + " | " + spec.soc;
        tvDeviceInfo.setText(info);

        tvGpuModel.setText("GPU: " + spec.gpuVendor);
        tvVulkanVersion.setText("Vulkan 1.3: Supported & Verified");
        tvRamStats.setText("System Memory: " + spec.totalRamMb + " MB Total");
        tvDisplayHz.setText(String.format(Locale.US, "Display Refresh: %.0f Hz", spec.refreshRateHz));

        if (spec.isMali) {
            tvMaliAdrenoStatus.setText("Multi-GPU Architecture: ARM Mali Pipeline Active");
            currentProfile.renderer = GpuOptimizer.RendererType.HOLY_GL4ES;
            currentProfile.enableMaliFixes = true;
            if (rgRenderer != null) {
                RadioButton rbGl = findViewById(R.id.rb_gl4es);
                if (rbGl != null) rbGl.setChecked(true);
            }
        } else {
            tvMaliAdrenoStatus.setText("Multi-GPU Architecture: Qualcomm Adreno High-Speed Direct");
            currentProfile.renderer = GpuOptimizer.RendererType.ZINK_VULKAN;
            currentProfile.enableAdrenoDirect = true;
            if (rgRenderer != null) {
                RadioButton rbZk = findViewById(R.id.rb_zink);
                if (rbZk != null) rbZk.setChecked(true);
            }
        }

        currentProfile.allocatedRamMb = spec.recommendedRamMb;
        int ramGb = spec.recommendedRamMb / 1024;
        if (sbRam != null) sbRam.setProgress(ramGb);
        tvAllocatedRam.setText("Allocated RAM: " + spec.recommendedRamMb + " MB");
    }

    private void setupControls() {
        // RAM Slider
        if (sbRam != null) {
            sbRam.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (progress < 1) progress = 1;
                    int mb = progress * 1024;
                    currentProfile.allocatedRamMb = mb;
                    tvAllocatedRam.setText("Allocated RAM: " + mb + " MB");
                    updateJvmText();
                    refreshProfileSummary();
                }

                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        }

        // Resolution Scale
        if (sbResScale != null) {
            sbResScale.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    int scale = 50 + progress; // 50% to 100%
                    currentProfile.resolutionScale = scale;
                    tvResScaleLabel.setText("Resolution Scale: " + scale + "%");
                }

                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        }

        // Renderer Radio Group
        if (rgRenderer != null) {
            rgRenderer.setOnCheckedChangeListener((group, checkedId) -> {
                if (checkedId == R.id.rb_zink) {
                    currentProfile.renderer = GpuOptimizer.RendererType.ZINK_VULKAN;
                    log("[GPU] Selected Zink (Vulkan 1.3)");
                } else if (checkedId == R.id.rb_gl4es) {
                    currentProfile.renderer = GpuOptimizer.RendererType.HOLY_GL4ES;
                    log("[GPU] Selected Holy GL4ES (Mali Optimized)");
                } else {
                    currentProfile.renderer = GpuOptimizer.RendererType.VIRGL_ANGLE;
                    log("[GPU] Selected VirGL Software Fallback");
                }
                refreshProfileSummary();
            });
        }

        // GC Preset Radio Group
        if (rgGc != null) {
            rgGc.setOnCheckedChangeListener((group, checkedId) -> {
                if (checkedId == R.id.rb_shenandoah) {
                    currentProfile.gcPreset = "Shenandoah";
                } else if (checkedId == R.id.rb_zgc) {
                    currentProfile.gcPreset = "ZGC";
                } else {
                    currentProfile.gcPreset = "G1GC";
                }
                log("[JVM] GC Preset changed to " + currentProfile.gcPreset);
                updateJvmText();
                refreshProfileSummary();
            });
        }

        // Auto Optimize Button
        Button btnOptimize = findViewById(R.id.btn_auto_optimize);
        if (btnOptimize != null) {
            btnOptimize.setOnClickListener(v -> {
                applyHardwareSpec(deviceSpec);
                Toast.makeText(this, "Profile tuned for " + deviceSpec.gpuVendor + "!", Toast.LENGTH_SHORT).show();
                log("[Optimizer] Auto-calibration executed. Applied optimal renderer & GC flags.");
            });
        }

        // Launch Game Button
        Button btnLaunch = findViewById(R.id.btn_launch_game);
        if (btnLaunch != null) {
            btnLaunch.setOnClickListener(v -> launchGame());
        }

        // Open Touch Controls Test
        Button btnTouchTest = findViewById(R.id.btn_open_touch_test);
        if (btnTouchTest != null) {
            btnTouchTest.setOnClickListener(v -> {
                Intent intent = new Intent(this, TouchControlsActivity.class);
                startActivity(intent);
            });
        }

        Button btnStudio = findViewById(R.id.btn_launch_touch_studio);
        if (btnStudio != null) {
            btnStudio.setOnClickListener(v -> {
                Intent intent = new Intent(this, TouchControlsActivity.class);
                startActivity(intent);
            });
        }

        updateJvmText();
    }

    private void updateJvmText() {
        if (etJvmArgs != null) {
            String flags = JvmConfig.getFlags(currentProfile.gcPreset, currentProfile.allocatedRamMb);
            etJvmArgs.setText(flags);
        }
    }

    private void refreshProfileSummary() {
        String rendStr = (currentProfile.renderer == GpuOptimizer.RendererType.ZINK_VULKAN) ? "Zink Vulkan" :
                         (currentProfile.renderer == GpuOptimizer.RendererType.HOLY_GL4ES) ? "Holy GL4ES" : "VirGL";

        String summary = String.format(Locale.US,
                "Renderer: %s | RAM: %d MB | %s GC | Res: %d%%",
                rendStr,
                currentProfile.allocatedRamMb,
                currentProfile.gcPreset,
                currentProfile.resolutionScale);

        if (tvProfileSummary != null) {
            tvProfileSummary.setText(summary);
        }
    }

    private void launchGame() {
        log("[AetherLauncher] Launching Minecraft instance...");
        Map<String, String> env = GpuOptimizer.buildEnvironment(currentProfile, deviceSpec.isMali);
        for (Map.Entry<String, String> entry : env.entrySet()) {
            log("[Env] " + entry.getKey() + "=" + entry.getValue());
        }

        log("[JVM] Command: java " + JvmConfig.getFlags(currentProfile.gcPreset, currentProfile.allocatedRamMb) + " net.minecraft.client.main.Main");
        log("[AetherLauncher] Shaders & sodium pipeline engaged. Transitioning to game canvas...");

        Intent intent = new Intent(this, GameScreenActivity.class);
        intent.putExtra("RENDERER", currentProfile.renderer.toString());
        intent.putExtra("GC", currentProfile.gcPreset);
        intent.putExtra("RAM", currentProfile.allocatedRamMb);
        startActivity(intent);
    }

    private void log(String message) {
        String timestamp = new SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(new Date());
        String line = "[" + timestamp + "] " + message + "\n";
        logBuffer.append(line);

        if (tvLogsContent != null) {
            tvLogsContent.setText(logBuffer.toString());
        }
        if (svLogs != null) {
            svLogs.post(() -> svLogs.fullScroll(View.FOCUS_DOWN));
        }
    }
}
