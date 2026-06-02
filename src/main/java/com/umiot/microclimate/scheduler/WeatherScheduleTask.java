package com.umiot.microclimate.scheduler;

import org.springframework.stereotype.Component;

@Component
public class WeatherScheduleTask {

    // Historical data is imported on-demand via POST /api/weather/import
    // rather than auto-scraped every 15 minutes.
    // To re-enable periodic import, add a @Scheduled method here that calls
    // WeatherHistoryScraperService.importHistoricalData().

}
