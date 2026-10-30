package com.lifepath.controller;

import com.lifepath.dto.TodayStatsResponse;
import com.lifepath.service.StatsService;
import com.lifepath.util.SecurityUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/stats")
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/today")
    public TodayStatsResponse getTodayStats() {
        return statsService.getTodayStats(SecurityUtil.currentUserId());
    }
}
