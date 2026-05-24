package com.voiceinput.pro.model.dto;

import java.math.BigDecimal;
import java.util.List;

public record UsageStatsResponse(
    BigDecimal averageRecognitionSeconds,
    BigDecimal averageOptimizationSeconds,
    BigDecimal averageTotalSeconds,
    BigDecimal averageWords,
    BigDecimal totalCost,
    BigDecimal averageCost,
    BigDecimal hotwordHitRate,
    List<TrendPointResponse> trend,
    List<DistributionPointResponse> sceneDistribution,
    List<SampleCompareResponse> samples,
    String conclusion
) {
}

