package com.babydust.api.repository;

import com.babydust.api.domain.Reminder;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReminderRepository extends JpaRepository<Reminder, UUID> {
    List<Reminder> findTop50ByFamilyIdOrderByTriggerAtAsc(UUID familyId);
}
