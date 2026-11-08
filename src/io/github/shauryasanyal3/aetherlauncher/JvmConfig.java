package io.github.shauryasanyal3.aetherlauncher;

public class JvmConfig {

    public static String getFlags(String gcPreset, int ramMb) {
        StringBuilder sb = new StringBuilder();
        sb.append("-Xms").append(ramMb).append("M ");
        sb.append("-Xmx").append(ramMb).append("M ");

        if ("Shenandoah".equalsIgnoreCase(gcPreset)) {
            sb.append("-XX:+UseShenandoahGC ");
            sb.append("-XX:ShenandoahGCMode=iu ");
            sb.append("-XX:ShenandoahGCHeuristics=adaptive ");
            sb.append("-XX:+AlwaysPreTouch ");
        } else if ("ZGC".equalsIgnoreCase(gcPreset)) {
            sb.append("-XX:+UseZGC ");
            sb.append("-XX:+ZGenerational ");
        } else {
            sb.append("-XX:+UseG1GC ");
            sb.append("-XX:MaxGCPauseMillis=20 ");
            sb.append("-XX:G1ReservePercent=15 ");
        }

        sb.append("-XX:+UseStringDeduplication ");
        sb.append("-XX:+OptimizeStringConcat ");
        sb.append("-Dsun.rmi.dgc.client.gcInterval=3600000 ");
        sb.append("-Dfile.encoding=UTF-8");

        return sb.toString();
    }
}
