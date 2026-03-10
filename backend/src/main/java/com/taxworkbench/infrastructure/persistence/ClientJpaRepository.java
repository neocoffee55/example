package com.taxworkbench.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientJpaRepository extends JpaRepository<ClientEntity, String> {

    List<ClientEntity> findByNameContainingIgnoreCaseOrBizNoContainingIgnoreCase(String name, String bizNo);
}
