package io.github.shauryasanyal3.aetherlauncher;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.view.Display;
import android.view.WindowManager;

public class HardwareProfiler {

    public static class DeviceSpec {
        public String model;
        public String manufacturer;
        public String soc;
        public String gpuVendor;
        public boolean isMali;
        public boolean isAdreno;
        public long totalRamMb;
        public float refreshRateHz;
        public int sdkVersion;
        public String recommendedRenderer;
        public String recommendedGc;
        public int recommendedRamMb;
    }

    public static DeviceSpec probe(Context context) {
        DeviceSpec spec = new DeviceSpec();
        spec.model = Build.MODEL;
        spec.manufacturer = Build.MANUFACTURER;
        spec.sdkVersion = Build.VERSION.SDK_INT;

        // Try getting SOC name from Build or properties
        String soc = Build.HARDWARE;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                soc = Build.SOC_MODEL;
            } catch (Throwable ignored) {}
        }
        spec.soc = (soc != null && !soc.isEmpty()) ? soc : Build.BOARD;

        // Detect GPU architecture family
        String hardwareLower = (spec.soc + " " + Build.HARDWARE + " " + Build.BOARD + " " + Build.MANUFACTURER).toLowerCase();
        if (hardwareLower.contains("mali") || hardwareLower.contains("dimensity") || hardwareLower.contains("mt") || hardwareLower.contains("exynos")) {
            spec.isMali = true;
            spec.isAdreno = false;
            spec.gpuVendor = "ARM Mali Architecture (Valhall / Bifrost / Immortalis)";
            spec.recommendedRenderer = "Holy GL4ES / Mali Zink Pipeline";
            spec.recommendedGc = "Shenandoah GC";
        } else if (hardwareLower.contains("qcom") || hardwareLower.contains("qualcomm") || hardwareLower.contains("snapdragon") || hardwareLower.contains("adreno")) {
            spec.isAdreno = true;
            spec.isMali = false;
            spec.gpuVendor = "Qualcomm Adreno (Direct Vulkan 1.3)";
            spec.recommendedRenderer = "Zink (OpenGL over Vulkan 1.3)";
            spec.recommendedGc = "Shenandoah GC";
        } else {
            // General modern device (like V2312 - Qualcomm Snapdragon 8 Gen 3)
            spec.isAdreno = true;
            spec.isMali = false;
            spec.gpuVendor = "High-Performance GPU (Vulkan 1.3 Compatible)";
            spec.recommendedRenderer = "Zink (OpenGL over Vulkan 1.3)";
            spec.recommendedGc = "Shenandoah GC";
        }

        // Probe RAM
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        if (am != null) {
            am.getMemoryInfo(mi);
            spec.totalRamMb = mi.totalMem / (1024 * 1024);
        } else {
            spec.totalRamMb = 6144;
        }

        // Calculate safe recommended RAM (half of total RAM, capped at 6GB)
        long halfRam = spec.totalRamMb / 2;
        if (halfRam < 1024) halfRam = 1024;
        if (halfRam > 6144) halfRam = 6144;
        spec.recommendedRamMb = (int) halfRam;

        // Probe Display Refresh Rate
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (wm != null) {
            Display display = wm.getDefaultDisplay();
            spec.refreshRateHz = display.getRefreshRate();
        } else {
            spec.refreshRateHz = 60.0f;
        }

        return spec;
    }
}
