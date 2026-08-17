package com.babydust.api.repository;

import com.babydust.api.domain.TodoItem;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TodoItemRepository extends JpaRepository<TodoItem, UUID> {
    List<TodoItem> findTop50ByFamilyIdOrderByDueAtAsc(UUID familyId);

    List<TodoItem> findTop100ByFamilyIdAndSubjectTypeAndSubjectIdOrderByDueAtAsc(UUID familyId, String subjectType, UUID subjectId);

    List<TodoItem> findTop200ByFamilyIdAndCategoryInOrderByDueAtAsc(UUID familyId, List<String> categories);

    boolean existsByFamilyIdAndSubjectTypeAndSubjectIdAndCategoryAndTitle(UUID familyId, String subjectType, UUID subjectId, String category, String title);
}
