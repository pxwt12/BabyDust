package com.babydust.api.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class PregnancyServiceTest {
    @Test
    void calculatesGestationalAgeFromLmp() {
        PregnancyService service = new PregnancyService(Clock.fixed(Instant.parse("2026-05-25T00:00:00Z"), ZoneId.of("UTC")));

        PregnancyService.GestationalAge age = service.gestationalAge(java.time.LocalDate.parse("2026-04-13"), java.time.LocalDate.parse("2026-05-25"));

        assertThat(age.weeks()).isEqualTo(6);
        assertThat(age.days()).isZero();
        assertThat(age.display()).isEqualTo("6+0");
        assertThat(age.pregnancyDay()).isEqualTo(43);
        assertThat(age.trimester()).isEqualTo(1);
    }
}
