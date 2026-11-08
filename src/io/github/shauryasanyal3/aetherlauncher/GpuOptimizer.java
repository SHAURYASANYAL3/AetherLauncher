package io.github.shauryasanyal3.aetherlauncher;

import java.util.HashMap;
import java.util.Map;

public class GpuOptimizer {

    public enum RendererType {
        ZINK_VULKAN,
        HOLY_GL4ES,
        VIRGL_ANGLE
    }

    public static class OptimizationProfile {
        public RendererType renderer = RendererType.ZINK_VULKAN;
        public boolean enableMaliFixes = true;
        public boolean enableAdrenoDirect = true;
        public boolean forceHighRefresh = true;
        public boolean unlimitedFps = true;
        public int resolutionScale = 100;
        public String gcPreset = "Shenandoah";
        public int allocatedRamMb = 4096;
        public float targetFps = 500.0f;
    }

    public static Map<String, String> buildEnvironment(OptimizationProfile profile, boolean isMali) {
        Map<String, String> env = new HashMap<>();

        if (profile.unlimitedFps) {
            env.put("vulkan_wsi_present_mode", "immediate");
            env.put("MESA_VK_WSI_PRESENT_MODE", "immediate");
            env.put("MESA_GL_SWAP_INTERVAL", "0");
            env.put("vblank_mode", "0");
        } else {
            env.put("vulkan_wsi_present_mode", "mailbox");
        }

        if (profile.renderer == RendererType.ZINK_VULKAN) {
            env.put("MESA_LOADER_DRIVER_OVERRIDE", "zink");
            env.put("GALLIUM_DRIVER", "zink");
            env.put("ZINK_DESCRIPTORS", "lazy");
            env.put("vulkan_wsi_present_mode", "mailbox");
        } else if (profile.renderer == RendererType.HOLY_GL4ES) {
            env.put("LIBGL_ES", "3");
            env.put("LIBGL_GL", "21");
            env.put("LIBGL_USEVBO", "1");
            env.put("LIBGL_BATCH", "1");
        } else {
            env.put("GALLIUM_DRIVER", "virpipe");
        }

        // Mali Specific Optimization Flags
        if (isMali && profile.enableMaliFixes) {
            env.put("MESA_GL_VERSION_OVERRIDE", "4.6COMPAT");
            env.put("MESA_GLES_VERSION_OVERRIDE", "3.2");
            env.put("PAN_MESA_DEBUG", "deqp");
            env.put("MESA_SHADER_CACHE_DISABLE", "false");
            env.put("MESA_NO_ERROR", "1");
        }

        // Adreno Specific Direct Acceleration
        if (!isMali && profile.enableAdrenoDirect) {
            env.put("TU_DEBUG", "noconform");
            env.put("VK_KHR_shader_float16_int8", "1");
            env.put("ADRENO_DIRECT_BUFFER", "1");
        }

        return env;
    }
}
