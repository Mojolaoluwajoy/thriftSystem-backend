package com.thrift.thriftsystem.service;

import com.thrift.thriftsystem.model.User;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SchedulerService {

    private final CurrencyService currencyService;
    private final HolidayService holidayService;
    private final EmailService emailService;
    private final UserService userService;
    private final ContributionService contributionService;
    private final CycleService cycleService;

    @Scheduled(cron = "0 0 0 * * *")
    public void updateExchangeRates() {
        log.info("Scheduled exchange rate update starting...");
        currencyService.updateExchangeRates();
        log.info("Scheduled exchange rate update completed.");
    }

    @Scheduled(cron = "0 30 0 * * *")
    public void advanceStalledGroups() {
        cycleService.advanceStalledGroups();
    }

    @Scheduled(cron = "0 0 9 * * *")
    public void sendContributionReminders() {
        int sent = contributionService.sendDueReminders();
        log.info("Contribution reminders sent: {}", sent);
    }

    @Scheduled(cron = "0 0 8 * * *")
    public void sendHolidayNotifications() {
        LocalDate today = LocalDate.now();

        if (holidayService.isPublicHoliday(today)) {
            String holidayName = holidayService.getHolidayName(today);
            List<User> users = userService.getAllUserModels();

            users.forEach(user -> emailService.sendHolidayNotification(user, holidayName));

            log.info("Holiday notifications sent to {} users", users.size());
        }
    }
}
