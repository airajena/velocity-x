package com.velocityx.transaction_service.util;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

/**
 * Base entity with audit fields
 * All entities should extend this class
 * 
 * Provides:
 * - Created timestamp and user
 * - Last modified timestamp and user
 * - Optimistic locking with version
 * 
 * @author VelocityX Team
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public abstract class BaseEntity {
    
    /**
     * Timestamp when entity was created
     * Automatically set by JPA auditing
     * Cannot be updated after creation
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    /**
     * Timestamp when entity was last modified
     * Automatically updated by JPA auditing
     */
    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;
    
    /**
     * User who created this entity
     * Automatically set by JPA auditing
     * Cannot be updated after creation
     */
    @CreatedBy
    @Column(name = "created_by", length = 255, updatable = false)
    private String createdBy;
    
    /**
     * User who last modified this entity
     * Automatically updated by JPA auditing
     */
    @LastModifiedBy
    @Column(name = "updated_by", length = 255)
    private String updatedBy;
    
    /**
     * Version for optimistic locking
     * Prevents lost updates in concurrent scenarios
     * Automatically incremented by JPA on each update
     */
    @Version
    @Column(name = "version")
    private Long version;
}
