package com.taxworkbench.bootstrap;

import com.taxworkbench.domain.shared.ClientStatus;
import com.taxworkbench.domain.shared.ClientTier;
import com.taxworkbench.domain.shared.ClientType;
import com.taxworkbench.domain.shared.WorkItemStatus;
import com.taxworkbench.domain.shared.WorkItemType;
import com.taxworkbench.infrastructure.persistence.ClientEntity;
import com.taxworkbench.infrastructure.persistence.ClientJpaRepository;
import com.taxworkbench.infrastructure.persistence.WorkItemEntity;
import com.taxworkbench.infrastructure.persistence.WorkItemJpaRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class SeedDataConfig {

    @Bean
    CommandLineRunner seedData(ClientJpaRepository clientJpaRepository, WorkItemJpaRepository workItemJpaRepository) {
        return ignored -> {
            if (clientJpaRepository.count() > 0 || workItemJpaRepository.count() > 0) {
                return;
            }

            ClientEntity client = new ClientEntity();
            client.setName("Hanbit Tax");
            client.setBizNo("123-45-67890");
            client.setType(ClientType.CORPORATE);
            client.setStatus(ClientStatus.ACTIVE);
            client.setTier(ClientTier.VIP);
            ClientEntity savedClient = clientJpaRepository.save(client);

            WorkItemEntity workItem = new WorkItemEntity();
            workItem.setClient(savedClient);
            workItem.setType(WorkItemType.FILING);
            workItem.setStatus(WorkItemStatus.TODO);
            workItem.setAssignee("kim");
            workItem.setDueDate(LocalDate.parse("2026-03-20"));
            workItem.setTags(new ArrayList<>(List.of("march")));
            workItem.setMemo("priority filing");
            workItemJpaRepository.save(workItem);
        };
    }
}
