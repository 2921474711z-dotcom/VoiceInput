package com.voiceinput.pro.controller;

import com.voiceinput.pro.model.dto.UsageStatsResponse;
import com.voiceinput.pro.model.enums.SceneType;
import com.voiceinput.pro.service.StatisticsService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatisticsService statisticsService;

    @GetMapping("/usage")
    public UsageStatsResponse usage(
        @RequestParam(required = false) SceneType sceneType,
        @RequestParam(required = false) LocalDate from,
        @RequestParam(required = false) LocalDate to
    ) {
        return statisticsService.usage(sceneType, from, to);
    }
}
