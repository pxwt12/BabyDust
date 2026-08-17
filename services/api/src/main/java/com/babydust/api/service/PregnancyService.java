package com.babydust.api.service;

import com.babydust.api.domain.PregnancyProfile;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Service;

@Service
public class PregnancyService {
    private final Clock clock;

    public PregnancyService(Clock clock) {
        this.clock = clock;
    }

    public GestationalAge gestationalAge(PregnancyProfile pregnancy) {
        return gestationalAge(pregnancy.getLmpDate(), LocalDate.now(clock));
    }

    public GestationalAge gestationalAge(LocalDate lmpDate, LocalDate onDate) {
        long daysSinceLmp = Math.max(0, ChronoUnit.DAYS.between(lmpDate, onDate));
        int completedWeeks = (int) (daysSinceLmp / 7);
        int extraDays = (int) (daysSinceLmp % 7);
        int pregnancyDay = (int) daysSinceLmp + 1;
        int trimester = trimester(completedWeeks);
        return new GestationalAge(completedWeeks, extraDays, pregnancyDay, trimester, completedWeeks + "+" + extraDays);
    }

    private int trimester(int completedWeeks) {
        if (completedWeeks < 14) {
            return 1;
        }
        if (completedWeeks < 28) {
            return 2;
        }
        return 3;
    }

    public record GestationalAge(int weeks, int days, int pregnancyDay, int trimester, String display) {
    }
}
