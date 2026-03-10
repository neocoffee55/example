package com.taxworkbench.bootstrap;

import com.taxworkbench.infrastructure.persistence.ClientEntity;
import com.taxworkbench.infrastructure.persistence.ClientJpaRepository;
import com.taxworkbench.infrastructure.persistence.WorkItemEntity;
import com.taxworkbench.infrastructure.persistence.WorkItemJpaRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class SeedDataConfig {

    @Bean
    CommandLineRunner seedWorkbenchData(ClientJpaRepository clientRepository, WorkItemJpaRepository workItemRepository) {
        return args -> {
            if (clientRepository.count() == 0) {
                clientRepository.saveAll(List.of(
                        new ClientEntity("CL-1001", "Han River Holdings", "123-45-67890", "CORPORATE", "ACTIVE", "VIP", Instant.now()),
                        new ClientEntity("CL-1002", "Mirae Clinic", "220-11-90876", "CORPORATE", "ACTIVE", "PREMIUM", Instant.now())
                ));
            }

            if (workItemRepository.count() == 0) {
                workItemRepository.saveAll(List.of(
                        new WorkItemEntity("WI-10031", "Han River Holdings", "123-45-67890", "FILING", "IN_PROGRESS", "insu", LocalDate.parse("2026-03-20"), Instant.now(), 0L),
                        new WorkItemEntity("WI-10032", "Mirae Clinic", "220-11-90876", "REVIEW", "HOLD", "jane", LocalDate.parse("2026-03-18"), Instant.now(), 0L)
                ));
            }
        };
    }
}
