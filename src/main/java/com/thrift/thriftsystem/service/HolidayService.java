package com.thrift.thriftsystem.service;

import com.thrift.thriftsystem.integration.HolidayClient;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class HolidayService {

    private final HolidayClient holidayClient;
    private static final String COUNTRY_CODE = "NG";

    public boolean isPublicHoliday(LocalDate date) {
        try {
            List<Map<String, Object>> holidays =
                    holidayClient.getPublicHolidays(date.getYear(), COUNTRY_CODE);
            return holidays.stream().anyMatch(h -> h.get("date").equals(date.toString()));
        } catch (Exception e) {
            log.error("Failed to check public holiday: {}", e.getMessage());
            return false;
        }
    }

    public LocalDate adjustForHoliday(LocalDate date) {
        while (isPublicHoliday(date) || isWeekend(date)) {
            date = date.plusDays(1);
        }
        return date;
    }

    private boolean isWeekend(LocalDate date) {
        return date.getDayOfWeek().getValue() >= 6;
    }

    public String getHolidayName(LocalDate date) {
        try {
            List<Map<String, Object>> holidays =
                    holidayClient.getPublicHolidays(date.getYear(), COUNTRY_CODE);
            return holidays.stream()
                    .filter(h -> h.get("date").equals(date.toString()))
                    .map(h -> (String) h.get("name"))
                    .findFirst()
                    .orElse("Public Holiday");
        } catch (Exception e) {
            log.error("Failed to get holiday name: {}", e.getMessage());
            return "Public Holiday";
        }
    }
}
