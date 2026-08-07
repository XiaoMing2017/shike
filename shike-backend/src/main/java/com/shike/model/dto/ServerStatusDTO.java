package com.shike.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServerStatusDTO {
    // 1. 系统与 JVM 基础信息
    private String osName;
    private String osArch;
    private Integer cpuCores;
    private String jvmVersion;
    private String jvmVendor;
    private String startTime;
    private String uptime;

    // 2. CPU & 内存 (Memory)
    private Double cpuUsage; // 百分比, 如 12.5%
    private Long jvmTotalMemoryMb;
    private Long jvmUsedMemoryMb;
    private Long jvmFreeMemoryMb;
    private Long jvmMaxMemoryMb;
    private Double jvmMemoryUsagePercent;

    // 3. 磁盘 (Disk) 状态
    private Double diskTotalGb;
    private Double diskUsedGb;
    private Double diskFreeGb;
    private Double diskUsagePercent;

    // 4. 线程状态 (Threads)
    private Integer threadCount;
    private Integer daemonThreadCount;
    private Integer peakThreadCount;

    // 5. 基础服务组件健康检测 (MySQL / Redis)
    private ComponentStatus mysqlStatus;
    private ComponentStatus redisStatus;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComponentStatus {
        private String name;
        private String status; // HEALTHY / DOWN
        private Long latencyMs; // 毫秒
        private String details;
    }
}
