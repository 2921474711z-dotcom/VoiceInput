package com.voiceinput.pro.service;

import com.voiceinput.pro.model.dto.DistributionPointResponse;
import com.voiceinput.pro.model.dto.SampleCompareResponse;
import com.voiceinput.pro.model.dto.TrendPointResponse;
import com.voiceinput.pro.model.dto.UsageStatsResponse;
import com.voiceinput.pro.model.entity.ProcessingTaskEntity;
import com.voiceinput.pro.model.enums.SceneType;
import com.voiceinput.pro.model.enums.TaskStatus;
import com.voiceinput.pro.repository.ProcessingTaskRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final ProcessingTaskRepository processingTaskRepository;

    public UsageStatsResponse usage(SceneType sceneType, LocalDate from, LocalDate to) {
        LocalDate startDate = from == null ? LocalDate.now().minusDays(6) : from;
        LocalDate endDate = to == null ? LocalDate.now() : to;

        List<ProcessingTaskEntity> tasks = processingTaskRepository
            .findBySavedToHistoryTrueAndDeletedFalseAndCreatedAtBetween(
                startDate.atStartOfDay(),
                endDate.plusDays(1).atStartOfDay()
            )
            .stream()
            .filter(task -> task.getStatus() == TaskStatus.SUCCESS)
            .filter(task -> sceneType == null || task.getSceneType() == sceneType)
            .toList();

        int count = tasks.isEmpty() ? 1 : tasks.size();
        BigDecimal avgRec = average(tasks.stream().map(ProcessingTaskEntity::getRecognitionDurationMs).toList(), 1000);
        BigDecimal avgOpt = average(tasks.stream().map(ProcessingTaskEntity::getOptimizationDurationMs).toList(), 1000);
        BigDecimal avgTotal = average(tasks.stream().map(ProcessingTaskEntity::getTotalDurationMs).toList(), 1000);
        BigDecimal avgWords = averageInt(tasks.stream().map(ProcessingTaskEntity::getOptimizedWordCount).toList());
        BigDecimal totalCost = tasks.stream()
            .map(task -> task.getEstimatedCost() == null ? BigDecimal.ZERO : task.getEstimatedCost())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal avgCost = totalCost.divide(BigDecimal.valueOf(count), 4, RoundingMode.HALF_UP);
        long hotwordHits = tasks.stream().filter(task -> task.getHotwordHitCount() != null && task.getHotwordHitCount() > 0).count();
        BigDecimal hotwordRate = BigDecimal.valueOf(hotwordHits * 100.0 / count).setScale(2, RoundingMode.HALF_UP);

        List<TrendPointResponse> trend = buildTrend(tasks, startDate, endDate);
        List<DistributionPointResponse> sceneDistribution = tasks.stream()
            .collect(java.util.stream.Collectors.groupingBy(task -> task.getSceneType().getLabel(), java.util.stream.Collectors.counting()))
            .entrySet().stream()
            .map(entry -> new DistributionPointResponse(entry.getKey(), entry.getValue()))
            .sorted(Comparator.comparingLong(DistributionPointResponse::value).reversed())
            .toList();

        List<SampleCompareResponse> samples = tasks.stream()
            .sorted(Comparator.comparing(ProcessingTaskEntity::getCompletedAt, Comparator.nullsLast(Comparator.reverseOrder())))
            .limit(3)
            .map(task -> new SampleCompareResponse(
                task.getId(),
                task.getTitle(),
                task.getRawText(),
                task.getOptimizedText(),
                "已完成断句、结构化整理、口语清理与场景化优化。"
            ))
            .toList();

        String conclusion = "共统计 %d 条有效记录，平均总耗时 %s 秒，平均成本 %s 元，热词命中率 %s%%。"
            .formatted(tasks.size(), avgTotal.toPlainString(), avgCost.toPlainString(), hotwordRate.toPlainString());

        return new UsageStatsResponse(
            avgRec,
            avgOpt,
            avgTotal,
            avgWords,
            totalCost.setScale(4, RoundingMode.HALF_UP),
            avgCost,
            hotwordRate,
            trend,
            sceneDistribution,
            samples,
            conclusion
        );
    }

    private List<TrendPointResponse> buildTrend(List<ProcessingTaskEntity> tasks, LocalDate from, LocalDate to) {
        List<TrendPointResponse> result = new ArrayList<>();
        Map<LocalDate, List<ProcessingTaskEntity>> grouped = tasks.stream()
            .collect(java.util.stream.Collectors.groupingBy(task -> task.getCreatedAt().toLocalDate()));
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            List<ProcessingTaskEntity> daily = grouped.getOrDefault(date, List.of());
            double averageCost = daily.stream()
                .map(task -> task.getEstimatedCost() == null ? BigDecimal.ZERO : task.getEstimatedCost())
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(daily.isEmpty() ? 1 : daily.size()), 4, RoundingMode.HALF_UP)
                .doubleValue();
            result.add(new TrendPointResponse(date.toString(), daily.size(), averageCost));
        }
        return result;
    }

    private BigDecimal average(List<Long> values, int divisor) {
        long valid = values.stream().filter(java.util.Objects::nonNull).mapToLong(Long::longValue).sum();
        long count = values.stream().filter(java.util.Objects::nonNull).count();
        if (count == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(valid)
            .divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP)
            .divide(BigDecimal.valueOf(divisor), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal averageInt(List<Integer> values) {
        int valid = values.stream().filter(java.util.Objects::nonNull).mapToInt(Integer::intValue).sum();
        long count = values.stream().filter(java.util.Objects::nonNull).count();
        if (count == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(valid).divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
    }
}
