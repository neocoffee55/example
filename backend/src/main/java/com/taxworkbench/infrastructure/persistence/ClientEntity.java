package com.taxworkbench.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "client")
public class ClientEntity {

    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(name = "biz_no", nullable = false)
    private String bizNo;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private String tier;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ClientEntity() {
    }

    public ClientEntity(String id, String name, String bizNo, String type, String status, String tier, Instant updatedAt) {
        this.id = id;
        this.name = name;
        this.bizNo = bizNo;
        this.type = type;
        this.status = status;
        this.tier = tier;
        this.updatedAt = updatedAt;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getBizNo() {
        return bizNo;
    }

    public String getType() {
        return type;
    }

    public String getStatus() {
        return status;
    }

    public String getTier() {
        return tier;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
