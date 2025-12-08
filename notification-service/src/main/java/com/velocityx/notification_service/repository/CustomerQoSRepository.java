package com.velocityx.notification_service.repository;

import com.velocityx.notification_service.entity.CustomerQoS;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerQoSRepository extends JpaRepository<CustomerQoS, Long> {
    
    Optional<CustomerQoS> findByCustomerId(Long customerId);
}
