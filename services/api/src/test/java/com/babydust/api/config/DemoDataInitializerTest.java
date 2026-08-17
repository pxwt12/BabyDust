package com.babydust.api.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.babydust.api.repository.AiConfigRepository;
import com.babydust.api.repository.AppUserRepository;
import com.babydust.api.repository.JsonRecordRepository;
import com.babydust.api.repository.MedicalReportRepository;
import com.babydust.api.repository.PregnancyProfileRepository;
import com.babydust.api.repository.TodoItemRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest(properties = "babydust.demo-data.enabled=true")
@DirtiesContext
class DemoDataInitializerTest {
    @Autowired
    private AppUserRepository users;

    @Autowired
    private PregnancyProfileRepository pregnancies;

    @Autowired
    private JsonRecordRepository records;

    @Autowired
    private MedicalReportRepository reports;

    @Autowired
    private TodoItemRepository todos;

    @Autowired
    private AiConfigRepository aiConfigs;

    @Autowired
    private DemoDataInitializer initializer;

    @Test
    void seedsDemoFamilyDataOnStartup() {
        assertThat(users.findByOpenid(DemoDataInitializer.DEMO_OPENID)).isPresent();
        assertThat(pregnancies.findByFamilyId(DemoDataInitializer.DEMO_FAMILY_ID)).hasSize(1);
        assertThat(records.findTop500ByFamilyIdOrderByOccurredAtAsc(DemoDataInitializer.DEMO_FAMILY_ID)).hasSizeGreaterThanOrEqualTo(20);
        assertThat(reports.findTop200ByFamilyIdOrderByExaminedAtAsc(DemoDataInitializer.DEMO_FAMILY_ID)).hasSize(3);
        assertThat(todos.findTop50ByFamilyIdOrderByDueAtAsc(DemoDataInitializer.DEMO_FAMILY_ID)).hasSizeGreaterThanOrEqualTo(5);
        assertThat(aiConfigs.findTop50ByOrderByCreatedAtDesc()).anyMatch(config -> "demo-deepseek-provider".equals(config.getConfigKey()));
    }

    @Test
    void reseedingIsIdempotent() throws Exception {
        int recordCount = records.findTop500ByFamilyIdOrderByOccurredAtAsc(DemoDataInitializer.DEMO_FAMILY_ID).size();
        int reportCount = reports.findTop200ByFamilyIdOrderByExaminedAtAsc(DemoDataInitializer.DEMO_FAMILY_ID).size();
        int todoCount = todos.findTop50ByFamilyIdOrderByDueAtAsc(DemoDataInitializer.DEMO_FAMILY_ID).size();

        initializer.run(new DefaultApplicationArguments());

        assertThat(pregnancies.findByFamilyId(DemoDataInitializer.DEMO_FAMILY_ID)).hasSize(1);
        assertThat(records.findTop500ByFamilyIdOrderByOccurredAtAsc(DemoDataInitializer.DEMO_FAMILY_ID)).hasSize(recordCount);
        assertThat(reports.findTop200ByFamilyIdOrderByExaminedAtAsc(DemoDataInitializer.DEMO_FAMILY_ID)).hasSize(reportCount);
        assertThat(todos.findTop50ByFamilyIdOrderByDueAtAsc(DemoDataInitializer.DEMO_FAMILY_ID)).hasSize(todoCount);
    }
}
