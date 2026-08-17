package com.babydust.api.repository;

import com.babydust.api.domain.AiConfig;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiConfigRepository extends JpaRepository<AiConfig, UUID> {
    List<AiConfig> findTop50ByOrderByCreatedAtDesc();

    List<AiConfig> findTop50ByConfigTypeOrderByCreatedAtDesc(String configType);

    List<AiConfig> findTop50ByConfigTypeAndStatusOrderByCreatedAtDesc(String configType, String status);

    Optional<AiConfig> findFirstByConfigTypeAndStatusOrderByCreatedAtDesc(String configType, String status);
}
