package com.gvp.marifariyaad.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DashboardStatsResponse {
    private long total;
    private long pending;
    private long inProgress;
    private long resolved;
}
