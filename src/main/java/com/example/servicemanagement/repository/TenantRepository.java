package com.example.servicemanagement.repository;

import com.example.servicemanagement.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantRepository extends JpaRepository<Tenant, Integer> {
    Tenant findTenantById(Integer id);

    Tenant findTenantByUserId(Integer userId);
}
