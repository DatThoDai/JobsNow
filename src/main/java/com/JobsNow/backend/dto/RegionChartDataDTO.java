package com.JobsNow.backend.dto;

import lombok.Data;

import java.util.List;

@Data
public class RegionChartDataDTO {
    private List<String> labels;
    private List<Long> counts;
}
