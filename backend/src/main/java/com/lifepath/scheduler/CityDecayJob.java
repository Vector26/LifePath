package com.lifepath.scheduler;

import com.lifepath.service.CityService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
public class CityDecayJob {

    private final CityService cityService;

    public CityDecayJob(CityService cityService) {
        this.cityService = cityService;
    }

    @Scheduled(fixedRate = 300000)
    @Transactional
    public void processDecay() {
        cityService.normalizeDecayStateForAllUsers(Instant.now());
    }
}
